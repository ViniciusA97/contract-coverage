package org.example.core.wrappers.spoon

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
                    resolveExpression(args[index], scopeMethod, model)
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
                            if (!resolved.contains("não resolvida")) return resolved
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

                return "<variável não resolvida>"
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

            else -> "<expressão não reconhecida>"
        }
    }
}