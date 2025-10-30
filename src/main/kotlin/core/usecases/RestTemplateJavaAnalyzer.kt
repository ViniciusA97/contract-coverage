package org.example.core.usecases

import org.example.core.entities.Report
import org.example.core.wrappers.StaticCodeAnalyzer

class RestTemplateJavaAnalyzer(
    private val analyzer: StaticCodeAnalyzer,
) {

    fun process(): Report {
        val endpointsList = analyzer.analyzeInvocations()
        val report = Report()
        
        endpointsList.forEach { endpoint ->
            report.registerEndpoint(endpoint)
        }
        
        return report
    }
}