package org.example.core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.utils.urlToPath
import org.example.core.wrappers.StaticCodeAnalyzer
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.code.*
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtParameter
import spoon.reflect.visitor.Filter

data class MethodCallContext(
    val method: CtMethod<*>,
    val arguments: List<CtExpression<*>>
)

class SpoonWrapper(
    private val projectDir: String,
) : StaticCodeAnalyzer {

    private val launcher = initLauncher()
    private val spoonExpressionResolver = SpoonExpressionResolver()

    override fun analyzeInvocations(): List<Endpoint> {
        val model = launcher.model
        val endpoints = extractEndpoints(model)
        println(endpoints)
        return endpoints
    }

    fun extractEndpoints(model: CtModel): List<Endpoint> {
        val endpoints = mutableListOf<Endpoint>()

        // TODO: Load only RestTemplate calls
        val projectCalls = findCallsWithResolvedArgs(model)

        val exchangeCalls = getExchangeCallsFromProjectCalls(projectCalls)
        val getCalls = getHttpGetCallsFromProjectCalls(projectCalls)
        val postCalls = getHttpPostCallsFromProjectCalls(projectCalls)
        val putCalls = getHttpPutCallsFromProjectCalls(projectCalls)
        val patchCalls = getHttpPatchCallsFromProjectCalls(projectCalls)
        val deleteCalls = getHttpDeleteCallsFromProjectCalls(projectCalls)

        exchangeCalls.forEach { (call, context) ->
            val (url, method) = extractExchangeCalls(call, context, model)
            endpoints.add(Endpoint(url, method))
        }

        getCalls.forEach { (call, context) ->
            val (url, method) = extractGetCalls(call, context, model)
            endpoints.add(Endpoint(url, method))
        }

        postCalls.forEach { (call, context) ->
            val (url, method) = extractPostCalls(call, context, model)
            endpoints.add(Endpoint(url, method))
        }

        putCalls.forEach { (call, context) ->
            val (url, method) = extractPutCalls(call, context, model)
            endpoints.add(Endpoint(url, method))
        }

        patchCalls.forEach { (call, context) ->
            val (url, method) = extractPatchCalls(call, context, model)
            endpoints.add(Endpoint(url, method))
        }

        deleteCalls.forEach { (call, context) ->
            val (url, method) = extractDeleteCalls(call, context, model)
            endpoints.add(Endpoint(url, method))
        }

        return endpoints
    }

    fun extractExchangeCalls(call: CtInvocation<*>,
                             context: MethodCallContext,
                             model: CtModel
    ): Pair<String, String> {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            spoonExpressionResolver.resolveExpressionWithParams(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model)
        } ?: "{unknown}"

        val resolvedMethod = call.arguments.getOrNull(1)?.let { arg ->
            spoonExpressionResolver.resolveExpressionWithParams(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model)
        } ?: "{unknown}"

        return urlToPath(resolvedUrl) to resolvedMethod
    }

    fun extractGetCalls(call: CtInvocation<*>,
                             context: MethodCallContext,
                             model: CtModel
    ): Pair<String, String> {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            spoonExpressionResolver.resolveExpressionWithParams(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model)
        } ?: "{unknown}"

        val resolvedMethod = "GET"

        return urlToPath(resolvedUrl) to resolvedMethod
    }

    fun extractPostCalls(call: CtInvocation<*>,
                        context: MethodCallContext,
                        model: CtModel
    ): Pair<String, String> {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            spoonExpressionResolver.resolveExpressionWithParams(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model)
        } ?: "{unknown}"

        val resolvedMethod = "POST"

        return urlToPath(resolvedUrl) to resolvedMethod
    }

    fun extractPatchCalls(call: CtInvocation<*>,
                         context: MethodCallContext,
                         model: CtModel
    ): Pair<String, String> {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            spoonExpressionResolver.resolveExpressionWithParams(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model)
        } ?: "{unknown}"

        val resolvedMethod = "PATCH"

        return urlToPath(resolvedUrl) to resolvedMethod
    }

    fun extractDeleteCalls(call: CtInvocation<*>,
                         context: MethodCallContext,
                         model: CtModel
    ): Pair<String, String> {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            spoonExpressionResolver.resolveExpressionWithParams(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model)
        } ?: "{unknown}"

        val resolvedMethod = "DELETE"

        return urlToPath(resolvedUrl) to resolvedMethod
    }

    fun extractPutCalls(call: CtInvocation<*>,
                         context: MethodCallContext,
                         model: CtModel
    ): Pair<String, String> {
        val method = context.method
        val parameters = method.parameters
        val callArgs = context.arguments

        val scopeMethod = call.getParent(CtMethod::class.java)

        val resolvedUrl = call.arguments.getOrNull(0)?.let { arg ->
            spoonExpressionResolver.resolveExpressionWithParams(
                expr = arg, params = parameters, args = callArgs, scopeMethod = scopeMethod, model = model)
        } ?: "{unknown}"

        val resolvedMethod = "PUT"

        return urlToPath(resolvedUrl) to resolvedMethod
    }


    fun findCallsWithResolvedArgs(model: CtModel): List<Pair<CtInvocation<*>, MethodCallContext>> {
        val result = mutableListOf<Pair<CtInvocation<*>, MethodCallContext>>()

        val methodInvocations = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filterIsInstance<CtInvocation<*>>()

        methodInvocations.forEach { topLevelCall ->
            val declaration = topLevelCall.executable.declaration as? CtMethod<*> ?: return@forEach
            val context = MethodCallContext(declaration, topLevelCall.arguments)

            val innerCalls = declaration.body?.getElements<CtInvocation<*>> { it is CtInvocation<*> }
                ?.filterIsInstance<CtInvocation<*>>() ?: return@forEach

            innerCalls.forEach { exchangeCall ->
                result.add(exchangeCall to context)
            }
        }

        return result
    }

    fun getExchangeCallsFromProjectCalls(projectCalls: List<Pair<CtInvocation<*>, MethodCallContext>>): List<Pair<CtInvocation<*>, MethodCallContext>> {
        return projectCalls.filter { (call, _) -> isRestTemplateExchange(call) }
    }

    fun getHttpGetCallsFromProjectCalls(projectCalls: List<Pair<CtInvocation<*>, MethodCallContext>>): List<Pair<CtInvocation<*>, MethodCallContext>> {
        return projectCalls.filter { (call, _) -> isRestTemplateGetForEntity(call) }
    }

    fun getHttpPostCallsFromProjectCalls(projectCalls: List<Pair<CtInvocation<*>, MethodCallContext>>): List<Pair<CtInvocation<*>, MethodCallContext>> {
        return projectCalls.filter { (call, _) -> isRestTemplateGetForEntity(call) }
    }

    fun getHttpPutCallsFromProjectCalls(projectCalls: List<Pair<CtInvocation<*>, MethodCallContext>>): List<Pair<CtInvocation<*>, MethodCallContext>> {
        return projectCalls.filter { (call, _) -> isRestTemplateGetForEntity(call) }
    }

    fun getHttpPatchCallsFromProjectCalls(projectCalls: List<Pair<CtInvocation<*>, MethodCallContext>>): List<Pair<CtInvocation<*>, MethodCallContext>> {
        return projectCalls.filter { (call, _) -> isRestTemplateGetForEntity(call) }
    }

    fun getHttpDeleteCallsFromProjectCalls(projectCalls: List<Pair<CtInvocation<*>, MethodCallContext>>): List<Pair<CtInvocation<*>, MethodCallContext>> {
        return projectCalls.filter { (call, _) -> isRestTemplateGetForEntity(call) }
    }

    fun isRestTemplateExchange(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "exchange" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplateGetForEntity(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "getForEntity" &&
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

            val resolvedExpression = spoonExpressionResolver.resolveExpression(
                expr = passedArg,
                contextMethod = callerMethod,
                context = call,
                model = model
            )

            if (!resolvedExpression.contains("não resolvida")) {
                return resolvedExpression
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
                    return spoonExpressionResolver.resolveExpression(
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