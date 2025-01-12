package ormapping.table

/**
 * Enum representing the types of relationships between entities in the ORM framework.
 */
enum class RelationType {
    /**
     * A one-to-one relationship.
     */
    ONE_TO_ONE,

    /**
     * A one-to-many relationship.
     */
    ONE_TO_MANY,

    /**
     * A many-to-one relationship.
     */
    MANY_TO_ONE,

    /**
     * A many-to-many relationship.
     */
    MANY_TO_MANY
}
