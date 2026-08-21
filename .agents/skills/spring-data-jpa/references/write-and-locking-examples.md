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
3. [Translating a lock failure at the service boundary](#translating-a-lock-failure-at-the-service-boundary)
4. [Retrying an optimistic failure](#retrying-an-optimistic-failure)
5. [Pessimistic locking](#pessimistic-locking)
6. [Lock timeouts](#lock-timeouts)
7. [Lock ordering and deadlocks](#lock-ordering-and-deadlocks)
8. [Uniqueness is a constraint, not a check](#uniqueness-is-a-constraint-not-a-check)
9. [Bulk DML](#bulk-dml)
10. [Rejected code](#rejected-code)

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

## Translating a lock failure at the service boundary

A lock failure is a normal, expected outcome under concurrency, not a server fault. It becomes a
stable conflict contract at the service boundary, never a leaked framework exception.

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

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);

        try {
            return InventoryDomainMapper.INSTANCE.mapInventoryEntityToInventoryDomain(
                    this.inventoryRepository.saveAndFlush(inventory));
        } catch (final ObjectOptimisticLockingFailureException failure) {
            throw new ConcurrentModificationConflictException(sku, failure);
        }
    }
}
```

`ConcurrentModificationConflictException` is a project exception declared in the error catalog that
`spring-boot-patterns` owns, and it maps to `409 Conflict`. The caller learns that the resource
changed underneath it and can retry with fresh state; it learns nothing about JPA.

**`saveAndFlush` is deliberate here, and it is the whole point of the example.** The version check
happens at flush, which by default is at commit — after the method body has returned. A `try` block
wrapped around a plain `save` therefore catches nothing, and the framework exception escapes to the
caller as a `500`. This is the most frequent mistake in the pattern. Either force the check to a
point the `catch` can observe, as above, or place the translation in an exception handler and accept
that the service method cannot react to the conflict itself.

## Retrying an optimistic failure

Retry only when repeating the **complete** operation is safe, and retry outside the failed
transaction. A retry inside it reuses a persistence context that is already inconsistent.

```java
@Service
public class InventoryReservationApplicationService {

    private static final int MAXIMUM_ATTEMPTS = 3;

    private final InventoryService inventoryService;

    public InventoryReservationApplicationService(final InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    InventoryDomain reserveWithRetry(final String sku, final int quantity) {
        ConcurrentModificationConflictException lastConflict = null;

        for (int attempt = 1; attempt <= MAXIMUM_ATTEMPTS; attempt++) {
            try {
                return this.inventoryService.reserve(sku, quantity);
            } catch (final ConcurrentModificationConflictException conflict) {
                lastConflict = conflict;
            }
        }

        throw lastConflict;
    }
}
```

Rules:

- The retry sits in a **different bean** from the transactional method. A retry loop inside the transactional method never starts a new transaction, because self-invocation bypasses the proxy — `spring-boot-patterns` owns that rule.
- Bound the attempts and count the retries as a metric, per `observability-and-logging`. Unbounded retry against a permanently contended row is an outage that reports itself as healthy.
- Do not retry an operation with an external side effect unless the effect is idempotent.
- Where `docs/project-profile.md` records a resilience library, use its retry support instead of a hand-written loop. Do not introduce one merely to avoid writing this loop.

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
public InventoryDomain reserve(final String sku, final int quantity) {
    try {
        return doReserve(sku, quantity);
    } catch (final ObjectOptimisticLockingFailureException failure) {
        return doReserve(sku, quantity);
    }
}
```

```java
// Rejected: a pessimistic lock with no transaction. The lock is released as the method
// returns, so the caller operates on unprotected state while believing it is locked.
public InventoryEntity load(final String sku) {
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
