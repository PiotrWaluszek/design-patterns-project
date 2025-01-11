package ormapping.relationuse

import ormapping.command.CommandExecutor
import ormapping.connection.DatabaseConfig
import ormapping.connection.SQLiteConnection
import ormapping.entity.Entity
import ormapping.table.CascadeType
import ormapping.table.Table
import java.time.LocalDate

//-------------------------------------
// 1. Definicje encji (data classes)
//-------------------------------------

data class Course(
    val departmentCode: String,
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

//-------------------------------------
// 2. Definicje tabel (singletony)
//-------------------------------------

/**
 * Tabela "department" dla encji [Department].
 * Zamiast `class` używamy `object`,
 * aby istniała tylko jedna instancja.
 */
object DepartmentTable : Table<Department>("department", Department::class) {
    val code = varchar("code", 10).primaryKey()  // <-- WYWOŁANIE Z NAWIASAMI
    val name = varchar("name", 100)
    val building = varchar("building", 50)
}

/**
 * Tabela "course" dla encji [Course].
 */
object CourseTable : Table<Course>("course", Course::class) {
    val departmentCode = varchar("department_code", 10)
    val courseNumber = integer("course_number")
    val name = varchar("name", 100)
    val credits = integer("credits")
    // Klucz złożony (department_code + course_number) ustawiamy niżej w setupAllRelations()
    // lub możesz go ustawić tu, jeśli wolisz (pamiętając o nawiasach):
    // departmentCode.primaryKey()
    // courseNumber.primaryKey()
}

/**
 * Tabela "student" dla encji [Student].
 */
object StudentTable : Table<Student>("student", Student::class) {
    val id = integer("id").primaryKey() // <-- WYWOŁANIE Z NAWIASAMI
    val name = varchar("name", 100)
    val enrollmentDate = date("enrollment_date")
}

/**
 * Tabela "professor" dla encji [Professor].
 */
object ProfessorTable : Table<Professor>("professor", Professor::class) {
    val id = integer("id").primaryKey()  // <-- WYWOŁANIE Z NAWIASAMI
    val name = varchar("name", 100)
}

//-------------------------------------
// 3. Funkcja do ustawiania relacji
//-------------------------------------
/**
 * Ustawiamy relacje między tabelami (oneToMany, manyToOne, manyToMany)
 * w osobnej funkcji, żeby uniknąć zapętlenia w init{}.
 */
fun setupAllRelations() {
    // Department ↔ Course
    DepartmentTable.oneToMany(CourseTable, CascadeType.ALL)
    // Dla klucza złożonego w Course
    CourseTable.departmentCode.primaryKey()  // <-- WYWOŁANIE Z NAWIASAMI
    CourseTable.courseNumber.primaryKey()    // <-- WYWOŁANIE Z NAWIASAMI
    CourseTable.manyToOne(DepartmentTable, CascadeType.NONE)

    // Department ↔ Professor
    DepartmentTable.oneToMany(ProfessorTable, CascadeType.NONE)
    ProfessorTable.manyToOne(DepartmentTable, CascadeType.NONE)

    // Course ↔ Course (prerequisites) => manyToMany
    CourseTable.manyToMany(CourseTable, CascadeType.NONE)

    // Course ↔ Student => manyToMany
    CourseTable.manyToMany(StudentTable, CascadeType.NONE)
    StudentTable.manyToMany(CourseTable, CascadeType.NONE)

    // Student ↔ Department => manyToOne
    StudentTable.manyToOne(DepartmentTable, CascadeType.NONE)

    // Student ↔ Professor => manyToOne
    StudentTable.manyToOne(ProfessorTable, CascadeType.NONE)
    ProfessorTable.oneToMany(StudentTable, CascadeType.NONE)

    // Professor ↔ Course => manyToMany
    ProfessorTable.manyToMany(CourseTable, CascadeType.NONE)
    CourseTable.manyToMany(ProfessorTable, CascadeType.NONE)
}

//-------------------------------------
// 4. Funkcja main do testowania
//-------------------------------------
fun main() {
    // 1. Tworzymy połączenie
    val connection = SQLiteConnection.create(
        DatabaseConfig(url = "jdbc:sqlite:orm.db")
    )
    val executor = CommandExecutor(connection)

    // 2. Najpierw ustawiamy relacje
    setupAllRelations()

    println("\n=== Test Relacji: Tworzenie Tabel (o ile nie istnieją) ===")
    try {
        val createDepartment = executor.createTable().fromTable(DepartmentTable)
        executor.executeSQL(createDepartment)

        val createCourse = executor.createTable().fromTable(CourseTable)
        executor.executeSQL(createCourse)

        val createStudent = executor.createTable().fromTable(StudentTable)
        executor.executeSQL(createStudent)

        val createProfessor = executor.createTable().fromTable(ProfessorTable)
        executor.executeSQL(createProfessor)

        println("Tabele zostały utworzone.")
    } catch (e: Exception) {
        println("Błąd podczas tworzenia tabel: ${e.message}")
    }

    println("\n=== Test Relacji: Wstawianie przykładowych danych ===")
    try {
        // 1. Tworzymy Department
        val csDepartment = Department(
            code = "CS",
            name = "Computer Science",
            building = "Tech Building"
        )
        val mathDepartment = Department(
            code = "MATH",
            name = "Mathematics",
            building = "Science Hall"
        )

        executor.persist(DepartmentTable, csDepartment, mathDepartment)
        println("Wstawiono Department: CS, MATH")

        // 2. Tworzymy kursy w Departamencie CS, z relacją many-to-many (prerequisites)
        val programmingCourse = Course(
            departmentCode = "CS",
            courseNumber = 101,
            name = "Introduction to Programming",
            credits = 3,
            department = csDepartment
        )
        val databaseCourse = Course(
            departmentCode = "CS",
            courseNumber = 301,
            name = "Database Systems",
            credits = 4,
            department = csDepartment,
            prerequisites = mutableSetOf(programmingCourse)
        )

        executor.persist(CourseTable, programmingCourse, databaseCourse)
        println("Wstawiono Course: Programming (101), Database (301)")

        // 3. Profesor w departamencie CS
        val professor = Professor(
            id = 1,
            name = "Dr. Smith",
            department = csDepartment
        )
        executor.persist(ProfessorTable, professor)
        println("Wstawiono Professor: Dr. Smith")

        // 4. Student zapisany na kurs
        val student = Student(
            id = 100,
            name = "John Doe",
            enrollmentDate = LocalDate.now(),
            major = csDepartment,
            academicAdvisor = professor
        )
        executor.persist(StudentTable, student)
        println("Wstawiono Student: John Doe (id=100)")

        // 5. Dodajemy studentowi kurs (relacja many-to-many)
        student.enrolledCourses.add(programmingCourse)
        executor.update(StudentTable, student)
        println("Student John Doe zapisany na kurs Introduction to Programming")

        // 6. Odczytujemy kurs Programowanie
        val foundCourse = executor.find(
            CourseTable,
            mapOf(
                "department_code" to "CS",
                "course_number" to 101
            )
        )
        println("\n--- Odczytany Course(101) ---")
        println("Nazwa: ${foundCourse?.name}")
        println("Departament: ${foundCourse?.department?.name}")
        println("Zapisani studenci: ${foundCourse?.enrolledStudents?.map { it.name }}")
        println("Prerequisites: ${foundCourse?.prerequisites?.map { it.name }}")

    } catch (e: Exception) {
        println("Błąd w Test Relacji: ${e.message}")
        e.printStackTrace()
    }

    println("\n=== Test Relacji: Usuwanie Departamentu CS ===")
    try {
        // Jeśli w definicjach relacji jest CascadeType.ALL,
        // to usunięcie departamentu 'CS' pociągnie za sobą
        // usunięcie powiązanych kursów, profesorów, studentów itp.
        val deletedCS = executor.delete(DepartmentTable, "CS")
        println("Usunięto Department CS? -> $deletedCS")

    } catch (e: Exception) {
        println("Błąd podczas usuwania Departamentu CS: ${e.message}")
    }

    connection.close()
    println("\nPołączenie z bazą zamknięte.")
}
