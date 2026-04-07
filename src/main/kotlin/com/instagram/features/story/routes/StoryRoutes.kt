package com.instagram.features.story.routes

import com.instagram.common.response.ApiResponse
import com.instagram.features.story.application.StoryService
import com.instagram.features.story.dto.CreateStoryRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.storyRoutes() {
    val storyService by inject<StoryService>()

    route("/api/v1/stories") {
        authenticate("auth-jwt") {
            
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error(HttpStatusCode.Unauthorized.value, "Active session not found"))
                
                val request = call.receive<CreateStoryRequest>()
                val story = storyService.createStory(userId, request)
                
                call.respond(HttpStatusCode.Created, ApiResponse.success(story))
            }

            get("/feed") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error(HttpStatusCode.Unauthorized.value, "Active session not found"))
                
                val stories = storyService.getFeedStories(userId)
                
                call.respond(HttpStatusCode.OK, ApiResponse.success(stories))
            }
        }
    }
}
