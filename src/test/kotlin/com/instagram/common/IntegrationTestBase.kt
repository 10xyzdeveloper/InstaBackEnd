package com.instagram.common

import com.instagram.di.appModule
import com.instagram.plugins.configureDatabase
import com.instagram.plugins.configureHTTP
import com.instagram.plugins.configureRouting
import com.instagram.plugins.configureSecurity
import com.instagram.plugins.configureSerialization
import com.instagram.plugins.configureStatusPages
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.core.context.stopKoin
import org.koin.ktor.plugin.Koin
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName
import io.ktor.server.application.*
import org.koin.logger.slf4jLogger

abstract class IntegrationTestBase {

    companion object {
        private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("instagram_test")
            .withUsername("testuser")
            .withPassword("testpass")

        private val elasticsearch = ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.0"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")

        @JvmStatic
        @BeforeAll
        fun startContainer() {
            postgres.start()
            elasticsearch.start()
            
            System.setProperty("ELASTICSEARCH_HOST", elasticsearch.host)
            System.setProperty("ELASTICSEARCH_PORT", elasticsearch.firstMappedPort.toString())
            
            // Run Flyway migrations directly on the testcontainer
            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate()
        }

        @JvmStatic
        @AfterAll
        fun stopContainer() {
            postgres.stop()
            elasticsearch.stop()
        }
    }

    /**
     * Helper method to set up the Ktor test environment with real DB bindings
     * via MapApplicationConfig.
     */
    protected fun IntegrationTestBase.withIntegrationApp(
        block: suspend ApplicationTestBuilder.(HttpClient) -> Unit
    ) = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.development" to "true",
                "jwt.secret"       to "test_secret",
                "jwt.issuer"       to "http://localhost:8080/",
                "jwt.audience"     to "http://localhost:8080/api",
                "jwt.realm"        to "instagram",
                "jwt.accessExpiry" to "900",
                "jwt.refreshExpiry" to "86400",
                "database.url"     to postgres.jdbcUrl,
                "database.user"    to postgres.username,
                "database.password" to postgres.password,
                "database.pool"    to "2",
                "storage.local.baseDir" to "build/tmp/test_storage"
            )
        }

        application {
            // First install Koin so other plugins can resolve dependencies
            install(Koin) {
                slf4jLogger()
                modules(appModule)
            }

            configureDatabase()

            // Setup Ktor features exactly as in normal `Application.module()`
            configureSerialization()
            configureSecurity()
            configureHTTP()
            configureStatusPages()
            configureRouting()
        }

        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        try {
            block(client)
        } finally {
            // Stop Koin manually after each test block so the next test
            // spins up fresh if necessary
            stopKoin()
        }
    }
}
