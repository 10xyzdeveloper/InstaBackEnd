package com.instagram.features.chat.routes

import com.auth0.jwt.JWT
import com.instagram.common.extensions.requireUserId
import com.instagram.common.response.ApiResponse
import com.instagram.features.chat.application.ChatService
import com.instagram.features.chat.application.ChatSessionManager
import com.instagram.features.chat.dto.ServerFrame
import com.instagram.features.chat.dto.StartConversationRequest
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.chatRoutes() {
    val chatService by inject<ChatService>()

    authenticate("jwt") {
        route("/conversations") {
            get {
                val userId = call.requireUserId()
                val convs  = chatService.getConversations(userId)
                call.respond(ApiResponse.success(convs))
            }
            post {
                val userId = call.requireUserId()
                val req    = call.receive<StartConversationRequest>()
                val conv   = chatService.getOrCreateConversation(userId, UUID.fromString(req.recipientId))
                call.respond(ApiResponse.success(conv))
            }
            get("/{id}/messages") {
                val userId = call.requireUserId()
                val convId = UUID.fromString(call.parameters["id"])
                val cursor = call.request.queryParameters["cursor"]
                val limit  = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val msgs   = chatService.getMessageHistory(convId, userId, cursor, limit)
                call.respond(ApiResponse.success(msgs))
            }
        }
    }
}

fun Route.chatSocketRoute() {
    val chatService    by inject<ChatService>()
    val sessionManager by inject<ChatSessionManager>()
    val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    webSocket("/ws/chat") {
        // Step 1: AUTH frame must be first
        val rawAuth  = incoming.receive()
        val authText = (rawAuth as? Frame.Text)?.readText() ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "First frame must be AUTH"))
            return@webSocket
        }

        // Step 2: Validate JWT from AUTH frame
        val userId = runCatching {
            val parsed = json.parseToJsonElement(authText).jsonObject
            val token  = parsed["token"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("No token in AUTH frame")
            UUID.fromString(JWT.decode(token).subject)
        }.getOrNull()

        if (userId == null) {
            send(Frame.Text("""{"type":"ERROR","code":4001,"message":"Invalid token"}"""))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }

        // Step 3: Register session
        sessionManager.connect(userId, this)

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text    = frame.readText()
                val jsonObj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: continue
                val type    = jsonObj["type"]?.jsonPrimitive?.content

                when (type) {
                    "MESSAGE" -> {
                        val convId = jsonObj["conversationId"]?.jsonPrimitive?.content ?: continue
                        val body   = jsonObj["body"]?.jsonPrimitive?.content ?: continue
                        chatService.sendMessage(userId, UUID.fromString(convId), body)
                    }
                    "READ" -> {
                        val msgId = jsonObj["messageId"]?.jsonPrimitive?.content ?: continue
                        chatService.markRead(userId, UUID.fromString(msgId))
                    }
                    else -> send(Frame.Text("""{"type":"ERROR","code":4000,"message":"Unknown frame: $type"}"""))
                }
            }
        } finally {
            sessionManager.disconnect(userId)
        }
    }
}
