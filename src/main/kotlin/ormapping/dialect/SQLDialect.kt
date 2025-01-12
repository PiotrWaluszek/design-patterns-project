package ormapping.dialect

/**
 * Abstract base class for defining SQL dialects.
 * Provides a structure for implementing database-specific SQL syntax.
 */
abstract class SQLDialect {

    /**
     * Retrieves the syntax for "insert ignore" operation.
     * This method must be implemented by subclasses.
     *
     * @return The database-specific syntax for "insert ignore".
     */
    abstract fun getInsertIgnoreSyntax(): String

    /**
     * Retrieves the syntax for upsert operation.
     * This method must be implemented by subclasses.
     *
     * @return The database-specific syntax for upsert.
     */
    abstract fun getUpsertSyntax(): String

    /**
     * Retrieves the syntax for different types of SQL joins.
     * This method provides default implementations for INNER, LEFT, and RIGHT joins.
     *
     * @param joinType The type of join (INNER, LEFT, RIGHT).
     * @return The join syntax specific to the provided join type.
     */
    open fun getJoinSyntax(joinType: JoinType) = when (joinType) {
        JoinType.INNER -> "INNER JOIN"
        JoinType.LEFT -> "LEFT JOIN"
        JoinType.RIGHT -> "RIGHT JOIN"
    }
}

/**
 * Enum representing types of SQL joins.
 */
enum class JoinType {
    INNER, LEFT, RIGHT
}
