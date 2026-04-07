package com.instagram.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.requestvalidation.*

/**
 * Configures HTTP-level concerns:
 *   - CORS: allows mobile/web clients from any origin during development.
 *   - RequestValidation: declarative input validation before the route handler runs.
 *
 * Note: CallLogging is installed separately as it has its own Ktor plugin import.
 */
fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowCredentials = true
        maxAgeInSeconds  = 3600
    }

    install(RequestValidation) {
        // Feature validators registered per-route
    }
}
