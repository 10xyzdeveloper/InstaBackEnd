package com.instagram.features.notification.routes

import com.instagram.common.response.ApiResponse
import com.instagram.features.notification.application.NotificationService
import com.instagram.features.notification.dto.RegisterDeviceTokenRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.notificationRoutes() {
    val notificationService by inject<NotificationService>()

    route("/api/v1/notifications") {
        authenticate("auth-jwt") {
            post("/device-token") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error(HttpStatusCode.Unauthorized.value, "Active session not found"))
                
                val request = call.receive<RegisterDeviceTokenRequest>()
                notificationService.registerDeviceToken(userId, request.fcmToken)
                
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("status" to "registered")))
            }
        }
    }
}
