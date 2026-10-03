package br.com.filafacil.logistics.model

import java.time.LocalDateTime

data class Pickup(
    var id: Long? = null,
    var customerId: Long? = null,
    var region: Region = Region.CENTER,
    var address: String = "",
    var packageCount: Int = 0,
    var manualPriority: Int? = null,
    var status: PickupStatus? = null,
    var priority: Int = 0,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null,
    var rescheduledFromId: Long? = null,
    var proofPath: String? = null,
)

enum class PickupStatus {
    PENDING,
    SCHEDULED,
    IN_ROUTE,
    COLLECTED,
    CANCELED,
    EXPIRED,
}

enum class Region {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    CENTER,
}
