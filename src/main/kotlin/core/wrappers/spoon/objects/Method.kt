package org.example.core.wrappers.spoon.objects

class Method(
    val methodName: String,
    val declaration: String,
) {
    private val arguments = mutableMapOf<String, Argument>()

    fun addArgument(argument: Argument) {
        arguments[argument.identifier] = argument
    }

    fun getArgument(identifier: String): Argument? {
        return arguments[identifier]
    }
}