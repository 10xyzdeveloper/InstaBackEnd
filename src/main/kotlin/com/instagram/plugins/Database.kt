package com.instagram.plugins

import com.instagram.infrastructure.database.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Configures the database layer:
 *   1. Creates a HikariCP connection pool (production-grade connection reuse)
 *   2. Connects Exposed to the pool
 *   3. Runs Flyway migrations (version-controlled schema changes)
 *
 * Why HikariCP?
 * - Opening a JDBC connection is expensive (~100ms). HikariCP keeps a pool
 *   of warm connections, reducing per-request overhead to near zero.
 *
 * Why Flyway?
 * - Every schema change becomes a numbered SQL file in db/migration/.
 * - Flyway tracks which scripts have run, so the schema evolves safely.
 */
fun Application.configureDatabase() {
    val url      = environment.config.property("database.url").getString()
    val user     = environment.config.property("database.user").getString()
    val password = environment.config.property("database.password").getString()
    val poolSize = environment.config.property("database.pool").getString().toInt()

    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl         = url
        username        = user
        this.password   = password
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = poolSize
        isAutoCommit    = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    // Run Flyway migrations before connecting Exposed —
    // ensures the schema is always up to date on startup.
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(dataSource)

    log.info("Database connected and migrations applied.")
}
