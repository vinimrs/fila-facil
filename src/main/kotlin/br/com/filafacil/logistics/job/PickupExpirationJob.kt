package br.com.filafacil.logistics.job

import br.com.filafacil.logistics.config.AppConfig
import br.com.filafacil.logistics.dao.PickupDao
import br.com.filafacil.logistics.model.PickupStatus
import br.com.filafacil.logistics.util.EventCounter
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PickupExpirationJob {

    private val pickupDao = PickupDao()
    private val log = LoggerFactory.getLogger(PickupExpirationJob::class.java)

    fun start() {
        val interval = AppConfig.get("expiration.interval.minutes").toLong()
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({ run() }, 1, interval, TimeUnit.MINUTES)
        log.info("pickup expiration job scheduled every {} minutes", interval)
    }

    fun run() {
        try {
            val limit = LocalDateTime.now().minusHours(AppConfig.get("expiration.pending.max.hours").toLong())
            val expired = pickupDao.findAll().filter { it.status == PickupStatus.PENDING && it.createdAt!!.isBefore(limit) }

            for (pickup in expired) {
                pickup.status = PickupStatus.EXPIRED
                pickup.updatedAt = LocalDateTime.now()
                pickupDao.update(pickup)
                log.info("[OPERATIONAL] pickup {} status changed from PENDING to EXPIRED", pickup.id)
                EventCounter.increment("PICKUP_STATUS_CHANGED")
            }
            log.info("expiration job finished: {} pickups expired", expired.size)
        } catch (e: Exception) {
            log.error("expiration job failed", e)
        }
    }
}
