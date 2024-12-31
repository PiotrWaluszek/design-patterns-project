package orm.builder

import orm.command.CreateTableCommand
import orm.connection.DatabaseConnection
import orm.logger.Logger
import orm.table.Table

class CreateTableQueryBuilder(private val table: Table<*>) {
    fun build(connection: DatabaseConnection, logger: Logger): CreateTableCommand {
        return CreateTableCommand(connection, logger, table)
    }
}