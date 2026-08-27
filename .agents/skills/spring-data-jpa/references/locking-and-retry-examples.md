# Locking and retry examples

Use this file when implementing or reviewing optimistic locking, the retry that absorbs contention,
or pessimistic locking. Apply every rule from `../SKILL.md`; imports are omitted.

**This file carries rules, not only examples.** The strategy-selection table, the retry-versus-
client-version decision, the composed `@OptimisticLockingRetry` annotation for both Spring Boot
generations, lock timeouts, lock ordering, and the rejected forms are stated here in full and nowhere
else. The `Rules` blocks below are as binding as `../SKILL.md`.

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
11. [Rejected code](#rejected-code)

Bulk DML bypasses every optimistic version check described here. When a statement touches
version-protected rows, read [write behavior examples](write-behavior-examples.md) as well.

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
    private Long version;

    // Accessors follow the style recorded in docs/project-profile.md.
}
```

The column is created by a migration owned by `sql-database-migration`, `NOT NULL` with a default of
`0`. Adding `@Version` to a mapping without the migration fails at startup under schema validation,
which is the intended behavior.

The field is the wrapper type `Long`, not `long`, in every entity in this skill set. A primitive is
`0` before the provider assigns anything, so a transient instance is indistinguishable from a row at
version zero; `null` says "never persisted" and lets the provider tell the two apart. Use one type
across every entity so the distinction never depends on which entity you are looking at.

Rules:

- The version is checked on flush, not on read. Two transactions can both read version `7`; the second to flush fails.
- A failed check raises `ObjectOptimisticLockingFailureException`. Do not catch it in the repository or in an entity callback.
- `@Version` protects the entity it is declared on. Changing a child row in a `@OneToMany` does not bump the parent's version unless the parent is also modified or `OPTIMISTIC_FORCE_INCREMENT` is requested.
- Bulk DML bypasses the check entirely. See [write behavior examples](write-behavior-examples.md#bulk-dml).

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

**The annotation is project-owned on both Spring Boot generations, and its name and call sites are
identical on both.** What differs is the retry engine it composes, because Spring Boot 4 brings
declarative retry into the framework itself. Write the variant for the generation
`docs/project-profile.md` records; never carry the other generation's attribute names across, because
several of them exist in both and mean different things.

The numbers below are the template's fallback policy — 3 attempts, 50 ms initial backoff, inside a
2 s budget. When the profile records a different `Optimistic retry policy`, use that instead; the
policy is a recorded decision, not a constant of this example.

### Spring Boot 3: Spring Retry

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
        maxAttempts = 3,
        backoff = @Backoff(delay = 50L, maxDelay = 500L, multiplier = 2.0d, random = true)
)
public @interface OptimisticLockingRetry {
}
```

Four details in it are load-bearing:

- **`random = true`.** Without jitter every loser of a collision wakes in the same millisecond and collides again, in waves. This attribute is the one most often left off, and its absence is invisible until real load.
- **`maxDelay`.** Bounds exponential growth, so a late attempt does not sit for seconds inside a request the caller has abandoned.
- **Both exception types.** Spring normally translates to `ObjectOptimisticLockingFailureException`, but `jakarta.persistence.OptimisticLockException` can still surface on some paths.
- **`METHOD` only.** On a type it would wrap every public method in a transaction and a retry, including reads and operations unsafe to repeat.

**`maxAttempts` counts total attempts**, so `3` means one call and two retries.

### Spring Boot 4: the framework's own resilience support

Spring Framework 7 carries declarative retry, so the project needs no retry library. The annotation is
`org.springframework.resilience.annotation.Retryable`, enabled by `@EnableResilientMethods` — not
`org.springframework.core.retry`, which holds the programmatic `RetryTemplate` and `RetryPolicy` and
declares no annotation.

```java
/**
 * Runs the annotated operation in a transaction and repeats it while it loses an optimistic
 * version check. Each attempt gets a fresh transaction.
 *
 * <p>Annotate only an operation safe to repeat in full: it must recompute from state it
 * re-reads and produce no external side effect before it commits. Requires
 * {@code @EnableResilientMethods}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
@Retryable(
        includes = {
                OptimisticLockException.class,
                ObjectOptimisticLockingFailureException.class
        },
        maxRetries = 2L,
        delay = 50L,
        jitter = 25L,
        multiplier = 2.0d,
        maxDelay = 500L,
        timeout = 2000L
)
public @interface OptimisticLockingRetry {
}
```

Five differences decide whether this is the same policy or a different one:

- **`maxRetries` counts retries, not attempts.** `maxRetries = 2` is three total calls, the same as `maxAttempts = 3` above. Copying the Spring Boot 3 number across silently adds an attempt.
- **`includes` replaces `retryFor`.** `excludes` is its counterpart, and a `predicate` handles anything the type list cannot express.
- **`jitter` is a value, not a flag.** There is no `random = true`; jitter is a span added to each computed delay, so `25` means up to 25 ms of spread. Omitting it leaves the retry storm the Spring Boot 3 note warns about, and here the omission is easier to miss because nothing looks switched off.
- **Backoff attributes sit directly on the annotation**, with no nested `@Backoff`.
- **`timeout` is a real wall-clock deadline**, and Spring Retry has no equivalent. Set it to the retry budget the profile records — `2000` here for a 2 s budget — so the worst case is bounded by a number someone decided rather than by whatever `maxRetries` and `maxDelay` happen to multiply out to. It is the single most useful thing this annotation gained.

