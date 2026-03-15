package org.example.core.wrappers.spoon

import spoon.reflect.code.CtInvocation

class RestTemplateCallClassifier {

    companion object {
        private val REST_TEMPLATE_TYPES = setOf(
            "org.springframework.web.client.RestTemplate",
            "RestTemplate"
        )
    }

    fun isRestTemplateCall(call: CtInvocation<*>): Boolean {
        return isRestTemplateExchange(call) ||
                isRestTemplateGetForEntity(call) ||
                isRestTemplateGetForObject(call) ||
                isRestTemplatePostForEntity(call) ||
                isRestTemplatePostForObject(call) ||
                isRestTemplatePut(call) ||
                isRestTemplatePatch(call) ||
                isRestTemplateDelete(call)
    }

    fun isRestTemplateExchange(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "exchange" && isRestTemplateTarget(call)
    }

    fun isRestTemplateGetForEntity(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "getForEntity" && isRestTemplateTarget(call)
    }

    fun isRestTemplateGetForObject(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "getForObject" && isRestTemplateTarget(call)
    }

    fun isRestTemplatePostForEntity(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "postForEntity" && isRestTemplateTarget(call)
    }

    fun isRestTemplatePostForObject(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "postForObject" && isRestTemplateTarget(call)
    }

    fun isRestTemplatePut(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "put" && isRestTemplateTarget(call)
    }

    fun isRestTemplatePatch(call: CtInvocation<*>): Boolean {
        val name = call.executable.simpleName
        return (name == "patchForObject" || name == "patchForEntity" || name == "patch") && isRestTemplateTarget(call)
    }

    fun isRestTemplateDelete(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "delete" && isRestTemplateTarget(call)
    }
    
    /**
     * Check if the target of the invocation is a RestTemplate.
     * This handles both direct field access (restTemplate.method()) and 
     * method calls that return RestTemplate (restTemplate().method()).
     */
    private fun isRestTemplateTarget(call: CtInvocation<*>): Boolean {
        val target = call.target ?: return false
        val targetType = target.type
        
        // Direct type check (field or variable)
        if (targetType?.qualifiedName in REST_TEMPLATE_TYPES) {
            return true
        }
        
        // Check if target is a method invocation that returns RestTemplate
        if (target is CtInvocation<*>) {
            val returnType = target.type?.qualifiedName
            val returnTypeSimple = target.type?.simpleName
            if (returnType in REST_TEMPLATE_TYPES || returnTypeSimple in REST_TEMPLATE_TYPES) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if the target is a method invocation that returns RestTemplate.
     * This is useful for detecting patterns like: restTemplate().exchange(...)
     */
    fun isMethodReturningRestTemplate(call: CtInvocation<*>): Boolean {
        val target = call.target
        if (target is CtInvocation<*>) {
            val returnType = target.type?.qualifiedName
            val returnTypeSimple = target.type?.simpleName
            return returnType in REST_TEMPLATE_TYPES || returnTypeSimple in REST_TEMPLATE_TYPES
        }
        return false
    }
}


