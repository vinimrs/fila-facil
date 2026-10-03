package br.com.filafacil.logistics.dao

import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.model.Customer
import br.com.filafacil.logistics.model.CustomerType
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp

class CustomerDao {

    fun insert(customer: Customer): Customer {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO customers (name, document, type, email, active, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            )
            stmt.setString(1, customer.name)
            stmt.setString(2, customer.document)
            stmt.setString(3, customer.type.name)
            stmt.setString(4, customer.email)
            stmt.setBoolean(5, customer.active)
            stmt.setTimestamp(6, Timestamp.valueOf(customer.createdAt))
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            keys.next()
            customer.id = keys.getLong(1)
            return customer
        }
    }

    fun findById(id: Long): Customer? {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM customers WHERE id = ?")
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) map(rs) else null
        }
    }

    fun findAll(): List<Customer> {
        Database.getConnection().use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM customers ORDER BY id")
            val result = mutableListOf<Customer>()
            while (rs.next()) {
                result.add(map(rs))
            }
            return result
        }
    }

    fun deactivate(id: Long): Boolean {
        Database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("UPDATE customers SET active = false WHERE id = ?")
            stmt.setLong(1, id)
            return stmt.executeUpdate() > 0
        }
    }

    private fun map(rs: ResultSet) = Customer(
        id = rs.getLong("id"),
        name = rs.getString("name"),
        document = rs.getString("document"),
        type = CustomerType.valueOf(rs.getString("type")),
        email = rs.getString("email"),
        active = rs.getBoolean("active"),
        createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
    )
}
