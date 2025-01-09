package ormapping

import ormapping.command.CommandExecutor
import ormapping.connection.DatabaseConfig
import ormapping.connection.SQLiteConnection
import ormapping.entity.Entity
import ormapping.table.Table


data class Employee(
    var id: Int,
    var name: String,
) : Entity

object Employees : Table<Employee>("employees", Employee::class) {
    var id = integer("id").primaryKey()
    var name = varchar("name", 255)
}

fun main() {
    val config = DatabaseConfig(
        url = "jdbc:sqlite:orm.db"
    )
    val connection = SQLiteConnection.create(config)
    val executor = CommandExecutor(connection)
    
    // 1. Tworzymy dwóch pracowników
    val employee1 = Employee(-1, "Jan Kowalski")
    val employee2 = Employee(2, "Anna Nowak")
    println("Utworzeni pracownicy:")
    println("Employee 1: $employee1")
    println("Employee 2: $employee2")
    println()
    
    // 2. Zapisujemy ich do bazy
    executor.persist(Employees, employee1, employee2)
    println("Pracownicy zostali zapisani do bazy")
    println()
    
    // 3. Odczytujemy ich z bazy i wyświetlamy
    val found1 = executor.find(Employees, 1)
    val found2 = executor.find(Employees, 2)
    println("Odczytani z bazy pracownicy:")
    println("Found 1: $found1")
    println("Found 2: $found2")
    println()
    
    // 4. Usuwamy pierwszego pracownika
    executor.delete(Employees, 1)
    println("Usunięto pracownika 1")
    println()
    
    // 5. Modyfikujemy drugiego pracownika
    found2?.let {
        it.name = "Anna Kowalska" // zmiana nazwiska
        executor.update(Employees, it)
        println("Zmodyfikowano pracownika 2")
    }
    println()
    
    // 6. Próbujemy odczytać obu pracowników (jeden powinien być null)
    val afterDelete1 = executor.find(Employees, 1)
    val afterUpdate2 = executor.find(Employees, 2)
    println("Stan po usunięciu/modyfikacji:")
    println("Employee 1 (powinien być null): $afterDelete1")
    println("Employee 2 (zmodyfikowany): $afterUpdate2")
    println()
    
    // 7. Usuwamy drugiego pracownika
    executor.delete(Employees, 2)
    println("Usunięto pracownika 2")
    println()
    
    // 8. Próbujemy odczytać obu pracowników (oba null)
    val final1 = executor.find(Employees, 1)
    val final2 = executor.find(Employees, 2)
    println("Stan końcowy (oba powinny być null):")
    println("Employee 1: $final1")
    println("Employee 2: $final2")
}
