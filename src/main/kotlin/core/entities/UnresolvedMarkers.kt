package org.example.core.entities

/**
 * Constants for marking unresolved expressions during static analysis.
 */
object UnresolvedMarkers {
    const val UNRESOLVED_URL = "<dynamic-url>"
    const val UNRESOLVED_VARIABLE = "<dynamic-variable>"
    const val UNRESOLVED_EXPRESSION = "<dynamic-expression>"
    
    /**
     * Check if a resolved string contains any unresolved marker.
     */
    fun isUnresolved(value: String): Boolean {
        return value.isEmpty() || 
               value.contains(UNRESOLVED_URL) ||
               value.contains(UNRESOLVED_VARIABLE) ||
               value.contains(UNRESOLVED_EXPRESSION) ||
               value.contains("não resolvida") ||
               value.contains("não reconhecida") ||
               value.contains("{unknown}")
    }
    
    /**
     * Check if an endpoint path is unresolved (empty or contains markers).
     */
    fun isPathUnresolved(path: String): Boolean {
        return path.isBlank() || isUnresolved(path)
    }
}
