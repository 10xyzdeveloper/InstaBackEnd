import io.ktor.plugin.features.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

repositories {
    mavenCentral()
}

group   = "com.instagram"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

ktor {
    fatJar {
        archiveFileName.set("instagram-backend.jar")
    }
}

dependencies {
    // ── Ktor Server ──────────────────────────────────────────────────────────
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.neg)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.serialization.json)

    // ── Database ─────────────────────────────────────────────────────────────
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)

    // ── Dependency Injection ─────────────────────────────────────────────────
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // ── Serialization ────────────────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // ── Security ─────────────────────────────────────────────────────────────
    implementation(libs.jbcrypt)

    implementation(libs.jedis)
    
    // AWS S3 ───────────────────────────────────────────────────────────────
    implementation(libs.aws.sdk.s3)

    // ── Push Notifications ───────────────────────────────────────────────────
    implementation(libs.firebase.admin)

    // ── Search ───────────────────────────────────────────────────────────────
    implementation(libs.elasticsearch.java)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    // ── Logging ──────────────────────────────────────────────────────────────
    implementation(libs.logback.classic)

    // ── Testing ──────────────────────────────────────────────────────────────
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.neg)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.koin.test)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.elasticsearch)
}

tasks.test {
    useJUnitPlatform()
}
