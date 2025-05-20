package org.example.core.wrappers

import org.example.core.entities.Endpoint

interface InvocationWrappers {
    fun resolveEndpoints(): List<Endpoint>
}