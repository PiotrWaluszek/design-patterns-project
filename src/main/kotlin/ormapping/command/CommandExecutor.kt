package ormapping.command


import ormapping.connection.DatabaseConnection
import ormapping.sql.*
import ormapping.entity.Entity
import ormapping.table.CascadeType
import ormapping.table.Relation
import ormapping.table.RelationType
import ormapping.table.Table
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class CommandExecutor(
    private val connection: DatabaseConnection,
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
        return when (builder) {
            is SelectBuilder -> SelectCommand(sql)
            is DeleteBuilder -> DeleteCommand(sql)
            is CreateTableBuilder -> CreateTableCommand(sql)
            is DropTableBuilder -> DropTableCommand(sql)
            else -> throw IllegalArgumentException("Unknown builder type")
        }.also { it.execute(connection) }
    }

    fun <T : Entity> find(table: Table<T>, value: Any): T? {
        val primaryKeyColumns = table.primaryKey
        if (primaryKeyColumns.isEmpty()) {
            throw IllegalArgumentException("Table must have primary keys defined")
        }
        val sql = buildString {
            append("SELECT ")
            append(table.columns.joinToString(", ") { it.name })
            append(" FROM ")
            append(table._name)
            append(" WHERE ")
            append(primaryKeyColumns.joinToString(" AND ") { "${it.name} = ?" })
        }
        
        return connection.getConnection().prepareStatement(sql).use { statement ->
            primaryKeyColumns.forEachIndexed { index, column ->
                setParameter(statement, index + 1, value, column.type)
            }
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val entity = table.toEntity(resultSet)
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
    
    fun <T : Entity> delete(table: Table<T>, id: Any): Boolean {
        val primaryKeyColumns = table.primaryKey
        if (primaryKeyColumns.isEmpty()) {
            throw IllegalArgumentException("Table must have primary keys defined")
        }
        
        val sql = buildString {
            append("DELETE FROM ")
            append(table._name)
            append(" WHERE ")
            append(primaryKeyColumns.joinToString(" AND ") { "${it.name} = ?" })
        }
        
        cascadeDelete(table, id)
        
        return connection.getConnection().prepareStatement(sql).use { statement ->
            primaryKeyColumns.forEachIndexed { index, column ->
                setParameter(statement, index + 1, id, column.type)
            }
            statement.executeUpdate() > 0
        }
    }
    
    fun <T : Entity> update(table: Table<T>, entity: T): Boolean {
        val columnValues = table.fromEntity(entity)
        val primaryKeyColumns = table.primaryKey
        if (primaryKeyColumns.isEmpty()) {
            throw IllegalArgumentException("Table must have primary keys defined")
        }
        
        val columnsToUpdate = table.columns.filterNot { it.primaryKey }
        
        val sql = buildString {
            append("UPDATE ")
            append(table._name)
            append(" SET ")
            append(columnsToUpdate.joinToString(", ") { "${it.name} = ?" })
            append(" WHERE ")
            append(primaryKeyColumns.joinToString(" AND ") { "${it.name} = ?" })
        }
        
        return connection.getConnection().prepareStatement(sql).use { statement ->
            var parameterIndex = 1
            columnsToUpdate.forEach { column ->
                setParameter(statement, parameterIndex++, columnValues[column], column.type)
            }
            primaryKeyColumns.forEach { column ->
                setParameter(statement, parameterIndex++, columnValues[column], column.type)
            }
            saveRelations(table, entity)
            statement.executeUpdate() > 0
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
            
            val columnValues = table.fromEntity(entity)
            val primaryKeyValues = columnValues.entries
                .filter { it.key.primaryKey }
                .map { it.value }
            
            connection.getConnection().prepareStatement(sql).use { statement ->
                primaryKeyValues.forEachIndexed { index, value ->
                    val column = table.primaryKey[index]
                    setParameter(statement, index + 1, value, column.type)
                }
                
                val resultSet = statement.executeQuery()
                val relatedEntities = mutableListOf<Entity>()
                
                while (resultSet.next()) {
                    @Suppress("UNCHECKED_CAST")
                    val relatedEntity = relation.targetTable.toEntity(resultSet)
                    relatedEntities.add(relatedEntity)
                }
                
                val property = entity::class.memberProperties.find { it.name == relation.joinTableName }
                
                when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.MANY_TO_ONE -> {
                        if (relatedEntities.isNotEmpty()) {
                            val setter = entity::class.members
                                .find { it.name == "${property?.name}_setter" }
                            setter?.call(entity, relatedEntities.first())
                        }
                    }
                    
                    RelationType.ONE_TO_MANY, RelationType.MANY_TO_MANY -> {
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
            append("t2.id = t1.${relation.targetTable._name}_id")
            append(" WHERE t1.${table.primaryKey.first().name} = ?")
        }
    }
    
    private fun <T : Entity> buildOneToManyQuery(table: Table<T>, relation: Relation<*>): String {
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN ${relation.targetTable._name} t2 ON ")
            append("t2.${table._name}_id = t1.id")
            append(" WHERE t1.${table.primaryKey.first().name} = ?")
        }
    }
    
    private fun <T : Entity> buildManyToOneQuery(table: Table<T>, relation: Relation<*>): String {
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN ${relation.targetTable._name} t2 ON ")
            append("t1.${relation.targetTable._name}_id = t2.id")
            append(" WHERE t1.${table.primaryKey.first().name} = ?")
        }
    }
    
    private fun <T : Entity> buildManyToManyQuery(table: Table<T>, relation: Relation<*>): String {
        val joinTableName = relation.joinTableName ?: "${table._name}_${relation.targetTable._name}"
        return buildString {
            append("SELECT t2.* FROM ${table._name} t1 ")
            append("JOIN $joinTableName j ON t1.id = j.${table._name}_id ")
            append("JOIN ${relation.targetTable._name} t2 ON j.${relation.targetTable._name}_id = t2.id")
            append(" WHERE t1.${table.primaryKey.first().name} = ?")
        }
    }
    
    private fun <T : Entity> saveRelations(table: Table<T>, entity: T) {
        table.relations.forEach { relation ->
            val property = entity::class.memberProperties.find { it.name == relation.joinTableName }
            
            @Suppress("UNCHECKED_CAST")
            val relatedEntities = when (val value = property?.call(entity)) {
                is MutableSet<*> -> value as? MutableSet<Entity>
                else -> null
            } ?: return@forEach
            
            relatedEntities.forEach { relatedEntity ->
                @Suppress("UNCHECKED_CAST")
                persist(relation.targetTable as Table<Entity>, relatedEntity)
            }
        }
    }
    
    private fun <T : Entity> cascadeDelete(table: Table<T>, id: Any) {
        table.relations.filter { it.cascade == CascadeType.DELETE || it.cascade == CascadeType.ALL }
            .forEach { relation ->
                val sql = when (relation.type) {
                    RelationType.ONE_TO_ONE, RelationType.ONE_TO_MANY -> {
                        buildString {
                            append("DELETE FROM ${relation.targetTable._name} WHERE ")
                            append("${table._name}_id = ?")
                        }
                    }
                    
                    RelationType.MANY_TO_MANY -> {
                        val joinTableName = relation.joinTableName
                            ?: "${table._name}_${relation.targetTable._name}"
                        buildString {
                            append("DELETE FROM $joinTableName WHERE ")
                            append("${table._name}_id = ?")
                        }
                    }
                    
                    RelationType.MANY_TO_ONE -> {
                        ""
                    }
                }
                
                if (sql.isNotEmpty()) {
                    connection.getConnection().prepareStatement(sql).use { statement ->
                        setParameter(statement, 1, id, table.primaryKey.first().type)
                        
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