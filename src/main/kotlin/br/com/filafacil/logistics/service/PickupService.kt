package br.com.filafacil.logistics.service

import br.com.filafacil.logistics.config.AppConfig
import br.com.filafacil.logistics.dao.CustomerDao
import br.com.filafacil.logistics.dao.PickupDao
import br.com.filafacil.logistics.model.CustomerType
import br.com.filafacil.logistics.model.Pickup
import br.com.filafacil.logistics.model.PickupStatus
import br.com.filafacil.logistics.model.Region
import br.com.filafacil.logistics.util.EventCounter
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime

class PickupService {

    private val pickupDao = PickupDao()
    private val customerDao = CustomerDao()
    private val log = LoggerFactory.getLogger(PickupService::class.java)

    fun create(pickup: Pickup): Pickup {
        if (pickup.customerId == null) {
            throw IllegalArgumentException("customerId is required")
        }
        val customer = customerDao.findById(pickup.customerId!!)
            ?: throw IllegalArgumentException("customer ${pickup.customerId} not found")
        if (!customer.active) {
            throw IllegalArgumentException("customer ${customer.id} is inactive")
        }
        if (pickup.address.isBlank()) {
            throw IllegalArgumentException("address is required")
        }
        if (pickup.packageCount <= 0) {
            throw IllegalArgumentException("packageCount must be greater than zero")
        }
        val manualPriority = pickup.manualPriority
        if (manualPriority != null && (manualPriority < 0 || manualPriority > 10)) {
            throw IllegalArgumentException("manualPriority must be between 0 and 10")
        }

        pickup.status = PickupStatus.PENDING
        pickup.updatedAt = pickup.createdAt

        val strategy = AppConfig.get("prioritization.strategy")
        if (strategy == "package-count") {
            pickup.priority = pickup.packageCount
        } else if (strategy == "customer-type") {
            pickup.priority = when (customer.type) {
                CustomerType.CORPORATE -> 3
                CustomerType.PREMIUM -> 2
                CustomerType.STANDARD -> 1
            }
        } else if (strategy == "manual") {
            pickup.priority = pickup.manualPriority ?: 0
        } else {
            throw IllegalArgumentException("unknown strategy: $strategy")
        }

        val saved = pickupDao.insert(pickup)

        log.info(
            "[OPERATIONAL] pickup {} created for customer {} ({} packages, region {})",
            saved.id, customer.name, saved.packageCount, saved.region,
        )
        EventCounter.increment("PICKUP_CREATED")

        return saved
    }

