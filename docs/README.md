# Database Management System Architecture Overview

## Introduction

This document provides an overview of a comprehensive database management system architecture implemented using modern design patterns and object-oriented principles.

## Diagram

![Database Architecture Class Diagram](diagrams/images/diagram-klas.drawio.svg?raw=true)

## Core Components

### Connection Management

- **DatabaseConnectionFactory** (Singleton)
  - Manages database connections centrally
  - Supports multiple connection types
  - Handles connection pooling and lifecycle

### Database Support

- Supports multiple database types:
  - SQLite
  - MySQL
  - PostgreSQL
- Each database type has specialized connection handling and features

### Table Management

#### Table Types

- **Physical Tables**: Represent actual database tables
- **Virtual Tables**: Represent results of table operations
  - Support for complex operations (JOIN, UNION, EXCEPT, INTERSECT)

#### Table Operations

- **Join Operations**: Multiple join types (INNER, LEFT, RIGHT, FULL, CROSS)
- **Set Operations**:
  - Union (UNION, UNION ALL)
  - Except (EXCEPT, EXCEPT ALL)
  - Intersect (INTERSECT, INTERSECT ALL)

### Schema Components

- **Columns**

  - Strong typing support via generics
  - Multiple data types (INTEGER, VARCHAR, BOOLEAN, etc.)
  - Constraints and validation

- **Constraints**

  - Check constraints
  - Unique constraints
  - Foreign key constraints with referential actions

- **Indices**
  - Support for unique and non-unique indices
  - Performance optimization

## Query Building and Execution System

### Command Pattern Implementation

The system implements database operations through the Command pattern:

- DatabaseCommand base class encapsulates operation execution
- Specialized commands (Select, Insert, Update, Delete, etc.)
- CommandExecutor handles command execution and transactions

### Query Building

Fluent interface for command construction:

- SELECT commands
- INSERT commands
- UPDATE commands
- DELETE commands
- Table creation/modification
- Schema alterations

## Utility Components

### Logging System

- Centralized logging (Singleton pattern)
- Multiple log levels
- Exception handling

### Metadata Management

- Table scanning capabilities
- Schema information extraction
- Database metadata access

## Design Patterns Used

1. **Singleton Pattern**
   - DatabaseConnectionFactory
   - Logger
2. **Factory Pattern**
   - ConnectionFactory
   - Database-specific connections
3. **Builder Pattern**
   - Query construction system
4. **Command Pattern**
   - Database operations encapsulation
   - Unified command execution

## Error Detection

- Comprehensive exception handling
- Logging integration
