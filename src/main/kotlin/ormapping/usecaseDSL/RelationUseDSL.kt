import ormapping.Employees
import ormapping.command.CommandExecutor
import ormapping.command.MultiDestinationLogger
import ormapping.connection.DatabaseConfig
import ormapping.connection.DatabaseConnection
import ormapping.connection.ProviderType
import ormapping.connection.SQLiteConnection
import ormapping.entity.Entity
import ormapping.table.*
import ormapping.sql.CreateTableBuilder
import java.time.LocalDate

data class Course(
    val courseNumber: Int,
    val name: String,
    val credits: Int,
    val department: Department? = null,
) : Entity(CourseTable)

data class Department(
    val code: String,
    val name: String,
    val building: String,
    val courses: MutableSet<Course> = mutableSetOf(),
    val professors: MutableSet<Professor> = mutableSetOf(),
) : Entity(DepartmentTable)

data class Student(
    val id: Int,
    val name: String,
    val enrollmentDate: LocalDate,
    val major: Department? = null,
    val academicAdvisor: Professor? = null,
) : Entity(StudentTable)

data class Professor(
    val id: Int,
    val name: String,
    val department: Department? = null,
    val advisees: MutableSet<Student> = mutableSetOf(),
) : Entity(ProfessorTable)

object CourseTable : Table<Course>("course", Course::class) {
    
    val courseNumber = integer("course_number").primaryKey()
    
    val name = varchar("name", 100)
    val credits = integer("credits")
    
    
}

object DepartmentTable : Table<Department>("department", Department::class) {
    val code = varchar("code", 10).primaryKey()
    val name = varchar("name", 100)
    val building = varchar("building", 50)
    
}

object StudentTable : Table<Student>("student", Student::class) {
    val id = integer("id").primaryKey()
    val name = varchar("name", 100)
    val enrollmentDate = date("enrollment_date")
    
}

object ProfessorTable : Table<Professor>("professor", Professor::class) {
    val id = integer("id").primaryKey()
    val name = varchar("name", 100)
    
}

fun setupAllRelations(
    departmentTable: DepartmentTable,
    courseTable: CourseTable,
    studentTable: StudentTable,
    professorTable: ProfessorTable,
) {
    // Department ↔ Course
    departmentTable.oneToMany(courseTable, CascadeType.ALL)
    courseTable.manyToOne(departmentTable, CascadeType.NONE)
    
    // Department ↔ Professor
    departmentTable.oneToMany(professorTable, CascadeType.NONE)
    professorTable.manyToOne(departmentTable, CascadeType.NONE)
    
    
    // Student ↔ Department
    studentTable.manyToOne(departmentTable, CascadeType.NONE)
    
    // Student ↔ Professor
    studentTable.manyToOne(professorTable, CascadeType.NONE)
    professorTable.oneToMany(studentTable, CascadeType.NONE)
    
}

fun main() {
    val config = DatabaseConfig(
        type = ProviderType.SQLITE,
        url = "jdbc:sqlite:orm.db"
    )
    val connection = DatabaseConnection.createConnection(config)
    val logger = MultiDestinationLogger.getInstance()
    val dest = "./logs/log3.log"
    logger.initializeLogDestination(dest)
    val executor = CommandExecutor(connection, logger, dest)
    
    
    val departmentTable = DepartmentTable
    val courseTable = CourseTable
    val studentTable = StudentTable
    val professorTable = ProfessorTable
    
    setupAllRelations(departmentTable, courseTable, studentTable, professorTable)
    
    println("\n=== Creating tables ===")
    try {
        listOf(departmentTable, courseTable, studentTable, professorTable).forEach { tbl ->
            val createSQL = CreateTableBuilder().fromTable(tbl)
            println("SQL for table [${tbl._name}]:\n${createSQL.build()}\n")
            executor.executeSQL(createSQL)
            
        }
        println("All tables created successfully.\n")
    } catch (e: Exception) {
        println("Error creating tables: ${e.message}")
    }
}