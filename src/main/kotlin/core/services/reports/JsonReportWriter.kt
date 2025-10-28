package org.example.core.services.reports

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.example.core.entities.EndpointData
import org.example.core.entities.Report
import org.example.core.entities.ReportData
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JsonReportWriter : ReportWriter {
    private val objectMapper = ObjectMapper()
        .registerModule(kotlinModule())
        .enable(SerializationFeature.INDENT_OUTPUT)

    override fun writeReport(report: Report, reportOutput: String) {
        val reportData = ReportData(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            totalEndpoints = report.getEndpointCount(),
            endpoints = report.getEndpoints().map { endpoint ->
                EndpointData(
                    path = endpoint.path,
                    method = endpoint.method.value
                )
            }
        )

        val outputFile = File(reportOutput)
        outputFile.parentFile?.mkdirs()
        
        objectMapper.writeValue(outputFile, reportData)
    }
}