package org.example.core.wrappers.spoon.objects

import spoon.reflect.code.CtInvocation

class MethodRegister {
    private val methods = mutableMapOf<String, Method>()
    private val clientCalls = mutableListOf<CtInvocation<*>>()
    private val parentCalls = mutableMapOf<String,CtInvocation<*>>()

    fun addMethod(method: Method) {
        methods[method.methodName] = method
    }

    fun registerCallToClient(call: CtInvocation<*>) {
        clientCalls.add(call)
    }

    fun getMethod(methodName: String): Method? {
        return methods[methodName]
    }

    fun registerCallToParent(methodName: String, call: CtInvocation<*>) {
        parentCalls[methodName] = call
    }

    fun getParentCall(methodName: String): CtInvocation<*>? {
        return parentCalls[methodName]
    }

    fun getAllMethods(): List<Method> {
        return methods.values.toList()
    }
}