//package ormapping.sobota
//
//import ormapping.command.EntityExecutor
//import ormapping.connection.DatabaseConfig
//import ormapping.connection.SQLiteConnection
//import ormapping.entity.Entity
//import ormapping.table.CascadeType
//import ormapping.table.Table
//import ormapping.table.eq
//
////------------------ ONE TO MANY --------------------
//data class Professor(
//    var surname: String,
//    var faculty: String,
//    var students: MutableSet<Student>? = mutableSetOf<Student>(),
//) : Entity(Professors) {
//    override fun toString(): String {
//        return "$surname $faculty"
//    }
//}
//
//data class Student(
//    var surname: String,
//    var student_index: Int,
//    var professor: Professor? = null,
//) : Entity(Students) {
//    override fun toString(): String {
//        return "$surname $student_index"
//    }
//}
//
//object Professors : Table<Professor>("professors", Professor::class) {
//    var faculty = varchar("faculty", 255)
//    var surname = varchar("surname", 255)
//
//
//    init {
//        surname.primaryKey()
//        faculty.primaryKey()
//        oneToMany(Students, cascade = CascadeType.ALL)
//    }
//}
//
//object Students : Table<Student>("students", Student::class) {
//    var surname = varchar("surname", 255)
//    var student_index = integer("student_index")
//
//    init {
//        surname.primaryKey()
//        student_index.primaryKey()
//        manyToOne(Professors)
//    }
//
//}
//
//fun `one to many and many to one`(executor: EntityExecutor) {
//    var professor1 = Professor("Mrozek", "WIMIR")
//    var professor2 = Professor("Nowak", "WEAIIB")
//    var professor3 = Professor(
//        "Kwiatek",
//        "WZ",
//    )
//    //executor.persist(Professors, professor1)
//
//    var stud1 = Student("Aaa", 111, professor1)
//    var stud2 = Student("Baa", 222, professor1)
//    //executor.persist(Students, stud1, stud2)
//
//    var findProf = executor.find(Professors, Professors.surname eq "Mrozek", Professors.faculty eq "WIMIR")
//    println(findProf)
//    println("lololol")
//    findProf?.students?.forEach(::println)
//    executor.delete(Professors, Professors.surname eq "Mrozek", Professors.faculty eq "WIMIR")
////    var orpanList = executor.findOrphans(Students, Professors)
////    println(orpanList)
////    val number = executor.slayOrphans(Students, Professors)
////    println(number)
////    professor1 = executor.update(Professors, professor1, Professors.surname eq "Mrozek")
////    println(professor1)
////    var orpanList = executor.findOrphans(Students, Professors)
////    println(orpanList)
//
//    //
//    //var stud3 = Student("Caa", 333)
//    //var stud4 = Student("Daa", 444)
////    executor.delete(Professors, Professors.surname eq "Mrozek", Professors.faculty eq "WIMIR")
////    val number = executor.slayOrphans(Students, Professors)
////    println(number)
//
//
//    //executor.update(Students, stud2)
//
//}
//
//fun main() {
//    val config = DatabaseConfig(
//        url = "jdbc:sqlite:ormSobota.db"
//    )
//    val connection = SQLiteConnection.create(config)
//    val executor = EntityExecutor(connection)
//    `one to many and many to one`(executor)
//}
//
//
////data class Employee(
////    var id: Int,
////    var name: String,
////    var surname: String,
////    var height: Int,
////) : Entity
////
////object Employees : Table<Employee>("employees", Employee::class) {
////    var id = integer("id").primaryKey()
////    var name = varchar("name", 255).primaryKey()
////    var surname = varchar("surname", 255).primaryKey()
////    var height = integer("height")
////
////}
////
////fun noRelations(executor: CommandExecutor) {
////    executor.delete(Employees, Employees.id eq 1, Employees.name eq "Jan", Employees.surname eq "Kowalski")
////    executor.delete(Employees, Employees.id eq 2, Employees.name eq "Anna", Employees.surname eq "Nowak")
////    executor.delete(Employees, Employees.id eq 1, Employees.name eq "Dzban", Employees.surname eq "Kowalski")
////
////    val emp1 = Employee(1, "Jan", "Kowalski", 160)
////    val emp2 = Employee(2, "Anna", "Nowak", 150)
////    executor.persist(Employees, emp1, emp2)
////    var findFirst =
////        executor.find(Employees, Employees.id eq 1, Employees.name eq "Jan", Employees.surname eq "Kowalski")
////    println(findFirst)
////    findFirst?.let {
////        executor.update(Employees, it, Employees.id eq 1, Employees.name eq "Jan", Employees.surname eq "Kowalski")
////    }
////    var findDzban =
////        executor.find(Employees, Employees.id eq 1, Employees.name eq "Dzban", Employees.surname eq "Kowalski")
////    println(findDzban)
////    executor.delete(Employees, Employees.id eq 1, Employees.name eq "Jan", Employees.surname eq "Kowalski")
////    executor.delete(Employees, Employees.id eq 2, Employees.name eq "Anna", Employees.surname eq "Nowak")
////    executor.delete(Employees, Employees.id eq 1, Employees.name eq "Dzban", Employees.surname eq "Kowalski")
////}
//