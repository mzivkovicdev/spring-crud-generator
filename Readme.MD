# 🚀 Spring CRUD Generator

![Java](https://img.shields.io/badge/Java-17--25-blue)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)
![MicrosoftSQLServer](https://img.shields.io/badge/Microsoft%20SQL%20Server-CC2927?style=for-the-badge&logo=microsoft%20sql%20server&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)
![Open Source](https://img.shields.io/badge/Open%20Source-Yes-success)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Spring CRUD Generator** is an open-source code generation tool designed to accelerate backend development with Spring Boot.  
It generates production-ready CRUD layers, database migrations, API documentation, Docker resources, tests, and more — directly from a single YAML or JSON configuration file.

This project aims to eliminate repetitive boilerplate code while keeping full control and transparency over the generated output.

---

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
  openApi:
    apiSpec: true
    generateResources: true
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
- Both YAML (`.yaml/.yml`) and JSON (`.json`) formats are supported.

> Full spec example: [`crud-spec-full.yaml`](docs/examples/crud-spec-full.yaml)

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

| Property            | Type    | Default | Description                                                                                                                                                                                                                                                                                                                                  |
| ------------------- | ------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `database`          | string  | `-`     | **Required.** Target SQL database (e.g. `postgresql`, `mysql`, `mssql`)                                                                                                                                                                                                                                                                      |
| `javaVersion`       | number  | `17`    | Java version used for generated code and Dockerfile                                                                                                                                                                                                                                                                                          |
| `springBootVersion` | string  | `4`     | Spring Boot **major** version (`3` or `4`). If not provided, the generator tries to detect it from the project `pom.xml` (parent version). If detection fails, it defaults to `4`. If an unsupported value is provided (e.g. `1`, `2`, `5`), it will be ignored and the generator will fall back to the detected value or the default (`4`). |
| `optimisticLocking` | boolean | `false` | Enables optimistic locking support                                                                                                                                                                                                                                                                                                           |
| `graphQl`           | boolean | `false` | Enables GraphQL query and mutation generation                                                                                                                                                                                                                                                                                                |
| `errorResponse`     | string  | `-`     | Error response strategy (`simple`, `detailed`, `minimal`, `none`)                                                                                                                                                                                                                                                                            |
| `migrationScripts`  | boolean | `false` | Enables Flyway migration script generation                                                                                                                                                                                                                                                                                                   |

---

### `configuration.openApi`

Controls per-entity OpenAPI / Swagger generation.

| Property             | Type    | Default | Description                                                                                           |
|----------------------|---------|---------|-------------------------------------------------------------------------------------------------------|
| `apiSpec`            | boolean | `false` | Generates a separate OpenAPI/Swagger specification file **for each entity** (e.g. `product-api.yaml`).|
| `generateResources`  | boolean | `false` | Generates REST resources/controllers based on the per-entity OpenAPI specs.                           |

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

| Property | Type   | Default                    | Description          |
|----------|--------|----------------------------|----------------------|
| `image`  | string | depends on `database`      | Database Docker image |
| `port`   | number | depends on `database`      | Database port        |
| `tag`    | string | `latest`                   | Docker image tag     |

**Defaults by database type**

When you don’t override `image`/`port`, the generator uses:

- **PostgreSQL**
  - `image`: `postgres`
  - `port`: `5432`
- **MySQL**
  - `image`: `mysql`
  - `port`: `3306`
- **MSSQL**
  - `image`: `mcr.microsoft.com/mssql/server`
  - `port`: `1433`

---

### `configuration.cache`

Enables and configures caching.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `false` | Enables caching |
| `type` | string | `null` | Cache provider (`CAFFEINE`, `REDIS`) |
| `expiration` | number | `null` | Cache expiration time (minutes) |
| `maxSize` | number | `null` | Maximum cache size |

---

### `configuration.tests`

Controls test generation.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `unit` | boolean | `false` | Enables unit test generation |
| `dataGenerator` | string | `instancio` | Test data generator (`instancio`, `podam`) |

**Validation rules**

The generator validates the test configuration:

- If `unit: true` → `dataGenerator` **must be set**.
- `dataGenerator` should be one of the supported values: `instancio`, `podam`
  (case-insensitive, mapped to the internal `DataGeneratorEnum`).
- If `unit: false` (or omitted), `dataGenerator` is optional and ignored by the generator.
- If `unit: true` and `dataGenerator` is missing, generation fails with a clear error:

  `Invalid test configuration: unit is enabled, but dataGenerator is not set. Please set dataGenerator to one of the following values: INSTANCIO, PODAM`

---

### `configuration.additionalProperties`

Advanced and feature-specific configuration options.

| Property                               | Type    | Default | Description                                                                                           |
| -------------------------------------- | ------- | ------- | ----------------------------------------------------------------------------------------------------- |
| `rest.basePath`                        | string  | `/api`  | Base path for generated REST endpoints. Example: `/api/v1`                                            |
| `graphql.scalarConfig`                 | boolean | `false` | Enables custom GraphQL scalar configuration                                                           |
| `optimisticLocking.retry.config`       | boolean | `false` | Enables generation of a dedicated `@Retryable` configuration for optimistic locking                   |
| `optimisticLocking.retry.maxAttempts`  | number  | `3`     | Maximum retry attempts (falls back to this value if not overridden)                                   |
| `optimisticLocking.backoff.delayMs`    | number  | `1000`  | Initial backoff delay in milliseconds (falls back to this value if not overridden)                    |
| `optimisticLocking.backoff.multiplier` | number  | `0.0`   | Backoff multiplier. `0.0` means no exponential backoff (constant delay) unless explicitly overridden  |
| `optimisticLocking.backoff.maxDelayMs` | number  | `0`     | Maximum backoff delay in milliseconds. `0` means “no explicit max”, only `delay * multiplier` applies |

> ℹ️ The retry configuration is generated only if **optimisticLocking** is enabled. Custom retry annotation is genereted if at least one of the `optimisticLocking.retry.*` / `optimisticLocking.backoff.*` properties is provided. Any missing values fall back to the defaults listed above.

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
- If `openApi.generateResources: true` is enabled, the `generated` package is required.
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
    audit:
      enabled: true
      type: Instant
    fields: []
```
| Property      | Type   | Required | Description                                                     |
| ------------- | ------ | -------- | ----------------------------------------------------------------|
| `name`        | string | ✅       | Java class name of the entity/model                             |
| `storageName` | string | ✅       | Database table name (storage name).                             |
| `description` | string | optional | Used to generate Javadoc and enrich API docs (where applicable) |
| `audit`       | object | optional | Audit configuration for `created_at` / `updated_at` columns     |
| `fields`      | list   | ✅       | List of fields for the entity                                   |

- Note: If description is provided, the generator can produce Javadoc for entities/fields.

### Audit configuration

The audit block controls automatic creation of audit columns on the entity’s table (typically `created_at` and `updated_at`).

```yaml
audit:
  enabled: true
  type: INSTANT
```
| Property  | Type    | Required | Description                                                                           |
| --------- | ------- | -------- | --------------------------------------------------------------------------------------|
| `enabled` | boolean | optional | Enables audit columns (`created_at`, `updated_at`) for this entity (default: `false`) |
| `type`    | enum    | optional | Underlying temporal type used for audit columns                                       |

Possible type values:
- Instant
- LocalDate
- LocalDateTime

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
| `type`        | string | ✅                  | Java type (e.g. `String`, `Long`, `UUID`, `LocalDate`, `Enum`, `JSON<Type>`, entity name for relations, or `List<BasicType>` / `Set<BasicType>`) |
| `description` | string | optional            | Used for Javadoc and API documentation                                                               |
| `id`          | object | optional            | Marks the field as primary key and defines generation strategy                                       |
| `column`      | object | optional            | Column constraints (unique, nullable, insertable, updateable, length etc.)                           |
| `relation`    | object | optional            | Relationship definition (JPA-style)                                                                  |
| `values`      | list   | required for `Enum` | Enum constant values (only when `type: Enum`)                                                        |

- Supported basic types are: String, Character, Integer, Long, Boolean, Double, Float, Short, Byte, UUID, BigDecimal, BigInteger, LocalDate, LocalDateTime, OffsetDateTime, Instant

#### Simple collections (ElementCollection)

Simple collections are supported only for basic types:
- `List<BasicType>`
- `Set<BasicType>`

Example:
```yaml
- name: phoneNumbers
  type: List<String>
- name: tags
  type: Set<Long>
```
For `List<BasicType>` / `Set<BasicType>`, the generator creates a separate collection table named `${storageName}_${snake_case(fieldName)}` and links it to the owner entity via `<entity>_id`.

### 🔑 Primary Key: id.strategy

To mark a field as the entity identifier, add an id block:

```yaml
- name: id
  type: Long
  description: "The unique identifier for the entity"
  id:
    strategy: IDENTITY
```
Supported strategies:
- TABLE
- SEQUENCE
- UUID
- IDENTITY
- AUTO

| Property          | Type   | Required | Applies to          | Description                                                                   |
| ----------------- | ------ | -------- | ------------------- | ----------------------------------------------------------------------------- |
| `strategy`        | enum   | ✅       | all                 | ID generation strategy                                                        |
| `sequenceName`    | string | optional | `SEQUENCE`, `TABLE` | For `SEQUENCE`: DB sequence name. For `TABLE`: generator table name.          |
| `allocationSize`  | number | optional | `SEQUENCE`, `TABLE` | Allocation size for sequence/table generators (defaults to `50`).             |
| `initialValue`    | number | optional | `SEQUENCE`, `TABLE` | Initial value for ID generation (defaults to `1`).                            |
| `pkColumnName`    | string | optional | `TABLE`             | Name of the “segment key” column in generator table (defaults to `gen_name`). |
| `valueColumnName` | string | optional | `TABLE`             | Name of the “counter” column in generator table (defaults to `gen_value`).      |

#### Sequence-Based ID Example
```yaml
- name: id
  type: Long
  id:
    strategy: SEQUENCE
    sequenceName: product_id_seq   # optional, default: <table>_id_seq
    allocationSize: 10             # optional, default: 50
    initialValue: 1                # optional, default: 1
```
#### Table-Based ID Example
```yaml
- name: id
  type: Long
  id:
    strategy: TABLE
    sequenceName: product_id_gen   # optional, default: <table>_id_gen
    pkColumnName: my_gen_name      # optional, default: gen_name
    valueColumnName: my_gen_val    # optional, default: gen_value
    allocationSize: 5              # optional, default: 50
    initialValue: 1000             # optional, default: 1
```
- For `TABLE` strategy, the `pkColumnValue` is automatically set to the entity's `storageName` (e.g. `user_table`), so you normally don't need to configure it manually.

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

## 🔄 Incremental Generation & State Files

The generator produces two internal state files:

### `generator-state.json`
Tracks generated entities using fingerprints to avoid unnecessary regeneration.

### `migration-state.json`
Tracks schema changes to safely generate Flyway migration scripts.

---

## 🗄️ Migrations Behavior (Flyway)

The generator supports automatic database schema migrations for SQL databases using **Flyway**.

Migrations are generated based on differences between:
- the **current configuration file** (e.g `crud-spec.yaml` / `.json`)
- the **previous generator state**, stored on disk

This allows safe, incremental schema evolution without manual SQL scripting in most cases.

---

### How Migration Generation Works

For SQL databases, the generator:
1. Analyzes the current entity configuration
2. Compares it with the previous generation state
3. Generates Flyway migration scripts accordingly

To track schema changes, the generator creates and maintains the following file:

```text
.crud-generator/migration-state.json
```
This file is used internally to detect what changed between generations.

#### migration-state.json

The migration-state.json file stores metadata about:
- previously generated entities
- their fields and types
- table and column mappings

It allows the generator to:
- detect newly added entities
- detect added fields
- detect removed fields
- generate correct Flyway migration scripts
This file should be committed to version control to ensure consistent migrations across environments.

#### Supported Migration Scenarios
##### ➕ Adding a New Entity
When a new entity is added to the configuration:
- the generator detects that new table is added
- a Flyway CREATE TABLE migration is created

##### ➕ Adding a New Field
When a new field is added to an existing entity:
- the generator detects the change
- a Flyway ALTER TABLE ADD COLUMN migration is created

##### ➖ Removing a Field
When a field is removed from an existing entity:
- the generator detects the removal
- a Flyway ALTER TABLE DROP COLUMN migration is created

### ⚠️ Important Limitation: No Table Drop
❌ Dropping database tables is NOT supported by the generator.
- If an entity is removed from the configuration file - no DROP TABLE migration is generated
- Table removal must be handled manually by the user
This design choice is intentional to prevent accidental data loss.

## 🔄 Generator State (`generator-state.json`)

To avoid unnecessary regeneration of code, the generator uses a state-based mechanism.

For this purpose, it generates and maintains the following file:

```text
.crud-generator/generator-state.json
```
This file tracks the state of the previously generated configuration and entities.

### What Is Stored in generator-state.json

The state file contains:
- a configuration SHA (hash of the global configuration)
- an entity SHA for each generated entity
These hashes are used to detect whether something has changed between generator runs.

### How Change Detection Works
#### Entity-Level Changes

- For each entity, the generator computes a fingerprint (SHA)
- If an entity’s definition has not changed, the entity is not regenerated
- If an entity’s definition has changed, the entity is included in generation
This allows fast, incremental code generation.

#### Configuration-Level Changes
- The generator also computes a fingerprint (SHA) for the global configuration
- If the configuration SHA changes, `all entities are regenerated, regardless of individual entity changes`
This ensures consistency when global settings (e.g. caching, Swagger/OpenApi, GraphQL, tests, etc.) are modified.

### 🔁 Interaction with forceRegeneration

By default, regeneration is controlled by generator-state.json.

If the following flag is enabled in `pom.xml`:

```xml
<forceRegeneration>true</forceRegeneration>
```

Then:
- the generator ignores generator-state.json
- all non-ignored entities are regenerated unconditionally
This is useful when:
- refactoring templates
- debugging generator behavior
- forcing a clean regeneration

#### Ignored Entities
Entities marked with:
```yaml
ignore: true
```
- are excluded from generation
- are not tracked for regeneration
- do not affect the generator state

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](./LICENSE) file for details.

---

## 🤝 Contact

For questions, ideas, or commercial usage of this project, feel free to reach out:

- Email: **mzivkovic.dev@gmail.com**
- LinkedIn: [Marko Zivkovic](https://www.linkedin.com/in/marko-zivkovic-84547017a/)
