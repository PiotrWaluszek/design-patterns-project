package ormapping.dialect

class MySQLDialect private constructor() : SQLDialect() {
    override fun getInsertIgnoreSyntax() = "INSERT IGNORE INTO"
    override fun getUpsertSyntax() = "ON DUPLICATE KEY UPDATE"
    
    companion object {
        fun create(): SQLDialect = MySQLDialect()
    }
}