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
    val entityClass: KClass<T>,
) {
    private val _foreignKeys = mutableListOf<ForeignKey>()
    val foreignKeys: List<ForeignKey>
        get() = _foreignKeys.toList()
    
    private val _relations = mutableListOf<Relation<*>>()
    val relations: List<Relation<*>>
        get() = _relations.toList()
    
    private val _columns = mutableListOf<Column<*>>()
    val columns: List<Column<*>>
        get() = _columns.toList()
    
    
    private val _foreignColumns = mutableListOf<Column<*>>()
    val foreignColumns: List<Column<*>>
        get() = _foreignColumns.toList()
    
    
    private val _primaryKey = mutableListOf<Column<*>>()
    val primaryKey: List<Column<*>>
        get() = _primaryKey.toList()
    
    fun integer(name: String): Column<Int> = Column<Int>(name, Int::class).also {
        _columns.add(it)
        it.table = this
    }
    
    fun varchar(name: String, length: Int): Column<String> = Column<String>(name, String::class, length = length).also {
        _columns.add(it)
        it.table = this
    }
    
    fun text(name: String): Column<String> = Column<String>(name, String::class).also {
        _columns.add(it)
        it.table = this
    }
    
    fun boolean(name: String): Column<Boolean> = Column<Boolean>(name, Boolean::class).also {
        _columns.add(it)
        it.table = this
    }
    
    fun date(name: String): Column<LocalDate> = Column<LocalDate>(name, LocalDate::class).also {
        _columns.add(it)
        it.table = this
    }
    
    fun decimal(name: String, precision: Int, scale: Int): Column<BigDecimal> =
        Column<BigDecimal>(name, BigDecimal::class, precision = precision, scale = scale).also {
            _columns.add(it)
            it.table = this
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
        _foreignColumns.addAll(foreignColumns)
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
        target.primaryKey.map { targetColumn ->
            Column<Any>("${target._name}_${targetColumn.name}", targetColumn.type)
                .also {
                    _columns.add(it)
                    _foreignColumns.add(it)
                }
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
            //potrzebuje joiner table
        )
        _relations.add(relation)
    }
    
    fun toEntity(row: ResultSet): T {
        val constructor = entityClass.primaryConstructor
            ?: throw IllegalStateException("Entity must have a primary constructor")
        
        // Pobieramy prefixy tabel z relacji
        val related_tables_prefixes = this.relations.map { it.targetTable._name }
        
        // Grupujemy parametry konstruktora
        val parameterGroups = constructor.parameters.groupBy { param ->
            when {
                // Ignorujemy kolekcje
                param.type.classifier is KClass<*> &&
                        Collection::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> {
                    "collections" // specjalny klucz dla kolekcji, które chcemy pominąć
                }
                // Jeśli parametr jest Entity...
                param.type.classifier is KClass<*> &&
                        (param.type.classifier as KClass<*>).supertypes.any { it.classifier == Entity::class } -> {
                    // ... reszta kodu bez zmian ...
                }
                // Jeśli nie jest Entity ani kolekcją, to null (grupa parametrów głównej tabeli)
                else -> null
            }
        }
        
        // Tworzymy mapę wartości dla parametrów głównej tabeli
        val mainTableValues = parameterGroups[null]?.associateWith { param ->
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
        } ?: emptyMap()
        
        // Tworzymy mapę wartości dla każdej powiązanej tabeli
        val relatedTablesValues = related_tables_prefixes.flatMap { prefix ->
            val params = parameterGroups[prefix] ?: return@flatMap emptyList()
            
            params.map { param ->
                val relatedEntityClass = param.type.classifier as KClass<*>
                val relatedTable = relations.find {
                    it.targetTable._name == prefix &&
                            it.targetTable.entityClass == relatedEntityClass
                }?.targetTable ?: return@map param to null
                
                // Sprawdzamy czy którakolwiek z kolumn klucza obcego jest NULL
                val hasNullValue = relatedTable.primaryKey.any { pk ->
                    val columnName = "${prefix}_${pk.name}"
                    row.getObject(columnName) == null
                }
                
                if (hasNullValue) {
                    param to null
                } else {
                    val relatedConstructor = relatedEntityClass.primaryConstructor
                        ?: throw IllegalStateException("Related entity must have primary constructor")
                    
                    val relatedParams = relatedConstructor.parameters.associateWith { relatedParam ->
                        val columnName = "${prefix}_${relatedParam.name}"
                        when (relatedParam.type.classifier) {
                            Int::class -> row.getInt(columnName)
                            String::class -> row.getString(columnName)
                            Boolean::class -> row.getBoolean(columnName)
                            LocalDate::class -> row.getDate(columnName)?.toLocalDate()
                            BigDecimal::class -> row.getBigDecimal(columnName)
                            else -> throw IllegalStateException("Unsupported type for related parameter ${relatedParam.type.classifier}")
                        }
                    }
                    
                    param to relatedConstructor.callBy(relatedParams)
                }
            }
        }.toMap()
        
        // Łączymy wszystkie parametry
        val allParameters = mainTableValues + relatedTablesValues
        
        return constructor.callBy(allParameters)
    }
    
    fun fromEntity(entity: T): Map<Column<*>, Any?> {
        val regularColumns = (columns - foreignColumns)
            .associateWith { column ->
                entityClass.memberProperties
                    .find { it.name == column.name }
                    ?.call(entity)
            }
        val foreignKeyColumns = foreignColumns
            .associateWith { column ->
                entityClass.memberProperties
                    .map { property ->
                        when {
                            property.returnType.classifier is KClass<*> &&
                                    (property.returnType.classifier as KClass<*>).supertypes.any { it.classifier == Entity::class } -> {
                                val entityValue = property.call(entity)
                                if (entityValue != null) {
                                    val table = (entityValue as Entity).table
                                    val primaryKeyValues = entityValue::class.memberProperties
                                        .filter { it.name in table.primaryKey.map { pk -> pk.name } }
                                        .associate { prop ->
                                            "${table._name}_${prop.name}" to prop.call(entityValue)
                                        }
                                    primaryKeyValues[column.name]
                                } else {
                                    null
                                }
                            }
                            
                            else -> null
                        }
                    }
                    .find { it != null }
            }
        return regularColumns + foreignKeyColumns
    }
}