Every timing attribute is a `long` in the unit `timeUnit()` selects, milliseconds by default. Each also
has a `…String` twin — `delayString`, `maxRetriesString`, `timeoutString` — that accepts a property
placeholder, which is how the policy becomes deployment-configurable without a second annotation.

The framework also publishes a `MethodRetryEvent` per failed attempt. Meter it rather than logging
each one; a routine retry is not a `WARN`.

### Ordering

The retry advice has to wrap the transaction advice. Inverted, every attempt rejoins a transaction
already marked rollback-only: all of them fail identically while the configuration looks correct.

- **On Spring Boot 3** the default is correct without an explicit order: `@EnableRetry` registers its interceptor at `Ordered.LOWEST_PRECEDENCE - 1` and the transaction interceptor defaults to `Ordered.LOWEST_PRECEDENCE`. Do not set an order on `@EnableTransactionManagement` that inverts it, and verify meta-annotation support on the project's Spring Retry version rather than assuming it.
- **On Spring Boot 4** the framework documents no ordering guarantee between the two, so the arrangement is not something to read off a default. Set the order explicitly if the project's Spring version exposes one.

**On both generations, prove the ordering with a test that counts committed attempts.** On Spring
Boot 4 that test is the only evidence you have.

### Exhaustion is part of the contract

When attempts run out the last exception propagates, which is a framework exception reaching the
caller as a `500`. `ConcurrentModificationConflictException` is declared in the project error catalog
as `CONCURRENT_MODIFICATION` and maps to `409`; `spring-boot-patterns` owns both.

**On Spring Boot 4, mapping it in the REST exception advice is the only option**, because the
framework's retry support has no recovery callback. Declare the handler once, and the whole project
is covered:

```java
@ExceptionHandler(ConcurrentModificationConflictException.class)
public ProblemDetail handleConcurrentModification(
        final ConcurrentModificationConflictException exception) {

    return createProblem(ApplicationError.CONCURRENT_MODIFICATION, exception);
}
```

That handler is the one already shown in
[error handling examples](../../spring-boot-patterns/references/error-handling-examples.md), so on
Spring Boot 4 nothing extra is needed beyond making sure the advice also translates the framework's
own exhaustion exception into it.

**On Spring Boot 3** the advice works identically and is the recommended shape. Spring Retry
additionally offers `@Recover`, which keeps the conflict policy beside the operation at the cost of a
method per return type:

```java
@Recover
public OrderDomain recover(
        final ObjectOptimisticLockingFailureException failure,
        final OrderPlacementDomain command) {

    throw new ConcurrentModificationConflictException(command.sku(), failure);
}
```

A `@Recover` method sits in the same bean, returns the same type, and takes the exception followed by
the original parameters. **Choose one shape for the whole project and record it**, because a codebase
where some use cases recover locally and others rely on the advice has two conflict policies and no
way to tell which applied. Choosing the advice keeps the project portable across both generations,
which is why it is the default here.

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
- Keep the worst case inside the request budget the profile records. On Spring Boot 4 say so directly with `timeout`. On Spring Boot 3 there is no wall-clock deadline at all, so the bound is `maxAttempts` and `maxDelay` multiplied out by hand — do that arithmetic rather than assuming, and lower both on a hot path.
- Meter attempts and exhaustions, per `observability-and-logging`. Log the exhaustion only — a routine retry is not a `WARN`.
- Tune by composing a second annotation with its own policy, never by loosening this one.
- Test it as concurrency: drive two transactions to the same row, assert one caller succeeds without an error and that the exhaustion path returns the conflict contract. A single-threaded test proves nothing.

`build-and-dependencies` owns the declaration on both generations, and
[generation differences](../../build-and-dependencies/references/generation-differences.md) carries
the coordinates: on Spring Boot 3 the project declares Spring Retry, on Spring Boot 4 the support is
in the framework and Spring Retry is no longer version-managed. Verify against the effective
dependency tree before writing either annotation. **The annotation is project-owned either way, so no
call site changes when a project moves between generations** — that is the whole reason it exists
instead of the engine's annotation being used directly.

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
// No jitter, Spring Boot 3 form: every loser of a collision wakes in the same millisecond and
// collides again, so retries arrive in waves.
@Backoff(delay = 50L, maxDelay = 500L, multiplier = 2.0d)

// The same defect in the Spring Boot 4 form, and harder to spot because nothing looks
// switched off — the jitter attribute is simply absent and defaults to none.
@Retryable(includes = ObjectOptimisticLockingFailureException.class,
        maxRetries = 2, delay = 50L, multiplier = 2.0d, maxDelay = 500L)

// The Spring Boot 3 attempt count copied onto the Spring Boot 4 annotation: maxRetries counts
// retries, so this is four calls where the policy says three.
@Retryable(includes = ObjectOptimisticLockingFailureException.class, maxRetries = 3)

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

