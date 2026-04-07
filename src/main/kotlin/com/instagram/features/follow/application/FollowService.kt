package com.instagram.features.follow.application

import com.instagram.common.exceptions.BadRequestException
import com.instagram.common.exceptions.ConflictException
import com.instagram.common.exceptions.NotFoundException
import com.instagram.common.pagination.CursorPage
import com.instagram.infrastructure.database.tables.FollowsTable
import com.instagram.infrastructure.database.tables.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * FollowService — manages the social graph.
 *
 * Teaching notes:
 * - The follow state machine: NONE → PENDING (private) or ACTIVE (public) → NONE
 * - When following a private account, status = "PENDING" until the account owner accepts.
 * - Follower/following counts are derived from the follows table at query time.
 *   At scale you'd cache these in Redis with INCR/DECR.
 */
class FollowService {

    suspend fun follow(followerId: UUID, followingId: UUID) {
        if (followerId == followingId) throw BadRequestException("You cannot follow yourself")

        newSuspendedTransaction {
            // Check if already following
            val existing = FollowsTable.select(FollowsTable.status)
                .where { (FollowsTable.followerId eq followerId) and (FollowsTable.followingId eq followingId) }
                .singleOrNull()

            if (existing != null) throw ConflictException("Already following or request pending")

            // Determine if target account is private
            val isPrivate = UsersTable
                .select(UsersTable.isPrivate)
                .where { UsersTable.id eq followingId }
                .singleOrNull()
                ?.get(UsersTable.isPrivate)
                ?: throw NotFoundException("User not found")

            val status = if (isPrivate) "PENDING" else "ACTIVE"

            FollowsTable.insert {
                it[FollowsTable.followerId]  = followerId
                it[FollowsTable.followingId] = followingId
                it[FollowsTable.status]      = status
            }
        }
    }

    suspend fun unfollow(followerId: UUID, followingId: UUID): Unit =
        newSuspendedTransaction {
            FollowsTable.deleteWhere {
                (FollowsTable.followerId eq followerId) and (FollowsTable.followingId eq followingId)
            }
        }

    suspend fun acceptRequest(ownerId: UUID, requesterId: UUID): Unit =
        newSuspendedTransaction {
            val updated = FollowsTable.update({
                (FollowsTable.followerId eq requesterId) and
                (FollowsTable.followingId eq ownerId) and
                (FollowsTable.status eq "PENDING")
            }) {
                it[status] = "ACTIVE"
            }
            if (updated == 0) throw NotFoundException("Follow request not found")
        }

    suspend fun declineRequest(ownerId: UUID, requesterId: UUID): Unit =
        newSuspendedTransaction {
            FollowsTable.deleteWhere {
                (FollowsTable.followerId eq requesterId) and
                (FollowsTable.followingId eq ownerId) and
                (FollowsTable.status eq "PENDING")
            }
        }

    suspend fun getFollowers(userId: UUID, limit: Int): List<Map<String, String>> =
        newSuspendedTransaction {
            FollowsTable
                .innerJoin(UsersTable, { FollowsTable.followerId }, { UsersTable.id })
                .select(UsersTable.id, UsersTable.username, UsersTable.avatarUrl)
                .where { (FollowsTable.followingId eq userId) and (FollowsTable.status eq "ACTIVE") }
                .limit(limit)
                .map { mapOf(
                    "id"        to it[UsersTable.id].toString(),
                    "username"  to it[UsersTable.username],
                    "avatarUrl" to (it[UsersTable.avatarUrl] ?: "")
                )}
        }

    suspend fun getFollowing(userId: UUID, limit: Int): List<Map<String, String>> =
        newSuspendedTransaction {
            FollowsTable
                .innerJoin(UsersTable, { FollowsTable.followingId }, { UsersTable.id })
                .select(UsersTable.id, UsersTable.username, UsersTable.avatarUrl)
                .where { (FollowsTable.followerId eq userId) and (FollowsTable.status eq "ACTIVE") }
                .limit(limit)
                .map { mapOf(
                    "id"        to it[UsersTable.id].toString(),
                    "username"  to it[UsersTable.username],
                    "avatarUrl" to (it[UsersTable.avatarUrl] ?: "")
                )}
        }

    suspend fun getPendingRequests(userId: UUID): List<Map<String, String>> =
        newSuspendedTransaction {
            FollowsTable
                .innerJoin(UsersTable, { FollowsTable.followerId }, { UsersTable.id })
                .select(UsersTable.id, UsersTable.username, UsersTable.avatarUrl)
                .where { (FollowsTable.followingId eq userId) and (FollowsTable.status eq "PENDING") }
                .map { mapOf(
                    "id"       to it[UsersTable.id].toString(),
                    "username" to it[UsersTable.username]
                )}
        }
}
