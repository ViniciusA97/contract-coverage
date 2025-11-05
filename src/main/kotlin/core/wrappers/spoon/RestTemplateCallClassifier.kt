package org.example.core.wrappers.spoon

import spoon.reflect.code.CtInvocation

class RestTemplateCallClassifier {

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
        return call.executable.simpleName == "exchange" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplateGetForEntity(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "getForEntity" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplateGetForObject(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "getForObject" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplatePostForEntity(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "postForEntity" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplatePostForObject(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "postForObject" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplatePut(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "put" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplatePatch(call: CtInvocation<*>): Boolean {
        val name = call.executable.simpleName
        return (name == "patchForObject" || name == "patchForEntity" || name == "patch") &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }

    fun isRestTemplateDelete(call: CtInvocation<*>): Boolean {
        return call.executable.simpleName == "delete" &&
                call.target?.type?.qualifiedName == "org.springframework.web.client.RestTemplate"
    }
}


