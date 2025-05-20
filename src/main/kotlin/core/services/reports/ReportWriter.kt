package org.example.core.services.reports

import org.example.core.entities.Report

interface ReportWriter {
    fun writeReport(report: Report, reportOutput: String)
}