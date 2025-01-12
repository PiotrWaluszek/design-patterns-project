package ormapping.table

/**
 * Represents a foreign key constraint in a database table.
 *
 * @property targetTable The name of the target table referenced by the foreign key.
 * @property targetColumns The list of column names in the target table referenced by the foreign key.
 * @property cascade The cascade type defining the behavior for `DELETE` and `UPDATE` operations.
 */
data class ForeignKey(
    val targetTable: String,
    val targetColumns: List<String>,
    val cascade: CascadeType = CascadeType.NONE,
) {

    /**
     * Converts the foreign key definition to its SQL representation.
     *
     * @param sourceColumn The source column in the current table that references the target table.
     * @return The SQL string defining the foreign key constraint.
     */
    fun toSQL(sourceColumn: String): String {
        val targetColumnsSQL = targetColumns.joinToString(", ")
        val cascadeSQL = when (cascade) {
            CascadeType.ALL -> "ON DELETE CASCADE ON UPDATE CASCADE"
            CascadeType.DELETE -> "ON DELETE CASCADE"
            CascadeType.UPDATE -> "ON UPDATE CASCADE"
            CascadeType.NONE -> ""
        }

        return "FOREIGN KEY ($sourceColumn) REFERENCES $targetTable ($targetColumnsSQL) $cascadeSQL"
    }
}
