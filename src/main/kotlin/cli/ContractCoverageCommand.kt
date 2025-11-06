package org.example.cli

import org.example.ContractCoverageApp
import org.example.core.services.reports.JsonReportWriter
import org.example.core.wrappers.spoon.SpoonWrapper
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.util.concurrent.Callable

@Command(
    name = "contract-coverage",
    description = ["Analyzes Java code for RestTemplate calls and compares with Pact contracts"],
    mixinStandardHelpOptions = true,
    version = ["1.0-SNAPSHOT"]
)
class ContractCoverageCommand : Callable<Int> {

    @Parameters(
        index = "0",
        description = ["Path to the Java source code directory to analyze"],
        paramLabel = "<code-path>"
    )
    private lateinit var codePath: String

    @Parameters(
        index = "1",
        description = ["Path to the Pact JSON file"],
        paramLabel = "<pact-file>"
    )
    private lateinit var pactPath: String

    @Option(
        names = ["-o", "--output"],
        description = ["Output path for the coverage report (default: ./reports/report.json)"],
        defaultValue = "./reports/report.json"
    )
    private var outputPath: String = "./reports/report.json"

    override fun call(): Int {
        try {
            // Validate inputs
            validateInputs()

            println("Analyzing code at: $codePath")
            println("Reading Pact file: $pactPath")
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
            if (System.getProperty("verbose") != null) {
                e.printStackTrace()
            }
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

        val pactFile = File(pactPath)
        if (!pactFile.exists()) {
            throw IllegalArgumentException("Pact file does not exist: $pactPath")
        }
        if (!pactFile.isFile) {
            throw IllegalArgumentException("Pact path is not a file: $pactPath")
        }
        if (!pactPath.endsWith(".json", ignoreCase = true)) {
            throw IllegalArgumentException("Pact file must be a JSON file: $pactPath")
        }

        // Create output directory if it doesn't exist
        val outputFile = File(outputPath)
        val outputDir = outputFile.parentFile
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs()
        }
    }
}

