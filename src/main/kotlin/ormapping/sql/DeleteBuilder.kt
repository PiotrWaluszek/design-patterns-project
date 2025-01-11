// Zmodyfikowany DeleteBuilder
package ormapping.sql

class DeleteBuilder : SQLBuilder {
    private var table: String = ""
    private val conditions = mutableListOf<String>()
    private var cascade = false

    fun from(table: String): DeleteBuilder {
        this.table = table
        return this
    }

    fun where(condition: String): DeleteBuilder {
        conditions.add(condition)
        return this
    }

    fun cascade(): DeleteBuilder {
        cascade = true
        return this
    }

    override fun build(): String = buildString {
        append("DELETE FROM $table")
        if (conditions.isNotEmpty()) {
            append(" WHERE ")
            append(conditions.joinToString(" AND "))
        }
        if (cascade) append(" CASCADE")
    }
}

