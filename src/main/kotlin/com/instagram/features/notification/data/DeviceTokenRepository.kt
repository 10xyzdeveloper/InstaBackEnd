package com.instagram.features.notification.data

import com.instagram.infrastructure.database.tables.DeviceTokensTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface DeviceTokenRepository {
    suspend fun registerToken(userId: UUID, token: String)
    suspend fun getTokensForUser(userId: UUID): List<String>
    suspend fun removeToken(token: String)
}

class DeviceTokenRepositoryImpl : DeviceTokenRepository {
    override suspend fun registerToken(userId: UUID, token: String) {
        newSuspendedTransaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            
            // Upsert device token
            val existing = DeviceTokensTable
                .select(DeviceTokensTable.id)
                .where { DeviceTokensTable.fcmToken eq token }
                .singleOrNull()
                
            if (existing != null) {
                DeviceTokensTable.update({ DeviceTokensTable.fcmToken eq token }) {
                    it[DeviceTokensTable.userId] = userId
                    it[DeviceTokensTable.updatedAt] = now
                }
            } else {
                DeviceTokensTable.insert {
                    it[id] = UUID.randomUUID()
                    it[DeviceTokensTable.userId] = userId
                    it[fcmToken] = token
                    it[updatedAt] = now
                }
            }
        }
    }

    override suspend fun getTokensForUser(userId: UUID): List<String> {
        return newSuspendedTransaction {
            DeviceTokensTable
                .select(DeviceTokensTable.fcmToken)
                .where { DeviceTokensTable.userId eq userId }
                .map { it[DeviceTokensTable.fcmToken] }
        }
    }

    override suspend fun removeToken(token: String) {
        newSuspendedTransaction {
            DeviceTokensTable.deleteWhere { DeviceTokensTable.fcmToken eq token }
        }
    }
}
