package ormapping.dialect

/**
 * Implementation of [SQLDialect] for MySQL databases.
 * Provides specific syntax for MySQL operations such as insert ignore and upsert.
 */
class MySQLDialect private constructor() : SQLDialect() {

    /**
     * Retrieves the syntax for "insert ignore" operation in MySQL.
     *
     * @return The "INSERT IGNORE INTO" syntax.
     */
    override fun getInsertIgnoreSyntax() = "INSERT IGNORE INTO"

    /**
     * Retrieves the syntax for upsert operation in MySQL.
     *
     * @return The "ON DUPLICATE KEY UPDATE" syntax.
     */
    override fun getUpsertSyntax() = "ON DUPLICATE KEY UPDATE"

    companion object {
        /**
         * Factory method to create an instance of [MySQLDialect].
         *
         * @return A new instance of [MySQLDialect].
         */
        fun create(): SQLDialect = MySQLDialect()
    }
}
