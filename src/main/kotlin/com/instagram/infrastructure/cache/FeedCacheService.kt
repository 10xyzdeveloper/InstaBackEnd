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
            val cachedList = jedis.lrange("feed_list:$userId", 0, -1)
            if (cachedList.isNullOrEmpty()) return null
            
            try {
                cachedList.map { json.decodeFromString<PostDto>(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun cacheFeed(userId: UUID, posts: List<PostDto>) {
        if (posts.isEmpty()) return
        jedisPool.resource.use { jedis ->
            val key = "feed_list:$userId"
            val pipeline = jedis.pipelined()
            pipeline.del(key)
            posts.forEach { post ->
                val serialized = json.encodeToString(post)
                pipeline.rpush(key, serialized) // rpush since posts are ordered descending
            }
            // Cache for 5 minutes
            pipeline.expire(key, 300)
            pipeline.sync()
        }
    }

    override suspend fun invalidateFeed(userId: UUID) {
        jedisPool.resource.use { jedis ->
            jedis.del("feed_list:$userId")
        }
    }
}
