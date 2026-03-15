package org.example.core.services.filedetection

import java.io.File

/**
 * Detects files that contain RestTemplate usage.
 */
class RestTemplateFileDetector : ClientFileDetector {
    
    override val clientName: String = "RestTemplate"
    
    private val patterns = listOf(
        "RestTemplate",
        "restTemplate"
    )
    
    override fun mightContainClient(file: File): Boolean {
        return try {
            file.useLines { lines ->
                lines.any { line ->
                    patterns.any { pattern -> line.contains(pattern) }
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getDetectionPatterns(): List<String> = patterns
}
