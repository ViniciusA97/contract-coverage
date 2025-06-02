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
    fun `should analyze invocations and extract endpoints correctly`() {
        val projectDir = Paths.get("$codePath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir, RestTemplateClientHelpers())

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = listOf(
            Endpoint("/test-2", "POST"),
            Endpoint("/test-1", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }
}