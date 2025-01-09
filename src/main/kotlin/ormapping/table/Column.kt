package ormapping.table

import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass

class Column<T>(
    val name: String,
    val type: KClass<*>,
    var primaryKey: Boolean = false,
    var nullable: Boolean = false,
    var unique: Boolean = false,
    private val length: Int = 0,
    private val scale: Int = 0,
    private val precision: Int = 0,
)

