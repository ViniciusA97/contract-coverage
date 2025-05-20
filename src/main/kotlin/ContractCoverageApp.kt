package org.example

import org.example.core.wrappers.StaticCodeAnalyzer
import org.example.core.services.reports.ReportWriter
import org.example.core.usecases.RestTemplateJavaAnalyzer

class ContractCoverageApp(
    private val analyzer: StaticCodeAnalyzer,
    private val reportWriter: ReportWriter
) {
    fun run(reportOutput: String) {
        val useCase = RestTemplateJavaAnalyzer(analyzer)
        val report = useCase.process()
        //reportWriter.writeReport(report, reportOutput)
    }
}