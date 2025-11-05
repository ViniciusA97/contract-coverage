package org.example.core.services.coverage

import org.example.core.entities.Endpoint
import org.example.core.entities.Coverage
import kotlin.math.round

class EndpointsComparator {
    fun compare(codeEndpoints: List<Endpoint>, pactEndpoints: List<Endpoint>): Coverage {
        val codeSet = codeEndpoints.distinctBy { it.path to it.method }.toSet()
        val pactSet = pactEndpoints.distinctBy { it.path to it.method }.toSet()

        val matched = mutableListOf<Endpoint>()
        val missing = mutableListOf<Endpoint>()
        
        for (codeEndpoint in codeSet) {
            val found = pactSet.find { pactEndpoint ->
                pactEndpoint.method == codeEndpoint.method && 
                pathsMatch(codeEndpoint.path, pactEndpoint.path)
            }
            
            if (found != null) {
                matched.add(codeEndpoint)
            } else {
                missing.add(codeEndpoint)
            }
        }

        val total = codeSet.size
        val matchedCount = matched.size
        val coverage = if (total == 0) 100.0 else (matchedCount.toDouble() / total.toDouble()) * 100.0
        val coverageRounded = round(coverage * 100.0) / 100.0

        return Coverage(
            totalCodeEndpoints = total,
            matchedByPact = matchedCount,
            coveragePercent = coverageRounded,
            missingEndpoints = missing,
            matchedEndpoints = matched
        )
    }
    
    /**
     * Checks if two paths match, considering path variables.
     * Examples:
     * - /users/{id} matches /users/999
     * - /users/{userId}/posts/{postId} matches /users/123/posts/456
     * - /users/{id} does not match /users/123/posts
     */
    private fun pathsMatch(codePath: String, pactPath: String): Boolean {
        // Exact match
        if (codePath == pactPath) return true
        
        // Normalize paths by splitting into segments
        val codeSegments = codePath.split('/').filter { it.isNotEmpty() }
        val pactSegments = pactPath.split('/').filter { it.isNotEmpty() }
        
        // Must have same number of segments
        if (codeSegments.size != pactSegments.size) return false
        
        // Compare each segment
        for (i in codeSegments.indices) {
            val codeSegment = codeSegments[i]
            val pactSegment = pactSegments[i]
            
            // If code segment is a path variable (e.g., {id}), it matches any value
            if (codeSegment.startsWith("{") && codeSegment.endsWith("}")) {
                // Path variable matches any value
                continue
            }
            
            // If pact segment looks like a path variable but code doesn't, no match
            // (This handles the case where Pact has {id} but code has a specific value)
            if (pactSegment.startsWith("{") && pactSegment.endsWith("}")) {
                // Pact has variable but code has literal - check if they're the same variable name
                if (codeSegment != pactSegment) {
                    return false
                }
                continue
            }
            
            // Both are literals - must match exactly
            if (codeSegment != pactSegment) {
                return false
            }
        }
        
        return true
    }
}


