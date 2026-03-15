package org.example.core.wrappers.spoon

import org.example.core.entities.UnresolvedMarkers
import spoon.reflect.CtModel
import spoon.reflect.code.*
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtMethod

/**
 * Resolves rootUri configuration for RestTemplate.
 * 
 * Logic:
 * - If rootUri() is not used → return NotFound (use path as-is)
 * - If rootUri() is used and value is a literal → return Resolved with path
 * - If rootUri() is used but value is dynamic → return Dynamic
 */
class RootUriDetector(
    private val expressionResolver: SpoonExpressionResolver
) {
    
    /**
     * Result of rootUri resolution
     */
    sealed class RootUriResult {
        /** No rootUri configuration found */
        object NotFound : RootUriResult()
        
        /** rootUri found and resolved to a static value */
        data class Resolved(val path: String) : RootUriResult()
        
        /** rootUri found but value is dynamic/unresolvable */
        object Dynamic : RootUriResult()
    }
    
    /**
     * Try to resolve rootUri for the class containing the RestTemplate call.
     */
    fun resolveRootUri(containingClass: CtClass<*>?, model: CtModel): RootUriResult {
        if (containingClass == null) return RootUriResult.NotFound
        
        try {
            // Strategy 1: Check Factory classes that create this client
            val factoryResult = checkFactoryClasses(containingClass.simpleName, model)
            if (factoryResult != RootUriResult.NotFound) return factoryResult
            
            // Strategy 2: Check if RestTemplate is created within the class itself
            val localResult = checkLocalRestTemplateCreation(containingClass, model)
            if (localResult != RootUriResult.NotFound) return localResult
        } catch (e: Exception) {
            // If any error occurs during analysis, assume dynamic
            return RootUriResult.NotFound
        }
        
        return RootUriResult.NotFound
    }
    
    /**
     * Check Factory classes that might create this client with rootUri
     */
    private fun checkFactoryClasses(clientClassName: String, model: CtModel): RootUriResult {
        val allClasses = model.allTypes.filterIsInstance<CtClass<*>>()
        
        val factoryClasses = allClasses.filter { 
            it.simpleName.endsWith("Factory") || 
            it.simpleName.endsWith("Builder") ||
            it.simpleName.contains(clientClassName.removeSuffix("Client"))
        }
        
        for (factory in factoryClasses) {
            for (method in factory.methods) {
                val result = tryResolveRootUriInMethod(method, model)
                if (result != RootUriResult.NotFound) return result
            }
        }
        
        return RootUriResult.NotFound
    }
    
    /**
     * Check if RestTemplate is created locally with rootUri
     */
    private fun checkLocalRestTemplateCreation(ctClass: CtClass<*>, model: CtModel): RootUriResult {
        for (method in ctClass.methods) {
            val result = tryResolveRootUriInMethod(method, model)
            if (result != RootUriResult.NotFound) return result
        }
        
        return RootUriResult.NotFound
    }
    
    /**
     * Try to resolve rootUri in a specific method
     */
    private fun tryResolveRootUriInMethod(method: CtMethod<*>, model: CtModel): RootUriResult {
        val rootUriCall = findRootUriCall(method) ?: return RootUriResult.NotFound
        
        val arg = rootUriCall.arguments.firstOrNull() ?: return RootUriResult.Dynamic
        
        // If argument is a literal string, we can resolve it
        if (arg is CtLiteral<*> && arg.value is String) {
            val url = arg.value as String
            val path = extractPathFromUrl(url)
            return if (path.isNotEmpty()) {
                RootUriResult.Resolved(path)
            } else {
                RootUriResult.Resolved(url)
            }
        }
        
        // Any other type of argument (method call, variable, etc.) is considered dynamic
        return RootUriResult.Dynamic
    }
    
    /**
     * Find a rootUri() call in a method by scanning its body
     */
    private fun findRootUriCall(method: CtMethod<*>): CtInvocation<*>? {
        val body = method.body ?: return null
        return findRootUriInElement(body)
    }
    
    /**
     * Recursively find rootUri invocation in an element
     */
    private fun findRootUriInElement(element: CtElement): CtInvocation<*>? {
        if (element is CtInvocation<*> && element.executable.simpleName == "rootUri") {
            return element
        }
        
        for (child in element.directChildren) {
            val found = findRootUriInElement(child)
            if (found != null) return found
        }
        
        return null
    }
    
    /**
     * Extract path from URL (e.g., "https://api.com/v1" -> "/v1")
     */
    private fun extractPathFromUrl(url: String): String {
        if (url.startsWith("/")) return url
        
        return try {
            val uri = java.net.URI(url)
            uri.path ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
