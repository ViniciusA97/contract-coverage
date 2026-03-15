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

    fun resolveExpressionWithParams(
        expr: CtExpression<*>,
        params: List<CtParameter<*>>,
        args: List<CtExpression<*>>,
        scopeMethod: CtMethod<*>?,
        model: CtModel
    ): String {
        return when (expr) {
            is CtVariableRead<*> -> {
                // Substitui parâmetro se for encontrado
                val index = params.indexOfFirst { it.simpleName == expr.variable.simpleName }
                if (index >= 0 && index < args.size) {
                    val argExpr = args[index]
                    // Use the caller's method as context, not the current scope
                    // The argument expression belongs to the caller method
                    val callerMethod = argExpr.getParent(CtMethod::class.java)
                    resolveExpression(argExpr, callerMethod ?: scopeMethod, model)
                } else {
                    resolveExpression(expr, scopeMethod, model)
                }
            }
            else -> resolveExpression(expr, scopeMethod, model)
        }
    }

    fun resolveExpression(expr: CtExpression<*>?, contextMethod: CtMethod<*>?, model: CtModel, context: CtElement? = null,): String {
        if (expr == null) return "null expression"

        return when (expr) {
            is CtLiteral<*> -> expr.value.toString()

            is CtBinaryOperator<*> -> {
                val left = resolveExpression(expr.leftHandOperand, contextMethod, model)
                val right = resolveExpression(expr.rightHandOperand, contextMethod, model)
                return left + right
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

                val typeName = fieldDecl?.type?.simpleName
                return if (typeName != null) {
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
                    return resolveExpression(localVar.defaultExpression, contextMethod, model)
                }

                // 2. Tenta resolver como parâmetro (mesmo que nome seja diferente)
                contextMethod?.parameters?.forEachIndexed { index, param ->
                    // Se o nome da variável lida bate com o nome do parâmetro
                    if (param.simpleName == varName) {
                        // Busca chamadas ao método atual
                        val calls = model.getElements<CtInvocation<*>>{it is CtInvocation<*>}
                            .filter { it.executable.simpleName == contextMethod.simpleName && it.arguments.size > index }

                        for (call in calls) {
                            val callerMethod = call.getParent(CtMethod::class.java)
                            val argExpr = call.arguments[index]
                            val resolved = resolveExpression(argExpr, callerMethod, model)
                            if (!UnresolvedMarkers.isUnresolved(resolved)) return resolved
                        }
                    }
                }

                // 3. Tenta resolver por casamento indireto (mesmo se nomes não batem)
                val indirectMatch = contextMethod?.parameters?.mapIndexedNotNull { index, param ->
                    val calls = model.getElements<CtInvocation<*>>{it is CtInvocation<*>}
                        .filter { it.executable.simpleName == contextMethod.simpleName && it.arguments.size > index }

                    for (call in calls) {
                        val argExpr = call.arguments[index]
                        if (argExpr is CtVariableRead<*> && argExpr.variable.simpleName == varName) {
                            val callerMethod = call.getParent(CtMethod::class.java)
                            return@mapIndexedNotNull resolveExpression(argExpr, callerMethod, model)
                        }
                    }
                    null
                }?.firstOrNull()

                if (indirectMatch != null) return indirectMatch

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

                // Check for UriComponentsBuilder pattern
                val uriBuilderResult = tryResolveUriComponentsBuilder(expr, contextMethod, model)
                if (uriBuilderResult != null) {
                    return uriBuilderResult
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
        
        // Extract path from base URL if present (e.g., https://api.example.com/v0.1 -> /v0.1)
        val baseUrlPath = chainInfo.baseUrl?.let { extractPathFromUrl(it) } ?: ""
        
        // Build the final path from base URL path + segments
        val segmentsPath = chainInfo.pathSegments.joinToString("")
        val fullPath = if (baseUrlPath.isNotEmpty() && baseUrlPath != "/") {
            baseUrlPath + segmentsPath
        } else {
            segmentsPath
        }
        
        // Return the full path (already extracted from URL)
        return if (fullPath.isNotEmpty()) fullPath else extractPathFromUrl(segmentsPath)
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
     */
    private fun extractUriBuilderChain(
        expr: CtInvocation<*>,
        contextMethod: CtMethod<*>?,
        model: CtModel
    ): UriBuilderChainInfo {
        var baseUrl: String? = null
        val pathSegments = mutableListOf<String>()
        
        var current: CtExpression<*>? = expr
        
        while (current is CtInvocation<*>) {
            val invocation = current
            val methodName = invocation.executable.simpleName
            
            when (methodName) {
                "path" -> {
                    // Extract single path argument
                    val pathArg = invocation.arguments.firstOrNull()
                    if (pathArg != null) {
                        val resolvedPath = resolveExpression(pathArg, contextMethod, model)
                        if (!UnresolvedMarkers.isUnresolved(resolvedPath)) {
                            // Add at beginning since we're walking backwards
                            pathSegments.add(0, resolvedPath)
                        }
                    }
                }
                "pathSegment" -> {
                    // pathSegment can have multiple arguments: .pathSegment("a", "b", "c") -> /a/b/c
                    // Preserve unresolved segments as {dynamic} to maintain path structure
                    val segments = invocation.arguments.map { arg ->
                        val resolved = resolveExpression(arg, contextMethod, model)
                        if (!UnresolvedMarkers.isUnresolved(resolved) && resolved.isNotBlank()) {
                            resolved
                        } else {
                            "{dynamic}" // Placeholder for dynamic/unresolved segments
                        }
                    }
                    if (segments.isNotEmpty()) {
                        // Join segments with "/" and add at beginning
                        val combinedPath = "/" + segments.joinToString("/")
                        pathSegments.add(0, combinedPath)
                    }
                }
                "fromHttpUrl", "fromUriString", "fromPath" -> {
                    // Extract base URL
                    val urlArg = invocation.arguments.firstOrNull()
                    if (urlArg != null) {
                        baseUrl = resolveExpression(urlArg, contextMethod, model)
                    }
                }
                // Methods that don't affect the path: build, encode, buildAndExpand, toUriString, etc.
            }
            
            // Move to the target of this invocation (the object/expression the method was called on)
            current = invocation.target
        }
        
        return UriBuilderChainInfo(baseUrl, pathSegments)
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