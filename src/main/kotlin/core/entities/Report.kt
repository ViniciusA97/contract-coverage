package org.example.core.entities

class Report {
    private val endpoints = mutableListOf<Endpoint>()

    fun registerEndpoint(endpoint: Endpoint) {
        endpoints.add(endpoint)
    }
    
    fun getEndpoints(): List<Endpoint> = endpoints.toList()
    
    fun getEndpointCount(): Int = endpoints.size
}