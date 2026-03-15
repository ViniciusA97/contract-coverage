package org.example.core.wrappers.spoon.callextractors

import org.example.core.entities.Endpoint
import org.example.core.entities.UnresolvedMarkers
import org.example.core.utils.urlToPath
import org.example.core.wrappers.spoon.MethodCallContext
import org.example.core.wrappers.spoon.RestTemplateCallClassifier
import org.example.core.wrappers.spoon.RootUriDetector
import org.example.core.wrappers.spoon.SpoonExpressionResolver
import org.example.core.entities.HttpMethod
import spoon.reflect.CtModel
import spoon.reflect.code.CtInvocation
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtMethod

class SimpleMethodCallExtractor(
    private val classifierPredicate: (RestTemplateCallClassifier, CtInvocation<*>) -> Boolean,
    private val httpMethod: HttpMethod,
    private val classifier: RestTemplateCallClassifier,
    private val resolver: SpoonExpressionResolver,
    private val rootUriDetector: RootUriDetector? = null,
) : CallExtractor {
    override fun supports(call: CtInvocation<*>): Boolean = classifierPredicate(classifier, call)

    override fun extract(call: CtInvocation<*>, context: MethodCallContext, model: CtModel): Endpoint {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments
        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            resolver.resolveExpressionWithParams(arg, parameters, callArgs, scopeMethod, model)
        } ?: UnresolvedMarkers.UNRESOLVED_URL

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

        return Endpoint(finalPath, httpMethod)
    }
    
    /**
     * Combine path with rootUri if available.
     */
    private fun resolvePathWithRootUri(path: String, containingClass: CtClass<*>?, model: CtModel): String {
        if (rootUriDetector == null) return path
        
        return when (val result = rootUriDetector.resolveRootUri(containingClass, model)) {
            is RootUriDetector.RootUriResult.NotFound -> path
            is RootUriDetector.RootUriResult.Resolved -> {
                val rootPath = result.path.trimEnd('/')
                val endpointPath = if (path == "/") "" else path
                rootPath + endpointPath
            }
            is RootUriDetector.RootUriResult.Dynamic -> UnresolvedMarkers.UNRESOLVED_URL
        }
    }
}


