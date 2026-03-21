package org.example.core.wrappers.spoon

import org.example.core.entities.UnresolvedMarkers
import spoon.reflect.CtModel
import spoon.reflect.code.CtBinaryOperator
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtFieldRead
import spoon.reflect.code.CtInvocation
import spoon.reflect.code.CtLiteral
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.code.CtReturn
import spoon.reflect.code.CtVariableRead
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtField
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtParameter
import spoon.reflect.reference.CtLocalVariableReference
import spoon.reflect.visitor.Filter

class SpoonExpressionResolver {

    /**
     * Context for parameter resolution within a specific call site
     */
    data class ParamContext(
        val params: List<CtParameter<*>>,
        val args: List<CtExpression<*>>
    )
    
    fun resolveExpressionWithParams(
        expr: CtExpression<*>,
        params: List<CtParameter<*>>,
        args: List<CtExpression<*>>,
        scopeMethod: CtMethod<*>?,
        model: CtModel
    ): String {
        val context = ParamContext(params, args)
        return resolveExpressionWithContext(expr, scopeMethod, model, context)
    }
    
    private fun resolveExpressionWithContext(
        expr: CtExpression<*>?,
        contextMethod: CtMethod<*>?,
        model: CtModel,
        paramContext: ParamContext?,
        depth: Int = 0
    ): String {
        if (expr == null) return "null expression"
        if (depth > 5) return UnresolvedMarkers.UNRESOLVED_URL
        
        return when (expr) {
            is CtVariableRead<*> -> {
                val varName = expr.variable.simpleName
                
                // First check if it's a parameter that we have an argument for
                if (paramContext != null) {
                    val index = paramContext.params.indexOfFirst { it.simpleName == varName }
                    if (index >= 0 && index < paramContext.args.size) {
                        val argExpr = paramContext.args[index]
                        val callerMethod = argExpr.getParent(CtMethod::class.java)
                        // Resolve the argument with NO param context (it's already the actual value)
                        return resolveExpression(argExpr, callerMethod ?: contextMethod, model, null, depth + 1)
                    }
                }
                
                // Check for local variable - resolve with context
                val localVar = contextMethod?.getElements<CtLocalVariable<*>>{it is CtLocalVariable<*>}
                    ?.filterIsInstance<CtLocalVariable<*>>()
                    ?.find { it.simpleName == varName }
                
                if (localVar != null) {
                    val defaultExpr = localVar.defaultExpression
                    if (defaultExpr != null) {
                        // Resolve with param context to propagate arguments
                        return resolveExpressionWithContext(defaultExpr, contextMethod, model, paramContext, depth + 1)
                    }
                }
                
                // Otherwise resolve normally
                resolveExpression(expr, contextMethod, model, null, depth + 1)
            }
            
            is CtLiteral<*> -> expr.value.toString()
            
            is CtBinaryOperator<*> -> {
                val left = resolveExpressionWithContext(expr.leftHandOperand, contextMethod, model, paramContext, depth + 1)
                val right = resolveExpressionWithContext(expr.rightHandOperand, contextMethod, model, paramContext, depth + 1)
                
                if (UnresolvedMarkers.isUnresolved(left) && right.startsWith("/")) {
                    return right
                }
                
                val result = left + right
                return if (UnresolvedMarkers.isUnresolved(right) && left.startsWith("/")) {
                    left.trimEnd('/') + "/{dynamic}"
                } else if (result.contains(UnresolvedMarkers.UNRESOLVED_VARIABLE)) {
                    result.replace(UnresolvedMarkers.UNRESOLVED_VARIABLE, "{dynamic}")
                } else {
                    result
                }
            }
            
            is CtInvocation<*> -> {
                // Check for UriComponentsBuilder pattern
                val uriBuilderResult = tryResolveUriComponentsBuilderWithContext(expr, contextMethod, model, paramContext, depth)
                if (uriBuilderResult != null) {
                    return uriBuilderResult
                }
                
                // Fall back to regular resolution
                resolveExpression(expr, contextMethod, model, null, depth + 1)
            }
            
            else -> resolveExpression(expr, contextMethod, model, null, depth + 1)
        }
    }
    
