package br.com.filafacil.logistics.model

import java.time.LocalDateTime

data class Customer(
    var id: Long? = null,
    var name: String = "",
    var document: String = "",
    var type: CustomerType = CustomerType.STANDARD,
    var email: String? = null,
    var active: Boolean = true,
    var createdAt: LocalDateTime? = null,
)

enum class CustomerType {
    STANDARD,
    PREMIUM,
    CORPORATE,
}
