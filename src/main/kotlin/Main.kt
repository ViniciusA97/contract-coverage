package org.example

import org.example.core.services.reports.JsonReportWriter
import org.example.core.wrappers.spoon.SpoonWrapper

const val projectDir = "/home/vini/IdeaProjects/contract-example"
const val specificFolder = "src/main/java/org/example/contractexample"

const val finalPath = "$projectDir/$specificFolder"

fun main() {
    val app = ContractCoverageApp(
        SpoonWrapper(finalPath),
        JsonReportWriter()
    )
    val pactPath = "$projectDir/build/pacts/contract-example-consumer-user-service-provider.json"
    app.run("./reports/report.json", pactPath)
}