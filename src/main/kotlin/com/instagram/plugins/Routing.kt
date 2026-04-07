package com.instagram.plugins

import com.instagram.features.auth.routes.authRoutes
import com.instagram.features.user.routes.userRoutes
import com.instagram.features.post.routes.postRoutes
import com.instagram.features.feed.routes.feedRoutes
import com.instagram.features.explore.routes.exploreRoutes
import com.instagram.features.follow.routes.followRoutes
import com.instagram.features.like.routes.likeRoutes
import com.instagram.features.comment.routes.commentRoutes
import com.instagram.features.chat.routes.chatRoutes
import com.instagram.features.chat.routes.chatSocketRoute
import com.instagram.features.notification.routes.notificationRoutes
import com.instagram.features.story.routes.storyRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Registers all feature routes under a versioned API prefix.
 *
 * Convention:
 * - All HTTP REST routes live under `/api/v1/`
 * - The WebSocket endpoint lives at `/ws/chat` (outside the REST prefix)
 *
 * Each feature's routes are defined in its own `routes/` file, keeping
 * this file as a clean registry rather than a dumping ground.
 */
fun Application.configureRouting() {
    routing {
        // Health check — useful for load balancer probes
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/api/v1") {
            authRoutes()
            userRoutes()
            postRoutes()
            feedRoutes()
            exploreRoutes()
            followRoutes()
            likeRoutes()
            commentRoutes()
            chatRoutes()
            notificationRoutes()
            storyRoutes()
        }

        // WebSocket — separate from REST prefix
        chatSocketRoute()
    }
}
