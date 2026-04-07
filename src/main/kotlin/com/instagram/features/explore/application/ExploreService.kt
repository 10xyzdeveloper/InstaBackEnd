package com.instagram.features.explore.application

import com.instagram.features.post.dto.MediaDto
import com.instagram.features.post.dto.PostAuthorDto
import com.instagram.features.post.dto.PostDto
import com.instagram.features.user.dto.UserSearchResult
import com.instagram.infrastructure.database.tables.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ExploreService {

    suspend fun getTrending(viewerId: UUID?, limit: Int): List<PostDto> =
        newSuspendedTransaction {
            val twentyFourHoursAgo = Clock.System.now().minus(24, DateTimeUnit.HOUR).toLocalDateTime(TimeZone.UTC)
            val likeCountExpr = LikesTable.userId.count()

            val trendingPostIds = LikesTable
                .select(LikesTable.postId, likeCountExpr)
                .where { LikesTable.createdAt greaterEq twentyFourHoursAgo }
                .groupBy(LikesTable.postId)
                .orderBy(likeCountExpr to SortOrder.DESC)
                .limit(limit)
                .map { it[LikesTable.postId] }

            trendingPostIds.mapNotNull { buildPostDto(it, viewerId) }
        }

    suspend fun getPostsByHashtag(tag: String, viewerId: UUID?, limit: Int): List<PostDto> =
        newSuspendedTransaction {
            val postIds = (PostHashtagsTable innerJoin HashtagsTable)
                .select(PostHashtagsTable.postId)
                .where { HashtagsTable.name eq tag.lowercase() }
                .limit(limit)
                .map { it[PostHashtagsTable.postId] }

            postIds.mapNotNull { buildPostDto(it, viewerId) }
        }

    suspend fun searchUsers(query: String): List<UserSearchResult> =
        newSuspendedTransaction {
            UsersTable
                .select(UsersTable.id, UsersTable.username, UsersTable.avatarUrl)
                .where { UsersTable.username like "%${query.lowercase()}%" }
                .limit(20)
                .map {
                    UserSearchResult(
                        id        = it[UsersTable.id].toString(),
                        username  = it[UsersTable.username],
                        avatarUrl = it[UsersTable.avatarUrl]
                    )
                }
        }

    private fun buildPostDto(postId: UUID, viewerId: UUID?): PostDto? {
        val row = (PostsTable innerJoin UsersTable)
            .select(PostsTable.columns + UsersTable.columns)
            .where { (PostsTable.id eq postId) and PostsTable.deletedAt.isNull() }
            .singleOrNull() ?: return null

        val media = PostMediaTable
            .select(PostMediaTable.url, PostMediaTable.mediaType, PostMediaTable.position)
            .where { PostMediaTable.postId eq postId }
            .orderBy(PostMediaTable.position)
            .map { MediaDto(it[PostMediaTable.url], it[PostMediaTable.mediaType], it[PostMediaTable.position]) }

        val likeCount = LikesTable.select(LikesTable.userId.count())
            .where { LikesTable.postId eq postId }.single()[LikesTable.userId.count()]
        val commentCount = CommentsTable.select(CommentsTable.id.count())
            .where { (CommentsTable.postId eq postId) and CommentsTable.deletedAt.isNull() }
            .single()[CommentsTable.id.count()]
        val isLiked = viewerId?.let {
            LikesTable.select(LikesTable.userId)
                .where { (LikesTable.userId eq it) and (LikesTable.postId eq postId) }.count() > 0
        } ?: false

        return PostDto(
            id           = postId.toString(),
            author       = PostAuthorDto(
                id        = row[UsersTable.id].toString(),
                username  = row[UsersTable.username],
                avatarUrl = row[UsersTable.avatarUrl]
            ),
            caption      = row[PostsTable.caption],
            media        = media,
            likeCount    = likeCount,
            commentCount = commentCount,
            isLiked      = isLiked,
            createdAt    = row[PostsTable.createdAt].toString()
        )
    }
}