    private fun tryResolveUriComponentsBuilderWithContext(
        expr: CtInvocation<*>,
        contextMethod: CtMethod<*>?,
        model: CtModel,
        paramContext: ParamContext?,
        depth: Int
    ): String? {
        val methodName = expr.executable.simpleName
        val typeName = expr.executable.declaringType?.qualifiedName ?: expr.type?.qualifiedName
        
        val isBuilder = typeName?.contains("UriComponents") == true ||
                        typeName?.contains("UriBuilder") == true ||
                        methodName in listOf("build", "toUri", "toUriString", "encode", "buildAndExpand")
        
        if (!isBuilder) return null
        
        // Extract chain info with param context
        val chainInfo = extractUriBuilderChainWithContext(expr, contextMethod, model, paramContext, depth)
        
        val segmentsPath = chainInfo.pathSegments.joinToString("")
        
        if (segmentsPath.isNotEmpty()) {
            val baseUrlPath = chainInfo.baseUrl?.let {
                if (!UnresolvedMarkers.isUnresolved(it)) { extractPathFromUrl(it) } else { "" }
            } ?: ""
            
            val fullPath = if (baseUrlPath.isNotEmpty() && baseUrlPath != "/") {
                baseUrlPath + segmentsPath
            } else {
                segmentsPath
            }
            
            return if (fullPath.startsWith("/")) fullPath else "/$fullPath"
        }
        
        return chainInfo.baseUrl?.let { 
            if (!UnresolvedMarkers.isUnresolved(it)) {
                extractPathFromUrl(it)
            } else {
                null
            }
        }
    }
    
    private fun extractUriBuilderChainWithContext(
        expr: CtInvocation<*>,
        contextMethod: CtMethod<*>?,
        model: CtModel,
        paramContext: ParamContext?,
        depth: Int
    ): UriBuilderChainInfo {
        var baseUrl: String? = null
        val pathSegments = mutableListOf<String>()
        
        var current: CtExpression<*>? = expr
        
        while (current is CtInvocation<*>) {
            val invocation = current
            val methodName = invocation.executable.simpleName
            
            when (methodName) {
                "path" -> {
                    val pathArg = invocation.arguments.firstOrNull()
                    if (pathArg != null) {
                        val resolvedPath = resolveExpressionWithContext(pathArg, contextMethod, model, paramContext, depth + 1)
                        if (!UnresolvedMarkers.isUnresolved(resolvedPath)) {
                            pathSegments.add(0, resolvedPath)
                        } else if (resolvedPath.contains("{dynamic}")) {
                            pathSegments.add(0, resolvedPath)
                        }
                    }
                }
                "pathSegment" -> {
                    val segments = invocation.arguments.map { arg ->
                        val resolved = resolveExpressionWithContext(arg, contextMethod, model, paramContext, depth + 1)
                        if (!UnresolvedMarkers.isUnresolved(resolved) && resolved.isNotBlank()) {
                            resolved
                        } else {
                            "{dynamic}"
                        }
                    }
                    if (segments.isNotEmpty()) {
                        val combinedPath = "/" + segments.joinToString("/")
                        pathSegments.add(0, combinedPath)
                    }
                }
                "fromHttpUrl", "fromUriString", "fromPath" -> {
                    val urlArg = invocation.arguments.firstOrNull()
                    if (urlArg != null) {
                        baseUrl = resolveExpressionWithContext(urlArg, contextMethod, model, paramContext, depth + 1)
                    }
                }
            }
            
            current = invocation.target
        }
        
        return UriBuilderChainInfo(baseUrl, pathSegments)
    }

