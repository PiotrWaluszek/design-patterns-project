package ormapping

import ormapping.entity.Entity
import ormapping.table.CascadeType
import ormapping.table.Table

data class Student(
    val id: Int,
    val name: String,
    val email: String,
) : Entity

data class Locker(
    val id: Int,
    val number: String,
    val floor: Int,
) : Entity

data class Class(
    val id: Int,
    val name: String,
    val year: Int,
) : Entity

data class Grade(
    val id: Int,
    val value: Int,
    val subject: String,
    val studentId: Int,
) : Entity

data class Subject(
    val id: Int,
    val name: String,
    val teacher: String,
) : Entity

class StudentTable : Table<Student>("students", Student::class) {
    // Kolumny podstawowe
    val id = integer("id")
    val name = varchar("name", 100)
    val email = varchar("email", 100)
    
    init {
        id.primaryKey()
        
        oneToOne(
            target = LockerTable(),
            cascade = CascadeType.NONE
        )
        
        manyToOne(
            target = ClassTable(),
            cascade = CascadeType.NONE
        )
        
        oneToMany(
            target = GradeTable(),
            cascade = CascadeType.ALL
        )
        
        manyToMany(
            target = SubjectTable(),
            cascade = CascadeType.NONE
        )
    }
}

class LockerTable : Table<Locker>("lockers", Locker::class) {
    val id = integer("id")
    val number = varchar("number", 10)
    val floor = integer("floor")
    
    init {
        id.primaryKey()
        
        // Druga strona relacji ONE-TO-ONE ze StudentTable
        oneToOne(
            target = StudentTable(),
            cascade = CascadeType.NONE
        )
    }
}

class ClassTable : Table<Class>("classes", Class::class) {
    val id = integer("id")
    val name = varchar("name", 50)
    val year = integer("year")
    
    init {
        id.primaryKey()
        
        // Druga strona relacji MANY-TO-ONE ze StudentTable
        // (z perspektywy klasy to jest ONE-TO-MANY)
        oneToMany(
            target = StudentTable(),
            cascade = CascadeType.NONE
        )
    }
}

class GradeTable : Table<Grade>("grades", Grade::class) {
    val id = integer("id")
    val value = integer("value")
    val subject = varchar("subject", 50)
    val studentId = integer("student_id")
    
    init {
        id.primaryKey()
        
        // Druga strona relacji ONE-TO-MANY ze StudentTable
        // (z perspektywy oceny to jest MANY-TO-ONE)
        manyToOne(
            target = StudentTable(),
            cascade = CascadeType.NONE
        )
    }
}

class SubjectTable : Table<Subject>("subjects", Subject::class) {
    val id = integer("id")
    val name = varchar("name", 100)
    val teacher = varchar("teacher", 100)
    
    init {
        id.primaryKey()
        
        // Druga strona relacji MANY-TO-MANY ze StudentTable
        manyToMany(
            target = StudentTable(),
            cascade = CascadeType.NONE
        )
    }
}