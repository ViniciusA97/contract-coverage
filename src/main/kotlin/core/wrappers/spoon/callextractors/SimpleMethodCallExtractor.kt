package org.example.core.wrappers.spoon.callextractors

import org.example.core.entities.Endpoint
import org.example.core.utils.urlToPath
import org.example.core.wrappers.spoon.MethodCallContext
import org.example.core.wrappers.spoon.RestTemplateCallClassifier
import org.example.core.wrappers.spoon.SpoonExpressionResolver
import org.example.core.entities.HttpMethod
import spoon.reflect.CtModel
import spoon.reflect.code.CtInvocation
import spoon.reflect.declaration.CtMethod

class SimpleMethodCallExtractor(
    private val classifierPredicate: (RestTemplateCallClassifier, CtInvocation<*>) -> Boolean,
    private val httpMethod: HttpMethod,
    private val classifier: RestTemplateCallClassifier,
    private val resolver: SpoonExpressionResolver,
) : CallExtractor {
    override fun supports(call: CtInvocation<*>): Boolean = classifierPredicate(classifier, call)

    override fun extract(call: CtInvocation<*>, context: MethodCallContext, model: CtModel): Endpoint {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments
        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            resolver.resolveExpressionWithParams(arg, parameters, callArgs, scopeMethod, model)
        } ?: "{unknown}"

        return Endpoint(urlToPath(resolvedUrl), httpMethod)
    }
}


