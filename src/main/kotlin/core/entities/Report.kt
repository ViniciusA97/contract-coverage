package org.example.core.entities

class Report {
    private val endpoints = mutableListOf<Endpoint>()

    fun registerEndpoint(endpoint: Endpoint) {
        endpoints.add(endpoint)
    }
}