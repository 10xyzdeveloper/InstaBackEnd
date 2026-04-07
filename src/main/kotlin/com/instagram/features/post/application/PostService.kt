package com.instagram.features.post.application

import com.instagram.common.exceptions.BadRequestException
import com.instagram.common.exceptions.NotFoundException
import com.instagram.features.post.domain.PostRepository
import com.instagram.features.post.dto.PostDto
import com.instagram.infrastructure.storage.StorageService
import io.ktor.http.content.*
import io.ktor.utils.io.toByteArray
import java.util.UUID

/**
 * PostService — use cases for posts.
 *
 * Key learning: multipart handling.
 * Instagram allows up to 10 images/videos in a carousel.
 * We iterate through the multipart parts, upload each to S3,
 * then create the post record with all media URLs in a single transaction.
 */
class PostService(
    private val postRepo: PostRepository,
    private val storageService: StorageService
) {
    companion object {
        const val MAX_MEDIA_ITEMS = 10
        val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/webp", "video/mp4")
    }

    suspend fun createPost(userId: UUID, multipart: MultiPartData): PostDto {
        var caption: String? = null
        val mediaItems = mutableListOf<Pair<String, String>>()  // (s3Url, mediaType)

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    if (part.name == "caption") caption = part.value.take(2200)  // Instagram caption limit
                }
                is PartData.FileItem -> {
                    if (mediaItems.size >= MAX_MEDIA_ITEMS) {
                        part.dispose(); return@forEachPart
                    }
                    val contentType = part.contentType?.toString() ?: ""
                    if (contentType !in ALLOWED_TYPES) {
                        part.dispose()
                        throw BadRequestException("Unsupported media type: $contentType")
                    }
                    val bytes = part.provider().toByteArray()
                    if (bytes.size > 20 * 1024 * 1024) {
                        throw BadRequestException("Media file size must be under 20MB")
                    }
                    val key   = "posts/$userId/${System.currentTimeMillis()}_${mediaItems.size}"
                    val url   = storageService.upload(key, bytes, contentType)
                    val type  = if (contentType.startsWith("video")) "VIDEO" else "IMAGE"
                    mediaItems.add(url to type)
                }
                else -> Unit
            }
            part.dispose()
        }

        if (mediaItems.isEmpty()) throw BadRequestException("At least one media file is required")

        return postRepo.createPost(userId, caption, mediaItems)
    }

    suspend fun getPost(postId: UUID, viewerId: UUID?): PostDto {
        return postRepo.findById(postId, viewerId)
            ?: throw NotFoundException("Post not found")
    }

    suspend fun deletePost(postId: UUID, userId: UUID) {
        postRepo.deletePost(postId, userId)
    }

    suspend fun getUserPosts(userId: UUID, viewerId: UUID?, limit: Int = 12): List<PostDto> {
        return postRepo.getPostsByUser(userId, viewerId, null, limit)
    }
}