    fun list(status: String?, region: String?, customerType: String?, strategy: String?): List<Pickup> {
        val effectiveStrategy = strategy ?: AppConfig.get("prioritization.strategy")
        if (effectiveStrategy != "package-count" && effectiveStrategy != "customer-type" && effectiveStrategy != "manual") {
            throw IllegalArgumentException("unknown strategy: $effectiveStrategy")
        }

        var result = pickupDao.findAll()

        if (status != null) {
            val parsed = try {
                PickupStatus.valueOf(status.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("invalid status: $status")
            }
            result = result.filter { it.status == parsed }
        }
        if (region != null) {
            val parsed = try {
                Region.valueOf(region.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("invalid region: $region")
            }
            result = result.filter { it.region == parsed }
        }
        if (customerType != null) {
            val parsed = try {
                CustomerType.valueOf(customerType.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("invalid customerType: $customerType")
            }
            result = result.filter { customerDao.findById(it.customerId!!)?.type == parsed }
        }

        return result
            .map { pickup ->
                val priority = when (effectiveStrategy) {
                    "package-count" -> pickup.packageCount
                    "customer-type" -> when (customerDao.findById(pickup.customerId!!)?.type) {
                        CustomerType.CORPORATE -> 3
                        CustomerType.PREMIUM -> 2
                        else -> 1
                    }
                    else -> pickup.manualPriority ?: 0
                }
                pickup.copy(priority = priority)
            }
            .sortedWith(compareByDescending<Pickup> { it.priority }.thenBy { it.id })
    }

    fun findById(id: Long): Pickup =
        pickupDao.findById(id) ?: throw NoSuchElementException("pickup $id not found")

    fun updateStatus(id: Long, status: String?): Pickup {
        val pickup = pickupDao.findById(id) ?: throw NoSuchElementException("pickup $id not found")
        if (status == null) {
            throw IllegalArgumentException("status is required")
        }
        val newStatus = try {
            PickupStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("invalid status: $status")
        }

        val currentStatus = pickup.status!!
        val allowed = when (currentStatus) {
            PickupStatus.PENDING -> listOf(PickupStatus.SCHEDULED, PickupStatus.CANCELED)
            PickupStatus.SCHEDULED -> listOf(PickupStatus.IN_ROUTE, PickupStatus.CANCELED)
            PickupStatus.IN_ROUTE -> listOf(PickupStatus.COLLECTED)
            PickupStatus.COLLECTED -> emptyList()
            PickupStatus.CANCELED -> emptyList()
            PickupStatus.EXPIRED -> emptyList()
        }
        if (newStatus !in allowed) {
            throw IllegalStateException("cannot change status from $currentStatus to $newStatus")
        }

        pickup.status = newStatus
        pickup.updatedAt = LocalDateTime.now()
        pickupDao.update(pickup)

        log.info("[OPERATIONAL] pickup {} status changed from {} to {}", pickup.id, currentStatus, newStatus)
        if (newStatus == PickupStatus.IN_ROUTE && AppConfig.get("routing.enabled") == "true") {
            try {
                val client = HttpClient.newHttpClient()
                val body = """{"pickupId":${pickup.id},"region":"${pickup.region}","address":"${pickup.address}"}"""
                val request = HttpRequest.newBuilder(URI(AppConfig.get("routing.url")))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                log.info("[ROUTE] pickup {} dispatched to routing (HTTP {})", pickup.id, response.statusCode())
            } catch (e: Exception) {
                log.warn("[ROUTE] could not dispatch pickup {} to routing: {}", pickup.id, e.toString())
            }
        }
        EventCounter.increment("PICKUP_STATUS_CHANGED")

        return pickup
    }

    fun reschedule(id: Long): Pickup {
        val original = pickupDao.findById(id) ?: throw NoSuchElementException("pickup $id not found")
        if (original.status != PickupStatus.CANCELED && original.status != PickupStatus.EXPIRED) {
            throw IllegalStateException("only canceled or expired pickups can be rescheduled")
        }

        val now = LocalDateTime.now()
        val pickup = Pickup()
        pickup.customerId = original.customerId
        pickup.region = original.region
        pickup.address = original.address
        pickup.packageCount = original.packageCount
        pickup.manualPriority = original.manualPriority
        pickup.status = PickupStatus.PENDING
        pickup.createdAt = now
        pickup.updatedAt = now
        pickup.rescheduledFromId = original.id

        val strategy = AppConfig.get("prioritization.strategy")
        pickup.priority = when (strategy) {
            "customer-type" -> when (customerDao.findById(original.customerId!!)?.type) {
                CustomerType.CORPORATE -> 3
                CustomerType.PREMIUM -> 2
                else -> 1
            }
            "manual" -> original.manualPriority ?: 0
            else -> original.packageCount
        }

        val saved = pickupDao.insert(pickup)
        log.info("[OPERATIONAL] pickup {} rescheduled as pickup {}", original.id, saved.id)
        EventCounter.increment("PICKUP_CREATED")
        return saved
    }

    fun delete(id: Long) {
        if (!pickupDao.deleteById(id)) {
            throw NoSuchElementException("pickup $id not found")
        }
    }

    fun summary(): Map<String, Any> {
        val byStatus = pickupDao.countByStatus()
        val all = pickupDao.findAll()
        return mapOf(
            "total" to all.size,
            "totalPackages" to all.sumOf { it.packageCount },
            "byStatus" to PickupStatus.entries.associate { it.name to (byStatus[it.name] ?: 0) },
            "byRegion" to Region.entries.associate { region -> region.name to all.count { it.region == region } },
            "events" to mapOf(
                "PICKUP_CREATED" to EventCounter.get("PICKUP_CREATED"),
                "PICKUP_STATUS_CHANGED" to EventCounter.get("PICKUP_STATUS_CHANGED"),
            ),
        )
    }

    fun saveProof(id: Long, content: ByteArray): Pickup {
        val pickup = pickupDao.findById(id) ?: throw NoSuchElementException("pickup $id not found")
        if (content.isEmpty()) {
            throw IllegalArgumentException("empty file")
        }
        val dir = File(AppConfig.get("uploads.dir"))
        dir.mkdirs()
        val file = File(dir, "pickup-$id-${System.currentTimeMillis()}.bin")
        file.writeBytes(content)

        pickup.proofPath = file.absolutePath
        pickup.updatedAt = LocalDateTime.now()
        pickupDao.update(pickup)
        log.info("proof saved for pickup {} at {}", id, file.absolutePath)
        return pickup
    }

    fun readProof(id: Long): ByteArray {
        val pickup = pickupDao.findById(id) ?: throw NoSuchElementException("pickup $id not found")
        val path = pickup.proofPath ?: throw NoSuchElementException("pickup $id has no proof")
        return File(path).readBytes()
    }
}
