package com.instagram.features.auth

import com.instagram.common.IntegrationTestBase
import com.instagram.features.auth.dto.LoginRequest
import com.instagram.features.auth.dto.RegisterRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest : IntegrationTestBase() {

    @Test
    fun `register - successful registration returns 201 with tokens`() = withIntegrationApp { client ->
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                username = "testuser",
                email    = "test@example.com",
                password = "securepassword123"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("accessToken"), "Response should contain accessToken")
        assertTrue(body.contains("refreshToken"), "Response should contain refreshToken")
    }

    @Test
    fun `register - duplicate username returns 409`() = withIntegrationApp { client ->
        // First user
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                username = "duplicateuser",
                email    = "first@example.com",
                password = "securepassword123"
            ))
        }

        // Second user - same username
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                username = "duplicateuser",
                email    = "second@example.com",
                password = "securepassword123"
            ))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `login - invalid password returns 401`() = withIntegrationApp { client ->
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(
                email    = "nonexistent@example.com",
                password = "wrongpassword"
            ))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
