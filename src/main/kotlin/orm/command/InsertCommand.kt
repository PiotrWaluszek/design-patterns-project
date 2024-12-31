package orm.command

import orm.connection.DatabaseConnection
import orm.logger.Logger
import orm.table.Column
import orm.table.Table
import java.sql.ResultSet
import kotlin.io.use

class InsertCommand(
    connection: DatabaseConnection,
    logger: Logger,
    private val table: Table<*>,
    private val values: Map<Column<*>, Any?>
) : DatabaseCommand(connection, logger) {
    
    override fun execute(): ResultSet {
        val query = buildQuery()
        logger.log("Executing query: $query")
        
        return connection.prepareStatement(query, true).use { stmt ->
            values.values.forEachIndexed { index, value ->
                stmt.setObject(index + 1, value)
            }
            stmt.executeUpdate()
            stmt.generatedKeys
        }
    }
    
    override fun buildQuery(): String {
        val columns = values.keys.joinToString(", ") { it._name }
        val placeholders = values.keys.map { "?" }.joinToString(", ")
        return "INSERT INTO ${table._name} ($columns) VALUES ($placeholders)"
    }
    
    override fun validate(): Boolean = values.isNotEmpty()
}