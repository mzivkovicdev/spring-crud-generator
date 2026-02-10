# Getting started

## 1) Add the Maven profile

Add this `profile` section to your `pom.xml`:

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

### Parameters

| Parameter           | Description                                                           |
| ------------------- | --------------------------------------------------------------------- |
| `inputSpecFile`     | Path to the YAML/JSON configuration file                              |
| `outputDir`         | Directory where generated source code is written                      |
| `forceRegeneration` | Forces regeneration of all non-ignored entities, ignoring state files |

## 2) Create the spec file

Create the file defined by `inputSpecFile`:

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

Full example spec: [docs/examples/crud-spec-full.yaml](./examples/crud-spec-full.yaml)

## 3) Run generation

```bash
mvn clean install -Pgenerate-resources -DskipTests
```

After execution:

- source code is generated into the directory defined by `outputDir`
- Flyway migration scripts are generated (if enabled)
- Swagger/OpenAPI resources are generated (if enabled)
- Docker and Docker Compose files are generated (if enabled)
- Graphql resources (schema, resolvers, mappers etc.) are generated (if enabled)

## Notes

Incremental generation is enabled by default:

- Generator state is stored in: `generator-state.json` and `migration-state.json`
- Setting `forceRegeneration=true` ignores these files
- Database tables are never dropped automatically
