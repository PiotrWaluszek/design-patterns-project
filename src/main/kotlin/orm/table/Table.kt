package orm.table

import orm.entity.Entity
import orm.sql.ResultRow
import orm.sql.SQLDataType
import java.math.BigDecimal
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor


abstract class Table<T: Entity>(
    override val _name: String,
    private val entityClass: KClass<T>
) : TableComponent {
    private val _columns = mutableListOf<Column<*>>()
    val columns: List<Column<*>> get() = _columns.toList()
    private var primaryKey: Column<*>? = null
    
    protected fun integer(columnName: String): Column<Int> =
        Column<Int>(columnName, SQLDataType.INTEGER).also {
            it.table = this
            _columns.add(it)
        }
    
    protected fun varchar(columnName: String): Column<String> =
        Column<String>(columnName, SQLDataType.VARCHAR).also {
            it.table = this
            _columns.add(it)
        }
    
    protected fun boolean(columnName: String): Column<Boolean> =
        Column<Boolean>(columnName, SQLDataType.BOOLEAN).also {
            it.table = this
            _columns.add(it)
        }
    
    protected fun decimal(columnName: String): Column<BigDecimal> =
        Column<BigDecimal>(columnName, SQLDataType.DECIMAL).also {
            it.table = this
            _columns.add(it)
        }
    
    protected fun text(columnName: String): Column<String> =
        Column<String>(columnName, SQLDataType.TEXT).also {
            it.table = this
            _columns.add(it)
        }
    
    protected fun <T> Column<T>.primaryKey(): Column<T> = apply {
        primaryKey = this
        autoIncrement()
    }
    
    fun toEntity(row: ResultRow): T {
        val constructor = entityClass.primaryConstructor
            ?: throw IllegalStateException("Entity must have a primary constructor")
        
        val parameters = constructor.parameters.associateWith { param ->
            row[columns.find { it._name == param.name }
                ?: throw IllegalStateException("No column found for parameter ${param.name}")]
        }
        
        return constructor.callBy(parameters)
    }
    
    fun fromEntity(entity: T): Map<Column<*>, Any?> {
        return columns.associateWith { column ->
            entityClass.memberProperties
                .find { it.name == column._name }
                ?.call(entity)
        }
    }
}