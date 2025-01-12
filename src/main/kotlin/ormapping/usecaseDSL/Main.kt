package ormapping

import ormapping.command.*
import ormapping.connection.DatabaseConfig
import ormapping.connection.DatabaseConnection
import ormapping.connection.ProviderType
import ormapping.connection.SQLiteConnection
import ormapping.entity.Entity
import ormapping.table.Table
import ormapping.sql.*

// Model danych

data class Employee(
    var id: Int,
    var name: String,
) : Entity(Employees)

object Employees : Table<Employee>("employees", Employee::class) {
    val id = integer("id").primaryKey()
    val name = varchar("name", 255).primaryKey()
}

data class Department(
    var id: Int,
    var employee_id: Int,
    var department_name: String,
) : Entity(Departments)

object Departments : Table<Department>("departments", Department::class) {
    val id = integer("id").primaryKey()
    val employeeId = integer("employee_id")
    val departmentName = varchar("department_name", 255)
}

fun main() {
    val config = DatabaseConfig(
        type = ProviderType.SQLITE,
        url = "jdbc:sqlite:orm.db"
    )
    val connection = DatabaseConnection.createConnection(config)
    val logger = MultiDestinationLogger.getInstance()
    val dest = "./logs/log2.log"
    logger.initializeLogDestination(dest)
    val executor = CommandExecutor(connection, logger, dest)
    
    println("=== Test 1: Tworzenie tabel ===")
    try {
        val createEmployeesTable = executor.createTable()
            .fromTable(Employees)
        
        
        println("Wygenerowane polecenie SQL:")
        println(createEmployeesTable.build())
        
        val createDepartmentsTable = executor.createTable()
            .fromTable(Departments)
        
        println("Wygenerowane polecenie SQL:")
        println(createDepartmentsTable.build())
        
        executor.executeSQL(createEmployeesTable)
        executor.executeSQL(createDepartmentsTable)
        
        println("Tabele zostały utworzone.")
    } catch (e: Exception) {
        println("Błąd podczas tworzenia tabel: ${e.message}")
    }
    
    println("\n=== Test 2: Wstawianie danych ===")
    val employee1 = Employee(1, "Jan Kowalski")
    val employee2 = Employee(2, "Anna Nowak")
    val department1 = Department(1, 1, "HR")
    val department2 = Department(2, 2, "IT")
    
    try {
        executor.persist(Employees, employee1, employee2)
        executor.persist(Departments, department1, department2)
        println("Dane zostały wstawione.")
    } catch (e: Exception) {
        println("Błąd podczas wstawiania danych: ${e.message}")
    }
    
    println("\n=== Test 3: Sprawdzenie zapisanych danych przez SQL DSL (WHERE) ===")
    try {
        val selectBuilder = executor.createSelect()
            .select("*")
            .from(Employees)
            .where("id IN (1, 2)")
        
        val sql = selectBuilder.build()
        println("Generated SQL Command:\n$sql")
        
        val selectCommand = executor.executeSQL(selectBuilder) as SelectCommand
        println("Wyniki zapytania SELECT:")
        selectCommand.printResults()
    } catch (e: Exception) {
        println("Błąd podczas SELECT: ${e.message}")
    }
    
    println("\n=== Test 4: Zapytania JOIN ===")
    try {
        val selectBuilderLeftJoin = executor.createSelect()
            .select(Employees.id, Employees.name, Departments.departmentName)
            .from(Employees)
            .leftJoin(Departments, Employees.id, Departments.employeeId)
            .where("departments.department_name IS NOT NULL")
        
        val sqlLeftJoin = selectBuilderLeftJoin.build()
        println("Generated SQL (LEFT JOIN):\n$sqlLeftJoin")
        
        val selectBuilderInnerJoin = executor.createSelect()
            .select(Employees.id, Employees.name, Departments.departmentName)
            .from(Employees)
            .innerJoin(Departments, Employees.id, Departments.employeeId)
            .where("departments.department_name = 'IT'")
        
        val sqlInnerJoin = selectBuilderInnerJoin.build()
        println("Generated SQL (INNER JOIN):\n$sqlInnerJoin")
        
    } catch (e: Exception) {
        println("Błąd przy testach JOIN: ${e.message}")
    }
    
    println("\n=== Test 5: GROUP BY, HAVING, ORDER BY ===")
    try {
        val selectGroupByHaving = executor.createSelect()
            .select(Departments.departmentName, "COUNT(*) AS employee_count")
            .from(Departments)
            .groupBy(Departments.departmentName)
            .having("employee_count > 1")
        
        val sqlGroupByHaving = selectGroupByHaving.build()
        println("Generated SQL (GROUP BY, HAVING):\n$sqlGroupByHaving")
        
        val selectOrderBy = executor.createSelect()
            .select(Employees.id, Employees.name)
            .from(Employees)
            .orderBy("name ASC", "id DESC")
        
        val sqlOrderBy = selectOrderBy.build()
        println("Generated SQL (ORDER BY):\n$sqlOrderBy")
        
    } catch (e: Exception) {
        println("Błąd przy testach GROUP BY / HAVING / ORDER BY: ${e.message}")
    }
    
    println("\n=== Test 6: UNION i UNION ALL ===")
    try {
        // Drugie zapytanie SELECT
        val selectUnion = executor.createSelect()
            .select(Employees.id, Employees.name)
            .from(Employees)
            .where("id > 2")
            .build()
        
        // Budowanie zapytania UNION
        val unionBuilder = executor.createSelect()
            .select(Employees.id, Employees.name)
            .from(Employees)
            .where("id <= 2")
            .union(selectUnion)  // Dodaj drugie zapytanie jako część UNION
        
        // Generowanie finalnego SQL
        val sqlUnion = unionBuilder.build()
        println("Generated SQL (UNION, UNION ALL):\n$sqlUnion")
        
    } catch (e: Exception) {
        println("Błąd przy testach UNION: ${e.message}")
    }
    println("\n=== Test 7: Funkcje agregujące ===")
    try {
        val selectAggregate = executor.createSelect()
            .select(
                SelectBuilder().count("id") + " AS total_employees",
                SelectBuilder().sum("id") + " AS total_id",
                SelectBuilder().avg("id") + " AS avg_id"
            )
            .from(Employees)
        
        val sqlAggregate = selectAggregate.build()
        println("Generated SQL (Aggregate Functions):\n$sqlAggregate")
        
    } catch (e: Exception) {
        println("Błąd przy testach funkcji agregujących: ${e.message}")
    }
    
    println("\n=== Test 8: Podzapytania (Subqueries) ===")
    try {
        val subQuery = executor.createSelect()
            .select("AVG(id)")
            .from(Employees)
            .build()
        
        val selectSubQuery = executor.createSelect()
            .select(Employees.id, Employees.name)
            .from(Employees)
            .where("id > ($subQuery)")
        
        val sqlSubQuery = selectSubQuery.build()
        println("Generated SQL (Subquery):\n$sqlSubQuery")
        
    } catch (e: Exception) {
        println("Błąd przy testach podzapytań: ${e.message}")
    }
    
    println("\n=== Test 9: Usuwanie danych ===")
    try {
        val deleteBuilder = executor.createDelete()
            .from(Employees)
            .where("id = 1")
        
        val sqlDelete = deleteBuilder.build()
        println("Generated SQL Command:\n$sqlDelete")
        
        val deleteCommand = executor.executeSQL(deleteBuilder) as DeleteCommand
        println("Usunięto rekordów: ${deleteCommand.getAffectedRows()}")
    } catch (e: Exception) {
        println("Błąd podczas usuwania danych: ${e.message}")
    }
    
    
    
    println("\n=== Test 10: Usuwanie tabel ===")
    try {
        val dropEmployeesTable = executor.dropTable(Employees)
        val dropDepartmentsTable = executor.dropTable(Departments)
        
        executor.executeSQL(dropEmployeesTable)
        executor.executeSQL(dropDepartmentsTable)
        
        println("Tabele zostały usunięte.")
    } catch (e: Exception) {
        println("Błąd podczas usuwania tabel: ${e.message}")
    }
    
    connection.close()
}
