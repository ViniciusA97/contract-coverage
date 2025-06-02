package org.example.core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.services.clients.ClientHelpers
import org.example.core.utils.urlToPath
import org.example.core.wrappers.StaticCodeAnalyzer
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.code.*
import spoon.reflect.declaration.CtField
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtParameter
import spoon.reflect.reference.CtFieldReference

class SpoonWrapper(
    private val projectDir: String,
    private val clientHelpers: ClientHelpers,
) : StaticCodeAnalyzer {

    private val launcher = initLauncher()

    override fun analyzeInvocations(): List<Endpoint> {
        val model = launcher.model
        val endpoints = mutableListOf<Endpoint>()

        val exchangeCalls = model.getElements<CtInvocation<*>> { e -> e is CtInvocation<*> }
            .filter {
                it.executable.simpleName == "exchange" &&
                        it.target?.type?.qualifiedName?.contains("RestTemplate") == true
            }

        val directExchangeMethods: Set<CtMethod<*>> = exchangeCalls.mapNotNull {
            it.getParent(CtMethod::class.java)
        }.toSet()

        val allWrappers = findTransitiveCallers(model, directExchangeMethods)
        val wrapperParamMappings = mutableMapOf<CtMethod<*>, Pair<String, String>>()

        directExchangeMethods.forEach { wrapper ->
            val invocation = wrapper.body?.statements
                ?.flatMap { it.getElements<CtInvocation<*>> { f -> f is CtInvocation<*> } }
                ?.firstOrNull { it.executable.simpleName == "exchange" }

            if (invocation != null) {
                // Aqui o tipo é CtBinaryOperator, mas estamos sempre tentando buscar como ctVariableRead
                val urlParam = (invocation.arguments.getOrNull(0) as? CtVariableRead<*>)?.variable?.simpleName
                val methodParam = (invocation.arguments.getOrNull(1) as? CtVariableRead<*>)?.variable?.simpleName

                if (urlParam != null && methodParam != null) {
                    wrapperParamMappings[wrapper] = urlParam to methodParam
                }
            }
        }

        allWrappers.forEach { wrapperMethod ->
            val paramMapping = wrapperParamMappings[wrapperMethod] ?: return@forEach
            val (urlParam, methodParam) = paramMapping

            val calls = model.getElements<CtInvocation<*>>{ e -> e is CtInvocation<*> }.filter {
                it.executable.simpleName == wrapperMethod.simpleName
            }

            calls.forEach { call ->
                val callerScope = call.getParent(CtMethod::class.java)
                val urlIndex = wrapperMethod.parameters.indexOfFirst { it.simpleName == urlParam }
                val methodIndex = wrapperMethod.parameters.indexOfFirst { it.simpleName == methodParam }

                if (urlIndex == -1 || methodIndex == -1) return@forEach
                if (call.arguments.size <= maxOf(urlIndex, methodIndex)) return@forEach

                val urlArg = call.arguments[urlIndex]
                val methodArg = call.arguments[methodIndex]

                val resolvedUrl = resolveExpression(urlArg, callerScope, model)
                val resolvedMethod = resolveExpression(methodArg, callerScope, model)

                endpoints.add(Endpoint(urlToPath(resolvedUrl), resolvedMethod))
            }
        }

        println(endpoints)
        return endpoints
    }

    fun findTransitiveCallers(model: CtModel, targets: Set<CtMethod<*>>): Set<CtMethod<*>> {
        val result = mutableSetOf<CtMethod<*>>()
        val queue = ArrayDeque(targets)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!result.add(current)) continue

            val callers = model.getElements<CtInvocation<*>> { e -> e is CtInvocation<*> }
                .filter { it.executable.simpleName == current.simpleName }
                .mapNotNull { it.getParent(CtMethod::class.java) }

            queue.addAll(callers)
        }
        return result
    }

    private fun resolveExpression(expr: CtExpression<*>?, contextMethod: CtMethod<*>?, model: CtModel): String {
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

            is CtFieldReference<*> -> {
                return resolveFieldReference(expr, model)
            }

            is CtVariableRead<*> -> {
                val varName = expr.variable.simpleName

                // Tenta encontrar como variável local
                val localVar = contextMethod?.getElements<CtLocalVariable<*>> { it is CtLocalVariable<*> }
                    ?.filterIsInstance<CtLocalVariable<*>>()
                    ?.find { it.simpleName == varName }

                if (localVar != null) {
                    return resolveExpression(localVar.defaultExpression, contextMethod, model)
                }

                // Verifica se é parâmetro
                val param = contextMethod?.parameters?.find { it.simpleName == varName }
                if (param != null) {
                    return findValueFromCaller(contextMethod, param, model)
                }

                "<variável não resolvida>"
            }

            is CtInvocation<*> -> {
                val execRef = expr.executable
                val calledMethod = model.getElements<CtMethod<*>> { e -> e is CtMethod<*> }
                    .filterIsInstance<CtMethod<*>>()
                    .find { it.simpleName == execRef.simpleName && it.parameters.size == execRef.parameters.size }

                val returnStmt = calledMethod?.getElements<CtReturn<*>> { e -> e is CtReturn<*> }
                    ?.filterIsInstance<CtReturn<*>>()
                    ?.firstOrNull()

                if (returnStmt != null) {
                    return resolveExpression(returnStmt.returnedExpression, calledMethod, model)
                }
                "<invocação não resolvida>"
            }

            else -> "<expressão não reconhecida>"
        }
    }

    fun resolveFieldReference(fieldRef: CtFieldReference<*>, model: CtModel): String {
        val fieldName = fieldRef.simpleName
        val declaringType = fieldRef.declaringType?.qualifiedName

        val fieldDecl = model.getElements<CtField<*>> { e -> e is CtField<*> }
            .find { it.simpleName == fieldName && it.declaringType?.qualifiedName == declaringType }

        val defaultExpr = fieldDecl?.defaultExpression
        if (defaultExpr != null) {
            return resolveExpression(defaultExpr, null, model)
        }

        if (declaringType != null) {
            return fieldName
        }

        return "<field não resolvido: $declaringType.$fieldName>"
    }


    private fun findValueFromCaller(targetMethod: CtMethod<*>, param: CtParameter<*>, model: CtModel): String {
        val paramIndex = targetMethod.parameters.indexOf(param)

        val calls = model.getElements<CtInvocation<*>> { e -> e is CtInvocation<*> }
            .filterIsInstance<CtInvocation<*>>()
            .filter {
                it.executable.simpleName == targetMethod.simpleName &&
                        it.arguments.size > paramIndex
            }

        for (call in calls) {
            val passedArg = call.arguments[paramIndex]
            val callerMethod = call.getParent(CtMethod::class.java)

            if (callerMethod != null) {
                val result = resolveExpression(passedArg, callerMethod, model)
                if (!result.contains("não resolvida")) return result
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