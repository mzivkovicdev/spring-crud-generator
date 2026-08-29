---
name: spring-data-jpa
description: Spring Data JPA and Hibernate patterns for Java 21+ applications on any supported relational database. Use whenever code touches JPA entities, repositories, Specifications, EntityManager, database reads or writes, or transactional behavior. Covers mapping, associations, fetch plans, queries, transaction behavior, pagination, locking, and persistence tests. Schema migration files belong to sql-database-migration.
---

# Spring Data JPA Skill

Design persistence for correctness, predictable SQL, and verified performance. JPA does not remove the need to understand relational modeling, indexes, query plans, transactions, and locking.

## Coordination with other skills

This skill owns JPA and database behavior beneath the service boundary. Use the architecture,
terminology, mapper directions, and package responsibilities from `spring-boot-patterns`.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what and carries
the precedence order for a genuine conflict. Read it there rather than from a copy here. The seams
this skill crosses most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| Transactions | what the settings mean: propagation, isolation, `readOnly`, flush | `spring-boot-patterns` owns which method carries them |
| Locking | `@Version`, lock modes, and the retry mechanism | `spring-boot-patterns` owns which layer the retry annotation sits on |
| Schema | what the schema must look like for a mapping to work | `sql-database-migration` owns the migration file that creates it |
| Persistence tests | which JPA scenarios need proof | `spring-boot-testing` owns scope, fixtures, and execution |

This skill is database-agnostic. Inspect the configured database and Hibernate dialect before using
vendor-specific SQL, types, indexes, hints, locking options, migration syntax, or identifier
strategies.

## Reference routing

The references are part of this skill's rules, not illustrations of them, and several rule sets live
only there. Read the one the change touches, and only that one:

- [Entity and query examples](references/entity-and-query-examples.md) for mappings, association ownership, repository design, projections, dynamic and sargable queries, pagination and scrolling, SQL and index performance, and the read-side anti-patterns.
- [Locking and retry examples](references/locking-and-retry-examples.md) for optimistic and pessimistic locking, the composed retry annotation for both generations, lock timeouts and ordering, and the concurrency anti-patterns.
- [Write behavior examples](references/write-behavior-examples.md) for flush timing, bulk DML, large batches, and the write-side anti-patterns.

## Before changing persistence

Read `docs/project-profile.md` first, whose template `project-decision-profile` owns. It records the
relational database engine and major version, the entity accessor style, and the identifier
strategy. When it does not, or when the repository contains no database dependency and no datasource
configuration, **ask the user which database engine and version the project uses, and record the
answer in the profile before writing persistence code**. That is an `ASK` decision in the token
vocabulary `project-decision-profile` defines: it blocks. Never pick a database, a dialect, or an
identifier strategy by default, and never infer the production database from a test dependency such
as H2. `sql-database-migration` settles which migration tool the project uses.

`build-and-dependencies` owns the driver, migration-tool, and annotation-processor declarations this
skill depends on; state the requirement to it rather than editing a build file from here. This skill
owns the persistence behavior they enable.

Then inspect:

1. Spring Boot, Spring Data JPA, Jakarta Persistence, Hibernate, driver, database, and migration-tool versions;
2. entity mappings, association ownership, converters, listeners, inheritance, identifier generation, and equality;
3. service transaction boundaries and every caller the change affects;
4. migrations, constraints, indexes, column types, defaults, and expected data volume;
5. query cardinality, selectivity, ordering, pagination, read/write ratio, and concurrency;
6. generated SQL and execution plans for important access paths;
7. repository, migration, locking, query-count, and database integration tests.

Do not copy a nearby persistence pattern before understanding its generated SQL and lifecycle behavior.

## Spring Boot 3 and 4

Both generations are supported, and `docs/project-profile.md` records which one applies. No mapping,
association, query, transaction, or locking rule changes between them: JPA semantics are the same,
and a correct entity stays correct. Only naming and versions differ — the Jakarta Persistence and
Hibernate lines, the `@EntityScan` package, the exception-translation property, the static metamodel
processor artifact, and the Spring Data JPA line. `build-and-dependencies` owns all of those
coordinates in
[generation differences](../build-and-dependencies/references/generation-differences.md), and the
effective versions are read from the build, never from a written-down table.

