package ormapping.table

import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * Represents a column in a database table.
 * Stores metadata about the column, such as its name, type, and constraints.
 *
 * @param T The type of data stored in the column.
 * @property name The name of the column.
 * @property type The Kotlin class representing the column's data type.
 * @property primaryKey Whether the column is a primary key.
 * @property nullable Whether the column can store null values.
 * @property unique Whether the column enforces uniqueness of its values.
 * @property length The maximum length of the column (applies to strings).
 * @property scale The scale (number of digits to the right of the decimal point) for decimal types.
 * @property precision The precision (total number of digits) for decimal types.
 */
class Column<T>(
    val name: String,
    val type: KClass<*>,
    var primaryKey: Boolean = false,
    var nullable: Boolean = false,
    var unique: Boolean = false,
    val length: Int = 0,
    val scale: Int = 0,
    val precision: Int = 0,
) {
    /**
     * The table to which this column belongs.
     */
    lateinit var table: Table<*>
}
