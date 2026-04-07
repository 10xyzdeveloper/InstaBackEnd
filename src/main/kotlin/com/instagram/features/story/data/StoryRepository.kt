package com.instagram.features.story.data

import com.instagram.features.story.domain.Story
import com.instagram.infrastructure.database.tables.StoriesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

interface StoryRepository {
    suspend fun create(story: Story): Story
    suspend fun getActiveStoriesByUserIds(userIds: List<UUID>, since: kotlinx.datetime.LocalDateTime): List<Story>
    suspend fun deleteExpiredStories(before: kotlinx.datetime.LocalDateTime): Int
}

class StoryRepositoryImpl : StoryRepository {
    override suspend fun create(story: Story): Story {
        return newSuspendedTransaction {
            StoriesTable.insert {
                it[id] = story.id
                it[userId] = story.userId
                it[mediaUrl] = story.mediaUrl
                it[mediaType] = story.mediaType
                it[caption] = story.caption
                it[createdAt] = story.createdAt
            }
            story
        }
    }

    override suspend fun getActiveStoriesByUserIds(userIds: List<UUID>, since: kotlinx.datetime.LocalDateTime): List<Story> {
        if (userIds.isEmpty()) return emptyList()
        return newSuspendedTransaction {
            StoriesTable.selectAll()
                .where { (StoriesTable.userId inList userIds) and (StoriesTable.createdAt greaterEq since) }
                .orderBy(StoriesTable.createdAt to SortOrder.DESC)
                .map {
                    Story(
                        id = it[StoriesTable.id],
                        userId = it[StoriesTable.userId],
                        mediaUrl = it[StoriesTable.mediaUrl],
                        mediaType = it[StoriesTable.mediaType],
                        caption = it[StoriesTable.caption],
                        createdAt = it[StoriesTable.createdAt]
                    )
                }
        }
    }

    override suspend fun deleteExpiredStories(before: kotlinx.datetime.LocalDateTime): Int {
        return newSuspendedTransaction {
            StoriesTable.deleteWhere { StoriesTable.createdAt less before }
        }
    }
}