    fun resolveExpression(expr: CtExpression<*>?, contextMethod: CtMethod<*>?, model: CtModel, context: CtElement? = null, depth: Int = 0): String {
        if (expr == null) return "null expression"
        
        // Prevent infinite recursion
        if (depth > 5) return UnresolvedMarkers.UNRESOLVED_URL

        return when (expr) {
            is CtLiteral<*> -> expr.value.toString()

            is CtBinaryOperator<*> -> {
                val left = resolveExpression(expr.leftHandOperand, contextMethod, model)
                val right = resolveExpression(expr.rightHandOperand, contextMethod, model)
                
                // If left is dynamic but right is a path (starts with /), use only the path
                // This handles: dynamicBaseUrl + "/api/users" -> "/api/users"
                if (UnresolvedMarkers.isUnresolved(left) && right.startsWith("/")) {
                    return right
                }
                
                // Replace unresolved parts with {dynamic} to preserve path structure
                // This handles: "/users/" + id -> "/users/{dynamic}"
                val result = left + right
                return if (UnresolvedMarkers.isUnresolved(right) && left.startsWith("/")) {
                    left.trimEnd('/') + "/{dynamic}"
                } else if (result.contains(UnresolvedMarkers.UNRESOLVED_VARIABLE)) {
                    result.replace(UnresolvedMarkers.UNRESOLVED_VARIABLE, "{dynamic}")
                } else {
                    result
                }
            }

            is CtFieldRead<*> -> {
                // Verifica se é enum e extrai apenas o nome
                val declType = expr.variable.declaringType?.qualifiedName
                val fieldName = expr.variable.simpleName

                val fieldDecl = model.getElements<CtField<*>> { e -> e is CtField<*> }
                    .find { it.simpleName == fieldName && it.declaringType?.qualifiedName == declType }

                // Se conseguirmos o valor default do campo, usamos ele
                val defaultExpr = fieldDecl?.defaultExpression
                if (defaultExpr != null) {
                    return resolveExpression(defaultExpr, null, model)
                }

                // If field is not found in model, return field name as fallback
                // This handles constants like BASE_URL, ENDPOINT, etc.
                // If the field name looks like a constant (all caps or contains URL/PATH/ENDPOINT), 
                // return it as a placeholder
                val typeName = fieldDecl?.type?.simpleName
                return if (typeName == "String" && fieldName.contains(Regex("[A-Z_]+"))) {
                    // This is likely a String constant we couldn't resolve - mark as dynamic
                    UnresolvedMarkers.UNRESOLVED_VARIABLE
                } else if (typeName != null) {
                    "{$typeName}"
                } else {
                    fieldName
                }
            }

            is CtVariableRead<*> -> {
                val varName = expr.variable.simpleName

                // 1. Tenta como variável local
                val localVar = contextMethod?.getElements<CtLocalVariable<*>>{it is CtLocalVariable<*>}
                    ?.filterIsInstance<CtLocalVariable<*>>()
                    ?.find { it.simpleName == varName }

                if (localVar != null) {
                    val defaultExpr = localVar.defaultExpression
                    if (defaultExpr != null) {
                        val resolved = resolveExpression(defaultExpr, contextMethod, model)
                        if (!UnresolvedMarkers.isUnresolved(resolved)) {
                            return resolved
                        }
                    }
                    
                    // Check for UriComponentsBuilder pattern: URI resourceURI = uriBuilder.build().toUri();
                    // In this case, we need to find the uriBuilder variable and trace its chain
                    if (defaultExpr is CtInvocation<*>) {
                        val methodName = defaultExpr.executable.simpleName
                        if (methodName in listOf("toUri", "build", "toUriString")) {
                            // Try to resolve the UriComponentsBuilder chain
                            val uriResult = tryResolveUriComponentsBuilder(defaultExpr, contextMethod, model)
                            if (uriResult != null && !UnresolvedMarkers.isUnresolved(uriResult)) {
                                return uriResult
                            }
                        }
                    }
                }

                // 2. Tenta resolver como parâmetro (mesmo que nome seja diferente)
                if (depth < 5) {
                    contextMethod?.parameters?.forEachIndexed { index, param ->
                        // Se o nome da variável lida bate com o nome do parâmetro
                        if (param.simpleName == varName) {
                            // Busca chamadas ao método atual
                            val calls = model.getElements<CtInvocation<*>>{it is CtInvocation<*>}
                                .filter { it.executable.simpleName == contextMethod.simpleName && it.arguments.size > index }

                            for (call in calls) {
                                val callerMethod = call.getParent(CtMethod::class.java)
                                val argExpr = call.arguments[index]
                                val resolved = resolveExpression(argExpr, callerMethod, model, null, depth + 1)
                                // Accept paths that start with / even if they contain {dynamic}
                                // This allows partially resolved paths like /users/{dynamic}
                                if (!UnresolvedMarkers.isUnresolved(resolved) || 
                                    (resolved.startsWith("/") && resolved.contains("{dynamic}"))) {
                                    return resolved
                                }
                            }
                        }
                    }

                    // 3. Tenta resolver por casamento indireto (mesmo se nomes não batem)
                    val indirectMatch = contextMethod?.parameters?.mapIndexedNotNull { index, _ ->
                        val calls = model.getElements<CtInvocation<*>>{it is CtInvocation<*>}
                            .filter { it.executable.simpleName == contextMethod.simpleName && it.arguments.size > index }

                        for (call in calls) {
                            val argExpr = call.arguments[index]
                            if (argExpr is CtVariableRead<*> && argExpr.variable.simpleName == varName) {
                                val callerMethod = call.getParent(CtMethod::class.java)
                                return@mapIndexedNotNull resolveExpression(argExpr, callerMethod, model, null, depth + 1)
                            }
                        }
                        null
                    }?.firstOrNull()

                    if (indirectMatch != null) return indirectMatch
                }

                return UnresolvedMarkers.UNRESOLVED_VARIABLE
            }


            is CtLocalVariableReference<*> -> {
                val method: CtMethod<*> = contextMethod ?: expr.getParent(CtMethod::class.java)
                val localVar = method.body?.statements
                    ?.filterIsInstance<CtLocalVariable<*>>()
                    ?.firstOrNull { it.simpleName == expr.simpleName }

                val defaultExpr = localVar?.defaultExpression
                return defaultExpr?.let { resolveExpression(it, method, model) } ?: "{${expr.simpleName}}"
            }

            is CtInvocation<*> -> {
                val execRef = expr.executable
                val resolvedArgs = expr.arguments.map { resolveExpression(it, contextMethod, model, context) }

                // Check for enum getValue() pattern: PathSegmentsEnum.API.getValue() -> "api"
                val enumValue = tryResolveEnumGetValue(expr, model)
                if (enumValue != null) {
                    return enumValue
                }

                // Check for UriComponentsBuilder pattern
                val uriBuilderResult = tryResolveUriComponentsBuilder(expr, contextMethod, model)
                if (uriBuilderResult != null) {
                    return uriBuilderResult
                }
                
                // Check for getter on parameter object: param.getBaseURL(), param.getTokenUrl(), etc.
                // Try to trace back to the caller and find the actual value
                val target = expr.target
                if (target is CtVariableRead<*> && depth < 5) {
                    val paramResult = tryResolveMethodOnParameter(target, expr, contextMethod, model, depth)
                    if (paramResult != null && !UnresolvedMarkers.isUnresolved(paramResult)) {
                        return paramResult
                    }
                }

                // Tratamento genérico para funções tipo format
                val firstArg = expr.arguments.firstOrNull()
                if (firstArg is CtLiteral<*> && firstArg.value is String) {
                    val formatString = firstArg.value as String
                    return try {
                        // Remove o primeiro argumento (formato) e aplica String.format
                        String.format(formatString, *resolvedArgs.drop(1).toTypedArray())
                    } catch (e: Exception) {
                        // Se der erro, apenas concatena como fallback
                        resolvedArgs.joinToString("")
                    }
                }

                // Tentativa de resolução por retorno do método
                val calledMethod = model.getElements<CtMethod<*>>{it -> it is CtMethod<*>}
                    .filterIsInstance<CtMethod<*>>()
                    .firstOrNull { it.simpleName == execRef.simpleName && it.parameters.size == execRef.parameters.size }

                if (calledMethod != null) {
                    val returnStmt = calledMethod.getElements<CtReturn<*>>(CtReturn::class.java as Filter<CtReturn<*>?>?)
                        .filterIsInstance<CtReturn<*>>()
                        .firstOrNull()

                    if (returnStmt != null) {
                        return resolveExpression(returnStmt.returnedExpression, calledMethod, model, context)
                    }
                }

                return resolvedArgs.joinToString("")
            }

            else -> UnresolvedMarkers.UNRESOLVED_EXPRESSION
        }
    }

