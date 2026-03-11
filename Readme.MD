# Spring CRUD Generator

[![CI](https://github.com/mzivkovicdev/spring-crud-generator/actions/workflows/ci.yml/badge.svg)](https://github.com/mzivkovicdev/spring-crud-generator/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=flat)](https://opensource.org/licenses/Apache-2.0)
![Open Source](https://img.shields.io/badge/Open%20Source-Yes-success?style=flat)
[![Maven Central](https://img.shields.io/maven-central/v/dev.markozivkovic/spring-crud-generator.svg)](https://central.sonatype.com/artifact/dev.markozivkovic/spring-crud-generator)
[![Docs](https://img.shields.io/badge/Docs-Available-blue?style=flat)](./docs/README.md)

![Java](https://img.shields.io/badge/Java-17--25-blue?style=flat&logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=flat&logo=springboot&logoColor=white)

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=flat&logo=mariadb&logoColor=white)
![Microsoft SQL Server](https://img.shields.io/badge/Microsoft%20SQL%20Server-CC2927?style=flat&logo=microsoftsqlserver&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DD0031?style=flat&logo=redis&logoColor=white)
![Hazelcast](https://img.shields.io/badge/Hazelcast-Real--time%20Data%20Platform-0A0A0A?style=flat&logo=hazelcast&logoColor=white)

Spring CRUD Generator is an open-source **Maven plugin** that accelerates backend development with **Spring Boot** by generating production-ready CRUD layers, migrations, API documentation, Docker resources, tests, and more — from a single YAML or JSON configuration file.

## Documentation

Full documentation is available in [`/docs`](./docs/README.md).

- [Getting started](./docs/getting-started.md)
- [Configuration reference](./docs/configuration.md)
- [Packages configuration](./docs/packages.md)
- [Entities and fields](./docs/entities.md)
- [Migrations](./docs/migrations.md)
- [Incremental generation](./docs/incremental-generation.md)
- [Full spec example](./docs/examples/crud-spec-full.yaml)
- [JSON Schema](./docs/schema/crud-spec.schema.json)

> Maven Central: `dev.markozivkovic:spring-crud-generator:1.5.0`  
> Artifact page: https://central.sonatype.com/artifact/dev.markozivkovic/spring-crud-generator

---

## What you get

From one spec file, the generator can produce:

- **JPA/Hibernate entity models**
- **Repositories**, **Services**, **Business services**
- **TOs & mappers**
- **REST controllers**
- **GraphQL schemas + resolvers** (optional)
- **OpenAPI/Swagger specs + resources** (optional)
- **Flyway migrations** (optional)
- **Dockerfile + docker-compose** (optional)
- **Unit tests + test data generation** (optional)
- **Caching** (Redis, Caffeine, Hazelcast or Simple) (optional)
- **Optimistic locking support** (optional)

> ⚠️ Table deletion is **not** supported by the generator and must be handled manually.

---

## Start here

Choose what you need:

- New user? → [Getting started](./docs/getting-started.md)
- Need all available options? → [Configuration reference](./docs/configuration.md)
- Working with entities and fields? → [Entities and fields](./docs/entities.md)
- Need package customization? → [Packages configuration](./docs/packages.md)
- Need a complete example? → [Full spec example](./docs/examples/crud-spec-full.yaml)
- Want autocomplete/validation? → [JSON Schema](./docs/schema/crud-spec.schema.json)

---

## Quick start

Generate a fully functional Spring Boot CRUD application in minutes using a single YAML or JSON configuration file.

### Step 1: Add Maven Profile

> Maven Central: `dev.markozivkovic:spring-crud-generator:1.5.0`  
> Artifact page: https://central.sonatype.com/artifact/dev.markozivkovic/spring-crud-generator

Add the following `profile` section to your `pom.xml`:

```xml
<profiles>
    <profile>
        <id>generate-resources</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>dev.markozivkovic</groupId>
                    <artifactId>spring-crud-generator</artifactId>
                    <version>1.5.0</version>
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

### Step 2: Create configuration file

Create the configuration file at the location defined by `inputSpecFile`.  
The file name is arbitrary. Only the extension matters: `.yaml`, `.yml`, or `.json`.

```text
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

This spec file can define:

- global generator configuration (database, OpenAPI, Docker, tests, caching, migration scripts, etc.)
- entities, fields, constraints, and relationships
- additional features like optimistic locking, base path, and more

Note: the example above shows only a minimal subset (database + OpenAPI + tests + one entity).

Both YAML (`.yaml/.yml`) and JSON (`.json`) formats are supported.

### Copilot + autocomplete for spec files

For better completion in VS Code/Copilot, bind your spec to the schema.

YAML (`*.yaml` / `*.yml`):

```yaml
# yaml-language-server: $schema=https://raw.githubusercontent.com/mzivkovicdev/spring-crud-generator/main/docs/schema/crud-spec.schema.json
configuration:
  database: postgresql
```

JSON (`*.json`):

```json
{
  "$schema": "https://raw.githubusercontent.com/mzivkovicdev/spring-crud-generator/main/docs/schema/crud-spec.schema.json",
  "configuration": {
    "database": "postgresql"
  }
}
```

### Step 3: Validate spec (dry-run, optional)

Use the `validate` goal to check whether the spec file is valid before generating code:

```bash
mvn spring-crud-generator:validate -DinputSpecFile=src/main/resources/crud-spec.yaml
```

### Step 4: Run the generator

Run the generator using the Maven profile:

```bash
mvn clean install -Pgenerate-resources
```

After execution:

- source code is generated into the directory defined by `outputDir`
- Flyway migration scripts are generated (if enabled)
- Swagger/OpenAPI resources are generated (if enabled)
- Docker and Docker Compose files are generated (if enabled)

### Step 5: Configuration parameters

| Parameter | Description |
| --- | --- |
| `inputSpecFile` | Path to the YAML/JSON configuration file |
| `outputDir` | Directory where generated source code is written |
| `forceRegeneration` | Forces regeneration of all non-ignored entities, ignoring state files |

For the `validate` goal, only `inputSpecFile` is required.

### Important notes

Incremental generation is enabled by default:

- Generator state is stored in: `generator-state.json` and `migration-state.json`
- Setting `forceRegeneration=true` ignores these files
- Database tables are never dropped automatically

---

## Supported Java versions

- Java **17** – **25**

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](./LICENSE.txt) file for details.

---

## Contact

For questions, ideas, or commercial usage of this project, feel free to reach out:

- Email: **mzivkovic.dev@gmail.com**
- LinkedIn: [Marko Zivkovic](https://www.linkedin.com/in/marko-zivkovic-84547017a/)
