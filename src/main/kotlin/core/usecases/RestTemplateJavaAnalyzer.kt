package org.example.core.usecases

import org.example.core.entities.Report
import org.example.core.wrappers.StaticCodeAnalyzer

class RestTemplateJavaAnalyzer(
    private val analyzer: StaticCodeAnalyzer,
) {

    fun process(): Report {
        val endpointsList = analyzer.analyzeInvocations()
        return Report()
    }
}