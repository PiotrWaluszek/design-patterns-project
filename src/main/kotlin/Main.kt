import orm.builder.CreateTableQueryBuilder
import orm.builder.InsertQueryBuilder
import orm.builder.SelectQueryBuilder
import orm.command.CommandExecutor
import orm.connection.DatabaseConnectionFactory
import orm.entity.Entity
import orm.logger.Logger
import orm.sql.ResultRow
import orm.table.Table

data class User(
    val id: Long? = null,
    val name: String,
    val email: String
) : Entity

class UserTable : Table<User>("users", User::class) {
    val id = integer("id").primaryKey()
    val name = varchar("name")
    val email = varchar("email")
}

fun main() {
    val factory = DatabaseConnectionFactory.getInstance()
    val connection = factory.getConnection("sqlite")
    val logger = Logger.getInstance()
    val executor = CommandExecutor(connection, logger)
    
    val userTable = UserTable()
    
    val createTableCommand = CreateTableQueryBuilder(userTable)
        .build(connection, logger)
    executor.execute(createTableCommand)
    
    val newUser = User(
        name = "Jan Kowalski",
        email = "jan@example.com"
    )
    
    val insertCommand = InsertQueryBuilder(userTable)
        .values(userTable.fromEntity(newUser))
        .build(connection, logger)
    executor.execute(insertCommand)
    
    val selectCommand = SelectQueryBuilder(userTable)
        .columns(userTable.id, userTable.name, userTable.email)
        .where("email = ?", "jan@example.com")
        .build(connection, logger)
    
    val resultSet = executor.execute(selectCommand)
    if (resultSet.next()) {
        val row = ResultRow(mapOf(
            "id" to resultSet.getLong("id"),
            "name" to resultSet.getString("name"),
            "email" to resultSet.getString("email")
        ))
        val foundUser = userTable.toEntity(row)
        logger.log("Found user: ID=${foundUser.id}, name=${foundUser.name}, email=${foundUser.email}")
    }
}