# Project profile

Last updated: YYYY-MM-DD

> Copy this file to `docs/project-profile.md` and fill it in. Every row carries a decision token:
> `ASK` must be answered by the user before dependent work starts, `RESOLVE` is looked up and
> recorded by whoever sets the project up, and `UNDECIDED` is a legitimate deferral that names what
> will force the decision. Never replace a token with a remembered value.
>
> A few `ASK` rows carry a **fallback**. That is the only sanctioned way to proceed without an
> answer: apply the fallback, write it into the `Value` column, and state in the handoff that a
> fallback was applied so the user overrides once instead of being asked every time. A row with an
> empty fallback blocks, with no exception. No skill may invent a fallback that is not written here.

## Platform

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Skill set revision this project follows | `RESOLVE` |  | the tag or commit of the skill set this project was built against | all skills |
| Java release | `RESOLVE` |  | 21 minimum | `build-and-dependencies` |
| Spring Boot generation | `ASK` |  | 3 \| 4 | `build-and-dependencies` |
| Spring Boot version | `RESOLVE` |  | the full version, including the minor line — rules branch on the generation, coordinates can differ per minor line | `build-and-dependencies` |
| Support end date of that branch | `RESOLVE` |  | looked up at setup, never remembered | `build-and-dependencies` |
| Support model | `ASK` |  | open source \| commercial subscription | `build-and-dependencies` |
| Version support re-check date | `RESOLVE` |  | when the two rows above are looked up again | `build-and-dependencies` |
| Build tool | `ASK` |  | Maven \| Gradle | `build-and-dependencies` |
| Uses Lombok | `ASK` | no | yes \| no | `build-and-dependencies` |
| Base package | `ASK` |  | com.example.myapp | `project-naming-conventions` |
| Maximum line length | `ASK` | 120 | the value the committed Checkstyle configuration enforces | `build-and-dependencies` |
| Nullability enforcement | `ASK` | IDE and review | IDE and review \| NullAway on Error Prone | `build-and-dependencies` |

## Persistence

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Database engine and major version | `ASK` |  |  | `spring-data-jpa` |
| Migration tool | `ASK` |  | Flyway \| Liquibase | `sql-database-migration` |
| Migration identifier scheme | `ASK` | UTC timestamp | UTC timestamp \| sequential counter | `sql-database-migration` |
| Migration user separate from application user | `ASK` | no | yes \| no | `sql-database-migration` |
| Migration datasource separate from the application pool | `ASK` | yes | yes \| no — a shared pool makes migrations inherit the request-sized statement and idle-in-transaction bounds, which cancels a large schema change at deploy time | `spring-data-jpa` |
| Entity accessor style | `ASK` |  | fluent \| void | `spring-data-jpa` |
| Identifier strategy | `ASK` |  |  | `spring-data-jpa` |
| Stale-write protection | `ASK` | server retry only | server retry only \| version field in update TO \| ETag + If-Match | `spring-data-jpa` |
| Optimistic retry policy | `ASK` | 3 attempts, 50 ms initial backoff, 2s budget | attempts, initial backoff, budget | `spring-data-jpa` |
| Pessimistic lock timeout | `ASK` | 3s | one duration, bound as configuration and read by both the database mechanism and the `Retry-After` header rather than written down twice; stays below the statement timeout. The mechanism that applies it is engine-specific — the JPA hint works on few engines | `spring-data-jpa` |

## Application design

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Service interface convention | `ASK` | concrete classes | interface + `*Impl` \| concrete classes | `spring-boot-patterns` |
| Aggregate roots and their tables | `ASK` |  | list, e.g. `User (users, user_address)`, `Organization (organization)` | `spring-boot-patterns` |
| Reliable-delivery mechanism for external effects | `ASK` |  | after-commit listener only \| outbox table \| broker-native transaction | `spring-boot-patterns` |
| Message broker | `ASK` | none | none \| UNDECIDED \| the broker and its major version | none yet |
| Message ordering guarantee required | `ASK` | none | none \| per key \| global | none yet |
| Resilience library | `ASK` | none | none \| Resilience4j \| other | `spring-boot-patterns` |
| Outbound timeout budget | `ASK` | connect 2s, read 5s, request budget 10s | e.g. connect 2s, read 5s, request budget 10s | `spring-boot-patterns` |
| API base path | `ASK` | /api/v1 | /api/v1 | `spring-boot-patterns` |
| Error catalog type | `ASK` |  | `com.example.myapp.exception.ApplicationError` | `spring-boot-patterns` |
| Problem type base URI | `ASK` |  |  | `project-naming-conventions` |

