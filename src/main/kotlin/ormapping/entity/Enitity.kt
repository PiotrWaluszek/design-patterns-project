package ormapping.entity

import ormapping.table.Table

/**
 * Abstract base class for representing entities in the ORM framework.
 * Each entity is associated with a specific table in the database.
 *
 * @property table The table definition associated with the entity.
 */
abstract class Entity(val table: Table<*>)
