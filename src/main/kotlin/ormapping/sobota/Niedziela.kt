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
//object Husbands : Table<Husband>("husbands", Husband::class) {
//    var name = varchar("name", 255)
//    var surname = varchar("surname", 255)
//
//    init {
//        surname.primaryKey()
//        oneToOne(Wives, CascadeType.ALL)
//    }
//}
//
//object Wives : Table<Wife>("wives", Wife::class) {
//    var name = varchar("name", 255)
//    var surname = varchar("surname", 255)
//
//    init {
//        surname.primaryKey()
//        oneToOne(Husbands, CascadeType.ALL)
//    }
//}
//
//data class Husband(
//    var surname: String,
//    var name: String,
//    var wife: Wife? = null,
//) : Entity(Husbands)
//
//data class Wife(
//    var surname: String,
//    var name: String,
//    var husband: Husband? = null,
//) : Entity(Wives)
//
//// Przykład użycia:
//fun main() {
//    val config = DatabaseConfig(
//        url = "jdbc:sqlite:ormSobota.db"
//    )
//    val connection = SQLiteConnection.create(config)
//    val entityExecutor = EntityExecutor(connection)
//    // Stwórzmy szczęśliwą parę
//    val wife = Wife("Kowalska", "Anna")
//    val husband = Husband("Kowalski", "Jan", wife)
//    wife.husband = husband
//    // Zapisujemy do bazy
//    entityExecutor.persist(Husbands, husband)
//    entityExecutor.persist(Wives, wife)
//
//    // Znajdujemy męża
//    val foundHusband = entityExecutor.find(Husbands, Husbands.surname eq "Kowalski")
//    println(foundHusband?.wife?.name)  // Powinno wydrukować "Anna"
//    println(foundHusband)
//
//    // Rozwód 💔 (usunie też żonę przez CascadeType.ALL)
//    entityExecutor.delete(Husbands, Husbands.surname eq "Kowalski")
//}