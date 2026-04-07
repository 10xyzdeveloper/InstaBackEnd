package com.instagram.common.response

import kotlinx.serialization.Serializable

/**
 * Unified API response envelope.
 *
 * Every endpoint returns one of:
 *   { "data": { ... } }           — success
 *   { "error": { "code": ..., "message": ..., "details": [...] } }  — error
 *
 * Why a sealed class?
 * - The Kotlin compiler ensures exhaustive `when` expressions on the response type.
 * - A single generic wrapper keeps client-side parsing code uniform.
 *
 * Usage in routes:
 *   call.respond(ApiResponse.success(userDto))
 *   call.respond(ApiResponse.error(404, "User not found"))
 */
@Serializable
sealed class ApiResponse<out T> {

    @Serializable
    data class Success<T>(val data: T) : ApiResponse<T>()

    @Serializable
    data class Error(
        val error: ErrorBody
    ) : ApiResponse<Nothing>()

    @Serializable
    data class ErrorBody(
        val code: Int,
        val message: String,
        val details: List<String> = emptyList()
    )

    companion object {
        fun <T> success(data: T): Success<T> = Success(data)

        fun error(code: Int, message: String, details: List<String> = emptyList()): Error =
            Error(ErrorBody(code, message, details))
    }
}
