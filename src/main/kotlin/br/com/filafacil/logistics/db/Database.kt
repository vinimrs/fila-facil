package br.com.filafacil.logistics.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection

object Database {

    private var dataSource: HikariDataSource? = null

    fun init(url: String, user: String, password: String, poolSize: Int = 10) {
        dataSource?.close()
        val config = HikariConfig()
        config.jdbcUrl = url
        config.username = user
        config.password = password
        config.maximumPoolSize = poolSize
        dataSource = HikariDataSource(config)
    }

    fun migrate() {
        Flyway.configure().dataSource(dataSource).load().migrate()
    }

    fun getConnection(): Connection =
        (dataSource ?: throw IllegalStateException("database not initialized")).connection
}
