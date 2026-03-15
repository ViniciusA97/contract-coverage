package org.example.core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.entities.HttpMethod
import org.example.core.services.filedetection.ClientFileDetector
import org.example.core.services.filedetection.ProjectFileScanner
import org.example.core.services.filedetection.RestTemplateFileDetector
import org.example.core.wrappers.StaticCodeAnalyzer
import org.example.core.wrappers.spoon.callextractors.CallExtractor
import org.example.core.wrappers.spoon.callextractors.ExchangeCallExtractor
import org.example.core.wrappers.spoon.callextractors.SimpleMethodCallExtractor
import spoon.Launcher
import org.example.core.entities.UnresolvedMarkers
import spoon.reflect.CtModel
import spoon.reflect.code.*
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtMethod

data class MethodCallContext(
    val method: CtMethod<*>,
    val arguments: List<CtExpression<*>>
)

class SpoonWrapper(
    private val projectDir: String,
    private val detectors: List<ClientFileDetector> = listOf(RestTemplateFileDetector()),
    private val callerDepth: Int = 1
) : StaticCodeAnalyzer {

    private val launcher = initLauncher()
    private val spoonExpressionResolver = SpoonExpressionResolver()
    private val classifier = RestTemplateCallClassifier()
    private val rootUriDetector = RootUriDetector(spoonExpressionResolver)
    private val parameterFlowAnalyzer = ParameterFlowAnalyzer()
    private val extractors: List<CallExtractor> = listOf(
        ExchangeCallExtractor(classifier, spoonExpressionResolver, rootUriDetector),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateGetForEntity(call) }, HttpMethod.GET, classifier, spoonExpressionResolver, rootUriDetector),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateGetForObject(call) }, HttpMethod.GET, classifier, spoonExpressionResolver, rootUriDetector),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePostForEntity(call) }, HttpMethod.POST, classifier, spoonExpressionResolver, rootUriDetector),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePostForObject(call) }, HttpMethod.POST, classifier, spoonExpressionResolver, rootUriDetector),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePut(call) }, HttpMethod.PUT, classifier, spoonExpressionResolver, rootUriDetector),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePatch(call) }, HttpMethod.PATCH, classifier, spoonExpressionResolver, rootUriDetector),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateDelete(call) }, HttpMethod.DELETE, classifier, spoonExpressionResolver, rootUriDetector),
    )

    override fun analyzeInvocations(): List<Endpoint> {
        val model = launcher.model
        return extractEndpoints(model)
    }

    fun extractEndpoints(model: CtModel): List<Endpoint> {
        // Phase 1: Direct RestTemplate calls
        val projectCalls = findCallsWithResolvedArgs(model)
        val directEndpoints = projectCalls.mapNotNull { (call, context) ->
            val extractor = extractors.firstOrNull { it.supports(call) } ?: return@mapNotNull null
            extractor.extract(call, context, model)
        }
        
        // Phase 2: Endpoints through wrapper methods (parameter flow analysis)
        val wrapperEndpoints = findEndpointsThroughWrappers(model)
        
        // Deduplicate endpoints
        // For dynamic URLs (<dynamic-url>), keep separate entries per sourceFile
        // For resolved URLs, deduplicate by path+method
        val allEndpoints = directEndpoints + wrapperEndpoints
        return allEndpoints
            .groupBy { endpoint ->
                if (UnresolvedMarkers.isUnresolved(endpoint.path)) {
                    // For dynamic URLs, key includes sourceFile to keep them separate
                    Triple(endpoint.path, endpoint.method, endpoint.sourceFile)
                } else {
                    // For resolved URLs, key by path+method only
                    Triple(endpoint.path, endpoint.method, null)
                }
            }
            .map { (_, endpoints) ->
                // Prefer endpoint with non-null sourceFile
                endpoints.firstOrNull { it.sourceFile != null } ?: endpoints.first()
            }
    }
    
    /**
     * Find endpoints that are passed through wrapper methods.
     * 
     * Detects patterns like:
     * ```java
     * // Pattern 1: Wrapper class with RestTemplate field
     * class MyRestClient {
     *     private RestTemplate restTemplate;
     *     
     *     public <T> T post(String endpoint, Object body, Class<T> type) {
     *         return restTemplate.postForEntity(baseUrl + endpoint, body, type);
     *     }
     * }
     * 
     * // Pattern 2: Wrapper class with method that returns RestTemplate
     * class LowLevelClient {
     *     private RestTemplate createRestTemplate() {
     *         return new RestTemplateBuilder().rootUri(BASE_URL).build();
     *     }
     *     
     *     public <T> T get(String url, Class<T> type) {
     *         return createRestTemplate().exchange(url, GET, ...);
     *     }
     * }
     * 
     * // Service class that uses the wrapper
     * class MyService {
     *     private static final String ENDPOINT = "/api/users";
     *     private MyRestClient restClient;
     *     
     *     void createUser() {
     *         restClient.post(ENDPOINT, ...);  // <- endpoint resolvable here
     *     }
     * }
     * ```
     */
    private fun findEndpointsThroughWrappers(model: CtModel): List<Endpoint> {
        val endpoints = mutableListOf<Endpoint>()
        
        val allClasses = model.allTypes.filterIsInstance<CtClass<*>>()
        
        // Step 1: Find wrapper classes and their wrapper methods
        val wrapperMethods = mutableListOf<WrapperMethodInfo>()
        
        for (ctClass in allClasses) {
            // Check if class has RestTemplate (field OR method that returns RestTemplate)
            val hasRestTemplateField = ctClass.fields.any { 
                it.type?.simpleName in listOf("RestTemplate", "TestRestTemplate") 
            }
            val hasRestTemplateMethod = hasMethodReturningRestTemplate(ctClass)
            
            if (!hasRestTemplateField && !hasRestTemplateMethod) continue
            
            // Find methods that delegate to RestTemplate with parameter as URL part
            for (method in ctClass.methods) {
                val restTemplateCalls = findRestTemplateCallsInMethod(method)
                
                for (restCall in restTemplateCalls) {
                    val urlArg = restCall.arguments.firstOrNull() ?: continue
                    val httpMethod = getHttpMethodFromRestTemplateCall(restCall) ?: continue
                    
                    // Check if URL depends on method parameters (directly or through variable)
                    var pathParamIndex = parameterFlowAnalyzer.findPathParameterIndex(urlArg, method)
                    
                    if (pathParamIndex < 0 && urlArg is CtVariableRead<*>) {
                        val varDeps = parameterFlowAnalyzer.traceVariableToParameter(
                            urlArg.variable.simpleName, method
                        )
                        if (!varDeps.isNullOrEmpty()) {
                            pathParamIndex = varDeps.first().parameterIndex
                        }
                    }
                    
                    if (pathParamIndex >= 0) {
                        wrapperMethods.add(WrapperMethodInfo(
                            wrapperClass = ctClass,
                            method = method,
                            endpointParamIndex = pathParamIndex,
                            httpMethod = httpMethod
                        ))
                    }
                }
            }
        }
        
        // Step 2: Find call sites of wrapper methods and resolve endpoints
        for (wrapperMethod in wrapperMethods) {
            val resolvedEndpoints = resolveEndpointsFromCallSites(
                wrapperMethod.method,
                wrapperMethod.endpointParamIndex,
                wrapperMethod.httpMethod,
                model
            )
            endpoints.addAll(resolvedEndpoints)
        }
        
        return endpoints
    }
    
    /**
     * Check if a class has a method that returns RestTemplate.
     * This detects patterns like: private RestTemplate createRestTemplate() { return new RestTemplateBuilder().build(); }
     */
    private fun hasMethodReturningRestTemplate(ctClass: CtClass<*>): Boolean {
        return ctClass.methods.any { method ->
            val returnType = method.type?.simpleName
            returnType in listOf("RestTemplate", "TestRestTemplate")
        }
    }
    
    /**
     * Info about a wrapper method that delegates to RestTemplate
     */
    private data class WrapperMethodInfo(
        val wrapperClass: CtClass<*>,
        val method: CtMethod<*>,
        val endpointParamIndex: Int,
        val httpMethod: HttpMethod
    )
    
    /**
     * Find RestTemplate method invocations in a method body.
     */
    private fun findRestTemplateCallsInMethod(method: CtMethod<*>): List<CtInvocation<*>> {
        val body = method.body ?: return emptyList()
        val result = mutableListOf<CtInvocation<*>>()
        findRestTemplateCallsRecursive(body, result)
        return result
    }
    
    private fun findRestTemplateCallsRecursive(element: spoon.reflect.declaration.CtElement, result: MutableList<CtInvocation<*>>) {
        if (element is CtInvocation<*> && classifier.isRestTemplateCall(element)) {
            result.add(element)
        }
        for (child in element.directChildren) {
            findRestTemplateCallsRecursive(child, result)
        }
    }
    
    /**
     * Get HTTP method from RestTemplate call.
     */
    private fun getHttpMethodFromRestTemplateCall(call: CtInvocation<*>): HttpMethod? {
        return when (call.executable?.simpleName) {
            "getForEntity", "getForObject" -> HttpMethod.GET
            "postForEntity", "postForObject" -> HttpMethod.POST
            "put" -> HttpMethod.PUT
            "patchForObject" -> HttpMethod.PATCH
            "delete" -> HttpMethod.DELETE
            "exchange" -> {
                val methodArg = call.arguments.getOrNull(1)?.toString() ?: ""
                when {
                    methodArg.contains("GET") -> HttpMethod.GET
                    methodArg.contains("POST") -> HttpMethod.POST
                    methodArg.contains("PUT") -> HttpMethod.PUT
                    methodArg.contains("PATCH") -> HttpMethod.PATCH
                    methodArg.contains("DELETE") -> HttpMethod.DELETE
                    else -> null
                }
            }
            else -> null
        }
    }
    
    /**
     * Find call sites of a method and resolve the endpoint argument.
     */
    private fun resolveEndpointsFromCallSites(
        wrapperMethod: CtMethod<*>,
        endpointParamIndex: Int,
        httpMethod: HttpMethod,
        model: CtModel
    ): List<Endpoint> {
        val endpoints = mutableListOf<Endpoint>()
        val methodName = wrapperMethod.simpleName
        val declaringTypeName = wrapperMethod.declaringType?.simpleName
        
        // Find all invocations of this method
        val allClasses = model.allTypes.filterIsInstance<CtClass<*>>()
        
        for (ctClass in allClasses) {
            for (method in ctClass.methods) {
                val body = method.body ?: continue
                val callSites = findCallSitesInBlock(body, methodName, declaringTypeName)
                
                for (callSite in callSites) {
                    val endpointArg = callSite.arguments.getOrNull(endpointParamIndex) ?: continue
                    
                    val resolvedPath = spoonExpressionResolver.resolveExpression(endpointArg, method, model)
                    
                    if (!UnresolvedMarkers.isUnresolved(resolvedPath)) {
                        val path = org.example.core.utils.urlToPath(resolvedPath)
                        // Only add valid paths (must start with / or be a full URL)
                        if (!UnresolvedMarkers.isPathUnresolved(path) && 
                            (path.startsWith("/") || path.contains("://"))) {
                            // Extract source file from call site
                            val sourceFile = callSite.position?.file?.name
                            endpoints.add(Endpoint(path, httpMethod, sourceFile))
                        }
                    }
                }
            }
        }
        
        return endpoints
    }
    
    /**
     * Find call sites in a code block.
     */
    private fun findCallSitesInBlock(
        block: CtBlock<*>,
        methodName: String,
        declaringTypeName: String?
    ): List<CtInvocation<*>> {
        val result = mutableListOf<CtInvocation<*>>()
        findCallSitesRecursive(block, methodName, declaringTypeName, result)
        return result
    }
    
    private fun findCallSitesRecursive(
        element: spoon.reflect.declaration.CtElement,
        methodName: String,
        declaringTypeName: String?,
        result: MutableList<CtInvocation<*>>
    ) {
        if (element is CtInvocation<*>) {
            val execMethodName = element.executable?.simpleName
            val execDeclaringType = element.executable?.declaringType?.simpleName
            
            if (execMethodName == methodName) {
                // Match by method name, optionally check declaring type
                if (declaringTypeName == null || execDeclaringType == declaringTypeName) {
                    result.add(element)
                }
            }
        }
        
        for (child in element.directChildren) {
            findCallSitesRecursive(child, methodName, declaringTypeName, result)
        }
    }

    fun findCallsWithResolvedArgs(model: CtModel): List<Pair<CtInvocation<*>, MethodCallContext>> {
        val result = mutableListOf<Pair<CtInvocation<*>, MethodCallContext>>()

        val allInvocations = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filterIsInstance<CtInvocation<*>>()

        allInvocations.forEach { invocation ->
            if (!classifier.isRestTemplateCall(invocation)) return@forEach

            val parentMethod = invocation.getParent(CtMethod::class.java) ?: return@forEach

            // Para cada chamada ao método que contém o RestTemplate, gerar um contexto com os argumentos do call site
            val callSites = findMethodCallSites(parentMethod, model)

            if (callSites.isEmpty()) {
                // No call sites found. Include if:
                // 1. It's a main method, OR
                // 2. It's a private method (implementation detail, typically called by other methods in same class)
                val isMainMethod = parentMethod.simpleName == "main"
                val isPrivateMethod = parentMethod.isPrivate
                
                if (isMainMethod || isPrivateMethod) {
                    result.add(invocation to MethodCallContext(parentMethod, emptyList()))
                }
            } else {
                callSites.forEach { site ->
                    result.add(invocation to MethodCallContext(parentMethod, site.arguments))
                }
            }
        }

        return result
    }
    
    private fun findMethodCallSites(method: CtMethod<*>, model: CtModel): List<CtInvocation<*>> {
        val allInvocations = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filterIsInstance<CtInvocation<*>>()
        return allInvocations.filter { inv ->
            val decl = inv.executable.declaration as? CtMethod<*>
            decl?.signature == method.signature &&
                    decl.declaringType?.qualifiedName == method.declaringType?.qualifiedName
        }
    }

    private fun initLauncher(): Launcher {
        try {
            val launcher = Launcher()
            
            // Configure Spoon to be more tolerant
            launcher.environment.apply {
                setNoClasspath(true)
                setIgnoreDuplicateDeclarations(true)
                complianceLevel = 17
                isAutoImports = false
            }
            
            val file = java.io.File(projectDir)
            
            if (file.isDirectory) {
                // Use ProjectFileScanner for smart file detection
                val scanner = ProjectFileScanner(detectors)
                val scanResult = scanner.scan(file, callerDepth)
                
                // Print scan statistics
                scanner.printScanStats(scanResult)
                
                // Add all relevant files to Spoon
                scanResult.allRelevantFiles.forEach { javaFile ->
                    launcher.addInputResource(javaFile.absolutePath)
                }
            } else if (file.isFile && file.extension == "java") {
                // Se for um arquivo Java específico
                launcher.addInputResource(projectDir)
            } else {
                // Fallback: adiciona como está
                launcher.addInputResource(projectDir)
            }
            
            launcher.buildModel()
            return launcher
        } catch (e: Exception) {
            // Check if error is related to unsupported Java version (e.g., "Unrecognized option : -23")
            var currentException: Throwable? = e
            var foundUnrecognizedOption = false
            var versionNumber = "unknown"
            
            while (currentException != null) {
                val message = currentException.message ?: ""
                if (message.contains("Unrecognized option")) {
                    foundUnrecognizedOption = true
                    val versionMatch = Regex("""-(\d+)""").find(message)
                    versionNumber = versionMatch?.groupValues?.get(1) ?: "unknown"
                    break
                }
                currentException = currentException.cause
            }
            
            if (foundUnrecognizedOption) {
                throw RuntimeException(
                    "Spoon detected Java version $versionNumber which is not supported by the JDT compiler. " +
                    "The JDT compiler used by Spoon may not support Java versions newer than 21. " +
                    "This error occurs when the binary was compiled with a newer Java version than what JDT supports. " +
                    "Original error: ${e.message}",
                    e
                )
            }
            
            // Build detailed error message
            val errorDetails = buildString {
                append("Failed to initialize Spoon launcher")
                if (e.message != null) {
                    append(": ${e.message}")
                }
                if (e.cause != null) {
                    append("\nCaused by: ${e.cause?.javaClass?.simpleName}")
                    if (e.cause?.message != null) {
                        append(": ${e.cause?.message}")
                    }
                }
            }
            
            throw RuntimeException(errorDetails, e)
        }
    }
}