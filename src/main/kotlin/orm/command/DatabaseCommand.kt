package orm.command

import orm.connection.DatabaseConnection
import orm.logger.Logger
import java.sql.ResultSet


abstract class DatabaseCommand(
    protected val connection: DatabaseConnection,
    protected val logger: Logger
) {
    abstract fun execute(): ResultSet
    protected abstract fun buildQuery(): String
    abstract fun validate(): Boolean
    
    protected fun logError(message: String, error: Exception) {
        logger.error(message, error)
        throw error
    }
}