package org.example.core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.entities.HttpMethod
import org.example.core.wrappers.StaticCodeAnalyzer
import org.example.core.wrappers.spoon.callextractors.CallExtractor
import org.example.core.wrappers.spoon.callextractors.ExchangeCallExtractor
import org.example.core.wrappers.spoon.callextractors.SimpleMethodCallExtractor
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.code.*
import spoon.reflect.declaration.CtMethod

data class MethodCallContext(
    val method: CtMethod<*>,
    val arguments: List<CtExpression<*>>
)

class SpoonWrapper(
    private val projectDir: String,
) : StaticCodeAnalyzer {

    private val launcher = initLauncher()
    private val spoonExpressionResolver = SpoonExpressionResolver()
    private val classifier = RestTemplateCallClassifier()
    private val extractors: List<CallExtractor> = listOf(
        ExchangeCallExtractor(classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateGetForEntity(call) }, HttpMethod.GET, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePostForEntity(call) }, HttpMethod.POST, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePut(call) }, HttpMethod.PUT, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplatePatch(call) }, HttpMethod.PATCH, classifier, spoonExpressionResolver),
        SimpleMethodCallExtractor({ c, call -> c.isRestTemplateDelete(call) }, HttpMethod.DELETE, classifier, spoonExpressionResolver),
    )

    override fun analyzeInvocations(): List<Endpoint> {
        val model = launcher.model
        return extractEndpoints(model)
    }

    fun extractEndpoints(model: CtModel): List<Endpoint> {
        val projectCalls = findCallsWithResolvedArgs(model)

        val endpoints = projectCalls.mapNotNull { (call, context) ->
            val extractor = extractors.firstOrNull { it.supports(call) } ?: return@mapNotNull null
            extractor.extract(call, context, model)
        }

        return endpoints.distinctBy { it.path to it.method }
    }

    fun findCallsWithResolvedArgs(model: CtModel): List<Pair<CtInvocation<*>, MethodCallContext>> {
        val result = mutableListOf<Pair<CtInvocation<*>, MethodCallContext>>()

        val allInvocations = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filterIsInstance<CtInvocation<*>>()

        allInvocations.forEach { invocation ->
            if (!classifier.isRestTemplateCall(invocation)) return@forEach

            val parentMethod = invocation.getParent(CtMethod::class.java) ?: return@forEach

            // Para cada chamada ao método que contém o RestTemplate, gerar um contexto com os argumentos do call site
            val callSites = findMethodCallSites(parentMethod, model)

            if (callSites.isEmpty()) {
                // Sem call sites: só considerar se for main (evita falsos positivos do teste 1)
                if (parentMethod.simpleName == "main") {
                    result.add(invocation to MethodCallContext(parentMethod, emptyList()))
                }
            } else {
                callSites.forEach { site ->
                    result.add(invocation to MethodCallContext(parentMethod, site.arguments))
                }
            }
        }

        return result
    }
    
    private fun findMethodCallSites(method: CtMethod<*>, model: CtModel): List<CtInvocation<*>> {
        val allInvocations = model.getElements<CtInvocation<*>> { it is CtInvocation<*> }
            .filterIsInstance<CtInvocation<*>>()
        return allInvocations.filter { inv ->
            val decl = inv.executable.declaration as? CtMethod<*>
            decl?.signature == method.signature &&
                    decl.declaringType?.qualifiedName == method.declaringType?.qualifiedName
        }
    }

    private fun initLauncher(): Launcher {
        val launcher = Launcher()
        launcher.addInputResource(projectDir)
        launcher.buildModel()
        return launcher
    }
}