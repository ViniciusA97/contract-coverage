package org.example.core.entities.pact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Pact(
    val consumer: Consumer? = null,
    val provider: Provider? = null,
    val interactions: List<Interaction> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Consumer(
    val name: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Provider(
    val name: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Interaction(
    val description: String? = null,
    val providerState: String? = null,
    val providerStates: List<ProviderState>? = null,
    val request: Request = Request(),
    val response: Response = Response()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProviderState(
    val name: String? = null,
    val params: Map<String, Any>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Request(
    val method: String = "GET",
    val path: String = "",
    val query: Map<String, Any>? = null,
    val headers: Map<String, String>? = null,
    val body: Any? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Response(
    val status: Int = 200,
    val headers: Map<String, String>? = null,
    val body: Any? = null
)


