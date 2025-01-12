package ormapping.table

import ormapping.entity.Entity
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Abstract base class for defining database tables with ORM functionality.
 * Provides mapping between database tables and Kotlin entities.
 *
 * @param T The entity type that this table represents
 * @param _name The name of the database table
 * @param entityClass The Kotlin class of the entity
 */
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

    /**
     * Creates an integer column in the table.
     * @param name The name of the column
     * @return A Column instance representing an integer column
     */
    fun integer(name: String): Column<Int> = Column<Int>(name, Int::class).also {
        _columns.add(it)
        it.table = this
    }

    /**
     * Creates a varchar column in the table.
     * @param name The name of the column
     * @param length The maximum length of the varchar
     * @return A Column instance representing a varchar column
     */
    fun varchar(name: String, length: Int): Column<String> = Column<String>(name, String::class, length = length).also {
        _columns.add(it)
        it.table = this
    }

    /**
     * Creates a text column in the table.
     * @param name The name of the column
     * @return A Column instance representing a text column
     */
    fun text(name: String): Column<String> = Column<String>(name, String::class).also {
        _columns.add(it)
        it.table = this
    }

    /**
     * Creates a boolean column in the table.
     * @param name The name of the column
     * @return A Column instance representing a boolean column
     */
    fun boolean(name: String): Column<Boolean> = Column<Boolean>(name, Boolean::class).also {
        _columns.add(it)
        it.table = this
    }

    /**
     * Creates a date column in the table.
     * @param name The name of the column
     * @return A Column instance representing a date column
     */
    fun date(name: String): Column<LocalDate> = Column<LocalDate>(name, LocalDate::class).also {
        _columns.add(it)
        it.table = this
    }

    /**
     * Creates a decimal column in the table.
     * @param name The name of the column
     * @param precision The total number of digits
     * @param scale The number of digits after the decimal point
     * @return A Column instance representing a decimal column
     */
    fun decimal(name: String, precision: Int, scale: Int): Column<BigDecimal> =
        Column<BigDecimal>(name, BigDecimal::class, precision = precision, scale = scale).also {
            _columns.add(it)
            it.table = this
        }

    /**
     * Marks a column as the primary key.
     * @return The modified column with primary key constraints
     */
    fun <T> Column<T>.primaryKey(): Column<T> {
        _primaryKey.add(this)
        primaryKey = true
        unique = true
        return this
    }

    /**
     * Marks a column as nullable.
     * @return The modified column allowing null values
     */
    fun <T> Column<T>.nullable(): Column<T> {
        nullable = true
        return this
    }

    /**
     * Marks a column as unique.
     * @return The modified column with unique constraint
     */
    fun <T> Column<T>.unique(): Column<T> {
        unique = true
        return this
    }

    /**
     * Establishes a one-to-one relationship with another table.
     * @param target The target table to establish the relationship with
     * @param cascade The cascade type for the relationship
     */
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

    /**
     * Establishes a many-to-one relationship with another table.
     * @param target The target table to establish the relationship with
     * @param cascade The cascade type for the relationship
     */
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

    /**
     * Establishes a one-to-many relationship with another table.
     * @param target The target table to establish the relationship with
     * @param cascade The cascade type for the relationship
     */
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

    /**
     * Establishes a many-to-many relationship with another table.
     * @param target The target table to establish the relationship with
     * @param cascade The cascade type for the relationship
     */
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

    /**
     * Converts a database result set row to an entity instance.
     * @param row The ResultSet containing the data
     * @return An instance of the entity populated with data from the result set
     * @throws IllegalStateException if the entity structure is invalid
     */
    fun toEntity(row: ResultSet): T {
        val constructor = entityClass.primaryConstructor
            ?: throw IllegalStateException("Entity must have a primary constructor")

        val related_tables_prefixes = this.relations.map { it.targetTable._name }

        val parameterGroups = constructor.parameters.groupBy { param ->
            when {
                param.type.classifier is KClass<*> &&
                        Collection::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> {
                    "collections"
                }
                param.type.classifier is KClass<*> &&
                        (param.type.classifier as KClass<*>).supertypes.any { it.classifier == Entity::class } -> {
                    related_tables_prefixes.find { prefix ->
                        relations.any {
                            it.targetTable._name == prefix &&
                                    it.targetTable.entityClass == param.type.classifier
                        }
                    }
                }
                else -> null
            }
        }

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

        val relatedTablesValues = related_tables_prefixes.flatMap { prefix ->
            val params = parameterGroups[prefix] ?: return@flatMap emptyList()

            params.map { param ->
                val relatedEntityClass = param.type.classifier as KClass<*>
                val relatedTable = relations.find {
                    it.targetTable._name == prefix &&
                            it.targetTable.entityClass == relatedEntityClass
                }?.targetTable ?: return@map param to null

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

        val allParameters = mainTableValues + relatedTablesValues

        return constructor.callBy(allParameters)
    }

    /**
     * Converts an entity instance to a map of column-value pairs.
     * @param entity The entity instance to convert
     * @return A map of columns to their corresponding values from the entity
     */
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
