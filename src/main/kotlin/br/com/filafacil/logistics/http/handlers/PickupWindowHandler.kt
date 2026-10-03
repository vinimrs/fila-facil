package br.com.filafacil.logistics.http.handlers

import br.com.filafacil.logistics.http.HttpUtils
import br.com.filafacil.logistics.service.PickupWindowService
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import java.time.LocalDate

class PickupWindowHandler(private val pickupWindowService: PickupWindowService) {

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun list(exchange: HttpExchange) {
        val date = HttpUtils.queryParams(exchange)["date"]?.let { LocalDate.parse(it) }
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(pickupWindowService.list(date)))
    }

    fun get(exchange: HttpExchange, id: Long) {
        val window = pickupWindowService.findById(id)
        if (window == null) {
            HttpUtils.send(exchange, 404, mapper.writeValueAsString(mapOf("error" to "pickup window $id not found")))
            return
        }
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(window))
    }
}
