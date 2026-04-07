package com.instagram.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

/**
 * Configures JWT-based authentication.
 *
 * Key concepts:
 * - Two token types: SHORT-LIVED access token (15 min) + LONG-LIVED refresh token (30 days).
 * - The JWT verifier only checks the access token here; refresh tokens have a DB check in AuthService.
 * - `validate {}` block extracts claims and rejects tokens without a valid userId.
 * - Routes wrap in `authenticate("jwt") {}` to require a valid token automatically.
 *
 * Claim layout:
 *   sub  → userId (UUID string)
 *   user → username (for display, not trust)
 *   type → "access" (prevents refresh tokens from being used as access tokens)
 */
fun Application.configureSecurity() {
    val secret   = environment.config.property("jwt.secret").getString()
    val issuer   = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val realm    = environment.config.property("jwt.realm").getString()

    install(Authentication) {
        jwt("jwt") {
            this.realm = realm
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .withClaim("type", "access") // Only accept access tokens
                    .build()
            )
            validate { credential ->
                val userId   = credential.payload.subject
                val tokenType = credential.payload.getClaim("type").asString()
                if (userId != null && tokenType == "access") {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("code" to 401, "message" to "Invalid or expired token")
                )
            }
        }
    }
}
