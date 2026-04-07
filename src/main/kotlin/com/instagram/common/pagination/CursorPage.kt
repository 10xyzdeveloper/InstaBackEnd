package com.instagram.common.pagination

import kotlinx.serialization.Serializable
import java.util.*

/**
 * Represents a single page in a cursor-paginated response.
 *
 * Why cursor (keyset) pagination over OFFSET?
 * - OFFSET forces the DB to scan and discard N rows. On a table with millions
 *   of posts this becomes extremely slow (O(N) instead of O(log N)).
 * - Cursors reference a specific row via its sort key, so the DB jumps straight
 *   there using the index — constant time regardless of dataset size.
 * - Cursors are also stable: if new posts are added, you won't see duplicates
 *   or skip posts as you scroll (a common OFFSET problem).
 *
 * How it works:
 *   1. Client sends `GET /feed?limit=20`  (no cursor on first load)
 *   2. Server responds with 20 posts + nextCursor = "eyJjcmVhdGVkQXQiO..."
 *   3. Client sends `GET /feed?limit=20&cursor=eyJjcmVhdGVkQXQiO...`
 *   4. Server decodes the cursor → (createdAt, id) → adds WHERE clause
 *   5. Repeat until `hasMore = false`
 */
@Serializable
data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,  // null when on the last page
    val hasMore: Boolean,
    val count: Int
) {
    companion object {
        fun <T> of(items: List<T>, limit: Int, cursorFn: (T) -> String): CursorPage<T> {
            val hasMore = items.size == limit
            return CursorPage(
                items      = items,
                nextCursor = if (hasMore) cursorFn(items.last()) else null,
                hasMore    = hasMore,
                count      = items.size
            )
        }
    }
}
