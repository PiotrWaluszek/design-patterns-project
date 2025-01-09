package ormapping.command

import ormapping.connection.DatabaseConnection
import ormapping.entity.Entity
import ormapping.table.Table
import java.math.BigDecimal
import java.sql.Date
import java.sql.SQLException
import java.time.LocalDate

class CommandExecutor(
    private val connection: DatabaseConnection,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Entity> find(table: Table<T>, value: Any): T? {
        val primaryKeyColumn = table.primaryKey
            ?: throw IllegalStateException("Table ${table._name} has no primary key defined")
        
        if (value::class != primaryKeyColumn.type) {
            throw IllegalArgumentException(
                "Value type ${value::class} doesn't match primary key type ${primaryKeyColumn.type}"
            )
        }
        
        val sql = buildString {
            append("SELECT ")
            append(table.columns.joinToString(", ") { it.name })
            append(" FROM ")
            append(table._name)
            append(" WHERE ")
            append(primaryKeyColumn.name)
            append(" = ?")
        }
        
        return connection.getConnection().prepareStatement(sql).use { statement ->
            when (value) {
                is Int -> statement.setInt(1, value)
                is String -> statement.setString(1, value)
                is Boolean -> statement.setBoolean(1, value)
                is LocalDate -> statement.setDate(1, Date.valueOf(value))
                is BigDecimal -> statement.setBigDecimal(1, value)
                else -> throw IllegalArgumentException("Unsupported primary key type: ${value::class}")
            }
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                table.toEntity(resultSet)
            } else {
                null
            }
        }
    }
    
    fun <T : Entity> persist(
        table: Table<T>,
        vararg entities: T,
        onDuplicateKey: DuplicateKeyStrategy = DuplicateKeyStrategy.ERROR,
    ) {
        if (entities.isEmpty()) return
        
        val primaryKey = table.primaryKey
            ?: throw IllegalStateException("Table ${table._name} has no primary key defined")
        
        val primaryKeyValues = entities.map { entity ->
            table.fromEntity(entity)[primaryKey]
        }
        
        if (primaryKeyValues.size != primaryKeyValues.distinct().size && onDuplicateKey == DuplicateKeyStrategy.ERROR) {
            throw IllegalArgumentException("Provided entities contain duplicate primary keys")
        }
        
        val columnValueMap = table.fromEntity(entities.first())
        val columns = columnValueMap.keys
        val dialect = connection.getDialect()
        
        val sql = when (onDuplicateKey) {
            DuplicateKeyStrategy.ERROR -> buildString {
                append("INSERT INTO ${table._name} (")
                append(columns.joinToString(", ") { it.name })
                append(") VALUES (")
                append(columns.joinToString(", ") { "?" })
                append(")")
            }
            
            DuplicateKeyStrategy.UPDATE -> buildString {
                // Używamy dialektu dla składni UPSERT
                append("INSERT INTO ${table._name} (")
                append(columns.joinToString(", ") { it.name })
                append(") VALUES (")
                append(columns.joinToString(", ") { "?" })
                append(") ")
                append(dialect.getUpsertSyntax())
                append("(${primaryKey.name}) DO UPDATE SET ")
                append(
                    columns.filterNot { it.primaryKey }
                        .joinToString(", ") { "${it.name} = excluded.${it.name}" }
                )
            }
            
            DuplicateKeyStrategy.IGNORE -> buildString {
                append(dialect.getInsertIgnoreSyntax())
                append(" ${table._name} (")
                append(columns.joinToString(", ") { it.name })
                append(") VALUES (")
                append(columns.joinToString(", ") { "?" })
                append(")")
            }
        }
        
        try {
            connection.getConnection().prepareStatement(sql).use { statement ->
                for (entity in entities) {
                    val values = table.fromEntity(entity)
                    var parameterIndex = 1
                    
                    for (column in columns) {
                        val value = values[column]
                        when (column.type) {
                            Int::class -> statement.setInt(parameterIndex, value as Int)
                            String::class -> statement.setString(parameterIndex, value as String)
                            Boolean::class -> statement.setBoolean(parameterIndex, value as Boolean)
                            LocalDate::class -> statement.setDate(
                                parameterIndex,
                                if (value != null) java.sql.Date.valueOf(value as LocalDate) else null
                            )
                            
                            BigDecimal::class -> statement.setBigDecimal(parameterIndex, value as BigDecimal)
                            else -> throw IllegalArgumentException("Unsupported type: ${column.type}")
                        }
                        parameterIndex++
                    }
                    
                    statement.addBatch()
                }
                
                statement.executeBatch()
            }
        } catch (e: SQLException) {
            when {
                e.message?.contains("Duplicate entry", ignoreCase = true) == true ->
                    throw DuplicateKeyException("Attempt to insert duplicate key in table ${table._name}", e)
                
                else -> throw e
            }
        }
    }
    
    fun <T : Entity> delete(table: Table<T>, id: Any): Boolean {
        val primaryKeyColumn = table.primaryKey
            ?: throw IllegalStateException("Table ${table._name} has no primary key defined")
        
        if (id::class != primaryKeyColumn.type) {
            throw IllegalArgumentException(
                "Value type ${id::class} doesn't match primary key type ${primaryKeyColumn.type}"
            )
        }
        
        val sql = """
            DELETE FROM ${table._name}
            WHERE ${primaryKeyColumn.name} = ?
        """.trimIndent()
        
        return connection.getConnection().prepareStatement(sql).use { statement ->
            when (id) {
                is Int -> statement.setInt(1, id)
                is String -> statement.setString(1, id)
                is Boolean -> statement.setBoolean(1, id)
                is LocalDate -> statement.setDate(1, java.sql.Date.valueOf(id))
                is BigDecimal -> statement.setBigDecimal(1, id)
                else -> throw IllegalArgumentException("Unsupported primary key type: ${id::class}")
            }
            
            statement.executeUpdate() > 0
        }
    }
    
    fun <T : Entity> update(table: Table<T>, entity: T): Boolean {
        val primaryKeyColumn = table.primaryKey
            ?: throw IllegalStateException("Table ${table._name} has no primary key defined")
        
        val columnValues = table.fromEntity(entity)
        
        val primaryKeyValue = columnValues[primaryKeyColumn]
            ?: throw IllegalArgumentException("Primary key value cannot be null")
        
        val columnsToUpdate = columnValues.keys.filterNot { it.primaryKey }
        
        if (columnsToUpdate.isEmpty()) {
            return false
        }
        
        val sql = buildString {
            append("UPDATE ${table._name} SET ")
            append(columnsToUpdate.joinToString(", ") { "${it.name} = ?" })
            append(" WHERE ${primaryKeyColumn.name} = ?")
        }
        
        return connection.getConnection().prepareStatement(sql).use { statement ->
            var parameterIndex = 1
            
            for (column in columnsToUpdate) {
                val value = columnValues[column]
                when (column.type) {
                    Int::class -> statement.setInt(parameterIndex, value as Int)
                    String::class -> statement.setString(parameterIndex, value as String)
                    Boolean::class -> statement.setBoolean(parameterIndex, value as Boolean)
                    LocalDate::class -> statement.setDate(
                        parameterIndex,
                        if (value != null) java.sql.Date.valueOf(value as LocalDate) else null
                    )
                    
                    BigDecimal::class -> statement.setBigDecimal(parameterIndex, value as BigDecimal)
                    else -> throw IllegalArgumentException("Unsupported type: ${column.type}")
                }
                parameterIndex++
            }
            
            when (primaryKeyColumn.type) {
                Int::class -> statement.setInt(parameterIndex, primaryKeyValue as Int)
                String::class -> statement.setString(parameterIndex, primaryKeyValue as String)
                Boolean::class -> statement.setBoolean(parameterIndex, primaryKeyValue as Boolean)
                LocalDate::class -> statement.setDate(
                    parameterIndex,
                    java.sql.Date.valueOf(primaryKeyValue as LocalDate)
                )
                
                BigDecimal::class -> statement.setBigDecimal(parameterIndex, primaryKeyValue as BigDecimal)
                else -> throw IllegalArgumentException("Unsupported primary key type: ${primaryKeyColumn.type}")
            }
            
            statement.executeUpdate() > 0
        }
    }
    
    
    inline fun <reified T : Entity> persistAll(
        table: Table<T>, entities: Collection<T>,
        onDuplicateKey: DuplicateKeyStrategy = DuplicateKeyStrategy.ERROR,
    ) {
        persist(table, *entities.toTypedArray(), onDuplicateKey = onDuplicateKey)
    }
}

enum class DuplicateKeyStrategy {
    ERROR,
    UPDATE,
    IGNORE
}

class DuplicateKeyException(message: String, cause: Throwable? = null) : Exception(message, cause)