A provider major version is not a formatting change. Hibernate 7 tightens specification conformance
where Hibernate 6 was lenient, so a mapping, an HQL query, or a lifecycle assumption that worked
before can now be rejected or produce different SQL. On an upgrade, verify the generated SQL and the
execution plans for the important access paths again rather than assuming the previous verification
still holds. On a new project, this is simply the behavior being designed against.

## Entity mapping

- Never use records as entities. A record may be an embeddable only when the configured provider and project version support it.
- Keep entities and persistent accessors non-final unless verified bytecode enhancement removes proxy limitations.
- Provide a `protected` no-argument constructor when possible.
- Use field or property access consistently, and place mapping annotations according to the chosen strategy.
- Use one accessor style across every entity, recorded in `docs/project-profile.md`. The examples use fluent setters returning the entity; plain `void` setters are equally acceptable. Never mix the two.
- Expose the getters persistence-to-domain mapping requires. Field access needs no public accessors, but mapping code must still be able to read the selected state.
- Keep entity mappings, migrations, and database definitions aligned for names, nullability, length, precision, scale, uniqueness, defaults, foreign keys, and indexes.
- Database constraints enforce integrity; application validation does not replace them.
- Use `@Version` when concurrent updates must not silently overwrite each other, and never modify the version value in application code.
- Prefer `EnumType.STRING`; treat enum renames as data migrations.
- Define timestamp/timezone policy explicitly, and use `BigDecimal` precision and scale for fixed-decimal columns.
- Choose identifier generation for the actual database and verify its effect on batching and round trips.
- Never use Lombok `@Data` on entities.
- Keep entity listeners limited to persistence concerns; never perform repository or remote calls from callbacks.
- Implement `equals` and `hashCode` explicitly using the strategy below, excluding lazy associations and mutable state from them and from `toString`. Never let Lombok, an IDE template, or a record-like default generate them, and never leave the JVM identity default in place when instances enter a `Set`, a `Map`, or a bidirectional collection. Test equality across transient, managed, detached, and proxied instances.

### Entity equality strategy

Choose per entity, in this order:

1. **Stable natural key.** When the entity has an immutable, non-null business key assigned before persistence — an ISO country code, an externally issued order number — compare on that key and derive `hashCode` from it. Preferred, because the contract then holds in every state.
2. **Surrogate identifier with a constant hash.** Otherwise compare on the surrogate identifier and return a constant `hashCode`. A constant hash is required, not a shortcut: the identifier is null before persistence and assigned afterwards, so any identifier-derived hash changes while the instance sits in a hash-based collection.

Rules for the surrogate strategy, shown as working code on `UserEntity` in
[entity and query examples](references/entity-and-query-examples.md):

- `instanceof` with pattern matching is the type check: it accepts a provider proxy of the same entity, so no provider-specific unwrapping is needed. Read the other identifier through its getter, never the field, so a proxy resolves.
- Two transient instances are never equal, and a transient instance is never equal to a persisted one. That is the intended contract.
- Return a constant class-derived `hashCode`. Not `Objects.hash(id)`, which breaks on persist, and not `getClass().hashCode()`, which differs between an entity and its proxy.
- Exclude mutable columns, versions, associations, and collections from both methods, and apply the same strategy to every entity so collection behavior is uniform.

## Association ownership

`spring-boot-patterns` decides which entities form one aggregate, and that decision constrains every
mapping: an association may only exist inside an aggregate. The rules that follow from it — the
identifier reference across a boundary, values copied so history cannot change retroactively,
explicit `LAZY` to-one associations, cascade and `orphanRemoval` behavior, bidirectional
synchronization, collection type, and when a join table becomes an entity — are stated in full in
[entity and query examples](references/entity-and-query-examples.md).

## N+1 and fetch plans

Choose the smallest suitable fetch mechanism, in this order: a projection for a read-only subset;
`@EntityGraph` for a known entity graph; a fetch join for a controlled association shape;
provider-supported batch fetching for intentional lazy traversal.

