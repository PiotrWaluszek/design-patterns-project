package ormapping.dialect

class SQLiteDialect private constructor() : SQLDialect() {
    override fun getInsertIgnoreSyntax() = "INSERT OR IGNORE INTO"
    override fun getUpsertSyntax() = "ON CONFLICT"
    
    override fun getJoinSyntax(joinType: JoinType) = when (joinType) {
        JoinType.INNER -> "INNER JOIN"
        JoinType.LEFT -> "LEFT JOIN"
        JoinType.RIGHT -> "LEFT JOIN" // SQLite nie wspiera RIGHT JOIN, używamy LEFT
    }
    
    companion object {
        fun create(): SQLDialect = SQLiteDialect()
    }
}