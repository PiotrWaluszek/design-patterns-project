package ormapping.connection

import ormapping.dialect.MySQLDialect
import ormapping.dialect.SQLDialect
import java.sql.Connection
import java.sql.DriverManager

class MySQLConnection private constructor(private val config: DatabaseConfig) : DatabaseConnection() {
    private val connection: Connection
    private val dialect = MySQLDialect.Companion.create()
    
    init {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            val username = config.username ?: throw IllegalArgumentException("Username is required for MySQL")
            val password = config.password ?: throw IllegalArgumentException("Password is required for MySQL")
            connection = DriverManager.getConnection(config.url, username, password)
        } catch (e: Exception) {
            throw ConnectionException("Failed to create MySQL connection", e)
        }
    }
    
    override fun getConnection(): Connection = connection
    override fun getDialect(): SQLDialect = dialect
    override fun close() = connection.close()
    
    companion object {
        fun create(config: DatabaseConfig): DatabaseConnection {
            return MySQLConnection(config)
        }
    }
}