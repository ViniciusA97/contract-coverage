package org.example

import org.example.core.services.clients.RestTemplateClientHelpers
import org.example.core.services.reports.JsonReportWriter
import org.example.core.wrappers.spoon.SpoonWrapper

const val projectDir = "/home/vini/IdeaProjects/contract-coverage"
const val specificFolder = "src/main/resources/code/test7"

const val finalPath = "$projectDir/$specificFolder"

fun main() {
    val app = ContractCoverageApp(
        SpoonWrapper(finalPath, RestTemplateClientHelpers()),
        JsonReportWriter()
    )
    app.run("")
}