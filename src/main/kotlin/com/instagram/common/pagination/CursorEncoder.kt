package com.instagram.common.pagination

import kotlinx.datetime.LocalDateTime
import java.util.*

/**
 * Creates and decodes opaque Base64 cursors.
 *
 * Why Base64 encode?
 * - The client treats the cursor as an opaque handle — it should NOT parse it.
 * - Encoding hides internal details (DB column names, timestamps) so we can
 *   change the underlying pagination strategy without breaking API clients.
 *
 * Cursor format (before encoding): "2026-04-07T10:30:00|550e8400-e29b-41d4-a716"
 * After Base64 encoding: "MjAyNi0wNC0wN1QxMDozMDowMHw1NTBl..."
 */
object CursorEncoder {

    /**
     * Encode a (createdAt, id) pair into an opaque cursor string.
     */
    fun encode(createdAt: LocalDateTime, id: UUID): String {
        val raw = "${createdAt}|${id}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /**
     * Decode a cursor string back into (createdAt, id).
     * Returns null if the cursor is malformed (treat as first page).
     */
    fun decode(cursor: String): Pair<LocalDateTime, UUID>? = runCatching {
        val raw   = String(Base64.getUrlDecoder().decode(cursor))
        val parts = raw.split("|")
        val createdAt = LocalDateTime.parse(parts[0])
        val id        = UUID.fromString(parts[1])
        Pair(createdAt, id)
    }.getOrNull()
}
