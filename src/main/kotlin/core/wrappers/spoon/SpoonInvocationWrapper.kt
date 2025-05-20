package org.example.core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.wrappers.InvocationWrappers
import org.slf4j.LoggerFactory
import spoon.reflect.code.*
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtParameter


class SpoonInvocationWrapper(
    private val invocation: CtInvocation<*>,
): InvocationWrappers {

    init {
        LoggerFactory.getLogger(SpoonInvocationWrapper::class.java)
            .debug("Initializing spoon invocation wrapper. Method invocation: ${invocation.executable.simpleName}")
    }

    override fun resolveEndpoints(): List<Endpoint> {
        val endpoints = mutableListOf<Endpoint>()

        if (invocation.executable.simpleName == "exchange") {
            val arguments = invocation.arguments
            val url = resolveArgumentValue(arguments[0])
            val method = resolveArgumentValue(arguments[1])

            if (method != null && url != null) {
                endpoints.add(Endpoint(method, url))
            }
        }

        return endpoints
    }

    private fun inferirValor(expr: CtExpression<*>): String {
        if (expr is CtLiteral<*>) {
            return expr.value.toString()
        } else if (expr is CtVariableRead<*>) {
            val variableName = expr.variable.simpleName

            val enclosingMethod = expr.getParent(CtMethod::class.java)
            if (enclosingMethod != null) {
                for (param in enclosingMethod.parameters) {
                    if (param.simpleName == variableName) {
                        return "Parâmetro do método: " + enclosingMethod.simpleName + " -> " + variableName
                    }
                }
            }

            val methodBody = expr.getParent(CtBlock::class.java)
            if (methodBody != null) {
                for (stmt in methodBody.statements) {
                    if (stmt is CtLocalVariable<*>) {
                        val localVar = stmt
                        if (localVar.simpleName == variableName) {
                            return inferirValor(localVar.defaultExpression) // Continua a inferência
                        }
                    }
                }
            }

            return "Valor desconhecido (precisa rastrear chamadas de método)"
        } else if (expr is CtBinaryOperator<*>) {
            val binaryOp = expr
            return inferirValor(binaryOp.leftHandOperand) + inferirValor(binaryOp.rightHandOperand)
        }
        return "Não identificado"
    }

    private fun resolveArgumentValue(argument: Any?): String? {
        return when (argument) {
            is CtLiteral<*> -> argument.value as? String
            is CtVariableRead<*> -> {
                val variable = argument.variable
                when (val declaration = variable.declaration) {
                    is CtLiteral<*> -> declaration.value as? String
                    is CtParameter<*> -> {
                        val defaultExpression = declaration.defaultExpression
                        if (defaultExpression is CtLiteral<*>) {
                            defaultExpression.value as? String
                        } else {
                            null
                        }
                    }
                    else -> null
                }
            }
            else -> inferArgumentValue(argument)
        }
    }

    private fun inferArgumentValue(argument: Any?): String? {
        return when (argument) {
            is CtInvocation<*> -> {
                val executable = argument.executable
                if (executable.simpleName == "toString") {
                    val target = argument.target
                    if (target is CtVariableRead<*>) {
                        val variable = target.variable
                        val declaration = variable.declaration
                        if (declaration is CtLiteral<*>) {
                            return declaration.value as? String
                        }
                    }
                }
                null
            }
            else -> null
        }
    }
}
