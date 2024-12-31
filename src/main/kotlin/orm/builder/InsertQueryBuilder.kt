package orm.builder

import orm.command.InsertCommand
import orm.connection.DatabaseConnection
import orm.logger.Logger
import orm.table.Column
import orm.table.Table

class InsertQueryBuilder(private val table: Table<*>) {
    private var values: Map<Column<*>, Any?> = emptyMap()
    
    fun values(values: Map<Column<*>, Any?>) = apply { this.values = values }
    
    fun build(connection: DatabaseConnection, logger: Logger): InsertCommand {
        return InsertCommand(connection, logger, table, values)
    }
}