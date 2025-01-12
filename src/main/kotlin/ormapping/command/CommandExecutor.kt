package ormapping.command

import ormapping.connection.DatabaseConnection
import ormapping.entity.Entity
import ormapping.sql.CreateTableBuilder
import ormapping.sql.DeleteBuilder
import ormapping.sql.DropTableBuilder
import ormapping.sql.SQLBuilder
import ormapping.sql.SelectBuilder
import ormapping.table.CascadeType
import ormapping.table.Relation
import ormapping.table.RelationType
import ormapping.table.Table
import ormapping.table.ColumnValue
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Executes database commands and manages database operations with logging capabilities.
 *
 * @property connection The database connection to use for executing commands
 * @property logger The logger instance for recording operations
 * @property logDest The destination for log entries
 */
class CommandExecutor(
    private val connection: DatabaseConnection,
    private val logger: MultiDestinationLogger,
    private val logDest: String,
) {

    /**
     * Creates a new SELECT query builder.
     * @return A new SelectBuilder instance
     */
    fun createSelect(): SelectBuilder = SelectBuilder()

    /**
     * Creates a new DELETE query builder.
     * @return A new DeleteBuilder instance
     */
    fun createDelete(): DeleteBuilder = DeleteBuilder()

    /**
     * Creates a new CREATE TABLE query builder.
     * @return A new CreateTableBuilder instance
     */
    fun createTable(): CreateTableBuilder = CreateTableBuilder()

    /**
     * Creates a DROP TABLE query builder for the specified table.
     * @param table The table to be dropped
     * @return A new DropTableBuilder instance
     */
    fun dropTable(table: Table<*>): DropTableBuilder {
        return DropTableBuilder(connection.getDialect(), table, this)
    }

    /**
     * Executes an SQL query built by the provided builder.
     * @param builder The SQL builder containing the query to execute
     * @return The appropriate SQLCommand instance based on the builder type
     * @throws IllegalArgumentException if the builder type is unknown
     */
    fun executeSQL(builder: SQLBuilder): SQLCommand {
        val sql = builder.build()
        logger.log(logDest, "Executing SQL:")
        logger.log(logDest, sql)
        return when (builder) {
            is SelectBuilder -> SelectCommand(sql)
            is DeleteBuilder -> DeleteCommand(sql)
            is CreateTableBuilder -> CreateTableCommand(sql)
            is DropTableBuilder -> DropTableCommand(sql)
            else -> throw IllegalArgumentException("Unknown builder type")
        }.also { it.execute(connection) }
    }

    /**
     * Finds an entity by its primary key values.
     *
     * @param table The table to search in
     * @param values The primary key column values to search for
     * @return The found entity or null if not found
     * @throws IllegalArgumentException if the provided columns don't match the table's primary keys
     * @throws IllegalStateException if the entity structure is invalid or contains unsupported types
     */
    fun <T : Entity> find(table: Table<T>, vararg values: ColumnValue<*>): T? {
        val primaryKeyColumns = table.primaryKey.toSet()
        val providedPrimaryKeys = values.map { it.column }.toSet()

        if (primaryKeyColumns != providedPrimaryKeys) {
            throw IllegalArgumentException(
                "Provided columns don't match table's primary keys exactly.\n" +
                        "Expected: ${primaryKeyColumns.map { it.name }}\n" +
                        "Got: ${providedPrimaryKeys.map { it.name }}"
            )
        }

        val sql = buildString {
            append("SELECT ")
            append(table.columns.joinToString(", ") { it.name })
            append(" FROM ")
            append(table._name)
            append(" WHERE ")
            append(values.joinToString(" AND ") { "${it.column.name} = ?" })
        }
        logger.log(logDest, "Executing Find")
        logger.log(logDest, sql)
        return connection.getConnection().prepareStatement(sql).use { statement ->
            values.forEachIndexed { index, columnValue ->
                setParameter(statement, index + 1, columnValue.value, columnValue.column.type)
            }

            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val constructor = table.entityClass.primaryConstructor
                    ?: throw IllegalStateException("Entity must have a primary constructor")

                val parameters = constructor.parameters.associateWith { param ->
                    when {
                        param.type.classifier is KClass<*> &&
                                Collection::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> {
                            when {
                                Set::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> mutableSetOf<Any>()
                                List::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> mutableListOf<Any>()
                                else -> throw IllegalStateException("Unsupported collection type ${param.type}")
                            }
                        }
                        else -> {
                            val column = table.columns.find { it.name == param.name }
                            if (column != null) {
                                when (column.type) {
                                    Int::class -> resultSet.getInt(column.name)
                                    String::class -> resultSet.getString(column.name)
                                    Boolean::class -> resultSet.getBoolean(column.name)
                                    LocalDate::class -> resultSet.getDate(column.name)?.toLocalDate()
                                    BigDecimal::class -> resultSet.getBigDecimal(column.name)
                                    else -> throw IllegalStateException("Unsupported type ${column.type} for column ${column.name}")
                                }
                            } else {
                                if (param.isOptional) null
                                else throw IllegalStateException("No column found for required parameter ${param.name}")
                            }
                        }
                    }
                }

                val entity = constructor.callBy(parameters)
                loadRelations(table, entity)
                entity
            } else {
                null
            }
        }
    }

    /**
     * Persists one or more entities to the database.
     *
     * @param table The table to persist the entities to
     * @param entities The entities to persist
     */
    fun <T : Entity> persist(
        table: Table<T>,
        vararg entities: T,
    ) {
        if (entities.isEmpty()) return

        val sql = buildString {
            append("INSERT INTO ")
            append(table._name)
            append(" (")
            append(table.columns.joinToString(", ") { it.name })
            append(") VALUES (")
            append(table.columns.joinToString(", ") { "?" })
            append(")")
        }
        logger.log(logDest, "Executing Persist")
        logger.log(logDest, sql)
        connection.getConnection().prepareStatement(sql).use { statement ->
            entities.forEach { entity ->
                val values = table.fromEntity(entity)
                table.columns.forEachIndexed { index, column ->
                    setParameter(statement, index + 1, values[column], column.type)
                }
                statement.addBatch()
                saveRelations(table, entity)
            }
            statement.executeBatch()
        }
    }

    /**
     * Deletes an entity from the database based on primary key values.
     *
     * @param table The table to delete from
     * @param values The primary key values identifying the record to delete
     * @return true if the record was deleted, false otherwise
     * @throws IllegalArgumentException if the provided columns don't match the table's primary keys
     */
    fun <T : Entity> delete(table: Table<T>, vararg values: ColumnValue<*>): Boolean {
        val primaryKeyColumns = table.primaryKey.toSet()
        val providedPrimaryKeys = values.map { it.column }.toSet()

        if (primaryKeyColumns != providedPrimaryKeys) {
            throw IllegalArgumentException(
                "Provided columns don't match table's primary keys exactly.\n" +
                        "Expected: ${primaryKeyColumns.map { it.name }}\n" +
                        "Got: ${providedPrimaryKeys.map { it.name }}"
            )
        }

        val sql = buildString {
            append("DELETE FROM ")
            append(table._name)
            append(" WHERE ")
            append(values.joinToString(" AND ") { "${it.column.name} = ?" })
        }

        cascadeDelete(table, values)
        orphan(table, values)

        logger.log(logDest, "Executing Delete")
        logger.log(logDest, sql)
        return connection.getConnection().prepareStatement(sql).use { statement ->
            values.forEachIndexed { index, columnValue ->
                setParameter(statement, index + 1, columnValue.value, columnValue.column.type)
            }
            statement.executeUpdate() > 0
        }
    }

    /**
     * Deletes orphaned records from a table based on foreign key relationships.
     * An orphaned record is one where the foreign key reference is NULL.
     *
     * @param table The table containing potential orphaned records
     * @param relatedTable The table that should be referenced by the foreign key
     * @return The number of deleted orphaned records
     * @throws IllegalArgumentException if no foreign key relationship exists between the tables
     */
    fun <T : Entity, R : Entity> slayOrphans(table: Table<T>, relatedTable: Table<R>): Int {
        val relevantForeignKey = table.foreignKeys.find { fk ->
            fk.targetTable == relatedTable._name
        } ?: throw IllegalArgumentException("No foreign key found from ${table._name} to ${relatedTable._name}")
        
        val orphanedRecords = buildString {
            append("DELETE FROM ${table._name} WHERE ")
            val foreignKeyColumns = table.columns.filter { column ->
                relevantForeignKey.targetColumns.any { targetCol ->
                    column.name == "${relatedTable._name}_$targetCol"
                }
            }
            append(foreignKeyColumns.joinToString(" AND ") {
                "${it.name} IS NULL"
            })
        }
        
        logger.log(logDest, "Executing slayOrphans")
        logger.log(logDest, orphanedRecords)
        return connection.getConnection().prepareStatement(orphanedRecords).use { statement ->
            statement.executeUpdate()
        }
    }

    /**
     * Finds all orphaned records in a table based on foreign key relationships.
     * An orphaned record is one where the foreign key reference is NULL.
     *
     * @param table The table to search for orphaned records
     * @param relatedTable The table that should be referenced by the foreign key
     * @return A list of orphaned entities
     * @throws IllegalArgumentException if no foreign key relationship exists between the tables
     */
    fun <T : Entity, R : Entity> findOrphans(table: Table<T>, relatedTable: Table<R>): List<T> {
        val relevantForeignKey = table.foreignKeys.find { fk ->
            fk.targetTable == relatedTable._name
        } ?: throw IllegalArgumentException("No foreign key found from ${table._name} to ${relatedTable._name}")
        
        val orphanedRecords = buildString {
            append("SELECT * FROM ${table._name} WHERE ")
            val foreignKeyColumns = table.columns.filter { column ->
                relevantForeignKey.targetColumns.any { targetCol ->
                    column.name == "${relatedTable._name}_$targetCol"
                }
            }
            append(foreignKeyColumns.joinToString(" AND ") {
                "${it.name} IS NULL"
            })
        }
        logger.log(logDest, "Executing findOrphans")
        logger.log(logDest, orphanedRecords)
        return connection.getConnection().prepareStatement(orphanedRecords).use { statement ->
            val results = statement.executeQuery()
            val orphans = mutableListOf<T>()
            while (results.next()) {
                orphans.add(table.toEntity(results))
            }
            orphans
        }
    }

    /**
     * Handles orphaned records during delete operations based on cascade settings.
     * For ONE_TO_ONE and ONE_TO_MANY relationships, sets foreign key references to NULL.
     * For MANY_TO_MANY relationships, deletes the corresponding join table records.
     *
     * @param table The table containing the record being deleted
     * @param values The primary key values of the record being deleted
     */
    private fun <T : Entity> orphan(table: Table<T>, values: Array<out ColumnValue<*>>) {
        table.relations.filter { it.cascade == CascadeType.NONE || it.cascade == CascadeType.UPDATE }
            .forEach { relation ->
                val sql = when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.ONE_TO_MANY -> {
                        buildString {
                            append("UPDATE ${relation.targetTable._name} SET ")
                            append(values.joinToString(", ") {
                                "${table._name}_${it.column.name} = NULL"
                            })
                            append(" WHERE ")
                            append(values.joinToString(" AND ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_MANY -> {
                        val joinTableName = relation.joinTableName
                            ?: "${table._name}_${relation.targetTable._name}"
                        buildString {
                            append("DELETE FROM $joinTableName WHERE ")
                            append(values.joinToString(" AND ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_ONE -> {
                        ""
                    }
                }
                logger.log(logDest, "Executing orphan")
                logger.log(logDest, sql)
                if (sql.isNotEmpty()) {
                    connection.getConnection().prepareStatement(sql).use { statement ->
                        values.forEachIndexed { index, columnValue ->
                            setParameter(statement, index + 1, columnValue.value, columnValue.column.type)
                        }
                        statement.executeUpdate()
                    }
                }
            }
    }

    /**
     * Updates an entity in the database with new values while maintaining referential integrity.
     * Handles cascade updates and orphaned records if primary keys are modified.
     *
     * @param table The table containing the entity to update
     * @param entity The entity to update
     * @param newValues The new values to set for specific columns
     * @return The updated entity instance
     * @throws IllegalArgumentException if the table has no primary keys defined
     * @throws IllegalStateException if the entity structure is invalid
     */
    fun <T : Entity> update(table: Table<T>, entity: T, vararg newValues: ColumnValue<*>): T {
        val columnValues = table.fromEntity(entity)
        val primaryKeyColumns = table.primaryKey
        if (primaryKeyColumns.isEmpty()) {
            throw IllegalArgumentException("Table must have primary keys defined")
        }

        val updatedPrimaryKeys = newValues.filter { newValue ->
            primaryKeyColumns.contains(newValue.column)
        }

        val sql = buildString {
            append("UPDATE ")
            append(table._name)
            append(" SET ")
            append(newValues.joinToString(", ") { "${it.column.name} = ?" })
            append(" WHERE ")
            append(primaryKeyColumns.joinToString(" AND ") { "${it.name} = ?" })
        }
        logger.log(logDest, "Executing Update")
        logger.log(logDest, sql)
        val updateSuccessful = connection.getConnection().prepareStatement(sql).use { statement ->
            var parameterIndex = 1

            newValues.forEach { columnValue ->
                setParameter(statement, parameterIndex++, columnValue.value, columnValue.column.type)
            }

            primaryKeyColumns.forEach { column ->
                setParameter(statement, parameterIndex++, columnValues[column], column.type)
            }
            
            statement.executeUpdate() > 0
        }

        if (updateSuccessful && updatedPrimaryKeys.isNotEmpty()) {

            table.relations
                .filter { it.cascade == CascadeType.ALL || it.cascade == CascadeType.UPDATE }
                .forEach { relation ->
                    cascadeUpdate(
                        table,
                        entity,
                        *updatedPrimaryKeys.toTypedArray()
                    )
                }

            table.relations
                .filter { it.cascade == CascadeType.NONE || it.cascade == CascadeType.DELETE }
                .forEach { relation ->
                    orphanUpdate(
                        table,
                        entity
                    )
                }
        }
        
        val constructor = table.entityClass.primaryConstructor
            ?: throw IllegalStateException("Entity must have a primary constructor")
        
        val originalValues = table.fromEntity(entity)

        val updatedValues = originalValues.toMutableMap()
        newValues.forEach { newValue ->
            updatedValues[newValue.column] = newValue.value
        }
        
        val parameters = constructor.parameters.associateWith { param ->
            when {
                param.type.classifier is KClass<*> &&
                        Collection::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> {
                    null
                }
                else -> {
                    val column = table.columns.find { it.name == param.name }
                    if (column != null) {
                        updatedValues[column]
                    } else {
                        if (param.isOptional) null else throw IllegalStateException("No column found for required parameter ${param.name}")
                    }
                }
            }
        }
        
        return constructor.callBy(parameters)
    }

    /**
     * Performs cascade updates on related tables when primary keys are modified.
     * Updates foreign key references in related tables based on cascade settings.
     *
     * @param table The table containing the updated entity
     * @param entity The entity being updated
     * @param updatedKeys The primary key columns that have been modified
     */
    private fun <T : Entity> cascadeUpdate(table: Table<T>, entity: T, vararg updatedKeys: ColumnValue<*>) {
        table.relations
            .filter { it.cascade == CascadeType.ALL || it.cascade == CascadeType.UPDATE }
            .forEach { relation ->
                val sql = when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.ONE_TO_MANY -> {
                        buildString {
                            append("UPDATE ${relation.targetTable._name} SET ")
                            append(updatedKeys.joinToString(", ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                            append(" WHERE ")
                            append(updatedKeys.joinToString(" AND ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_MANY -> {
                        val joinTableName = relation.joinTableName
                            ?: "${table._name}_${relation.targetTable._name}"
                        buildString {
                            append("UPDATE $joinTableName SET ")
                            append(updatedKeys.joinToString(", ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                            append(" WHERE ")
                            append(updatedKeys.joinToString(" AND ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_ONE -> ""
                }
                logger.log(logDest, "Executing Cascade Update")
                logger.log(logDest, sql)
                if (sql.isNotEmpty()) {
                    connection.getConnection().prepareStatement(sql).use { statement ->
                        var paramIndex = 1
                        updatedKeys.forEach {
                            setParameter(statement, paramIndex++, it.value, it.column.type)
                        }
                        updatedKeys.forEach {
                            val oldValue = table.fromEntity(entity)[it.column]
                            setParameter(statement, paramIndex++, oldValue, it.column.type)
                        }
                        statement.executeUpdate()
                    }
                }
            }
    }

    /**
     * Handles orphaned records during update operations for non-cascading relationships.
     * Sets foreign key references to NULL or removes join table entries.
     *
     * @param table The table containing the updated entity
     * @param entity The entity being updated
     * @throws IllegalArgumentException if required foreign key relationships are not found
     */
    private fun <T : Entity> orphanUpdate(table: Table<T>, entity: T) {
        table.relations.filter { it.cascade == CascadeType.NONE || it.cascade == CascadeType.DELETE }
            .forEach { relation ->
                val relevantForeignKey = relation.targetTable.foreignKeys.find { fk ->
                    fk.targetTable == table._name
                }
                    ?: throw IllegalArgumentException("No foreign key found from ${relation.targetTable._name} to ${table._name}")

                val foreignKeyColumns = relation.targetTable.columns.filter { column ->
                    relevantForeignKey.targetColumns.any { targetCol ->
                        column.name == "${table._name}_$targetCol"
                    }
                }
                
                val sql = when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.ONE_TO_MANY -> {
                        buildString {
                            append("UPDATE ${relation.targetTable._name} SET ")
                            append(foreignKeyColumns.joinToString(", ") {
                                "${it.name} = NULL"
                            })
                            append(" WHERE ")
                            append(foreignKeyColumns.joinToString(" AND ") {
                                "${it.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_MANY -> {
                        val joinTableName = relation.joinTableName
                            ?: "${table._name}_${relation.targetTable._name}"
                        buildString {
                            append("DELETE FROM $joinTableName WHERE ")
                            append(foreignKeyColumns.joinToString(" AND ") {
                                "${it.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_ONE -> {
                        ""
                    }
                }
                logger.log(logDest, "Executing Orphan Update")
                logger.log(logDest, sql)
                if (sql.isNotEmpty()) {
                    connection.getConnection().prepareStatement(sql).use { statement ->
                        val entityValues = table.fromEntity(entity)
                        foreignKeyColumns.forEachIndexed { index, column ->
                            val primaryKeyColumn = table.primaryKey.find {
                                column.name == "${table._name}_${it.name}"
                            } ?: throw IllegalStateException("No matching primary key column found for ${column.name}")
                            
                            setParameter(
                                statement,
                                index + 1,
                                entityValues[primaryKeyColumn],
                                primaryKeyColumn.type
                            )
                        }
                        statement.executeUpdate()
                    }
                }
            }
    }

    /**
     * Persists a collection of entities to the database.
     *
     * @param table The table to persist the entities to
     * @param entities The collection of entities to persist
     */
    inline fun <reified T : Entity> persistAll(
        table: Table<T>, entities: Collection<T>,
    ) {
        persist(table, *entities.toTypedArray())
    }

    /**
     * Sets a parameter value in a PreparedStatement with appropriate type conversion.
     *
     * @param statement The PreparedStatement to set the parameter in
     * @param index The parameter index
     * @param value The value to set
     * @param type The Kotlin type of the parameter
     * @throws IllegalArgumentException if the type is not supported
     */
    private fun setParameter(statement: java.sql.PreparedStatement, index: Int, value: Any?, type: KClass<*>) {
        when (type) {
            Int::class -> statement.setInt(index, value as Int)
            String::class -> statement.setString(index, value as String)
            Boolean::class -> statement.setBoolean(index, value as Boolean)
            LocalDate::class -> statement.setDate(index, if (value != null) Date.valueOf(value as LocalDate) else null)
            BigDecimal::class -> statement.setBigDecimal(index, value as BigDecimal)
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
    }

    /**
     * Loads related entities for all defined relationships of an entity.
     * Handles different relationship types (ONE_TO_ONE, ONE_TO_MANY, etc.).
     *
     * @param table The table containing the entity
     * @param entity The entity to load relations for
     */
    private fun <T : Entity> loadRelations(table: Table<T>, entity: T) {
        table.relations.forEach { relation ->
            val sql = when (relation.type) {
                RelationType.ONE_TO_ONE -> buildOneToOneQuery(table, relation)
                RelationType.ONE_TO_MANY -> buildOneToManyQuery(table, relation)
                RelationType.MANY_TO_ONE -> buildManyToOneQuery(table, relation)
                RelationType.MANY_TO_MANY -> buildManyToManyQuery(table, relation)
            }
            logger.log(logDest, "Executing Loading Related Relations")
            logger.log(logDest, sql)
            val columnValues = table.fromEntity(entity)
            
            connection.getConnection().prepareStatement(sql).use { statement ->
                table.primaryKey.forEachIndexed { index, pkColumn ->
                    val value = columnValues[pkColumn]
                        ?: throw IllegalStateException("No value found for primary key column ${pkColumn.name}")
                    setParameter(statement, index + 1, value, pkColumn.type)
                }
                
                val resultSet = statement.executeQuery()
                val relatedEntities = mutableListOf<Entity>()
                
                while (resultSet.next()) {
                    @Suppress("UNCHECKED_CAST")
                    val relatedEntity = relation.targetTable.toEntity(resultSet)
                    relatedEntities.add(relatedEntity)
                }
                
                
                
                when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.MANY_TO_ONE -> {
                        val property = entity::class.memberProperties.find {
                            it.returnType.classifier == relation.targetTable.entityClass
                        }
                        if (relatedEntities.isNotEmpty()) {
                            property?.let { prop ->
                                val currentValue = prop.call(entity) as? Entity
                                if (currentValue == null) {

                                    (prop as? KMutableProperty<*>)?.setter?.call(entity, relatedEntities.first())
                                }
                            }
                        }
                    }
                    
                    RelationType.ONE_TO_MANY, RelationType.MANY_TO_MANY -> {
                        val property = entity::class.memberProperties.find {
                            it.name == relation.targetTable._name.lowercase()
                        }
                        @Suppress("UNCHECKED_CAST")
                        (property?.call(entity) as? MutableSet<Entity>)?.apply {
                            clear()
                            addAll(relatedEntities)
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds a SQL query for ONE_TO_ONE relationships.
     *
     * @param table The source table
     * @param relation The relationship definition
     * @return The SQL query string
     */
    private fun <T : Entity> buildOneToOneQuery(table: Table<T>, relation: Relation<*>): String {
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN ${relation.targetTable._name} t2 ON ")
            append(table.primaryKey.joinToString(" AND ") { pk ->
                "t2.${table._name}_${pk.name} = t1.${pk.name}"
            })
            append(" WHERE ")
            append(table.primaryKey.joinToString(" AND ") {
                "t1.${it.name} = ?"
            })
        }
    }

    /**
     * Builds a SQL query for ONE_TO_MANY relationships.
     *
     * @param table The source table
     * @param relation The relationship definition
     * @return The SQL query string
     */
    private fun <T : Entity> buildOneToManyQuery(table: Table<T>, relation: Relation<*>): String {
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN ${relation.targetTable._name} t2 ON ")
            append(table.primaryKey.joinToString(" AND ") { pk ->
                "t2.${table._name}_${pk.name} = t1.${pk.name}"
            })
            append(" WHERE ")
            append(table.primaryKey.joinToString(" AND ") {
                "t1.${it.name} = ?"
            })
        }
    }

    /**
     * Builds a SQL query for MANY_TO_ONE relationships.
     *
     * @param table The source table
     * @param relation The relationship definition
     * @return The SQL query string
     */
    private fun <T : Entity> buildManyToOneQuery(table: Table<T>, relation: Relation<*>): String {
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN ${relation.targetTable._name} t2 ON ")
            append(relation.targetTable.primaryKey.joinToString(" AND ") { pk ->
                "t1.${relation.targetTable._name}_${pk.name} = t2.${pk.name}"
            })
            append(" WHERE ")
            append(table.primaryKey.joinToString(" AND ") {
                "t1.${it.name} = ?"
            })
        }
    }

    /**
     * Builds a SQL query for MANY_TO_MANY relationships using a join table.
     *
     * @param table The source table
     * @param relation The relationship definition
     * @return The SQL query string
     */
    private fun <T : Entity> buildManyToManyQuery(table: Table<T>, relation: Relation<*>): String {
        val joinTableName = relation.joinTableName ?: "${table._name}_${relation.targetTable._name}"
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN $joinTableName j ON ")
            append(table.primaryKey.joinToString(" AND ") { pk ->
                "t1.${pk.name} = j.${table._name}_${pk.name}"
            })
            append(" JOIN ${relation.targetTable._name} t2 ON ")
            append(relation.targetTable.primaryKey.joinToString(" AND ") { pk ->
                "j.${relation.targetTable._name}_${pk.name} = t2.${pk.name}"
            })
            append(" WHERE ")
            append(table.primaryKey.joinToString(" AND ") {
                "t1.${it.name} = ?"
            })
        }
    }

    /**
     * Performs cascade deletes on related tables based on cascade settings.
     * Handles different relationship types and maintains referential integrity.
     *
     * @param table The table containing the deleted entity
     * @param values The primary key values of the deleted entity
     */
    private fun <T : Entity> saveRelations(table: Table<T>, entity: T) {
        table.relations.forEach { relation ->
            val property = entity::class.memberProperties
                .find { it.name == relation.targetTable._name.lowercase() }
                ?.call(entity) ?: return@forEach
            
            when (property) {
                is Entity -> {
                    @Suppress("UNCHECKED_CAST")
                    persist(relation.targetTable as Table<Entity>, property)

                    val backProperty = property::class.memberProperties
                        .find { it.name == table._name.lowercase() }
                    if (backProperty != null) {
                        val setter = property::class.members
                            .find { it.name == "${backProperty.name}_setter" }
                        setter?.call(property, entity)
                    }
                }
                is Collection<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    property.forEach { relatedEntity ->
                        if (relatedEntity is Entity) {
                            persist(relation.targetTable as Table<Entity>, relatedEntity)

                            val backProperty = relatedEntity::class.memberProperties
                                .find { it.name == table._name.lowercase() }
                            if (backProperty != null) {
                                val setter = relatedEntity::class.members
                                    .find { it.name == "${backProperty.name}_setter" }
                                setter?.call(relatedEntity, entity)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads related entities based on primary key values.
     *
     * @param table The table to load entities from
     * @param primaryKeyValues Map of primary key column names to their values
     * @return List of loaded entities
     * @throws IllegalStateException if a referenced column is not found
     */
    private fun <T : Entity> cascadeDelete(table: Table<T>, values: Array<out ColumnValue<*>>) {
        table.relations.filter { it.cascade == CascadeType.DELETE || it.cascade == CascadeType.ALL }
            .forEach { relation ->
                val sql = when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.ONE_TO_MANY -> {
                        buildString {
                            append("DELETE FROM ${relation.targetTable._name} WHERE ")
                            append(values.joinToString(" AND ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_MANY -> {
                        val joinTableName = relation.joinTableName
                            ?: "${table._name}_${relation.targetTable._name}"
                        buildString {
                            append("DELETE FROM $joinTableName WHERE ")
                            append(values.joinToString(" AND ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                        }
                    }
                    
                    RelationType.MANY_TO_ONE -> {
                        ""
                    }
                }
                logger.log(logDest, "Executing Cascade Delete")
                logger.log(logDest, sql)
                if (sql.isNotEmpty()) {
                    connection.getConnection().prepareStatement(sql).use { statement ->
                        values.forEachIndexed { index, columnValue ->
                            setParameter(statement, index + 1, columnValue.value, columnValue.column.type)
                        }
                        
                        if (relation.type == RelationType.MANY_TO_MANY &&
                            (relation.cascade == CascadeType.ALL || relation.cascade == CascadeType.DELETE)
                        ) {
                            val orphanedRecordsSql = buildString {
                                append("DELETE FROM ${relation.targetTable._name} WHERE id NOT IN ")
                                append("(SELECT ${relation.targetTable._name}_id FROM ${relation.joinTableName})")
                            }
                            connection.getConnection().prepareStatement(orphanedRecordsSql).executeUpdate()
                        }
                        
                        statement.executeUpdate()
                    }
                }
            }
    }

    /**
     * Loads related entities from the database based on primary key values.
     * This method constructs and executes a SELECT query to fetch entities matching the provided primary key values.
     *
     * @param table The table to load entities from
     * @param primaryKeyValues A map of column names to their values for identifying the related entities
     * @return A list of entities matching the primary key values, or an empty list if no matches or no values provided
     * @throws IllegalStateException if a referenced column is not found in the table
     *
     * Example usage:
     * ```
     * val pkValues = mapOf("id" to 1, "type" to "user")
     * val relatedEntities = loadRelatedEntities(UserTable, pkValues)
     * ```
     */
    private fun <T : Entity> loadRelatedEntities(
        table: Table<T>,
        primaryKeyValues: Map<String, Any?>,
    ): List<T> {
        if (primaryKeyValues.isEmpty()) return emptyList()
        
        val sql = buildString {
            append("SELECT ")
            append(table.columns.joinToString(", ") { it.name })
            append(" FROM ")
            append(table._name)
            append(" WHERE ")
            append(primaryKeyValues.keys.joinToString(" AND ") { "$it = ?" })
        }
        
        return connection.getConnection().prepareStatement(sql).use { statement ->
            primaryKeyValues.values.forEachIndexed { index, value ->
                val column = table.columns.find { it.name == primaryKeyValues.keys.elementAt(index) }
                    ?: throw IllegalStateException("Column not found")
                setParameter(statement, index + 1, value, column.type)
            }
            
            val resultSet = statement.executeQuery()
            val entities = mutableListOf<T>()
            while (resultSet.next()) {
                entities.add(table.toEntity(resultSet))
            }
            entities
        }
    }
}