package com.instagram.features.comment.application

import com.instagram.common.exceptions.BadRequestException
import com.instagram.common.exceptions.ForbiddenException
import com.instagram.common.exceptions.NotFoundException
import com.instagram.common.pagination.CursorPage
import com.instagram.features.comment.dto.CommentAuthorDto
import com.instagram.features.comment.dto.CommentDto
import com.instagram.features.comment.dto.CreateCommentRequest
import com.instagram.infrastructure.database.tables.CommentLikesTable
import com.instagram.infrastructure.database.tables.CommentsTable
import com.instagram.infrastructure.database.tables.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class CommentService {

    suspend fun addComment(userId: UUID, postId: UUID, req: CreateCommentRequest): CommentDto {
        if (req.body.isBlank()) throw BadRequestException("Comment body cannot be empty")
        if (req.body.length > 2200) throw BadRequestException("Comment too long")

        val parentId = req.parentId?.let { pid ->
            UUID.fromString(pid).also { parentUuid ->
                newSuspendedTransaction {
                    val parent = CommentsTable.select(CommentsTable.parentId)
                        .where { CommentsTable.id eq parentUuid }
                        .singleOrNull() ?: throw NotFoundException("Parent comment not found")
                    if (parent[CommentsTable.parentId] != null)
                        throw BadRequestException("Cannot reply to a reply")
                }
            }
        }

        return newSuspendedTransaction {
            val commentId = CommentsTable.insert {
                it[CommentsTable.postId]   = postId
                it[CommentsTable.userId]   = userId
                it[CommentsTable.parentId] = parentId
                it[CommentsTable.body]     = req.body
            }[CommentsTable.id]
            queryComment(commentId, userId)!!
        }
    }

    suspend fun deleteComment(commentId: UUID, requestingUserId: UUID): Unit =
        newSuspendedTransaction {
            val ownerId = CommentsTable
                .select(CommentsTable.userId)
                .where { CommentsTable.id eq commentId }
                .singleOrNull()?.get(CommentsTable.userId)
                ?: throw NotFoundException("Comment not found")

            if (ownerId != requestingUserId) throw ForbiddenException("Cannot delete another user's comment")

            CommentsTable.update({ CommentsTable.id eq commentId }) {
                it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }

    suspend fun getPostComments(postId: UUID, viewerId: UUID?, limit: Int): CursorPage<CommentDto> =
        newSuspendedTransaction {
            val rows = (CommentsTable innerJoin UsersTable)
                .select(CommentsTable.columns + UsersTable.columns)
                .where {
                    (CommentsTable.postId eq postId) and
                    CommentsTable.parentId.isNull() and
                    CommentsTable.deletedAt.isNull()
                }
                .orderBy(CommentsTable.createdAt to SortOrder.ASC)
                .limit(limit + 1)
                .toList()

            val hasMore = rows.size > limit
            val pageRows = if (hasMore) rows.dropLast(1) else rows

            val items = pageRows.map { row ->
                val commentId  = row[CommentsTable.id]
                val likeCount  = CommentLikesTable.select(CommentLikesTable.userId.count())
                    .where { CommentLikesTable.commentId eq commentId }
                    .single()[CommentLikesTable.userId.count()]
                val replyCount = CommentsTable.select(CommentsTable.id.count())
                    .where { (CommentsTable.parentId eq commentId) and CommentsTable.deletedAt.isNull() }
                    .single()[CommentsTable.id.count()]
                val isLiked = viewerId?.let {
                    CommentLikesTable.select(CommentLikesTable.userId)
                        .where { (CommentLikesTable.userId eq it) and (CommentLikesTable.commentId eq commentId) }
                        .count() > 0
                } ?: false

                CommentDto(
                    id          = commentId.toString(),
                    postId      = postId.toString(),
                    author      = CommentAuthorDto(
                        id        = row[UsersTable.id].toString(),
                        username  = row[UsersTable.username],
                        avatarUrl = row[UsersTable.avatarUrl]
                    ),
                    body        = row[CommentsTable.body],
                    likeCount   = likeCount,
                    replyCount  = replyCount,
                    isLiked     = isLiked,
                    createdAt   = row[CommentsTable.createdAt].toString()
                )
            }

            CursorPage(items = items, nextCursor = null, hasMore = hasMore, count = items.size)
        }

    suspend fun getCommentReplies(commentId: UUID, viewerId: UUID?, limit: Int): List<CommentDto> =
        newSuspendedTransaction {
            (CommentsTable innerJoin UsersTable)
                .select(CommentsTable.columns + UsersTable.columns)
                .where {
                    (CommentsTable.parentId eq commentId) and CommentsTable.deletedAt.isNull()
                }
                .orderBy(CommentsTable.createdAt to SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    CommentDto(
                        id         = row[CommentsTable.id].toString(),
                        postId     = row[CommentsTable.postId].toString(),
                        author     = CommentAuthorDto(
                            id        = row[UsersTable.id].toString(),
                            username  = row[UsersTable.username],
                            avatarUrl = row[UsersTable.avatarUrl]
                        ),
                        body       = row[CommentsTable.body],
                        likeCount  = 0,
                        replyCount = 0,
                        createdAt  = row[CommentsTable.createdAt].toString()
                    )
                }
        }

    private fun queryComment(commentId: UUID, viewerId: UUID?): CommentDto? =
        (CommentsTable innerJoin UsersTable)
            .select(CommentsTable.columns + UsersTable.columns)
            .where { CommentsTable.id eq commentId }
            .singleOrNull()
            ?.let { row ->
                CommentDto(
                    id         = row[CommentsTable.id].toString(),
                    postId     = row[CommentsTable.postId].toString(),
                    author     = CommentAuthorDto(
                        id        = row[UsersTable.id].toString(),
                        username  = row[UsersTable.username],
                        avatarUrl = row[UsersTable.avatarUrl]
                    ),
                    body       = row[CommentsTable.body],
                    likeCount  = 0, replyCount = 0,
                    createdAt  = row[CommentsTable.createdAt].toString()
                )
            }
}
