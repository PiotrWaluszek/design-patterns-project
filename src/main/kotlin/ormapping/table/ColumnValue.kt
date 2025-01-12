package ormapping.table

/**
 * Represents a value assigned to a specific column in a database table.
 * Used to bind values to columns for SQL operations.
 *
 * @param T The type of the column's data.
 * @property column The column to which the value is assigned.
 * @property value The value to be assigned to the column.
 */
data class ColumnValue<T>(
    val column: Column<T>,
    val value: T
)

/**
 * Creates a `ColumnValue` by binding a value to a column using the `eq` infix function.
 *
 * Example usage:
 * ```kotlin
 * val columnValue = column eq value
 * ```
 *
 * @param T The type of the column's data.
 * @param value The value to bind to the column.
 * @return A new `ColumnValue` instance representing the binding.
 */
infix fun <T> Column<T>.eq(value: T): ColumnValue<T> = ColumnValue(this, value)
