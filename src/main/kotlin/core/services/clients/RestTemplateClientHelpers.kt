package org.example.core.services.clients

class RestTemplateClientHelpers: ClientHelpers {
    private val type = "org.springframework.web.client.RestTemplate"

    override fun type(): String {
        return type
    }
}