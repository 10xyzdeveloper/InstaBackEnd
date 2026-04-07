package com.instagram.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

/**
 * Configures the Ktor WebSocket plugin.
 *
 * Key settings:
 * - `pingPeriod`: Server pings clients every 15s. If no pong is received
 *   within `timeoutPeriod`, the server closes the connection. This detects
 *   stale connections (e.g. phone lost network silently).
 * - `maxFrameSize`: 64KB limit prevents clients from sending oversized frames
 *   that could cause memory pressure.
 * - `masking`: RFC 6455 requires client→server frames to be masked.
 *   Ktor handles this transparently.
 */
fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod    = 15.seconds
        timeout       = 30.seconds
        maxFrameSize  = Long.MAX_VALUE
        masking       = false
    }
}
