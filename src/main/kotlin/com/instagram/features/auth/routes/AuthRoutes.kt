package com.instagram.features.auth.routes

import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.auth.application.AuthService
import com.instagram.features.auth.dto.LoginRequest
import com.instagram.features.auth.dto.RefreshTokenRequest
import com.instagram.features.auth.dto.RegisterRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Auth routes.
 *
 * Teaching notes:
 * - `receive<T>()` deserializes the JSON body into a Kotlin data class.
 *   If the body is malformed, Ktor throws a BadRequestException automatically.
 * - `inject<AuthService>()` retrieves the service from Koin's DI container.
 *   This is the only place Koin is referenced; the service itself has no Koin code.
 * - `authenticate("jwt") {}` wraps routes that require a valid access token.
 *   Non-authenticated routes (register, login) are OUTSIDE this block.
 */
fun Route.authRoutes() {
    val authService by inject<AuthService>()

    route("/auth") {

        // POST /api/v1/auth/register
        post("/register") {
            val req    = call.receive<RegisterRequest>()
            val tokens = authService.register(req)
            call.respond(HttpStatusCode.Created, ApiResponse.success(tokens))
        }

        // POST /api/v1/auth/login
        post("/login") {
            val req    = call.receive<LoginRequest>()
            val tokens = authService.login(req)
            call.respond(HttpStatusCode.OK, ApiResponse.success(tokens))
        }

        // POST /api/v1/auth/refresh
        post("/refresh") {
            val req    = call.receive<RefreshTokenRequest>()
            val tokens = authService.refresh(req.refreshToken)
            call.respond(HttpStatusCode.OK, ApiResponse.success(tokens))
        }

        // Authenticated routes below
        authenticate("jwt") {

            // POST /api/v1/auth/logout  — invalidates the supplied refresh token
            post("/logout") {
                val req = call.receive<RefreshTokenRequest>()
                authService.logout(req.refreshToken)
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("message" to "Logged out")))
            }

            // POST /api/v1/auth/logout-all — invalidates ALL refresh tokens (all devices)
            post("/logout-all") {
                val userId = call.requireUserId()
                authService.logoutAll(userId)
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("message" to "Logged out from all devices")))
            }
        }
    }
}
