package org.example

import org.example.cli.ContractCoverageCommand
import picocli.CommandLine

fun main(args: Array<String>) {
    val exitCode = CommandLine(ContractCoverageCommand()).execute(*args)
    System.exit(exitCode)
}