    /**
     * Try to resolve UriComponentsBuilder pattern.
     * Handles patterns like:
     *   UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/users").toUriString()
     *   UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/users").buildAndExpand(...).toUriString()
     *
     * @return The resolved path if this is a UriComponentsBuilder chain, null otherwise
     */
    private fun tryResolveUriComponentsBuilder(
        expr: CtInvocation<*>,
        contextMethod: CtMethod<*>?,
        model: CtModel
    ): String? {
        val methodName = expr.executable.simpleName
        
        // Check if this is a terminal method of UriComponentsBuilder chain
        if (methodName !in listOf("toUriString", "toString", "toUri", "build", "encode")) {
            return null
        }
        
        // Walk up the invocation chain to find path() and fromHttpUrl() calls
        val chainInfo = extractUriBuilderChain(expr, contextMethod, model)
        
        if (chainInfo.pathSegments.isEmpty() && chainInfo.baseUrl == null) {
            return null
        }
        
        // Build the path from segments
        val segmentsPath = chainInfo.pathSegments.joinToString("")
        
        // If we have path segments, prioritize them (ignore dynamic base URL)
        if (segmentsPath.isNotEmpty()) {
            // If the only content is {dynamic}, it's not useful - return null
            val cleanedPath = segmentsPath.replace("/", "").replace("{dynamic}", "")
            if (cleanedPath.isBlank()) {
                return null // All dynamic, no static paths found
            }
            
            // Check if baseUrl has a static path component we should include
            val baseUrlPath = chainInfo.baseUrl?.let { 
                if (!UnresolvedMarkers.isUnresolved(it)) {
                    extractPathFromUrl(it) 
                } else {
                    "" // Ignore dynamic base URLs
                }
            } ?: ""
            
            val fullPath = if (baseUrlPath.isNotEmpty() && baseUrlPath != "/") {
                baseUrlPath + segmentsPath
            } else {
                segmentsPath
            }
            
            // Ensure path starts with /
            return if (fullPath.startsWith("/")) fullPath else "/$fullPath"
        }
        
        // No segments, try to extract path from base URL
        return chainInfo.baseUrl?.let { 
            if (!UnresolvedMarkers.isUnresolved(it)) {
                extractPathFromUrl(it)
            } else {
                null // Can't resolve - base URL is dynamic with no path segments
            }
        }
    }
    
