// SQLCommand.kt
package ormapping.command

import ormapping.connection.DatabaseConnection
import java.sql.ResultSet

abstract class SQLCommand(protected val sql: String) : Command() {
    abstract override fun execute(connection: DatabaseConnection)
}

class SelectCommand(sql: String) : SQLCommand(sql) {
    private lateinit var resultSet: ResultSet
    
    override fun execute(connection: DatabaseConnection) {
        resultSet = connection.getConnection().prepareStatement(sql).executeQuery()
    }
    
    fun getResults(): ResultSet = resultSet
    
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

class DeleteCommand(sql: String) : SQLCommand(sql) {
    private var affectedRows: Int = 0
    
    override fun execute(connection: DatabaseConnection) {
        affectedRows = connection.getConnection().prepareStatement(sql).executeUpdate()
    }
    
    fun getAffectedRows(): Int = affectedRows
}

class CreateTableCommand(sql: String) : SQLCommand(sql) {
    private var success: Boolean = false
    
    override fun execute(connection: DatabaseConnection) {
        success = connection.getConnection().prepareStatement(sql).execute()
    }
    
    fun isSuccess(): Boolean = success
}

class DropTableCommand(sql: String) : SQLCommand(sql) {
    private var success: Boolean = false
    
    override fun execute(connection: DatabaseConnection) {
        success = connection.getConnection().prepareStatement(sql).execute()
    }
    
    fun isSuccess(): Boolean = success
}