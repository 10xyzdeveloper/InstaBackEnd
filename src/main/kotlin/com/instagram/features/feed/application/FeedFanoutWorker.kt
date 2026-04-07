package com.instagram.features.feed.application

import com.instagram.features.post.dto.PostDto
import com.instagram.infrastructure.database.tables.FollowsTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool
import java.util.UUID

class FeedFanoutWorker(
    private val jedisPool: JedisPool
) {
    private val logger = LoggerFactory.getLogger(FeedFanoutWorker::class.java)
    private val fanoutQueue = Channel<PostDto>(Channel.UNLIMITED)
    private val json = Json { ignoreUnknownKeys = true }

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            for (post in fanoutQueue) {
                try {
                    processFanout(post)
                } catch (e: Exception) {
                    logger.error("Failed to fan-out post \${post.id}", e)
                }
            }
        }
    }

    suspend fun enqueuePost(post: PostDto) {
        fanoutQueue.send(post)
    }

    private suspend fun processFanout(post: PostDto) {
        val authorId = UUID.fromString(post.author.id)
        
        // Find all followers
        val followers = newSuspendedTransaction {
            FollowsTable
                .select(FollowsTable.followerId)
                .where { (FollowsTable.followingId eq authorId) and (FollowsTable.status eq "ACTIVE") }
                .map { it[FollowsTable.followerId] }
        }

        if (followers.isEmpty()) return

        val postJson = json.encodeToString(post)

        jedisPool.resource.use { jedis ->
            // Pipeline for performance
            val pipeline = jedis.pipelined()
            
            followers.forEach { followerId ->
                val key = "feed_list:\$followerId"
                pipeline.lpush(key, postJson)
                pipeline.ltrim(key, 0, 199) // Keep only latest 200 posts
                pipeline.expire(key, 86400 * 7L) // 7 days expiry
            }
            
            pipeline.sync()
        }

        logger.info("Fanned out post \${post.id} to \${followers.size} followers")
    }
}