    /**
     * Data class to hold extracted information from UriComponentsBuilder chain
     */
    private data class UriBuilderChainInfo(
        val baseUrl: String? = null,
        val pathSegments: List<String> = emptyList()
    )
    
    /**
     * Extract information from UriComponentsBuilder method chain.
     * Walks backwards through the chain to collect path() and fromHttpUrl() values.
     * Also handles separate statement modifications like:
     *   UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
     *   uriBuilder.pathSegment("api").pathSegment("users");
     *   uri = uriBuilder.build().toUri();
     */
    private fun extractUriBuilderChain(
        expr: CtInvocation<*>,
        contextMethod: CtMethod<*>?,
        model: CtModel
    ): UriBuilderChainInfo {
        var baseUrl: String? = null
        val pathSegments = mutableListOf<String>()
        
        var current: CtExpression<*>? = expr
        var builderVarName: String? = null
        
        while (current is CtInvocation<*>) {
            val invocation = current
            val methodName = invocation.executable.simpleName
            
            when (methodName) {
                "path" -> {
                    val pathArg = invocation.arguments.firstOrNull()
                    if (pathArg != null) {
                        val resolvedPath = resolveExpression(pathArg, contextMethod, model)
                        if (!UnresolvedMarkers.isUnresolved(resolvedPath)) {
                            pathSegments.add(0, resolvedPath)
                        }
                    }
                }
                "pathSegment" -> {
                    val segments = invocation.arguments.map { arg ->
                        val resolved = resolveExpression(arg, contextMethod, model)
                        if (!UnresolvedMarkers.isUnresolved(resolved) && resolved.isNotBlank()) {
                            resolved
                        } else {
                            "{dynamic}"
                        }
                    }
                    if (segments.isNotEmpty()) {
                        val combinedPath = "/" + segments.joinToString("/")
                        pathSegments.add(0, combinedPath)
                    }
                }
                "fromHttpUrl", "fromUriString", "fromPath" -> {
                    val urlArg = invocation.arguments.firstOrNull()
                    if (urlArg != null) {
                        baseUrl = resolveExpression(urlArg, contextMethod, model)
                    }
                }
            }
            
            // Check if target is a variable read (like uriBuilder.build())
            val target = invocation.target
            if (target is CtVariableRead<*> && builderVarName == null) {
                builderVarName = target.variable.simpleName
            }
            
            current = target
        }
        
        // If we found a builder variable, look for additional pathSegment calls on it
        if (builderVarName != null && contextMethod != null) {
            val additionalPaths = findPathSegmentCallsOnVariable(builderVarName, contextMethod, model)
            pathSegments.addAll(0, additionalPaths)
            
            // Also find the baseUrl from variable initialization if not found
            if (baseUrl == null) {
                baseUrl = findBuilderInitialization(builderVarName, contextMethod, model)
            }
        }
        
        return UriBuilderChainInfo(baseUrl, pathSegments)
    }
    
