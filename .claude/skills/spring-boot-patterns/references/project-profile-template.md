# Project profile template

Use this reference when creating `docs/project-profile.md`, when a required decision is missing from
it, or when a change makes one of its entries obsolete.

The profile is the single record of the decisions every other skill reads instead of guessing.
`spring-boot-patterns` owns the file; each skill named below owns the meaning of its own entries.
Copy the template, fill what the repository already proves, ask the user once for the rest, and
commit it before implementing.

## How to fill it

1. **Take what the repository proves.** A declared dependency, an applied migration, an existing package layout, or a configured datasource is an answer. Record it and move on.
2. **Ask once for the remainder.** One message listing every unresolved decision the task depends on, not one question per skill. Present the options this template lists so the user can answer in a word.
3. **Leave `UNDECIDED` for anything genuinely deferred**, and add a sentence saying what will force the decision. `UNDECIDED` is a legitimate value; a guessed value is not.
4. **Never infer a decision from a test dependency or an example.** An H2 dependency does not make H2 the database, and an example showing Lombok does not make Lombok a project choice.
5. **Update the entry in the same change that changes the decision.** A profile that disagrees with the code is worse than no profile.

## Template

````markdown
# Project profile

Last updated: YYYY-MM-DD

## Platform

| Decision | Value | Owner skill |
| --- | --- | --- |
| Java release | 21 | `build-and-dependencies` |
| Spring Boot version |  | `build-and-dependencies` |
| Build tool | Maven \| Gradle | `build-and-dependencies` |
| Uses Lombok | yes \| no | `build-and-dependencies` |
| Base package | com.example.myapp | `project-naming-conventions` |

## Persistence

| Decision | Value | Owner skill |
| --- | --- | --- |
| Database engine and major version |  | `spring-data-jpa` |
| Migration tool | Flyway \| Liquibase | `spring-data-jpa` |
| Entity accessor style | fluent \| void | `spring-data-jpa` |
| Identifier strategy |  | `spring-data-jpa` |

## Application design

| Decision | Value | Owner skill |
| --- | --- | --- |
| Application-service convention | interface + `*ServiceImpl` \| concrete service | `spring-boot-patterns` |
| API base path | /api/v1 | `spring-boot-patterns` |
| Error catalog type | `com.example.myapp.exception.ApplicationError` | `spring-boot-patterns` |
| Problem type base URI |  | `project-naming-conventions` |

## API contract

| Decision | Value | Owner skill |
| --- | --- | --- |
| Authoring direction | code-first \| contract-first | `rest-api-contract` |
| OpenAPI version | 3.0 \| 3.1 | `rest-api-contract` |
| Committed document path | src/main/resources/openapi/openapi.json | `rest-api-contract` |
| Document regeneration command |  | `rest-api-contract` |
| Published document location |  | `rest-api-contract` |
| Live API versions and retirement dates |  | `rest-api-contract` |
| Interactive UI exposed | never \| non-production only | `application-security` |
| Generated-type naming resolution | suffix \| interfaces only \| n/a | `rest-api-contract` |

## Security

| Decision | Value | Owner skill |
| --- | --- | --- |
| Token issuance profile | A: application-issued \| B: external IdP | `application-security` |
| Token issuer identifier |  | `application-security` |
| Self-registration exists | yes \| no | `application-security` |
| Management port |  | `application-security` |
| Management authority |  | `application-security` |

## Observability

| Decision | Value | Owner skill |
| --- | --- | --- |
| Log JSON format | ecs \| logstash \| gelf | `observability-and-logging` |
| Correlation header name | Correlation-Id | `observability-and-logging` |
| Tracing enabled | yes \| no \| UNDECIDED | `observability-and-logging` |
| Tracing bridge and exporter |  | `observability-and-logging` |
| Metrics registry | prometheus \| otlp \| elastic \| UNDECIDED | `observability-and-logging` |
| Exposed actuator endpoints | health,info | `observability-and-logging` |

## Caching

| Decision | Value | Owner skill |
| --- | --- | --- |
| Cache used | yes \| no \| UNDECIDED | none yet |
| Cache technology |  | none yet |

## Testing and build commands

| Decision | Value | Owner skill |
| --- | --- | --- |
| Integration test naming and phase | `*IntegrationTest`, Failsafe \| Gradle suite | `spring-boot-testing` |
| Database cleanup strategy | truncate after each method \| per-class container | `spring-boot-testing` |
| Unit and slice tests | `./mvnw test` | `spring-boot-testing` |
| Full verification | `./mvnw verify` | `spring-boot-testing` |
| Quality gate check | `./mvnw validate` | `build-and-dependencies` |
| Quality gate auto-fix | `./mvnw spotless:apply` | `build-and-dependencies` |

## Deferred decisions

| Decision | Deferred because | What will force it |
| --- | --- | --- |
|  |  |  |
````

## Notes on specific entries

- **Token issuance profile** determines how integration tests obtain a credential. Until it is decided, `spring-boot-testing` permits a documented temporary test-only issuer; record that here as a deferred decision with its removal condition.
- **Entity accessor style** and **application-service convention** both change generated code shape, so a project that leaves them unrecorded will produce a different shape per feature.
- **Caching has no owner skill yet.** No skill in this set decides whether the project uses a cache or which technology it uses; a caching skill will own that. Record the answer here when it is made, and leave both rows `UNDECIDED` until then. Do not introduce a cache to fill the row.
  - `application-security` owns what may be cached and under what conditions: classification of cached values, TTL, tenant scope, serialization, and eviction of sensitive data. It does not own the decision itself.
  - `project-naming-conventions` owns cache and cache-key names.
- **Management authority** depends on whether a custom authority converter is installed; record the literal value the configuration uses, not the scope name.
- **Quality gate commands** exist so that the first response to a failed gate is to run the fixer rather than to disable the gate.
- **Authoring direction** must be decided before the first endpoint. It cannot be switched later without a dedicated project, and under contract-first it also forces the generated-type naming resolution. `rest-api-contract` presents the trade-off; the user chooses.
