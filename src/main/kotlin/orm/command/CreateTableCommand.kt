package orm.command

import orm.connection.DatabaseConnection
import orm.logger.Logger
import orm.table.Column
import orm.table.Table
import java.sql.ResultSet


class CreateTableCommand(
    connection: DatabaseConnection,
    logger: Logger,
    private val table: Table<*>
) : DatabaseCommand(connection, logger) {
    
    override fun execute(): ResultSet {
        val query = buildQuery()
        logger.log("Executing query: $query")
        connection.prepareStatement(query).executeUpdate()
        return connection.prepareStatement("SELECT 1").executeQuery()
    }
    
    override fun buildQuery(): String {
        val columnsSQL = table.columns.joinToString(",\n") { column ->
            buildColumnDefinition(column)
        }
        return "CREATE TABLE IF NOT EXISTS ${table._name} (\n$columnsSQL\n)"
    }
    
    private fun buildColumnDefinition(column: Column<*>): String {
        val definition = StringBuilder()
        definition.append("${column._name} ${column.sqlType.toSqlString()}")
        
        if (column.autoIncrement) {
            definition.append(" PRIMARY KEY AUTOINCREMENT")
        }
        
        return definition.toString()
    }
    
    override fun validate(): Boolean = table.columns.isNotEmpty()
}