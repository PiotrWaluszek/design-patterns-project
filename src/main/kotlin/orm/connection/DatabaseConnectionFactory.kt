package orm.connection

class DatabaseConnectionFactory private constructor() {
    private val connections = mutableMapOf<String, DatabaseConnection>()
    
    fun getConnection(type: String): DatabaseConnection {
        return connections.getOrPut(type) {
            when (type) {
                "sqlite" -> SqliteConnection()
                else -> throw IllegalArgumentException("Unsupported database type: $type")
            }
        }
    }
    
    companion object {
        private var instance: DatabaseConnectionFactory? = null
        fun getInstance(): DatabaseConnectionFactory =
            instance ?: DatabaseConnectionFactory().also { instance = it }
    }
}