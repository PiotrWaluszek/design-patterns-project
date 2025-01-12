package ormapping.dialect

class PostgresDialect private constructor() : SQLDialect() {
    override fun getInsertIgnoreSyntax() = "INSERT INTO"
    override fun getUpsertSyntax() = "ON CONFLICT"
    
    override fun getJoinSyntax(joinType: JoinType) = when (joinType) {
        JoinType.INNER -> "INNER JOIN"
        JoinType.LEFT -> "LEFT JOIN"
        JoinType.RIGHT -> "RIGHT JOIN"
    }
    
    
    companion object {
        fun create(): SQLDialect = PostgresDialect()
    }
}