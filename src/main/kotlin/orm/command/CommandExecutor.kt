package orm.command

import orm.connection.DatabaseConnection
import orm.logger.Logger
import java.sql.ResultSet


class CommandExecutor(
    private val connection: DatabaseConnection,
    private val logger: Logger
) {
    fun execute(command: DatabaseCommand): ResultSet {
        return try {
            if (!command.validate()) {
                throw IllegalStateException("Invalid command")
            }
            command.execute()
        } catch (e: Exception) {
            logger.error("Command execution failed", e)
            throw e
        }
    }
}