package br.com.filafacil.logistics.model

import java.time.LocalDate
import java.time.LocalTime

data class PickupWindow(
    val id: Long,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val maxCapacity: Int,
    val occupied: Int,
    val status: String,
)
