# ${artifactId}

This project was generated using [Spring CRUD Generator](https://github.com/mzivkovicdev/spring-crud-generator).

## Build & Run

```bash
# Build (skip tests)
mvn clean install -DskipTests

# Build with tests
mvn clean install

# Run
mvn spring-boot:run

# Re-run the generator (regenerate from spec)
mvn clean install -Pgenerate-resources
```

## Database

This project uses **${database}**. Configure the connection in `src/main/resources/application.properties` or `application.yml`.
<#assign mongoDb = (isMongoDatabase!false) || ((database!"") == "MONGODB")>
<#if migrationScripts>
<#if mongoDb>
Database migrations are managed by **Mongock**. Change units are in `${mongoMigrationPath}`. Never manually rename or reorder migration classes.
<#else>
Database schema is managed by **Flyway**. Migration scripts are in `src/main/resources/db/migration/`. Never manually rename or reorder migration files.
</#if>
</#if>
<#if cacheEnabled>

## Cache

This project uses **${cacheType}** caching.
<#if cacheType == "REDIS">
Redis must be running before starting the application (default: `localhost:6379`).
<#elseif cacheType == "HAZELCAST">
Hazelcast must be configured and available before starting the application.
<#elseif cacheType == "CAFFEINE">
Caffeine cache is embedded — no external infrastructure required.
<#elseif cacheType == "SIMPLE">
Simple in-memory cache is used — no external infrastructure required.
</#if>
</#if>
<#if dockerEnabled>

## Docker
<#if dockerComposeEnabled>
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down
```
<#else>
```bash
docker build -t ${artifactId} .
docker run -p ${appPort?c}:${appPort?c} ${artifactId}
```
</#if>
</#if>
<#if openApiEnabled>

## API Documentation

OpenAPI specs are generated in: `src/main/resources/swagger/`
</#if>
<#if graphqlEnabled>

## GraphQL

GraphQL endpoint is available at: `http://localhost:${appPort?c}/graphql`
</#if>
<#if testsEnabled>

## Testing

```bash
<#if unitTestsEnabled>
# Run unit tests
mvn test
</#if>
<#if integrationTestsEnabled>
# Run integration tests
mvn verify
</#if>
```
</#if>

## Project Structure

Generated from `src/main/resources/crud-spec.yaml`:

Package names are configurable via the `packages` section in `crud-spec.yaml`.

- `packages.models` — JPA entities
- `packages.repositories` — Spring Data repositories
- `packages.services` — Service interfaces and implementations
- `packages.businessservices` — Business logic layer (safe to extend)
- `packages.transferobjects` — Transfer Objects (DTOs)
- `packages.mappers` — Entity ↔ DTO mappers
- `packages.controllers` — REST controllers
- `packages.configurations` — Application configuration
- `packages.exceptions` — Exception handling
<#if graphqlEnabled>
- `packages.resolvers` — GraphQL resolvers and schema
</#if>

## Key Conventions

- **Do not manually edit generated files.** Re-run the generator instead (`mvn clean install -Pgenerate-resources`).
- Custom business logic belongs in the package mapped by `packages.businessservices` (or in new classes outside generated packages).
- `crud-spec.yaml` is the single source of truth for entity definitions and configuration.
- `generator-state.json` tracks generation state — do not delete it.
<#if migrationScripts>
<#if mongoDb>
- Never rename or reorder Mongock migration classes in `${mongoMigrationPath}`.
<#else>
- Never rename or reorder Flyway migration scripts in `src/main/resources/db/migration/`.
</#if>
</#if>
<#if cacheRequiresInfrastructure>
- Ensure ${cacheType} is running before starting the application — lazy-loaded relations and caching depend on it.
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
