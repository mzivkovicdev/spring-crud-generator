# 🚀 Spring CRUD Generator

![Java](https://img.shields.io/badge/Java-17--25-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Open Source](https://img.shields.io/badge/Open%20Source-Yes-success)

**Spring CRUD Generator** is an open-source code generation tool designed to accelerate backend development with Spring Boot.  
It generates production-ready CRUD layers, database migrations, API documentation, Docker resources, tests, and more — directly from a single YAML or JSON configuration file.

This project aims to eliminate repetitive boilerplate code while keeping full control and transparency over the generated output.

---

## 🚀 Quick Start

Generate a fully functional Spring Boot CRUD application in minutes using a single YAML or JSON configuration file.

### Step 1: Add Maven Profile

Add the following `profile` section to your **`pom.xml`**:

```xml
<profiles>
    <profile>
        <id>generate-resources</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>com.markozivkovic.codegen</groupId>
                    <artifactId>spring-crud-generator</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <executions>
                        <execution>
                            <id>generate-spring-crud</id>
                            <goals>
                                <goal>generate</goal>
                            </goals>
                            <configuration>
                                <inputSpecFile>
                                    ${project.basedir}/src/main/resources/crud-spec.yaml
                                </inputSpecFile>
                                <outputDir>
                                    ${project.basedir}/src/main/java/com/sql/demo/springboot_postgres_json_demo
                                </outputDir>
                                <forceRegeneration>true</forceRegeneration>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### Step 2: Create Configuration File

Create the configuration file at the location defined by inputSpecFile:
```
src/main/resources/crud-spec.yaml
```
```yaml
configuration:
  database: postgresql
  swagger: true
  openApiCodegen: true
  errorResponse: simple
  tests:
    unit: true
    dataGenerator: instancio
entities:
  - name: ProductModel
    storageName: product_table
    description: "Represents a product"
    fields:
      - name: id
        type: Long
        description: "The unique identifier for the product"
        id:
          strategy: TABLE
      - name: name
        description: "The name of the product"
        type: String
        column:
          nullable: false
          updateable: true
          unique: true
          length: 10000
```

This configuration file defines:
- global generator configuration
- database and migration settings
- entities and relationships
- caching, tests, and API generation
- Both YAML and JSON formats are supported.

### Step 3: Run the Generator

Run the generator using the Maven profile:
```
mvn clean install -Pgenerate-resources
```
After execution:
- source code is generated into the directory defined by outputDir
- Flyway migration scripts are generated (if enabled)
- Swagger/OpenAPI resources are generated (if enabled)
- Docker and Docker Compose files are generated (if enabled)

### Step 4: Configuration Parameters

| Parameter           | Description                                                           |
| ------------------- | --------------------------------------------------------------------- |
| `inputSpecFile`     | Path to the YAML/JSON configuration file                              |
| `outputDir`         | Directory where generated source code is written                      |
| `forceRegeneration` | Forces regeneration of all non-ignored entities, ignoring state files |

### Important Notes

Incremental generation is enabled by default
- Generator state is stored in: generator-state.json and migration-state.json
- Setting forceRegeneration=true ignores these files
- Database tables are never dropped automatically

## ⚙️ Configuration Reference

This section describes all available configuration options for the generator.  
All options are defined under the root `configuration` key.

---

### `configuration`

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `database` | string | `postgresql` | Target SQL database (e.g. `postgresql`, `mysql`, `mssql`) |
| `javaVersion` | number | `17` | Java version used for generated code and Dockerfile |
| `optimisticLocking` | boolean | `false` | Enables optimistic locking support |
| `swagger` | boolean | `false` | Enables Swagger/OpenAPI specification generation |
| `openApiCodegen` | boolean | `false` | Runs OpenAPI Generator and generates API resources |
| `graphQl` | boolean | `false` | Enables GraphQL query and mutation generation |
| `errorResponse` | string | `simple` | Error response strategy (`simple`, `detailed`, `minimal`, `none`) |
| `migrationScripts` | boolean | `false` | Enables Flyway migration script generation |

---

### `configuration.docker`

Controls Docker and Docker Compose generation.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `dockerfile` | boolean | `false` | Generates a Dockerfile |
| `dockerCompose` | boolean | `false` | Generates a docker-compose.yml |

#### `configuration.docker.app`

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `image` | string | `eclipse-temurin` | Base Docker image for the application |
| `port` | number | `8080` | Exposed application port |
| `tag` | string | `alpine` | Docker image tag |

#### `configuration.docker.db`

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `image` | string | `postgres` | Database Docker image |
| `port` | number | `5432` | Database port |
| `tag` | string | `latest` | Docker image tag |

---

### `configuration.cache`

Enables and configures caching.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `false` | Enables caching |
| `type` | string | `CAFFEINE` | Cache provider (`CAFFEINE`, `REDIS`) |
| `expiration` | number | `5` | Cache expiration time (minutes) |
| `maxSize` | number | `10000` | Maximum cache size |

---

### `configuration.tests`

Controls test generation.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `unit` | boolean | `false` | Enables unit test generation |
| `dataGenerator` | string | `instancio` | Test data generator (`instancio`, `podam`) |

---

### `configuration.additionalProperties`

Advanced and feature-specific configuration options.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `graphql.scalarConfig` | boolean | `false` | Enables custom GraphQL scalar configuration |
| `optimisticLocking.retry.config` | boolean | `false` | Enables retry mechanism for optimistic locking |
| `optimisticLocking.retry.maxAttempts` | number | `null` | Maximum retry attempts |
| `optimisticLocking.backoff.delayMs` | number | `null` | Initial backoff delay (ms) |
| `optimisticLocking.backoff.multiplier` | number | `null` | Backoff multiplier |
| `optimisticLocking.backoff.maxDelayMs` | number | `null` | Maximum backoff delay (ms) |

---

### ⚠️ Java Version & Docker Image Notes

- The `javaVersion` property defines the Java version used for:
  - generated source code
  - the Dockerfile base image version
- The Docker image **tag** is controlled separately via:
  ```yaml
  configuration:
    docker:
      app:
        image: eclipse-temurin
        tag: alpine
  ```
This allows combinations such as:
- Java 21 with ```eclipse-temurin:21-alpine```
- Java 17 with ```eclipse-temurin:17-jre```

## 📦 Packages Configuration

By default, the generator uses a predefined package structure for all generated code.  
If you want full control over package naming and organization, you can override the default structure using the `packages` section.

---

### Default Package Structure

If the `packages` section is **not defined**, the generator uses the following default package structure:

```text
|- annotations
|- businessservices
|- configurations
|- controllers
|- enums
|- exceptions
|- generated
|- mappers
|- models
|- resolvers
|- repositories
|- services
|- transferobjects
```

### Custom Package Structure

You can define a custom package structure by adding the `packages` section to your configuration file.

If the `packages` section is **not provided**, the generator uses the default package structure.

```yaml
packages:
  annotations: annotation
  businessservices: businessservice
  configurations: configuration
  controllers: controller
  enums: enumqwe
  exceptions: exception
  generated: generated
  mappers: mapper
  models: persistance.entity
  resolvers: graphql
  repositories: persistance.repository
  services: persistance.service
  transferobjects: transferobject
```

Each key represents a logical group of generated code, while the value defines the actual package name that will be used.

| Key                | Description                                                    |
| ------------------ | -------------------------------------------------------------- |
| `annotations`      | Custom and generated annotations                               |
| `businessservices` | Business-level service classes                                 |
| `configurations`   | Spring configuration classes                                   |
| `controllers`      | REST controllers                                               |
| `enums`            | Generated enum classes                                         |
| `exceptions`       | Custom and generated exceptions (including exception handlers) |
| `generated`        | Internal/generated support classes (OpenAPI codegen output)    |
| `mappers`          | Entity ↔ DTO mappers                                           |
| `models`           | Persistence models / entities                                  |
| `resolvers`        | GraphQL resolvers                                              |
| `repositories`     | Spring Data repositories                                       |
| `services`         | Service layer classes                                          |
| `transferobjects`  | DTOs / transfer objects                                        |

#### Validation Rules & Notes

If the `packages` section is `not defined`, the generator uses the default package structure.
If the `packages` section is `defined` and `any core package is specified`, then all core packages must be defined.

Core packages:
- `businessservices`
- `controllers`
- `enums`
- `exceptions`
- `mappers`
- `models`
- `repositories`
- `services`
- `transferobjects`
- If `graphQl: true` is enabled, the `resolvers` package is required.
- If `openApiCodegen: true` is enabled, the `generated` package is required.
- If required packages are missing, the generator fails fast with an error explaining which packages are missing.

Partial customization of only core packages is `not supported` to avoid inconsistent package structures.

## 🧱 Entity & Field Schema

Entities are defined under the `entities` section. Each entity describes:
- a generated Java model/entity class
- its database storage representation (table name)
- its fields, constraints, and relationships
- optional metadata used for documentation (Javadoc, OpenAPI descriptions, etc.)

---

### Entity Schema

```yaml
entities:
  - name: ProductModel
    storageName: product_table
    description: "Represents a product"
    fields: []
```
| Property      | Type   | Required       | Description                                                                              |
| ------------- | ------ | -------------- | ---------------------------------------------------------------------------------------- |
| `name`        | string | ✅             | Java class name of the entity/model                                                      |
| `storageName` | string | ✅             | Database table name (or storage name). If omitted, a default naming strategy may be used |
| `description` | string | optional       | Used to generate Javadoc and enrich API docs (where applicable)                          |
| `fields`      | list   | ✅             | List of fields for the entity                                                            |

- Note: If description is provided, the generator can produce Javadoc for entities/fields.

### Field Schema
```yaml
fields:
  - name: id
    type: Long
    description: "Primary key"
    id:
      strategy: IDENTITY
```
| Property      | Type   | Required            | Description                                                                                          |
| ------------- | ------ | ------------------- | ---------------------------------------------------------------------------------------------------- |
| `name`        | string | ✅                  | Java field name                                                                                      |
| `type`        | string | ✅                  | Java type (e.g. `String`, `Long`, `UUID`, `LocalDate`, `Enum`, `JSON<Type>`, or another entity name) |
| `description` | string | optional            | Used for Javadoc and API documentation                                                               |
| `id`          | object | optional            | Marks the field as primary key and defines generation strategy                                       |
| `column`      | object | optional            | Column constraints (unique, nullable, insertable, updateable, length etc.)                           |
| `relation`    | object | optional            | Relationship definition (JPA-style)                                                                  |
| `values`      | list   | required for `Enum` | Enum constant values (only when `type: Enum`)                                                        |

- Supported basic types are: String, Character, Integer, Long, Boolean, Double, Float, Short, Byte, UUID, BigDecimal, BigInteger, LocalDate, LocalDateTime, OffsetDateTime, Instant

### 🔑 Primary Key: id.strategy

To mark a field as the entity identifier, add an id block:

```yaml
- name: id
  type: Long
  description: "The unique identifier for the entity"
  id:
    strategy: IDENTITY
```
Supported strategies (example):
- TABLE
- SEQUENCE (if supported by the selected database)
- UUID
- IDENTIT
- AU

| Property         | Type   | Required | Description                                                         |
| ---------------- | ------ | -------- | ------------------------------------------------------------------- |
| `strategy`       | enum   | ✅       | ID generation strategy                                              |
| `sequenceName`   | string | optional | Name of the database sequence (used with sequence-based strategies) |
| `allocationSize` | number | optional | Allocation size for sequence/table generators                       |
| `initialValue`   | number | optional | Initial value for ID generation                                     |

#### Sequence-Based ID Example
```yaml
- name: id
  type: Long
  id:
    strategy: SEQUENCE
    sequenceName: product_seq
    allocationSize: 10
    initialValue: 1
```
#### Table-Based ID Example
```yaml
- name: id
  type: Long
  id:
    strategy: TABLE
    allocationSize: 5
    initialValue: 1000
```
#### Note
- Not all databases support all ID strategies

### 🧾 Column Constraints: column.*

Use the column block to control column-level constraints:
```yaml
- name: name
  type: String
  column:
    nullable: false
    updateable: true
    unique: true
    length: 255
```
| Property       | Type    | Description                                |
|---------------|---------|--------------------------------------------|
| `nullable`    | boolean | Whether the column can be null             |
| `unique`      | boolean | Whether the column must be unique          |
| `length`      | number  | Column length (primarily for strings)      |
| `insertable`  | boolean | Whether the column can be inserted         |
| `updateable`  | boolean | Whether the column can be updated          |

### 🔗 Relationships: relation.*
Relationships are defined via the relation block. The type must be one of:
- OneToOne
- OneToMany
- ManyToOne
- ManyToMany

#### One-to-One Example
```yaml
- name: product
  type: ProductModel
  relation:
    type: OneToOne
    joinColumn: product_id
    fetch: EAGER
    cascade: MERGE
```

#### One-to-Many Example
```yaml
- name: users
  type: UserEntity
  relation:
    type: OneToMany
    joinColumn: product_id
    fetch: LAZY
    cascade: MERGE
```


#### Many-to-Many Example
```yaml
- name: users
  type: UserEntity
  relation:
    type: ManyToMany
    fetch: LAZY
    cascade: MERGE
    joinTable:
      name: order_user_table
      joinColumn: order_id
      inverseJoinColumn: user_id
```
| Property     | Type   | Description                                                                  |
| ------------ | ------ | ---------------------------------------------------------------------------- |
| `type`       | string | Relationship type (`OneToOne`, `OneToMany`, `ManyToOne`, `ManyToMany`)       |
| `fetch`      | string | Fetch type (`EAGER`, `LAZY`)                                                 |
| `cascade`    | string | Cascade type (e.g. `MERGE`, `ALL`, `PERSIST`, etc.)                          |
| `joinColumn` | string | Join column name (for `OneToOne`, `OneToMany`, `ManyToOne` where applicable) |
| `joinTable`  | object | Join table config (used in `ManyToMany`)                                     |

### 🧩 Enums: type: Enum + values

To define an enum field:
```yaml
- name: status
  type: Enum
  description: "The status of the product"
  values:
    - ACTIVE
    - INACTIVE
```
- `values` is required when type is Enum.

### 🧷 JSON Fields: JSON<Type>

JSON fields are declared using the `JSON<Type>` syntax:
```yaml
- name: details
  type: JSON<Details>
```
Where Details is another entity-like schema definition (often used as an embedded structure):
```yaml
- name: Details
  description: "Represents a user details"
  fields:
    - name: firstName
      type: String
    - name: lastName
      type: String
```
- JSON types are useful for storing structured data inside a single database column (database support and mapping strategy depend on the selected SQL database).

### 🚫 Ignoring an Entity

To skip generation for a specific entity:
```yaml
- name: SomeEntity
  ignore: true
  fields: []
```

## ✨ Key Features

### 🏗️ Code Generation
- Entity models (JPA/Hibernate)
- Repositories
- Services
- Business services
- Mappers
- REST Controllers
- GraphQL Queries & Mutations
- Custom annotations
- Javadoc (optional, based on descriptions)

### 🗄️ Database & Migrations
- SQL database support (MySQL, PostgreSQL and Microsoft SQL)
- Flyway migration script generation
- Automatic detection of:
  - new entities
  - new fields
  - removed fields
- Safe schema evolution  
  ⚠️ **Table deletion is NOT supported** (must be done manually)

### 📦 API & Documentation
- Swagger / OpenAPI specification generation
- Integrated OpenAPI Generator
- Automatic API resource generation

### 🧪 Testing & Data Generation
- Unit test generation
- Test data generation using:
  - **Instancio**
  - **Podam**

### 🐳 DevOps & Containers
- Dockerfile generation
- Docker Compose generation

### ⚡ Caching
- Supported cache providers:
  - **Redis**
  - **Caffeine**
- Configurable expiration and size limits

### 🔒 Optimistic Locking
- Built-in optimistic locking support
- Optional custom annotations
- Retry & backoff configuration support

### 📄 Configuration Formats
- YAML
- JSON

---

## ☕ Supported Java Versions

- **Java 17 – Java 25**

---

## 📁 Default & Custom Package Structure

If the `packages` section is **not defined**, a default package structure is used.

If you want full control, you can define your own package layout.

---

## 🔄 Incremental Generation & State Files

The generator produces two internal state files:

### `generator-state.json`
Tracks generated entities using fingerprints to avoid unnecessary regeneration.

### `migration-state.json`
Tracks schema changes to safely generate Flyway migration scripts.

---

## 🔁 Force Regeneration

Add this flag to `pom.xml`:

```xml
<forceRegeneration>true</forceRegeneration>
```

---

## 📄 License

MIT License