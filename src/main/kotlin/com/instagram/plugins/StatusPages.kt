package com.instagram.plugins

import com.instagram.common.exceptions.*
import com.instagram.common.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

/**
 * Centralised error handling.
 *
 * Why centralised?
 * - Without this, unhandled exceptions would return a 500 with an HTML stack trace.
 * - Every exception type maps to a specific HTTP status code and a structured JSON body.
 * - Route handlers can throw domain exceptions freely; this plugin catches them all.
 *
 * Teaching note:
 * - Use domain-specific exception classes (NotFoundException, AuthorizationException)
 *   rather than checking return types everywhere. It keeps route handlers clean.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {

        // 400 — Validation errors (from RequestValidation plugin)
        exception<RequestValidationException> { call, e ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.error(400, "Validation failed", e.reasons)
            )
        }

        // 400 — Bad input (explicit domain exceptions)
        exception<BadRequestException> { call, e ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.error(400, e.message ?: "Bad request")
            )
        }

        // 401 — Unauthenticated
        exception<UnauthorizedException> { call, e ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse.error(401, e.message ?: "Unauthorized")
            )
        }

        // 403 — Authenticated but not allowed
        exception<ForbiddenException> { call, e ->
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse.error(403, e.message ?: "Forbidden")
            )
        }

        // 404 — Resource not found
        exception<NotFoundException> { call, e ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse.error(404, e.message ?: "Not found")
            )
        }

        // 409 — Conflict (e.g. username already taken)
        exception<ConflictException> { call, e ->
            call.respond(
                HttpStatusCode.Conflict,
                ApiResponse.error(409, e.message ?: "Conflict")
            )
        }

        // 500 — Catch-all: log and return generic message (never leak internals)
        exception<Throwable> { call, e ->
            call.application.log.error("Unhandled exception: ${e.message}", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse.error(500, "An unexpected error occurred")
            )
        }
    }
}
