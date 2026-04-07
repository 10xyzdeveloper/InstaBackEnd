package com.instagram.features.follow.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.follow.application.FollowService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.followRoutes() {
    val followService by inject<FollowService>()

    authenticate("jwt") {
        route("/users/{id}") {

            // POST /api/v1/users/{id}/follow
            post("/follow") {
                val currentUser = call.requireUserId()
                val targetUser  = UUID.fromString(call.parameters["id"])
                followService.follow(currentUser, targetUser)
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("message" to "Follow request sent")))
            }

            // DELETE /api/v1/users/{id}/follow
            delete("/follow") {
                val currentUser = call.requireUserId()
                val targetUser  = UUID.fromString(call.parameters["id"])
                followService.unfollow(currentUser, targetUser)
                call.respond(ApiResponse.success(mapOf("message" to "Unfollowed")))
            }

            // GET /api/v1/users/{id}/followers
            get("/followers") {
                val userId  = UUID.fromString(call.parameters["id"])
                val limit   = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val result  = followService.getFollowers(userId, limit)
                call.respond(ApiResponse.success(result))
            }

            // GET /api/v1/users/{id}/following
            get("/following") {
                val userId = UUID.fromString(call.parameters["id"])
                val limit  = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val result = followService.getFollowing(userId, limit)
                call.respond(ApiResponse.success(result))
            }
        }

        // GET /api/v1/users/me/requests  — pending follow requests
        get("/users/me/requests") {
            val userId  = call.requireUserId()
            val pending = followService.getPendingRequests(userId)
            call.respond(ApiResponse.success(pending))
        }

        // POST /api/v1/users/me/requests/{requesterId}/accept
        post("/users/me/requests/{requesterId}/accept") {
            val ownerId     = call.requireUserId()
            val requesterId = UUID.fromString(call.parameters["requesterId"])
            followService.acceptRequest(ownerId, requesterId)
            call.respond(ApiResponse.success(mapOf("message" to "Request accepted")))
        }

        // DELETE /api/v1/users/me/requests/{requesterId}/decline
        delete("/users/me/requests/{requesterId}/decline") {
            val ownerId     = call.requireUserId()
            val requesterId = UUID.fromString(call.parameters["requesterId"])
            followService.declineRequest(ownerId, requesterId)
            call.respond(ApiResponse.success(mapOf("message" to "Request declined")))
        }
    }
}
