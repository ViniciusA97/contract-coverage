package org.example.infraestructure.interfaces

import org.example.ContractCoverageApp
import org.example.core.entities.UnresolvedMarkers
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

    @CommandLine.Option(
        names = ["-d", "--dry-run"],
        description = ["Run in dry-run mode (always returns exit code 0, even on errors)"],
        defaultValue = "false"
    )
    private var dryRun: Boolean = false

    @CommandLine.Option(
        names = ["-t", "--threshold"],
        description = ["Minimum coverage percentage required (0-100). Fails if coverage is below threshold."],
        paramLabel = "<percentage>"
    )
    private var threshold: Double? = null

    override fun call(): Int {
        try {
            printBanner()
            
            if (dryRun) {
                println("Running in DRY-RUN mode (exit code will always be 0)")
                println()
            }
            
            // Validate inputs
            validateInputs()

            val analyzer = SpoonWrapper(codePath)
            val reportWriter = JsonReportWriter()
            val app = ContractCoverageApp(analyzer, reportWriter)

            val coverage = app.run(outputPath, pactPath)

            // Log coverage information
            println("Coverage: ${String.format("%.2f", coverage.coveragePercent)}%")
            println("Total endpoints: ${coverage.totalCodeEndpoints}")
            println("Matched: ${coverage.matchedByPact}")
            println("Missing: ${coverage.totalCodeEndpoints - coverage.matchedByPact}")
            println()
            
            // Separate resolved and unresolved endpoints
            val (unresolvedEndpoints, resolvedMissing) = coverage.missingEndpoints.partition { 
                UnresolvedMarkers.isPathUnresolved(it.path) 
            }
            
            // Log matched endpoints
            if (coverage.matchedEndpoints.isNotEmpty()) {
                println("Matched endpoints:")
                coverage.matchedEndpoints.forEach { endpoint ->
                    println("  ✓ ${endpoint.method.value} ${endpoint.path}")
                }
                println()
            }
            
            // Log missing endpoints (resolved URLs only)
            if (resolvedMissing.isNotEmpty()) {
                println("Missing endpoints:")
                resolvedMissing.forEach { endpoint ->
                    println("  ✗ ${endpoint.method.value} ${endpoint.path}")
                }
                println()
            }
            
            // Log unresolved endpoints (dynamic URLs)
            if (unresolvedEndpoints.isNotEmpty()) {
                println(yellow("Unresolved endpoints (dynamic URLs - cannot be analyzed statically):"))
                unresolvedEndpoints.forEach { endpoint ->
                    println("  ⚠ ${endpoint.method.value} ${endpoint.path}")
                }
                println()
            }
            
            println("Report generated at: $outputPath")
            println()

            // Check threshold if specified
            var exitCode = CommandLine.ExitCode.OK
            if (threshold != null) {
                val thresholdValue = threshold!!
                if (thresholdValue < 0 || thresholdValue > 100) {
                    System.err.println("${red("Error:")} Threshold must be between 0 and 100")
                    return if (dryRun) {
                        CommandLine.ExitCode.OK
                    } else {
                        CommandLine.ExitCode.USAGE
                    }
                }

                val thresholdMet = coverage.coveragePercent >= thresholdValue
                if (thresholdMet) {
                    println(green("✓ Threshold met: ${String.format("%.2f", coverage.coveragePercent)}% >= ${String.format("%.2f", thresholdValue)}%"))
                } else {
                    println(red("✗ Threshold not met: ${String.format("%.2f", coverage.coveragePercent)}% < ${String.format("%.2f", thresholdValue)}%"))
                    if (!dryRun) {
                        exitCode = CommandLine.ExitCode.SOFTWARE  // Exit code 1: threshold not met
                    }
                }
            }

            return exitCode
        } catch (e: IllegalArgumentException) {
            System.err.println("Error: ${e.message}")
            return if (dryRun) {
                CommandLine.ExitCode.OK  // In dry-run mode, always return success
            } else {
                CommandLine.ExitCode.USAGE  // Exit code 2: usage error
            }
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            return if (dryRun) {
                CommandLine.ExitCode.OK  // In dry-run mode, always return success
            } else {
                CommandLine.ExitCode.SOFTWARE  // Exit code 1: software error
            }
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

    private fun printBanner() {
        val banner = """
            
            ╔═══════════════════════════════════════════════════════════════╗
            ║                                                               ║
            ║                    Contract Coverage                          ║
            ║                                                               ║
            ║                     v1.0-SNAPSHOT                             ║
            ║                                                               ║
            ╚═══════════════════════════════════════════════════════════════╝
            
        """.trimIndent()
        println(banner)
    }

    private fun green(text: String): String {
        // Check if we're in a terminal that supports colors
        val isTerminal = System.console() != null || 
                        System.getenv("TERM") != null ||
                        System.getProperty("java.class.path", "").contains("gradle")
        
        return if (isTerminal && System.getProperty("NO_COLOR", "").isEmpty()) {
            "\u001B[32m$text\u001B[0m"  // ANSI green
        } else {
            text  // No color if not in terminal or NO_COLOR is set
        }
    }

    private fun red(text: String): String {
        // Check if we're in a terminal that supports colors
        val isTerminal = System.console() != null || 
                        System.getenv("TERM") != null ||
                        System.getProperty("java.class.path", "").contains("gradle")
        
        return if (isTerminal && System.getProperty("NO_COLOR", "").isEmpty()) {
            "\u001B[31m$text\u001B[0m"  // ANSI red
        } else {
            text  // No color if not in terminal or NO_COLOR is set
        }
    }

    private fun yellow(text: String): String {
        // Check if we're in a terminal that supports colors
        val isTerminal = System.console() != null || 
                        System.getenv("TERM") != null ||
                        System.getProperty("java.class.path", "").contains("gradle")
        
        return if (isTerminal && System.getProperty("NO_COLOR", "").isEmpty()) {
            "\u001B[33m$text\u001B[0m"  // ANSI yellow
        } else {
            text  // No color if not in terminal or NO_COLOR is set
        }
    }
}