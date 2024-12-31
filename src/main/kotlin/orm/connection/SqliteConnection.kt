package orm.connection

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement

class SqliteConnection : DatabaseConnection() {
    override fun connect(): Connection {
        if (!isConnected()) {
            connection = DriverManager.getConnection("jdbc:sqlite:orm.db")
        }
        return connection!!
    }
    
    override fun disconnect() {
        connection?.close()
        connection = null
    }
    
    override fun isConnected(): Boolean =
        connection?.isClosed == false
    
    override fun prepareStatement(sql: String, returnGeneratedKeys: Boolean): PreparedStatement {
        val connection = connect()
        return if (returnGeneratedKeys) {
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        } else {
            connection.prepareStatement(sql)
        }
    }
}