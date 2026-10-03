package br.com.filafacil.logistics.dao

import br.com.filafacil.logistics.db.Database
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

class ManifestDao {

    fun insert(routeCode: String?, uploadedBy: String?, content: String): Long {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO manifests (route_code, uploaded_by, content, created_at) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            )
            stmt.setString(1, routeCode)
            stmt.setString(2, uploadedBy)
            stmt.setString(3, content)
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()))
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            keys.next()
            return keys.getLong(1)
        }
    }

    fun findAll(): List<Map<String, Any?>> {
        Database.getConnection().use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT id, route_code, uploaded_by, created_at FROM manifests ORDER BY id")
            val result = mutableListOf<Map<String, Any?>>()
            while (rs.next()) {
                result.add(
                    mapOf(
                        "id" to rs.getLong("id"),
                        "routeCode" to rs.getString("route_code"),
                        "uploadedBy" to rs.getString("uploaded_by"),
                        "createdAt" to rs.getTimestamp("created_at").toLocalDateTime(),
                    ),
                )
            }
            return result
        }
    }

    fun findContent(id: Long): String? {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT content FROM manifests WHERE id = ?")
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getString("content") else null
        }
    }
}
