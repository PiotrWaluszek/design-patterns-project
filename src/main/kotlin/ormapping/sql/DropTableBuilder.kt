package ormapping.sql

import ormapping.command.CommandExecutor
import ormapping.connection.DatabaseConnection
import ormapping.dialect.SQLDialect
import ormapping.table.Table

/**
 * Builder, który generuje instrukcję:
 *   DROP TABLE [IF EXISTS] tableName [CASCADE]
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
     * Ustawia tabelę na podstawie obiektu `Table<*>`.
     */
    fun fromTable(table: Table<*>): DropTableBuilder {
        this.tableName = table._name
        return this
    }

    /**
     * Ustawia tabelę na podstawie nazwy w postaci String.
     */
    fun from(tableName: String): DropTableBuilder {
        this.tableName = tableName
        return this
    }

    /**
     * Dodaje klauzulę IF EXISTS (jeśli dialekt ją wspiera).
     * Przykład: DROP TABLE IF EXISTS ...
     */
    fun ifExists(): DropTableBuilder {
        this.ifExists = true
        return this
    }

    /**
     * Dodaje klauzulę CASCADE (jeśli dialekt ją wspiera).
     * Przykład: DROP TABLE ... CASCADE
     */
    fun cascade(): DropTableBuilder {
        this.cascade = true
        return this
    }

    /**
     * Buduje finalne zapytanie w stylu:
     *   DROP TABLE [IF EXISTS] tableName [CASCADE]
     */
    override fun build(): String = buildString {
        append("DROP TABLE ")
        if (ifExists) {
            // Dialekt np. PostgreSQL wspiera IF EXISTS,
            // inne bazy mogą to ignorować lub rzucić błąd.
            append("IF EXISTS ")
        }
        append(tableName)
        if (cascade) {
            // W PostgreSQL: "CASCADE";
            // w innych dialektach może to nie działać lub działać inaczej.
            append(" CASCADE")
        }
    }
}
