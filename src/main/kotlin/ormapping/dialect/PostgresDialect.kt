package ormapping.dialect

/**
 * Implementation of [SQLDialect] for PostgreSQL databases.
 * Provides specific syntax for PostgreSQL operations such as insert ignore, upsert, and joins.
 */
class PostgresDialect private constructor() : SQLDialect() {

    /**
     * Retrieves the syntax for "insert ignore" operation in PostgreSQL.
     *
     * @return The "INSERT INTO" syntax.
     */
    override fun getInsertIgnoreSyntax() = "INSERT INTO"

    /**
     * Retrieves the syntax for upsert operation in PostgreSQL.
     *
     * @return The "ON CONFLICT" syntax.
     */
    override fun getUpsertSyntax() = "ON CONFLICT"

    /**
     * Retrieves the syntax for different types of SQL joins in PostgreSQL.
     *
     * @param joinType The type of join (INNER, LEFT, RIGHT).
     * @return The join syntax specific to the provided join type.
     */
    override fun getJoinSyntax(joinType: JoinType) = when (joinType) {
        JoinType.INNER -> "INNER JOIN"
        JoinType.LEFT -> "LEFT JOIN"
        JoinType.RIGHT -> "RIGHT JOIN"
    }

    companion object {
        /**
         * Factory method to create an instance of [PostgresDialect].
         *
         * @return A new instance of [PostgresDialect].
         */
        fun create(): SQLDialect = PostgresDialect()
    }
}
