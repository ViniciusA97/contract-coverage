package org.example.core.services.reports

class ReportFactory {
    fun create(reportType: String): ReportWriter {
        return JsonReportWriter()
    }
}