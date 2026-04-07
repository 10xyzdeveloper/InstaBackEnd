package com.instagram.features.like.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.like.application.LikeService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.likeRoutes() {
    val likeService by inject<LikeService>()

    authenticate("jwt") {
        route("/posts/{id}") {
            // POST /api/v1/posts/{id}/like
            post("/like") {
                val userId = call.requireUserId()
                val postId = UUID.fromString(call.parameters["id"])
                likeService.likePost(userId, postId)
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("liked" to true)))
            }
            // DELETE /api/v1/posts/{id}/like
            delete("/like") {
                val userId = call.requireUserId()
                val postId = UUID.fromString(call.parameters["id"])
                likeService.unlikePost(userId, postId)
                call.respond(ApiResponse.success(mapOf("liked" to false)))
            }
            // GET /api/v1/posts/{id}/likes
            get("/likes") {
                val postId = UUID.fromString(call.parameters["id"])
                val limit  = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val likers = likeService.getPostLikers(postId, limit)
                call.respond(ApiResponse.success(likers))
            }
        }

        route("/comments/{id}") {
            // POST /api/v1/comments/{id}/like
            post("/like") {
                val userId    = call.requireUserId()
                val commentId = UUID.fromString(call.parameters["id"])
                likeService.likeComment(userId, commentId)
                call.respond(ApiResponse.success(mapOf("liked" to true)))
            }
            // DELETE /api/v1/comments/{id}/like
            delete("/like") {
                val userId    = call.requireUserId()
                val commentId = UUID.fromString(call.parameters["id"])
                likeService.unlikeComment(userId, commentId)
                call.respond(ApiResponse.success(mapOf("liked" to false)))
            }
        }
    }
}
