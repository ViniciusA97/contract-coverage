package core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.entities.HttpMethod
import org.example.core.wrappers.spoon.SpoonWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Compare endpoints ignoring sourceFile field.
 * This allows tests to focus on path and method without worrying about file detection.
 */
fun assertEndpointsEqual(expected: List<Endpoint>, actual: List<Endpoint>) {
    val expectedNormalized = expected.map { it.copy(sourceFile = null) }.sortedBy { "${it.method}:${it.path}" }
    val actualNormalized = actual.map { it.copy(sourceFile = null) }.sortedBy { "${it.method}:${it.path}" }
    assertEquals(expectedNormalized, actualNormalized)
}

class SpoonWrapperTest {

    val codePath = "src/test/resources/code/"
    val exchangePath = codePath.plus("exchange")
    val getPath = codePath.plus("get")
    val getForObjectPath = codePath.plus("getForObject")
    val deletePath = codePath.plus("delete")
    val patchPath = codePath.plus("patch")
    val postPath = codePath.plus("post")
    val postForObjectPath = codePath.plus("postForObject")
    val putPath = codePath.plus("put")

    @Test
    fun `1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$exchangePath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$exchangePath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$exchangePath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$exchangePath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.POST),
            Endpoint("<dynamic-url>", HttpMethod.GET),
            Endpoint("<dynamic-url>", HttpMethod.PUT)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$exchangePath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$exchangePath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `8) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test8").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/template-style", method = HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `9) Multiple Wrapper - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test9").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/multi-wrapper", method = HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `10) Parameters - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test10").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/local-vars", method = HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `11) Parameters - should analyze invocations and extract endpoints correctly without get domain url value`() {
        val projectDir = Paths.get("$exchangePath/test11").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/cars", method = HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // GET Tests
    @Test
    fun `GET 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$getPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$getPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$getPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$getPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$getPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$getPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$getPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // POST Tests
    @Test
    fun `POST 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$postPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$postPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$postPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$postPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$postPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$postPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$postPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // PUT Tests
    @Test
    fun `PUT 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$putPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$putPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.PUT)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$putPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.PUT)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$putPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.PUT)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$putPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.PUT)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$putPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.PUT)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$putPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.PUT)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // PATCH Tests
    @Test
    fun `PATCH 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$patchPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$patchPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.PATCH)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$patchPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.PATCH)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$patchPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.PATCH)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$patchPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.PATCH)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$patchPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.PATCH)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$patchPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.PATCH)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // DELETE Tests
    @Test
    fun `DELETE 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$deletePath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$deletePath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$deletePath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$deletePath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$deletePath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$deletePath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$deletePath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // GET FOR OBJECT Tests
    @Test
    fun `GET FOR OBJECT 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$getForObjectPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET FOR OBJECT 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$getForObjectPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET FOR OBJECT 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$getForObjectPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET FOR OBJECT 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$getForObjectPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET FOR OBJECT 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$getForObjectPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET FOR OBJECT 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$getForObjectPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET FOR OBJECT 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$getForObjectPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // POST FOR OBJECT Tests
    @Test
    fun `POST FOR OBJECT 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$postForObjectPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST FOR OBJECT 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$postForObjectPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST FOR OBJECT 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$postForObjectPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST FOR OBJECT 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$postForObjectPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST FOR OBJECT 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$postForObjectPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST FOR OBJECT 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$postForObjectPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST FOR OBJECT 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$postForObjectPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // ========================
    // UriComponentsBuilder Tests
    // ========================

    @Test
    fun `12) UriComponentsBuilder - should extract endpoint from UriComponentsBuilder with fromHttpUrl and path`() {
        val projectDir = Paths.get("$exchangePath/test12").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        // Path now includes the base URL path (/v1 from https://api.example.com/v1)
        val expectedEndpoints = listOf(
            Endpoint("/v1/users", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `13) UriComponentsBuilder with wrapper - should extract endpoints from UriComponentsBuilder passed through wrapper method`() {
        val projectDir = Paths.get("$exchangePath/test13").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        // Path now includes the base URL path (/v0.1 from https://api.example.com/v0.1)
        val expectedEndpoints = listOf(
            Endpoint("/v0.1/merchants/readers", HttpMethod.GET),
            Endpoint("/v0.1/orders", HttpMethod.POST),
            Endpoint("/v0.1/merchants/readers/delete", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `14) UriComponentsBuilder with pathSegment - should extract endpoints from pathSegment with multiple arguments`() {
        val projectDir = Paths.get("$exchangePath/test14").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        // Path now includes the base URL path (/v0.1 from https://api.example.com/v0.1)
        val expectedEndpoints = listOf(
            Endpoint("/v0.1/merchants/code123/readers", HttpMethod.GET),
            Endpoint("/v0.1/me/transactions", HttpMethod.GET),
            Endpoint("/v0.1/me/refund/tx123", HttpMethod.POST)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // ========================
    // RestTemplate in local method Tests (Pattern: restTemplate().exchange())
    // ========================

    @Test
    fun `15) RestTemplate in method - should detect RestTemplate created via method call`() {
        val projectDir = Paths.get("$exchangePath/test15").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        // The URI is passed from Main, so we should detect the endpoint
        val expectedEndpoints = listOf(
            Endpoint("/v1/users", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // ========================
    // Wrapper with method returning RestTemplate Tests
    // ========================

    @Test
    fun `16) Wrapper with createRestTemplate method - should detect endpoints through wrapper with method-based RestTemplate`() {
        val projectDir = Paths.get("$exchangePath/test16").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        // Should detect all endpoints passed through the LowLevelClient wrapper
        val expectedEndpoints = listOf(
            Endpoint("/groups", HttpMethod.GET),
            Endpoint("/receivers", HttpMethod.POST),
            Endpoint("/receivers", HttpMethod.PUT),
            Endpoint("/receivers", HttpMethod.DELETE)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    // ========================
    // Builder pattern with parameter tracing Tests
    // ========================

    @Test
    fun `17) Builder pattern - complex builder object as parameter is marked as dynamic`() {
        val projectDir = Paths.get("$exchangePath/test17").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        // Complex builder patterns like GetRequest.builder().baseURL().pathVariables().build()
        // passed as a parameter to a method that calls getBaseURL() and getPathVariables()
        // are too complex for static analysis - they are marked as dynamic
        // The URL is constructed from getRequest.getBaseURL() and getRequest.getPathVariables()
        // which require deep builder tracing across method calls
        val expectedEndpoints = listOf(
            Endpoint("<dynamic-url>", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }

    @Test
    fun `18) UriComponentsBuilder with path parameter - should resolve base URL and path from parameter`() {
        val projectDir = Paths.get("$exchangePath/test18").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        // BASE_URL is constant, path comes from callers
        // Should detect: /users (from getUsers) and /users/{dynamic} (from getUser with concatenation)
        val expectedEndpoints = listOf(
            Endpoint("/users", HttpMethod.GET),
            Endpoint("/users/{dynamic}", HttpMethod.GET)
        )

        assertEndpointsEqual(expectedEndpoints, endpoints)
    }
}