package br.com.filafacil.logistics.http.handlers

import br.com.filafacil.logistics.dao.ManifestDao
import br.com.filafacil.logistics.http.HttpUtils
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.net.httpserver.HttpExchange

class ManifestHandler {

    private val dao = ManifestDao()
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun upload(exchange: HttpExchange) {
        val content = HttpUtils.readBody(exchange)
        val body: Map<String, Any?> = mapper.readValue(content)
        val entries = body["entries"] as? List<*> ?: emptyList<Any>()

        val id = dao.insert(body["routeCode"] as? String, body["uploadedBy"] as? String, content)
        println("manifest $id received with ${entries.size} entries")
        HttpUtils.send(exchange, 201, mapper.writeValueAsString(mapOf("id" to id, "entries" to entries.size)))
    }

    fun list(exchange: HttpExchange) {
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(dao.findAll()))
    }

    fun get(exchange: HttpExchange, id: Long) {
        val content = dao.findContent(id)
        if (content == null) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to "manifest $id not found")))
            return
        }
        HttpUtils.send(exchange, 200, content)
    }
}
