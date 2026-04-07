package com.instagram.infrastructure.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

// ─── Users ───────────────────────────────────────────────────────────────────

object UsersTable : Table("users") {
    val id        = uuid("id").autoGenerate()
    val username  = varchar("username", 30).uniqueIndex()
    val email     = varchar("email", 255).uniqueIndex()
    val password  = varchar("password_hash", 255)
    val bio       = text("bio").nullable()
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val isPrivate = bool("is_private").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

// ─── Refresh Tokens ───────────────────────────────────────────────────────────

object RefreshTokensTable : Table("refresh_tokens") {
    val id        = uuid("id").autoGenerate()
    val userId    = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val token     = varchar("token", 512).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

// ─── Posts ────────────────────────────────────────────────────────────────────

object PostsTable : Table("posts") {
    val id        = uuid("id").autoGenerate()
    val userId    = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val caption   = text("caption").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val deletedAt = datetime("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

// ─── Post Media ───────────────────────────────────────────────────────────────

object PostMediaTable : Table("post_media") {
    val id        = uuid("id").autoGenerate()
    val postId    = reference("post_id", PostsTable.id, onDelete = ReferenceOption.CASCADE)
    val url       = varchar("url", 512)
    val mediaType = varchar("media_type", 10)
    val position  = integer("position")
    override val primaryKey = PrimaryKey(id)
}

// ─── Follows ──────────────────────────────────────────────────────────────────

object FollowsTable : Table("follows") {
    val followerId  = reference("follower_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val followingId = reference("following_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val status      = varchar("status", 10).default("ACTIVE")
    val createdAt   = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(followerId, followingId)
}

// ─── Likes ────────────────────────────────────────────────────────────────────

object LikesTable : Table("likes") {
    val userId    = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val postId    = reference("post_id", PostsTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(userId, postId)
}

// ─── Comments ─────────────────────────────────────────────────────────────────

object CommentsTable : Table("comments") {
    val id        = uuid("id").autoGenerate()
    val postId    = reference("post_id", PostsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId    = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val parentId  = uuid("parent_id").nullable()
    val body      = text("body")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val deletedAt = datetime("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

// ─── Comment Likes ────────────────────────────────────────────────────────────

object CommentLikesTable : Table("comment_likes") {
    val userId    = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val commentId = reference("comment_id", CommentsTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(userId, commentId)
}

// ─── Hashtags ─────────────────────────────────────────────────────────────────

object HashtagsTable : Table("hashtags") {
    val id   = uuid("id").autoGenerate()
    val name = varchar("name", 100).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

object PostHashtagsTable : Table("post_hashtags") {
    val postId    = reference("post_id", PostsTable.id, onDelete = ReferenceOption.CASCADE)
    val hashtagId = reference("hashtag_id", HashtagsTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(postId, hashtagId)
}

// ─── Conversations ────────────────────────────────────────────────────────────

object ConversationsTable : Table("conversations") {
    val id        = uuid("id").autoGenerate()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object ConversationMembersTable : Table("conversation_members") {
    val conversationId = reference("conversation_id", ConversationsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId         = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(conversationId, userId)
}

// ─── Messages ─────────────────────────────────────────────────────────────────

object MessagesTable : Table("messages") {
    val id             = uuid("id").autoGenerate()
    val conversationId = reference("conversation_id", ConversationsTable.id, onDelete = ReferenceOption.CASCADE)
    val senderId       = reference("sender_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val body           = text("body")
    val readAt         = datetime("read_at").nullable()
    val createdAt      = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

// ─── Stories ──────────────────────────────────────────────────────────────────

object StoriesTable : Table("stories") {
    val id        = uuid("id").autoGenerate()
    val userId    = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val mediaUrl  = varchar("media_url", 512)
    val mediaType = varchar("media_type", 10)
    val caption   = varchar("caption", 255).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

// ─── Device Tokens ────────────────────────────────────────────────────────────

object DeviceTokensTable : Table("device_tokens") {
    val id        = uuid("id").autoGenerate()
    val userId    = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val fcmToken  = varchar("fcm_token", 512).uniqueIndex()
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}
