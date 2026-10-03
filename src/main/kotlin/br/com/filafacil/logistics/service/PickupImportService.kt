package br.com.filafacil.logistics.service

import br.com.filafacil.logistics.dao.PickupDao
import br.com.filafacil.logistics.model.Pickup
import br.com.filafacil.logistics.model.PickupStatus
import br.com.filafacil.logistics.model.Region
import br.com.filafacil.logistics.util.EventCounter
import java.time.LocalDateTime

class PickupImportService {

    private val pickupDao = PickupDao()

    fun import(items: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val results = mutableListOf<Map<String, Any?>>()

        items.forEachIndexed { index, item ->
            try {
                val customerId = (item["customerId"] as? Number)?.toLong()
                    ?: throw IllegalArgumentException("customerId is required")
                val region = item["region"] as? String ?: throw IllegalArgumentException("region is required")
                val address = item["address"] as? String ?: ""
                val packageCount = (item["packageCount"] as? Number)?.toInt() ?: 0
                if (address.isBlank()) throw IllegalArgumentException("address is required")
                if (packageCount <= 0) throw IllegalArgumentException("packageCount must be greater than zero")

                val pickup = Pickup(
                    customerId = customerId,
                    region = Region.valueOf(region.uppercase()),
                    address = address,
                    packageCount = packageCount,
                    manualPriority = (item["manualPriority"] as? Number)?.toInt(),
                    status = PickupStatus.PENDING,
                    priority = packageCount,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )
                val saved = pickupDao.insert(pickup)
                println("[OPERATIONAL] pickup ${saved.id} created by batch import")
                EventCounter.increment("PICKUP_CREATED")
                results.add(mapOf("index" to index, "id" to saved.id))
            } catch (e: Exception) {
                results.add(mapOf("index" to index, "error" to e.message))
            }
        }

        return results
    }
}
