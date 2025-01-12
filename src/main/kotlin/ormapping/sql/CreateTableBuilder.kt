package ormapping.sql

import ormapping.table.Column
import ormapping.table.ForeignKey
import ormapping.table.Table
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * Builder that generates a SQL `CREATE TABLE` statement based on a [Table] object.
 * It includes:
 * - A single `PRIMARY KEY` clause for one or more columns.
 * - `FOREIGN KEY` clauses for relationships.
 */
class CreateTableBuilder : SQLBuilder {
    private var tableName: String = ""
    private val columns = mutableListOf<String>()
    private val constraints = mutableListOf<String>()

    /**
     * Configures the builder using the provided table definition.
     *
     * @param table The table definition containing column and constraint details.
     * @return The current [CreateTableBuilder] instance for chaining.
     */
    fun fromTable(table: Table<*>): CreateTableBuilder {
        tableName = table._name

        table.columns.forEach { col ->
            val sqlType = mapKClassToSQLType(col.type, col)
            val colConstraints = buildColumnConstraints(col, skipPrimaryKey = true)
            columns.add("${col.name} $sqlType ${colConstraints.joinToString(" ")}".trim())
        }

        val pkColumns = table.columns.filter { it.primaryKey }.map { it.name }
        if (pkColumns.isNotEmpty()) {
            constraints.add("PRIMARY KEY (${pkColumns.joinToString(", ")})")
        }

        table.foreignKeys.forEach { fk ->
            addForeignKeyConstraint(fk)
        }

        return this
    }

    /**
     * Builds the SQL `CREATE TABLE` statement as a string.
     *
     * @return The complete SQL `CREATE TABLE` statement.
     */
    override fun build(): String = buildString {
        append("CREATE TABLE IF NOT EXISTS $tableName (\n")
        append(columns.joinToString(",\n"))
        if (constraints.isNotEmpty()) {
            append(",\n")
            append(constraints.joinToString(",\n"))
        }
        append("\n)")
    }

    /**
     * Adds a `FOREIGN KEY` constraint definition.
     *
     * @param fk The foreign key definition.
     */
    private fun addForeignKeyConstraint(fk: ForeignKey) {
        val targetColumn = fk.targetColumns.firstOrNull() ?: return
        val referencingColumn = "${fk.targetTable}_$targetColumn"
        val referenceDefinition = "${fk.targetTable}($targetColumn)"

        constraints.add("FOREIGN KEY ($referencingColumn) REFERENCES $referenceDefinition")
    }

    /**
     * Maps a Kotlin class to its corresponding SQL data type.
     *
     * @param kClass The Kotlin class type.
     * @param column The column definition.
     * @return The SQL data type for the column.
     */
    private fun mapKClassToSQLType(kClass: KClass<*>, column: Column<*>): String {
        return when (kClass) {
            Int::class -> "INTEGER"
            String::class ->
                if (column.length > 0) "VARCHAR(${column.length})" else "TEXT"
            Boolean::class -> "BOOLEAN"
            BigDecimal::class -> "DECIMAL(${column.precision},${column.scale})"
            LocalDate::class -> "DATE"
            else -> "TEXT"
        }
    }

    /**
     * Builds the column constraints (e.g., `NOT NULL`, `UNIQUE`).
     *
     * @param column The column definition.
     * @param skipPrimaryKey Whether to skip adding `PRIMARY KEY` to the constraints.
     * @return A list of SQL constraint strings for the column.
     */
    private fun buildColumnConstraints(column: Column<*>, skipPrimaryKey: Boolean): List<String> {
        val constraints = mutableListOf<String>()
        if (!skipPrimaryKey && column.primaryKey) {
            constraints.add("PRIMARY KEY")
        }
        if (!column.nullable) {
            constraints.add("NOT NULL")
        }
        if (column.unique) {
            constraints.add("UNIQUE")
        }
        return constraints
    }
}
