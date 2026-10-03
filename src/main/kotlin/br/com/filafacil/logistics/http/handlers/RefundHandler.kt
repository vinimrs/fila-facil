package br.com.filafacil.logistics.http.handlers

import br.com.filafacil.logistics.http.HttpUtils
import br.com.filafacil.logistics.service.RefundService
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.net.httpserver.HttpExchange

class RefundHandler(private val refundService: RefundService) {

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun request(exchange: HttpExchange) {
        try {
            val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
            val refund = refundService.request(
                (body["deliveryId"] as? Number)?.toLong(),
                body["reason"] as? String,
                body["amount"]?.toString()?.toBigDecimalOrNull(),
                body["requestedBy"] as? String,
            )
            HttpUtils.send(exchange, 201, mapper.writeValueAsString(refund))
        } catch (e: IllegalArgumentException) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun list(exchange: HttpExchange) {
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(refundService.list(HttpUtils.queryParams(exchange)["status"])))
    }

    fun get(exchange: HttpExchange, id: Long) {
        try {
            HttpUtils.send(exchange, 200, mapper.writeValueAsString(refundService.findById(id)))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun approve(exchange: HttpExchange, id: Long) {
        try {
            val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
            HttpUtils.send(exchange, 200, mapper.writeValueAsString(refundService.approve(id, body["approvedBy"] as? String)))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        } catch (e: IllegalStateException) {
            HttpUtils.send(exchange, 422, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun reject(exchange: HttpExchange, id: Long) {
        try {
            val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
            val refund = refundService.reject(id, body["rejectedBy"] as? String, body["notes"] as? String)
            HttpUtils.send(exchange, 200, mapper.writeValueAsString(refund))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        } catch (e: IllegalStateException) {
            HttpUtils.send(exchange, 422, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun report(exchange: HttpExchange) {
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(refundService.report()))
    }
}
