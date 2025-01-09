package ormapping.connection

import ormapping.dialect.SQLDialect
import ormapping.dialect.SQLiteDialect
import java.sql.Connection
import java.sql.DriverManager

class SQLiteConnection private constructor(private val config: DatabaseConfig) : DatabaseConnection() {
    private val connection: Connection
    private val dialect = SQLiteDialect.Companion.create()
    
    init {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection(config.url)
        } catch (e: Exception) {
            throw ConnectionException("Failed to create SQLite connection", e)
        }
    }
    
    override fun getConnection(): Connection = connection
    override fun getDialect(): SQLDialect = dialect
    override fun close() = connection.close()
    
    companion object {
        fun create(config: DatabaseConfig): DatabaseConnection {
            return SQLiteConnection(config)
        }
    }
}