package br.com.filafacil.logistics.service

import br.com.filafacil.logistics.dao.DeliveryDao
import br.com.filafacil.logistics.dao.RefundDao
import br.com.filafacil.logistics.model.Refund
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

class RefundService {

    private val refundDao = RefundDao()
    private val deliveryDao = DeliveryDao()
    private val log = LoggerFactory.getLogger(RefundService::class.java)

    fun request(deliveryId: Long?, reason: String?, amount: BigDecimal?, requestedBy: String?): Refund {
        if (deliveryId == null) throw IllegalArgumentException("deliveryId is required")
        if (reason !in listOf("DELAY", "DAMAGED_PACKAGE", "INCOMPLETE_DELIVERY")) {
            throw IllegalArgumentException("invalid reason: $reason")
        }
        if (amount == null || amount <= BigDecimal.ZERO) throw IllegalArgumentException("amount must be greater than zero")

        val refund = Refund(
            deliveryId = deliveryId,
            reason = reason,
            amount = amount,
            status = "REQUESTED",
            requestedBy = requestedBy,
            requestedAt = LocalDateTime.now(),
        )
        return refundDao.insert(refund)
    }

    fun approve(id: Long, approvedBy: String?): Refund {
        val refund = refundDao.findById(id) ?: throw NoSuchElementException("refund $id not found")
        if (refund.status != "REQUESTED") {
            throw IllegalStateException("refund $id is already ${refund.status}")
        }

        refundDao.updateStatus(id, "APPROVED", approvedBy, LocalDateTime.now())

        val delivery = deliveryDao.findById(refund.deliveryId!!)
            ?: throw NoSuchElementException("delivery ${refund.deliveryId} not found")
        if (delivery.refundedAmount + refund.amount > delivery.freightValue) {
            throw IllegalStateException("refund exceeds freight value of delivery ${delivery.id}")
        }

        refundDao.insertPayment(id, refund.amount, LocalDateTime.now())
        deliveryDao.addRefundedAmount(delivery.id!!, refund.amount)

        log.info("refund {} approved by {} ({})", id, approvedBy, refund.amount)
        return refundDao.findById(id)!!
    }

    fun reject(id: Long, rejectedBy: String?, notes: String?): Refund {
        val refund = refundDao.findById(id) ?: throw NoSuchElementException("refund $id not found")
        if (refund.status != "REQUESTED") {
            throw IllegalStateException("refund $id is already ${refund.status}")
        }
        refundDao.updateStatus(id, "REJECTED", rejectedBy, LocalDateTime.now(), notes)
        return refundDao.findById(id)!!
    }

    fun list(status: String?): List<Refund> {
        val all = refundDao.findAll()
        return if (status == null) all else all.filter { it.status == status.uppercase() }
    }

    fun findById(id: Long): Refund = refundDao.findById(id) ?: throw NoSuchElementException("refund $id not found")

    fun report(): Map<String, Any?> {
        val refunds = refundDao.findAll()
        val byDriver = mutableMapOf<String, BigDecimal>()
        val byCustomer = mutableMapOf<String, BigDecimal>()
        var orphans = 0

        for (refund in refunds) {
            val delivery = deliveryDao.findById(refund.deliveryId!!)
            if (delivery == null) {
                orphans++
                continue
            }
            if (refund.status == "APPROVED") {
                byDriver[delivery.driverName ?: "?"] = (byDriver[delivery.driverName ?: "?"] ?: BigDecimal.ZERO) + refund.amount
                byCustomer[delivery.customerName ?: "?"] = (byCustomer[delivery.customerName ?: "?"] ?: BigDecimal.ZERO) + refund.amount
            }
        }

        val decided = refunds.count { it.status == "APPROVED" || it.status == "REJECTED" }
        val approved = refunds.count { it.status == "APPROVED" }

        return mapOf(
            "totalRefunds" to refunds.size,
            "byStatus" to refunds.groupingBy { it.status }.eachCount(),
            "byReason" to refunds.groupingBy { it.reason }.eachCount(),
            "approvedAmount" to refunds.filter { it.status == "APPROVED" }.sumOf { it.amount },
            "approvalRate" to if (decided == 0) null else BigDecimal(approved * 100).divide(BigDecimal(decided), 1, RoundingMode.HALF_UP),
            "approvedAmountByDriver" to byDriver,
            "approvedAmountByCustomer" to byCustomer,
            "refundsWithoutDelivery" to orphans,
        )
    }
}
