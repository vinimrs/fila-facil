package br.com.filafacil.logistics

import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.service.RefundService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import kotlin.test.assertEquals

class RefundServiceIntegrationTest {

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

    @Test
    fun `should approve refund`() {
        val service = RefundService()
        val refund = service.request(1, "DELAY", BigDecimal("10.00"), "test")

        val approved = service.approve(refund.id!!, "test")

        assertEquals("APPROVED", approved.status)
    }
}
