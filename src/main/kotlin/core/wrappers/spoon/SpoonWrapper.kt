package org.example.core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.services.clients.ClientHelpers
import org.example.core.utils.urlToPath
import org.example.core.wrappers.StaticCodeAnalyzer
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.code.*
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtField
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtParameter
import spoon.reflect.reference.CtLocalVariableReference
import spoon.reflect.visitor.Filter

data class MethodCallContext(
    val method: CtMethod<*>,
    val arguments: List<CtExpression<*>>
)

class SpoonWrapper(
    private val projectDir: String,
) : StaticCodeAnalyzer {

    private val launcher = initLauncher()

    override fun analyzeInvocations(): List<Endpoint> {
        val model = launcher.model
        val endpoints = extractEndpoints(model)
        println(endpoints)
        return endpoints
    }

    fun extractEndpoints(model: CtModel): List<Endpoint> {
        val endpoints = mutableListOf<Endpoint>()

        val resolvedCalls = findExchangeCallsWithResolvedArgs(model)

        resolvedCalls.forEach { (call, context) ->
            val (url, method) = resolveArgumentsWithContext(call, context, model)
            endpoints.add(Endpoint(url, method))
        }

        return endpoints
    }


    fun findExchangeCallsWithResolvedArgs(model: CtModel): List<Pair<CtInvocation<*>, MethodCallContext>> {
        val result = mutableListOf<Pair<CtInvocation<*>, MethodCallContext>>()

        val methodInvocations = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filterIsInstance<CtInvocation<*>>()

        methodInvocations.forEach { topLevelCall ->
            val declaration = topLevelCall.executable.declaration as? CtMethod<*> ?: return@forEach
            val context = MethodCallContext(declaration, topLevelCall.arguments)

            val innerCalls = declaration.body?.getElements<CtInvocation<*>> { it is CtInvocation<*> }
                ?.filterIsInstance<CtInvocation<*>>() ?: return@forEach

            innerCalls.filter { isRestTemplateExchange(it) }.forEach { exchangeCall ->
                result.add(exchangeCall to context)
            }
        }

        return result
    }

    fun resolveArgumentsWithContext(
        call: CtInvocation<*>,
        context: MethodCallContext,
        model: CtModel
    ): Pair<String, String> {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            SpoonExpressionResolver(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model).resolve()
        } ?: "{unknown}"

        val resolvedMethod = call.arguments.getOrNull(1)?.let { arg ->
            SpoonExpressionResolver(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model).resolve()
        } ?: "{unknown}"

        return urlToPath(resolvedUrl) to resolvedMethod
    }

    fun isRestTemplateExchange(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "exchange" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    private fun findValueFromCaller(
        targetMethod: CtMethod<*>,
        param: CtParameter<*>,
        model: CtModel
    ): String {
        val paramIndex = targetMethod.parameters.indexOf(param)

        val calls = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filter {
                it.executable.simpleName == targetMethod.simpleName &&
                        it.arguments.size > paramIndex
            }

        for (call in calls) {
            val passedArg = call.arguments[paramIndex]
            val callerMethod = call.getParent(CtMethod::class.java)

            val resolved = SpoonExpressionResolver(
                expr = passedArg,
                contextMethod = callerMethod,
                context = call,
                model = model
            ).resolve()

            if (!resolved.contains("não resolvida")) {
                return resolved
            }

            if (passedArg is CtVariableRead<*>) {
                val varName = passedArg.variable.simpleName

                val nextParam = callerMethod?.parameters?.find { it.simpleName == varName }
                if (nextParam != null) {
                    return findValueFromCaller(callerMethod, nextParam, model)
                }

                val localVar = callerMethod?.getElements(CtLocalVariable::class.java as Filter<CtLocalVariable<*>>)
                    ?.find { it.simpleName == varName }

                if (localVar?.defaultExpression != null) {
                    return resolveExpression(
                        expr = localVar.defaultExpression,
                        contextMethod = callerMethod,
                        context = call,
                        model = model
                    )
                }
            }
        }

        return "<parametro: valor não resolvido>"
    }


    private fun initLauncher(): Launcher {
        val launcher = Launcher()
        launcher.addInputResource(projectDir)
        launcher.buildModel()
        return launcher
    }
}