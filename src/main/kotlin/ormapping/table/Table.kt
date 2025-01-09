package ormapping.table

import ormapping.entity.Entity
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor


abstract class Table<T : Entity>(
    val _name: String,
    private val entityClass: KClass<T>,
) {
    private val _foreignKeys = mutableListOf<ForeignKey>()
    private val _relations = mutableListOf<Relation<*>>()
    private val _columns = mutableListOf<Column<*>>()
    val columns: List<Column<*>>
        get() = _columns.toList()
    private val _primaryKey = mutableListOf<Column<*>>()
    val primaryKey: List<Column<*>>
        get() = _primaryKey.toList()
    
    fun integer(name: String): Column<Int> = Column<Int>(name, Int::class).also {
        _columns.add(it)
    }
    
    fun varchar(name: String, length: Int): Column<String> = Column<String>(name, String::class, length = length).also {
        _columns.add(it)
    }
    
    fun text(name: String): Column<String> = Column<String>(name, String::class).also {
        _columns.add(it)
    }
    
    fun boolean(name: String): Column<Boolean> = Column<Boolean>(name, Boolean::class).also {
        _columns.add(it)
    }
    
    fun date(name: String): Column<LocalDate> = Column<LocalDate>(name, LocalDate::class).also {
        _columns.add(it)
    }
    
    fun decimal(name: String, precision: Int, scale: Int): Column<BigDecimal> =
        Column<BigDecimal>(name, BigDecimal::class, precision = precision, scale = scale).also {
            _columns.add(it)
        }
    
    
    fun <T> Column<T>.primaryKey(): Column<T> {
        _primaryKey.add(this)
        primaryKey = true
        unique = true
        return this
    }
    
    fun <T> Column<T>.nullable(): Column<T> {
        nullable = true
        return this
    }
    
    fun <T> Column<T>.unique(): Column<T> {
        unique = true
        return this
    }
    
    fun <R : Entity> oneToOne(
        target: Table<R>,
        cascade: CascadeType = CascadeType.NONE,
    ) {
        val foreignColumns = target.primaryKey.map { targetColumn ->
            Column<Any>("${target._name}_${targetColumn.name}", targetColumn.type)
                .also { it.unique = true }
                .also { _columns.add(it) }
        }
        
        val foreignKey = ForeignKey(
            targetTable = target._name,
            targetColumns = target.primaryKey.map { it.name },
            cascade = cascade
        )
        _foreignKeys.add(foreignKey)
        
        val relation = Relation(
            type = RelationType.ONE_TO_ONE,
            targetTable = target,
            cascade = cascade
        )
        relation.foreignKey = foreignKey
        _relations.add(relation)
    }
    
    fun <R : Entity> manyToOne(
        target: Table<R>,
        cascade: CascadeType = CascadeType.NONE,
    ) {
        val foreignColumns = target.primaryKey.map { targetColumn ->
            Column<Any>("${target._name}_${targetColumn.name}", targetColumn.type)
                .also { _columns.add(it) }
        }
        
        val foreignKey = ForeignKey(
            targetTable = target._name,
            targetColumns = target.primaryKey.map { it.name },
            cascade = cascade
        )
        _foreignKeys.add(foreignKey)
        
        val relation = Relation(
            type = RelationType.MANY_TO_ONE,
            targetTable = target,
            cascade = cascade
        )
        relation.foreignKey = foreignKey
        _relations.add(relation)
    }
    
    fun <R : Entity> oneToMany(
        target: Table<R>,
        cascade: CascadeType = CascadeType.NONE,
    ) {
        val relation = Relation(
            type = RelationType.ONE_TO_MANY,
            targetTable = target,
            cascade = cascade
        )
        _relations.add(relation)
    }
    
    fun <R : Entity> manyToMany(
        target: Table<R>,
        cascade: CascadeType = CascadeType.NONE,
    ) {
        val relation = Relation(
            type = RelationType.MANY_TO_MANY,
            targetTable = target,
            cascade = cascade
        )
        _relations.add(relation)
    }
    
    fun toEntity(row: ResultSet): T {
        val constructor = entityClass.primaryConstructor
            ?: throw IllegalStateException("Entity must have a primary constructor")
        
        val parameters = constructor.parameters.associateWith { param ->
            val column = columns.find { it.name == param.name }
                ?: throw IllegalStateException("No column found for parameter ${param.name}")
            
            when (column.type) {
                Int::class -> row.getInt(column.name)
                String::class -> row.getString(column.name)
                Boolean::class -> row.getBoolean(column.name)
                LocalDate::class -> row.getDate(column.name)?.toLocalDate()
                BigDecimal::class -> row.getBigDecimal(column.name)
                else -> throw IllegalStateException("Unsupported type ${column.type} for column ${column.name}")
            }
        }
        
        return constructor.callBy(parameters)
    }
    
    fun fromEntity(entity: T): Map<Column<*>, Any?> {
        return columns.associateWith { column ->
            entityClass.memberProperties
                .find { it.name == column.name }
                ?.call(entity)
        }
    }
}

class Relation<R : Entity>(
    val type: RelationType,
    val targetTable: Table<R>,
    val cascade: CascadeType = CascadeType.NONE,
    val joinTableName: String? = null,
) {
    lateinit var foreignKey: ForeignKey
}

enum class RelationType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
}