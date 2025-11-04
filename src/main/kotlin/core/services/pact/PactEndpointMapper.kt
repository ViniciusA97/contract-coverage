package org.example.core.services.pact

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.example.core.entities.Endpoint
import org.example.core.entities.HttpMethod
import org.example.core.entities.pact.Pact
import org.example.core.entities.pact.Interaction
import org.example.core.entities.pact.Request
import org.example.core.entities.pact.Response
import java.io.File

class PactEndpointMapper {
    private val objectMapper = ObjectMapper().registerModule(kotlinModule())

    fun interactionsToEndpoints(pact: Pact): List<Endpoint> {
        return pact.interactions.map { it.toEndpoint() }
    }

    private fun Interaction.toEndpoint(): Endpoint {
        return Endpoint(
            path = this.request.path,
            method = HttpMethod.fromStringOrDefault(this.request.method)
        )
    }
}


