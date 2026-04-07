package com.instagram.features.comment.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.comment.application.CommentService
import com.instagram.features.comment.dto.CreateCommentRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.commentRoutes() {
    val commentService by inject<CommentService>()

    authenticate("jwt") {
        // POST /api/v1/posts/{id}/comments
        post("/posts/{id}/comments") {
            val userId = call.requireUserId()
            val postId = UUID.fromString(call.parameters["id"])
            val req    = call.receive<CreateCommentRequest>()
            val comment = commentService.addComment(userId, postId, req)
            call.respond(HttpStatusCode.Created, ApiResponse.success(comment))
        }

        // DELETE /api/v1/comments/{id}
        delete("/comments/{id}") {
            val userId    = call.requireUserId()
            val commentId = UUID.fromString(call.parameters["id"])
            commentService.deleteComment(commentId, userId)
            call.respond(ApiResponse.success(mapOf("message" to "Comment deleted")))
        }

        // GET /api/v1/comments/{id}/replies
        get("/comments/{id}/replies") {
            val commentId = UUID.fromString(call.parameters["id"])
            val viewerId  = runCatching { call.requireUserId() }.getOrNull()
            val limit     = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val replies   = commentService.getCommentReplies(commentId, viewerId, limit)
            call.respond(ApiResponse.success(replies))
        }
    }

    // GET /api/v1/posts/{id}/comments  — optionally authenticated
    get("/posts/{id}/comments") {
        val postId   = UUID.fromString(call.parameters["id"])
        val viewerId = runCatching { call.requireUserId() }.getOrNull()
        val limit    = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val page     = commentService.getPostComments(postId, viewerId, limit)
        call.respond(ApiResponse.success(page))
    }
}
