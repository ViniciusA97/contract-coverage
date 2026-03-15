package org.example.core.entities

data class Endpoint(
    val path: String,
    val method: HttpMethod,
    val sourceFile: String? = null,
)
