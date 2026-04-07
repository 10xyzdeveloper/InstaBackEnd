package com.instagram.features.story.application

import com.instagram.features.story.data.StoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

class StoryEvictionWorker(
    private val storyRepository: StoryRepository
) {
    private val logger = LoggerFactory.getLogger(StoryEvictionWorker::class.java)

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Evict stories older than 24 hours
                    val before = (Clock.System.now() - 24.hours).toLocalDateTime(TimeZone.UTC)
                    val deletedCount = storyRepository.deleteExpiredStories(before)
                    
                    if (deletedCount > 0) {
                        logger.info("Evicted \$deletedCount expired stories")
                    }
                } catch (e: Exception) {
                    logger.error("Error during story eviction", e)
                }
                
                // Run eviction check every hour
                delay(1.hours)
            }
        }
    }
}
