package ormapping.sql

import ormapping.table.Column
import ormapping.table.Table

/**
 * Builder, który generuje instrukcję DELETE FROM ...
 * z opcjonalnym WHERE oraz (w pewnych dialektach) CASCADE.
 */
class DeleteBuilder : SQLBuilder {
    private var tableName: String = ""
    private val conditions = mutableListOf<String>()
    private var cascade = false

    /**
     * Ustawia tabelę (na podstawie obiektu `Table<*>`),
     * z której chcemy usuwać rekordy.
     */
    fun from(table: Table<*>): DeleteBuilder {
        this.tableName = table._name
        return this
    }

    /**
     * Ustawia tabelę (za pomocą nazwy w postaci String),
     * z której chcemy usuwać rekordy.
     */
    fun from(tableName: String): DeleteBuilder {
        this.tableName = tableName
        return this
    }

    /**
     * Dodaje warunek do klauzuli WHERE (jako String).
     * Można dodać wiele warunków, będą łączone klauzulą AND.
     */
    fun where(condition: String): DeleteBuilder {
        conditions.add(condition)
        return this
    }

    /**
     * Przeciążona metoda `where`, pozwala budować
     * proste warunki typu `id = 5` na podstawie kolumny z Table.
     *
     * Przykład użycia:
     *  .where(Employees.id, "=", 1)
     */
    fun where(column: Column<*>, operator: String, value: Any?): DeleteBuilder {
        val condition = buildString {
            append(column.name)
            append(" ")
            append(operator)
            append(" ")
            // Jeżeli wartość jest Stringiem, dodaj cudzysłowy
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
     * Ustawia kasowanie kaskadowe (zależne od dialektu SQL).
     * W standardzie SQL CASCADE pojawia się głównie przy DROP TABLE
     * albo przy usuwaniu rekordów, do których istnieją klucze obce
     * zdefiniowane z CASCADE DELETE. Traktuj opcjonalnie.
     */
    fun cascade(): DeleteBuilder {
        cascade = true
        return this
    }

    /**
     * Składa finalną komendę SQL, np:
     *  DELETE FROM employees WHERE id = 1 [CASCADE]
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
