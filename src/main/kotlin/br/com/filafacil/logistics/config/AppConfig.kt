package br.com.filafacil.logistics.config

import java.util.Properties

object AppConfig {

    private val props = Properties().apply {
        AppConfig::class.java.getResourceAsStream("/application.properties").use { load(it) }
    }

    fun get(key: String): String =
        props.getProperty(key) ?: throw IllegalStateException("missing config: $key")

    fun getInt(key: String): Int = get(key).toInt()

    fun all(): Properties = props
}
