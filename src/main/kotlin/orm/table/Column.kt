package orm.table

import orm.sql.SQLDataType

class Column<T>(
    override val _name: String,
    val sqlType: SQLDataType,
    var autoIncrement: Boolean = false
) : TableComponent {
    internal lateinit var table: Table<*>
    
    fun autoIncrement(): Column<T> = apply {
        autoIncrement = true
    }
}