package com.instagram.features.user.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.user.application.UserService
import com.instagram.features.user.dto.UpdateProfileRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val userService by inject<UserService>()

    route("/users") {

        // GET /api/v1/users/search?q=john
        get("/search") {
            val query   = call.request.queryParameters["q"] ?: ""
            val results = userService.searchUsers(query)
            call.respond(ApiResponse.success(results))
        }

        authenticate("jwt") {

            // GET /api/v1/users/me  — own profile
            get("/me") {
                val userId  = call.requireUserId()
                val profile = userService.getProfileById(userId, userId)
                call.respond(ApiResponse.success(profile))
            }

            // PUT /api/v1/users/me  — update bio / privacy
            put("/me") {
                val userId = call.requireUserId()
                val req    = call.receive<UpdateProfileRequest>()
                userService.updateProfile(userId, req)
                call.respond(ApiResponse.success(mapOf("message" to "Profile updated")))
            }

            // POST /api/v1/users/me/avatar  — multipart upload
            post("/me/avatar") {
                val userId    = call.requireUserId()
                val multipart = call.receiveMultipart()
                userService.uploadAvatar(userId, multipart)
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("message" to "Avatar updated")))
            }
        }

        // GET /api/v1/users/{username}  — public profile (optional auth for follow status)
        get("/{username}") {
            val username = call.parameters["username"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            // viewerId is optional — unauthenticated viewers won't see follow status
            val viewerId = runCatching { call.requireUserId() }.getOrNull()
            val profile  = userService.getProfileByUsername(username, viewerId)
            call.respond(ApiResponse.success(profile))
        }
    }
}
