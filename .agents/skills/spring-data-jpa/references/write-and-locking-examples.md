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
7. [Lock timeouts](#lock-timeouts)
8. [Lock ordering and deadlocks](#lock-ordering-and-deadlocks)
9. [Uniqueness is a constraint, not a check](#uniqueness-is-a-constraint-not-a-check)
10. [Bulk DML](#bulk-dml)
11. [Rejected code](#rejected-code)

## Choosing a strategy

| Situation | Strategy | Why |
| --- | --- | --- |
| Normal concurrent editing of a row by different users | Optimistic, `@Version` | Conflicts are rare; blocking every reader to prevent a rare conflict costs more than resolving it |
| A counter, balance, or quota that must never be lost | Optimistic with a retry, or one atomic UPDATE | Read-modify-write across two transactions loses one of them silently |
| A measured hot row where optimistic retries keep failing | Pessimistic write lock | Only after measuring; contention is the justification, not the fear of it |
| An invariant spanning rows that must not be read mid-change | Pessimistic write lock, consistent order | Optimistic checks each row separately and cannot see the invariant |
| Uniqueness of a business key | A database unique constraint | An application check has a race window no lock closes |

Start optimistic. Pessimistic locking is a decision backed by a measurement and recorded in the
change, because it converts a concurrency problem into an availability problem: every blocked caller
now holds a connection while it waits.

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

Before writing any retry, decide which problem is being solved. They look identical and they are not.

| Problem | What happens | What fixes it |
| --- | --- | --- |
| **Internal contention** — two concurrent operations recompute their result from state they re-read | The loser fails the version check at commit | Server-side retry. The second attempt re-reads and recomputes, so it produces a correct result without the caller ever knowing |
| **Stale-client write** — a caller read at T1, a human edited, another caller changed the row at T2, the edit arrives at T3 | The stale values are applied on top of newer ones | Only a version supplied by the caller. Retry cannot help and makes it worse |

**Server-side retry is the default, and it is the backend's job.** A caller should never be asked to
repeat a request because two transactions collided inside the application; that is contention the
application created and the application absorbs. Only after the retry policy is exhausted does a
`409 Conflict` reach the caller, and by then it means genuine sustained contention, not a routine
race.

The limit is worth stating precisely, because it is the reason a retry cannot be the whole answer.
Retry works when the operation **recomputes** its result from re-read state: decrement a quantity,
advance a status, apply a delta. On the second attempt the entity is loaded again, the new state is
observed, and the outcome is correct. Retry does **not** work when the operation **overwrites** with
values a caller computed earlier from state it no longer has: a full-representation update where a
person edited a stale form. Re-reading changes nothing, because the values being written did not come
from the read. The retry succeeds, no exception is thrown anywhere, and one user's edit silently
disappears.

So the two mechanisms are not alternatives:

- **Partial or transformational updates** — a delta, a state transition, a `PATCH` of named fields —
  are covered by retry alone. Return `409` on exhaustion and require nothing of the caller.
- **Full-representation updates where a human edited a form** need the version the caller actually
  edited. Two shapes are acceptable, and `docs/project-profile.md` records which the project uses:
  a read-only `version` field carried in the update TO, which is the simpler option and needs no
  header handling; or `ETag` with `If-Match`, which is the standards-conformant option and lets
  intermediaries participate. With the header, a mismatch is `412 Precondition Failed` and a missing
  header is rejected as `428 Precondition Required`; with the TO field, the mismatch is `409`. Either
  way the version is **verified, never applied** — `@Version` stays owned by the provider and is
  never assigned from a request.

`spring-boot-patterns` owns where the check sits in the layers and `rest-api-contract` owns
documenting whatever the project chose. Neither is required when every write is transformational,
which is common; recording `server retry only` is a legitimate answer.

## Translating a lock failure at the service boundary

A lock failure is a normal, expected outcome under concurrency, not a server fault. It becomes a
stable conflict contract at the use-case boundary, never a leaked framework exception.

The aggregate service does not catch it. It performs the write and lets the version check happen at
commit, where the retry advice can observe it:

```java
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(final InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

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
}
```

**Why there is no `try`/`catch` here, and why this is the whole point.** The version check happens at
flush, which by default is at commit — after the method body has returned. A `catch` wrapped around
`save` inside this method therefore catches nothing, and the framework exception escapes as a `500`.
Forcing the check earlier with `saveAndFlush` only to catch it locally is the wrong repair: it
discards a round trip's worth of batching and it puts the conflict policy inside the aggregate, which
has no idea whether the use case may be repeated. Let the exception surface from the transaction
interceptor and handle it one layer out, where the retry interceptor runs.

## Retrying an optimistic failure

Retry is expressed as one project-owned composed annotation. It carries the transaction and the retry
policy together, so every use case that absorbs contention does it identically, the policy is visible
at the call site, and there is no hand-written advice to maintain.

**Do not write a retry loop.** A `while` or `for` loop around a transactional call is the form this
annotation replaces. It reimplements backoff, budget, and exception selection slightly differently in
each use case, and it is the single most common place this pattern is got wrong.

### The annotation

```java
/**
 * Runs the annotated operation in a transaction and repeats it while it loses an optimistic
 * version check.
 *
 * <p>The retry advice wraps the transaction advice, so every attempt commits or rolls back on
 * its own. Annotate only an operation that is safe to repeat in full: it must recompute its
 * result from state it re-reads, and it must produce no external side effect before it commits.
 *
 * <p>Requires {@code @EnableRetry} on a configuration class.
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
        backoff = @Backoff(
                delay = 100L,
                maxDelay = 1000L,
                multiplier = 2.0d,
                random = true
        )
)
public @interface OptimisticLockingRetry {
}
```

Four details in that declaration are load-bearing:

- **`random = true`.** Without it every loser of a collision wakes at exactly the same millisecond and
  collides again, in waves. The randomized multiplier separates them. This is the one attribute most
  often left off, and its absence is invisible until the system is under real load.
- **`maxDelay`.** Bounds the exponential growth, so attempt five does not sit for eight seconds inside
  a request the caller has already given up on.
- **Both exception types.** Spring normally translates the provider failure into
  `ObjectOptimisticLockingFailureException`, but `jakarta.persistence.OptimisticLockException` can
  still surface on some paths. Listing only one leaves a gap that appears rarely and looks like a
  random `500`.
- **`ElementType.METHOD` only.** On a type the annotation would wrap every public method of the class
  in a transaction and a retry, including reads and including operations that are not safe to repeat.
  Apply it per operation, deliberately.

### Why the ordering works without an explicit `@Order`

`@EnableRetry` registers its interceptor at `Ordered.LOWEST_PRECEDENCE - 1`, and the transaction
interceptor defaults to `Ordered.LOWEST_PRECEDENCE`. The retry therefore has higher precedence and
wraps the transaction, which is exactly what this pattern needs: each attempt runs in a fresh
transaction with a fresh persistence context.

That default is what makes the composed annotation correct, so it is also what must not be disturbed:

- Do not set an explicit order on `@EnableTransactionManagement` that would give the transaction
  advice higher precedence than the retry.
- Verify the composed form on the project's Spring Retry version. Both `@Transactional` and
  `@Retryable` are resolved through merged annotations, so a meta-annotation works, but confirm it
  rather than assume it.
- **Prove the ordering with a test that counts committed attempts**, not by reading the annotations.
  Get it inverted and every attempt rejoins a transaction already marked rollback-only: all of them
  fail identically, while the configuration looks correct.

### Exhaustion is part of the contract

When the attempts run out, the last exception propagates. Left alone that is a framework exception
reaching the caller as a `500`, which contradicts the error contract `spring-boot-patterns` owns.
Close it in one of two ways, and use the same one throughout the project:

```java
@Recover
public OrderDomain recover(
        final ObjectOptimisticLockingFailureException failure,
        final OrderPlacementDomain orderPlacementDomain) {

    throw new ConcurrentModificationConflictException(orderPlacementDomain.sku(), failure);
}
```

A `@Recover` method must sit in the same bean, return the same type, and accept the exception
followed by the original parameters. The alternative is to map the optimistic failure to `409` once
in the REST exception advice; that is less code, at the cost of the conflict policy no longer being
visible beside the operation. Either way `ConcurrentModificationConflictException` is declared in the
project error catalog and maps to `409 Conflict`, and the caller learns the resource is contended and
nothing about JPA.

### Applying it

The annotation goes on the use-case entry point — the application service method when one exists,
otherwise the aggregate service method — and never on a method already running inside a caller's
transaction.

```java
@Service
public class OrderPlacementApplicationService {

    private final InventoryService inventoryService;
    private final OrderService orderService;

    // Constructor omitted; both dependencies are required.

    /**
     * Places an order, absorbing inventory contention transparently.
     *
     * @param command the validated placement values; must not be {@code null}
     * @return the placed order; never {@code null}
     * @throws ConcurrentModificationConflictException when contention outlives the retry policy
     */
    @OptimisticLockingRetry
    public OrderDomain place(final OrderPlacementDomain command) {
        final InventoryDomain reserved =
                this.inventoryService.reserve(command.sku(), command.quantity());
        return this.orderService.create(command, reserved.reservationId());
    }
}
```

The aggregate services it calls stay annotated `@Transactional` with the default propagation, so they
join the transaction the composed annotation opened.

### Rules

- **Only annotate an operation that is safe to repeat in full.** It must recompute from state it
  re-reads, and it must not have produced an external side effect. An effect published through
  `@TransactionalEventListener(AFTER_COMMIT)` is safe, because a rolled-back attempt never fires it; a
  direct call to a payment provider inside the method is not.
- **Do not catch the optimistic failure below the annotated method.** A swallowed
  `ObjectOptimisticLockingFailureException` never reaches the retry interceptor, and the annotation
  becomes decoration.
- **Do not annotate a method a caller already wrapped in a transaction**, and never rely on
  self-invocation. Both defeat the fresh-transaction requirement or bypass the proxy entirely.
- **Keep the worst case inside the request budget.** Five attempts with this backoff can spend roughly
  two seconds in pauses alone, before the work itself. Spring Retry's annotation has no wall-clock
  deadline, so the bound is the attempt count and `maxDelay` together — check them against the request
  budget `spring-boot-patterns` records, and lower `maxAttempts` for an operation on a hot path.
- **Meter attempts and exhaustions**, per `observability-and-logging`. An operation that routinely
  succeeds on the fourth attempt is a design problem reporting itself as healthy. Log the exhaustion
  only; a routine retry is not a `WARN`.
- **Tune per operation by composing another annotation**, not by loosening this one. A hot path that
  needs three attempts gets its own composed annotation with its own policy, so both policies stay
  declarative and reviewable.
- **Test it as concurrency, not as configuration.** A single-threaded test proves nothing here. Drive
  two transactions to the same row, assert one caller succeeds without an error, assert the attempt
  counter moved, and assert the exhaustion path returns the conflict contract.

### Which retry implementation, by generation

`build-and-dependencies` owns the declaration and the version; state the requirement to it rather than
editing a build file from here.

| | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Implementation | Spring Retry (`spring-retry`), `@EnableRetry` | Spring Framework's own resilience support may remove the need for a separate library |
| Consequence | declare the dependency and enable it | verify what the framework provides before adding `spring-retry` |

Treat that second column as a lookup, not an answer: it names artifacts and annotations, which is the
class of value this skill set refuses to trust from memory. Verify it against the project's effective
dependency tree and the upstream documentation, exactly as
[generation differences](../../build-and-dependencies/references/generation-differences.md) requires.
Whichever implementation the project uses, the composed annotation is project-owned, so call sites
never change when it does — only the annotation's own declaration does.

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

```java
// Rejected: catches nothing. With a plain save, the version check happens at commit,
// after this block has exited, so the framework exception reaches the caller as a 500.
try {
    this.inventoryRepository.save(inventory);
} catch (final ObjectOptimisticLockingFailureException failure) {
    throw new ConcurrentModificationConflictException(sku, failure);
}
```

```java
// Rejected: self-invocation. The retry reuses the same failed transaction and the same
// inconsistent persistence context, because the proxy is bypassed.
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
// Rejected: a hand-written retry loop where the project's composed annotation applies. It
// reimplements backoff, jitter, bounds, and exception selection, slightly differently each time.
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
// Rejected: the failure swallowed below the annotated method. The annotation is decoration,
// because the exception the retry exists to observe never reaches the interceptor.
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
// Rejected: no backoff jitter. Every loser of a collision wakes in the same millisecond and
// collides again, so the retries arrive in waves and make the contention worse.
@Backoff(delay = 100L, maxDelay = 1000L, multiplier = 2.0d)
```

```java
// Rejected: a pessimistic lock with no transaction. The lock is released as the method
// returns, so the caller operates on unprotected state while believing it is locked.
InventoryEntity load(final String sku) {
    return this.inventoryRepository.findForUpdate(sku).orElseThrow();
}
```

```java
// Rejected: the version copied from a client request. The client then decides whether the
// conflict check passes, which removes the check.
inventory.setVersion(request.version());
```

```java
// Rejected: locking in request order. Two callers with overlapping SKUs in different
// orders deadlock under load.
for (final String sku : request.skus()) {
    this.inventoryRepository.findForUpdate(sku);
}
```
