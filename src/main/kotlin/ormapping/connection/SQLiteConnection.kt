package ormapping.connection

import ormapping.dialect.SQLDialect
import ormapping.dialect.SQLiteDialect
import java.sql.Connection
import java.sql.DriverManager

/**
 * Implementation of [DatabaseConnection] for SQLite databases.
 *
 * @property config The configuration details for connecting to the SQLite database.
 */
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

    /**
     * Retrieves the active SQLite database connection.
     *
     * @return The active [Connection] instance.
     */
    override fun getConnection(): Connection = connection

    /**
     * Retrieves the SQL dialect specific to SQLite.
     *
     * @return The [SQLDialect] instance for SQLite.
     */
    override fun getDialect(): SQLDialect = dialect

    /**
     * Closes the active database connection.
     */
    override fun close() = connection.close()

    companion object {
        /**
         * Factory method to create an SQLite database connection.
         *
         * @param config The configuration object containing connection details.
         * @return A [DatabaseConnection] instance for SQLite.
         */
        fun create(config: DatabaseConfig): DatabaseConnection {
            return SQLiteConnection(config)
        }
    }
}
