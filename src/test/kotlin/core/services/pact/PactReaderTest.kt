package org.example.core.services.pact

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class PactReaderTest {

    @Test
    fun `should read pact file and map interactions`() {
        val pactPath = Paths.get("src/test/resources/pacts/sample-pact.json").toAbsolutePath().toString()
        val reader = PactReader()

        val pact = reader.read(pactPath)

        assertEquals("sample-consumer", pact.consumer?.name)
        assertEquals("sample-provider", pact.provider?.name)
        assertEquals(2, pact.interactions.size)
        assertEquals("GET", pact.interactions[0].request.method)
        assertEquals("/api/users", pact.interactions[0].request.path)
        assertEquals(200, pact.interactions[0].response.status)

        assertEquals("POST", pact.interactions[1].request.method)
        assertEquals("/api/users", pact.interactions[1].request.path)
        assertEquals(201, pact.interactions[1].response.status)
    }

    @Test
    fun `should throw when file not found`() {
        val reader = PactReader()
        try {
            reader.read("/path/that/does/not/exist.json")
            assertTrue(false, "Expected exception to be thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Pact file not found"))
        }
    }
}


