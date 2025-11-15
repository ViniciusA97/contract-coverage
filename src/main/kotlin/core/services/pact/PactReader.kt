package org.example.core.services.pact

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.example.core.entities.pact.Pact
import java.io.File

class PactReader {
    private val objectMapper = ObjectMapper().registerModule(kotlinModule())

    fun read(filePath: String): Pact {
        val file = File(filePath)
        require(file.exists()) { "Pact file not found at: $filePath" }
        require(file.isFile) { "Pact path must be a file: $filePath" }
        return objectMapper.readValue(file, Pact::class.java)
    }

    fun readDirectory(directoryPath: String): List<Pact> {
        val directory = File(directoryPath)
        require(directory.exists()) { "Pact directory not found at: $directoryPath" }
        require(directory.isDirectory) { "Pact path must be a directory: $directoryPath" }
        
        val jsonFiles = directory.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        } ?: emptyArray()
        
        require(jsonFiles.isNotEmpty()) { "No JSON files found in directory: $directoryPath" }
        
        return jsonFiles.map { file ->
            try {
                objectMapper.readValue(file, Pact::class.java)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to read Pact file ${file.name}: ${e.message}", e)
            }
        }
    }
}


