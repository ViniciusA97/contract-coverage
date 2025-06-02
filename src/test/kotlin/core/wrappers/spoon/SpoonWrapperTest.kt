package core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.services.clients.RestTemplateClientHelpers
import org.example.core.wrappers.spoon.SpoonWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class SpoonWrapperTest {

    val codePath = "src/test/resources/code"

    @Test
    fun `1 - should analyze invocations and extract endpoints correctly without call`() {
        val projectDir = Paths.get("$codePath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = listOf(
            Endpoint("", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `2 - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$codePath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `3 - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$codePath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `4 - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$codePath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", "POST"),
            Endpoint("", "GET"),
            Endpoint("", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `5 - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$codePath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `6 - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$codePath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `7 - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$codePath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }
}