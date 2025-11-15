package org.example

import org.example.infraestructure.interfaces.ContractCoverageCommand
import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        val command = ContractCoverageCommand()
        val commandLine = CommandLine(command)
        commandLine.isUnmatchedArgumentsAllowed = false
        commandLine.isStopAtUnmatched = false
        val exitCode = commandLine.execute(*args)
        exitProcess(exitCode)
    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}