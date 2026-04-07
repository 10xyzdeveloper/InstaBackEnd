package com.instagram.features.auth.data

import com.instagram.features.auth.domain.AuthRepository
import com.instagram.infrastructure.database.tables.RefreshTokensTable
import com.instagram.infrastructure.database.tables.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * AuthRepositoryImpl — all datetime uses kotlinx.datetime.LocalDateTime
 * because exposed-kotlin-datetime maps datetime() columns to kotlinx.datetime.LocalDateTime.
 */
class AuthRepositoryImpl : AuthRepository {

    override suspend fun createUser(
        username: String, email: String, passwordHash: String
    ): UUID = newSuspendedTransaction {
        UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.email]    = email
            it[UsersTable.password] = passwordHash
        }[UsersTable.id]
    }

    override suspend fun findPasswordHashByEmail(email: String): Pair<UUID, String>? =
        newSuspendedTransaction {
            UsersTable
                .select(UsersTable.id, UsersTable.password)
                .where { UsersTable.email eq email }
                .singleOrNull()
                ?.let { it[UsersTable.id] to it[UsersTable.password] }
        }

    override suspend fun saveRefreshToken(
        userId: UUID, token: String, expiresAt: LocalDateTime
    ): Unit = newSuspendedTransaction {
        // exposed-kotlin-datetime stores kotlinx.datetime.LocalDateTime natively
        RefreshTokensTable.insert {
            it[RefreshTokensTable.userId]    = userId
            it[RefreshTokensTable.token]     = token
            it[RefreshTokensTable.expiresAt] = expiresAt
        }
    }

    override suspend fun findUserIdByRefreshToken(token: String): UUID? =
        newSuspendedTransaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            RefreshTokensTable
                .select(RefreshTokensTable.userId, RefreshTokensTable.expiresAt)
                .where { RefreshTokensTable.token eq token }
                .singleOrNull()
                ?.let { row ->
                    val expiry: LocalDateTime = row[RefreshTokensTable.expiresAt]
                    // Compare using kotlinx.datetime ordering
                    if (expiry > now) row[RefreshTokensTable.userId] else null
                }
        }

    override suspend fun deleteRefreshToken(token: String): Unit =
        newSuspendedTransaction {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq token }
        }

    override suspend fun deleteAllRefreshTokensForUser(userId: UUID): Unit =
        newSuspendedTransaction {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        }
}
