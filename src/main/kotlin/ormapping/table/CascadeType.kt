package ormapping.table

/**
 * Enum representing the cascade types for entity relationships in the ORM framework.
 * Specifies how related entities should be affected by operations on a parent entity.
 */
enum class CascadeType {
    /**
     * Apply all cascading operations (update, delete, etc.).
     */
    ALL,

    /**
     * Do not apply any cascading operations.
     */
    NONE,

    /**
     * Apply cascading updates to related entities.
     */
    UPDATE,

    /**
     * Apply cascading deletes to related entities.
     */
    DELETE
}
