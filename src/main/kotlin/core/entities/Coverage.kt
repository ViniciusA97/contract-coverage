package org.example.core.entities

data class Coverage(
    val totalCodeEndpoints: Int,
    val matchedByPact: Int,
    val coveragePercent: Double,
    val missingEndpoints: List<Endpoint>,
    val matchedEndpoints: List<Endpoint>
)


