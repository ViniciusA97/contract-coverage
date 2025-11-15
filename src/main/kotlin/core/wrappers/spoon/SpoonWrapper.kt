package org.example.core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.entities.HttpMethod
import org.example.core.wrappers.StaticCodeAnalyzer
import org.example.core.wrappers.spoon.callextractors.CallExtractor
import org.example.core.wrappers.spoon.callextractors.ExchangeCallExtractor
import org.example.core.wrappers.spoon.callextractors.SimpleMethodCallExtractor
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.code.*
import spoon.reflect.declaration.CtMethod

data class MethodCallContext(
    val method: CtMethod<*>,
    val arguments: List<CtExpression<*>>
)

class SpoonWrapper(
    private val projectDir: String,
) : StaticCodeAnalyzer {

    private val launcher = initLauncher()
    private val spoonExpressionResolver = SpoonExpressionResolver()
    private val classifier = RestTemplateCallClassifier()
    private val extractors: List<CallExtractor> = listOf(
        ExchangeCallExtractor(classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateGetForEntity(call) }, HttpMethod.GET, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateGetForObject(call) }, HttpMethod.GET, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePostForEntity(call) }, HttpMethod.POST, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePostForObject(call) }, HttpMethod.POST, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePut(call) }, HttpMethod.PUT, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePatch(call) }, HttpMethod.PATCH, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateDelete(call) }, HttpMethod.DELETE, classifier, spoonExpressionResolver),
    )

    override fun analyzeInvocations(): List<Endpoint> {
        val model = launcher.model
        return extractEndpoints(model)
    }

    fun extractEndpoints(model: CtModel): List<Endpoint> {
        val projectCalls = findCallsWithResolvedArgs(model)

        val endpoints = projectCalls.mapNotNull { (call, context) ->
            val extractor = extractors.firstOrNull { it.supports(call) } ?: return@mapNotNull null
            extractor.extract(call, context, model)
        }

        return endpoints.distinctBy { it.path to it.method }
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
                // Sem call sites: só considerar se for main (evita falsos positivos do teste 1)
                if (parentMethod.simpleName == "main") {
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
            val file = java.io.File(projectDir)
            
            if (file.isDirectory) {
                // Se for um diretório, adiciona recursivamente todos os arquivos Java
                val javaFiles = file.walkTopDown()
                    .filter { it.isFile && it.extension == "java" }
                    .toList()
                
                println("Found ${javaFiles.size} Java files in $projectDir")
                javaFiles.forEach { javaFile ->
                    launcher.addInputResource(javaFile.absolutePath)
                }
            } else if (file.isFile && file.extension == "java") {
                // Se for um arquivo Java específico
                println("Processing single Java file: $projectDir")
                launcher.addInputResource(projectDir)
            } else {
                // Fallback: adiciona como está
                println("Adding input resource as-is: $projectDir")
                launcher.addInputResource(projectDir)
            }
            
            launcher.buildModel()
            println("Spoon model built. Total types: ${launcher.model.allTypes.size}")
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