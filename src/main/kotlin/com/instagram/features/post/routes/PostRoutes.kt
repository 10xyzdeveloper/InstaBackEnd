package com.instagram.features.post.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.post.application.PostService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.postRoutes() {
    val postService by inject<PostService>()

    authenticate("jwt") {
        route("/posts") {

            // POST /api/v1/posts — create a post (multipart)
            post {
                val userId    = call.requireUserId()
                val multipart = call.receiveMultipart()
                val post      = postService.createPost(userId, multipart)
                call.respond(HttpStatusCode.Created, ApiResponse.success(post))
            }

            route("/{id}") {
                // GET /api/v1/posts/{id}
                get {
                    val postId   = UUID.fromString(call.parameters["id"])
                    val viewerId = runCatching { call.requireUserId() }.getOrNull()
                    val post     = postService.getPost(postId, viewerId)
                    call.respond(ApiResponse.success(post))
                }

                // DELETE /api/v1/posts/{id}
                delete {
                    val userId = call.requireUserId()
                    val postId = UUID.fromString(call.parameters["id"])
                    postService.deletePost(postId, userId)
                    call.respond(ApiResponse.success(mapOf("message" to "Post deleted")))
                }
            }
        }
    }
}
