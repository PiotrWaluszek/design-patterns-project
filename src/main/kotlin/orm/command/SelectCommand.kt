package orm.command

import orm.connection.DatabaseConnection
import orm.logger.Logger
import orm.table.Column
import orm.table.Table
import java.sql.ResultSet

class SelectCommand(
    connection: DatabaseConnection,
    logger: Logger,
    private val table: Table<*>,
    private val columns: List<Column<*>>,
    private val condition: Pair<String, List<Any?>>? = null
) : DatabaseCommand(connection, logger) {
    
    override fun execute(): ResultSet {
        val query = buildQuery()
        logger.log("Executing query: $query")
        return connection.prepareStatement(query).apply {
            condition?.second?.forEachIndexed { index, value ->
                setObject(index + 1, value)
            }
        }.executeQuery()
    }
    
    override fun buildQuery(): String {
        val columnsList = columns.joinToString(", ") { it._name }
        var query = "SELECT $columnsList FROM ${table._name}"
        if (condition != null) {
            query += " WHERE ${condition.first}"
        }
        return query
    }
    
    override fun validate(): Boolean = columns.isNotEmpty()
}