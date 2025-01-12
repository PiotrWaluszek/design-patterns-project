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


class CommandExecutor(
    private val connection: DatabaseConnection,
    private val logger: MultiDestinationLogger,
    private val logDest: String,
) {
    
    fun createSelect(): SelectBuilder = SelectBuilder()
    
    fun createDelete(): DeleteBuilder = DeleteBuilder()
    
    fun createTable(): CreateTableBuilder = CreateTableBuilder()
    
    fun dropTable(table: Table<*>): DropTableBuilder {
        return DropTableBuilder(connection.getDialect(), table, this)
    }
    
    // Metoda wykonująca zbudowane zapytanie
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
                // Tworzymy główną encję z pominięciem kolekcji (będą nullami)
                val constructor = table.entityClass.primaryConstructor
                    ?: throw IllegalStateException("Entity must have a primary constructor")
                
                val parameters = constructor.parameters.associateWith { param ->
                    when {
                        // Dla kolekcji zwracamy PUSTĄ KOLEKCJĘ odpowiedniego typu
                        param.type.classifier is KClass<*> &&
                                Collection::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> {
                            when {
                                Set::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> mutableSetOf<Any>()
                                List::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> mutableListOf<Any>()
                                else -> throw IllegalStateException("Unsupported collection type ${param.type}")
                            }
                        }
                        // Dla zwykłych kolumn pobieramy wartości
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
                                // Jeśli nie ma kolumny a parametr jest opcjonalny, używamy null
                                if (param.isOptional) null
                                else throw IllegalStateException("No column found for required parameter ${param.name}")
                            }
                        }
                    }
                }
                
                // Tworzymy encję
                val entity = constructor.callBy(parameters)
                
                // Ładujemy relacje
                loadRelations(table, entity)
                
                entity
            } else {
                null
            }
        }
    }
    
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
                println(values)
                table.columns.forEachIndexed { index, column ->
                    setParameter(statement, index + 1, values[column], column.type)
                }
                statement.addBatch()
                saveRelations(table, entity)
            }
            statement.executeBatch()
        }
    }
    
    fun <T : Entity> delete(table: Table<T>, vararg values: ColumnValue<*>): Boolean {
        // Sprawdzenie czy przekazane kolumny pokrywają wszystkie klucze główne
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
        
        // Zakładam, że cascadeDelete powinno otrzymać wszystkie wartości kluczy
        // zamiast pojedynczej wartości id
        
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
    
    fun <T : Entity, R : Entity> slayOrphans(table: Table<T>, relatedTable: Table<R>): Int {
        // Znajdujemy odpowiedni klucz obcy pomiędzy tabelami
        val relevantForeignKey = table.foreignKeys.find { fk ->
            fk.targetTable == relatedTable._name
        } ?: throw IllegalArgumentException("No foreign key found from ${table._name} to ${relatedTable._name}")
        
        val orphanedRecords = buildString {
            append("DELETE FROM ${table._name} WHERE ")
            // Bierzemy tylko kolumny związane z konkretnym kluczem obcym
            val foreignKeyColumns = table.columns.filter { column ->
                relevantForeignKey.targetColumns.any { targetCol ->
                    column.name == "${relatedTable._name}_$targetCol"
                }
            }
            // Sprawdzamy czy te konkretne kolumny są NULL
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
    
    // I podobnie dla findOrphans:
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
    
    private fun <T : Entity> orphan(table: Table<T>, values: Array<out ColumnValue<*>>) {
        table.relations.filter { it.cascade == CascadeType.NONE || it.cascade == CascadeType.UPDATE }
            .forEach { relation ->
                val sql = when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.ONE_TO_MANY -> {
                        buildString {
                            append("UPDATE ${relation.targetTable._name} SET ")
                            // Ustawiamy wszystkie kolumny klucza obcego na NULL
                            append(values.joinToString(", ") {
                                "${table._name}_${it.column.name} = NULL"
                            })
                            append(" WHERE ")
                            // Warunek WHERE dla aktualnych wartości
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
    
    fun <T : Entity> update(table: Table<T>, entity: T, vararg newValues: ColumnValue<*>): T {
        val columnValues = table.fromEntity(entity)
        val primaryKeyColumns = table.primaryKey
        if (primaryKeyColumns.isEmpty()) {
            throw IllegalArgumentException("Table must have primary keys defined")
        }
        
        // Sprawdzamy czy któreś z nowych wartości dotyczą kluczy głównych
        val updatedPrimaryKeys = newValues.filter { newValue ->
            primaryKeyColumns.contains(newValue.column)
        }
        
        // Przygotowujemy SQL update
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
            
            // SET values - używamy nowych wartości
            newValues.forEach { columnValue ->
                setParameter(statement, parameterIndex++, columnValue.value, columnValue.column.type)
            }
            
            // WHERE values - używamy oryginalnych wartości z entity
            primaryKeyColumns.forEach { column ->
                setParameter(statement, parameterIndex++, columnValues[column], column.type)
            }
            
            statement.executeUpdate() > 0
        }
        
        // Jeśli update się powiódł i modyfikujemy klucze główne
        if (updateSuccessful && updatedPrimaryKeys.isNotEmpty()) {
            
            // Najpierw obsługujemy relacje z CASCADE ALL lub UPDATE
            table.relations
                .filter { it.cascade == CascadeType.ALL || it.cascade == CascadeType.UPDATE }
                .forEach { relation ->
                    cascadeUpdate(
                        table,
                        entity,
                        *updatedPrimaryKeys.toTypedArray()
                    )
                }
            
            // Następnie obsługujemy relacje z NONE lub DELETE
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
        
        // Nadpisujemy oryginalne wartości nowymi
        val updatedValues = originalValues.toMutableMap()
        newValues.forEach { newValue ->
            updatedValues[newValue.column] = newValue.value
        }
        
        val parameters = constructor.parameters.associateWith { param ->
            when {
                // Jeśli parametr jest kolekcją (Set, List, etc.), zwracamy null
                param.type.classifier is KClass<*> &&
                        Collection::class.java.isAssignableFrom((param.type.classifier as KClass<*>).java) -> {
                    null
                }
                // Dla zwykłych pól szukamy wartości w kolumnach
                else -> {
                    val column = table.columns.find { it.name == param.name }
                    if (column != null) {
                        updatedValues[column]
                    } else {
                        // Jeśli nie znaleźliśmy kolumny, a parametr ma wartość domyślną, pozwalamy konstruktorowi użyć defaultowej
                        if (param.isOptional) null else throw IllegalStateException("No column found for required parameter ${param.name}")
                    }
                }
            }
        }
        
        return constructor.callBy(parameters)
    }
    
    private fun <T : Entity> cascadeUpdate(table: Table<T>, entity: T, vararg updatedKeys: ColumnValue<*>) {
        table.relations
            .filter { it.cascade == CascadeType.ALL || it.cascade == CascadeType.UPDATE }
            .forEach { relation ->
                val sql = when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.ONE_TO_MANY -> {
                        buildString {
                            append("UPDATE ${relation.targetTable._name} SET ")
                            // Dla każdego zmienionego klucza, aktualizujemy odpowiadającą mu kolumnę w tabeli docelowej
                            append(updatedKeys.joinToString(", ") {
                                "${table._name}_${it.column.name} = ?"
                            })
                            append(" WHERE ")
                            // Znajdujemy rekordy z starymi wartościami kluczy
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
                        // Nowe wartości
                        updatedKeys.forEach {
                            setParameter(statement, paramIndex++, it.value, it.column.type)
                        }
                        // Stare wartości
                        updatedKeys.forEach {
                            val oldValue = table.fromEntity(entity)[it.column]
                            setParameter(statement, paramIndex++, oldValue, it.column.type)
                        }
                        statement.executeUpdate()
                    }
                }
            }
    }
    
    private fun <T : Entity> orphanUpdate(table: Table<T>, entity: T) {
        table.relations.filter { it.cascade == CascadeType.NONE || it.cascade == CascadeType.DELETE }
            .forEach { relation ->
                // Szukamy klucza obcego w tabeli DOCELOWEJ, który wskazuje na naszą tabelę
                val relevantForeignKey = relation.targetTable.foreignKeys.find { fk ->
                    fk.targetTable == table._name
                }
                    ?: throw IllegalArgumentException("No foreign key found from ${relation.targetTable._name} to ${table._name}")
                
                // Znajdujemy kolumny klucza obcego w tabeli DOCELOWEJ
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
                        // Pobieramy wartości kluczy głównych z entity
                        val entityValues = table.fromEntity(entity)
                        // Dla każdej kolumny klucza obcego w tabeli docelowej
                        // ustawiamy odpowiadającą jej wartość z klucza głównego naszej encji
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
    
    inline fun <reified T : Entity> persistAll(
        table: Table<T>, entities: Collection<T>,
    ) {
        persist(table, *entities.toTypedArray())
    }
    
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
                                    
                                    // Jeśli wartość jest null, ustawiamy ją na pierwszą encję z listy powiązań
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
    
    private fun <T : Entity> buildOneToOneQuery(table: Table<T>, relation: Relation<*>): String {
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN ${relation.targetTable._name} t2 ON ")
            // Łączymy po wszystkich kolumnach klucza obcego
            append(table.primaryKey.joinToString(" AND ") { pk ->
                "t2.${table._name}_${pk.name} = t1.${pk.name}"
            })
            // WHERE po wszystkich kolumnach klucza głównego
            append(" WHERE ")
            append(table.primaryKey.joinToString(" AND ") {
                "t1.${it.name} = ?"
            })
        }
    }
    
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
    
    private fun <T : Entity> buildManyToManyQuery(table: Table<T>, relation: Relation<*>): String {
        val joinTableName = relation.joinTableName ?: "${table._name}_${relation.targetTable._name}"
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN $joinTableName j ON ")
            // Łączenie z tabelą łączącą
            append(table.primaryKey.joinToString(" AND ") { pk ->
                "t1.${pk.name} = j.${table._name}_${pk.name}"
            })
            append(" JOIN ${relation.targetTable._name} t2 ON ")
            // Łączenie tabeli łączącej z tabelą docelową
            append(relation.targetTable.primaryKey.joinToString(" AND ") { pk ->
                "j.${relation.targetTable._name}_${pk.name} = t2.${pk.name}"
            })
            append(" WHERE ")
            append(table.primaryKey.joinToString(" AND ") {
                "t1.${it.name} = ?"
            })
        }
    }
    
    private fun <T : Entity> saveRelations(table: Table<T>, entity: T) {
        table.relations.forEach { relation ->
            // Znajdujemy property odpowiadające powiązanej tabeli
            val property = entity::class.memberProperties
                .find { it.name == relation.targetTable._name.lowercase() }
                ?.call(entity) ?: return@forEach
            
            when (property) {
                // Dla relacji jeden-do-jeden lub wiele-do-jednego
                is Entity -> {
                    @Suppress("UNCHECKED_CAST")
                    persist(relation.targetTable as Table<Entity>, property)
                    
                    // Ustawiamy relację zwrotną
                    val backProperty = property::class.memberProperties
                        .find { it.name == table._name.lowercase() }
                    if (backProperty != null) {
                        val setter = property::class.members
                            .find { it.name == "${backProperty.name}_setter" }
                        setter?.call(property, entity)
                    }
                }
                // Dla relacji jeden-do-wielu lub wiele-do-wielu
                is Collection<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    property.forEach { relatedEntity ->
                        if (relatedEntity is Entity) {
                            persist(relation.targetTable as Table<Entity>, relatedEntity)
                            
                            // Ustawiamy relację zwrotną
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