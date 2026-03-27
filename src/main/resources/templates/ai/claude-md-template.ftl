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
<#if migrationScripts>

Database schema is managed by **Flyway**. Migration scripts are in `src/main/resources/db/migration/`. Never manually rename or reorder migration files.
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
docker run -p ${appPort}:${appPort} ${artifactId}
```
</#if>
</#if>
<#if openApiEnabled>

## API Documentation

Swagger UI is available at: `http://localhost:${appPort}/swagger-ui/index.html`
</#if>
<#if graphqlEnabled>

## GraphQL

GraphQL playground is available at: `http://localhost:${appPort}/graphiql`
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

- `model/` — JPA entities
- `repository/` — Spring Data repositories
- `service/` — Service interfaces and implementations
- `businessservice/` — Business logic layer (safe to extend)
- `to/` — Transfer Objects (DTOs)
- `mapper/` — Entity ↔ DTO mappers
- `controller/` — REST controllers
- `config/` — Application configuration
- `exception/` — Exception handling
<#if graphqlEnabled>
- `graphql/` — GraphQL resolvers and schema
</#if>

## Key Conventions

- **Do not manually edit generated files.** Re-run the generator instead (`mvn clean install -Pgenerate-resources`).
- Custom business logic belongs in the `businessservice/` layer or in new classes outside generated packages.
- `crud-spec.yaml` is the single source of truth for entity definitions and configuration.
- `generator-state.json` tracks generation state — do not delete it.
<#if migrationScripts>
- Never rename or reorder Flyway migration scripts in `src/main/resources/db/migration/`.
</#if>
<#if cacheRequiresInfrastructure>
- Ensure ${cacheType} is running before starting the application — lazy-loaded relations and caching depend on it.
</#if>
