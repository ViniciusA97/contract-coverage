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

        model.getElements<CtInvocation<*>> { e -> e is CtInvocation<*> }
            .filter { invocation ->
                invocation.executable.simpleName == "exchange" &&
                        invocation.target != null &&
                        invocation.target?.type?.qualifiedName == clientHelpers.type()
            }
            .forEach { invocation ->
                val urlArg = invocation.arguments[0]
                val methodArg = invocation.arguments[1]
                val resolvedUrl = resolveExpression(urlArg, invocation.getParent(CtMethod::class.java), model)
                val resolvedMethod = resolveExpression(methodArg, invocation.getParent(CtMethod::class.java), model)
                endpoints.add(Endpoint(urlToPath(resolvedUrl), resolvedMethod))
            }


        println(endpoints)
        return endpoints
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