// SQLCommand.kt
package ormapping.command

import ormapping.connection.DatabaseConnection
import java.sql.ResultSet

/**
 * Abstract base class representing an SQL command.
 *
 * @property sql The SQL query or statement to be executed.
 */
abstract class SQLCommand(protected val sql: String) : Command() {
    /**
     * Executes the SQL command using the provided database connection.
     *
     * @param connection The database connection used for execution.
     */
    abstract override fun execute(connection: DatabaseConnection)
}

/**
 * Represents a SELECT SQL command.
 * Used to execute queries and retrieve results from the database.
 *
 * @param sql The SELECT SQL query to be executed.
 */
class SelectCommand(sql: String) : SQLCommand(sql) {
    private lateinit var resultSet: ResultSet

    /**
     * Executes the SELECT SQL command and stores the result set.
     *
     * @param connection The database connection used for execution.
     */
    override fun execute(connection: DatabaseConnection) {
        resultSet = connection.getConnection().prepareStatement(sql).executeQuery()
    }

    /**
     * Retrieves the result set of the executed query.
     *
     * @return The ResultSet object containing the query results.
     */
    fun getResults(): ResultSet = resultSet

    /**
     * Prints the results of the executed query to the console.
     */
    fun printResults() {
        while (resultSet.next()) {
            val metaData = resultSet.metaData
            val columnCount = metaData.columnCount

            for (i in 1..columnCount) {
                val columnName = metaData.getColumnName(i)
                val value = resultSet.getString(i)
                print("$columnName: $value | ")
            }
            println()
        }
    }
}

/**
 * Represents a DELETE SQL command.
 * Used to execute delete operations in the database.
 *
 * @param sql The DELETE SQL query to be executed.
 */
class DeleteCommand(sql: String) : SQLCommand(sql) {
    private var affectedRows: Int = 0

    /**
     * Executes the DELETE SQL command and stores the number of affected rows.
     *
     * @param connection The database connection used for execution.
     */
    override fun execute(connection: DatabaseConnection) {
        affectedRows = connection.getConnection().prepareStatement(sql).executeUpdate()
    }

    /**
     * Retrieves the number of rows affected by the DELETE operation.
     *
     * @return The number of affected rows.
     */
    fun getAffectedRows(): Int = affectedRows
}

/**
 * Represents a CREATE TABLE SQL command.
 * Used to create new tables in the database.
 *
 * @param sql The CREATE TABLE SQL statement to be executed.
 */
class CreateTableCommand(sql: String) : SQLCommand(sql) {
    private var success: Boolean = false

    /**
     * Executes the CREATE TABLE SQL command and stores whether it succeeded.
     *
     * @param connection The database connection used for execution.
     */
    override fun execute(connection: DatabaseConnection) {
        success = connection.getConnection().prepareStatement(sql).execute()
    }

    /**
     * Checks if the CREATE TABLE operation was successful.
     *
     * @return True if successful, false otherwise.
     */
    fun isSuccess(): Boolean = success
}

/**
 * Represents a DROP TABLE SQL command.
 * Used to remove tables from the database.
 *
 * @param sql The DROP TABLE SQL statement to be executed.
 */
class DropTableCommand(sql: String) : SQLCommand(sql) {
    private var success: Boolean = false

    /**
     * Executes the DROP TABLE SQL command and stores whether it succeeded.
     *
     * @param connection The database connection used for execution.
     */
    override fun execute(connection: DatabaseConnection) {
        success = connection.getConnection().prepareStatement(sql).execute()
    }

    /**
     * Checks if the DROP TABLE operation was successful.
     *
     * @return True if successful, false otherwise.
     */
    fun isSuccess(): Boolean = success
}
