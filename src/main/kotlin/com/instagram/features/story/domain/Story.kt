package com.instagram.features.story.domain

import kotlinx.datetime.LocalDateTime
import java.util.UUID

data class Story(
    val id: UUID,
    val userId: UUID,
    val mediaUrl: String,
    val mediaType: String,
    val caption: String?,
    val createdAt: LocalDateTime
)
