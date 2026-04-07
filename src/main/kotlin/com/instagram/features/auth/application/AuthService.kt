package com.instagram.features.auth.application

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.instagram.common.exceptions.ConflictException
import com.instagram.common.exceptions.UnauthorizedException
import com.instagram.features.auth.domain.AuthRepository
import com.instagram.features.auth.dto.LoginRequest
import com.instagram.features.auth.dto.RegisterRequest
import com.instagram.features.auth.dto.TokenResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.mindrot.jbcrypt.BCrypt
import java.util.*

/**
 * AuthService — pure application service with no Ktor or Exposed imports.
 *
 * Config values are passed via constructor so this class is easily testable.
 *
 * Teaching notes — why constructor injection instead of reading config here?
 * - AuthService is a use-case class; it should not know about Ktor's config system.
 * - Passing values in makes unit testing trivial: just pass strings.
 * - Koin's DI module reads the config once and injects the values.
 */
class AuthService(
    private val authRepo: AuthRepository,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String,
    private val accessExpiry: Long,    // seconds
    private val refreshExpiry: Long    // seconds
) {
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    suspend fun register(req: RegisterRequest): TokenResponse {
        validateRegistration(req)
        val passwordHash = BCrypt.hashpw(req.password, BCrypt.gensalt(12))
        val userId = runCatching {
            authRepo.createUser(req.username.lowercase().trim(), req.email.lowercase().trim(), passwordHash)
        }.getOrElse { e ->
            if (e.message?.contains("unique", ignoreCase = true) == true ||
                e.message?.contains("duplicate", ignoreCase = true) == true) {
                throw ConflictException("Username or email is already taken")
            }
            throw e
        }
        return issueTokenPair(userId, req.username)
    }

    suspend fun login(req: LoginRequest): TokenResponse {
        val (userId, hash) = authRepo.findPasswordHashByEmail(req.email.lowercase().trim())
            ?: throw UnauthorizedException("Invalid email or password")
        if (!BCrypt.checkpw(req.password, hash)) throw UnauthorizedException("Invalid email or password")
        return issueTokenPair(userId, req.email)
    }

    suspend fun refresh(refreshToken: String): TokenResponse {
        val userId = authRepo.findUserIdByRefreshToken(refreshToken)
            ?: throw UnauthorizedException("Refresh token is invalid or expired")
        authRepo.deleteRefreshToken(refreshToken)
        return issueTokenPair(userId, userId.toString())
    }

    suspend fun logout(refreshToken: String) = authRepo.deleteRefreshToken(refreshToken)
    suspend fun logoutAll(userId: UUID)       = authRepo.deleteAllRefreshTokensForUser(userId)

    private suspend fun issueTokenPair(userId: UUID, username: String): TokenResponse {
        val now          = Clock.System.now()
        val accessExpAt  = now.plus(accessExpiry, DateTimeUnit.SECOND)
        val refreshExpAt = now.plus(refreshExpiry, DateTimeUnit.SECOND)

        val accessToken = JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withSubject(userId.toString())
            .withClaim("user", username)
            .withClaim("type", "access")
            .withExpiresAt(Date(accessExpAt.toEpochMilliseconds()))
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withSubject(userId.toString())
            .withClaim("type", "refresh")
            .withExpiresAt(Date(refreshExpAt.toEpochMilliseconds()))
            .sign(algorithm)

        val refreshExpLocal = refreshExpAt.toLocalDateTime(TimeZone.UTC)
        authRepo.saveRefreshToken(userId, refreshToken, refreshExpLocal)

        return TokenResponse(
            accessToken  = accessToken,
            refreshToken = refreshToken,
            expiresIn    = accessExpiry
        )
    }

    private fun validateRegistration(req: RegisterRequest) {
        require(req.username.length in 3..30) { "Username must be 3–30 characters" }
        require(req.username.matches(Regex("^[a-zA-Z0-9_.]+$"))) {
            "Username may only contain letters, numbers, underscores, and dots"
        }
        require(req.email.contains("@")) { "Invalid email address" }
        require(req.password.length >= 8) { "Password must be at least 8 characters" }
    }
}
