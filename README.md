### Documentation for the Database ORM Diagram

This documentation outlines the components of the Object Relational Mapper (ORM) system illustrated in the provided class diagram. It focuses on the use of design patterns, providing insights into the architecture, responsibility, and interaction of each component. The folder structure for this project aligns with the following structure:

```
docs/
├── diagrams/
│   ├── images/
│   │   ├── classesdiagramORM.svg  # Diagram image file
│   ├── src/
│   │   ├── first.puml             # Source of PlantUML diagram
│   ├── README.md                  # This documentation
```

---

### Overview

This ORM design provides a flexible and maintainable approach to database interaction, leveraging design patterns such as Singleton, Factory, Builder, and Strategy. Each class and interface has a well-defined responsibility that adheres to software engineering best practices.

---

### Core Components

#### 1. **DatabaseConnectionFactory**
- **Purpose**: Manages the creation and caching of database connections.
- **Design Pattern**: Singleton
  - Ensures only one instance of the `DatabaseConnectionFactory` exists.
- **Responsibilities**:
  - Provide global access to the factory via `getInstance()`.
  - Create and return database connections based on their type (e.g., SQLite, MySQL, Postgres).
  - Maintain a map of connections for reuse.

---

#### 2. **Logger**
- **Purpose**: Centralized logging system.
- **Design Pattern**: Singleton
  - Guarantees a single logger instance is used across the application.
- **Responsibilities**:
  - Log application messages (`log`, `debug`, `error`).
  - Manage a log file to persist log data.

---

#### 3. **ConnectionFactory**
- **Purpose**: Interface for creating database connections.
- **Design Pattern**: Factory
  - Abstracts the creation process for different database connections.
- **Responsibilities**:
  - Define a contract for implementing classes to create connections.

---

#### 4. **DatabaseConnection** (Abstract Class)
- **Purpose**: Base class for all specific database connections.
- **Responsibilities**:
  - Provide common methods for connecting, disconnecting, and executing queries.
  - Delegate the creation of actual database connections to subclasses.

---

#### 5. **Database Connection Subclasses**
- **SQLiteConnection**
- **PostgresConnection**
- **MySQLConnection**

- **Purpose**: Specialized implementations for different databases.
- **Responsibilities**:
  - Implement the `createConnection` method to establish a connection with a specific database type.

---

#### 6. **EntityManager**
- **Purpose**: High-level class for managing entities in the database.
- **Responsibilities**:
  - Save, find, update, and delete database entities.
  - Use a `QueryStrategy` to execute database queries.
  - Utilize `EntityMapper` to map database results to entity objects and vice versa.

---

#### 7. **EntityMapper Interface**
- **Purpose**: Abstraction for mapping data between entities and the database.
- **Responsibilities**:
  - Convert database results (`ResultSet`) into entities.
  - Convert entities into database-compatible formats.

---

#### 8. **QueryBuilder**
- **Purpose**: Fluent interface for constructing database queries.
- **Design Pattern**: Builder
  - Provides an intuitive API for query construction.
- **Responsibilities**:
  - Generate SQL queries for `SELECT`, `INSERT`, `UPDATE`, and `DELETE` operations.
  - Return corresponding `QueryStrategy` implementations.

---

#### 9. **Query Builders**
- **SelectQueryBuilder**
- **InsertQueryBuilder**
- **UpdateQueryBuilder**
- **DeleteQueryBuilder**

- **Purpose**: Specialized classes for building specific SQL queries.
- **Responsibilities**:
  - Collect parameters such as table name, columns, and conditions.
  - Use the `build` method to return a `QueryStrategy` object.

---

#### 10. **QueryStrategy Interface**
- **Purpose**: Abstraction for different query execution strategies.
- **Design Pattern**: Strategy
  - Allows swapping query execution logic dynamically.
- **Responsibilities**:
  - Build SQL query strings.
  - Execute queries and return results.

---

#### 11. **Query Strategy Implementations**
- **SelectQueryStrategy**
- **InsertQueryStrategy**
- **UpdateQueryStrategy**
- **DeleteQueryStrategy**

- **Purpose**: Define concrete strategies for executing specific types of queries.
- **Responsibilities**:
  - Construct and execute SQL commands based on the provided input.

---

### Design Patterns in Use

1. **Singleton**
   - Used in `DatabaseConnectionFactory` and `Logger` to ensure a single point of access and avoid redundancy.

2. **Factory**
   - Encapsulates the creation of database connections in the `ConnectionFactory`.

3. **Builder**
   - Simplifies the construction of complex SQL queries with the `QueryBuilder` and its subclasses.

4. **Strategy**
   - Provides flexibility in executing and building queries with `QueryStrategy` and its implementations.

---

### Conclusion

This ORM architecture is modular and extensible, allowing the addition of new database types or query features with minimal changes to existing code. Its reliance on design patterns ensures clarity, reusability, and scalability. For additional details, refer to the [diagram file](diagrams/images/classesdiagramORM.svg).