# Ownership map

This is the single canonical statement of which skill owns which topic. Every skill links here
instead of restating the full map, so an ownership change is one edit rather than twelve.

Each skill keeps a short table of the owners it defers to most often. That table is a convenience
pointer, never a redefinition: when a skill's local table and this file disagree, **this file wins**
and the skill's table is the defect to fix.

## The map

| Owner | Owns |
| --- | --- |
| `modern-java-21` | Java language use, imports, Javadoc, nullability, exception mechanics, type and method design, source structure. Applies to every touched `.java` file, production and test |
| `project-decision-profile` | `docs/project-profile.md`, the `ASK` / `RESOLVE` / `UNDECIDED` tokens, the fallback rule, and the order of work for filling a row. Owns the mechanism only; each row's named owner decides what its value means |
| `spring-boot-patterns` | Controllers, TOs, services and their two levels, domain models, mappers, validation, the error contract, configuration design, package responsibilities, outbound-call structure, **where the transaction boundary sits**, and the application's runtime shape above the database: the concurrency model, the request budget, conditional reads, compression, and shutdown |
| `spring-data-jpa` | Entities, repositories, queries, projections, fetch plans, locking, database performance, and **transaction behavior inside the boundary**: propagation, isolation, `readOnly`, flush timing |
| `sql-database-migration` | Migration files, ordering, immutability, expand-and-contract, backfills, seed data, clean-install verification |
| `rest-api-contract` | The public contract and its document: completeness, required-ness and nullability, breaking-change judgement, versioning, deprecation, drift |
| `application-security` | Trust boundaries, authentication, authorization, tenant and object ownership, confidentiality and data classification, secrets, cryptography, dangerous sinks, abuse prevention, supply chain, and security verification scenarios |
| `spring-boot-testing` | Test levels and placement, scenario selection, fixtures, doubles, isolation, determinism, execution |
| `observability-and-logging` | What must be instrumented and how: log levels and placement, correlation context, meters and tag cardinality, tracing, actuator endpoints, probes |
| `build-and-dependencies` | Build files, dependency and plugin declarations, **all version selection**, compiler and annotation-processor configuration, test-phase separation, quality gates |
| `project-naming-conventions` | Every developer-owned name and every rename migration: identifiers, packages, tests, REST paths and fields, database objects, configuration keys, meters, spans, log fields |
| `application-caching` | Cache design, independent of the store: what may be cached and why, key identity, staleness budget and TTL, invalidation and its ordering against a transaction, sizing and eviction, serialization of cached values, failure behavior, and the Hibernate second-level cache |
| `spring-boot-code-review` | Review scope, evidence, severity, reporting, merge readiness. Never a second coding standard |

## Split topics

These are the boundaries that get misread. Each row is one topic with two owners and a clean seam.

| Topic | Owner of the rule | Owner of the surrounding decision |
| --- | --- | --- |
| Transactions | `spring-data-jpa` owns what the settings mean | `spring-boot-patterns` owns which method carries them |
| Locking | `spring-data-jpa` owns `@Version`, lock modes, the conditional-`UPDATE` alternative, and the retry mechanism | `spring-boot-patterns` owns which layer the retry annotation sits on |
| Logging in a Java file | `observability-and-logging` owns level, placement, and fields | `application-security` owns what may never appear |
| Actuator endpoints | `observability-and-logging` owns which are exposed | `application-security` owns how the exposed set is protected |
| Interactive API documentation | `rest-api-contract` owns whether it is exposed | `application-security` owns how it is protected |
| Meters, spans, log fields | `observability-and-logging` owns which must exist | `project-naming-conventions` owns what they are called |
| Migration wiring | `sql-database-migration` owns that a migration must exist | `build-and-dependencies` owns the dependency that runs it |
| Security test scenarios | `application-security` owns which scenarios are required | `spring-boot-testing` owns the level each runs at |
| Contract assertions | `rest-api-contract` owns what must be asserted | `spring-boot-testing` owns the level it runs at |
| Idempotency | `application-security` owns the policy | `spring-boot-patterns` owns where it lives in the layers |
| Authentication and authorization failures | `application-security` owns the `401` and `403` responses, which the filter chain produces | `spring-boot-patterns` owns the error catalog they are built from, and the two advice handlers that decline both denial families so the chain still sees them |
| The pessimistic lock timeout | `spring-data-jpa` owns the value and how it reaches the database | `spring-boot-patterns` owns the `Retry-After` header, derived from that value and never retyped |
| Nullability in Java code | `modern-java-21` owns the annotation convention and where it goes | `build-and-dependencies` owns the artifact, its version, and how hard the contract is checked |
| Nullability in the published contract | `rest-api-contract` owns whether a field may be absent or null on the wire | `modern-java-21` owns how the Java declaration behind it is annotated, which is not the same question |
| Cache keys | `application-caching` owns the key's **identity** — every input that varies the value, tenant and authorizing subject included | `project-naming-conventions` owns the cache name and the key's textual form |
| What may be cached | `application-security` owns classification, the values that may never be cached, and abuse limits on cache growth | `application-caching` owns everything about the entry once the value is permitted |
| Cache invalidation | `application-caching` owns that it happens after commit and what it must cover | `spring-boot-patterns` owns the after-commit mechanism it uses and the layer the trigger sits in |
| The second-level cache | `application-caching` owns that it is a cache, its concurrency strategy, and the query-cache decision | `spring-data-jpa` owns the mapping, the regions, and the provider settings |
| Profile decisions | `project-decision-profile` owns the file, the three tokens, the fallback rule, and whether a missing row blocks | the row's named owner decides what its value means; `build-and-dependencies` classifies every build and version row |
| Versions of anything | `build-and-dependencies` owns every version choice | no other skill selects a version |
| Resource bounds | `spring-data-jpa` owns the database-side bounds: statement and transaction timeouts, pool size, connection wait, batch size, page size | `spring-boot-patterns` owns the request budget they all fit inside, and `application-security` owns the per-caller limits that stop one client consuming them |
| Concurrency at the request boundary | `spring-boot-patterns` owns the model — virtual threads or a sized platform pool — and the fact that changing it moves the limit rather than removing it | `spring-data-jpa` owns the database pool that becomes the limit, and `application-security` owns the per-caller limits that become the other one |
| Conditional reads | `spring-boot-patterns` owns where the `If-None-Match` check sits and that it runs before the representation is built | `rest-api-contract` owns whether the `ETag` and `304` are part of the published contract, and `spring-data-jpa` owns the version the validator is derived from |

## Precedence when two skills genuinely conflict

Report the conflict rather than inventing a third standard. When the work cannot wait for an answer,
resolve it in this order and say in the handoff which rule was applied and which was set aside:

1. **`application-security`.** A control is never weakened to satisfy another rule. Where two
   readings are compatible, take the stricter one.
2. **`sql-database-migration` and `rest-api-contract`.** These describe commitments already deployed
   to somebody else — a schema in a running database, a payload a consumer parses. Breaking one is a
   deployment of another team's software.
3. **`spring-boot-patterns`.** The architecture the rest of the set assumes.
4. **The topic owner from the map above.**
5. **`project-naming-conventions` and `modern-java-21`.** Style and vocabulary yield to correctness,
   never the reverse.

`spring-boot-code-review` never wins a precedence contest, because it states no rule of its own. It
reports the conflict.
