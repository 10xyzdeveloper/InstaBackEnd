package com.instagram.features.explore.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.explore.application.ExploreService
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.exploreRoutes() {
    val exploreService by inject<ExploreService>()

    route("/explore") {
        // GET /api/v1/explore?limit=20
        get {
            val viewerId = runCatching { call.requireUserId() }.getOrNull()
            val limit    = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val posts    = exploreService.getTrending(viewerId, limit)
            call.respond(ApiResponse.success(posts))
        }

        // GET /api/v1/explore/hashtags/{tag}
        get("/hashtags/{tag}") {
            val tag      = call.parameters["tag"]
                ?: return@get call.respond(ApiResponse.error(400, "Tag required"))
            val viewerId = runCatching { call.requireUserId() }.getOrNull()
            val limit    = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val posts    = exploreService.getPostsByHashtag(tag, viewerId, limit)
            call.respond(ApiResponse.success(posts))
        }

        // GET /api/v1/explore/search?q=john
        get("/search") {
            val query = call.request.queryParameters["q"] ?: ""
            val users = exploreService.searchUsers(query)
            call.respond(ApiResponse.success(users))
        }
    }
}
