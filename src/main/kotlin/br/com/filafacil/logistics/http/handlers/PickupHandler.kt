package br.com.filafacil.logistics.http.handlers

import br.com.filafacil.logistics.http.HttpUtils
import br.com.filafacil.logistics.model.Pickup
import br.com.filafacil.logistics.model.Region
import br.com.filafacil.logistics.service.PickupImportService
import br.com.filafacil.logistics.service.PickupService
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.net.httpserver.HttpExchange
import java.time.LocalDateTime

class PickupHandler(
    private val pickupService: PickupService,
    private val importService: PickupImportService,
) {

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun create(exchange: HttpExchange) {
        try {
            val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
            val region = body["region"] as? String ?: throw IllegalArgumentException("region is required")
            val pickup = Pickup(
                customerId = (body["customerId"] as? Number)?.toLong(),
                region = try {
                    Region.valueOf(region.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("invalid region: $region")
                },
                address = body["address"] as? String ?: "",
                packageCount = (body["packageCount"] as? Number)?.toInt() ?: 0,
                manualPriority = (body["manualPriority"] as? Number)?.toInt(),
                createdAt = LocalDateTime.now(),
            )
            HttpUtils.send(exchange, 201, mapper.writeValueAsString(pickupService.create(pickup)))
        } catch (e: IllegalArgumentException) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun importBatch(exchange: HttpExchange) {
        val items: List<Map<String, Any?>> = mapper.readValue(HttpUtils.readBody(exchange))
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(importService.import(items)))
    }

    fun list(exchange: HttpExchange) {
        val params = HttpUtils.queryParams(exchange)
        try {
            val result = pickupService.list(params["status"], params["region"], params["customerType"], params["strategy"])
            HttpUtils.send(exchange, 200, mapper.writeValueAsString(result))
        } catch (e: IllegalArgumentException) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun get(exchange: HttpExchange, id: Long) {
        try {
            HttpUtils.send(exchange, 200, mapper.writeValueAsString(pickupService.findById(id)))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun updateStatus(exchange: HttpExchange, id: Long) {
        try {
            val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
            val updated = pickupService.updateStatus(id, body["status"] as? String)
            HttpUtils.send(exchange, 200, mapper.writeValueAsString(updated))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        } catch (e: IllegalStateException) {
            HttpUtils.send(exchange, 409, mapper.writeValueAsString(mapOf("error" to e.message)))
        } catch (e: IllegalArgumentException) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun reschedule(exchange: HttpExchange, id: Long) {
        try {
            HttpUtils.send(exchange, 201, mapper.writeValueAsString(pickupService.reschedule(id)))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        } catch (e: IllegalStateException) {
            HttpUtils.send(exchange, 409, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun delete(exchange: HttpExchange, id: Long) {
        try {
            pickupService.delete(id)
            HttpUtils.send(exchange, 204, "")
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun summary(exchange: HttpExchange) {
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(pickupService.summary()))
    }

    fun uploadProof(exchange: HttpExchange, id: Long) {
        try {
            val pickup = pickupService.saveProof(id, HttpUtils.readBytes(exchange))
            HttpUtils.send(exchange, 200, mapper.writeValueAsString(pickup))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        } catch (e: IllegalArgumentException) {
            HttpUtils.send(exchange, 400, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }

    fun downloadProof(exchange: HttpExchange, id: Long) {
        try {
            HttpUtils.sendBytes(exchange, pickupService.readProof(id))
        } catch (e: NoSuchElementException) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to e.message)))
        }
    }
}
