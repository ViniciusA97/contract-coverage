package org.example.core.wrappers.spoon.callextractors

import org.example.core.entities.Endpoint
import org.example.core.wrappers.spoon.MethodCallContext
import spoon.reflect.CtModel
import spoon.reflect.code.CtInvocation

interface CallExtractor {
    fun supports(call: CtInvocation<*>): Boolean
    fun extract(call: CtInvocation<*>, context: MethodCallContext, model: CtModel): Endpoint
}


