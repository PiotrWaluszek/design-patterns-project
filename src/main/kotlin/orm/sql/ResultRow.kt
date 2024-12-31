package orm.sql

import orm.table.Column

class ResultRow(private val data: Map<String, Any?>) {
    operator fun <T> get(column: Column<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return data[column._name] as T?
    }
}
