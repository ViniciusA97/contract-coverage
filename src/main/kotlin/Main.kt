package org.example

import org.example.core.services.reports.JsonReportWriter
import org.example.core.wrappers.spoon.SpoonWrapper

const val projectDir = "/home/vini/IdeaProjects/contract-coverage"
const val specificFolder = "src/test/resources/code/delete/test2"

const val finalPath = "$projectDir/$specificFolder"

fun main() {
    val app = ContractCoverageApp(
        SpoonWrapper(finalPath),
        JsonReportWriter()
    )
    app.run("./reports/report.json")
}