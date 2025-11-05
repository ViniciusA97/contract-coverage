package org.example.core.utils

import java.net.URI

fun urlToPath(url: String): String {
    // Handle special case where URL starts with {String}
    if(url.startsWith("{String}"))
        return extractPathFromUrl("http://localhost:8080${url.substring(8)}")
    
    return extractPathFromUrl(url)
}

private fun extractPathFromUrl(url: String): String {
    // If URL contains path variables (curly braces), extract path manually
    if (url.contains("{") && url.contains("}")) {
        return extractPathManually(url)
    }
    
    // Try to use URI.create() for normal URLs
    return try {
        URI.create(url).path
    } catch (e: IllegalArgumentException) {
        // If URI.create() fails, extract path manually
        extractPathManually(url)
    }
}

private fun extractPathManually(url: String): String {
    // Remove protocol and domain to get path
    val urlWithoutProtocol = when {
        url.startsWith("http://") -> url.substring(7)
        url.startsWith("https://") -> url.substring(8)
        else -> url
    }
    
    // Find the first '/' after the domain (if any)
    val firstSlashIndex = urlWithoutProtocol.indexOf('/')
    if (firstSlashIndex == -1) {
        // No path found, return empty string
        return ""
    }
    
    // Extract path starting from the first '/'
    val pathWithQuery = urlWithoutProtocol.substring(firstSlashIndex)
    
    // Remove query parameters if present
    val queryIndex = pathWithQuery.indexOf('?')
    val path = if (queryIndex != -1) {
        pathWithQuery.substring(0, queryIndex)
    } else {
        pathWithQuery
    }
    
    // If path doesn't start with '/', add it
    return if (path.startsWith("/")) path else "/$path"
}