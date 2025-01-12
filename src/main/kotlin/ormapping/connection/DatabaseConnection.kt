package ormapping.connection

import ormapping.dialect.SQLDialect
import java.sql.Connection

data class DatabaseConfig(
    val type: ProviderType,
    val url: String,
    val username: String? = null,
    val password: String? = null,
)

enum class ProviderType {
    POSTGRESQL, SQLITE, MYSQL
}

abstract class DatabaseConnection {
    abstract fun getConnection(): Connection
    abstract fun getDialect(): SQLDialect
    abstract fun close()
    
    companion object {
        fun createConnection(config: DatabaseConfig): DatabaseConnection {
            return when (config.type) {
                ProviderType.POSTGRESQL -> PostgresConnection.create(config)
                ProviderType.SQLITE -> SQLiteConnection.create(config)
                ProviderType.MYSQL -> MySQLConnection.create(config)
            }
        }
    }
}

class ConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)