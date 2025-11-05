package org.example.core.services.coverage

import org.example.core.entities.Endpoint
import org.example.core.entities.HttpMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EndpointsComparatorTest {

    @Test
    fun `should compute full coverage when all endpoints are matched`() {
        val code = listOf(
            Endpoint("/a", HttpMethod.GET),
            Endpoint("/b", HttpMethod.POST)
        )
        val pact = listOf(
            Endpoint("/a", HttpMethod.GET),
            Endpoint("/b", HttpMethod.POST)
        )

        val coverage = EndpointsComparator().compare(code, pact)

        assertEquals(2, coverage.totalCodeEndpoints)
        assertEquals(2, coverage.matchedByPact)
        assertEquals(100.0, coverage.coveragePercent)
        assertEquals(0, coverage.missingEndpoints.size)
        assertEquals(2, coverage.matchedEndpoints.size)
    }

    @Test
    fun `should compute partial coverage with missing endpoints`() {
        val code = listOf(
            Endpoint("/a", HttpMethod.GET),
            Endpoint("/b", HttpMethod.POST),
            Endpoint("/c", HttpMethod.PUT)
        )
        val pact = listOf(
            Endpoint("/a", HttpMethod.GET),
            Endpoint("/b", HttpMethod.POST)
        )

        val coverage = EndpointsComparator().compare(code, pact)

        assertEquals(3, coverage.totalCodeEndpoints)
        assertEquals(2, coverage.matchedByPact)
        assertEquals(66.67, coverage.coveragePercent)
        assertEquals(1, coverage.missingEndpoints.size)
        assertEquals(Endpoint("/c", HttpMethod.PUT), coverage.missingEndpoints.first())
    }

    @Test
    fun `should match paths with path variables`() {
        val code = listOf(
            Endpoint("/users/{id}", HttpMethod.GET)
        )
        val pact = listOf(
            Endpoint("/users/999", HttpMethod.GET)
        )

        val coverage = EndpointsComparator().compare(code, pact)

        assertEquals(1, coverage.totalCodeEndpoints)
        assertEquals(1, coverage.matchedByPact)
        assertEquals(100.0, coverage.coveragePercent)
        assertEquals(0, coverage.missingEndpoints.size)
        assertEquals(1, coverage.matchedEndpoints.size)
    }

    @Test
    fun `should match paths with multiple path variables`() {
        val code = listOf(
            Endpoint("/users/{userId}/posts/{postId}", HttpMethod.GET)
        )
        val pact = listOf(
            Endpoint("/users/123/posts/456", HttpMethod.GET)
        )

        val coverage = EndpointsComparator().compare(code, pact)

        assertEquals(1, coverage.totalCodeEndpoints)
        assertEquals(1, coverage.matchedByPact)
        assertEquals(100.0, coverage.coveragePercent)
        assertEquals(0, coverage.missingEndpoints.size)
    }

    @Test
    fun `should not match paths with different segment counts`() {
        val code = listOf(
            Endpoint("/users/{id}", HttpMethod.GET)
        )
        val pact = listOf(
            Endpoint("/users/123/posts", HttpMethod.GET)
        )

        val coverage = EndpointsComparator().compare(code, pact)

        assertEquals(1, coverage.totalCodeEndpoints)
        assertEquals(0, coverage.matchedByPact)
        assertEquals(0.0, coverage.coveragePercent)
        assertEquals(1, coverage.missingEndpoints.size)
    }

    @Test
    fun `should not match paths with different literal segments`() {
        val code = listOf(
            Endpoint("/users/{id}", HttpMethod.GET)
        )
        val pact = listOf(
            Endpoint("/products/999", HttpMethod.GET)
        )

        val coverage = EndpointsComparator().compare(code, pact)

        assertEquals(1, coverage.totalCodeEndpoints)
        assertEquals(0, coverage.matchedByPact)
        assertEquals(0.0, coverage.coveragePercent)
        assertEquals(1, coverage.missingEndpoints.size)
    }
}


