# Configuration Reference

All options are defined under the root `configuration` key.

---

## `configuration`

| Property            | Type    | Default | Description                                                                                                                                                                                                                                                                                                                                  |
| ------------------- | ------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `database`          | string  | `-`     | **Required.** Target database: SQL (`postgresql`, `mysql`, `mariadb`, `mssql`) or NoSQL (`mongodb`)                                                                                                                                                                                                                                                     |
| `javaVersion`       | number  | `17`    | Java version used for generated code and Dockerfile                                                                                                                                                                                                                                                                                          |
| `springBootVersion` | string  | `4`     | Spring Boot **major** version (`3` or `4`). If not provided, the generator tries to detect it from the project `pom.xml` (parent version). If detection fails, it defaults to `4`. If an unsupported value is provided (e.g. `1`, `2`, `5`), it will be ignored and the generator will fall back to the detected value or the default (`4`). |
| `optimisticLocking` | boolean | `false` | Enables optimistic locking. Generates a `@Version` field on each entity/document and a `@OptimisticLockingRetry` annotation. See details below. |
| `errorResponse`     | string  | `-`     | Error response strategy (`simple`, `detailed`, `minimal`, `none`)                                                                                                                                                                                                                                                                            |
| `migrationScripts`  | boolean | `false` | Enables migration generation. For SQL databases: Flyway `.sql` scripts. For MongoDB: Mongock `@ChangeUnit` Java classes. See [migrations](migrations.md).                                                                                                                                                                                    |
| `dependencyCheck`   | boolean | `false` | Enables post-validation check that scans the host project `pom.xml` and prints warnings for missing dependencies required by selected features (database driver, GraphQL, Flyway, Mongock, cache, OpenAPI resources, tests, etc.)                                                                                                             |

### Optimistic locking behavior

When `optimisticLocking: true` is set, the generator:

1. Adds a `version` field to each generated entity/document:
   - **SQL**: `@Version private Integer version;` (uses `jakarta.persistence.Version`)
   - **MongoDB**: `@Version private Long version;` (uses `org.springframework.data.annotation.Version`)

2. Generates a custom `@OptimisticLockingRetry` annotation (when retry properties are configured) that retries the method on a version conflict:
   - **SQL**: catches `jakarta.persistence.OptimisticLockException` and `ObjectOptimisticLockingFailureException`
   - **MongoDB**: catches `org.springframework.dao.OptimisticLockingFailureException`

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
- **MariaDB**
  - `image`: `mariadb`
  - `port`: `3306`
- **MSSQL**
  - `image`: `mcr.microsoft.com/mssql/server`
  - `port`: `1433`
- **MongoDB**
  - `image`: `mongo`
  - `port`: `27017`

---

## `configuration.cache`

Enables and configures caching.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `false` | Enables caching |
| `type` | string | `null` | Cache provider (`CAFFEINE`, `REDIS`, `HAZELCAST`, `SIMPLE`) |
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

## `configuration.security`

Controls generated Spring Security setup.

| Property | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `false` | Enables security generation and endpoint protection. |
| `type` | string | `BASIC_AUTH` (when enabled) | Security mode: `BASIC_AUTH`, `JWT`, `OAUTH2_RESOURCE_SERVER`, `API_KEY`. |
| `basicAuth` | object | `null` | Basic auth user definitions (`users`). |
| `jwt` | object | `null` | JWT settings (`secret`, `expirationMs`, `issuer`). |
| `oauth2` | object | `null` | OAuth2 resource server settings (`issuerUri`, `jwkSetUri`, `rolesClaim`). |
| `apiKey` | object | `null` | API key settings (`headerName`, `keys`). |

Security type matching is case-insensitive in the generator.
When GraphQL is enabled, generated resolver methods also receive operation-level security annotations based on entity `security` mapping.

### Basic Auth (`BASIC_AUTH`)

```yaml
configuration:
  security:
    enabled: true
    type: BASIC_AUTH
    basicAuth:
      users:
        - username: admin
          password: admin
          roles: [ADMIN]
        - username: user
          password: user
          roles: [USER]
```

If `basicAuth.users` is omitted, generator falls back to an in-memory `admin/admin` user with role `ADMIN`.

