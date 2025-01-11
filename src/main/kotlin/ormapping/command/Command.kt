// Command.kt
package ormapping.command

import ormapping.connection.DatabaseConnection

/**
 * Bazowa klasa abstrakcyjna dla wzorca Command
 * Wszystkie konkretne komendy muszą dziedziczyć po tej klasie
 */
abstract class Command {
    /**
     * Metoda wykonująca komendę
     * @param connection Połączenie z bazą danych
     */
    abstract fun execute(connection: DatabaseConnection)
}