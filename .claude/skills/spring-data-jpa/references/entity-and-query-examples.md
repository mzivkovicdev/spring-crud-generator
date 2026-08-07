# Entity and Query Examples

Use these examples when implementing or reviewing entity mappings, associations, repositories, projections, fetch plans, dynamic queries, pagination, or SQL access paths. Apply every rule from `../SKILL.md`; imports are omitted.

## Contents

1. [Entity mapping](#entity-mapping)
2. [Association mapping](#association-mapping)
3. [Repository and projection queries](#repository-and-projection-queries)
4. [Fetch plans](#fetch-plans)
5. [Dynamic queries](#dynamic-queries)
6. [Pagination](#pagination)
7. [SQL and indexes](#sql-and-indexes)

## Entity mapping

```java
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "username", nullable = false, length = 120)
    private String username;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserEntity() {
        // Required by the persistence provider.
    }

    public UserEntity(
            final String username,
            final String email,
            final String passwordHash,
            final UserStatus status,
            final Instant createdAt) {

        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }

    public Long getVersion() {
        return this.version;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public UserStatus getStatus() {
        return this.status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public UserEntity setUsername(final String username) {
        this.username = username;
        return this;
    }

    public UserEntity setEmail(final String email) {
        this.email = email;
        return this;
    }

    public UserEntity setStatus(final UserStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserEntity otherUser)) {
            return false;
        }

        return this.id != null && this.id.equals(otherUser.getId());
    }

    @Override
    public int hashCode() {
        return UserEntity.class.hashCode();
    }
}
```

This class is complete and compiles as written; copy its structure rather than a reduced version.

Why each part is there:

- The `protected` no-argument constructor belongs to the provider. The `public` constructor is the single creation path and is what `UserDomainMapper.mapToNewUserEntity` uses, so every mapped creation property is set exactly once.
- `id` and `version` are provider-owned. They have getters and no constructor parameter and no setter, so application code and MapStruct cannot write them.
- Setters exist only for the fields a use case actually updates. Add another setter when a real operation needs it, not preemptively; a setter for `passwordHash` belongs to the credential-change operation that hashes the new value.
- Every `@Column` names its column explicitly so the mapping, the migration, and native SQL cannot drift apart through an implicit naming strategy.
- `equals` and `hashCode` follow the surrogate-identifier strategy from `../SKILL.md`: `instanceof` accepts a provider proxy, the other identifier is read through its getter, and the hash is a constant derived from the class literal so it is stable before and after persistence.

`GenerationType.AUTO` is illustrative, not the project default. Select the identifier strategy for the database recorded in `docs/project-profile.md` and verify its effect on batching and round trips before implementation.

Mutation stays as narrow as the use cases require. Recompile MapStruct-generated sources after changing either side of the mapping, so an incompatible entity fails the build rather than a request.

If `docs/project-profile.md` records plain `void` setters instead of the fluent style shown here, declare them as `void` and split chained calls into separate statements. Everything else in this example is unchanged.

`UserStatus` represents business state, so place it beside the related domain types rather than in a
generic `enums` package. If a different enum exists only to represent persistence state, keep that
enum in the persistence boundary.

## Association mapping

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(
        name = "customer_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_orders_customer"))
private CustomerEntity customer;
```

## Repository and projection queries

Bound every collection result and make ordering deterministic:

```java
public interface UserRepository extends JpaRepository<UserEntity, Long>,
        JpaSpecificationExecutor<UserEntity> {

    boolean existsByEmail(final String email);

    Optional<UserEntity> findByEmail(final String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<UserEntity> findWithRolesById(final Long id);

    Slice<UserEntity> findByStatusOrderByCreatedAtDescIdDesc(
            final UserStatus status,
            final Pageable pageable);
}
```

Reject an unbounded collection query:

```java
List<UserEntity> findAllByStatus(final UserStatus status);
```

Use a closed persistence projection when a bounded read path needs selected columns:

```java
public interface UserSummaryProjection {

    Long getId();

    String getUsername();

    UserStatus getStatus();
}
```

Create `repository.projection` with this first projection; do not create the subpackage in advance.

```java
@Query("""
        select
            user.id as id,
            user.username as username,
            user.status as status
        from UserEntity user
        where user.status = :status
        order by user.createdAt desc, user.id desc
        """)
Slice<UserSummaryProjection> findSummariesByStatus(
        @Param("status") final UserStatus status,
        final Pageable pageable);
```

## Fetch plans

The following loop can trigger N+1 queries:

```java
final List<OrderEntity> orders = this.orderRepository.findAll();

for (final OrderEntity order : orders) {
    LOGGER.debug("Customer: {}", order.getCustomer().getName());
}
```

For a bounded slice that needs one to-one or many-to-one state, use an explicit fetch plan:

```java
@EntityGraph(attributePaths = {"customer"})
Slice<OrderEntity> findByStatusOrderByCreatedAtDescIdDesc(
        final OrderStatus status,
        final Pageable pageable);
```

Disable Open EntityManager in View for REST services:

```properties
spring.jpa.open-in-view=false
```

## Dynamic queries

Reject optional-filter queries that force every predicate into one `OR` expression on a hot path:

```java
@Query("""
        select user
        from UserEntity user
        where (:email is null or lower(user.email) = lower(:email))
          and (:status is null or user.status = :status)
        """)
List<UserEntity> search(
        @Param("email") final String email,
        @Param("status") final UserStatus status);
```

Build only the predicates required by the request:

```java
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserEntity> withFilters(
            final String email,
            final UserStatus status) {

        return (root, query, criteriaBuilder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (email != null) {
                predicates.add(criteriaBuilder.equal(root.get("email"), email));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
```

Place reusable Specification types in `repository.specification`. Keep a one-off predicate with the
repository implementation or query that owns it instead of creating a reusable-looking type.

## Pagination

Reject a collection fetch join combined with pagination:

```java
@Query("""
        select order
        from OrderEntity order
        join fetch order.items
        order by order.createdAt desc
        """)
Page<OrderEntity> findPageWithItems(final Pageable pageable);
```

Page root identifiers first and load the required graph with a bounded second query, or use a projection.

## SQL and indexes

Avoid applying a function to an indexed column by habit:

```sql
SELECT * FROM users WHERE LOWER(email) = LOWER(?);
```

When the application stores an approved normalized email, query the normalized value and select only required columns:

```sql
SELECT id, username, status FROM users WHERE email = ?;
```

```sql
CREATE UNIQUE INDEX uk_users_email ON users (email);
```
