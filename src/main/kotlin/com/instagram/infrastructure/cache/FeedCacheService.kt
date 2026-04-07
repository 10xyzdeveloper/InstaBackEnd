package com.instagram.infrastructure.cache

import com.instagram.features.post.dto.PostDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool
import java.util.UUID

interface FeedCacheService {
    suspend fun getCachedFeed(userId: UUID): List<PostDto>?
    suspend fun cacheFeed(userId: UUID, posts: List<PostDto>)
    suspend fun invalidateFeed(userId: UUID)
}

class RedisFeedCacheService(
    private val jedisPool: JedisPool
) : FeedCacheService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getCachedFeed(userId: UUID): List<PostDto>? {
        return jedisPool.resource.use { jedis ->
            val cached = jedis.get("feed:$userId") ?: return null
            try {
                json.decodeFromString<List<PostDto>>(cached)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun cacheFeed(userId: UUID, posts: List<PostDto>) {
        jedisPool.resource.use { jedis ->
            val serialized = json.encodeToString(posts)
            // Cache for 5 minutes (300 seconds)
            jedis.setex("feed:$userId", 300, serialized)
        }
    }

    override suspend fun invalidateFeed(userId: UUID) {
        jedisPool.resource.use { jedis ->
            jedis.del("feed:$userId")
        }
    }
}
