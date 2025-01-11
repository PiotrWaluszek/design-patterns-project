// Zmodyfikowany DropTableBuilder
package ormapping.sql
import ormapping.table.*
import ormapping.command.*

import ormapping.dialect.SQLDialect
import ormapping.dialect.SQLiteDialect

class DropTableBuilder(
    private val dialect: SQLDialect,
    private val table: Table<*>,
    private val executor: CommandExecutor
) : SQLBuilder {
    private var cascade = false

    fun cascade(): DropTableBuilder {
        cascade = true
        return this
    }

    override fun build(): String {
        return buildString {
            append("DROP TABLE ")
            append(table._name)
            if (cascade) append(" CASCADE")
        }
    }

    fun execute() {
        if (cascade && dialect is SQLiteDialect) {
            cascadeDelete()
        }
        executor.executeSQL(this)
    }

    private fun cascadeDelete() {
        for (relation in table.relations) {
            when (relation.type) {
                RelationType.ONE_TO_MANY, RelationType.MANY_TO_MANY -> {
                    val targetTable = relation.targetTable
                    println("Cascading delete for table: ${targetTable._name}")
                    executor.delete(targetTable, "1=1") // Usuwamy wszystkie rekordy
                }
                else -> {
                    // Inne typy relacji w zależności od potrzeb
                }
            }
        }
    }
}
