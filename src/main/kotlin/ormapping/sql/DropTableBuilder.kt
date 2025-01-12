package ormapping.sql

import ormapping.command.CommandExecutor
import ormapping.connection.DatabaseConnection
import ormapping.dialect.SQLDialect
import ormapping.table.Table

/**
 * Builder that generates a `DROP TABLE` SQL statement.
 * Supports optional `IF EXISTS` and `CASCADE` clauses based on SQL dialect.
 *
 * Example output:
 * `DROP TABLE [IF EXISTS] tableName [CASCADE]`
 */
class DropTableBuilder(
    private val dialect: SQLDialect,
    table: Table<*>,
    private val executor: CommandExecutor
) : SQLBuilder {

    private var tableName: String = table._name
    private var ifExists = false
    private var cascade = false

    /**
     * Sets the target table for the drop operation using a [Table] object.
     *
     * @param table The table definition to be dropped.
     * @return The current [DropTableBuilder] instance for chaining.
     */
    fun fromTable(table: Table<*>): DropTableBuilder {
        this.tableName = table._name
        return this
    }

    /**
     * Sets the target table for the drop operation using its name.
     *
     * @param tableName The name of the table to be dropped.
     * @return The current [DropTableBuilder] instance for chaining.
     */
    fun from(tableName: String): DropTableBuilder {
        this.tableName = tableName
        return this
    }

    /**
     * Adds the `IF EXISTS` clause to the `DROP TABLE` statement.
     *
     * Note: Not all SQL dialects support this clause. Unsupported dialects may ignore it or throw an error.
     *
     * @return The current [DropTableBuilder] instance for chaining.
     */
    fun ifExists(): DropTableBuilder {
        this.ifExists = true
        return this
    }

    /**
     * Adds the `CASCADE` clause to the `DROP TABLE` statement.
     *
     * Note: The `CASCADE` clause may behave differently across SQL dialects.
     *
     * @return The current [DropTableBuilder] instance for chaining.
     */
    fun cascade(): DropTableBuilder {
        this.cascade = true
        return this
    }

    /**
     * Builds the final `DROP TABLE` SQL statement.
     *
     * @return The SQL `DROP TABLE` statement as a string.
     */
    override fun build(): String = buildString {
        append("DROP TABLE ")
        if (ifExists) {
            append("IF EXISTS ")
        }
        append(tableName)
        if (cascade) {
            append(" CASCADE")
        }
    }
}
