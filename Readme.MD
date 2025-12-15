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