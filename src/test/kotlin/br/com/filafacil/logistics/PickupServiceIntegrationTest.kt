package br.com.filafacil.logistics

import br.com.filafacil.logistics.dao.CustomerDao
import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.model.Customer
import br.com.filafacil.logistics.model.CustomerType
import br.com.filafacil.logistics.model.Pickup
import br.com.filafacil.logistics.model.PickupStatus
import br.com.filafacil.logistics.model.Region
import br.com.filafacil.logistics.service.PickupService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PickupServiceIntegrationTest {

    companion object {
        private val postgres = PostgreSQLContainer("postgres:17")

        @JvmStatic
        @BeforeAll
        fun setup() {
            postgres.start()
            Database.init(postgres.jdbcUrl, postgres.username, postgres.password)
            Database.migrate()
        }
    }

    private val service = PickupService()

    @Test
    fun `should create pickup as pending`() {
        val customer = CustomerDao().insert(
            Customer(name = "Test", document = "123", type = CustomerType.PREMIUM, createdAt = LocalDateTime.now()),
        )

        val pickup = service.create(
            Pickup(customerId = customer.id, region = Region.SOUTH, address = "Rua Teste, 1", packageCount = 5, createdAt = LocalDateTime.now()),
        )

        assertEquals(PickupStatus.PENDING, pickup.status)
        assertEquals(5, pickup.priority)
    }

    @Test
    fun `should move pickup to scheduled`() {
        val customer = CustomerDao().insert(
            Customer(name = "Test 2", document = "456", type = CustomerType.STANDARD, createdAt = LocalDateTime.now()),
        )
        val pickup = service.create(
            Pickup(customerId = customer.id, region = Region.NORTH, address = "Rua Teste, 2", packageCount = 1, createdAt = LocalDateTime.now()),
        )

        val updated = service.updateStatus(pickup.id!!, "SCHEDULED")

        assertEquals(PickupStatus.SCHEDULED, updated.status)
        assertFailsWith<IllegalStateException> { service.updateStatus(pickup.id!!, "PENDING") }
    }
}
