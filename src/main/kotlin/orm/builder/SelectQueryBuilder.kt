package orm.builder

import orm.command.SelectCommand
import orm.connection.DatabaseConnection
import orm.logger.Logger
import orm.table.Column
import orm.table.Table

class SelectQueryBuilder(private val table: Table<*>) {
    private val columns = mutableListOf<Column<*>>()
    private var condition: Pair<String, List<Any?>>? = null
    
    fun columns(vararg cols: Column<*>) = apply {
        columns.addAll(cols)
    }
    
    fun where(condition: String, vararg params: Any?) = apply {
        this.condition = condition to params.toList()
    }
    
    fun build(connection: DatabaseConnection, logger: Logger): SelectCommand {
        return SelectCommand(connection, logger, table, columns, condition)
    }
}