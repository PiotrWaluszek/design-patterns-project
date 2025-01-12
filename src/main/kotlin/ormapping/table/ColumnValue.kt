package ormapping.table

data class ColumnValue<T>(val column: Column<T>, val value: T)

infix fun <T> Column<T>.eq(value: T): ColumnValue<T> = ColumnValue(this, value)