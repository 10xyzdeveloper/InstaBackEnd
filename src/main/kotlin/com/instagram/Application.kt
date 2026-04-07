package com.instagram

import com.instagram.plugins.*
import io.ktor.server.application.*

/**
 * Application entry point.
 *
 * Ktor calls this function when the server starts (configured in application.conf).
 * We follow the "extension functions as plugins" pattern to keep each concern isolated.
 *
 * Order matters:
 *   1. Koin (DI must be first — routes depend on injected services)
 *   2. Database (Flyway migrations run here, must finish before routes up)
 *   3. Security (JWT config)
 *   4. Serialization
 *   5. WebSockets
 *   6. Routing (installs all feature routes)
 *   7. StatusPages (error handling)
 *   8. HTTP utilities (CORS, call logging, request validation)
 */
fun Application.module() {
    configureDI()
    configureDatabase()
    configureSecurity()
    configureSerialization()
    configureSockets()
    configureRouting()
    configureStatusPages()
    configureHTTP()
}
