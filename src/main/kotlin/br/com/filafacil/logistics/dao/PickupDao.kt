package br.com.filafacil.logistics.dao

import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.model.Pickup
import br.com.filafacil.logistics.model.PickupStatus
import br.com.filafacil.logistics.model.Region
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.sql.Types

class PickupDao {

    fun insert(pickup: Pickup): Pickup {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO pickups (customer_id, region, address, package_count, manual_priority, status, priority, " +
                    "created_at, updated_at, rescheduled_from_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            )
            stmt.setLong(1, pickup.customerId!!)
            stmt.setString(2, pickup.region.name)
            stmt.setString(3, pickup.address)
            stmt.setInt(4, pickup.packageCount)
            if (pickup.manualPriority != null) stmt.setInt(5, pickup.manualPriority!!) else stmt.setNull(5, Types.INTEGER)
            stmt.setString(6, pickup.status!!.name)
            stmt.setInt(7, pickup.priority)
            stmt.setTimestamp(8, Timestamp.valueOf(pickup.createdAt))
            stmt.setTimestamp(9, Timestamp.valueOf(pickup.updatedAt))
            if (pickup.rescheduledFromId != null) stmt.setLong(10, pickup.rescheduledFromId!!) else stmt.setNull(10, Types.BIGINT)
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            keys.next()
            pickup.id = keys.getLong(1)
            return pickup
        }
    }

    fun update(pickup: Pickup) {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "UPDATE pickups SET status = ?, priority = ?, updated_at = ?, proof_path = ? WHERE id = ?",
            )
            stmt.setString(1, pickup.status!!.name)
            stmt.setInt(2, pickup.priority)
            stmt.setTimestamp(3, Timestamp.valueOf(pickup.updatedAt))
            stmt.setString(4, pickup.proofPath)
            stmt.setLong(5, pickup.id!!)
            stmt.executeUpdate()
        }
    }

    fun findById(id: Long): Pickup? {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM pickups WHERE id = ?")
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) map(rs) else null
        }
    }

    fun findAll(): List<Pickup> {
        Database.getConnection().use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM pickups")
            val result = mutableListOf<Pickup>()
            while (rs.next()) {
                result.add(map(rs))
            }
            return result
        }
    }

    fun deleteById(id: Long): Boolean {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("DELETE FROM pickups WHERE id = ?")
            stmt.setLong(1, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun countByStatus(): Map<String, Int> {
        Database.getConnection().use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT status, COUNT(*) AS total FROM pickups GROUP BY status")
            val result = mutableMapOf<String, Int>()
            while (rs.next()) {
                result[rs.getString("status")] = rs.getInt("total")
            }
            return result
        }
    }

    private fun map(rs: ResultSet) = Pickup(
        id = rs.getLong("id"),
        customerId = rs.getLong("customer_id"),
        region = Region.valueOf(rs.getString("region")),
        address = rs.getString("address"),
        packageCount = rs.getInt("package_count"),
        manualPriority = rs.getInt("manual_priority").takeUnless { rs.wasNull() },
        status = PickupStatus.valueOf(rs.getString("status")),
        priority = rs.getInt("priority"),
        createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
        rescheduledFromId = rs.getLong("rescheduled_from_id").takeUnless { rs.wasNull() },
        proofPath = rs.getString("proof_path"),
    )
}
