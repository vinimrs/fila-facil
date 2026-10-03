package br.com.filafacil.logistics.http

import com.sun.net.httpserver.HttpExchange
import java.net.URLDecoder

object HttpUtils {

    fun readBody(exchange: HttpExchange): String =
        exchange.requestBody.bufferedReader().use { it.readText() }

    fun readBytes(exchange: HttpExchange): ByteArray =
        exchange.requestBody.use { it.readAllBytes() }

    fun send(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        if (status == 204) {
            exchange.sendResponseHeaders(204, -1)
        } else {
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        exchange.close()
    }

    fun sendBytes(exchange: HttpExchange, bytes: ByteArray) {
        exchange.responseHeaders.add("Content-Type", "application/octet-stream")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }

    fun queryParams(exchange: HttpExchange): Map<String, String> {
        val query = exchange.requestURI.rawQuery ?: return emptyMap()
        return query.split("&")
            .filter { it.contains("=") }
            .associate {
                val (k, v) = it.split("=", limit = 2)
                URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
            }
    }

    fun pathId(path: String, index: Int): Long = path.split("/")[index].toLong()
}
