package org.example.core.services.filedetection

import java.io.File

/**
 * Result of a project scan for HTTP client files.
 */
data class ScanResult(
    val clientFiles: List<File>,
    val callerFiles: List<File>,
    val allRelevantFiles: List<File>
) {
    val totalFiles: Int get() = allRelevantFiles.size
    val clientFileCount: Int get() = clientFiles.size
    val callerFileCount: Int get() = callerFiles.size
}

/**
 * Scans a project directory to find files relevant for HTTP client analysis.
 * 
 * Uses a two-phase approach:
 * 1. Find files containing HTTP client usage (fast text search)
 * 2. Find files that call/reference those files (for context resolution)
 */
class ProjectFileScanner(
    private val detectors: List<ClientFileDetector>,
    private val callerResolver: CallerResolver = CallerResolver()
) {
    
    /**
     * Scan a project directory and return relevant files.
     * 
     * @param projectDir The root directory to scan
     * @param callerDepth How many levels of callers to include (0 = only client files)
     * @return ScanResult with categorized files
     */
    fun scan(projectDir: File, callerDepth: Int = 1): ScanResult {
        require(projectDir.isDirectory) { "Project path must be a directory: ${projectDir.absolutePath}" }
        
        // Collect all Java files
        val allJavaFiles = projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()
        
        if (allJavaFiles.isEmpty()) {
            throw RuntimeException("No Java files found in: ${projectDir.absolutePath}")
        }
        
        // Phase 1: Find files containing HTTP clients
        val clientFiles = allJavaFiles.filter { file ->
            detectors.any { detector -> detector.mightContainClient(file) }
        }
        
        if (clientFiles.isEmpty()) {
            val detectorNames = detectors.joinToString(", ") { it.clientName }
            throw RuntimeException("No files containing HTTP clients ($detectorNames) found in: ${projectDir.absolutePath}")
        }
        
        // Phase 2: Find callers if depth > 0
        val allRelevantFiles = if (callerDepth > 0) {
            callerResolver.findCallers(clientFiles, allJavaFiles, callerDepth)
        } else {
            clientFiles.toSet()
        }
        
        val callerFiles = allRelevantFiles - clientFiles.toSet()
        
        return ScanResult(
            clientFiles = clientFiles,
            callerFiles = callerFiles.toList(),
            allRelevantFiles = allRelevantFiles.toList()
        )
    }
    
    /**
     * Print scan statistics to stderr (for CLI feedback)
     */
    fun printScanStats(result: ScanResult) {
        val detectorNames = detectors.joinToString(", ") { it.clientName }
        System.err.println("Scan complete:")
        System.err.println("  - Files with HTTP clients ($detectorNames): ${result.clientFileCount}")
        System.err.println("  - Caller files (for context): ${result.callerFileCount}")
        System.err.println("  - Total files to analyze: ${result.totalFiles}")
    }
}
