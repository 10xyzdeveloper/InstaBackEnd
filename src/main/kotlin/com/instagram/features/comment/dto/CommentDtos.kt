package com.instagram.features.comment.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    val id: String,
    val postId: String,
    val author: CommentAuthorDto,
    val body: String,
    val likeCount: Long,
    val replyCount: Long,
    val isLiked: Boolean = false,
    val createdAt: String
)

@Serializable
data class CommentAuthorDto(
    val id: String,
    val username: String,
    val avatarUrl: String?
)

@Serializable
data class CreateCommentRequest(
    val body: String,
    val parentId: String? = null  // for replies
)
