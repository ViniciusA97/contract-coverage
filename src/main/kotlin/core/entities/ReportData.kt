package org.example.core.entities

data class ReportData(
    val timestamp: String,
    val coverage: CoverageData? = null
)

data class CoverageData(
    val totalEndpoints: Int,
    val matchedByPact: Int,
    val coveragePercent: Double,
    val missingEndpoints: List<EndpointData>,
    val matchedEndpoints: List<EndpointData>
)

data class EndpointData(
    val path: String,
    val method: String,
    val sourceFile: String? = null
)
