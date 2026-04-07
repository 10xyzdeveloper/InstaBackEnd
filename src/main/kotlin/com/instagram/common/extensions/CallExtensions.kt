package com.instagram.common.extensions

import com.instagram.common.exceptions.UnauthorizedException
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

/**
 * Extension functions on ApplicationCall.
 *
 * These helpers centralise common call-level operations so routes
 * remain concise and don't repeat boilerplate.
 */

/**
 * Extracts the authenticated user's UUID from the JWT principal.
 *
 * Usage in routes:
 *   val userId = call.requireUserId()
 *
 * Throws [UnauthorizedException] if the principal is missing — this
 * should never happen inside an `authenticate {}` block, but acts
 * as a safety net for routes accidentally left unauthenticated.
 */
fun io.ktor.server.application.ApplicationCall.requireUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException("Missing authentication principal")
    val subject = principal.payload.subject
        ?: throw UnauthorizedException("Missing subject claim in token")
    return runCatching { UUID.fromString(subject) }.getOrElse {
        throw UnauthorizedException("Invalid user ID in token")
    }
}

/**
 * Extracts the authenticated user's username from the JWT principal.
 * Used for display purposes only — never for authorization decisions.
 */
fun io.ktor.server.application.ApplicationCall.requireUsername(): String {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException("Missing authentication principal")
    return principal.payload.getClaim("user").asString()
        ?: throw UnauthorizedException("Missing username claim in token")
}
