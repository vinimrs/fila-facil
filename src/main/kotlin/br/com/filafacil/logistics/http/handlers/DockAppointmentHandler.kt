package br.com.filafacil.logistics.http.handlers

import br.com.filafacil.logistics.config.AppConfig
import br.com.filafacil.logistics.dao.DockAppointmentDao
import br.com.filafacil.logistics.http.HttpUtils
import br.com.filafacil.logistics.model.DockAppointment
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.net.httpserver.HttpExchange
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DockAppointmentHandler {

    private val dao = DockAppointmentDao()
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    fun create(exchange: HttpExchange) {
        val body: Map<String, Any?> = mapper.readValue(HttpUtils.readBody(exchange))
        val carrier = body["carrier"] as? String
        val plate = body["truckPlate"] as? String
        val dock = (body["dock"] as? Number)?.toInt()
        if (carrier.isNullOrBlank() || plate.isNullOrBlank() || dock == null || body["date"] == null || body["startTime"] == null) {
            HttpUtils.send(exchange, 400, "{\"error\":\"carrier, truckPlate, dock, date and startTime are required\"}")
            return
        }

        val start = LocalTime.parse(body["startTime"] as String)
        val appointment = DockAppointment(
            carrier = carrier,
            truckPlate = plate.uppercase(),
            dock = dock,
            date = LocalDate.parse(body["date"] as String),
            startTime = start,
            endTime = (body["endTime"] as? String)?.let { LocalTime.parse(it) } ?: start.plusHours(1),
            status = "SCHEDULED",
            createdAt = LocalDateTime.now(),
        )
        HttpUtils.send(exchange, 201, mapper.writeValueAsString(dao.insert(appointment)))
    }

    fun list(exchange: HttpExchange) {
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(dao.findAll()))
    }

    fun uploadAttachment(exchange: HttpExchange, id: Long) {
        val appointment = dao.findById(id)
        if (appointment == null) {
            HttpUtils.send(exchange, 404, "{\"error\":\"dock appointment $id not found\"}")
            return
        }
        val fileName = File(exchange.requestHeaders.getFirst("X-File-Name") ?: "attachment.bin").name
        val dir = File(AppConfig.get("uploads.dir"), "docks")
        dir.mkdirs()
        val file = File(dir, "${id}_$fileName")
        file.writeBytes(HttpUtils.readBytes(exchange))
        dao.updateAttachment(id, file.absolutePath)
        HttpUtils.send(exchange, 200, mapper.writeValueAsString(dao.findById(id)))
    }

    fun downloadAttachment(exchange: HttpExchange, id: Long) {
        val path = dao.findById(id)?.attachmentPath
        if (path == null) {
            HttpUtils.send(exchange, 404, "{\"error\":\"attachment not found\"}")
            return
        }
        HttpUtils.sendBytes(exchange, File(path).readBytes())
    }
}
