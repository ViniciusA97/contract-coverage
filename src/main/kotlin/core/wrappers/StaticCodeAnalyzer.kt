package org.example.core.wrappers

import org.example.core.entities.Endpoint

interface StaticCodeAnalyzer {
    fun analyzeInvocations(): List<Endpoint>
}