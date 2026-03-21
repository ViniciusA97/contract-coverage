package org.example.core.wrappers.spoon

import spoon.reflect.code.CtInvocation
import spoon.reflect.declaration.CtClass

/**
 * Extracts the source file name from a call invocation.
 * Falls back to the containing class name when position info is unavailable.
 */
fun resolveSourceFile(call: CtInvocation<*>): String? {
    return call.position?.file?.name
        ?: call.getParent(CtClass::class.java)?.position?.file?.name
        ?: call.getParent(CtClass::class.java)?.simpleName?.let { "$it.java" }
}
