package ormapping.sql

import ormapping.table.Column
import ormapping.table.Table

/**
 * Builder class for constructing `SELECT` SQL statements.
 * Supports various SQL clauses such as `FROM`, `WHERE`, `GROUP BY`, `ORDER BY`, and joins.
 * Includes support for aggregate functions, unions, and aliases.
 */
class SelectBuilder : SQLBuilder {
    private val columns = mutableListOf<String>()
    private val tables = mutableListOf<String>()
    private val joins = mutableListOf<String>()
    private val conditions = mutableListOf<String>()
    private val groupBy = mutableListOf<String>()
    private val orderBy = mutableListOf<String>()
    private var distinct = false
    private var limit: Int? = null
    private val having = mutableListOf<String>()
    private val unions = mutableListOf<String>()
    private val tableAliases = mutableMapOf<String, String>()

    /**
     * Adds a `UNION` clause to the query.
     *
     * @param query The SQL query to union with.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun union(query: String): SelectBuilder {
        unions.add("UNION $query")
        return this
    }

    /**
     * Adds a `UNION ALL` clause to the query.
     *
     * @param query The SQL query to union with.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun unionAll(query: String): SelectBuilder {
        unions.add("UNION ALL $query")
        return this
    }

    /**
     * Adds a `HAVING` clause to the query.
     *
     * @param condition The condition for the `HAVING` clause.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun having(condition: String): SelectBuilder {
        having.add(condition)
        return this
    }

    /**
     * Adds columns to the `SELECT` clause.
     *
     * @param cols The columns to select.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun select(vararg cols: Any): SelectBuilder {
        columns.addAll(cols.map {
            when (it) {
                is Column<*> -> "${it.table._name}.${it.name}"
                is String -> it
                else -> throw IllegalArgumentException("Unsupported column type: $it")
            }
        })
        return this
    }

    /**
     * Adds a table to the `FROM` clause.
     *
     * @param table The table definition.
     * @param alias An optional alias for the table.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun from(table: Table<*>, alias: String? = null): SelectBuilder {
        val tableAlias = alias ?: table._name
        tables.add("${table._name} ${alias.orEmpty()}".trim())
        table.columns.forEach { column -> tableAliases[column.name] = tableAlias }
        return this
    }

    /**
     * Adds an `INNER JOIN` clause to the query.
     *
     * @param table The table to join.
     * @param columnLeft The column from the left table.
     * @param columnRight The column from the right table.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun innerJoin(table: Table<*>, columnLeft: Column<*>, columnRight: Column<*>): SelectBuilder {
        val joinStatement = "INNER JOIN ${table._name} ON ${columnLeft.table._name}.${columnLeft.name} = ${columnRight.table._name}.${columnRight.name}".trim()
        joins.add(joinStatement)
        table.columns.forEach { column ->
            tableAliases[column.name] = table._name
        }
        return this
    }

    /**
     * Adds a `LEFT JOIN` clause to the query.
     *
     * @param table The table to join.
     * @param columnLeft The column from the left table.
     * @param columnRight The column from the right table.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun leftJoin(table: Table<*>, columnLeft: Column<*>, columnRight: Column<*>): SelectBuilder {
        val joinStatement = "LEFT JOIN ${table._name} ON ${columnLeft.table._name}.${columnLeft.name} = ${columnRight.table._name}.${columnRight.name}".trim()
        joins.add(joinStatement)
        table.columns.forEach { column ->
            tableAliases[column.name] = table._name
        }
        return this
    }

    /**
     * Adds conditions to the `WHERE` clause.
     *
     * @param conditions The conditions to add.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun where(vararg conditions: Any): SelectBuilder {
        this.conditions.addAll(conditions.map {
            when (it) {
                is Column<*> -> {
                    val tableName = tableAliases[it.name] ?: throw IllegalArgumentException("Alias for column ${it.name} not set")
                    "$tableName.${it.name}"
                }
                is String -> it
                else -> throw IllegalArgumentException("Unsupported condition type: $it")
            }
        })
        return this
    }

    /**
     * Adds columns to the `GROUP BY` clause.
     *
     * @param cols The columns to group by.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun groupBy(vararg cols: Any): SelectBuilder {
        groupBy.addAll(cols.map {
            when (it) {
                is Column<*> -> {
                    val tableName = tableAliases[it.name] ?: throw IllegalArgumentException("Alias for column ${it.name} not set")
                    "$tableName.${it.name}"
                }
                is String -> it
                else -> throw IllegalArgumentException("Unsupported column type in GROUP BY: $it")
            }
        })
        return this
    }

    /**
     * Adds columns to the `ORDER BY` clause.
     *
     * @param cols The columns to order by.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun orderBy(vararg cols: Any): SelectBuilder {
        orderBy.addAll(cols.map {
            when (it) {
                is Column<*> -> {
                    val tableName = tableAliases[it.name] ?: throw IllegalArgumentException("Alias for column ${it.name} not set")
                    "$tableName.${it.name}"
                }
                is String -> it
                else -> throw IllegalArgumentException("Unsupported column type in ORDER BY: $it")
            }
        })
        return this
    }

    /**
     * Enables the `DISTINCT` keyword in the query.
     *
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun distinct(): SelectBuilder {
        distinct = true
        return this
    }

    /**
     * Adds a `LIMIT` clause to the query.
     *
     * @param value The maximum number of rows to return.
     * @return The current [SelectBuilder] instance for chaining.
     */
    fun limit(value: Int): SelectBuilder {
        limit = value
        return this
    }

    /**
     * Helper methods for common aggregate functions.
     */
    fun count(column: String = "*"): String = "COUNT($column)"
    fun max(column: String): String = "MAX($column)"
    fun min(column: String): String = "MIN($column)"
    fun avg(column: String): String = "AVG($column)"
    fun sum(column: String): String = "SUM($column)"

    /**
     * Builds the final `SELECT` SQL query.
     *
     * @return The complete SQL query as a string.
     */
    override fun build(): String {
        if (tables.isEmpty()) throw IllegalStateException("FROM clause is required")
        return buildString {
            append("SELECT ")
            if (distinct) append("DISTINCT ")
            append(columns.joinToString(", ").ifEmpty { "*" })
            append(" FROM ")
            append(tables.joinToString(", "))
            if (joins.isNotEmpty()) append(" ").append(joins.joinToString(" "))
            if (conditions.isNotEmpty()) append(" WHERE ").append(conditions.joinToString(" AND "))
            if (groupBy.isNotEmpty()) append(" GROUP BY ").append(groupBy.joinToString(", "))
            if (having.isNotEmpty()) append(" HAVING ").append(having.joinToString(" AND "))
            if (orderBy.isNotEmpty()) append(" ORDER BY ").append(orderBy.joinToString(", "))
            limit?.let { append(" LIMIT $it") }
            if (unions.isNotEmpty()) {
                append(" ")
                append(unions.joinToString(" "))
            }
        }
    }
}
