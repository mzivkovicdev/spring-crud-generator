# Write and Locking Examples

Use these examples when implementing or reviewing bulk DML, persistence-context synchronization, or pessimistic locking. Apply every rule from `../SKILL.md`; imports are omitted.

## Contents

1. [Bulk DML](#bulk-dml)
2. [Pessimistic locking](#pessimistic-locking)

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

return USER_DOMAIN_MAPPER.mapUserEntityToUserDomain(user);
```

The loaded entity can be stale because bulk DML bypasses normal persistence-context synchronization.

## Pessimistic locking

Use a pessimistic lock only for a justified blocking invariant:

```java
/**
 * Loads inventory for an update while holding a pessimistic database lock.
 *
 * @param sku inventory identifier; must not be {@code null}
 * @return the locked inventory, or empty when it does not exist; never {@code null}
 * @throws PessimisticLockingFailureException when the lock cannot be acquired before the
 *                                             configured timeout expires
 */
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select inventory from InventoryEntity inventory where inventory.sku = :sku")
Optional<InventoryEntity> findForUpdate(@Param("sku") final String sku);
```

Invoke this repository method only inside the write transaction that completes the protected operation.
