# Spring CRUD Generator

[![CI](https://github.com/mzivkovicdev/spring-crud-generator/actions/workflows/ci.yml/badge.svg)](https://github.com/mzivkovicdev/spring-crud-generator/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=flat)](https://opensource.org/licenses/Apache-2.0)
![Open Source](https://img.shields.io/badge/Open%20Source-Yes-success?style=flat)
[![Maven Central](https://img.shields.io/maven-central/v/dev.markozivkovic/spring-crud-generator.svg)](https://central.sonatype.com/artifact/dev.markozivkovic/spring-crud-generator)

![Java](https://img.shields.io/badge/Java-17--25-blue?style=flat&logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=flat&logo=springboot&logoColor=white)

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)
![Microsoft SQL Server](https://img.shields.io/badge/Microsoft%20SQL%20Server-CC2927?style=flat&logo=microsoftsqlserver&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DD0031?style=flat&logo=redis&logoColor=white)

Spring CRUD Generator is an open-source **Maven plugin** that accelerates backend development with **Spring Boot** by generating production-ready CRUD layers, database migrations, API documentation, Docker resources, tests, and more — directly from a single YAML or JSON configuration file.

This project aims to eliminate repetitive boilerplate code while keeping full control and transparency over the generated output.

> Maven Central: `dev.markozivkovic:spring-crud-generator:1.1.0`

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
- **Unit tests + test data generation** (Instancio / Podam) - (optional)
- **Caching** (Redis / Caffeine) - (optional)
- **Optimistic locking support** (optional)

> ⚠️ Table deletion is **not** supported by the generator (must be handled manually).

---

## Supported Java versions

- Java **17** – Java **25**

---

## Quick start

Generate a fully functional Spring Boot CRUD application in minutes using a single YAML or JSON configuration file.

### Step 1: Add Maven Profile

> Maven Central: `dev.markozivkovic:spring-crud-generator:1.1.0`  
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
                    <version>1.1.0</version>
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

Create the configuration file at the location defined by `inputSpecFile`:

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

This spec file can define:
- global generator configuration (database, OpenAPI, Docker, tests, caching, migration scripts, etc.)
- entities, fields, constraints, and relationships
- additional features like optimistic locking, base path etc.

Note: the example above shows only a minimal subset (database + OpenAPI + tests + one entity).

Both YAML (`.yaml/.yml`) and JSON (`.json`) formats are supported.

> Full spec example: [`crud-spec-full.yaml`](docs/examples/crud-spec-full.yaml)

### Step 3: Run the generator

Run the generator using the Maven profile:

```bash
mvn clean install -Pgenerate-resources
```

After execution:
- source code is generated into the directory defined by `outputDir`
- Flyway migration scripts are generated (if enabled)
- Swagger/OpenAPI resources are generated (if enabled)
- Docker and Docker Compose files are generated (if enabled)

### Step 4: Configuration Parameters

| Parameter           | Description                                                           |
| ------------------- | --------------------------------------------------------------------- |
| `inputSpecFile`     | Path to the YAML/JSON configuration file                              |
| `outputDir`         | Directory where generated source code is written                      |
| `forceRegeneration` | Forces regeneration of all non-ignored entities, ignoring state files |

### Important notes

Incremental generation is enabled by default:
- Generator state is stored in: `generator-state.json` and `migration-state.json`
- Setting `forceRegeneration=true` ignores these files
- Database tables are never dropped automatically

---

## Documentation

The README is intentionally short. Full documentation lives in **`/docs`**:

- **Getting started:** [`docs/getting-started.md`](./docs/getting-started.md)
- **Configuration reference:** [`docs/configuration.md`](./docs/configuration.md)
- **Packages configuration:** [`docs/packages.md`](./docs/packages.md)
- **Entity & field schema:** [`docs/entities.md`](./docs/entities.md)
- **Migrations & incremental generation:** [`docs/migrations.md`](./docs/migrations.md), [`docs/incremental-generation.md`](./docs/incremental-generation.md)

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](./LICENSE.txt) file for details.

---

## Contact

For questions, ideas, or commercial usage of this project, feel free to reach out:

- Email: **mzivkovic.dev@gmail.com**
- LinkedIn: [Marko Zivkovic](https://www.linkedin.com/in/marko-zivkovic-84547017a/)