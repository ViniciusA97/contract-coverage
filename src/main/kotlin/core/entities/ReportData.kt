package org.example.core.entities

data class ReportData(
    val timestamp: String,
    val totalEndpoints: Int,
    val endpoints: List<EndpointData>
)

data class EndpointData(
    val path: String,
    val method: String
)
