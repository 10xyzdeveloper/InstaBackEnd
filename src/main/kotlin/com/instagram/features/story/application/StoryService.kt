package com.instagram.features.story.application

import com.instagram.common.exceptions.NotFoundException
import com.instagram.features.story.data.StoryRepository
import com.instagram.features.story.domain.Story
import com.instagram.features.story.dto.CreateStoryRequest
import com.instagram.features.story.dto.StoryDto
import com.instagram.features.story.dto.StoryAuthorDto
import com.instagram.infrastructure.database.tables.FollowsTable
import com.instagram.infrastructure.database.tables.UsersTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

class StoryService(
    private val storyRepository: StoryRepository
) {
    suspend fun createStory(userId: UUID, request: CreateStoryRequest): StoryDto {
        return newSuspendedTransaction {
            val userRow = UsersTable.select(UsersTable.username, UsersTable.avatarUrl)
                .where { UsersTable.id eq userId }
                .singleOrNull() ?: throw NotFoundException("User not found")

            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            
            val story = Story(
                id = UUID.randomUUID(),
                userId = userId,
                mediaUrl = request.mediaUrl,
                mediaType = request.mediaType,
                caption = request.caption,
                createdAt = now
            )
            
            val created = storyRepository.create(story)
            
            StoryDto(
                id = created.id.toString(),
                userId = created.userId.toString(),
                author = StoryAuthorDto(
                    id = userId.toString(),
                    username = userRow[UsersTable.username],
                    avatarUrl = userRow[UsersTable.avatarUrl]
                ),
                mediaUrl = created.mediaUrl,
                mediaType = created.mediaType,
                caption = created.caption,
                createdAt = created.createdAt.toString()
            )
        }
    }

    suspend fun getFeedStories(userId: UUID): List<StoryDto> {
        return newSuspendedTransaction {
            // Get followed users
            val followedIds = FollowsTable
                .select(FollowsTable.followingId)
                .where { (FollowsTable.followerId eq userId) and (FollowsTable.status eq "ACTIVE") }
                .map { it[FollowsTable.followingId] }
            
            // We also want the user's own stories
            val targetUserIds = followedIds + userId

            // 24 hours ago
            val since = (Clock.System.now() - 24.hours).toLocalDateTime(TimeZone.UTC)
            val stories = storyRepository.getActiveStoriesByUserIds(targetUserIds, since)
            
            if (stories.isEmpty()) return@newSuspendedTransaction emptyList()

            // Fetch authors bulk
            val allUserIds = stories.map { it.userId }.distinct()
            val authorMap = UsersTable
                .select(UsersTable.id, UsersTable.username, UsersTable.avatarUrl)
                .where { UsersTable.id inList allUserIds }
                .associate { 
                    it[UsersTable.id] to StoryAuthorDto(
                        id = it[UsersTable.id].toString(),
                        username = it[UsersTable.username],
                        avatarUrl = it[UsersTable.avatarUrl]
                    )
                }

            stories.map { story ->
                StoryDto(
                    id = story.id.toString(),
                    userId = story.userId.toString(),
                    author = authorMap[story.userId] ?: throw NotFoundException("Author not found"),
                    mediaUrl = story.mediaUrl,
                    mediaType = story.mediaType,
                    caption = story.caption,
                    createdAt = story.createdAt.toString()
                )
            }
        }
    }
}
