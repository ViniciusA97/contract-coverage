package org.example.core.entities

class Report {
    private val endpoints = mutableListOf<Endpoint>()
    private var coverage: Coverage? = null

    fun registerEndpoint(endpoint: Endpoint) {
        endpoints.add(endpoint)
    }
    
    fun getEndpoints(): List<Endpoint> = endpoints.toList()
    
    fun getEndpointCount(): Int = endpoints.size

    fun setCoverage(coverage: Coverage) {
        this.coverage = coverage
    }

    fun getCoverage(): Coverage? = coverage
}