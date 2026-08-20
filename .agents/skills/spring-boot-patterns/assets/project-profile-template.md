# Project profile

Last updated: YYYY-MM-DD

> Copy this file to `docs/project-profile.md` and fill it in. Every row carries a decision token:
> `ASK` must be answered by the user before dependent work starts, `RESOLVE` is looked up and
> recorded by whoever sets the project up, and `UNDECIDED` is a legitimate deferral that names what
> will force the decision. Never replace a token with a remembered value.

## Platform

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Skill set revision this project follows | `RESOLVE` | e.g. 1.0.0, from the `metadata.version` entry in any skill's frontmatter; if the loader in use does not expose it, read it from the file | all skills |
| Java release | `RESOLVE` | 21 minimum | `build-and-dependencies` |
| Spring Boot generation | `ASK` | 3 \| 4 | `build-and-dependencies` |
| Spring Boot version | `RESOLVE` | with the date its branch loses support | `build-and-dependencies` |
| Build tool | `ASK` | Maven \| Gradle | `build-and-dependencies` |
| Uses Lombok | `ASK` | yes \| no | `build-and-dependencies` |
| Base package | `ASK` | com.example.myapp | `project-naming-conventions` |

## Persistence

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Database engine and major version | `ASK` |  | `spring-data-jpa` |
| Migration tool | `ASK` | Flyway \| Liquibase | `sql-database-migration` |
| Migration identifier scheme | `ASK` | UTC timestamp \| sequential counter | `sql-database-migration` |
| Migration user separate from application user | `ASK` | yes \| no | `sql-database-migration` |
| Entity accessor style | `ASK` | fluent \| void | `spring-data-jpa` |
| Identifier strategy | `ASK` |  | `spring-data-jpa` |

## Application design

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Service interface convention | `ASK` | interface + `*Impl` \| concrete classes | `spring-boot-patterns` |
| Aggregate roots and their tables | `ASK` | list, e.g. `User (users, user_address)`, `Organization (organization)` | `spring-boot-patterns` |
| Reliable-delivery mechanism for external effects | `ASK` | after-commit listener only \| outbox table \| broker-native transaction | `spring-boot-patterns` |
| Resilience library | `ASK` | none \| Resilience4j \| other | `spring-boot-patterns` |
| Outbound timeout budget | `ASK` | e.g. connect 2s, read 5s, request budget 10s | `spring-boot-patterns` |
| API base path | `ASK` | /api/v1 | `spring-boot-patterns` |
| Error catalog type | `ASK` | `com.example.myapp.exception.ApplicationError` | `spring-boot-patterns` |
| Problem type base URI | `ASK` |  | `project-naming-conventions` |

## API contract

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Contract document | `ASK` | OpenAPI \| none | `rest-api-contract` |
| Authoring direction | `ASK` | code-first \| contract-first \| n/a | `rest-api-contract` |
| OpenAPI version | `ASK` | 3.0 \| 3.1 \| n/a | `rest-api-contract` |
| API versioning strategy | `ASK` | URI path \| header \| query parameter \| media type | `rest-api-contract` |
| Known consumers | `ASK` |  | `rest-api-contract` |
| Committed document path | `ASK` | src/main/resources/openapi/openapi.json | `rest-api-contract` |
| Document regeneration command | `RESOLVE` |  | `rest-api-contract` |
| Published document location | `ASK` |  | `rest-api-contract` |
| Live API versions and retirement dates | `ASK` |  | `rest-api-contract` |
| Interactive UI exposed | `ASK` | never \| non-production only | `rest-api-contract` |
| Generated-type naming resolution | `ASK` | suffix \| interfaces only \| n/a | `rest-api-contract` |

## Security

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Token issuance profile | `ASK` | A: application-issued \| B: external IdP | `application-security` |
| Token issuer identifier | `ASK` |  | `application-security` |
| Self-registration exists | `ASK` | yes \| no | `application-security` |
| Management port | `ASK` |  | `application-security` |
| Management authority | `ASK` |  | `application-security` |

## Observability

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Log JSON format | `ASK` | ecs \| logstash \| gelf | `observability-and-logging` |
| Correlation header name | `ASK` | Correlation-Id | `observability-and-logging` |
| Tracing enabled | `ASK` | yes \| no \| UNDECIDED | `observability-and-logging` |
| Telemetry export model | `ASK` | Prometheus scrape \| OTLP push \| other \| UNDECIDED | `observability-and-logging` |
| Telemetry wiring | `RESOLVE` | follows the Spring Boot generation above | `observability-and-logging` |
| Exposed actuator endpoints | `ASK` | health,info | `observability-and-logging` |

## Caching

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Cache used | `ASK` | yes \| no \| UNDECIDED | none yet |
| Cache technology | `ASK` |  | none yet |

## Testing and build commands

| Decision | Token | Value | Owner skill |
| --- | --- | --- | --- |
| Integration test naming and phase | `ASK` | `*IntegrationTest`, Failsafe \| Gradle suite | `spring-boot-testing` |
| Database cleanup strategy | `ASK` | truncate after each method \| per-class container | `spring-boot-testing` |
| Unit and slice tests | `RESOLVE` | e.g. `./mvnw test` | `spring-boot-testing` |
| Full verification | `RESOLVE` | e.g. `./mvnw verify` | `spring-boot-testing` |
| Quality gate check | `RESOLVE` | e.g. `./mvnw validate` | `build-and-dependencies` |
| Quality gate auto-fix | `RESOLVE` | e.g. `./mvnw spotless:apply` | `build-and-dependencies` |

## Resolved tool versions

Filled by `build-and-dependencies` when the build is set up. Each is a `RESOLVE`; an unresolved one
stays `UNDECIDED` with its reason, never a remembered number.

| Artifact | Token | Version | Resolved on |
| --- | --- | --- | --- |
| Spring Boot | `RESOLVE` |  |  |
| Checkstyle | `RESOLVE` |  |  |
| Spotless | `RESOLVE` |  |  |
| MapStruct | `RESOLVE` |  |  |
| Lombok–MapStruct binding, if Lombok is used | `RESOLVE` |  |  |
| OpenAPI generator, if contract-first | `RESOLVE` |  |  |

## Deferred decisions

| Decision | Deferred because | What will force it |
| --- | --- | --- |
|  |  |  |
