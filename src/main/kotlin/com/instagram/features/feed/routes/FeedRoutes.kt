package com.instagram.features.feed.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.feed.application.FeedService
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.feedRoutes() {
    val feedService by inject<FeedService>()

    authenticate("jwt") {
        // GET /api/v1/feed?cursor=&limit=20
        get("/feed") {
            val userId = call.requireUserId()
            val cursor = call.request.queryParameters["cursor"]
            val limit  = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 50)
            val page   = feedService.getHomeFeed(userId, cursor, limit)
            call.respond(ApiResponse.success(page))
        }
    }
}