- Do not solve N+1 with blanket `EAGER` fetching.
- Disable Open EntityManager in View for REST services with `spring.jpa.open-in-view=false`, and never enable `hibernate.enable_lazy_load_no_trans`. Both let lazy loading succeed outside the service transaction, so the queries appear at rendering time where no service test observes them.
- Resolve `LazyInitializationException` by fetching required state inside the service transaction.
- Check mapper, serializer, logging, debugger, `equals`, `hashCode`, and `toString` access for accidental lazy loading.
- Do not fetch-join multiple collections without proving cardinality and provider behavior.
- Do not combine collection fetch joins with pagination. Page root identifiers first and load the required graph in a bounded second query, or use a projection.
- Do not use `distinct` to hide a cartesian product or an incorrect fetch plan.
- Add query-count tests for N+1-sensitive flows.

## Repositories, queries, and access paths

The complete rules for repository design, read projections, sargable and dynamic queries, pagination
and keyset scrolling, and SQL and index performance are stated in
[entity and query examples](references/entity-and-query-examples.md), beside the queries they
govern. Read that file before writing or reviewing any of them; none of it is decided here.

## Transaction behavior

`spring-boot-patterns` owns where the boundary sits: which method carries `@Transactional`, proxy
semantics, and how long a transaction may stay open. This section owns what happens inside it.

- Use `readOnly = true` for read operations as an optimization hint, not as an authorization guarantee.
- `readOnly` takes effect only where the transaction actually starts. On a method that joins an existing write transaction the attribute is ignored, so declaring it there proves nothing and reads as a guarantee the code does not have.
- Use `REQUIRES_NEW` only for a documented consistency reason, and account for the extra connection demand.
- Follow the complete explicit update-and-save structure owned by `spring-boot-patterns`; do not replace it with a dirty-checking-only implementation.
- Never rebuild a detached entity from client input and save it as an update. Every field the client did not send is written too, so an omitted field becomes a silent overwrite that no validation reports.
- Choose isolation levels from actual anomalies and database behavior.

## Flush behavior, bulk DML, and large batches

The complete rules for flush timing and explicit synchronization, for bulk JPQL and Criteria DML,
and for bounded batch processing are stated in
[write behavior examples](references/write-behavior-examples.md). Read it before writing a
`@Modifying` query, a `flush` or `saveAndFlush` call, or a loop that touches thousands of rows.

## Concurrency and locking

- **Choose the strategy per operation, not per entity.** Both appear in most applications and frequently on the same aggregate: editing a product is optimistic, reserving its stock is pessimistic. Optimistic is the default because it costs nothing when nothing collides.
- **The application absorbs contention; the caller does not.** Repeat the operation through the project's composed `@OptimisticLockingRetry` annotation at the use-case boundary, and surface `409 Conflict` only when the retry policy is exhausted. Never answer routine contention by asking the caller to send the request again, and never write a retry loop by hand.
- That annotation composes `@Transactional` with the generation's declarative retry so the retry advice wraps the transaction and each attempt gets a fresh one. On Spring Boot 3 the interceptor orders make that arrangement the default; on Spring Boot 4 it is not documented as a guarantee, so prove it with a test on either generation. Never catch the optimistic failure below the annotation: an aggregate service performs the write and lets the version check surface at commit.
- Retry only when the complete operation is safe to repeat: it recomputes from state it re-reads and has produced no external side effect.
- Retry cannot prevent a stale-client overwrite, where a caller submits values computed from state it no longer has. That needs a version supplied by the caller — a read-only field in the update TO, or `ETag` with `If-Match` — and `docs/project-profile.md` records which, or records that every write is transformational and neither is needed. A caller-supplied version is verified, never assigned to `@Version`.
- Use a pessimistic lock when the invariant requires blocking — allocating limited stock, seats, or a numbered sequence, claiming a work item, or protecting an invariant spanning rows. Those are structurally pessimistic and need no measurement. Only buying a lock purely for throughput on a hot row requires evidence from production.
- `@Version` is a property of the entity, not of an operation, so it cannot exist for only some methods. Keep it whenever any path to that entity uses optimistic concurrency, including paths that also take a pessimistic lock. An entity reached exclusively under a pessimistic lock does not need it, and adding one there buys nothing.
- Modifying a pessimistically locked entity increments the version through the ordinary update, so a concurrent optimistic **writer** still sees the conflict. Locking without modifying does not; `PESSIMISTIC_FORCE_INCREMENT` exists for that case.
- Never add `PessimisticLockingFailureException` or `CannotAcquireLockException` to the optimistic retry annotation. A lock timeout means another caller holds the row, and retrying immediately lengthens the queue. Decide retry for a locked path separately, with a smaller attempt count.
- Invoke pessimistic-lock repository methods only inside an active transaction, and complete all locked work before that transaction ends.
- Configure lock timeouts where supported, lock multiple rows in a consistent order, and keep locked transactions especially short.
- Enforce uniqueness with a database constraint and handle the race after an application existence check.
- Remember that bulk DML bypasses normal optimistic version checks, so it can overwrite a concurrent edit with no exception anywhere.

