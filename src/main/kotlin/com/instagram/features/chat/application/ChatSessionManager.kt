package com.instagram.features.chat.application

import com.instagram.features.chat.dto.ServerFrame
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ChatSessionManager — in-memory registry of active WebSocket connections.
 *
 * Teaching notes:
 *
 * 1. ConcurrentHashMap:
 *    - Multiple Ktor coroutines may connect/disconnect simultaneously.
 *    - A regular HashMap is NOT thread-safe — concurrent writes can corrupt it.
 *    - ConcurrentHashMap uses lock striping: it only locks the segment being modified,
 *      allowing other segments to be read/written concurrently.
 *
 * 2. Koin singleton:
 *    - This class is registered as `single {}` in Koin, so there is exactly ONE
 *      instance throughout the app's lifetime. All routes share the same map.
 *
 * 3. Horizontal scaling limitation:
 *    - This map lives in memory — it doesn't work if you run 2 server instances.
 *    - At scale, you'd use Redis Pub/Sub: server A publishes to a Redis channel,
 *      server B (which holds the recipient's socket) subscribes and forwards it.
 *    - This is a great architectural discussion point for learning.
 *
 * 4. Lifecycle:
 *    - `connect()` is called when a WS connection is established + authenticated.
 *    - `disconnect()` is called in the `finally {}` block — guaranteed even on crash.
 */
class ChatSessionManager {
    private val sessions = ConcurrentHashMap<UUID, DefaultWebSocketSession>()

    fun connect(userId: UUID, session: DefaultWebSocketSession) {
        sessions[userId] = session
    }

    fun disconnect(userId: UUID) {
        sessions.remove(userId)
    }

    fun isOnline(userId: UUID): Boolean = sessions.containsKey(userId)

    /**
     * Sends a frame to a specific user if they're connected.
     * Returns true if delivered, false if the user is offline.
     */
    suspend fun sendTo(userId: UUID, frame: ServerFrame): Boolean {
        val session = sessions[userId] ?: return false
        return try {
            val json = Json.encodeToString(frame)
            session.send(Frame.Text(json))
            true
        } catch (e: Exception) {
            // Session likely closed — clean up
            sessions.remove(userId)
            false
        }
    }
}
