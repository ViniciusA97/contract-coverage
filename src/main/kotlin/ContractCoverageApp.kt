package org.example

import org.example.core.services.pact.PactReader
import org.example.core.services.pact.PactEndpointMapper
import org.example.core.services.coverage.EndpointsComparator
import org.example.core.wrappers.StaticCodeAnalyzer
import org.example.core.services.reports.ReportWriter
import org.example.core.usecases.RestTemplateJavaAnalyzer
import org.example.core.entities.Coverage

class ContractCoverageApp(
    private val analyzer: StaticCodeAnalyzer,
    private val reportWriter: ReportWriter
) {
    fun run(reportOutput: String, pactFilePath: String) {
        val pact = PactReader().read(pactFilePath)
        val sourceCodeAnalyzer = RestTemplateJavaAnalyzer(analyzer)
        val report = sourceCodeAnalyzer.process()

        // Map pact interactions to endpoints and compare
        val pactEndpoints = PactEndpointMapper().interactionsToEndpoints(pact)
        val codeEndpoints = report.getEndpoints()
        val coverage = EndpointsComparator().compare(codeEndpoints, pactEndpoints)
        report.setCoverage(coverage)

        reportWriter.writeReport(report, reportOutput)
    }
}