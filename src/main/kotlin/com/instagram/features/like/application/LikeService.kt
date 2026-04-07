package com.instagram.features.like.application

import com.instagram.common.exceptions.ConflictException
import com.instagram.infrastructure.database.tables.CommentLikesTable
import com.instagram.infrastructure.database.tables.LikesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * LikeService — handles liking/unliking posts and comments.
 *
 * Teaching notes:
 * - Likes use a composite PK (user_id, post_id). This is the DB-level uniqueness constraint.
 * - `insertIgnore` is the Exposed equivalent of INSERT … ON CONFLICT DO NOTHING.
 *   This makes the like operation *idempotent* — liking twice doesn't throw an error.
 * - "Idempotent" means: calling the same operation multiple times has the same result
 *   as calling it once. Essential for network retries in mobile apps.
 */
class LikeService(
    private val notificationService: com.instagram.features.notification.application.NotificationService
) {

    suspend fun likePost(userId: UUID, postId: UUID): Unit =
        newSuspendedTransaction {
            val inserted = LikesTable.insertIgnore {
                it[LikesTable.userId] = userId
                it[LikesTable.postId] = postId
            }.insertedCount
            // insertIgnore returns 0 if the row already existed — silently ignore
            if (inserted > 0) {
                // Find post owner to notify
                val postOwner = com.instagram.infrastructure.database.tables.PostsTable
                    .select(com.instagram.infrastructure.database.tables.PostsTable.userId)
                    .where { com.instagram.infrastructure.database.tables.PostsTable.id eq postId }
                    .singleOrNull()?.get(com.instagram.infrastructure.database.tables.PostsTable.userId)
                
                if (postOwner != null && postOwner != userId) {
                    notificationService.sendPushNotification(postOwner, "New Like", "Someone liked your post!")
                }
            }
        }

    suspend fun unlikePost(userId: UUID, postId: UUID): Unit =
        newSuspendedTransaction {
            LikesTable.deleteWhere {
                (LikesTable.userId eq userId) and (LikesTable.postId eq postId)
            }
        }

    suspend fun likeComment(userId: UUID, commentId: UUID): Unit =
        newSuspendedTransaction {
            CommentLikesTable.insertIgnore {
                it[CommentLikesTable.userId]    = userId
                it[CommentLikesTable.commentId] = commentId
            }
        }

    suspend fun unlikeComment(userId: UUID, commentId: UUID): Unit =
        newSuspendedTransaction {
            CommentLikesTable.deleteWhere {
                (CommentLikesTable.userId eq userId) and (CommentLikesTable.commentId eq commentId)
            }
        }

    suspend fun getPostLikers(postId: UUID, limit: Int): List<Map<String, String>> =
        newSuspendedTransaction {
            (LikesTable innerJoin com.instagram.infrastructure.database.tables.UsersTable)
                .select(
                    com.instagram.infrastructure.database.tables.UsersTable.id,
                    com.instagram.infrastructure.database.tables.UsersTable.username,
                    com.instagram.infrastructure.database.tables.UsersTable.avatarUrl
                )
                .where { LikesTable.postId eq postId }
                .orderBy(LikesTable.createdAt to SortOrder.DESC)
                .limit(limit)
                .map { mapOf(
                    "id"       to it[com.instagram.infrastructure.database.tables.UsersTable.id].toString(),
                    "username" to it[com.instagram.infrastructure.database.tables.UsersTable.username]
                )}
        }
}
