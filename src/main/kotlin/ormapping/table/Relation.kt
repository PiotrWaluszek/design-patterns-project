package ormapping.table

import ormapping.entity.Entity

/**
 * Represents a relationship between two entities in the ORM framework.
 *
 * @param R The type of the related entity.
 * @property type The type of the relationship (e.g., ONE_TO_MANY, MANY_TO_ONE).
 * @property targetTable The table of the related entity.
 * @property cascade The cascade type defining how changes in the parent entity affect the related entity.
 * @property joinTableName The name of the join table for many-to-many relationships (optional).
 */
class Relation<R : Entity>(
    val type: RelationType,
    val targetTable: Table<R>,
    val cascade: CascadeType = CascadeType.NONE,
    val joinTableName: String? = null,
) {
    /**
     * The foreign key constraint representing the relationship.
     */
    lateinit var foreignKey: ForeignKey
}