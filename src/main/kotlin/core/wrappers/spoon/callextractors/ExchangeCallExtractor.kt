package org.example.core.wrappers.spoon.callextractors

import org.example.core.entities.Endpoint
import org.example.core.entities.UnresolvedMarkers
import org.example.core.utils.urlToPath
import org.example.core.wrappers.spoon.MethodCallContext
import org.example.core.wrappers.spoon.RestTemplateCallClassifier
import org.example.core.wrappers.spoon.RootUriDetector
import org.example.core.wrappers.spoon.SpoonExpressionResolver
import org.example.core.entities.HttpMethod
import org.example.core.wrappers.spoon.resolveSourceFile
import spoon.reflect.CtModel
import spoon.reflect.code.CtInvocation
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtMethod

class ExchangeCallExtractor(
    private val classifier: RestTemplateCallClassifier,
    private val resolver: SpoonExpressionResolver,
    private val rootUriDetector: RootUriDetector? = null,
): CallExtractor {
    override fun supports(call: CtInvocation<*>): Boolean = classifier.isRestTemplateExchange(call)

    override fun extract(call: CtInvocation<*>, context: MethodCallContext, model: CtModel): Endpoint {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            resolver.resolveExpressionWithParams(arg, parameters, callArgs, scopeMethod, model)
        } ?: UnresolvedMarkers.UNRESOLVED_URL

        val resolvedMethodStr = call.arguments.getOrNull(1)?.let { arg ->
            resolver.resolveExpressionWithParams(arg, parameters, callArgs, scopeMethod, model)
        } ?: UnresolvedMarkers.UNRESOLVED_EXPRESSION
        
        val httpMethod = HttpMethod.fromStringOrDefault(resolvedMethodStr)
        
        var path = urlToPath(resolvedUrl)
        
        // Try to resolve rootUri and combine with path
        if (rootUriDetector != null && path == "/") {
            try {
                val containingClass = call.getParent(CtClass::class.java)
                path = resolvePathWithRootUri(path, containingClass, model)
            } catch (e: Exception) {
                // Ignore errors in rootUri detection
            }
        }
        
        val finalPath = if (UnresolvedMarkers.isPathUnresolved(path)) {
            UnresolvedMarkers.UNRESOLVED_URL
        } else {
            path
        }
        
        val sourceFile = resolveSourceFile(call)
        
        return Endpoint(finalPath, httpMethod, sourceFile)
    }
    
    /**
     * Combine path with rootUri if available.
     * 
     * Logic:
     * - If rootUri not found → use path as-is
     * - If rootUri resolved → combine rootUri + path
     * - If rootUri is dynamic → mark as unresolved
     */
    private fun resolvePathWithRootUri(path: String, containingClass: CtClass<*>?, model: CtModel): String {
        if (rootUriDetector == null) return path
        
        return when (val result = rootUriDetector.resolveRootUri(containingClass, model)) {
            is RootUriDetector.RootUriResult.NotFound -> path
            is RootUriDetector.RootUriResult.Resolved -> {
                // Combine rootUri path with endpoint path
                val rootPath = result.path.trimEnd('/')
                val endpointPath = if (path == "/") "" else path
                rootPath + endpointPath
            }
            is RootUriDetector.RootUriResult.Dynamic -> UnresolvedMarkers.UNRESOLVED_URL
        }
    }
}


