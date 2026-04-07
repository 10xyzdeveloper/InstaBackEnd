package com.instagram.features.chat.application

import com.instagram.common.exceptions.ForbiddenException
import com.instagram.common.exceptions.NotFoundException
import com.instagram.features.chat.dto.*
import com.instagram.infrastructure.database.tables.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ChatService(private val sessionManager: ChatSessionManager) {

    suspend fun getOrCreateConversation(userA: UUID, userB: UUID): ConversationDto =
        newSuspendedTransaction {
            val existing = findDirectConversation(userA, userB)
            if (existing != null) return@newSuspendedTransaction existing

            val convId = ConversationsTable.insert {}[ConversationsTable.id]
            ConversationMembersTable.insert { it[conversationId] = convId; it[userId] = userA }
            ConversationMembersTable.insert { it[conversationId] = convId; it[userId] = userB }
            buildConversationDto(convId, userA)!!
        }

    suspend fun getConversations(userId: UUID): List<ConversationDto> =
        newSuspendedTransaction {
            ConversationMembersTable
                .select(ConversationMembersTable.conversationId)
                .where { ConversationMembersTable.userId eq userId }
                .map { it[ConversationMembersTable.conversationId] }
                .mapNotNull { buildConversationDto(it, userId) }
        }

    suspend fun sendMessage(senderId: UUID, convId: UUID, body: String): MessageDto {
        val isMember = newSuspendedTransaction {
            ConversationMembersTable
                .select(ConversationMembersTable.userId)
                .where { (ConversationMembersTable.conversationId eq convId) and (ConversationMembersTable.userId eq senderId) }
                .count() > 0
        }
        if (!isMember) throw ForbiddenException("Not a member of this conversation")

        val messageDto = newSuspendedTransaction {
            val senderUsername = UsersTable.select(UsersTable.username)
                .where { UsersTable.id eq senderId }.single()[UsersTable.username]

            val msgId = MessagesTable.insert {
                it[conversationId]          = convId
                it[MessagesTable.senderId]  = senderId
                it[MessagesTable.body]      = body
            }[MessagesTable.id]

            val createdAt = MessagesTable.select(MessagesTable.createdAt)
                .where { MessagesTable.id eq msgId }.single()[MessagesTable.createdAt]

            MessageDto(
                id             = msgId.toString(),
                conversationId = convId.toString(),
                senderId       = senderId.toString(),
                senderUsername = senderUsername,
                body           = body,
                readAt         = null,
                createdAt      = createdAt.toString()
            )
        }

        val recipients = newSuspendedTransaction {
            ConversationMembersTable
                .select(ConversationMembersTable.userId)
                .where { (ConversationMembersTable.conversationId eq convId) and (ConversationMembersTable.userId neq senderId) }
                .map { it[ConversationMembersTable.userId] }
        }
        recipients.forEach { sessionManager.sendTo(it, ServerFrame.NewMessage(messageDto)) }

        return messageDto
    }

    suspend fun markRead(userId: UUID, msgId: UUID): Unit =
        newSuspendedTransaction {
            val msg = MessagesTable
                .select(MessagesTable.conversationId, MessagesTable.senderId)
                .where { MessagesTable.id eq msgId }
                .singleOrNull() ?: throw NotFoundException("Message not found")

            if (msg[MessagesTable.senderId] == userId) return@newSuspendedTransaction

            MessagesTable.update({ MessagesTable.id eq msgId }) {
                it[readAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }

            sessionManager.sendTo(
                msg[MessagesTable.senderId],
                ServerFrame.ReadReceipt(messageId = msgId.toString(), readBy = userId.toString())
            )
        }

    suspend fun getMessageHistory(convId: UUID, userId: UUID, cursor: String?, limit: Int): List<MessageDto> =
        newSuspendedTransaction {
            val isMember = ConversationMembersTable
                .select(ConversationMembersTable.userId)
                .where { (ConversationMembersTable.conversationId eq convId) and (ConversationMembersTable.userId eq userId) }
                .count() > 0
            if (!isMember) throw ForbiddenException("Not a member of this conversation")

            MessagesTable
                .innerJoin(UsersTable, { MessagesTable.senderId }, { UsersTable.id })
                .select(MessagesTable.columns + listOf(UsersTable.username))
                .where { MessagesTable.conversationId eq convId }
                .orderBy(MessagesTable.createdAt to SortOrder.DESC)
                .limit(limit)
                .map {
                    MessageDto(
                        id             = it[MessagesTable.id].toString(),
                        conversationId = convId.toString(),
                        senderId       = it[MessagesTable.senderId].toString(),
                        senderUsername = it[UsersTable.username],
                        body           = it[MessagesTable.body],
                        readAt         = it[MessagesTable.readAt]?.toString(),
                        createdAt      = it[MessagesTable.createdAt].toString()
                    )
                }
        }

    private fun findDirectConversation(userA: UUID, userB: UUID): ConversationDto? {
        val aConvIds = ConversationMembersTable
            .select(ConversationMembersTable.conversationId)
            .where { ConversationMembersTable.userId eq userA }
            .map { it[ConversationMembersTable.conversationId] }

        val commonConvId = ConversationMembersTable
            .select(ConversationMembersTable.conversationId)
            .where {
                (ConversationMembersTable.userId eq userB) and
                (ConversationMembersTable.conversationId inList aConvIds)
            }
            .singleOrNull()?.get(ConversationMembersTable.conversationId) ?: return null

        return buildConversationDto(commonConvId, userA)
    }

    private fun buildConversationDto(convId: UUID, currentUserId: UUID): ConversationDto? {
        val participants = (ConversationMembersTable innerJoin UsersTable)
            .select(UsersTable.id, UsersTable.username, UsersTable.avatarUrl)
            .where { ConversationMembersTable.conversationId eq convId }
            .map { ParticipantDto(it[UsersTable.id].toString(), it[UsersTable.username], it[UsersTable.avatarUrl]) }

        val lastMessage = MessagesTable
            .innerJoin(UsersTable, { MessagesTable.senderId }, { UsersTable.id })
            .select(MessagesTable.columns + listOf(UsersTable.username))
            .where { MessagesTable.conversationId eq convId }
            .orderBy(MessagesTable.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let {
                MessageDto(
                    id             = it[MessagesTable.id].toString(),
                    conversationId = convId.toString(),
                    senderId       = it[MessagesTable.senderId].toString(),
                    senderUsername = it[UsersTable.username],
                    body           = it[MessagesTable.body],
                    readAt         = it[MessagesTable.readAt]?.toString(),
                    createdAt      = it[MessagesTable.createdAt].toString()
                )
            }

        val unreadCount = MessagesTable
            .select(MessagesTable.id.count())
            .where {
                (MessagesTable.conversationId eq convId) and
                (MessagesTable.senderId neq currentUserId) and
                MessagesTable.readAt.isNull()
            }
            .single()[MessagesTable.id.count()].toInt()

        return ConversationDto(convId.toString(), participants, lastMessage, unreadCount)
    }
}
