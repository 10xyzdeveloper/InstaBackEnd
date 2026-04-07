package com.instagram.features.user.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    val bio: String?,
    val avatarUrl: String?,
    val isPrivate: Boolean,
    val postCount: Long,
    val followerCount: Long,
    val followingCount: Long,
    val isFollowing: Boolean = false,   // populated relative to requesting user
    val followStatus: String? = null    // "ACTIVE" | "PENDING" | null
)

@Serializable
data class UpdateProfileRequest(
    val bio: String? = null,
    val isPrivate: Boolean? = null
)

@Serializable
data class UserSearchResult(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val isVerified: Boolean = false
)
