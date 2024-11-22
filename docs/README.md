### Documentation for the ORM Diagram

Below is detailed documentation for the Object Relational Mapper (ORM) diagram, designed with patterns like Singleton, Factory, Builder, and Strategy. The diagram is displayed below:

![ORM Diagram](diagrams/images/classesdiagramORM.svg)

---

### Overview

The diagram represents an ORM structure that handles database connections, SQL query execution, and object mapping for database-stored data. Leveraging design patterns ensures that the architecture is flexible, modular, and maintainable.

---

### Core Components

#### 1. **DatabaseConnectionFactory**
- **Purpose**: Manages the creation and caching of database connections.
- **Design Pattern**: Singleton
  - Ensures only one instance of the connection factory exists.
- **Responsibilities**:
  - Provide global access to the factory via `getInstance()`.
  - Create and return connections based on their type (e.g., SQLite, MySQL, Postgres).
  - Maintain a map of connections for reuse.

---

#### 2. **Logger**
- **Purpose**: Centralized logging system for the application.
- **Design Pattern**: Singleton
  - Guarantees a single logger instance.
- **Responsibilities**:
  - Log application messages (`log`, `debug`, `error`).
  - Manage a log file to store log data.

---

#### 3. **ConnectionFactory**
- **Purpose**: Interface for creating database connections.
- **Design Pattern**: Factory
  - Abstracts the process of creating different types of connections.
- **Responsibilities**:
  - Define a contract for implementing classes to handle connection creation.

---

#### 4. **DatabaseConnection** (Abstract Class)
- **Purpose**: Base class for all specific database connection types.
- **Responsibilities**:
  - Provide common methods for connecting, disconnecting, and executing queries.
  - Delegate actual connection creation to subclasses.

---

#### 5. **Database Connection Subclasses**
- **SQLiteConnection**
- **PostgresConnection**
- **MySQLConnection**

- **Purpose**: Specialized implementations for different databases.
- **Responsibilities**:
  - Implement the `createConnection` method to establish connections with a specific database.

---

#### 6. **EntityManager**
- **Purpose**: High-level class for managing database entities.
- **Responsibilities**:
  - Perform CRUD (Create, Read, Update, Delete) operations on entities.
  - Use a `QueryStrategy` for executing database queries.
  - Utilize `EntityMapper` for mapping database results to objects and vice versa.

---

#### 7. **EntityMapper**
- **Purpose**: Abstraction for mapping data between entities and the database.
- **Responsibilities**:
  - Convert database results (`ResultSet`) into entity objects.
  - Convert entities into database-compatible formats.

---

#### 8. **QueryBuilder**
- **Purpose**: Fluent interface for constructing database queries.
- **Design Pattern**: Builder
  - Simplifies the creation of complex SQL queries.
- **Responsibilities**:
  - Generate SQL queries for `SELECT`, `INSERT`, `UPDATE`, and `DELETE` operations.
  - Return corresponding `QueryStrategy` objects.

---

#### 9. **Query Builders**
- **SelectQueryBuilder**
- **InsertQueryBuilder**
- **UpdateQueryBuilder**
- **DeleteQueryBuilder**

- **Purpose**: Specialized classes for constructing specific types of SQL queries.
- **Responsibilities**:
  - Gather parameters like table names, columns, and conditions.
  - Use the `build` method to return a `QueryStrategy` object.

---

#### 10. **QueryStrategy**
- **Purpose**: Abstraction for various query execution strategies.
- **Design Pattern**: Strategy
  - Allows for dynamic switching of query execution logic.
- **Responsibilities**:
  - Build SQL query strings.
  - Execute queries and return results.

---

#### 11. **Query Strategies**
- **SelectQueryStrategy**
- **InsertQueryStrategy**
- **UpdateQueryStrategy**
- **DeleteQueryStrategy**

- **Purpose**: Define concrete strategies for executing specific query types.
- **Responsibilities**:
  - Construct and execute SQL commands based on provided input.

---

### Design Patterns in Use

1. **Singleton**
   - Used in `DatabaseConnectionFactory` and `Logger` to ensure a single point of access and avoid redundancy.

2. **Factory**
   - Encapsulates the creation of database connections in the `ConnectionFactory`.

3. **Builder**
   - Simplifies the construction of complex SQL queries through the `QueryBuilder` and its subclasses.

4. **Strategy**
   - Provides flexibility in query execution with `QueryStrategy` and its implementations.

---

### Conclusion

This ORM architecture is modular and extensible, enabling the addition of new database types or query features with minimal changes to the existing code. The diagram and its elements detail the responsibilities of classes and the design patterns applied in the system.