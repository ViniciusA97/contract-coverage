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
import spoon.reflect.reference.CtFieldReference
import spoon.reflect.reference.CtLocalVariableReference
import spoon.reflect.visitor.Filter

data class MethodCallContext(
    val method: CtMethod<*>,
    val arguments: List<CtExpression<*>>
)


class SpoonWrapper(
    private val projectDir: String,
    private val clientHelpers: ClientHelpers,
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
            resolveExpressionWithParams(arg, parameters, callArgs, scopeMethod, model)
        } ?: "{unknown}"

        val resolvedMethod = call.arguments.getOrNull(1)?.let { arg ->
            resolveExpressionWithParams(arg, parameters, callArgs, scopeMethod, model)
        } ?: "{unknown}"

        return urlToPath(resolvedUrl) to resolvedMethod
    }

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

    fun isRestTemplateExchange(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "exchange" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    private fun resolveExpression(expr: CtExpression<*>?, contextMethod: CtMethod<*>?, model: CtModel, context: CtElement? = null,): String {
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

            // Tentamos resolver o argumento
            val resolved = resolveExpression(
                expr = passedArg,
                contextMethod = callerMethod,
                context = call,
                model = model
            )

            // Se for uma string literal ou expressão resolvida, retornamos
            if (!resolved.contains("não resolvida")) {
                return resolved
            }

            // Se for uma variável ou parâmetro, continuamos rastreando recursivamente
            if (passedArg is CtVariableRead<*>) {
                val varName = passedArg.variable.simpleName

                // Se for um parâmetro do método chamador, resolvemos recursivamente
                val nextParam = callerMethod?.parameters?.find { it.simpleName == varName }
                if (nextParam != null) {
                    return findValueFromCaller(callerMethod, nextParam, model)
                }

                // Se for uma variável local, tentamos resolver normalmente
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