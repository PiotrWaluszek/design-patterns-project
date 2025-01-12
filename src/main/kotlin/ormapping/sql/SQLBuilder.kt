package ormapping.sql

/**
 * Interface for building SQL queries dynamically.
 * Classes implementing this interface must define a method to construct
 * and return the final SQL query as a string.
 */
interface SQLBuilder {

    /**
     * Builds and returns the SQL query as a string.
     *
     * @return The constructed SQL query.
     */
    fun build(): String
}
