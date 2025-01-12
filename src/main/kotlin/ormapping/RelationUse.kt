import ormapping.Employees
import ormapping.command.CommandExecutor
import ormapping.connection.DatabaseConfig
import ormapping.connection.DatabaseConnection
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
    val prerequisites: MutableSet<Course> = mutableSetOf(),
    val enrolledStudents: MutableSet<Student> = mutableSetOf(),
) : Entity

data class Department(
    val code: String,
    val name: String,
    val building: String,
    val courses: MutableSet<Course> = mutableSetOf(),
    val professors: MutableSet<Professor> = mutableSetOf(),
) : Entity

data class Student(
    val id: Int,
    val name: String,
    val enrollmentDate: LocalDate,
    val major: Department? = null,
    val enrolledCourses: MutableSet<Course> = mutableSetOf(),
    val academicAdvisor: Professor? = null,
) : Entity

data class Professor(
    val id: Int,
    val name: String,
    val department: Department? = null,
    val advisees: MutableSet<Student> = mutableSetOf(),
    val taughtCourses: MutableSet<Course> = mutableSetOf(),
) : Entity

class CourseTable : Table<Course>("course", Course::class) {

    val courseNumber = integer("course_number")

    val name = varchar("name", 100)
    val credits = integer("credits")


}

class DepartmentTable : Table<Department>("department", Department::class) {
    val code = varchar("code", 10).primaryKey()
    val name = varchar("name", 100)
    val building = varchar("building", 50)

}

class StudentTable : Table<Student>("student", Student::class) {
    val id = integer("id").primaryKey()
    val name = varchar("name", 100)
    val enrollmentDate = date("enrollment_date")

}

class ProfessorTable : Table<Professor>("professor", Professor::class) {
    val id = integer("id").primaryKey()
    val name = varchar("name", 100)

}

fun setupAllRelations(
    departmentTable: DepartmentTable,
    courseTable: CourseTable,
    studentTable: StudentTable,
    professorTable: ProfessorTable
) {
    // Department ↔ Course
    departmentTable.oneToMany(courseTable, CascadeType.ALL)
    courseTable.manyToOne(departmentTable, CascadeType.NONE)

    // Department ↔ Professor
    departmentTable.oneToMany(professorTable, CascadeType.NONE)
    professorTable.manyToOne(departmentTable, CascadeType.NONE)

    // Course ↔ Course (prerequisites)
    courseTable.manyToMany(courseTable, CascadeType.NONE)

    // Course ↔ Student
    courseTable.manyToMany(studentTable, CascadeType.NONE)
    studentTable.manyToMany(courseTable, CascadeType.NONE)

    // Student ↔ Department
    studentTable.manyToOne(departmentTable, CascadeType.NONE)

    // Student ↔ Professor
    studentTable.manyToOne(professorTable, CascadeType.NONE)
    professorTable.oneToMany(studentTable, CascadeType.NONE)

    // Professor ↔ Course
    professorTable.manyToMany(courseTable, CascadeType.NONE)
    courseTable.manyToMany(professorTable, CascadeType.NONE)
}

fun main() {
    val config = DatabaseConfig(
        url = "jdbc:sqlite:orm.db"
    )
    val connection = SQLiteConnection.create(config)
    val executor = CommandExecutor(connection)



    val departmentTable = DepartmentTable()
    val courseTable = CourseTable()
    val studentTable = StudentTable()
    val professorTable = ProfessorTable()

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

    val csDepartment = Department(
        code = "CS",
        name = "Computer Science",
        building = "Tech Building"
    )
    executor.persist(departmentTable, csDepartment)

    val programmingCourse = Course(
        //departmentCode = "CS",
        courseNumber = 101,
        name = "Introduction to Programming",
        credits = 3,
        department = csDepartment
    )

    val databaseCourse = Course(
        //departmentCode = "CS",
        courseNumber = 301,
        name = "Database Systems",
        credits = 4,
        department = csDepartment,
        prerequisites = mutableSetOf(programmingCourse)
    )

    executor.persist(courseTable, programmingCourse, databaseCourse)

    val professor = Professor(
        id = 1,
        name = "Dr. Smith",
        department = csDepartment
    )
    executor.persist(professorTable, professor)

    val student = Student(
        id = 1,
        name = "John Doe",
        enrollmentDate = LocalDate.now(),
        major = csDepartment,
        academicAdvisor = professor
    )
    executor.persist(studentTable, student)

    student.enrolledCourses.add(programmingCourse)
    executor.update(studentTable, student)

    val foundCourse = executor.find(
        courseTable, mapOf(
            "department_code" to "CS",
            "course_number" to 101
        )
    )

    println("Found course: ${foundCourse?.name}")
    println("Department: ${foundCourse?.department?.name}")
    println("Enrolled students: ${foundCourse?.enrolledStudents?.map { it.name }}")



    executor.delete(departmentTable, "CS")
}