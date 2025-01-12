package ormapping.command

import ormapping.connection.DatabaseConnection

/**
 * Abstract base class for the Command pattern.
 * All concrete commands must inherit from this class.
 */
abstract class Command {

    /**
     * Executes the command using the provided database connection.
     *
     * @param connection The database connection to be used for executing the command.
     */
    abstract fun execute(connection: DatabaseConnection)
}
