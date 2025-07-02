package org.example.core.wrappers

import org.example.core.entities.Endpoint

interface InvocationReader {
    fun resolveEndpoints(): List<Endpoint>
}