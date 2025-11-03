package org.example.core.services.coverage

import org.example.core.entities.Endpoint
import org.example.core.entities.Coverage
import kotlin.math.round

class EndpointsComparator {
    fun compare(codeEndpoints: List<Endpoint>, pactEndpoints: List<Endpoint>): Coverage {
        val codeSet = codeEndpoints.distinctBy { it.path to it.method }.toSet()
        val pactSet = pactEndpoints.distinctBy { it.path to it.method }.toSet()

        val matched = codeSet.intersect(pactSet)
        val missing = codeSet.minus(pactSet)

        val total = codeSet.size
        val matchedCount = matched.size
        val coverage = if (total == 0) 100.0 else (matchedCount.toDouble() / total.toDouble()) * 100.0
        val coverageRounded = round(coverage * 100.0) / 100.0

        return Coverage(
            totalCodeEndpoints = total,
            matchedByPact = matchedCount,
            coveragePercent = coverageRounded,
            missingEndpoints = missing.toList(),
            matchedEndpoints = matched.toList()
        )
    }
}


