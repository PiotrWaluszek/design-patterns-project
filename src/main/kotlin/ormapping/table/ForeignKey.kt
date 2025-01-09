package ormapping.table

data class ForeignKey(
    val targetTable: String,
    val targetColumns: List<String>,
    val cascade: CascadeType = CascadeType.NONE,
) {
    
    //for now
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