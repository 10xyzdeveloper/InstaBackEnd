package com.instagram.features.story.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateStoryRequest(
    val mediaUrl: String,
    val mediaType: String,
    val caption: String? = null
)

@Serializable
data class StoryDto(
    val id: String,
    val userId: String,
    val author: StoryAuthorDto,
    val mediaUrl: String,
    val mediaType: String,
    val caption: String?,
    val createdAt: String
)

@Serializable
data class StoryAuthorDto(
    val id: String,
    val username: String,
    val avatarUrl: String?
)