    /**
     * Find pathSegment() calls on a variable throughout the method.
     * Handles: uriBuilder.pathSegment("api").pathSegment("search").pathSegment("order");
     */
    private fun findPathSegmentCallsOnVariable(
        varName: String,
        contextMethod: CtMethod<*>,
        model: CtModel
    ): List<String> {
        val pathSegments = mutableListOf<String>()
        
        // Find all invocations in the method body
        val invocations = contextMethod.body?.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            ?.filterIsInstance<CtInvocation<*>>()
            ?: return pathSegments
        
        for (invocation in invocations) {
            // Check if this is a pathSegment call on our variable
            if (invocation.executable.simpleName == "pathSegment") {
                // Walk up the chain to see if it starts with our variable
                var current: CtExpression<*>? = invocation
                var foundVar = false
                
                while (current is CtInvocation<*>) {
                    val target = current.target
                    if (target is CtVariableRead<*> && target.variable.simpleName == varName) {
                        foundVar = true
                        break
                    }
                    current = target
                }
                
                if (foundVar) {
                    // Extract path segments from this chain
                    val segments = extractPathSegmentsFromChain(invocation, model)
                    pathSegments.addAll(segments)
                }
            }
        }
        
        return pathSegments.distinct()
    }
    
    /**
     * Extract pathSegment values from a method chain.
     */
    private fun extractPathSegmentsFromChain(
        expr: CtInvocation<*>,
        model: CtModel
    ): List<String> {
        val segments = mutableListOf<String>()
        var current: CtExpression<*>? = expr
        
        while (current is CtInvocation<*>) {
            if (current.executable.simpleName == "pathSegment") {
                val resolvedSegments = current.arguments.map { arg ->
                    val resolved = resolveExpression(arg, null, model)
                    if (!UnresolvedMarkers.isUnresolved(resolved) && resolved.isNotBlank()) {
                        resolved
                    } else {
                        "{dynamic}"
                    }
                }
                if (resolvedSegments.isNotEmpty()) {
                    segments.add(0, "/" + resolvedSegments.joinToString("/"))
                }
            }
            current = current.target
        }
        
        return segments
    }
    
