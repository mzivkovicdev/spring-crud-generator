# ${artifactId}

This project was generated using [Spring CRUD Generator](https://github.com/mzivkovicdev/spring-crud-generator).

## Build & Test

```bash
# Build
mvn clean install -DskipTests

# Build with tests
mvn clean install
<#if unitTestsEnabled>

# Run unit tests
mvn test
</#if>
<#if integrationTestsEnabled>

# Run integration tests
mvn verify
</#if>
```

## Run

```bash
mvn spring-boot:run
```
<#if dockerComposeEnabled>

Or with Docker Compose (starts app + database<#if cacheEnabled> + cache</#if>):

```bash
docker-compose up -d
```
<#elseif dockerEnabled>

Or with Docker:

```bash
docker build -t ${artifactId} .
docker run -p ${appPort}:${appPort} ${artifactId}
```
</#if>

## Architecture

Spring Boot application with a layered architecture generated from `src/main/resources/crud-spec.yaml`.

**Database:** ${database}
<#if cacheEnabled>**Cache:** ${cacheType}</#if>
<#if graphqlEnabled>**APIs:** REST + GraphQL<#else>**API:** REST</#if>

### Generated Layers

| Package | Purpose |
|---------|---------|
| `model/` | JPA entities (database tables) |
| `repository/` | Spring Data JPA repositories |
| `service/` | CRUD service interfaces and implementations |
| `businessservice/` | Business logic layer |
| `to/` | Transfer Objects (request/response DTOs) |
| `mapper/` | Entity ↔ DTO mappers |
| `controller/` | REST controllers |
| `config/` | Application configuration |
| `exception/` | Exception types and error handling |
<#if graphqlEnabled>| `graphql/` | GraphQL resolvers and schema |
</#if>
<#if entities?has_content>
### Entities

<#list entities as entity>
- `${entity}`
</#list>
</#if>
<#if openApiEnabled>
## API Documentation

Swagger UI: `http://localhost:${appPort}/swagger-ui/index.html`
</#if>
<#if graphqlEnabled>
## GraphQL

Playground: `http://localhost:${appPort}/graphiql`
</#if>

## Important Rules

- **Never edit generated files directly.** Changes will be overwritten on next generator run. Re-run with `mvn clean install -Pgenerate-resources` instead.
- Custom business logic goes in `businessservice/` or new classes outside generated packages.
- `crud-spec.yaml` defines all entities, fields, and configuration — this is the single source of truth.
- `generator-state.json` must not be deleted (tracks incremental generation state).
<#if migrationScripts>
- Flyway migration files in `src/main/resources/db/migration/` must never be renamed or reordered.
</#if>
<#if cacheRequiresInfrastructure>
- **${cacheType} must be running** before starting the application.
</#if>

## Scope

Keep changes focused on explicit task requirements and avoid speculative rewrites.

## Workflow

- Understand the task before editing code.
- Prefer small, reviewable changes with clear intent.
- Preserve existing architecture and conventions unless a refactor is explicitly requested.

## Code Quality

- Keep code readable, typed, and production-ready.
- Add concise comments only when intent is not obvious from code.

## Git Guidelines

- Use feature branches for all non-trivial changes.
- Use Conventional Commit messages for every commit.
- Require pull requests with human review before merging.

## Delivery Guidelines

- Run relevant tests before considering the task complete.
- Keep changes within the requested scope and avoid unrelated rewrites.
