package com.instagram.features.feed.application

import com.instagram.common.pagination.CursorEncoder
import com.instagram.common.pagination.CursorPage
import com.instagram.features.post.dto.MediaDto
import com.instagram.features.post.dto.PostAuthorDto
import com.instagram.features.post.dto.PostDto
import com.instagram.infrastructure.cache.FeedCacheService
import com.instagram.infrastructure.database.tables.*
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * FeedService — Fan-out on Read with keyset cursor pagination.
 *
 * exposed-kotlin-datetime maps datetime() → kotlinx.datetime.LocalDateTime.
 * The Exposed DSL comparison operators (less, eq, etc.) work natively with
 * kotlinx.datetime.LocalDateTime through the extension functions provided by the library.
 */
class FeedService(private val feedCache: FeedCacheService) {

    suspend fun getHomeFeed(userId: UUID, cursor: String?, limit: Int): CursorPage<PostDto> =
        newSuspendedTransaction {
            val followedIds = FollowsTable
                .select(FollowsTable.followingId)
                .where { (FollowsTable.followerId eq userId) and (FollowsTable.status eq "ACTIVE") }
                .map { it[FollowsTable.followingId] }

            if (followedIds.isEmpty()) return@newSuspendedTransaction CursorPage(emptyList(), null, false, 0)

            // Try cache for page 1
            if (cursor == null) {
                val cached = feedCache.getCachedFeed(userId)
                if (cached != null) {
                    val nextCursor = if (cached.size > limit) CursorEncoder.encode(
                        kotlinx.datetime.LocalDateTime.parse(cached.last().createdAt),
                        UUID.fromString(cached.last().id)
                    ) else null
                    val paginated = cached.take(limit)
                    return@newSuspendedTransaction CursorPage(paginated, nextCursor, cached.size > limit, paginated.size)
                }
            }

            // Decode opaque cursor to (createdAt, id)
            val decoded = cursor?.let { CursorEncoder.decode(it) }

            val rows = (PostsTable innerJoin UsersTable)
                .select(PostsTable.columns + UsersTable.columns)
                .where {
                    (PostsTable.userId inList followedIds) and
                    PostsTable.deletedAt.isNull() and
                    if (decoded != null) {
                        val (cursorDt, cursorId) = decoded
                        (PostsTable.createdAt less cursorDt) or
                        ((PostsTable.createdAt eq cursorDt) and (PostsTable.id less cursorId))
                    } else {
                        Op.TRUE
                    }
                }
                .orderBy(PostsTable.createdAt to SortOrder.DESC, PostsTable.id to SortOrder.DESC)
                .limit(limit + 1)
                .toList()

            val hasMore = rows.size > limit
            val pageRows = if (hasMore) rows.dropLast(1) else rows

            val posts = pageRows.map { row ->
                val postId   = row[PostsTable.id]
                val media    = PostMediaTable
                    .select(PostMediaTable.url, PostMediaTable.mediaType, PostMediaTable.position)
                    .where { PostMediaTable.postId eq postId }
                    .orderBy(PostMediaTable.position)
                    .map { MediaDto(it[PostMediaTable.url], it[PostMediaTable.mediaType], it[PostMediaTable.position]) }
                val likeCount    = LikesTable.select(LikesTable.userId.count())
                    .where { LikesTable.postId eq postId }.single()[LikesTable.userId.count()]
                val commentCount = CommentsTable.select(CommentsTable.id.count())
                    .where { (CommentsTable.postId eq postId) and CommentsTable.deletedAt.isNull() }
                    .single()[CommentsTable.id.count()]
                val isLiked = LikesTable.select(LikesTable.userId)
                    .where { (LikesTable.userId eq userId) and (LikesTable.postId eq postId) }.count() > 0

                PostDto(
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

            val nextCursor = if (hasMore) {
                val last = pageRows.last()
                CursorEncoder.encode(last[PostsTable.createdAt], last[PostsTable.id])
            } else null

            // Write to cache if page 1
            if (cursor == null) {
                feedCache.cacheFeed(userId, posts)
            }

            CursorPage(posts, nextCursor, hasMore, posts.size)
        }
}
