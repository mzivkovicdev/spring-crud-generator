---
name: spring-data-jpa
description: Production Spring Data JPA and Hibernate patterns for Java 21+ applications using any supported relational database. Covers entity mapping, repositories, associations, fetch plans, JPQL, native SQL, transactions, pagination, bulk work, locking, migrations, query performance, and persistence tests. Use whenever code touches JPA entities, repositories, Specifications, EntityManager, schema migrations, database reads or writes, or transactional persistence behavior.
---

# Spring Data JPA Skill

Design persistence for correctness, predictable SQL, and verified performance. JPA does not remove the need to understand relational modeling, indexes, query plans, transactions, and locking.

## Coordination with other skills

Apply `modern-java-21` to every touched Java file and `spring-boot-patterns` to controller, service, domain, mapper, and transaction boundaries. Those skills own general Java style, imports, Javadoc, tests, TOs, domain models, service parameters, mapper construction, and service update structure. Do not repeat their rules here.

Apply `application-security` when persistence affects confidential data, tenant or object ownership, authorization scope, encryption, audit data, backups, exports, or dangerous query input. Apply `project-naming-conventions` to entity, repository, table, column, constraint, index, and migration names and to every escaped rename.

Use their established terminology consistently:

| Type | Boundary |
| --- | --- |
| `UserCreateTO`, `UserUpdateTO`, `UserTO` | REST/controller |
| `UserDomain` | Domain/service result |
| `UserEntity` | JPA persistence |
| `UserSummaryProjection` | Repository read projection |
| `UserRestMapper` | Domain → response TO; request TO → a justified focused domain/service input |
| `UserDomainMapper` | Entity/projection → domain; explicit creation values → entity |

Repositories return entities or persistence projections. Services map them to domain objects before returning. This skill owns the JPA behavior beneath that boundary.

This skill is database-agnostic. Inspect the configured database and Hibernate dialect before using vendor-specific SQL, types, indexes, hints, locking options, migration syntax, or identifier strategies.

## References

Read only the examples required by the change:

- Read [entity and query examples](references/entity-and-query-examples.md) for mappings, associations, repositories, projections, fetch plans, dynamic queries, pagination, or SQL access paths.
- Read [write and locking examples](references/write-and-locking-examples.md) for bulk DML, persistence-context synchronization, or pessimistic locking.

## Before changing persistence

Inspect:

1. Spring Boot, Spring Data JPA, Jakarta Persistence, Hibernate, JDBC driver, database, and migration-tool versions;
2. entity mappings, association ownership, converters, listeners, inheritance, identifier generation, and equality;
3. service transaction boundaries and every caller affected by the change;
4. schema migrations, constraints, indexes, column types, defaults, and expected data volume;
5. query cardinality, selectivity, ordering, pagination, read/write ratio, and concurrency;
6. generated SQL and database execution plans for important access paths;
7. repository, migration, locking, query-count, and database integration tests.

Do not copy a nearby persistence pattern before understanding its generated SQL and lifecycle behavior.

## Entity mapping

Entity rules:

- Do not use records as entities. Records may be embeddables only when supported by the configured provider and project version.
- Keep entities and persistent accessors non-final unless verified bytecode enhancement removes proxy limitations.
- Provide a `protected` no-argument constructor when possible.
- Use field or property access consistently; place mapping annotations according to the chosen strategy.
- Expose the getters required by persistence-to-domain mapping. With field access, JPA does not require public accessors, but mapping code must still be able to read the selected state.
- Keep entity mappings, migrations, and database definitions aligned for names, nullability, length, precision, scale, uniqueness, defaults, foreign keys, and indexes.
- Database constraints enforce integrity; application validation does not replace them.
- Use `@Version` when concurrent updates must not silently overwrite each other. Never modify the version value in application code.
- Prefer `EnumType.STRING`; treat enum renames as data migrations.
- Define timestamp/timezone policy explicitly and use `BigDecimal` precision and scale for fixed-decimal columns.
- Choose identifier generation for the actual database and verify its effect on batching and round trips.
- Never use Lombok `@Data` on entities.
- Exclude lazy associations and mutable state from `equals`, `hashCode`, and `toString`.
- Test equality across transient, managed, detached, and proxied instances when entities enter sets or maps.
- Keep entity listeners limited to persistence concerns; never perform repository or remote calls from callbacks.

## Association ownership

- Set to-one associations to `LAZY` explicitly unless a measured access path proves another choice.
- Treat fetching as a query/use-case decision, not an entity-wide default.
- Cascade only lifecycle operations owned by the aggregate; never default to `CascadeType.ALL`.
- Never cascade remove from a child or shared reference to its parent.
- Use `orphanRemoval` only when removing the child from the owning collection must delete it.
- Use unidirectional associations by default.
- Introduce a bidirectional association only when concrete use cases require navigation in both directions.
- Keep both sides of every bidirectional association synchronized through explicit helper methods.
- Choose `List`, `Set`, or `Map` from business and ordering semantics.
- Query large child sets separately instead of exposing unbounded entity collections.
- Model a many-to-many join table as an entity when it has attributes, ordering, audit data, identity, lifecycle, or independent constraints.

