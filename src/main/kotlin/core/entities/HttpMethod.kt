package org.example.core.entities

enum class HttpMethod(val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE");
    
    companion object {
        fun fromString(method: String): HttpMethod? {
            return values().find { it.value.equals(method, ignoreCase = true) }
        }
        
        fun fromStringOrDefault(method: String, default: HttpMethod = POST): HttpMethod {
            return fromString(method) ?: default
        }
    }
}
