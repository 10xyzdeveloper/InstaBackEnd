package com.instagram.di

import com.instagram.features.auth.application.AuthService
import com.instagram.features.auth.data.AuthRepositoryImpl
import com.instagram.features.auth.domain.AuthRepository
import com.instagram.features.chat.application.ChatService
import com.instagram.features.chat.application.ChatSessionManager
import com.instagram.features.comment.application.CommentService
import com.instagram.features.explore.application.ExploreService
import com.instagram.features.feed.application.FeedService
import com.instagram.features.follow.application.FollowService
import com.instagram.features.like.application.LikeService
import com.instagram.features.post.application.PostService
import com.instagram.features.post.data.PostRepositoryImpl
import com.instagram.features.post.domain.PostRepository
import com.instagram.features.user.application.UserService
import com.instagram.features.user.data.UserRepositoryImpl
import com.instagram.features.user.domain.UserRepository
import com.instagram.infrastructure.cache.FeedCacheService
import com.instagram.infrastructure.cache.RedisFeedCacheService
import com.instagram.infrastructure.storage.LocalStorageService
import com.instagram.infrastructure.storage.S3StorageService
import com.instagram.infrastructure.storage.StorageService
import io.ktor.server.application.*
import org.koin.dsl.module
import redis.clients.jedis.JedisPool
import org.koin.ktor.ext.get

/**
 * Root Koin module.
 *
 * Teaching notes:
 *
 * - `single {}` = one instance for the app's lifetime (singleton).
 *   Use for: repositories, services, session managers, DB connections.
 *
 * - `factory {}` = a new instance every time it's requested.
 *   Use for: request-scoped objects, utilities you don't want to share state.
 *
 * - We bind interfaces to their implementations:
 *   `single<UserRepository> { UserRepositoryImpl() }`
 *   This means anywhere UserRepository is injected, Koin provides UserRepositoryImpl.
 *   In tests, you override this binding with a fake implementation.
 *
 * - `get()` inside a module = "fetch this dependency from Koin" (constructor injection).
 *   Example: AuthService depends on AuthRepository, so `get()` resolves it.
 *
 * - Storage: environment variable `USE_LOCAL_STORAGE=true` switches to the
 *   local filesystem implementation. No code changes needed — just Koin config.
 */

val appModule = module {
    // ── Infrastructure ────────────────────────────────────────────────────────
    
    // Redis
    single {
        val host = get<Application>().environment.config.propertyOrNull("redis.host")?.getString() ?: "localhost"
        val port = get<Application>().environment.config.propertyOrNull("redis.port")?.getString()?.toInt() ?: 6379
        JedisPool(host, port)
    }
    single<FeedCacheService> { RedisFeedCacheService(get()) }

    single<StorageService> {
        val useLocal = System.getenv("USE_LOCAL_STORAGE") == "true"
        if (useLocal) {
            LocalStorageService()
        } else {
            val config = get<Application>().environment.config
            S3StorageService(
                bucket = config.property("aws.bucket").getString(),
                region = config.property("aws.region").getString()
            )
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    single<AuthRepository> { AuthRepositoryImpl() }
    single {
        val config = get<Application>().environment.config
        AuthService(
            authRepo      = get<AuthRepository>(),
            jwtSecret     = config.property("jwt.secret").getString(),
            jwtIssuer     = config.property("jwt.issuer").getString(),
            jwtAudience   = config.property("jwt.audience").getString(),
            accessExpiry  = config.property("jwt.accessExpiry").getString().toLong(),
            refreshExpiry = config.property("jwt.refreshExpiry").getString().toLong()
        )
    }

    // ── User ──────────────────────────────────────────────────────────────────
    single<UserRepository> { UserRepositoryImpl() }
    single { UserService(userRepo = get(), storageService = get()) }

    // ── Post ──────────────────────────────────────────────────────────────────
    single<PostRepository> { PostRepositoryImpl() }
    single { PostService(postRepo = get(), storageService = get()) }

    // ── Follow ────────────────────────────────────────────────────────────────
    single { FollowService() }

    // ── Like ──────────────────────────────────────────────────────────────────
    single { LikeService() }

    // ── Comment ───────────────────────────────────────────────────────────────
    single { CommentService() }

    // ── Feed ──────────────────────────────────────────────────────────────────
    single { FeedService(get()) }

    // ── Explore ───────────────────────────────────────────────────────────────
    single { ExploreService() }

    // ── Chat ──────────────────────────────────────────────────────────────────
    single { ChatSessionManager() }
    single { ChatService(sessionManager = get()) }
}