### JWT (`JWT`)

```yaml
configuration:
  security:
    enabled: true
    type: JWT
    jwt:
      secret: "change-me-very-long-secret-key-at-least-32-chars"
      expirationMs: 3600000
      issuer: "my-crud-app"
```

Generated JWT mode includes:
- `POST /auth/login` endpoint (public)
- `POST /auth/refresh` endpoint (public)
- JWT auth filter + token provider
- starter `UserDetailsServiceImpl` stub

Important:
- Replace the generated `UserDetailsServiceImpl` stub with your repository-backed implementation.
- `POST /auth/login` returns both access token (`token`) and refresh token (`refreshToken`) in `AuthResponse`.
- `POST /auth/refresh` accepts `refreshToken` and issues a fresh access+refresh token pair.
- JWT token provider reads `jwt.secret`, `jwt.expiration-ms`, `jwt.refresh-expiration-ms`, and `jwt.issuer` Spring properties (defaults exist in generated code).

### OAuth2 Resource Server (`OAUTH2_RESOURCE_SERVER`)

```yaml
configuration:
  security:
    enabled: true
    type: OAUTH2_RESOURCE_SERVER
    oauth2:
      rolesClaim: realm_access.roles
      issuerUri: https://idp.example.com/realms/myrealm
      jwkSetUri: https://idp.example.com/realms/myrealm/protocol/openid-connect/certs
```

Notes:
- Generated `JwtRoleConverter` maps roles from `security.oauth2.roles-claim` (default `realm_access.roles`).
- Resource server issuer/JWK endpoint should be configured in Spring security properties for your runtime environment.

### API Key (`API_KEY`)

```yaml
configuration:
  security:
    enabled: true
    type: API_KEY
    apiKey:
      headerName: X-API-Key
      keys:
        - name: internal-client
          value: "dev-key-123"
          roles: [ADMIN]
        - name: readonly-client
          value: "dev-key-456"
          roles: [USER]
```

If `headerName` is omitted, default header is `X-API-Key`.

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
| `rest.response.excludeNull`            | boolean | `false` | When enabled, exclude `null` fields from JSON responses globally (Jackson `NON_NULL`).                |
| `optimisticLocking.retry.config`       | boolean | `false` | Enables generation of a dedicated `@EnableRetry` Spring configuration class.                          |
| `optimisticLocking.retry.maxAttempts`  | number  | `3`     | Maximum retry attempts for the generated `@OptimisticLockingRetry` annotation.                        |
| `optimisticLocking.backoff.delayMs`    | number  | `1000`  | Initial backoff delay in milliseconds.                                                                |
| `optimisticLocking.backoff.multiplier` | number  | `0.0`   | Backoff multiplier. `0.0` means no exponential backoff (constant delay) unless explicitly overridden. |
| `optimisticLocking.backoff.maxDelayMs` | number  | `0`     | Maximum backoff delay in milliseconds. `0` means “no explicit max”, only `delay * multiplier` applies.|
| `spring.jpa.open-in-view`              | boolean | `false` | Enables/disables OSIV. The generator always supports explicit fetch plans via `EntityGraph` and when this property is false it treats them as the default approach for loading LAZY relations to avoid `LazyInitializationException` during DTO mapping. **SQL/JPA only.** |
| `github.actions`                       | boolean | `false` | Generates a basic GitHub Actions CI workflow file at `.github/workflows/ci.yml` (checkout, setup Java, Maven cache, build, test). |

> The retry configuration is generated only if `optimisticLocking: true`. The generated `@OptimisticLockingRetry` annotation catches the appropriate exception per database type:
> - **SQL**: catches `jakarta.persistence.OptimisticLockException` and `ObjectOptimisticLockingFailureException`
> - **MongoDB**: catches `org.springframework.dao.OptimisticLockingFailureException`
>
> Custom retry annotation is generated if at least one of the `optimisticLocking.retry.*` / `optimisticLocking.backoff.*` properties is provided. Any missing values fall back to the defaults listed above.
>
> `spring.jpa.open-in-view`: when `false`, the generator uses `EntityGraph` as the default fetch strategy for `LAZY` relations to avoid `LazyInitializationException` during DTO mapping.

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
