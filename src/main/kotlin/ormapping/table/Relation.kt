package ormapping.table

import ormapping.entity.Entity

class Relation<R : Entity>(
    val type: RelationType,
    val targetTable: Table<R>,
    val cascade: CascadeType = CascadeType.NONE,
    val joinTableName: String? = null,
) {
    lateinit var foreignKey: ForeignKey
}