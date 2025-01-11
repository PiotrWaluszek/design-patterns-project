package ormapping.sql

import ormapping.table.Column
import ormapping.table.Table
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * Builder, który na podstawie struktury obiektu Table<*>
 * generuje polecenie CREATE TABLE ... w SQL.
 */
class CreateTableBuilder : SQLBuilder {
    private var tableName: String = ""
    private val columns = mutableListOf<String>()
    private val constraints = mutableListOf<String>()

    /**
     * Odczytuje metadane z obiektu [table] i tworzy definicje kolumn, kluczy obcych etc.
     */
    fun fromTable(table: Table<*>): CreateTableBuilder {
        // Ustawiamy nazwę tabeli
        tableName = table._name

        // Dodajemy kolumny
        table.columns.forEach { col ->
            val sqlType = mapKClassToSQLType(col.type, col)
            val colConstraints = buildColumnConstraints(col)
            // Składamy definicję kolumny np. "id INTEGER PRIMARY KEY NOT NULL"
            column(col.name, sqlType, *colConstraints.toTypedArray())
        }

        // Jeżeli mamy klucze obce, można je też tu przetwarzać i dodawać do constraints
        table.foreignKeys.forEach { fk ->
            // Dla uproszczenia: zakładamy, że jest tylko jedna kolumna w kluczu (lub bierzemy pierwszą).
            val targetColumn = fk.targetColumns.firstOrNull() ?: return@forEach

            // FOREIGN KEY (orders_customer_id) REFERENCES customers(id)
            val referencingColumn = "${fk.targetTable}_$targetColumn"
            val referenceDefinition = "${fk.targetTable}($targetColumn)"
            foreignKey(referencingColumn, referenceDefinition)
        }

        return this
    }

    /**
     * Ręczne ustawienie nazwy tabeli (jeśli chcemy).
     */
    fun name(name: String): CreateTableBuilder {
        tableName = name
        return this
    }

    /**
     * Dodaje definicję kolumny w stylu:
     *  "id INTEGER PRIMARY KEY",
     *  "name VARCHAR(255) NOT NULL"
     */
    fun column(name: String, type: String, vararg modifiers: String): CreateTableBuilder {
        val definition = listOf(type, *modifiers).joinToString(" ")
        columns.add("$name $definition")
        return this
    }

    /**
     * Dodaje constraint PRIMARY KEY.
     * Można go użyć do definicji wielokolumnowych kluczy głównych,
     * np. primaryKey("col1", "col2").
     */
    fun primaryKey(vararg columns: String): CreateTableBuilder {
        constraints.add("PRIMARY KEY (${columns.joinToString(", ")})")
        return this
    }

    /**
     * Dodaje constraint FOREIGN KEY (column) REFERENCES reference.
     */
    fun foreignKey(column: String, reference: String): CreateTableBuilder {
        constraints.add("FOREIGN KEY ($column) REFERENCES $reference")
        return this
    }

    /**
     * Na końcu składamy instrukcję CREATE TABLE.
     */
    override fun build(): String = buildString {
        append("CREATE TABLE $tableName (\n")
        append(columns.joinToString(",\n"))
        if (constraints.isNotEmpty()) {
            append(",\n")
            append(constraints.joinToString(",\n"))
        }
        append("\n)")
    }

    /**
     * Mapa typów z `KClass` na konkretny typ SQL (dla prostych przypadków).
     */
    private fun mapKClassToSQLType(kClass: KClass<*>, column: Column<*>): String {
        return when (kClass) {
            Int::class -> "INTEGER"
            String::class -> {
                // jeżeli mamy ustawioną długość > 0, to generujemy VARCHAR, w przeciwnym razie TEXT
                if (column.length > 0) {
                    "VARCHAR(${column.length})"
                } else {
                    "TEXT"
                }
            }
            Boolean::class -> "BOOLEAN"
            BigDecimal::class -> {
                // Używamy precision i scale w definicji np. DECIMAL(10,2)
                "DECIMAL(${column.precision},${column.scale})"
            }
            LocalDate::class -> "DATE"
            else -> "TEXT" // fallback, można rzucić wyjątek albo inaczej obsłużyć
        }
    }

    /**
     * Zbiera listę constraintów dla pojedynczej kolumny (PRIMARY KEY, NOT NULL, UNIQUE).
     */
    private fun buildColumnConstraints(column: Column<*>): List<String> {
        val constraints = mutableListOf<String>()
        if (column.primaryKey) constraints.add("PRIMARY KEY")
        if (!column.nullable) constraints.add("NOT NULL")
        if (column.unique) constraints.add("UNIQUE")
        return constraints
    }
}
