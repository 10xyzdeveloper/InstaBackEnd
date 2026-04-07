package com.instagram.features.user.domain

import com.instagram.features.user.dto.UserProfileDto
import com.instagram.features.user.dto.UserSearchResult
import java.util.UUID

/**
 * UserRepository interface.
 *
 * Notice this interface contains NO Exposed or JDBC imports.
 * Repositories are pure Kotlin — the domain layer doesn't care how data is stored.
 */
interface UserRepository {
    suspend fun findById(userId: UUID, viewerId: UUID?): UserProfileDto?
    suspend fun findByUsername(username: String, viewerId: UUID?): UserProfileDto?
    suspend fun updateBio(userId: UUID, bio: String?)
    suspend fun updatePrivacy(userId: UUID, isPrivate: Boolean)
    suspend fun updateAvatar(userId: UUID, avatarUrl: String)
    suspend fun searchByUsername(query: String, limit: Int): List<UserSearchResult>
}
