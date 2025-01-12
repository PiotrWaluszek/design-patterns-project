package ormapping.sql

import ormapping.table.Column
import ormapping.table.Table

/**
 * Builder that generates a `DELETE FROM` SQL statement with optional
 * `WHERE` clauses and cascade options (depending on SQL dialect).
 */
class DeleteBuilder : SQLBuilder {
    private var tableName: String = ""
    private val conditions = mutableListOf<String>()
    private var cascade = false

    /**
     * Sets the target table for deletion using a [Table] object.
     *
     * @param table The table definition from which records will be deleted.
     * @return The current [DeleteBuilder] instance for chaining.
     */
    fun from(table: Table<*>): DeleteBuilder {
        this.tableName = table._name
        return this
    }

    /**
     * Sets the target table for deletion using its name.
     *
     * @param tableName The name of the table from which records will be deleted.
     * @return The current [DeleteBuilder] instance for chaining.
     */
    fun from(tableName: String): DeleteBuilder {
        this.tableName = tableName
        return this
    }

    /**
     * Adds a condition to the `WHERE` clause.
     * Multiple conditions will be combined with `AND`.
     *
     * @param condition The condition to add.
     * @return The current [DeleteBuilder] instance for chaining.
     */
    fun where(condition: String): DeleteBuilder {
        conditions.add(condition)
        return this
    }

    /**
     * Overloaded method to add a simple condition to the `WHERE` clause.
     * Constructs conditions like `id = 5` based on column, operator, and value.
     *
     * Example usage:
     * `.where(Employees.id, "=", 1)`
     *
     * @param column The column involved in the condition.
     * @param operator The operator (e.g., `=`, `<`, `>`).
     * @param value The value to compare against.
     * @return The current [DeleteBuilder] instance for chaining.
     */
    fun where(column: Column<*>, operator: String, value: Any?): DeleteBuilder {
        val condition = buildString {
            append(column.name)
            append(" ")
            append(operator)
            append(" ")
            if (value is String) {
                append("'$value'")
            } else {
                append(value)
            }
        }
        conditions.add(condition)
        return this
    }

    /**
     * Enables cascading delete (depending on SQL dialect support).
     *
     * Note: Cascade delete typically applies to scenarios where foreign key
     * constraints are defined with `ON DELETE CASCADE`.
     *
     * @return The current [DeleteBuilder] instance for chaining.
     */
    fun cascade(): DeleteBuilder {
        cascade = true
        return this
    }

    /**
     * Builds the final SQL `DELETE` command.
     *
     * Example output:
     * `DELETE FROM employees WHERE id = 1 [CASCADE]`
     *
     * @return The SQL `DELETE` statement as a string.
     */
    override fun build(): String = buildString {
        append("DELETE FROM $tableName")
        if (conditions.isNotEmpty()) {
            append(" WHERE ")
            append(conditions.joinToString(" AND "))
        }
        if (cascade) {
            append(" CASCADE")
        }
    }
}
