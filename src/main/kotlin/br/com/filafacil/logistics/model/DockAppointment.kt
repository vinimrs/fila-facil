package br.com.filafacil.logistics.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class DockAppointment(
    var id: Long? = null,
    var carrier: String = "",
    var truckPlate: String = "",
    var dock: Int = 0,
    var date: LocalDate? = null,
    var startTime: LocalTime? = null,
    var endTime: LocalTime? = null,
    var status: String = "SCHEDULED",
    var attachmentPath: String? = null,
    var createdAt: LocalDateTime? = null,
)
