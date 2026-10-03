package br.com.filafacil.logistics.dao

import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.model.Refund
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

class RefundDao {

    fun insert(refund: Refund): Refund {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO refunds (delivery_id, reason, amount, status, requested_by, requested_at) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            )
            stmt.setLong(1, refund.deliveryId!!)
            stmt.setString(2, refund.reason)
            stmt.setBigDecimal(3, refund.amount)
            stmt.setString(4, refund.status)
            stmt.setString(5, refund.requestedBy)
            stmt.setTimestamp(6, Timestamp.valueOf(refund.requestedAt))
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            keys.next()
            refund.id = keys.getLong(1)
            return refund
        }
    }

    fun findAll(): List<Refund> {
        Database.getConnection().use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM refunds ORDER BY id")
            val result = mutableListOf<Refund>()
            while (rs.next()) {
                result.add(map(rs))
            }
            return result
        }
    }

    fun findById(id: Long): Refund? {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM refunds WHERE id = ?")
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) map(rs) else null
        }
    }

    fun updateStatus(id: Long, status: String, by: String?, at: LocalDateTime, notes: String? = null) {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "UPDATE refunds SET status = ?, approved_by = ?, approved_at = ?, notes = COALESCE(?, notes) WHERE id = ?",
            )
            stmt.setString(1, status)
            stmt.setString(2, by)
            stmt.setTimestamp(3, Timestamp.valueOf(at))
            stmt.setString(4, notes)
            stmt.setLong(5, id)
            stmt.executeUpdate()
        }
    }

    fun insertPayment(refundId: Long, amount: BigDecimal, paidAt: LocalDateTime) {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("INSERT INTO payments (refund_id, amount, paid_at) VALUES (?, ?, ?)")
            stmt.setLong(1, refundId)
            stmt.setBigDecimal(2, amount)
            stmt.setTimestamp(3, Timestamp.valueOf(paidAt))
            stmt.executeUpdate()
        }
    }

    private fun map(rs: ResultSet) = Refund(
        id = rs.getLong("id"),
        deliveryId = rs.getLong("delivery_id"),
        reason = rs.getString("reason"),
        amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
        status = rs.getString("status"),
        requestedBy = rs.getString("requested_by"),
        requestedAt = rs.getTimestamp("requested_at")?.toLocalDateTime(),
        approvedBy = rs.getString("approved_by"),
        approvedAt = rs.getTimestamp("approved_at")?.toLocalDateTime(),
        notes = rs.getString("notes"),
    )
}
