# Database Management System Architecture Overview

## Introduction
This document provides an overview of a comprehensive database management system architecture implemented using modern design patterns and object-oriented principles.

## Diagram
![Database Architecture Class Diagram](diagrams/images/diagramklas.drawio.svg?raw=true)

## Core Components

### Connection Management
- **DatabaseConnectionFactory** (Singleton)
  - Manages database connections centrally
  - Supports multiple connection types
  - Handles connection pooling and lifecycle

### Database Support
- Supports multiple database types:
  - SQLite
  - PostgreSQL
  - MySQL
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

## Query Building System

### Builder Pattern Implementation
Fluent interface for query construction:
- SELECT queries
- INSERT operations
- UPDATE operations
- DELETE operations
- Table creation/modification
- Schema alterations

### Query Strategy Pattern
Each query type implements specific execution strategy:
- Query building
- Execution handling
- Result processing

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
   
4. **Strategy Pattern**
   - Query execution strategies

## Error Detection
- Comprehensive exception handling
- Logging integration
