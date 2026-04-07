package com.instagram.features.auth.domain

import java.util.UUID

/**
 * The Auth repository interface lives in the DOMAIN layer.
 *
 * Teaching note — Why an interface here?
 * - The Application layer (AuthService) depends on THIS interface, not the implementation.
 * - This means AuthService has zero knowledge of Exposed, PostgreSQL, or any DB library.
 * - In tests, we can swap in a fake/mock implementation without touching the service.
 * - This is the Dependency Inversion Principle (the D in SOLID).
 */
interface AuthRepository {

    /** Returns the newly created user's UUID, or throws ConflictException if taken. */
    suspend fun createUser(username: String, email: String, passwordHash: String): UUID

    /** Returns the password hash for the given email, or null if user doesn't exist. */
    suspend fun findPasswordHashByEmail(email: String): Pair<UUID, String>?

    /** Stores a refresh token associated with a user. */
    suspend fun saveRefreshToken(userId: UUID, token: String, expiresAt: kotlinx.datetime.LocalDateTime)

    /** Returns the userId for a valid (non-expired) refresh token, or null. */
    suspend fun findUserIdByRefreshToken(token: String): UUID?

    /** Invalidates a specific refresh token (logout / rotation). */
    suspend fun deleteRefreshToken(token: String)

    /** Invalidates ALL refresh tokens for a user (logout all devices). */
    suspend fun deleteAllRefreshTokensForUser(userId: UUID)
}
