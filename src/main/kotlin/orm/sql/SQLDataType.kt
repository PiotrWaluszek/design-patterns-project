package orm.sql

enum class SQLDataType {
    INTEGER,
    VARCHAR,
    BOOLEAN,
    DECIMAL,
    TEXT;
    
    fun toSqlString(): String = when (this) {
        INTEGER -> "INTEGER"
        VARCHAR -> "TEXT"
        BOOLEAN -> "INTEGER"  // zalezy od bazy danych? - sqllite jako integer
        DECIMAL -> "DECIMAL"
        TEXT -> "TEXT"
    }
}