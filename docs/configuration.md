# Configuration Reference

All options are defined under the root `configuration` key.

---

## `configuration`

| Property            | Type    | Default | Description                                                                                                                                                                                                                                                                                                                                  |
| ------------------- | ------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `database`          | string  | `-`     | **Required.** Target SQL database (e.g. `postgresql`, `mysql`, `mssql`)                                                                                                                                                                                                                                                                      |
| `javaVersion`       | number  | `17`    | Java version used for generated code and Dockerfile                                                                                                                                                                                                                                                                                          |
| `springBootVersion` | string  | `4`     | Spring Boot **major** version (`3` or `4`). If not provided, the generator tries to detect it from the project `pom.xml` (parent version). If detection fails, it defaults to `4`. If an unsupported value is provided (e.g. `1`, `2`, `5`), it will be ignored and the generator will fall back to the detected value or the default (`4`). |
| `optimisticLocking` | boolean | `false` | Enables optimistic locking support                                                                                                                                                                                                                                                                                                           |
| `errorResponse`     | string  | `-`     | Error response strategy (`simple`, `detailed`, `minimal`, `none`)                                                                                                                                                                                                                                                                            |
| `migrationScripts`  | boolean | `false` | Enables Flyway migration script generation                                                                                                                                                                                                                                                                                                   |

---

## `configuration.openApi`

Controls per-entity OpenAPI / Swagger generation.

| Property             | Type    | Default | Description                                                                                           |
|----------------------|---------|---------|-------------------------------------------------------------------------------------------------------|
| `apiSpec`            | boolean | `false` | Generates a separate OpenAPI/Swagger specification file **for each entity** (e.g. `product-api.yaml`).|
| `generateResources`  | boolean | `false` | Generates REST resources/controllers based on the per-entity OpenAPI specs.                           |

---

## `configuration.docker`

Controls Docker and Docker Compose generation.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `dockerfile` | boolean | `false` | Generates a Dockerfile |
| `dockerCompose` | boolean | `false` | Generates a docker-compose.yml |

### `configuration.docker.app`

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `image` | string | `eclipse-temurin` | Base Docker image for the application |
| `port` | number | `8080` | Exposed application port |
| `tag` | string | `alpine` | Docker image tag |

### `configuration.docker.db`

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

## `configuration.cache`

Enables and configures caching.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `false` | Enables caching |
| `type` | string | `null` | Cache provider (`CAFFEINE`, `REDIS`) |
| `expiration` | number | `null` | Cache expiration time (minutes) |
| `maxSize` | number | `null` | Maximum cache size |

---

## `configuration.graphql`

Controls GraphQL generation and scalar configuration.

```yaml
configuration:
  graphql:
    enabled: true
    scalarConfig: true
```

| Property       | Type    | Default | Description                                                                                                                         |
| -------------- | ------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `enabled`      | boolean | `false` | Enables GraphQL support: generates `.graphqls` schema per entity with an ID and the corresponding resolver classes.                 |
| `scalarConfig` | boolean | `false` | Enables generation of Spring GraphQL configuration for custom scalars (e.g. `Instant`, `LocalDate`, `LocalDateTime`, audit fields). |

> GraphQL generation (schemas + resolvers) only happens when graphql.enabled: true. The scalar configuration is generated when GraphQL is enabled and graphql.scalarConfig: true.

---

## `configuration.tests`

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

## `configuration.additionalProperties`

Advanced and feature-specific configuration options.

| Property                               | Type    | Default | Description                                                                                           |
| -------------------------------------- | ------- | ------- | ----------------------------------------------------------------------------------------------------- |
| `rest.basePath`                        | string  | `/api`  | Base path for generated REST endpoints. Example: `/api/v1`                                            |
| `optimisticLocking.retry.config`       | boolean | `false` | Enables generation of a dedicated `@Retryable` configuration for optimistic locking                   |
| `optimisticLocking.retry.maxAttempts`  | number  | `3`     | Maximum retry attempts (falls back to this value if not overridden)                                   |
| `optimisticLocking.backoff.delayMs`    | number  | `1000`  | Initial backoff delay in milliseconds (falls back to this value if not overridden)                    |
| `optimisticLocking.backoff.multiplier` | number  | `0.0`   | Backoff multiplier. `0.0` means no exponential backoff (constant delay) unless explicitly overridden  |
| `optimisticLocking.backoff.maxDelayMs` | number  | `0`     | Maximum backoff delay in milliseconds. `0` means “no explicit max”, only `delay * multiplier` applies |

> The retry configuration is generated only if **optimisticLocking** is enabled. Custom retry annotation is genereted if at least one of the `optimisticLocking.retry.*` / `optimisticLocking.backoff.*` properties is provided. Any missing values fall back to the defaults listed above.

---

## Java Version & Docker Image Notes

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