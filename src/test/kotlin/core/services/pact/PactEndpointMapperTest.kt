package org.example.core.services.pact

import org.example.core.entities.HttpMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class PactEndpointMapperTest {

    @Test
    fun `should map pact interactions to endpoints`() {
        val pactPath = Paths.get("src/test/resources/pacts/sample-pact.json").toAbsolutePath().toString()
        val pact = PactReader().read(pactPath)
        val endpoints = PactEndpointMapper().interactionsToEndpoints(pact)

        assertEquals(2, endpoints.size)
        assertEquals("/api/users", endpoints[0].path)
        assertEquals(HttpMethod.GET, endpoints[0].method)
        assertEquals("/api/users", endpoints[1].path)
        assertEquals(HttpMethod.POST, endpoints[1].method)
    }
}


