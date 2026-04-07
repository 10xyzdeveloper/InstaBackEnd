package com.instagram.features.user.application

import com.instagram.common.exceptions.ForbiddenException
import com.instagram.common.exceptions.NotFoundException
import com.instagram.features.user.domain.UserRepository
import com.instagram.features.user.dto.UpdateProfileRequest
import com.instagram.features.user.dto.UserProfileDto
import com.instagram.features.user.dto.UserSearchResult
import com.instagram.infrastructure.storage.StorageService
import io.ktor.http.content.*
import io.ktor.utils.io.toByteArray
import java.util.UUID

/**
 * UserService — orchestrates user-related use cases.
 *
 * Key things to teach:
 * - `getProfile` checks if the requesting user can see the profile.
 *   Private accounts only show stats to followers — enforced HERE, not in routes.
 * - Avatar upload goes through a StorageService abstraction, so switching
 *   from S3 to local disk for tests requires no changes to this class.
 */
class UserService(
    private val userRepo: UserRepository,
    private val storageService: StorageService
) {
    suspend fun getProfileByUsername(username: String, viewerId: UUID?): UserProfileDto {
        return userRepo.findByUsername(username, viewerId)
            ?: throw NotFoundException("User '$username' not found")
    }

    suspend fun getProfileById(userId: UUID, viewerId: UUID?): UserProfileDto {
        return userRepo.findById(userId, viewerId)
            ?: throw NotFoundException("User not found")
    }

    suspend fun updateProfile(userId: UUID, req: UpdateProfileRequest) {
        req.bio?.let       { userRepo.updateBio(userId, it) }
        req.isPrivate?.let { userRepo.updatePrivacy(userId, it) }
    }

    /**
     * Handles multipart avatar upload:
     *   1. Extract file part from multipart form data
     *   2. Validate MIME type
     *   3. Upload to storage (S3 / local)
     *   4. Persist URL in DB
     */
    suspend fun uploadAvatar(userId: UUID, multipart: MultiPartData) {
        var avatarUrl: String? = null

        multipart.forEachPart { part ->
            if (part is PartData.FileItem && part.name == "avatar") {
                val contentType = part.contentType?.toString() ?: ""
                require(contentType in listOf("image/jpeg", "image/png", "image/webp")) {
                    "Avatar must be JPEG, PNG, or WebP"
                }
                val bytes = part.provider().toByteArray()
                require(bytes.size <= 5 * 1024 * 1024) { "Avatar must be under 5MB" }

                avatarUrl = storageService.upload(
                    key         = "avatars/$userId",
                    bytes       = bytes,
                    contentType = contentType
                )
            }
            part.dispose()
        }

        avatarUrl?.let { userRepo.updateAvatar(userId, it) }
            ?: throw com.instagram.common.exceptions.BadRequestException("No avatar file provided")
    }

    suspend fun searchUsers(query: String): List<UserSearchResult> {
        if (query.length < 2) return emptyList()
        return userRepo.searchByUsername(query.trim(), limit = 20)
    }
}
