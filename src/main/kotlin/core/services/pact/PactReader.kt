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
        return objectMapper.readValue(file, Pact::class.java)
    }
}


