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
| `optimisticLocking.retry.maxAttempts` | number | `3` | Maximum retry attempts |
| `optimisticLocking.backoff.delayMs` | number | `100` | Initial backoff delay (ms) |
| `optimisticLocking.backoff.multiplier` | number | `2.0` | Backoff multiplier |
| `optimisticLocking.backoff.maxDelayMs` | number | `1000` | Maximum backoff delay (ms) |

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

You can define a custom package structure by adding the packages section to your configuration file:
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
| Key                | Description                        |
| ------------------ | ---------------------------------- |
| `annotations`      | Custom and generated annotations   |
| `businessservices` | Business-level service classes     |
| `configurations`   | Spring configuration classes       |
| `controllers`      | REST controllers                   |
| `enums`            | Generated enum classes             |
| `exceptions`       | Custom and generated exceptions    |
| `generated`        | Internal/generated support classes |
| `mappers`          | Entity ↔ DTO mappers               |
| `models`           | Persistence models / entities      |
| `resolvers`        | GraphQL resolvers                  |
| `repositories`     | Spring Data repositories           |
| `services`         | Service layer classes              |
| `transferobjects`  | DTOs / transfer objects            |

### Notes
- Only packages explicitly defined in the packages section are overridden
- Any missing package definition falls back to the default structure
- This allows partial customization without redefining the entire structure

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