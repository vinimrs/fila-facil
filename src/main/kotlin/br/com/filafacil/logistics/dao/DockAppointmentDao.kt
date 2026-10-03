package br.com.filafacil.logistics.dao

import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.model.DockAppointment
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp

class DockAppointmentDao {

    fun insert(appointment: DockAppointment): DockAppointment {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO dock_appointments (carrier, truck_plate, dock, date, start_time, end_time, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            )
            stmt.setString(1, appointment.carrier)
            stmt.setString(2, appointment.truckPlate)
            stmt.setInt(3, appointment.dock)
            stmt.setDate(4, Date.valueOf(appointment.date))
            stmt.setTime(5, Time.valueOf(appointment.startTime))
            stmt.setTime(6, Time.valueOf(appointment.endTime))
            stmt.setString(7, appointment.status)
            stmt.setTimestamp(8, Timestamp.valueOf(appointment.createdAt))
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            keys.next()
            appointment.id = keys.getLong(1)
            return appointment
        }
    }

    fun findAll(): List<DockAppointment> {
        Database.getConnection().use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM dock_appointments ORDER BY id")
            val result = mutableListOf<DockAppointment>()
            while (rs.next()) {
                result.add(map(rs))
            }
            return result
        }
    }

    fun findById(id: Long): DockAppointment? {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM dock_appointments WHERE id = ?")
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) map(rs) else null
        }
    }

    fun updateAttachment(id: Long, path: String) {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("UPDATE dock_appointments SET attachment_path = ? WHERE id = ?")
            stmt.setString(1, path)
            stmt.setLong(2, id)
            stmt.executeUpdate()
        }
    }

    private fun map(rs: ResultSet) = DockAppointment(
        id = rs.getLong("id"),
        carrier = rs.getString("carrier"),
        truckPlate = rs.getString("truck_plate"),
        dock = rs.getInt("dock"),
        date = rs.getDate("date").toLocalDate(),
        startTime = rs.getTime("start_time").toLocalTime(),
        endTime = rs.getTime("end_time").toLocalTime(),
        status = rs.getString("status"),
        attachmentPath = rs.getString("attachment_path"),
        createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
    )
}
