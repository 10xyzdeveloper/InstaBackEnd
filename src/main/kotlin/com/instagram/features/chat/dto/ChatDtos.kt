package com.instagram.features.chat.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── REST DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class ConversationDto(
    val id: String,
    val participants: List<ParticipantDto>,
    val lastMessage: MessageDto?,
    val unreadCount: Int
)

@Serializable
data class ParticipantDto(
    val id: String,
    val username: String,
    val avatarUrl: String?
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderUsername: String,
    val body: String,
    val readAt: String?,
    val createdAt: String
)

@Serializable
data class StartConversationRequest(
    val recipientId: String
)

// ─── WebSocket Frames ─────────────────────────────────────────────────────────

/**
 * Server → Client frames.
 * The `type` field acts as the discriminator for the sealed class hierarchy.
 *
 * Teaching note: `@Serializable` sealed classes use `classDiscriminator` in Json config
 * to emit a type tag. We set `classDiscriminator = "type"` in the Json instance.
 */
@Serializable
sealed class ServerFrame {
    @Serializable
    @SerialName("MESSAGE")
    data class NewMessage(val message: MessageDto) : ServerFrame()

    @Serializable
    @SerialName("DELIVERED")
    data class Delivered(val messageId: String) : ServerFrame()

    @Serializable
    @SerialName("READ")
    data class ReadReceipt(val messageId: String, val readBy: String) : ServerFrame()

    @Serializable
    @SerialName("ERROR")
    data class Error(val code: Int, val message: String) : ServerFrame()
}
