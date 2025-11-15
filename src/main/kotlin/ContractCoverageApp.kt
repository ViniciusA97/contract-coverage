package org.example

import org.example.core.services.pact.PactReader
import org.example.core.services.pact.PactEndpointMapper
import org.example.core.services.coverage.EndpointsComparator
import org.example.core.wrappers.StaticCodeAnalyzer
import org.example.core.services.reports.ReportWriter
import org.example.core.usecases.RestTemplateJavaAnalyzer
import java.io.File

class ContractCoverageApp(
    private val analyzer: StaticCodeAnalyzer,
    private val reportWriter: ReportWriter
) {
    fun run(reportOutput: String, pactPath: String) {
        val pactReader = PactReader()
        
        // Read all Pacts from directory
        val pacts = if (File(pactPath).isDirectory) {
            val directoryPacts = pactReader.readDirectory(pactPath)
            println("Found ${directoryPacts.size} Pact file(s) in directory")
            directoryPacts
        } else {
            // Backward compatibility: if it's a file, read it
            listOf(pactReader.read(pactPath))
        }
        
        val sourceCodeAnalyzer = RestTemplateJavaAnalyzer(analyzer)
        val report = sourceCodeAnalyzer.process()

        // Map all pact interactions to endpoints and combine them
        val pactEndpoints = pacts.flatMap { pact ->
            PactEndpointMapper().interactionsToEndpoints(pact)
        }
        
        println("Total endpoints from Pacts: ${pactEndpoints.size}")
        
        val codeEndpoints = report.getEndpoints()
        val coverage = EndpointsComparator().compare(codeEndpoints, pactEndpoints)
        report.setCoverage(coverage)

        reportWriter.writeReport(report, reportOutput)
    }
}