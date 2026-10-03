package br.com.filafacil.logistics.http.handlers

import br.com.filafacil.logistics.dao.CustomerDao
import br.com.filafacil.logistics.http.HttpUtils
import br.com.filafacil.logistics.model.Customer
import br.com.filafacil.logistics.model.CustomerType
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.net.httpserver.HttpExchange
import java.time.LocalDateTime

class CustomerHandler {

    private val customerDao = CustomerDao()
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun create(exchange: HttpExchange) {
        val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
        val name = body["name"] as? String
        val document = body["document"] as? String
        val type = body["type"] as? String
        if (name.isNullOrBlank() || document.isNullOrBlank() || type == null) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to "name, document and type are required")))
            return
        }
        val customerType = try {
            CustomerType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to "invalid type: $type")))
            return
        }

        val customer = customerDao.insert(
            Customer(
                name = name,
                document = document,
                type = customerType,
                email = body["email"] as? String,
                active = true,
                createdAt = LocalDateTime.now(),
            ),
        )
        HttpUtils.send(exchange, 201, mapper.writeValueAsString(customer))
    }

    fun list(exchange: HttpExchange) {
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(customerDao.findAll()))
    }

    fun get(exchange: HttpExchange, id: Long) {
        val customer = customerDao.findById(id)
        if (customer == null) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to "customer $id not found")))
            return
        }
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(customer))
    }

    fun deactivate(exchange: HttpExchange, id: Long) {
        if (!customerDao.deactivate(id)) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to "customer $id not found")))
            return
        }
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(customerDao.findById(id)))
    }
}
