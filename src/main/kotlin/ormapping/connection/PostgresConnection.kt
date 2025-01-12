package ormapping.connection

import ormapping.dialect.PostgresDialect
import ormapping.dialect.SQLDialect
import java.sql.Connection
import java.sql.DriverManager

/**
 * Implementation of [DatabaseConnection] for PostgreSQL databases.
 *
 * @property config The configuration details for connecting to the PostgreSQL database.
 */
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

    /**
     * Retrieves the active PostgreSQL database connection.
     *
     * @return The active [Connection] instance.
     */
    override fun getConnection(): Connection = connection

    /**
     * Retrieves the SQL dialect specific to PostgreSQL.
     *
     * @return The [SQLDialect] instance for PostgreSQL.
     */
    override fun getDialect(): SQLDialect = dialect

    /**
     * Closes the active database connection.
     */
    override fun close() = connection.close()

    companion object {
        /**
         * Factory method to create a PostgreSQL database connection.
         *
         * @param config The configuration object containing connection details.
         * @return A [DatabaseConnection] instance for PostgreSQL.
         */
        fun create(config: DatabaseConfig): DatabaseConnection {
            return PostgresConnection(config)
        }
    }
}
