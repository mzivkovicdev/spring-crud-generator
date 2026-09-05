# Locking and retry examples

Use this file when implementing or reviewing optimistic locking, the retry that absorbs contention,
or pessimistic locking. Apply every rule from `../SKILL.md`; imports are omitted.

**This file carries rules, not only examples.** The strategy-selection table, the retry-versus-
client-version decision, the composed `@OptimisticLockingRetry` annotation for both Spring Boot
generations, lock timeouts, lock ordering, the conditional-`UPDATE` alternative, and the rejected
forms are stated here in full and nowhere else. The `Rules` blocks below are as binding as `../SKILL.md`.

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
10. [Atomic conditional DML](#atomic-conditional-dml)
11. [Uniqueness is a constraint, not a check](#uniqueness-is-a-constraint-not-a-check)
12. [Rejected code](#rejected-code)

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
| A counter, balance, or quota that must never be lost | Optimistic with the retry annotation, or [one atomic `UPDATE`](#atomic-conditional-dml) | Read-modify-write across two transactions loses one of them silently |
| Allocating a limited resource: stock, seats, a licence pool, a numbered sequence | Pessimistic write lock | The invariant is "never oversell", and optimistic retry under real contention degrades into a retry storm rather than a queue |
| That same allocation when it is one row and the whole rule fits in a `WHERE` clause | One conditional `UPDATE`, decided by its affected row count | The same guarantee for less: the row is held from statement to commit instead of from read to commit, with no application round trip inside the window |
| Claiming a work item so exactly one worker processes it | Pessimistic write lock, or a [conditional `UPDATE`](#atomic-conditional-dml) that claims by status | Two workers must not both win; a version check tells them so only after both did the work |
| An invariant spanning rows that must not be read mid-change | Pessimistic write lock, consistent order | Optimistic checks each row separately and cannot see the invariant |
| A measured hot row where optimistic retries keep failing | Pessimistic write lock | Contention observed in production, not feared in review |
| Uniqueness of a business key | A database unique constraint | An application check has a race window no lock closes |

Optimistic is the default because it costs nothing when nothing collides. Every row above that names
a lock or a conditional `UPDATE` is **structurally** so — the invariant itself decides, and needs no
measurement to justify the choice — with one exception. Only the hot-row case requires evidence,
because there the invariant would have been satisfied either way and the lock is bought purely for
throughput.

What a pessimistic lock costs is worth stating plainly, since it is why optimistic wins by default:
it converts a concurrency problem into an availability one. Every blocked caller holds a connection
while it waits, so an unbounded lock wait exhausts the pool long before it corrupts anything.

## Optimistic locking

Add `@Version` to the aggregate root whose consistency matters. Never assign the version value in
Java, never expose it as a writable transport field, and never copy it from a request. The single
exception is a bulk statement, which bypasses the provider's own increment and must therefore carry
it — [atomic conditional DML](#atomic-conditional-dml) below does exactly that, and
[write behavior examples](write-behavior-examples.md#bulk-dml) states the two forms.

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
    private @Nullable Long version;

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

That is also why the field carries `@Nullable` over a `NOT NULL` column. `modern-java-21` owns the
placement rule and the exception behind it.

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

The numbers below are illustrative. `Optimistic retry policy` is a recorded decision, not a constant
of this example: read the value from the profile, or apply the fallback the template records for that
row when the profile is silent. Never copy these three numbers forward as though they were the rule.

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

- **On Spring Boot 3** the default is correct without an explicit order: `@EnableRetry` registers its interceptor at `Ordered.LOWEST_PRECEDENCE - 1` and the transaction interceptor defaults to `Ordered.LOWEST_PRECEDENCE`. Verify meta-annotation support on the project's Spring Retry version rather than assuming it.
- **On Spring Boot 4** the default is correct as well, for the same reason under different names: `@EnableResilientMethods` documents its `order()` default as `Ordered.LOWEST_PRECEDENCE - 1`, and `@EnableTransactionManagement` documents `Ordered.LOWEST_PRECEDENCE`. The lower number is the higher precedence, so the retry advisor is the outer one on both generations.

**Leave both at their defaults.** An explicit order that only restates a default is a second source
of truth, and it outlives the framework change it was written to guard against. If something else in
the advice chain forces an order to be set, set both ends rather than one — half a pair is how a pair
gets inverted.

**On both generations, prove the ordering with a test that counts committed attempts.** The defaults
are documented, not enforced: an order set elsewhere in the project, a custom advisor, or a manually
registered interceptor can invert them, and the inverted arrangement fails on every attempt while the
configuration still reads as correct. Nothing but that test tells the two apart.

### Exhaustion is part of the contract

**When attempts run out, the last original exception propagates.** There is no separate
"retry exhausted" exception to catch: Spring Framework 7 documents that `@Retryable` rethrows the
last exception from the target method, and Spring Retry does the same when no `@Recover` method
applies. What reaches the advice is therefore `ObjectOptimisticLockingFailureException`, not a
project type — so an advice that handles only project exceptions sends it through the catch-all as a
`500`, and the conflict contract this section describes never happens.

So the advice has to translate the Spring type itself, not a project exception.
[Error handling examples](../../spring-boot-patterns/references/error-handling-examples.md) declares
that handler once, along with the catalog constant, the exception hierarchy it relies on, and the
status each contention condition produces. Do not write a second copy here or in a service.

**This applies on both generations.** It is not a Spring Boot 4 workaround for the missing
`@Recover`: a Spring Boot 3 project that chose the advice shape — the recommended, portable one —
needs exactly the same handler, and omitting it is the single most common way this whole mechanism
ends up decorative.

`ConcurrentModificationConflictException` remains the project type for the `@Recover` shape below.
Keeping a handler for it as well costs nothing and lets a project move between the two shapes
without touching the advice.

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
- Test it as concurrency: drive two transactions to the same row, assert one caller succeeds without an error and that the exhaustion path returns `409` rather than `500`. A single-threaded test proves nothing, and asserting only that an exception was thrown proves nothing about the status the caller sees — which is the half that was missing when this mechanism was decoration.

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
 * @param sku inventory identifier
 * @return the locked inventory, or empty when it does not exist
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
- **Give the two failures different caller-visible contracts, because they are different conditions.** An exhausted optimistic retry means the resource genuinely changed under the caller, which is `409`. A lock wait that timed out means the row was busy and nothing about the resource conflicts with the request, which is `503` with `Retry-After`. Both are contention, and that is exactly why merging them loses the signal: one tells a client to re-read and resubmit, the other tells it to come back unchanged. `spring-boot-patterns` owns both catalog constants and states the RFC 9110 reasoning; declare them as two conditions, not one.

Test both paths, and test them as concurrency. An operation that takes a pessimistic lock needs a
test with two real transactions where the second one blocks or times out; the optimistic path needs
its own, as described above. Neither test substitutes for the other.

**Assert the status, not only the exception.** The timed-out caller must receive `503` with a
`Retry-After` header, and the exhausted optimistic caller `409` — both produced by the advice, from
framework exception types. A test that asserts only that the transaction failed passes just as
happily when the caller is receiving `500`, which is precisely the defect these two tests exist to
catch.

## Lock timeouts

An unbounded lock wait is a connection held indefinitely, so every pessimistic lock has a timeout.
Two things about it are easy to get wrong, and both are silent: **the JPA hint does not work on
every engine**, and **the number ends up written down twice** — once for the database and once for
the `Retry-After` header the caller receives.

### The value is recorded once and read, never retyped

`docs/project-profile.md` records **Pessimistic lock timeout** under *Persistence*. Bind it as an
ordinary configuration record — in `config.properties`, like every `@ConfigurationProperties` type
under the layout `spring-boot-patterns` owns — and let both readers take it from there:

```java
@ConfigurationProperties("persistence.locking")
@Validated
public record LockingProperties(
        @NotNull @DurationMin(millis = 1) Duration pessimisticLockTimeout) {
}
```

Register it with `@EnableConfigurationProperties(LockingProperties.class)` on the configuration class
that uses it, or through the project's `@ConfigurationPropertiesScan`. A `@ConfigurationProperties`
record that nothing registers is not a bean, and the injection point fails at startup — loudly, which
is the good case.

- **Keep it below the statement timeout**, which is itself below the request budget `spring-boot-patterns` records.
- **`Retry-After` is derived from this value at runtime, rounded up to whole seconds**, so the header can never be shorter than the wait that produced it. `spring-boot-patterns` owns that header and [deriving Retry-After](../../spring-boot-patterns/references/error-handling-examples.md#deriving-retry-after-from-the-lock-timeout) shows the arithmetic; what matters here is that it reads this property rather than carrying a second number.
- **A `@QueryHint` value must be a compile-time constant**, so no annotation can read this property. That is the practical reason every mechanism below is configuration rather than an annotation, and the reason a per-query override is a deliberate exception rather than the default shape.

### Applying it to the database, by engine

**A positive `jakarta.persistence.lock.timeout` is honoured by very few engines.** The hint compiles,
the build is green, the test passes on the happy path, and on most engines nothing bounds the lock
wait except whatever bounds the statement — which is a different mechanism producing a different
exception. Confirm the mechanism for the engine the profile records before relying on any of this.

| Engine | What actually bounds the wait | How the value gets there |
| --- | --- | --- |
| Oracle | The JPA hint, rendered as `for update wait n` | `spring.jpa.properties.jakarta.persistence.lock.timeout`, from the bound property |
| PostgreSQL | The `lock_timeout` setting. The hint expresses only `NOWAIT`, at value `0`; a positive value is **ignored** | A connection-init statement on the datasource, or `set_config` per transaction |
| MySQL / InnoDB | `innodb_lock_wait_timeout`, in whole seconds | A connection-init statement on the datasource |
| Others | Verify | Treat "the hint is accepted" as no evidence — an ignored hint throws nothing |

**Prefer setting it once per connection over once per transaction.** Both PostgreSQL and MySQL take
it from a connection-init statement, which the pool runs on every connection it opens, so no call
site can forget it:

```yaml
spring:
  datasource:
    hikari:
      # PostgreSQL. On MySQL: set session innodb_lock_wait_timeout = 3
      connection-init-sql: "set lock_timeout = '3000ms'"
```

That is one literal in configuration beside the property it must equal, which is as close to a single
source as a connection-init string allows; assert the pair in a test rather than trusting the two to
stay in step. It also applies to **every** statement on the connection, so check that the migration
tool does not share the pool — a schema change that has to wait behind a long transaction should not
be cancelled by an application-sized lock timeout. Give migrations their own datasource, or narrow
the setting to the transactions that lock:

```java
/**
 * Bounds how long the current transaction waits for a row lock.
 *
 * @param timeout PostgreSQL setting value, such as {@code 3000ms}
 * @return the applied value, as PostgreSQL reports it
 */
@Query(value = "select set_config('lock_timeout', :timeout, true)", nativeQuery = true)
String applyLockTimeout(@Param("timeout") final String timeout);
```

```java
@Transactional
public InventoryDomain reserve(final String sku, final int quantity) {
    this.inventoryRepository.applyLockTimeout(this.lockingProperties.pessimisticLockTimeout()
            .toMillis() + "ms");
    // ... the locking read and the write follow, inside this same transaction.
}
```

Three details make that work. `set_config` is used rather than `SET LOCAL` because `SET` accepts no
bind parameter, so the literal it would need is the second copy of the number this section exists to
remove. The third argument, `true`, scopes the setting to the current transaction, so it reverts on
commit or rollback and cannot leak into the next caller that borrows the pooled connection — calling
it outside a transaction sets nothing and protects nothing. And the value is formatted from the
`Duration` explicitly: `Duration.toString()` yields ISO-8601 such as `PT3S`, which PostgreSQL
rejects.

**A per-query `@QueryHints` override is the one shape that reintroduces a second number**, because
the annotation needs a compile-time constant that no property can supply. Take it only on an engine
that honours the hint and only where one query genuinely needs a different bound, record that the
value is now compile-time, and assert in a test that the constant equals the configured property.
Otherwise the `Retry-After` derived from that property is describing a wait the query no longer has.

### What the failure becomes

A timeout raises `PessimisticLockingFailureException`, and so do a deadlock victim and a
serialization failure through its subclasses. **Do not catch it in the service.** Like the optimistic
check, it surfaces from the transaction interceptor after the method body returned, so a `catch`
around the repository call catches nothing. It is translated once, in the REST exception advice, to
`503` with `Retry-After` — see
[error handling examples](../../spring-boot-patterns/references/error-handling-examples.md).
Contention is not a server fault, but it is also not a conflict with the resource's state.

**Test the timeout, not the configuration.** Hold the row in one transaction, attempt the locked read
in a second, and assert that it fails within the configured bound rather than blocking until the test
framework gives up. That test is the only thing that distinguishes a working timeout from an ignored
hint, and it is the reason the engine table above is worth reading rather than assuming.

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

## Atomic conditional DML

A third option sits between the two locks: one `UPDATE` that carries the invariant in its `WHERE`
clause, where the affected row count is the business answer. This is the operation from the
optimistic example above, written as a single statement.

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("""
        update InventoryEntity inventory
        set inventory.availableQuantity = inventory.availableQuantity - :quantity,
            inventory.version = inventory.version + 1
        where inventory.sku = :sku
          and inventory.availableQuantity >= :quantity
        """)
int reserveQuantity(@Param("sku") final String sku, @Param("quantity") final int quantity);
```

```java
@Transactional
public void reserve(final String sku, final int quantity) {

    if (this.inventoryRepository.reserveQuantity(sku, quantity) == 0) {
        throw new InsufficientInventoryException(sku, quantity);
    }
}
```

Zero rows means the predicate was false — there was not enough stock — not that anything failed. The
statement cannot oversell, because the database evaluates `availableQuantity >= :quantity` against the
row it has just locked, inside the statement that writes it. There is no window between the check and
the write for a second transaction to fit into, so no version check and no retry are needed to close
one.

**Why this can beat the lock.** A pessimistic read takes its lock at the `select … for update` and
holds it until commit, so the application's decision — and every round trip it makes — happens inside
the locked window. The conditional statement takes the same row lock and holds it until commit too,
but it does not take it until it runs, by which point there is no decision left to make. The window
shrinks to the tail of the transaction instead of spanning all of it, and every caller queued behind
it holds a database connection for that much less time. Under real contention that is the difference
between a queue that drains and a pool that empties, which is why the
[resource budgets](../SKILL.md#resource-budgets) treat a lock wait as a pool cost.

What it gives up:

- **It is bulk DML**, so every consequence [write behavior examples](write-behavior-examples.md) states applies here too. One of them is load-bearing for this section: the version column is not incremented for you, which is why the statement above assigns it. On a version-protected row, omitting that assignment leaves optimistic writers elsewhere unaware the row ever changed.
- **The row count answers "did it apply", not "why not".** Distinguishing "no such SKU" from "not enough stock" costs a second query, and a caller that needs the new value costs a read after the clear. Two extra round trips turn the saving back into a loss.
- **It fits one row and one predicate.** When the decision spans several rows, needs application logic between reading and writing, or feeds later work in the same transaction that must re-read what it wrote, take the lock instead.

What it gains in return is a failure mode that is deterministic. Both locks need a test with two real
transactions before anyone can believe them; the zero-row branch here is reached by a single-threaded
test that sets the stock too low. Prove the concurrent behavior once, against the configured engine
and isolation level, and the per-operation tests stay ordinary.

**Isolation decides what a collision does, so confirm the configured level before relying on either
behavior.** On PostgreSQL under `READ COMMITTED`, a statement blocked by another transaction's
uncommitted write waits, and once that transaction commits "the search condition of the command (the
`WHERE` clause) is re-evaluated to see if the updated version of the row still matches" — which is
exactly what makes this pattern safe with no retry. Under `REPEATABLE READ` the same collision
instead aborts the transaction with a serialization failure, which reaches the advice as
`PessimisticLockingFailureException` and is a `503` by the contract above, unless the operation is
idempotent enough to retry deliberately. Engines differ: InnoDB's locking statements read the most
recent committed row rather than the snapshot, so `REPEATABLE READ` there behaves like the first case,
not the second.

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

