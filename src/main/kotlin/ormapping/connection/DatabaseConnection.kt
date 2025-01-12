package ormapping.connection

import ormapping.dialect.SQLDialect
import java.sql.Connection

/**
 * Configuration data class for setting up database connections.
 *
 * @property type The type of database provider (e.g., PostgreSQL, SQLite, MySQL).
 * @property url The connection URL for the database.
 * @property username Optional username for the database.
 * @property password Optional password for the database.
 */
data class DatabaseConfig(
    val type: ProviderType,
    val url: String,
    val username: String? = null,
    val password: String? = null,
)

/**
 * Enum representing the supported types of database providers.
 */
enum class ProviderType {
    POSTGRESQL, SQLITE, MYSQL
}

/**
 * Abstract base class for managing database connections.
 * This class provides methods to retrieve connections and SQL dialects,
 * and to close the connection.
 */
abstract class DatabaseConnection {

    /**
     * Retrieves the active database connection.
     *
     * @return The active database [Connection].
     */
    abstract fun getConnection(): Connection

    /**
     * Retrieves the SQL dialect associated with the database.
     *
     * @return The [SQLDialect] used by the database.
     */
    abstract fun getDialect(): SQLDialect

    /**
     * Closes the database connection.
     */
    abstract fun close()

    companion object {
        /**
         * Factory method to create a database connection based on the provided configuration.
         *
         * @param config The configuration object containing connection details.
         * @return An instance of [DatabaseConnection] specific to the database type.
         */
        fun createConnection(config: DatabaseConfig): DatabaseConnection {
            return when (config.type) {
                ProviderType.POSTGRESQL -> PostgresConnection.create(config)
                ProviderType.SQLITE -> SQLiteConnection.create(config)
                ProviderType.MYSQL -> MySQLConnection.create(config)
            }
        }
    }
}

/**
 * Exception class for handling database connection errors.
 *
 * @param message The error message.
 * @param cause The underlying cause of the exception, if any.
 */
class ConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)
