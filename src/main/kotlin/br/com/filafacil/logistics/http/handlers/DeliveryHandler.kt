package br.com.filafacil.logistics.http.handlers

import br.com.filafacil.logistics.dao.DeliveryDao
import br.com.filafacil.logistics.dao.RefundDao
import br.com.filafacil.logistics.http.HttpUtils
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.net.httpserver.HttpExchange
import java.time.LocalDate
import java.time.LocalDateTime

class DeliveryHandler {

    private val deliveryDao = DeliveryDao()
    private val refundDao = RefundDao()
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun list(exchange: HttpExchange) {
        val params = HttpUtils.queryParams(exchange)
        var result = deliveryDao.findAll()

        params["status"]?.let { status -> result = result.filter { it.status.equals(status, ignoreCase = true) } }
        params["driver"]?.let { driver -> result = result.filter { it.driverName?.contains(driver, ignoreCase = true) == true } }
        params["customer"]?.let { customer -> result = result.filter { it.customerName?.contains(customer, ignoreCase = true) == true } }
        params["from"]?.let { from -> result = result.filter { it.scheduledDate != null && !it.scheduledDate!!.isBefore(LocalDate.parse(from)) } }
        params["to"]?.let { to -> result = result.filter { it.scheduledDate != null && !it.scheduledDate!!.isAfter(LocalDate.parse(to)) } }
        params["hasRefund"]?.let { hasRefund ->
            val refunded = refundDao.findAll().map { it.deliveryId }.toSet()
            result = result.filter { (it.id in refunded) == hasRefund.toBoolean() }
        }

        val page = params["page"]?.toInt() ?: 1
        val size = params["size"]?.toInt() ?: 50
        val paged = result.drop((page - 1) * size).take(size)

        HttpUtils.send(exchange, 200, mapper.writeValueAsString(paged))
    }

    fun get(exchange: HttpExchange, id: Long) {
        val delivery = deliveryDao.findById(id)
        if (delivery == null) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to "delivery $id not found")))
            return
        }
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(delivery))
    }

    fun registerOccurrence(exchange: HttpExchange, id: Long) {
        val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
        val type = body["type"] as? String
        if (type.isNullOrBlank()) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to "type is required")))
            return
        }
        if (deliveryDao.findById(id) == null) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to "delivery $id not found")))
            return
        }
        deliveryDao.updateOccurrence(id, type.uppercase(), body["notes"] as? String, LocalDateTime.now())
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(deliveryDao.findById(id)))
    }
}
