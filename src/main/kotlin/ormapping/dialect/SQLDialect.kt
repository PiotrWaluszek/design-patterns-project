package ormapping.dialect

abstract class SQLDialect {
    
    abstract fun getInsertIgnoreSyntax(): String
    abstract fun getUpsertSyntax(): String
    
    open fun getJoinSyntax(joinType: JoinType) = when (joinType) {
        JoinType.INNER -> "INNER JOIN"
        JoinType.LEFT -> "LEFT JOIN"
        JoinType.RIGHT -> "RIGHT JOIN"
    }
}

enum class JoinType {
    INNER, LEFT, RIGHT
}

