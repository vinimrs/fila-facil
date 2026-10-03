package br.com.filafacil.logistics.service

import br.com.filafacil.logistics.model.PickupWindow
import java.time.LocalDate
import java.time.LocalTime

class PickupWindowService {

    private val windows: List<PickupWindow> = load()

    fun list(date: LocalDate?): List<PickupWindow> =
        if (date == null) windows else windows.filter { it.date == date }

    fun findById(id: Long): PickupWindow? = windows.firstOrNull { it.id == id }

    private fun load(): List<PickupWindow> {
        val lines = PickupWindowService::class.java.getResourceAsStream("/pickup-windows.csv")!!
            .bufferedReader().readLines()

        val result = mutableListOf<PickupWindow>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val cols = line.split(",")
            try {
                result.add(
                    PickupWindow(
                        id = cols[0].toLong(),
                        date = LocalDate.parse(cols[1]),
                        startTime = LocalTime.parse(cols[2]),
                        endTime = LocalTime.parse(cols[3]),
                        maxCapacity = cols[4].toInt(),
                        occupied = cols[5].toInt(),
                        status = cols[6],
                    ),
                )
            } catch (e: Exception) {
                println("WARN skipping invalid pickup window line: $line")
            }
        }
        println("Loaded ${result.size} pickup windows from pickup-windows.csv")
        return result
    }
}
