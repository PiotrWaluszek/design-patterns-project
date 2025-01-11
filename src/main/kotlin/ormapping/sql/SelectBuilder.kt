package ormapping.sql

import ormapping.table.Column
import ormapping.table.Table

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
    private val tableAliases = mutableMapOf<String, String>() // Mapowanie nazwy kolumny -> aliasu tabeli

    fun union(query: String): SelectBuilder {
        unions.add("UNION $query")
        return this
    }

    fun unionAll(query: String): SelectBuilder {
        unions.add("UNION ALL $query")
        return this
    }

    fun having(condition: String): SelectBuilder {
        having.add(condition)
        return this
    }

    fun select(vararg cols: Any): SelectBuilder {
        columns.addAll(cols.map {
            when (it) {
                is Column<*> -> {
                    "${it.table._name}.${it.name}"
                }
                is String -> it
                else -> throw IllegalArgumentException("Unsupported column type: $it")
            }
        })
        return this
    }

    fun from(table: Table<*>, alias: String? = null): SelectBuilder {
        val tableAlias = alias ?: table._name
        tables.add("${table._name} ${alias.orEmpty()}".trim())
        table.columns.forEach { column -> tableAliases[column.name] = tableAlias } // Rejestracja aliasów
        return this
    }

    fun innerJoin(table: Table<*>, columnLeft: Column<*>, columnRight: Column<*>): SelectBuilder {
        val joinStatement = "INNER JOIN ${table._name} ON ${columnLeft.table._name}.${columnLeft.name} = ${columnRight.table._name}.${columnRight.name}".trim()
        joins.add(joinStatement)
//        table.columns.forEach { column -> tableAliases[column.name] = alias ?: table._name } // Rejestracja aliasów
        table.columns.forEach { column ->
            tableAliases[column.name] = table._name
        }

        return this
    }

    fun leftJoin(table: Table<*>, columnLeft: Column<*>, columnRight: Column<*>): SelectBuilder {
        val joinStatement = "LEFT JOIN ${table._name} ON ${columnLeft.table._name}.${columnLeft.name} = ${columnRight.table._name}.${columnRight.name}".trim()
        joins.add(joinStatement)
//      table.columns.forEach { column -> tableAliases[column.name] = alias ?: table._name } // Rejestracja aliasów
        table.columns.forEach { column ->
            tableAliases[column.name] = table._name
        }

        return this
    }

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

    fun distinct(): SelectBuilder {
        distinct = true
        return this
    }

    fun limit(value: Int): SelectBuilder {
        limit = value
        return this
    }

    fun count(column: String = "*"): String = "COUNT($column)"
    fun max(column: String): String = "MAX($column)"
    fun min(column: String): String = "MIN($column)"
    fun avg(column: String): String = "AVG($column)"
    fun sum(column: String): String = "SUM($column)"

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
