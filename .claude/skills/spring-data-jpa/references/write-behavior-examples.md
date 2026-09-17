# Write behavior examples

Use this file when implementing or reviewing flush timing, bulk JPQL or Criteria DML, or a batch that
touches large numbers of rows. Apply every rule from `../SKILL.md`; imports are omitted.

**This file carries rules, not only examples.** Flush behavior, bulk DML, large batches, and the
write-side anti-pattern catalogue are stated here in full and nowhere else. The `Rules` blocks below
are as binding as `../SKILL.md`.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Flush behavior](#flush-behavior)
2. [Bulk DML](#bulk-dml)
3. [Write-side anti-patterns](#write-side-anti-patterns)

**Bulk DML bypasses the optimistic version check**, so a statement over version-protected rows can
overwrite a concurrent edit with no exception anywhere. The locking side of that problem is in
[locking and retry examples](locking-and-retry-examples.md).

## Flush behavior

- Treat flush timing separately from update intent. JPA can synchronize managed state at flush or commit even when the project requires an explicit repository `save` call, so the presence of a `save` call says nothing about when the SQL runs.
- Use `flush` or `saveAndFlush` only when subsequent logic must observe database synchronization immediately, such as a deliberately handled constraint failure or database-generated effect; document and test that reason.
- Never call `saveAndFlush` for every item in a loop. Each call discards JDBC batching and turns one round trip into as many as there are rows.
- Remember that JPQL/HQL and some native queries can trigger an automatic flush before query execution.

## Bulk DML

Rules:

- Bulk JPQL/Criteria updates and deletes bypass entity synchronization, callbacks, cascades, and optimistic-lock checks.
- Invoke bulk DML through an active write transaction owned by a public service method.
- Flush pending changes first when required and clear or refresh affected managed state deliberately.
- Pair `clearAutomatically = true` with `flushAutomatically = true` when pending changes must not be discarded.
- Return and verify the affected row count when it is part of correctness.
- Prefer set-based DML over loading thousands of entities only to update or delete them.
- Process large entity batches in bounded chunks and clear the persistence context between chunks.
- `saveAll` is not proof of JDBC batching; configure and verify batching for the provider, database, and identifier strategy.
- Never retain an unbounded number of managed entities in one persistence context.

Declare synchronization behavior explicitly and verify the affected row count:

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("""
        update UserEntity user
        set user.status = :newStatus
        where user.status = :oldStatus
        """)
int updateStatus(
        @Param("oldStatus") final UserStatus oldStatus,
        @Param("newStatus") final UserStatus newStatus);
```

Do not use a previously loaded entity after bulk DML without deliberately refreshing or clearing it:

```java
final UserEntity user = this.userRepository.findById(id).orElseThrow();

this.userRepository.updateStatus(UserStatus.ACTIVE, UserStatus.SUSPENDED);

return UserDomainMapper.INSTANCE.mapUserEntityToUserDomain(user);
```

The loaded entity can be stale because bulk DML bypasses normal persistence-context synchronization.

**Bulk DML also bypasses the optimistic version check.** It does not increment `@Version` and does
not fail when a row was modified concurrently, so a bulk update over rows another transaction is
editing overwrites that work silently, with no exception anywhere. Where a bulk statement touches
version-protected rows, either increment the version column in the same statement or scope the
statement so it cannot overlap concurrent edits, and record which was chosen. Plain JPQL increments
it with an explicit `set entity.version = entity.version + 1`; on Hibernate, `update versioned` in
place of `update` does the same without the assignment.

Bulk DML additionally skips entity callbacks, cascades, and auditing. Anything those would have done
must be done explicitly, by the statement or by the caller.

## Write-side anti-patterns

Reject. The entity-shape, fetch-plan, query-shape, and schema groups are in
[entity and query examples](entity-and-query-examples.md).

**Write behavior.** Detached entities reconstructed from client input and saved as updates;
unnecessary early flushes and `saveAndFlush` inside per-row loops; bulk DML followed by use of stale
managed entities; pessimistic locks without bounded scope and timeout consideration.

**Concurrency policy.** A hand-written retry loop where the project's composed annotation applies; a
retry policy whose backoff has no jitter or no maximum delay; a transaction advice ordered ahead of
the retry advice; an optimistic failure caught below the annotated method; an exhausted retry that
leaks the framework exception instead of the conflict contract; a retry on an operation with an
external side effect or one that overwrites rather than recomputes; `409` returned to the caller for
contention the application never attempted to absorb; a `@Version` value assigned from a request.