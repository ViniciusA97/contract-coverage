package core.wrappers.spoon

import org.example.core.entities.Endpoint
import org.example.core.wrappers.spoon.SpoonWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class SpoonWrapperTest {

    val codePath = "src/test/resources/code/"
    val exchangePath = codePath.plus("exchange")
    val getPath = codePath.plus("get")
    val deletePath = codePath.plus("delete")
    val patchPath = codePath.plus("patch")
    val postPath = codePath.plus("post")
    val putPath = codePath.plus("put")

    @Test
    fun `1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$exchangePath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$exchangePath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$exchangePath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$exchangePath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", "POST"),
            Endpoint("", "GET"),
            Endpoint("", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$exchangePath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$exchangePath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `8) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test8").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/template-style", method = "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `9) Multiple Wrapper - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test9").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/multi-wrapper", method = "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `10) Parameters - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$exchangePath/test10").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/local-vars", method = "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `11) Parameters - should analyze invocations and extract endpoints correctly without get domain url value`() {
        val projectDir = Paths.get("$exchangePath/test11").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint(path = "/cars", method = "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    // GET Tests
    @Test
    fun `GET 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$getPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$getPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$getPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$getPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$getPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$getPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `GET 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$getPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", "GET")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    // POST Tests
    @Test
    fun `POST 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$postPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$postPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$postPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$postPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$postPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$postPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `POST 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$postPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", "POST")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    // PUT Tests
    @Test
    fun `PUT 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$putPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$putPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$putPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$putPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$putPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$putPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PUT 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$putPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", "PUT")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    // PATCH Tests
    @Test
    fun `PATCH 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$patchPath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$patchPath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", "PATCH")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$patchPath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", "PATCH")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$patchPath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", "PATCH")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$patchPath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", "PATCH")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$patchPath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", "PATCH")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `PATCH 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$patchPath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", "PATCH")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    // DELETE Tests
    @Test
    fun `DELETE 1) Basic Case - should analyze invocations and extract no endpoints when wrapper is not called`() {
        val projectDir = Paths.get("$deletePath/test1").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()
        
        val expectedEndpoints = emptyList<Endpoint>()

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 2) Basic Case - should analyze invocations and extract endpoints correctly with call in method of same class`() {
        val projectDir = Paths.get("$deletePath/test2").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-2", "DELETE")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 3) Basic Case - should analyze invocations and extract endpoints correctly with call in another class`() {
        val projectDir = Paths.get("$deletePath/test3").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-3", "DELETE")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 4) Basic Case - should analyze invocations and extract endpoints correctly with multiple calls in the same class`() {
        val projectDir = Paths.get("$deletePath/test4").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-4", "DELETE")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 5) String Manipulation - should analyze invocations and extract endpoints correctly in another class with string operation on URL`() {
        val projectDir = Paths.get("$deletePath/test5").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-5", "DELETE")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 6) String Manipulation - should analyze invocations and extract endpoints correctly with the URL in the body of wrapper`() {
        val projectDir = Paths.get("$deletePath/test6").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/test-6", "DELETE")
        )

        assertEquals(expectedEndpoints, endpoints)
    }

    @Test
    fun `DELETE 7) String Manipulation - should analyze invocations and extract endpoints correctly passing URL as attribute and path variable as parameter`() {
        val projectDir = Paths.get("$deletePath/test7").toAbsolutePath().toString()

        val spoonWrapper = SpoonWrapper(projectDir)

        val endpoints: List<Endpoint> = spoonWrapper.analyzeInvocations()

        val expectedEndpoints = listOf(
            Endpoint("/variable/test7", "DELETE")
        )

        assertEquals(expectedEndpoints, endpoints)
    }
}