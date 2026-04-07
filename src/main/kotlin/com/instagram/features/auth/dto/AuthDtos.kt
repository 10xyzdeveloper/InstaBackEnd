package com.instagram.features.auth.dto

import kotlinx.serialization.Serializable

/**
 * DTOs (Data Transfer Objects) for Auth.
 *
 * Teaching note:
 * - DTOs are what crosses the HTTP boundary. They are NEVER the same as domain entities.
 * - Reason: domain entities may have sensitive fields (password hash) or DB internals
 *   (createdAt, updatedAt) you never want to send over the wire.
 * - Keep DTOs in the `dto/` package of each feature.
 */

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long   // seconds until access token expires
)
