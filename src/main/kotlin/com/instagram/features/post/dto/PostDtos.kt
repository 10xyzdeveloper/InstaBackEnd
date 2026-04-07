package com.instagram.features.post.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    val id: String,
    val author: PostAuthorDto,
    val caption: String?,
    val media: List<MediaDto>,
    val likeCount: Long,
    val commentCount: Long,
    val isLiked: Boolean = false,
    val createdAt: String
)

@Serializable
data class PostAuthorDto(
    val id: String,
    val username: String,
    val avatarUrl: String?
)

@Serializable
data class MediaDto(
    val url: String,
    val type: String,  // "IMAGE" | "VIDEO"
    val position: Int
)
