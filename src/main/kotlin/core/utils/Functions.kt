package org.example.core.utils

import java.net.URI

fun urlToPath(url: String): String {
    if(url.startsWith("{String}"))
        return URI.create("http://localhost:8080${url.substring(8)}").path
    return URI.create(url).path
}