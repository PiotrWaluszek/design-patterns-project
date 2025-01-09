package ormapping.connection

import ormapping.dialect.SQLDialect
import java.sql.Connection
import java.sql.PreparedStatement

data class DatabaseConfig(
    val url: String,
    val username: String? = null,
    val password: String? = null,
)

abstract class DatabaseConnection {
    abstract fun getConnection(): Connection
    abstract fun getDialect(): SQLDialect
    abstract fun close()
    
}

class ConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)