## API contract

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Contract document | `ASK` |  | OpenAPI \| none | `rest-api-contract` |
| Authoring direction | `ASK` |  | code-first \| contract-first \| n/a | `rest-api-contract` |
| OpenAPI version | `ASK` | 3.1 | 3.0 \| 3.1 \| n/a | `rest-api-contract` |
| API versioning strategy | `ASK` | URI path | URI path \| header \| query parameter \| media type | `rest-api-contract` |
| Known consumers | `ASK` |  |  | `rest-api-contract` |
| Committed document path | `ASK` | src/main/resources/openapi/openapi.json | one path; its `.json` or `.yaml` extension selects the format | `rest-api-contract` |
| Document regeneration command | `RESOLVE` |  |  | `rest-api-contract` |
| Published document location | `ASK` |  |  | `rest-api-contract` |
| Live API versions and retirement dates | `ASK` |  |  | `rest-api-contract` |
| Interactive UI exposed | `ASK` | never | never \| non-production only | `rest-api-contract` |
| Generated-type naming resolution | `ASK` |  | suffix \| interfaces only \| n/a | `rest-api-contract` |
| Generator Jackson binding | `ASK` |  | jackson 3 \| jackson 2 \| n/a &mdash; Spring Boot 4 with contract-first only; verified against the generated imports, never assumed | `rest-api-contract` |

## Security

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Token issuance profile | `ASK` |  | A: application-issued \| B: external IdP | `application-security` |
| Token issuer identifier | `ASK` |  |  | `application-security` |
| Self-registration exists | `ASK` |  | yes \| no | `application-security` |
| Management port | `ASK` |  |  | `application-security` |
| Management authority | `ASK` |  |  | `application-security` |

## Observability

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Log JSON format | `ASK` | ecs | ecs \| logstash \| gelf | `observability-and-logging` |
| Correlation header name | `ASK` | Correlation-Id | Correlation-Id | `observability-and-logging` |
| Tracing enabled | `ASK` |  | yes \| no \| UNDECIDED | `observability-and-logging` |
| Telemetry export model | `ASK` |  | Prometheus scrape \| OTLP push \| other \| UNDECIDED | `observability-and-logging` |
| Telemetry wiring | `RESOLVE` |  | follows the Spring Boot generation above | `observability-and-logging` |
| Exposed actuator endpoints | `ASK` | health,info | health,info | `observability-and-logging` |

## Caching

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Cache used | `ASK` | no | yes \| no \| UNDECIDED — gates every row below it | `application-caching` |
| Cache technology | `ASK` |  | the store, or `UNDECIDED`. Design does not wait for it; configuration does | `application-caching` |
| Cache topology | `ASK` |  | local \| distributed \| near — a separate decision from the technology, and the one that decides whether an eviction on one instance is seen by another | `application-caching` |
| Cache failure mode | `ASK` | fail open | fail open \| fail closed — fail open needs an error handler, or a cache outage becomes an application outage | `application-caching` |
| Cache operation timeout | `ASK` | 50ms | the bound on one cache read, write, or evict; well inside the request budget, because a hanging cache costs the wait *and* the work it was meant to avoid | `application-caching` |
| Cached value serialization | `ASK` |  | the format and its configuration; never Java serialization. Changes here move the key namespace version | `application-caching` |
| Cache key namespace | `ASK` |  | the prefix and version segment every key carries | `project-naming-conventions` |
| Invalidation shape | `ASK` | after-commit listener | after-commit listener \| transaction-aware cache manager — one for the whole project, because a codebase with both has no rule | `application-caching` |
| Source survives a cold cache | `ASK` |  | yes \| no. `no` makes the cache part of the availability design, not an optimization, and changes how a deploy is sequenced | `application-caching` |
| Hibernate second-level cache | `ASK` | no | no \| the entities cached and the concurrency strategy per region. It is a cache and every row above applies to it | `application-caching` |
| Hibernate query cache | `ASK` | no | no \| the queries cached, with the measurement that justified each | `application-caching` |

**Cache register.** One row per cache. `application-caching` owns the columns; a cache that is not
here does not exist.

| Cache | Contents | Justification | Staleness budget | TTL | Invalidation | Key inputs | Size bound |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |

> While `Cache used` is `no` or `UNDECIDED`, do not introduce a cache, a cache annotation, or a
> cache dependency into the project — including the Hibernate second-level and query caches, which
> are switched on by a property that reads like tuning. `application-caching` owns that guard and
> states what to do once the answer is `yes`.

> The two messaging rows under **Application design** carry the same guard for the same reason.
> Security, naming, layering, and testing of messaging are owned; the delivery and consumer
> *mechanism* is not. While `Message broker` is `none` or `UNDECIDED`, do not introduce a broker, a
> listener, or a messaging dependency. `_core/README.md` lists exactly which parts are owned and by
> whom, so an owned rule is not mistaken for a missing one.

## Performance and capacity