## Repository design

- Use derived queries while their names remain short and their generated predicates are appropriate.
- Use explicit JPQL when derivation becomes ambiguous or hides important joins.
- JPQL uses entity and attribute names, not table and column names.
- Bind values through parameters; never concatenate data into JPQL or SQL.
- Use `Optional` for an optional single result and `existsBy...` when only presence is needed.
- Bound every result that can grow with production data.
- Do not invoke inherited destructive or unbounded methods on production-sized data without a bounded use case. When preventing those calls at the repository API is a project requirement, define and verify a tailored base repository instead of assuming `JpaRepository` hides them.
- Use a custom repository for queries clearer with Specifications, Criteria, Querydsl, `EntityManager`, or native SQL.
- Consume repository `Stream<T>` results inside the required transaction and close them with try-with-resources; never return an open stream across the service boundary.
- Add Javadoc only when locking, timeout, fetch, ordering, native-SQL, or consistency semantics are non-obvious.

## Read projections

- Use projections for bounded read paths that need only selected columns.
- A persistence projection is neither a TO nor a domain result; map it before leaving the service.
- Keep interface projections closed and top-level. Nested properties can materialize joins and more data than expected.
- Avoid `Object[]`, raw `Tuple`, and `Map<String, Object>` as cross-layer contracts.
- Cover native projections with integration tests against the supported database.

## N+1 and fetch plans

Choose the smallest suitable fetch mechanism:

1. projection for a read-only subset;
2. `@EntityGraph` for a known entity graph;
3. fetch join for a controlled association shape;
4. provider-supported batch fetching for intentional lazy traversal.

- Do not solve N+1 with blanket `EAGER` fetching.
- Disable Open EntityManager in View for REST services with `spring.jpa.open-in-view=false`.

- Resolve `LazyInitializationException` by fetching required state inside the service transaction.
- Check mapper, logging, debugger, `equals`, `hashCode`, and `toString` access for accidental lazy loading.
- Do not fetch-join multiple collections without proving cardinality and provider behavior.
- Do not combine collection fetch joins with pagination. Page root identifiers first and load the required graph in a bounded second query, or use a projection.
- Do not use `distinct` to hide a cartesian product or incorrect fetch plan.
- Add query-count tests for N+1-sensitive flows.

## Sargable and dynamic queries

- Build only required predicates for optional filters.
- Prefer the JPA static metamodel or Querydsl for non-trivial dynamic queries; raw attribute-name strings fail only at runtime after incompatible refactoring.
- Normalize values according to the business contract before querying; do not apply functions to indexed columns by habit.
- Functions, casts, arithmetic, and implicit type conversion on indexed columns can prevent normal index access.
- Avoid leading-wildcard searches on large tables unless a suitable search/index feature is deliberately used.
- Bound `IN` collections; use chunking or a measured database-specific bulk strategy for very large sets.
- Never issue a repository query inside a per-row loop when one set-based query can retrieve the data.
- Allowlist sort fields and directions; never pass user input to `JpaSort.unsafe`.

## Pagination and scrolling

- Enforce maximum page size at the REST boundary.
- Always sort deterministically with a unique tie-breaker.
- Use `Page` only when the caller needs a total and the count query is acceptably cheap.
- Use `Slice` when only next-page information is needed.
- Prefer keyset scrolling for deep or high-volume traversal when the API can represent a cursor.
- Keyset sort columns must be non-null, deterministic, and supported by an effective index.
- Never paginate or sort database-sized results in memory.
- Supply an explicit `countQuery` when a complex or native paged query cannot be derived correctly or efficiently.

## SQL and index performance

- Inspect generated SQL for every complex or high-volume query.
- Use the supported database's execution-plan tool with representative statistics and data volume.
- Select only required columns for read-heavy paths; avoid loading full entities and LOBs for summaries.
- Align composite index order with actual equality, range, join, and sort predicates.
- Avoid redundant and speculative indexes because each index adds storage and write cost.
- Index foreign-key and join columns when required by the database and access paths.
- Prevent accidental cartesian products and duplicate rows from incorrect joins.
- Use existence queries instead of counting all rows when only presence is required.
- Apply tenant and soft-delete predicates to derived, JPQL, native, bulk, and count queries.
- Include tenant keys in relevant unique constraints and indexes for tenant-scoped data.
- Configure query or transaction timeouts for bounded operational work.
- Use native SQL only for a concrete feature, portability, or measured performance reason.

## Transactions and flush behavior

- Keep transaction boundaries on public service methods reached through the Spring proxy.
- Use `readOnly = true` for read operations as an optimization hint, not as an authorization guarantee.
- Keep transactions short; do not perform remote calls, unbounded iteration, or long CPU work inside them.
- Do not rely on self-invocation. Move a separate transaction boundary to another bean when required.
- Use `REQUIRES_NEW` only for a documented consistency reason and account for extra connection demand.
- Follow the `spring-boot-patterns` update structure: load the entity inside the write transaction, invoke explicit mutations, call repository `save` exactly once, and map the saved entity returned by the repository to domain.
- JPA can synchronize changes to a managed entity at flush/commit, but this project deliberately requires the explicit `save` call for update intent and consistency with the Spring Data repository abstraction. Do not omit it as a dirty-checking shortcut.
- Use `flush` or `saveAndFlush` only when subsequent logic must observe database synchronization immediately, such as a deliberately handled constraint failure or database-generated effect; document and test that reason.
- Never call `saveAndFlush` for every item in a loop.
- Remember that JPQL/HQL and some native queries can trigger an automatic flush before query execution.
- Choose isolation levels from actual anomalies and database behavior.

