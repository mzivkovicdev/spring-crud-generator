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
- [Locking and retry examples](references/locking-and-retry-examples.md) for optimistic and pessimistic locking, the atomic conditional `UPDATE` that is the third strategy, the composed retry annotation for both generations, lock timeouts and ordering, and the concurrency anti-patterns.
- [Write behavior examples](references/write-behavior-examples.md) for flush timing, bulk DML, large batches, and the write-side anti-patterns.
- [Resource bounds](references/resource-bounds.md) *(rules)* for how a statement, transaction, connection-acquisition, or pool bound is actually applied: the three levels a bound can sit at, the delivery channel each engine offers and why they all share one, the migration exception, and the tests that prove a bound is real.

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
- Use `@Version` when concurrent updates must not silently overwrite each other, and never assign the version value in Java: it is provider-owned, no setter is written for it, and it is never copied from a request. The one place a version is written deliberately is a bulk statement, which bypasses the provider's own increment and must therefore carry it — [write behavior examples](references/write-behavior-examples.md#bulk-dml) states how, and Jakarta Persistence requires it of a portable application.
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

- **Choose the strategy per operation, not per entity.** There are three, and all three appear in most applications, frequently on the same aggregate: editing a product is optimistic, reserving its stock is pessimistic, decrementing it by a fixed amount is one conditional `UPDATE`. Optimistic is the default because it costs nothing when nothing collides.
- **The application absorbs contention; the caller does not.** Repeat the operation through the project's composed `@OptimisticLockingRetry` annotation at the use-case boundary, and surface `409 Conflict` only when the retry policy is exhausted. Never answer routine contention by asking the caller to send the request again, and never write a retry loop by hand.
- That annotation composes `@Transactional` with the generation's declarative retry so the retry advice wraps the transaction and each attempt gets a fresh one. **Both generations document interceptor orders that already produce that arrangement, so leave them at their defaults** — an explicit order that restates a default is a second source of truth. Prove it anyway with a test that counts committed attempts: the defaults are documented, not enforced, and another advisor in the project can invert them while the configuration still reads as correct. Never catch the optimistic failure below the annotation: an aggregate service performs the write and lets the version check surface at commit.
- Retry only when the complete operation is safe to repeat: it recomputes from state it re-reads and has produced no external side effect.
- Retry cannot prevent a stale-client overwrite, where a caller submits values computed from state it no longer has. That needs a version supplied by the caller — a read-only field in the update TO, or `ETag` with `If-Match` — and `docs/project-profile.md` records which, or records that every write is transformational and neither is needed. A caller-supplied version is verified, never assigned to `@Version`.
- Use a pessimistic lock when the invariant requires blocking — allocating limited stock, seats, or a numbered sequence, claiming a work item, or protecting an invariant spanning rows. Those need a mechanism stronger than retry, and none of them waits on a measurement. Only buying a lock purely for throughput on a hot row requires evidence from production.
- **Prefer one atomic conditional `UPDATE` over the lock where the whole rule fits one row and one `WHERE` clause** — a decrement that must not go negative, a status claim that exactly one worker may win. The affected row count is the business answer, and the row is held from the statement to the commit rather than from the read to the commit, so every caller queued behind it holds a connection for less time. It is bulk DML, so it carries every bulk consequence: read [locking and retry examples](references/locking-and-retry-examples.md#atomic-conditional-dml) before choosing it, and take the lock instead when the decision spans rows, needs application logic between reading and writing, or feeds later work in the same transaction.
- `@Version` is a property of the entity, not of an operation, so it cannot exist for only some methods. Keep it whenever any path to that entity uses optimistic concurrency, including paths that also take a pessimistic lock. An entity reached exclusively under a pessimistic lock does not need it, and adding one there buys nothing.
- Modifying a pessimistically locked entity increments the version through the ordinary update, so a concurrent optimistic **writer** still sees the conflict. Locking without modifying does not; `PESSIMISTIC_FORCE_INCREMENT` exists for that case.
- Never add `PessimisticLockingFailureException` or `CannotAcquireLockException` to the optimistic retry annotation. A lock timeout means another caller holds the row, and retrying immediately lengthens the queue. Decide retry for a locked path separately, with a smaller attempt count.
- **The two contention failures are different conditions, and neither is caught in a service.** An exhausted optimistic retry means the row genuinely changed under the caller; a lock timeout, deadlock victim, serialization failure, or a statement the database cancelled while it waited means the row was held and the wait expired. Both surface from the transaction interceptor after the method body returned, so both are translated in the REST exception advice, from the Spring framework types rather than a project exception. `spring-boot-patterns` decides what each one shows the caller — and it gives them **different** statuses, so do not declare them as one condition.
- Invoke pessimistic-lock repository methods only inside an active transaction, and complete all locked work before that transaction ends.
- **Bound every lock wait, and verify the mechanism against the configured engine.** A positive `jakarta.persistence.lock.timeout` is honoured by very few engines — on PostgreSQL it is ignored outright, and the wait is then bounded only by the statement timeout, which cancels the statement and surfaces as `QueryTimeoutException` rather than as a lock failure. [Lock timeouts](references/locking-and-retry-examples.md#lock-timeouts) carries the mechanism per engine and the rule that the timeout is recorded once and read, never retyped. Lock multiple rows in a consistent order, and keep locked transactions especially short.
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

## Resource budgets

Every rule above shapes *what* the database does. This section is about *how long* and *how much*,
and it is the half that decides whether a slow dependency degrades one request or the whole
application. `docs/project-profile.md` records the numbers under **Performance and capacity**; this
skill owns what they mean and what happens when one is missing.

**A default that is "no limit" is the dangerous kind, because nothing reports it.** An unbounded
statement holds its connection; a held connection is one the pool cannot hand out; an exhausted pool
turns a slow query on one endpoint into a timeout on every endpoint. That chain is the reason these
are bounds rather than tuning.

- **Set a statement timeout at the connection level**, so it covers every statement the connection carries and not only the queries the application issues itself. Keep it below the request budget `spring-boot-patterns` records: a statement still running after the caller gave up is pure cost.
- **Set a transaction timeout**, project-wide by default and tighter where a use case needs it, at or below the request budget and never below the statement timeout. It bounds the whole unit, including the parts between statements — and a read needs one as much as a write does, because `readOnly` bounds nothing.
- **Size the connection pool from the engine's limit and the instance count**, not from a guess. Pool size × instances must stay within what the database accepts, with headroom for migrations and operators. A larger pool is not faster: past the point the database can execute concurrently, it converts queuing in the application into queuing in the engine, where it is harder to see.
- **Bound the wait for a connection**, and keep it short. A long acquisition wait does not prevent exhaustion; it hides it, by turning a fast failure into a stalled request.
- **Enable JDBC batching deliberately and verify it in the generated SQL.** `saveAll` is not batching, and the identifier strategy can silently disable it.
- **Every read that can grow is already bounded** by the pagination rules above; the maximum page size is recorded with the rest of these numbers so it is one decision rather than a constant somebody re-picks.

**How each of those is applied is stated once, in
[resource bounds](references/resource-bounds.md)** — the three levels a bound can sit at and which
one is the real one, the delivery channel each engine offers, the rule that every connection-level
setting shares one channel, why migrations must not inherit the application's bounds, and the test
that proves each bound rather than reading it. Read it before configuring or reviewing any of these
numbers; a bound the engine ignores looks exactly like a bound that works.

Two consequences worth stating, because they are where these bounds actually get lost:

- **`REQUIRES_NEW` doubles the connection demand** for the duration of the inner transaction, since the outer one stays open. Account for it in the pool size, or do not use it.
- **A lock wait sits inside all of this.** The pessimistic lock timeout the profile records is bounded for the same reason, and it must be below the statement timeout, not merely below the request budget. A timeout the engine ignores is the worst case in this whole section: it reads as bounded, the wait falls through to the statement timeout instead, and the failure arrives as a different exception type than the contract expects.

What this skill does **not** own is measurement: whether the application meets a latency target, and
what a load test has to prove before a release. [`_core/README.md`](../_core/README.md) records that
as an open gap rather than leaving it to be assumed.

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
- [ ] The advice translates the framework contention types themselves, so neither an exhausted retry nor a lock timeout reaches the catch-all as `500`, and the two are not declared as one condition.
- [ ] Any operation that overwrites rather than recomputes uses the stale-write protection the profile records.
- [ ] Tests run against the supported database and cover changed persistence behavior.
- [ ] Statement, transaction, connection-wait, pool, and pessimistic-lock bounds come from the profile, fit inside the request budget, and no path was left with an unbounded default.
- [ ] The lock timeout is applied through a mechanism the configured engine actually honours, proven by a test that a blocked lock fails within the bound rather than waiting.