[Locking and retry examples](references/locking-and-retry-examples.md) carries the
strategy-selection table, the `@Version` mapping, the retry-versus-client-version decision, the
`@OptimisticLockingRetry` annotation for both generations, lock timeouts, lock ordering, and the
rejected forms. Read it before adding any lock: the three mistakes it prevents — a `catch` that
never fires because the version check happens at commit, a retry that reuses the failed transaction,
and a retry that silently overwrites a concurrent edit because the values never came from the
re-read — all compile and all pass a single-threaded test.

## Schema migrations

`sql-database-migration` owns migration files, ordering, expand-and-contract, backfills, seed data,
and verification. This skill owns only what the schema has to look like for the mappings to work.

- Every mapping change this skill produces requires a migration in the same commit. Do not treat schema generation as a substitute.
- `sql-database-migration` sets the Hibernate schema-generation mode. What matters here is the consequence: with validation on, a mapping that has outrun its migration fails at startup rather than at the first query, so treat that failure as a missing migration.
- Give constraints and indexes explicit names in the mapping and in the migration, and keep them equal. `project-naming-conventions` owns the form.

## Persistence tests and observability

- Apply `spring-boot-testing` for test structure, data, isolation, and execution.
- Use Testcontainers or an equivalent environment with the actual supported database engine; H2-only tests are not evidence of production behavior.
- Test entity mappings, converters, constraints, generated identifiers, repository queries, projections, entity graphs, pagination, locking, bulk DML, and migrations where relevant.
- Assert query counts for N+1-sensitive flows, and test deterministic ordering and count queries separately from result queries.
- Test uniqueness races, optimistic conflicts, pessimistic timeouts, commit, and rollback behavior.
- Enable SQL and bind logging only in safe local and test environments.
- Compare representative execution plans before and after performance-sensitive query changes.
- Monitor slow queries, transaction duration, connection-pool saturation, lock waits, deadlocks, rows examined, and database CPU and I/O.

## JPA-specific anti-patterns

The full catalogue sits beside the examples of each one: entity shape, fetch plans, query shape, and
schema verification in [entity and query examples](references/entity-and-query-examples.md); write
behavior in [write behavior examples](references/write-behavior-examples.md); concurrency policy in
[locking and retry examples](references/locking-and-retry-examples.md). The ones that most often
survive review:

- A collection fetch join combined with pagination, or `distinct` used to hide the cartesian product it produces.
- N+1 queries hidden in a mapper, a serializer, logging, a loop, or an accessor, where no single line looks like a query.
- A mapping change merged without its migration, or with constraint and index names that differ from the migration's.
- H2-only verification standing in for another production database.
- A hand-written retry loop, or an optimistic failure caught below the composed annotation, which leaves the annotation as decoration.

## Completion checklist

- [ ] Mappings, migrations, associations, cascades, and orphan behavior agree.
- [ ] Fetch plans are explicit; N+1 risk is tested.
- [ ] Queries are bounded and deterministic, verified against generated SQL and representative plans.
- [ ] Pagination, projections, transactions, bulk DML, and locking match the access path.
- [ ] Contention is absorbed by the composed retry annotation at the use-case boundary, a test proves the retry advice wraps the transaction, and `409` reaches the caller only on exhaustion.
- [ ] Any operation that overwrites rather than recomputes uses the stale-write protection the profile records.
- [ ] Tests run against the supported database and cover changed persistence behavior.
