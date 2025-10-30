package org.example.core.services.reports

import org.example.core.entities.Endpoint
import org.example.core.entities.HttpMethod
import org.example.core.entities.Report
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class JsonReportWriterTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should write report to JSON file`() {
        // Given
        val report = Report()
        report.registerEndpoint(Endpoint("/api/users", HttpMethod.GET))
        report.registerEndpoint(Endpoint("/api/users", HttpMethod.POST))
        report.registerEndpoint(Endpoint("/api/users/1", HttpMethod.PUT))

        val outputFile = tempDir.resolve("report.json").toFile()
        val jsonWriter = JsonReportWriter()

        // When
        jsonWriter.writeReport(report, outputFile.absolutePath)

        // Then
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val content = outputFile.readText()
        assertTrue(content.contains("\"totalEndpoints\" : 3"))
        assertTrue(content.contains("\"path\" : \"/api/users\""))
        assertTrue(content.contains("\"method\" : \"GET\""))
        assertTrue(content.contains("\"method\" : \"POST\""))
        assertTrue(content.contains("\"method\" : \"PUT\""))
        assertTrue(content.contains("\"timestamp\""))
    }

    @Test
    fun `should create directory if it does not exist`() {
        // Given
        val report = Report()
        report.registerEndpoint(Endpoint("/test", HttpMethod.GET))

        val outputFile = tempDir.resolve("subdir/report.json").toFile()
        val jsonWriter = JsonReportWriter()

        // When
        jsonWriter.writeReport(report, outputFile.absolutePath)

        // Then
        assertTrue(outputFile.exists())
        assertTrue(outputFile.parentFile.exists())
    }

    @Test
    fun `should handle empty report`() {
        // Given
        val report = Report()
        val outputFile = tempDir.resolve("empty-report.json").toFile()
        val jsonWriter = JsonReportWriter()

        // When
        jsonWriter.writeReport(report, outputFile.absolutePath)

        // Then
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("\"totalEndpoints\" : 0"))
        assertTrue(content.contains("\"endpoints\" : [ ]"))
    }
}