These are the **bounds the code enforces**, not performance targets. Each one turns an unbounded
wait or an unbounded result into a failure someone chose, which is what keeps a slow dependency from
becoming a held connection and a held connection from becoming an outage.

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Concurrency model | `ASK` | platform threads, pool recorded below | virtual threads \| platform threads — changing it moves the limit on concurrency rather than removing it, so the database pool, the per-caller limits, and the outbound client pools are re-derived with it | `spring-boot-patterns` |
| Server thread pool size | `ASK` | the server default, recorded explicitly | maximum in-flight requests when the model is platform threads; `n/a` on virtual threads, where the database pool is the admission control instead | `spring-boot-patterns` |
| Ingress request ceiling | `ASK` |  | the wall-clock timeout the platform enforces in front of the application. No fallback: nothing inside the application bounds a synchronous request, so this number and the parts arithmetic are the whole budget | `spring-boot-patterns` |
| Response compression | `ASK` | at the ingress, not in the application | ingress \| application \| none | `spring-boot-patterns` |
| Conditional reads on polled endpoints | `ASK` | no | yes \| no — validated before the representation is built, against a version the aggregate already keeps | `spring-boot-patterns` |
| Shutdown grace period | `ASK` | 20s | above the request budget, and below the platform's own termination grace period | `spring-boot-patterns` |
| Maximum page size | `ASK` | 100 | the value the shared bound constant declares, enforced at the REST boundary and on the service contract | `spring-data-jpa` |
| Statement timeout | `ASK` | 5s | per-statement ceiling, applied at the connection level so it covers every statement the connection carries; stays below the request budget. Not every engine has one — record the gap where it does not | `spring-data-jpa` |
| Idle-in-transaction timeout | `ASK` | 10s | how long an open transaction may sit between statements before the engine terminates the session; `none` where the engine has no equivalent | `spring-data-jpa` |
| Connection-level settings channel | `RESOLVE` |  | the one driver mechanism that carries the statement, lock, and idle-in-transaction settings together; follows the engine and driver, and there is exactly one per pool | `spring-data-jpa` |
| Transaction timeout | `ASK` | the recorded request budget | project-wide ceiling for a whole transaction, tightened per use case where needed; never below the statement timeout, and required for reads as well as writes | `spring-data-jpa` |
| Connection pool size | `ASK` |  | maximum pool size. No fallback: it depends on the engine's own connection limit and on how many instances share it, and a guessed value either starves the application or overloads the database | `spring-data-jpa` |
| Maximum wait for a connection | `ASK` | 2s | how long a caller waits for a pooled connection before failing. Short on purpose — a long wait converts pool exhaustion into a stalled request nobody times out | `spring-data-jpa` |
| JDBC batch size | `ASK` | none | batching is enabled deliberately and verified against the generated SQL, never assumed from `saveAll` | `spring-data-jpa` |

> The request budget these rows sit inside is the **Outbound timeout budget** row under
> **Application design**, which `spring-boot-patterns` owns. Read it first: every value here has to
> fit within it, and a bound larger than the budget is a bound the caller never waits for.

## Testing and build commands

| Decision | Token | Fallback | Value | Owner skill |
| --- | --- | --- | --- | --- |
| Integration test naming and phase | `ASK` | `*IntegrationTest` | `*IntegrationTest`, Failsafe \| Gradle suite | `spring-boot-testing` |
| Database cleanup strategy | `ASK` | truncate after each method | truncate after each method \| per-class container | `spring-boot-testing` |
| Assertion library | `ASK` | AssertJ | AssertJ \| JUnit assertions | `spring-boot-testing` |
| Test-data generator | `ASK` | focused hand-written factories | focused hand-written factories \| Instancio \| other | `spring-boot-testing` |
| Unit and slice tests | `RESOLVE` |  | e.g. `./mvnw test` | `spring-boot-testing` |
| Full verification | `RESOLVE` |  | e.g. `./mvnw verify` | `spring-boot-testing` |
| Quality gate check | `RESOLVE` |  | e.g. `./mvnw validate` | `build-and-dependencies` |
| Quality gate auto-fix | `RESOLVE` |  | e.g. `./mvnw spotless:apply` | `build-and-dependencies` |

## Resolved tool versions

Filled by `build-and-dependencies` when the build is set up. Each is a `RESOLVE`; an unresolved one
stays `UNDECIDED` with its reason, never a remembered number.

| Artifact | Token | Version | Resolved on |
| --- | --- | --- | --- |
| Spring Boot | `RESOLVE` |  |  |
| Checkstyle, the tool | `RESOLVE` |  |  |
| Checkstyle build plugin | `RESOLVE` |  |  |
| Spotless | `RESOLVE` |  |  |
| JSpecify | `RESOLVE` |  |  |
| Surefire, on Maven | `RESOLVE` |  |  |
| Failsafe, on Maven | `RESOLVE` |  |  |
| Enforcer, on Maven | `RESOLVE` |  |  |
| MapStruct | `RESOLVE` |  |  |
| Lombok–MapStruct binding, if Lombok is used | `RESOLVE` |  |  |
| JPA static metamodel processor, if Specifications or Criteria are used | `RESOLVE` |  |  |
| Retry engine, if the project declares one | `RESOLVE` |  |  |
| OpenAPI generator, if contract-first | `RESOLVE` |  |  |
| springdoc, if code-first with an OpenAPI document | `RESOLVE` |  |  |

> The Spring Boot BOM manages some of these and not others, and which is which changes between
> generations. A row is filled with the version the build actually resolves, not with the version
> someone intended; `build-and-dependencies` states where each one comes from.

## Deferred decisions

| Decision | Deferred because | What will force it |
| --- | --- | --- |
|  |  |  |