    /**
     * Find the initialization of a UriComponentsBuilder variable.
     */
    private fun findBuilderInitialization(
        varName: String,
        contextMethod: CtMethod<*>,
        model: CtModel
    ): String? {
        val localVar = contextMethod.body?.getElements<CtLocalVariable<*>> { it is CtLocalVariable<*> }
            ?.filterIsInstance<CtLocalVariable<*>>()
            ?.find { it.simpleName == varName }
        
        val defaultExpr = localVar?.defaultExpression
        if (defaultExpr is CtInvocation<*>) {
            val methodName = defaultExpr.executable.simpleName
            if (methodName in listOf("fromHttpUrl", "fromUriString", "fromPath")) {
                val urlArg = defaultExpr.arguments.firstOrNull()
                if (urlArg != null) {
                    return resolveExpression(urlArg, contextMethod, model)
                }
            }
        }
        
        return null
    }
    
    /**
     * Try to resolve enum getValue() pattern.
     * Handles: PathSegmentsEnum.API.getValue() -> "api"
     * 
     * Works by:
     * 1. Finding the enum constant and extracting its constructor argument (if enum is in model)
     * 2. Fallback: converting enum name to lowercase with _/- conventions
     */
    private fun tryResolveEnumGetValue(expr: CtInvocation<*>, model: CtModel): String? {
        val methodName = expr.executable.simpleName
        
        // Check if it's a getValue() or similar accessor method
        if (methodName !in listOf("getValue", "value", "getCode", "getPath", "getName")) {
            return null
        }
        
        // Check if target is a field read (enum constant access)
        val target = expr.target
        if (target !is CtFieldRead<*>) {
            return null
        }
        
        val enumConstantName = target.variable.simpleName
        val enumTypeName = target.variable.declaringType?.qualifiedName
        
        // Try to find the enum type in the model
        val enumType = if (enumTypeName != null) {
            model.allTypes.find { it.qualifiedName == enumTypeName && it.isEnum }
        } else null
        
        if (enumType != null) {
            // Find the enum constant
            val enumConstant = enumType.fields.find { it.simpleName == enumConstantName }
            
            if (enumConstant != null) {
                // Get the default expression (constructor call) of the enum constant
                val defaultExpr = enumConstant.defaultExpression
                
                // For enums like: API("api"), the defaultExpression is a constructor call
                if (defaultExpr is CtInvocation<*>) {
                    val firstArg = defaultExpr.arguments.firstOrNull()
                    if (firstArg is CtLiteral<*>) {
                        return firstArg.value?.toString()
                    }
                }
            }
        }
        
        // Fallback: Convert enum constant name to likely value
        // API -> "api", ORDER_ADDRESS -> "order-address", LINE_ITEMS -> "line-items"
        return enumConstantNameToValue(enumConstantName)
    }
    
    /**
     * Convert enum constant name to its likely string value.
     * Examples:
     *   API -> "api"
     *   ORDER_ADDRESS -> "order-address"  
     *   LINE_ITEMS -> "line-items"
     */
    private fun enumConstantNameToValue(name: String): String {
        return name.lowercase().replace("_", "-")
    }
    
