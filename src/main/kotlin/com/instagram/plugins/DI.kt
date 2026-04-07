package com.instagram.plugins

import com.instagram.di.appModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

/**
 * Installs Koin as a Ktor plugin.
 *
 * Why Koin over Hilt/Dagger?
 * - Koin has first-class Ktor integration and works without annotation processing.
 * - The `koin-ktor` module adds `inject()` syntax directly inside routes.
 *
 * Pattern: the root `appModule` uses `includes()` to pull in all feature modules,
 * so this file never needs to change when you add a new feature.
 */
fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
