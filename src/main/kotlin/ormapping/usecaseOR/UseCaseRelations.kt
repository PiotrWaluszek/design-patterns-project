package ormapping.usecaseOR

import ormapping.command.CommandExecutor
import ormapping.command.MultiDestinationLogger
import ormapping.connection.DatabaseConfig
import ormapping.connection.DatabaseConnection
import ormapping.connection.ProviderType
import ormapping.connection.SQLiteConnection
import ormapping.entity.Entity
import ormapping.table.CascadeType
import ormapping.table.Table
import ormapping.table.eq

object Husbands : Table<Husband>("husbands", Husband::class) {
    var name = varchar("name", 255)
    var surname = varchar("surname", 255)
    
    init {
        surname.primaryKey()
        oneToOne(Wives, CascadeType.ALL)
    }
}

object Wives : Table<Wife>("wives", Wife::class) {
    var name = varchar("name", 255)
    var surname = varchar("surname", 255)
    
    init {
        surname.primaryKey()
        oneToOne(Husbands, CascadeType.ALL)
    }
}

data class Husband(
    var surname: String,
    var name: String,
    var wife: Wife? = null,
) : Entity(Husbands)

data class Wife(
    var surname: String,
    var name: String,
    var husband: Husband? = null,
) : Entity(Wives)


data class Professor(
    var surname: String,
    var faculty: String,
    var students: MutableSet<Student>? = mutableSetOf<Student>(),
) : Entity(Professors) {
    override fun toString(): String {
        return "$surname $faculty"
    }
}

//------------------------------------------
data class Student(
    var surname: String,
    var student_index: Int,
    var professor: Professor? = null,
) : Entity(Students) {
    override fun toString(): String {
        return "$surname $student_index"
    }
}

object Professors : Table<Professor>("professors", Professor::class) {
    var faculty = varchar("faculty", 255)
    var surname = varchar("surname", 255)
    
    
    init {
        surname.primaryKey()
        faculty.primaryKey()
        oneToMany(Students, cascade = CascadeType.UPDATE)
    }
}

object Students : Table<Student>("students", Student::class) {
    var surname = varchar("surname", 255)
    var student_index = integer("student_index")
    
    init {
        surname.primaryKey()
        student_index.primaryKey()
        manyToOne(Professors)
    }
}

fun main() {
    val config = DatabaseConfig(
        type = ProviderType.SQLITE,
        url = "jdbc:sqlite:ormSobota.db"
    )
    val connection = DatabaseConnection.createConnection(config)
    
    val logger = MultiDestinationLogger.getInstance()
    val dest = "./logs/log1.log"
    logger.initializeLogDestination(dest)
    val executor = CommandExecutor(connection, logger, dest)
    `Showcase one to one relations`(executor)
    `Showacase one to many and many to one relations orphans`(executor)
}

fun `Showcase one to one relations`(commandExecutor: CommandExecutor) {
    val wife = Wife("Kowalska", "Anna")
    val husband = Husband("Kowalski", "Jan", wife)
    wife.husband = husband
    // Zapisujemy do bazy
    commandExecutor.persist(Husbands, husband)
    commandExecutor.persist(Wives, wife)
    
    // Znajdujemy męża
    val foundHusband = commandExecutor.find(Husbands, Husbands.surname eq "Kowalski")
    println("${foundHusband?.wife?.name} - powinno być Anna")  // Powinno wydrukować "Anna"
    println(foundHusband)
    
    // Rozwód (usunie też żonę przez CascadeType.ALL)
    commandExecutor.delete(Husbands, Husbands.surname eq "Kowalski")
    val foundHusbandAfter = commandExecutor.find(Husbands, Husbands.surname eq "Kowalski")
    println("$foundHusbandAfter - powinno być null")
}

fun `Showacase one to many and many to one relations orphans`(executor: CommandExecutor) {
    var professor1 = Professor("Mrozek", "WIMIR")
    var professor2 = Professor("Nowak", "WEAIIB")
    
    executor.persist(Professors, professor1, professor2)
    
    var stud1 = Student("Aaa", 111, professor1)
    var stud2 = Student("Baa", 222, professor1)
    var stud3 = Student("Caa", 333, professor2)
    var stud4 = Student("Daa", 444, professor2)
    executor.persist(Students, stud1, stud2, stud3, stud4)
    
    var findProf = executor.find(Professors, Professors.surname eq "Mrozek", Professors.faculty eq "WIMIR")
    println("$findProf - dane profesora")
    println("Jego studenci")
    findProf?.students?.forEach(::println)
    println("Usunięcie profesora - brak Kaskady")
    executor.delete(Professors, Professors.surname eq "Mrozek", Professors.faculty eq "WIMIR")
    
    var orphanList = executor.findOrphans(Students, Professors)
    println("$orphanList - lista sierot")
    val number = executor.slayOrphans(Students, Professors)
    println("$number - ilość usuniętych sierot")
    orphanList = executor.findOrphans(Students, Professors)
    println("$orphanList - pusta lista po usunięciu")
    
    println("Zmiana drugiego profesora - Kaskada")
    professor2 = executor.update(Professors, professor2, Professors.surname eq "Kwiatek")
    orphanList = executor.findOrphans(Students, Professors)
    println("$orphanList - brak sierot ze względu na kaskadę") // puste
    var findUpdatedProf =
        executor.find(Professors, Professors.surname eq professor2.surname, Professors.faculty eq professor2.faculty)
    
    println("$findUpdatedProf - odszukany profesor")
    println("Jego studenci:")
    findUpdatedProf?.students?.forEach(::println)
    executor.delete(Professors, Professors.surname eq professor2.surname, Professors.faculty eq professor2.faculty)
    executor.slayOrphans(Students, Professors)
}