package ormapping.connection

import ormapping.dialect.PostgresDialect
import ormapping.dialect.SQLDialect
import java.sql.Connection
import java.sql.DriverManager

class PostgresConnection private constructor(private val config: DatabaseConfig) : DatabaseConnection() {
    private val connection: Connection
    private val dialect = PostgresDialect.Companion.create()
    
    init {
        try {
            Class.forName("org.postgresql.Driver")
            val username = config.username ?: throw IllegalArgumentException("Username is required for PostgreSQL")
            val password = config.password ?: throw IllegalArgumentException("Password is required for PostgreSQL")
            connection = DriverManager.getConnection(config.url, username, password)
        } catch (e: Exception) {
            throw ConnectionException("Failed to create PostgreSQL connection", e)
        }
    }
    
    override fun getConnection(): Connection = connection
    override fun getDialect(): SQLDialect = dialect
    override fun close() = connection.close()
    
    companion object {
        fun create(config: DatabaseConfig): DatabaseConnection {
            return PostgresConnection(config)
        }
    }
}