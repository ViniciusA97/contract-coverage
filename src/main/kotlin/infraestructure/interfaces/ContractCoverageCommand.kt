package org.example.infraestructure.interfaces

import org.example.ContractCoverageApp
import org.example.core.services.reports.JsonReportWriter
import org.example.core.wrappers.spoon.SpoonWrapper
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "contract-coverage",
    description = ["Analyzes Java code for RestTemplate calls and compares with Pact contracts"],
    mixinStandardHelpOptions = true,
    version = ["1.0-SNAPSHOT"]
)
class ContractCoverageCommand : Callable<Int> {

    @CommandLine.Option(
        names = ["-s", "--source-code-dir"],
        description = ["Path to the Java source code directory to analyze"],
        required = true,
        paramLabel = "<code-path>"
    )
    private lateinit var codePath: String

    @CommandLine.Option(
        names = ["-p", "--pact-path"],
        description = ["Path to the Pact directory containing JSON files"],
        required = true,
        paramLabel = "<pact-dir>"
    )
    private lateinit var pactPath: String

    @CommandLine.Option(
        names = ["-o", "--output"],
        description = ["Output path for the coverage report (default: ./reports/report.json)"],
        defaultValue = "./reports/report.json",
        paramLabel = "<output-path>"
    )
    private var outputPath: String = "./reports/report.json"

    override fun call(): Int {
        try {
            // Validate inputs
            validateInputs()

            println("Analyzing code at: $codePath")
            println("Reading Pact files from: $pactPath")
            println("Generating report at: $outputPath")
            println()

            val analyzer = SpoonWrapper(codePath)
            val reportWriter = JsonReportWriter()
            val app = ContractCoverageApp(analyzer, reportWriter)

            app.run(outputPath, pactPath)

            println("✓ Analysis complete!")
            println("✓ Report generated at: $outputPath")

            return CommandLine.ExitCode.OK
        } catch (e: IllegalArgumentException) {
            System.err.println("✗ Error: ${e.message}")
            return CommandLine.ExitCode.USAGE
        } catch (e: Exception) {
            System.err.println("✗ Error: ${e.message}")
            System.err.println("Exception type: ${e.javaClass.name}")
            // Always print stack trace for debugging in native binary
            e.printStackTrace()
            return CommandLine.ExitCode.SOFTWARE
        }
    }

    private fun validateInputs() {
        val codeDir = File(codePath)
        if (!codeDir.exists()) {
            throw IllegalArgumentException("Code path does not exist: $codePath")
        }
        if (!codeDir.isDirectory) {
            throw IllegalArgumentException("Code path is not a directory: $codePath")
        }

        val pactDir = File(pactPath)
        if (!pactDir.exists()) {
            throw IllegalArgumentException("Pact directory does not exist: $pactPath")
        }
        if (!pactDir.isDirectory) {
            throw IllegalArgumentException("Pact path is not a directory: $pactPath")
        }
        
        // Check if directory contains JSON files
        val jsonFiles = pactDir.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        } ?: emptyArray()
        
        if (jsonFiles.isEmpty()) {
            throw IllegalArgumentException("No JSON files found in Pact directory: $pactPath")
        }

        // Create output directory if it doesn't exist
        val outputFile = File(outputPath)
        val outputDir = outputFile.parentFile
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs()
        }
    }
}