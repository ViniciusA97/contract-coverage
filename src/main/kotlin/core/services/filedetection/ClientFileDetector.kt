package org.example.core.services.filedetection

import java.io.File

/**
 * Interface for detecting files that contain HTTP client usage.
 * Implementations can detect different client types (RestTemplate, WebClient, Feign, etc.)
 */
interface ClientFileDetector {
    /**
     * The name of the client this detector handles (for logging purposes)
     */
    val clientName: String
    
    /**
     * Quick check if a file potentially contains the client usage.
     * This should be a fast text-based check, not a full parse.
     * 
     * @param file The Java file to check
     * @return true if the file might contain client usage
     */
    fun mightContainClient(file: File): Boolean
    
    /**
     * Get the patterns used for detection (for logging/debugging)
     */
    fun getDetectionPatterns(): List<String>
}
