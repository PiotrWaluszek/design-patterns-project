package orm.connection

import java.sql.Connection
import java.sql.PreparedStatement

abstract class DatabaseConnection {
    protected var connection: Connection? = null
    abstract fun connect(): Connection
    abstract fun disconnect()
    abstract fun isConnected(): Boolean
    abstract fun prepareStatement(sql: String, returnGeneratedKeys: Boolean = false): PreparedStatement
}