    /**
     * Try to resolve a method call on a parameter object by tracing back to caller.
     * Handles patterns like: param.getBaseURL() where param is passed from caller.
     * 
     * Example flow:
     * 1. In RestService.performGet(GetRequest getRequest): getRequest.getBaseURL()
     * 2. Caller: restService.performGet(myRequest) where myRequest.baseURL = CONSTANT
     * 3. We trace: getRequest -> myRequest -> resolve myRequest.getBaseURL()
     */
    private fun tryResolveMethodOnParameter(
        target: CtVariableRead<*>,
        methodCall: CtInvocation<*>,
        contextMethod: CtMethod<*>?,
        model: CtModel,
        depth: Int
    ): String? {
        if (contextMethod == null) return null
        
        val varName = target.variable.simpleName
        val getterName = methodCall.executable.simpleName
        
        // Check if varName is a parameter of contextMethod
        val paramIndex = contextMethod.parameters.indexOfFirst { it.simpleName == varName }
        if (paramIndex < 0) return null
        
        // Find callers of this method and get the argument at paramIndex
        val callers = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filter { 
                it.executable.simpleName == contextMethod.simpleName && 
                it.arguments.size > paramIndex 
            }
        
        for (caller in callers) {
            val argExpr = caller.arguments[paramIndex]
            val callerMethod = caller.getParent(CtMethod::class.java)
            
            // Now we need to find what getterName returns on this object
            // If argExpr is a builder pattern like GetRequest.builder().baseURL(X).build()
            // we need to find the value of X
            val builderValue = tryResolveBuilderValue(argExpr, getterName, callerMethod, model, depth + 1)
            if (builderValue != null && !UnresolvedMarkers.isUnresolved(builderValue)) {
                return builderValue
            }
        }
        
        return null
    }
    
    /**
     * Try to resolve a value from a builder pattern.
     * Handles: SomeClass.builder().fieldName(VALUE).build() -> returns VALUE for getFieldName()
     */
    private fun tryResolveBuilderValue(
        expr: CtExpression<*>?,
        getterName: String,
        contextMethod: CtMethod<*>?,
        model: CtModel,
        depth: Int
    ): String? {
        if (expr == null) return null
        
        // Convert getter name to setter/builder method name
        // getBaseURL -> baseURL, getTokenUrl -> tokenUrl
        val fieldName = if (getterName.startsWith("get")) {
            getterName.removePrefix("get").replaceFirstChar { it.lowercase() }
        } else {
            getterName
        }
        
        // Walk the builder chain to find the setter with this name
        return findBuilderSetterValue(expr, fieldName, contextMethod, model, depth)
    }
    
    /**
     * Find the value passed to a builder setter method.
     * Walks a builder chain like: builder().foo(X).bar(Y).build() 
     * to find the value of foo or bar.
     */
    private fun findBuilderSetterValue(
        expr: CtExpression<*>?,
        fieldName: String,
        contextMethod: CtMethod<*>?,
        model: CtModel,
        depth: Int
    ): String? {
        if (depth > 5) return null
        
        var current: CtExpression<*>? = expr
        
        while (current is CtInvocation<*>) {
            val methodName = current.executable.simpleName
            
            // Check if this is the setter we're looking for
            if (methodName.equals(fieldName, ignoreCase = true)) {
                val arg = current.arguments.firstOrNull()
                if (arg != null) {
                    return resolveExpression(arg, contextMethod, model, null, depth + 1)
                }
            }
            
            // Move to target (the expression this method was called on)
            current = current.target
        }
        
        // Also check if expr is a variable that was assigned a builder result
        if (expr is CtVariableRead<*>) {
            val varName = expr.variable.simpleName
            val localVar = contextMethod?.getElements<CtLocalVariable<*>> { it is CtLocalVariable<*> }
                ?.filterIsInstance<CtLocalVariable<*>>()
                ?.find { it.simpleName == varName }
            
            if (localVar?.defaultExpression != null) {
                return findBuilderSetterValue(localVar.defaultExpression, fieldName, contextMethod, model, depth + 1)
            }
        }
        
        return null
    }

    /**
     * Extract the path portion from a URL string.
     * Example: "https://api.example.com/v1/users" -> "/v1/users"
     *          "/users" -> "/users"
     */
    private fun extractPathFromUrl(url: String): String {
        // If it's already just a path, return it
        if (url.startsWith("/")) {
            return url
        }
        
        // Try to parse as URL and extract path
        return try {
            val uri = java.net.URI(url)
            uri.path ?: url
        } catch (e: Exception) {
            // If parsing fails, try simple regex extraction
            val pathMatch = Regex("""https?://[^/]+(/[^\s]*)""").find(url)
            pathMatch?.groupValues?.get(1) ?: url
        }
    }
}