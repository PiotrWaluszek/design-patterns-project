package ormapping.dialect

/**
 * Implementation of [SQLDialect] for SQLite databases.
 * Provides specific syntax for SQLite operations such as insert ignore, upsert, and joins.
 */
class SQLiteDialect private constructor() : SQLDialect() {

    /**
     * Retrieves the syntax for "insert ignore" operation in SQLite.
     *
     * @return The "INSERT OR IGNORE INTO" syntax.
     */
    override fun getInsertIgnoreSyntax() = "INSERT OR IGNORE INTO"

    /**
     * Retrieves the syntax for upsert operation in SQLite.
     *
     * @return The "ON CONFLICT" syntax.
     */
    override fun getUpsertSyntax() = "ON CONFLICT"

    /**
     * Retrieves the syntax for different types of SQL joins in SQLite.
     *
     * @param joinType The type of join (INNER, LEFT, RIGHT).
     * @return The join syntax specific to the provided join type.
     *         Note: SQLite does not support RIGHT JOIN, so LEFT JOIN is used instead.
     */
    override fun getJoinSyntax(joinType: JoinType) = when (joinType) {
        JoinType.INNER -> "INNER JOIN"
        JoinType.LEFT -> "LEFT JOIN"
        JoinType.RIGHT -> "LEFT JOIN"
    }

    companion object {
        /**
         * Factory method to create an instance of [SQLiteDialect].
         *
         * @return A new instance of [SQLiteDialect].
         */
        fun create(): SQLDialect = SQLiteDialect()
    }
}
