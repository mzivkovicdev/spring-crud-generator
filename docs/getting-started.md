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
                    <version>1.7.0</version>
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

Create the file defined by `inputSpecFile`.
The file name is arbitrary. Only the extension is required: `.yaml`, `.yml`, or `.json`.

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

Full example spec: [crud-spec-full.yaml](./examples/crud-spec-full.yaml)  
Schema for validation and editor autocomplete: [crud-spec.schema.json](./schema/crud-spec.schema.json)

### Schema-based autocomplete and validation

Schema hints are optional, but recommended.

They are the easiest way to enable validation and autocomplete for spec files with arbitrary names. If your editor is already configured to associate these files with `crud-spec.schema.json`, you do not need to add the schema hint manually.

For YAML files, add a schema hint comment:

```yaml
# yaml-language-server: $schema=https://raw.githubusercontent.com/mzivkovicdev/spring-crud-generator/main/docs/schema/crud-spec.schema.json
configuration:
  database: postgresql
```

For JSON files, use the `$schema` property:

```json
{
  "$schema": "https://raw.githubusercontent.com/mzivkovicdev/spring-crud-generator/main/docs/schema/crud-spec.schema.json",
  "configuration": {
    "database": "postgresql"
  }
}
```

Works in editors that support JSON Schema, including YAML editors that use `yaml-language-server`.

## 3) Validate spec (dry-run, optional)

Use the `validate` goal to verify spec correctness before generation:

```bash
mvn spring-crud-generator:validate -DinputSpecFile=src/main/resources/crud-spec.yaml
```

## 4) Run generation

```bash
mvn clean install -Pgenerate-resources -DskipTests
```

After execution:

- source code is generated into the directory defined by `outputDir`
- Flyway migration scripts are generated (if enabled)
- Swagger/OpenAPI resources are generated (if enabled)
- Docker and Docker Compose files are generated (if enabled)
- Graphql resources (schema, resolvers, mappers etc.) are generated (if enabled)

For the `validate` goal, only `inputSpecFile` is required.

## Notes

Incremental generation is enabled by default:

- Generator state is stored in: `generator-state.json` and `migration-state.json`
- Setting `forceRegeneration=true` ignores these files
- Database tables are never dropped automatically
