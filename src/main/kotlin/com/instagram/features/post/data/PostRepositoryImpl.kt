package com.instagram.features.post.data

import com.instagram.common.exceptions.ForbiddenException
import com.instagram.common.exceptions.NotFoundException
import com.instagram.features.post.domain.PostRepository
import com.instagram.features.post.dto.MediaDto
import com.instagram.features.post.dto.PostAuthorDto
import com.instagram.features.post.dto.PostDto
import com.instagram.infrastructure.database.tables.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class PostRepositoryImpl : PostRepository {

    override suspend fun createPost(
        userId: UUID,
        caption: String?,
        mediaItems: List<Pair<String, String>>
    ): PostDto = newSuspendedTransaction {
        val postId = PostsTable.insert {
            it[PostsTable.userId]  = userId
            it[PostsTable.caption] = caption
        }[PostsTable.id]

        mediaItems.forEachIndexed { index, (url, type) ->
            PostMediaTable.insert {
                it[PostMediaTable.postId]    = postId
                it[PostMediaTable.url]       = url
                it[PostMediaTable.mediaType] = type
                it[PostMediaTable.position]  = index
            }
        }

        caption?.let { extractHashtags(it, postId) }
        findByIdInternal(postId, userId)!!
    }

    override suspend fun findById(postId: UUID, viewerId: UUID?): PostDto? =
        newSuspendedTransaction { findByIdInternal(postId, viewerId) }

    private fun findByIdInternal(postId: UUID, viewerId: UUID?): PostDto? {
        val postRow = (PostsTable innerJoin UsersTable)
            .select(PostsTable.columns + UsersTable.columns)
            .where { (PostsTable.id eq postId) and PostsTable.deletedAt.isNull() }
            .singleOrNull() ?: return null

        val media = PostMediaTable
            .select(PostMediaTable.url, PostMediaTable.mediaType, PostMediaTable.position)
            .where { PostMediaTable.postId eq postId }
            .orderBy(PostMediaTable.position)
            .map { MediaDto(it[PostMediaTable.url], it[PostMediaTable.mediaType], it[PostMediaTable.position]) }

        val likeCount = LikesTable.select(LikesTable.userId.count())
            .where { LikesTable.postId eq postId }
            .single()[LikesTable.userId.count()]

        val commentCount = CommentsTable.select(CommentsTable.id.count())
            .where { (CommentsTable.postId eq postId) and CommentsTable.deletedAt.isNull() }
            .single()[CommentsTable.id.count()]

        val isLiked = viewerId?.let {
            LikesTable.select(LikesTable.userId)
                .where { (LikesTable.userId eq it) and (LikesTable.postId eq postId) }
                .count() > 0
        } ?: false

        return PostDto(
            id           = postId.toString(),
            author       = PostAuthorDto(
                id        = postRow[UsersTable.id].toString(),
                username  = postRow[UsersTable.username],
                avatarUrl = postRow[UsersTable.avatarUrl]
            ),
            caption      = postRow[PostsTable.caption],
            media        = media,
            likeCount    = likeCount,
            commentCount = commentCount,
            isLiked      = isLiked,
            createdAt    = postRow[PostsTable.createdAt].toString()
        )
    }

    override suspend fun deletePost(postId: UUID, requestingUserId: UUID): Unit =
        newSuspendedTransaction {
            val ownerId = PostsTable
                .select(PostsTable.userId)
                .where { PostsTable.id eq postId }
                .singleOrNull()?.get(PostsTable.userId)
                ?: throw NotFoundException("Post not found")

            if (ownerId != requestingUserId) throw ForbiddenException("You can only delete your own posts")

            PostsTable.update({ PostsTable.id eq postId }) {
                it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }

    override suspend fun extractAndSaveHashtags(caption: String, postId: UUID) {
        newSuspendedTransaction { extractHashtags(caption, postId) }
    }

    private fun extractHashtags(caption: String, postId: UUID) {
        val tags = Regex("#(\\w+)").findAll(caption).map { it.groupValues[1].lowercase() }.toSet()
        tags.forEach { tag ->
            val existingId = HashtagsTable
                .select(HashtagsTable.id)
                .where { HashtagsTable.name eq tag }
                .singleOrNull()
                ?.get(HashtagsTable.id)

            val hashtagId = existingId ?: HashtagsTable.insert {
                it[HashtagsTable.name] = tag
            }[HashtagsTable.id]

            val alreadyLinked = PostHashtagsTable
                .select(PostHashtagsTable.postId)
                .where { (PostHashtagsTable.postId eq postId) and (PostHashtagsTable.hashtagId eq hashtagId) }
                .count() > 0

            if (!alreadyLinked) {
                PostHashtagsTable.insert {
                    it[PostHashtagsTable.postId]    = postId
                    it[PostHashtagsTable.hashtagId] = hashtagId
                }
            }
        }
    }

    override suspend fun getPostsByUser(
        userId: UUID,
        viewerId: UUID?,
        cursor: Pair<String, String>?,
        limit: Int
    ): List<PostDto> = newSuspendedTransaction {
        PostsTable
            .select(PostsTable.id)
            .where { (PostsTable.userId eq userId) and PostsTable.deletedAt.isNull() }
            .orderBy(PostsTable.createdAt to SortOrder.DESC, PostsTable.id to SortOrder.DESC)
            .limit(limit)
            .map { it[PostsTable.id] }
            .mapNotNull { findByIdInternal(it, viewerId) }
    }
}
