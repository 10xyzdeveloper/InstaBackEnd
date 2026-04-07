package com.instagram.features.user.data

import com.instagram.features.user.domain.UserRepository
import com.instagram.features.user.dto.UserProfileDto
import com.instagram.features.user.dto.UserSearchResult
import com.instagram.infrastructure.database.tables.FollowsTable
import com.instagram.infrastructure.database.tables.LikesTable
import com.instagram.infrastructure.database.tables.PostsTable
import com.instagram.infrastructure.database.tables.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserRepositoryImpl : UserRepository {

    override suspend fun findById(userId: UUID, viewerId: UUID?): UserProfileDto? =
        newSuspendedTransaction { queryProfile(userId, viewerId) }

    override suspend fun findByUsername(username: String, viewerId: UUID?): UserProfileDto? =
        newSuspendedTransaction {
            val userId = UsersTable
                .select(UsersTable.id)
                .where { UsersTable.username eq username.lowercase() }
                .singleOrNull()
                ?.get(UsersTable.id) ?: return@newSuspendedTransaction null
            queryProfile(userId, viewerId)
        }

    /**
     * Aggregate query that fetches the profile with counts in one round-trip.
     *
     * Teaching note: We use subqueries here to get post/follower/following counts.
     * In a high-scale system you'd maintain these as denormalized counters in Redis
     * and sync them back to the DB periodically. For learning, subqueries are fine.
     */
    private fun queryProfile(userId: UUID, viewerId: UUID?): UserProfileDto? {
        val row = UsersTable
            .select(
                UsersTable.id, UsersTable.username, UsersTable.bio,
                UsersTable.avatarUrl, UsersTable.isPrivate
            )
            .where { UsersTable.id eq userId }
            .singleOrNull() ?: return null

        val postCount = PostsTable
            .select(PostsTable.id.count())
            .where { (PostsTable.userId eq userId) and PostsTable.deletedAt.isNull() }
            .single()[PostsTable.id.count()]

        val followerCount = FollowsTable
            .select(FollowsTable.followerId.count())
            .where { (FollowsTable.followingId eq userId) and (FollowsTable.status eq "ACTIVE") }
            .single()[FollowsTable.followerId.count()]

        val followingCount = FollowsTable
            .select(FollowsTable.followingId.count())
            .where { (FollowsTable.followerId eq userId) and (FollowsTable.status eq "ACTIVE") }
            .single()[FollowsTable.followingId.count()]

        val followStatus = viewerId?.let { vid ->
            FollowsTable
                .select(FollowsTable.status)
                .where { (FollowsTable.followerId eq vid) and (FollowsTable.followingId eq userId) }
                .singleOrNull()
                ?.get(FollowsTable.status)
        }

        return UserProfileDto(
            id             = row[UsersTable.id].toString(),
            username       = row[UsersTable.username],
            bio            = row[UsersTable.bio],
            avatarUrl      = row[UsersTable.avatarUrl],
            isPrivate      = row[UsersTable.isPrivate],
            postCount      = postCount,
            followerCount  = followerCount,
            followingCount = followingCount,
            isFollowing    = followStatus == "ACTIVE",
            followStatus   = followStatus
        )
    }

    override suspend fun updateBio(userId: UUID, bio: String?): Unit =
        newSuspendedTransaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.bio] = bio
            }
        }

    override suspend fun updatePrivacy(userId: UUID, isPrivate: Boolean): Unit =
        newSuspendedTransaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.isPrivate] = isPrivate
            }
        }

    override suspend fun updateAvatar(userId: UUID, avatarUrl: String): Unit =
        newSuspendedTransaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.avatarUrl] = avatarUrl
            }
        }

    /**
     * ILIKE search with a pg_trgm index for fast fuzzy matching.
     *
     * Teaching note: `%query%` becomes a GIN index lookup with pg_trgm instead
     * of a full table scan. This is why we added:
     *   CREATE EXTENSION pg_trgm;
     *   CREATE INDEX idx_users_username_trgm ON users USING gin (username gin_trgm_ops);
     */
    override suspend fun searchByUsername(query: String, limit: Int): List<UserSearchResult> =
        newSuspendedTransaction {
            UsersTable
                .select(UsersTable.id, UsersTable.username, UsersTable.avatarUrl)
                .where { UsersTable.username like "%${query.lowercase()}%" }
                .limit(limit)
                .map {
                    UserSearchResult(
                        id        = it[UsersTable.id].toString(),
                        username  = it[UsersTable.username],
                        avatarUrl = it[UsersTable.avatarUrl]
                    )
                }
        }
}
