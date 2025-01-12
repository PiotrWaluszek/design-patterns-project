package ormapping.sql

import ormapping.table.Column
import ormapping.table.ForeignKey
import ormapping.table.Table
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * Builder, który na podstawie obiektu Table<*> generuje
 * polecenie CREATE TABLE ... w SQL, w tym:
 *  - jedną klauzulę PRIMARY KEY (col1, col2, ...)
 *  - klauzule FOREIGN KEY (....) REFERENCES ...
 */
class CreateTableBuilder : SQLBuilder {
    private var tableName: String = ""
    private val columns = mutableListOf<String>()
    private val constraints = mutableListOf<String>()

    fun fromTable(table: Table<*>): CreateTableBuilder {
        // 1. Ustawiamy nazwę tabeli
        tableName = table._name

        // 2. Dodajemy kolumny (pomijając "PRIMARY KEY" w definicji kolumn)
        table.columns.forEach { col ->
            val sqlType = mapKClassToSQLType(col.type, col)
            val colConstraints = buildColumnConstraints(col, skipPrimaryKey = true)
            columns.add("${col.name} $sqlType ${colConstraints.joinToString(" ")}".trim())
        }

        // 3. Jedna zbiorcza klauzula PRIMARY KEY (dla 1+ kolumn)
        val pkColumns = table.columns.filter { it.primaryKey }.map { it.name }
        if (pkColumns.isNotEmpty()) {
            constraints.add("PRIMARY KEY (${pkColumns.joinToString(", ")})")
        }

        // 4. FOREIGN KEYS (dla manyToOne, etc.)
        table.foreignKeys.forEach { fk ->
            addForeignKeyConstraint(fk)
        }

        return this
    }

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
     * Dodaje definicję FOREIGN KEY w stylu:
     *    FOREIGN KEY (some_column) REFERENCES some_table(some_column)
     */
    private fun addForeignKeyConstraint(fk: ForeignKey) {
        // Prosty przykład: bierzemy pierwszą kolumnę docelowego klucza
        // i budujemy nazwę "targetTable_targetColumn" w naszej tabeli
        val targetColumn = fk.targetColumns.firstOrNull() ?: return
        val referencingColumn = "${fk.targetTable}_$targetColumn"
        val referenceDefinition = "${fk.targetTable}($targetColumn)"

        constraints.add("FOREIGN KEY ($referencingColumn) REFERENCES $referenceDefinition")
    }

    private fun mapKClassToSQLType(kClass: KClass<*>, column: Column<*>): String {
        return when (kClass) {
            Int::class -> "INTEGER"
            String::class ->
                if (column.length > 0) "VARCHAR(${column.length})" else "TEXT"
            Boolean::class -> "BOOLEAN"
            BigDecimal::class -> "DECIMAL(${column.precision},${column.scale})"
            LocalDate::class -> "DATE"
            else -> "TEXT" // fallback
        }
    }

    /**
     * Zbiera pojedyncze constrainty (NOT NULL, UNIQUE, itp.).
     * Pomijamy "PRIMARY KEY" dla kolumn, aby generować *zbiorczy* PK.
     */
    private fun buildColumnConstraints(column: Column<*>, skipPrimaryKey: Boolean): List<String> {
        val constraints = mutableListOf<String>()
        // Pomijamy 'PRIMARY KEY' inline dla composite PK
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
