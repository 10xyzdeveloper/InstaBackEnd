package com.instagram.features.post.domain

import com.instagram.features.post.dto.PostDto
import java.util.UUID

interface PostRepository {
    suspend fun createPost(userId: UUID, caption: String?, mediaItems: List<Pair<String, String>>): PostDto
    suspend fun findById(postId: UUID, viewerId: UUID?): PostDto?
    suspend fun deletePost(postId: UUID, requestingUserId: UUID)
    suspend fun extractAndSaveHashtags(caption: String, postId: UUID)
    suspend fun getPostsByUser(userId: UUID, viewerId: UUID?, cursor: Pair<String, String>?, limit: Int): List<PostDto>
}
