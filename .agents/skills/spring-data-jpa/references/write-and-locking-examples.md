# Write and locking examples

Use these examples when implementing or reviewing bulk DML, persistence-context synchronization,
optimistic locking, or pessimistic locking. Apply every rule from `../SKILL.md`; imports are omitted.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

Concurrency defects are the most expensive class of persistence bug, because they are invisible under
the conditions most test suites run: one thread, one transaction, an empty table. Every example here
exists because the naive version passes its tests and loses data in production.

## Contents

1. [Choosing a strategy](#choosing-a-strategy)
2. [Optimistic locking](#optimistic-locking)
3. [Server-side retry versus a client-supplied version](#server-side-retry-versus-a-client-supplied-version)
4. [Translating a lock failure at the service boundary](#translating-a-lock-failure-at-the-service-boundary)
5. [Retrying an optimistic failure](#retrying-an-optimistic-failure)
6. [Pessimistic locking](#pessimistic-locking)
7. [When one aggregate uses both](#when-one-aggregate-uses-both)
8. [Lock timeouts](#lock-timeouts)
9. [Lock ordering and deadlocks](#lock-ordering-and-deadlocks)
10. [Uniqueness is a constraint, not a check](#uniqueness-is-a-constraint-not-a-check)
11. [Bulk DML](#bulk-dml)
12. [Rejected code](#rejected-code)

## Choosing a strategy

**The strategy is chosen per operation, not per entity.** Both appear in most real applications, and
frequently on the same aggregate: editing a product's description is optimistic, while reserving its
stock is pessimistic. Do not look for one project-wide answer, and do not assume an entity carrying
`@Version` never takes a pessimistic lock.

| Situation | Strategy | Why |
| --- | --- | --- |
| Normal concurrent editing of a row by different users | Optimistic, `@Version` | Conflicts are rare; blocking every reader to prevent a rare conflict costs more than resolving it |
| A counter, balance, or quota that must never be lost | Optimistic with the retry annotation, or one atomic UPDATE | Read-modify-write across two transactions loses one of them silently |
| Allocating a limited resource: stock, seats, a licence pool, a numbered sequence | Pessimistic write lock | The invariant is "never oversell", and optimistic retry under real contention degrades into a retry storm rather than a queue |
| Claiming a work item so exactly one worker processes it | Pessimistic write lock, or a conditional UPDATE that claims by status | Two workers must not both win; a version check tells them so only after both did the work |
| An invariant spanning rows that must not be read mid-change | Pessimistic write lock, consistent order | Optimistic checks each row separately and cannot see the invariant |
| A measured hot row where optimistic retries keep failing | Pessimistic write lock | Contention observed in production, not feared in review |
| Uniqueness of a business key | A database unique constraint | An application check has a race window no lock closes |

Optimistic is the default because it costs nothing when nothing collides. But the last four rows are
**structurally** pessimistic: the invariant itself decides, and they need no measurement to justify
the choice. Only the hot-row case requires evidence, because there the invariant would have been
satisfied either way and the lock is bought purely for throughput.

What a pessimistic lock costs is worth stating plainly, since it is why optimistic wins by default:
it converts a concurrency problem into an availability one. Every blocked caller holds a connection
while it waits, so an unbounded lock wait exhausts the pool long before it corrupts anything.

## Optimistic locking

Add `@Version` to the aggregate root whose consistency matters. Never modify the version value in
application code, never expose it as a writable transport field, and never copy it from a request.

```java
@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @Column(name = "sku", nullable = false, updatable = false)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    // Accessors follow the style recorded in docs/project-profile.md.
}
```

The column is created by a migration owned by `sql-database-migration`, `NOT NULL` with a default of
`0`. Adding `@Version` to a mapping without the migration fails at startup under schema validation,
which is the intended behavior.

Rules:

- The version is checked on flush, not on read. Two transactions can both read version `7`; the second to flush fails.
- A failed check raises `ObjectOptimisticLockingFailureException`. Do not catch it in the repository or in an entity callback.
- `@Version` protects the entity it is declared on. Changing a child row in a `@OneToMany` does not bump the parent's version unless the parent is also modified or `OPTIMISTIC_FORCE_INCREMENT` is requested.
- Bulk DML bypasses the check entirely. See [bulk DML](#bulk-dml).

## Server-side retry versus a client-supplied version

Two problems look identical and are not.

| Problem | What happens | What fixes it |
| --- | --- | --- |
| **Internal contention** — concurrent operations that recompute from state they re-read | The loser fails the version check at commit | Server-side retry. The next attempt re-reads and produces a correct result, invisibly to the caller |
| **Stale-client write** — a caller read at T1, a human edited, another caller changed the row at T2, the edit arrives at T3 | Stale values land on top of newer ones | Only a version supplied by the caller. Retry cannot help and makes it worse |

**Server-side retry is the default, and absorbing contention is the backend's job.** A caller is
never asked to repeat a request because two transactions collided inside the application. `409`
reaches it only once the retry policy is exhausted, where it means sustained contention rather than a
routine race.

The limit matters because it is why retry cannot be the whole answer. Retry works when the operation
**recomputes** from re-read state: a decrement, a status transition, a `PATCH` of named fields. It
does not work when the operation **overwrites** with values computed earlier from state the caller no
longer has. Re-reading changes nothing there, because the written values never came from the read —
so the retry succeeds, nothing throws, and one user's edit silently disappears.

So the two are not alternatives:

- **Partial or transformational updates** need retry alone. Return `409` on exhaustion and require nothing of the caller.
- **Full-representation updates a human edited from a stale read** need the version the caller actually saw. Two shapes are acceptable, and `docs/project-profile.md` records which: a read-only `version` field in the update TO, where a mismatch is `409`; or `ETag` with `If-Match`, where a mismatch is `412 Precondition Failed` and a missing header is `428 Precondition Required`. Either way the version is **verified, never applied** — `@Version` stays provider-owned and is never assigned from a request.

`spring-boot-patterns` owns where the check sits in the layers and `rest-api-contract` documents
whichever shape was chosen. Neither is needed when every write is transformational, which is common;
`server retry only` is a legitimate recorded answer.

## Translating a lock failure at the service boundary

A lock failure is a normal, expected outcome under concurrency, not a server fault. It becomes a
stable conflict contract at the use-case boundary, never a leaked framework exception.

The aggregate service does not catch it. It performs the write and lets the version check happen at
commit, where the retry interceptor can observe it:

```java
@Transactional
public InventoryDomain reserve(final String sku, final int quantity) {

    final InventoryEntity inventory = this.inventoryRepository
            .findById(sku)
            .orElseThrow(() -> new InventoryNotFoundException(sku));

    if (inventory.getAvailableQuantity() < quantity) {
        throw new InsufficientInventoryException(sku, quantity);
    }
    inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);

    return InventoryDomainMapper.INSTANCE.mapInventoryEntityToInventoryDomain(
            this.inventoryRepository.save(inventory));
}
```

**Why there is no `try`/`catch` here.** The version check happens at flush, which defaults to commit —
after the method body returned. A `catch` around `save` therefore catches nothing and the framework
exception escapes as a `500`. Forcing the check earlier with `saveAndFlush` to catch it locally is the
wrong repair: it discards batching and puts the conflict policy inside the aggregate, which cannot
know whether the use case may be repeated. Let it surface from the transaction interceptor and handle
it one layer out.

## Retrying an optimistic failure

Retry is one project-owned composed annotation, so every use case absorbs contention identically and
the policy is visible at the call site. **Never write a retry loop by hand.**

```java
/**
 * Runs the annotated operation in a transaction and repeats it while it loses an optimistic
 * version check. Each attempt gets a fresh transaction.
 *
 * <p>Annotate only an operation safe to repeat in full: it must recompute from state it
 * re-reads and produce no external side effect before it commits. Requires {@code @EnableRetry}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
@Retryable(
        retryFor = {
                OptimisticLockException.class,
                ObjectOptimisticLockingFailureException.class
        },
        maxAttempts = 5,
        backoff = @Backoff(delay = 100L, maxDelay = 1000L, multiplier = 2.0d, random = true)
)
public @interface OptimisticLockingRetry {
}
```

Four details in it are load-bearing:

- **`random = true`.** Without jitter every loser of a collision wakes in the same millisecond and collides again, in waves. This attribute is the one most often left off, and its absence is invisible until real load.
- **`maxDelay`.** Bounds exponential growth, so a late attempt does not sit for seconds inside a request the caller has abandoned.
- **Both exception types.** Spring normally translates to `ObjectOptimisticLockingFailureException`, but `jakarta.persistence.OptimisticLockException` can still surface on some paths.
- **`METHOD` only.** On a type it would wrap every public method in a transaction and a retry, including reads and operations unsafe to repeat.

### Ordering, and why no explicit `@Order`

`@EnableRetry` registers its interceptor at `Ordered.LOWEST_PRECEDENCE - 1` and the transaction
interceptor defaults to `Ordered.LOWEST_PRECEDENCE`, so the retry wraps the transaction. That default
is what makes the composed annotation correct, so do not set an order on
`@EnableTransactionManagement` that inverts it, and verify meta-annotation support on the project's
Spring Retry version rather than assuming it.

**Prove the ordering with a test that counts committed attempts.** Inverted, every attempt rejoins a
transaction already marked rollback-only: all fail identically while the configuration looks correct.

### Exhaustion is part of the contract

When attempts run out the last exception propagates, which is a framework exception reaching the
caller as a `500`. Close it the same way throughout the project:

```java
@Recover
public OrderDomain recover(
        final ObjectOptimisticLockingFailureException failure,
        final OrderPlacementDomain command) {

    throw new ConcurrentModificationConflictException(command.sku(), failure);
}
```

A `@Recover` method sits in the same bean, returns the same type, and takes the exception followed by
the original parameters. Mapping the failure to `409` once in the REST exception advice is the
alternative: less code, at the cost of the conflict policy no longer sitting beside the operation.
`ConcurrentModificationConflictException` is declared in the project error catalog and maps to `409`.

### Applying it

Annotate the use-case entry point — the application service method when one exists, otherwise the
aggregate service method. The aggregate services it calls keep plain `@Transactional` with default
propagation and join the transaction it opened.

```java
@OptimisticLockingRetry
public OrderDomain place(final OrderPlacementDomain command) {
    final InventoryDomain reserved =
            this.inventoryService.reserve(command.sku(), command.quantity());
    return this.orderService.create(command, reserved.reservationId());
}
```

### Rules

- Repeat only what is safe to repeat in full. An effect published through `@TransactionalEventListener(AFTER_COMMIT)` is safe because a rolled-back attempt never fires it; a direct provider call inside the method is not.
- Never catch the optimistic failure below the annotated method; the interceptor then never sees it and the annotation is decoration.
- Never annotate a method already inside a caller's transaction, and never rely on self-invocation.
- Keep the worst case inside the request budget. The annotation has no wall-clock deadline, so the bound is `maxAttempts` and `maxDelay` together; lower them on a hot path.
- Meter attempts and exhaustions, per `observability-and-logging`. Log the exhaustion only — a routine retry is not a `WARN`.
- Tune by composing a second annotation with its own policy, never by loosening this one.
- Test it as concurrency: drive two transactions to the same row, assert one caller succeeds without an error and that the exhaustion path returns the conflict contract. A single-threaded test proves nothing.

On Spring Boot 3 this is Spring Retry with `@EnableRetry`. On Spring Boot 4 the framework's own
resilience support may remove the need for the library — verify against the effective dependency tree
per [generation differences](../../build-and-dependencies/references/generation-differences.md), and
let `build-and-dependencies` own the declaration. The annotation is project-owned either way, so call
sites never change when the implementation does.

## Pessimistic locking

Use a pessimistic lock only for a justified blocking invariant.

```java
/**
 * Loads inventory for an update while holding a pessimistic database lock.
 *
 * @param sku inventory identifier; must not be {@code null}
 * @return the locked inventory, or empty when it does not exist; never {@code null}
 * @throws PessimisticLockingFailureException when the lock cannot be acquired before the
 *                                            configured timeout expires
 */
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select inventory from InventoryEntity inventory where inventory.sku = :sku")
Optional<InventoryEntity> findForUpdate(@Param("sku") final String sku);
```

Invoke this repository method only inside the write transaction that completes the protected
operation. A pessimistic lock acquired outside a transaction is released immediately and protects
nothing, while still costing a round trip.

Mode selection:

| Mode | Use for |
| --- | --- |
| `PESSIMISTIC_READ` | A shared lock: others may read, nobody may write. Rarely the right answer in an application. |
| `PESSIMISTIC_WRITE` | An exclusive lock. This is the default choice when a pessimistic lock is justified. |
| `PESSIMISTIC_FORCE_INCREMENT` | An exclusive lock that also bumps `@Version`, so optimistic readers elsewhere observe the change. |

## When one aggregate uses both

This is the common case, not an exotic one, and four things about it are easy to get wrong.

- **`@Version` belongs to the entity, not to an operation.** It cannot be present for some methods and absent for others, so keep it whenever any path to that entity uses optimistic concurrency — including paths that also lock. Removing it because "this path locks anyway" silently unprotects every other path. The converse also holds: an entity reached exclusively under a pessimistic lock does not need `@Version`, and adding one there is a column and a check with no reader.
- **A lock does not bump the version; a modification does.** Modifying a pessimistically locked entity increments it through the ordinary update, so a concurrent optimistic writer still sees the conflict. Locking and only reading leaves the version untouched, which is precisely what `PESSIMISTIC_FORCE_INCREMENT` in the mode table above is for: use it when the protected decision is a read, or when a change to a child must mark the aggregate as changed.
- **Do not route a pessimistic failure through the optimistic retry annotation.** `PessimisticLockingFailureException` and `CannotAcquireLockException` are not in its `retryFor` and must not be added. A lock timeout means somebody else is holding the row; retrying immediately adds another waiter to a queue that is already too long, and a deadlock victim usually needs the whole use case reconsidered rather than repeated. Decide retry for a locked path deliberately and separately, with a smaller attempt count.
- **Map the two failures to the same caller-visible contract, from different exceptions.** Both are contention and both are `409` under the error catalog, but they arrive as different types and only one of them is worth retrying first. Declaring them as one condition in the catalog while handling them separately keeps the public contract stable without merging two different operational signals.

Test both paths, and test them as concurrency. An operation that takes a pessimistic lock needs a
test with two real transactions where the second one blocks or times out; the optimistic path needs
its own, as described above. Neither test substitutes for the other.

## Lock timeouts

An unbounded lock wait is a connection held indefinitely. Set a timeout wherever the provider and
database support one.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("select inventory from InventoryEntity inventory where inventory.sku = :sku")
Optional<InventoryEntity> findForUpdateWithTimeout(@Param("sku") final String sku);
```

- The unit and the support level are database-specific. Confirm the behavior against the configured engine rather than assuming; some engines accept only particular values, and a hint the engine silently ignores gives false confidence.
- Keep the timeout below the request budget `spring-boot-patterns` records. A lock wait longer than the caller's timeout produces a response nobody receives while the transaction stays open.
- A timeout raises `PessimisticLockingFailureException`. Translate it at the service boundary exactly as above; contention is not a server fault.

## Lock ordering and deadlocks

When one operation locks more than one row, **lock in a consistent order across the entire
application**, derived from a stable value such as the identifier.

```java
final List<String> skusInLockOrder = requestedSkus.stream()
        .distinct()
        .sorted()
        .toList();

for (final String sku : skusInLockOrder) {
    this.inventoryRepository.findForUpdate(sku)
            .orElseThrow(() -> new InventoryNotFoundException(sku));
}
```

Two operations locking the same two rows in opposite orders deadlock. The database detects it and
kills one transaction, so the symptom is an intermittent failure under load that no single-threaded
test reproduces. Sorting the identifiers before locking removes the possibility rather than reducing
its frequency.

Keep locked transactions especially short: no outbound call, no user interaction, and no work that
could have been done before the lock was taken.

## Uniqueness is a constraint, not a check

An application existence check has a race window that no lock closes, because two transactions can
both find nothing.

```java
try {
    return this.userRepository.saveAndFlush(newUser);
} catch (final DataIntegrityViolationException violation) {
    throw new EmailAlreadyRegisteredException(newUser.getEmail(), violation);
}
```

The unique index is the guarantee; the catch turns the race into the same public conflict a
pre-check would have produced. Keep a friendly pre-check if it improves the common-path message, but
never rely on it for correctness. `sql-database-migration` owns the constraint and
`project-naming-conventions` owns its name.

## Bulk DML

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
statement so it cannot overlap concurrent edits, and record which was chosen.

Bulk DML additionally skips entity callbacks, cascades, and auditing. Anything those would have done
must be done explicitly, by the statement or by the caller.

## Rejected code

Each of these compiles and passes a single-threaded test.

```java
// Catches nothing: with a plain save the version check happens at commit, after this block
// exited, so the framework exception reaches the caller as a 500.
try {
    this.inventoryRepository.save(inventory);
} catch (final ObjectOptimisticLockingFailureException failure) {
    throw new ConcurrentModificationConflictException(sku, failure);
}
```

```java
// Self-invocation: the retry reuses the same failed transaction and the same inconsistent
// persistence context, because the proxy is bypassed.
@Transactional
InventoryDomain reserve(final String sku, final int quantity) {
    try {
        return doReserve(sku, quantity);
    } catch (final ObjectOptimisticLockingFailureException failure) {
        return doReserve(sku, quantity);
    }
}
```

```java
// A hand-written loop where the composed annotation applies, reimplementing backoff, jitter,
// bounds, and exception selection slightly differently each time.
while (true) {
    try {
        return this.inventoryService.reserve(sku, quantity);
    } catch (final ObjectOptimisticLockingFailureException failure) {
        if (++attempt >= 3) {
            throw failure;
        }
    }
}
```

```java
// The failure swallowed below the annotated method, so the interceptor never sees it and the
// annotation is decoration.
@OptimisticLockingRetry
public OrderDomain place(final OrderPlacementDomain command) {
    try {
        return this.orderService.create(command);
    } catch (final OptimisticLockingFailureException failure) {
        throw new OrderPlacementFailedException(failure);
    }
}
```

```java
// No jitter: every loser of a collision wakes in the same millisecond and collides again,
// so retries arrive in waves.
@Backoff(delay = 100L, maxDelay = 1000L, multiplier = 2.0d)

// The version copied from a request, which lets the client decide whether the check passes.
inventory.setVersion(request.version());

// A pessimistic lock with no transaction: released as the method returns, so the caller
// operates on unprotected state while believing it is locked.
return this.inventoryRepository.findForUpdate(sku).orElseThrow();

// Locking in request order: two callers with overlapping keys in different orders deadlock.
for (final String sku : request.skus()) {
    this.inventoryRepository.findForUpdate(sku);
}
```