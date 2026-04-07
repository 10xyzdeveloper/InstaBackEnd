package com.instagram.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

/**
 * Configures kotlinx.serialization with sensible defaults.
 *
 * - `ignoreUnknownKeys`: Clients can send extra fields without causing errors.
 *   Useful when the client SDK is ahead of the server version.
 * - `encodeDefaults`: Ensures null fields are included in the JSON response,
 *   making the contract explicit for clients.
 * - `prettyPrint`: Disabled in production for smaller payloads.
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint        = false
            isLenient          = true
            ignoreUnknownKeys  = true
            encodeDefaults     = true
            explicitNulls      = false
        })
    }
}
