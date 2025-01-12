package ormapping.connection

import ormapping.dialect.MySQLDialect
import ormapping.dialect.SQLDialect
import java.sql.Connection
import java.sql.DriverManager

/**
 * Implementation of [DatabaseConnection] for MySQL databases.
 *
 * @property config The configuration details for connecting to the MySQL database.
 */
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

    /**
     * Retrieves the active MySQL database connection.
     *
     * @return The active [Connection] instance.
     */
    override fun getConnection(): Connection = connection

    /**
     * Retrieves the SQL dialect specific to MySQL.
     *
     * @return The [SQLDialect] instance for MySQL.
     */
    override fun getDialect(): SQLDialect = dialect

    /**
     * Closes the active database connection.
     */
    override fun close() = connection.close()

    companion object {
        /**
         * Factory method to create a MySQL database connection.
         *
         * @param config The configuration object containing connection details.
         * @return A [DatabaseConnection] instance for MySQL.
         */
        fun create(config: DatabaseConfig): DatabaseConnection {
            return MySQLConnection(config)
        }
    }
}
