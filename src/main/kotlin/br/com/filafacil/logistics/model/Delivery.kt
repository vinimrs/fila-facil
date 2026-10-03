package br.com.filafacil.logistics.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class Delivery(
    var id: Long? = null,
    var trackingCode: String? = null,
    var customerName: String? = null,
    var customerDocument: String? = null,
    var customerEmail: String? = null,
    var driverName: String? = null,
    var driverPhone: String? = null,
    var vehiclePlate: String? = null,
    var region: String? = null,
    var destinationAddress: String? = null,
    var status: String? = null,
    var freightValue: BigDecimal = BigDecimal.ZERO,
    var refundedAmount: BigDecimal = BigDecimal.ZERO,
    var scheduledDate: LocalDate? = null,
    var deliveredAt: LocalDateTime? = null,
    var occurrenceType: String? = null,
    var occurrenceNotes: String? = null,
    var occurrenceAt: LocalDateTime? = null,
)

data class Refund(
    var id: Long? = null,
    var deliveryId: Long? = null,
    var reason: String? = null,
    var amount: BigDecimal = BigDecimal.ZERO,
    var status: String? = null,
    var requestedBy: String? = null,
    var requestedAt: LocalDateTime? = null,
    var approvedBy: String? = null,
    var approvedAt: LocalDateTime? = null,
    var notes: String? = null,
)