## Bulk DML and large batches

- Bulk JPQL/Criteria updates and deletes bypass entity synchronization, callbacks, cascades, and optimistic-lock checks.
- Invoke bulk DML through an active write transaction owned by a public service method.
- Flush pending changes first when required and clear or refresh affected managed state deliberately.
- Pair `clearAutomatically = true` with `flushAutomatically = true` when pending changes must not be discarded.
- Return and verify the affected row count when it is part of correctness.
- Prefer set-based DML over loading thousands of entities only to update or delete them.
- Process large entity batches in bounded chunks and clear the persistence context between chunks.
- `saveAll` is not proof of JDBC batching; configure and verify batching for the provider, database, and identifier strategy.
- Never retain an unbounded number of managed entities in one persistence context.

## Concurrency and locking

- Prefer optimistic locking with `@Version` for normal concurrent editing.
- Translate lock failures into a stable conflict or retry contract at the service boundary.
- Retry only when the complete operation is safe to repeat.
- Use pessimistic locking only when measured contention and invariants justify blocking.
- Invoke pessimistic-lock repository methods only inside an active transaction and complete all locked work before that transaction ends.
- Configure lock timeouts where supported and lock multiple rows in a consistent order.
- Keep locked transactions especially short.
- Enforce uniqueness with a database constraint and handle the race after an application existence check.
- Remember that bulk DML bypasses normal optimistic version checks.

## Schema migrations

- Use the project's Flyway or Liquibase convention as schema source of truth.
- Do not use Hibernate auto-DDL to create or update production schemas.
- Give constraints and indexes stable, explicit names.
- Keep migrations compatible with rolling deployments.
- Use expand-and-contract for incompatible changes.
- Do not run massive blocking backfills in application-startup migrations without lock, duration, recovery, and rollout analysis.
- Separate large data backfills from schema changes when operational control is required.
- Evaluate vendor-specific online/concurrent index features for large production tables.
- Test migrations from the previous supported schema on the production database engine and major version.

## Persistence tests and observability

- Use Testcontainers or an equivalent environment with the actual supported database engine; H2-only tests are not evidence of production behavior.
- Test entity mappings, converters, constraints, generated identifiers, repository queries, projections, entity graphs, pagination, locking, bulk DML, and migrations where relevant.
- Assert query counts for N+1-sensitive flows.
- Test deterministic ordering and count queries separately from result queries.
- Test uniqueness races, optimistic conflicts, pessimistic timeouts, commit, and rollback behavior.
- Enable SQL and bind logging only in safe local/test environments.
- Compare representative execution plans before and after performance-sensitive query changes.
- Monitor slow queries, transaction duration, connection-pool saturation, lock waits, deadlocks, rows examined, and database CPU/I/O.

## JPA-specific anti-patterns

Reject:

- records used as entities;
- Lombok `@Data` on entities;
- blanket `FetchType.EAGER`;
- Open EntityManager in View and `hibernate.enable_lazy_load_no_trans`;
- N+1 queries hidden in mappers, serializers, logging, loops, or accessors;
- collection fetch joins combined with pagination;
- multiple collection fetch joins that create cartesian multiplication;
- `distinct` used to hide an incorrect join or fetch plan;
- detached entities reconstructed from client input and saved as updates;
- unbounded repository reads, streams, or association traversal;
- full-entity loading when a bounded projection is sufficient;
- query-per-row loops;
- optional-filter `OR` queries and functions on indexed columns on hot paths without verified plans;
- leading-wildcard searches on large tables without a suitable search index;
- unbounded `IN` predicates;
- unsafe user-controlled sorting;
- missing, ineffective, redundant, or speculative indexes;
- `CascadeType.ALL` without aggregate lifecycle ownership;
- cascade remove from a child or shared reference to its parent;
- lazy or mutable associations in `equals`, `hashCode`, or `toString`;
- unnecessary early flushes and `saveAndFlush` inside per-row loops;
- bulk DML followed by use of stale managed entities;
- pessimistic locks without bounded scope and timeout consideration;
- production schema mutation through Hibernate auto-DDL;
- destructive one-step migrations and uncontrolled startup backfills;
- H2-only persistence verification for another production database.

## Completion checklist

- [ ] Mappings, migrations, associations, cascades, and orphan behavior agree.
- [ ] Fetch plans are explicit and N+1 risk is tested.
- [ ] Queries are bounded, deterministic, and verified with generated SQL and representative plans.
- [ ] Pagination, projections, transactions, bulk DML, and locking match the access path.
- [ ] Tests run against the supported database and cover changed persistence behavior.
