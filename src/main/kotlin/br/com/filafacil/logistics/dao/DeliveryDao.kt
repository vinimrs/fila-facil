package br.com.filafacil.logistics.dao

import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.model.Delivery
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

class DeliveryDao {

    fun findAll(): List<Delivery> {
        Database.getConnection().use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM deliveries ORDER BY id")
            val result = mutableListOf<Delivery>()
            while (rs.next()) {
                result.add(map(rs))
            }
            return result
        }
    }

    fun findById(id: Long): Delivery? {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM deliveries WHERE id = ?")
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) map(rs) else null
        }
    }

    fun updateOccurrence(id: Long, type: String, notes: String?, at: LocalDateTime) {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "UPDATE deliveries SET occurrence_type = ?, occurrence_notes = ?, occurrence_at = ? WHERE id = ?",
            )
            stmt.setString(1, type)
            stmt.setString(2, notes)
            stmt.setTimestamp(3, Timestamp.valueOf(at))
            stmt.setLong(4, id)
            stmt.executeUpdate()
        }
    }

    fun addRefundedAmount(id: Long, amount: BigDecimal) {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("UPDATE deliveries SET refunded_amount = refunded_amount + ? WHERE id = ?")
            stmt.setBigDecimal(1, amount)
            stmt.setLong(2, id)
            stmt.executeUpdate()
        }
    }

    private fun map(rs: ResultSet) = Delivery(
        id = rs.getLong("id"),
        trackingCode = rs.getString("tracking_code"),
        customerName = rs.getString("customer_name"),
        customerDocument = rs.getString("customer_document"),
        customerEmail = rs.getString("customer_email"),
        driverName = rs.getString("driver_name"),
        driverPhone = rs.getString("driver_phone"),
        vehiclePlate = rs.getString("vehicle_plate"),
        region = rs.getString("region"),
        destinationAddress = rs.getString("destination_address"),
        status = rs.getString("status"),
        freightValue = rs.getBigDecimal("freight_value") ?: BigDecimal.ZERO,
        refundedAmount = rs.getBigDecimal("refunded_amount") ?: BigDecimal.ZERO,
        scheduledDate = rs.getDate("scheduled_date")?.toLocalDate(),
        deliveredAt = rs.getTimestamp("delivered_at")?.toLocalDateTime(),
        occurrenceType = rs.getString("occurrence_type"),
        occurrenceNotes = rs.getString("occurrence_notes"),
        occurrenceAt = rs.getTimestamp("occurrence_at")?.toLocalDateTime(),
    )
}
