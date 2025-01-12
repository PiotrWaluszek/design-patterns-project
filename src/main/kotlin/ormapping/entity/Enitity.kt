package ormapping.entity


import ormapping.table.Table


abstract class Entity(val table: Table<*>)
