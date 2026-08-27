# Entity and Query Examples

Use this file when implementing or reviewing entity mappings, associations, repositories, projections, fetch plans, dynamic queries, pagination, or SQL access paths. Apply every rule from `../SKILL.md`; imports are omitted.

**This file carries rules, not only examples.** Association ownership, repository design, read
projections, sargable and dynamic queries, pagination and scrolling, SQL and index performance, and
the read-side anti-pattern catalogue are stated here in full and nowhere else, because they apply
while writing exactly this code. The `Rules` blocks below are as binding as `../SKILL.md`.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Entity mapping](#entity-mapping)
2. [Association mapping](#association-mapping)
3. [Repository and projection queries](#repository-and-projection-queries)
4. [Fetch plans](#fetch-plans)
5. [Dynamic queries](#dynamic-queries)
6. [Pagination](#pagination)
7. [SQL and indexes](#sql-and-indexes)
8. [Read-side anti-patterns](#read-side-anti-patterns)

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

    @OneToMany(
            mappedBy = "user",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = true)
    private List<UserAddressEntity> addresses = new ArrayList<>();

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

    public List<UserAddressEntity> getAddresses() {
        return Collections.unmodifiableList(this.addresses);
    }

    /**
     * Adds an address and enforces the aggregate's single-primary-address invariant.
     *
     * @param address address to attach; must not be {@code null}
     */
    public void addAddress(final UserAddressEntity address) {
        if (address.isPrimary()) {
            this.addresses.forEach(existing -> existing.setPrimary(false));
        }
        this.addresses.add(address);
        address.setUser(this);
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

This class is complete and compiles as written, and it is the **only** declaration of `UserEntity` in
this skill set. Every other file that mentions it — the aggregate service in `spring-boot-patterns`,
the mapper, the tests — refers to this one rather than declaring a variant. Copy its structure rather
than a reduced version.

Why each part is there:

- The `protected` no-argument constructor belongs to the provider. The `public` constructor is the single creation path and is what `UserDomainMapper.mapToNewUserEntity` uses, so every mapped creation property is set exactly once.
- `addresses` is the aggregate's child collection, and it carries the invariant rather than exposing it. `addAddress` clears any previous primary flag and keeps both sides of the association in step; `getAddresses` returns an unmodifiable view so no caller can bypass either. The collection is mapped inside the aggregate, which is the one place an association is allowed at all.
- `orphanRemoval` is correct here precisely because an address cannot exist without its user. `CascadeType.ALL` is not used: removing a user is a decision for the aggregate service, not a side effect of a mapping.
- MapStruct selects that constructor **because it is the only `public` one**. Widening the no-argument constructor to `public` would make MapStruct prefer it and silently produce an entity with every mapped property left null. Keep it `protected`, and treat a change to its visibility as a change to the mapping contract.
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

`spring-boot-patterns` decides which entities form one aggregate. That decision constrains every
mapping here: an association may only exist inside an aggregate.

Rules:

- Reference another aggregate root by its identifier, as a plain column, never as a JPA association. A `@ManyToOne` across the boundary hands every caller a writable path into the other aggregate, and no service-layer rule can close it again.
- Copy a value that must not change retroactively — a price at order time, a rate at signing — onto the referencing entity instead of reading it through an association. This is a business rule about history, not a performance choice.
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

The excerpt below is a field of `UserAddressEntity`, the
child of the `users` root and the owning side of the collection declared on `UserEntity` above:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_user_address_user"))
private UserEntity user;
```

`UserAddressEntity` follows the same shape as `UserEntity` above: a `protected` no-argument
constructor, one `public` constructor taking the owning user and the address values, and setters only
where a use case updates. The members the rest of this skill set refers to are `isPrimary()`,
`setPrimary(boolean)`, and the package-visible `setUser(UserEntity)` that `UserEntity.addAddress`
calls to keep both sides in step.

Across aggregates, store the identifier instead. The excerpts below belong to `OrderEntity`, an
illustrative root outside the `user` example, whose customer is a separate root with its own service
and lifecycle:

```java
@Column(name = "customer_id", nullable = false, updatable = false)
private Long customerId;
```

Rejected, on the same field:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "customer_id", nullable = false)
private CustomerEntity customer;
```

The rejected form compiles and works, which is why it survives review unless the rule is explicit.
It hands every holder of an `OrderEntity` a writable path into the customer aggregate, and it invites
a cascade or a fetch plan that loads one aggregate while saving another.

Note what this means for `UserEntity` above: it has a collection but no `@ManyToOne`. The user's
organization membership is a row the `organization` aggregate owns, reached through
`OrganizationService`, not a column or an association on `UserEntity`. That is the same rule seen
from the other side.

## Repository and projection queries

Repository rules:

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

Read-projection rules:

- Use projections for bounded read paths that need only selected columns.
- A persistence projection is neither a TO nor a domain result; map it before leaving the aggregate service that loaded it.
- Keep interface projections closed and top-level. Nested properties can materialize joins and more data than expected.
- Avoid `Object[]`, raw `Tuple`, and `Map<String, Object>` as cross-layer contracts.
- Cover native projections with integration tests against the supported database.

Bound every collection result and make ordering deterministic:

This is the single declaration of `UserRepository` for the whole skill set. `spring-boot-patterns`
shows how an aggregate service calls it and where the entity stops; it does not declare its own
version.

```java
public interface UserRepository extends JpaRepository<UserEntity, Long>,
        JpaSpecificationExecutor<UserEntity> {

    boolean existsByEmail(final String email);

    Optional<UserEntity> findByEmail(final String email);

    @EntityGraph(attributePaths = {"addresses"})
    Optional<UserEntity> findWithAddressesById(final Long id);

    Page<UserEntity> findByStatus(final UserStatus status, final Pageable pageable);

    Slice<UserEntity> findByStatusOrderByCreatedAtDescIdDesc(
            final UserStatus status,
            final Pageable pageable);
}
```

`findByStatus` takes its ordering from the caller's `Pageable` and returns a `Page`, so the caller
must supply a deterministic `Sort` with a unique tie-breaker; `findByStatusOrderByCreatedAtDescIdDesc`
carries the ordering in the method name and returns a `Slice`, so it costs no count query. Both are
here because the choice between them is a real one, and neither is a default.

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

```java
public interface UserAddressCountProjection {

    Long getId();

    long getAddressCount();
}
```

`UserAddressCountProjection` is the one the fetch-plan section uses to avoid loading a collection for
a page of roots. Both live in `repository.projection`; create that subpackage with the first
projection and not in advance.

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

The following loop can trigger N+1 queries: each `getAddresses()` call resolves a lazy collection with
its own query.

```java
final Slice<UserEntity> users = this.userRepository.findByStatusOrderByCreatedAtDescIdDesc(
        UserStatus.ACTIVE, pageable);

for (final UserEntity user : users) {
    LOGGER.debug("Address count: {}", user.getAddresses().size());
}
```

When a single aggregate genuinely needs its children loaded, use an explicit fetch plan on a
single-result method — `findWithAddressesById` above does exactly that.

For a page of roots, do **not** attach a collection entity graph to a paginated method: the provider
either paginates in memory or multiplies rows. Page the identifiers first and load the graph in a
bounded second query, or return a projection that already carries the derived value:

```java
@Query("""
        select user.id as id, count(address.id) as addressCount
        from UserEntity user
            left join user.addresses address
        where user.status = :status
        group by user.id
        """)
Slice<UserAddressCountProjection> findAddressCountsByStatus(
        @Param("status") final UserStatus status,
        final Pageable pageable);
```

Disable Open EntityManager in View for REST services:

```properties
spring.jpa.open-in-view=false
```

## Dynamic queries

Sargability and dynamic-query rules:

- Build only required predicates for optional filters.
- Prefer the JPA static metamodel or Querydsl for non-trivial dynamic queries; raw attribute-name strings fail only at runtime after incompatible refactoring. The metamodel comes from an annotation processor whose artifact differs by generation — state the requirement to `build-and-dependencies`, which owns the processor path, rather than adding it to the build from here.
- Normalize values according to the business contract before querying; do not apply functions to indexed columns by habit.
- Functions, casts, arithmetic, and implicit type conversion on indexed columns can prevent normal index access.
- Avoid leading-wildcard searches on large tables unless a suitable search/index feature is deliberately used.
- Bound `IN` collections; use chunking or a measured database-specific bulk strategy for very large sets.
- Never issue a repository query inside a per-row loop when one set-based query can retrieve the data.
- Allowlist sort fields and directions; never pass user input to `JpaSort.unsafe`.

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

Build only the predicates required by the request, and address attributes through the generated static
metamodel rather than by name:

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
                predicates.add(criteriaBuilder.equal(root.get(UserEntity_.email), email));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get(UserEntity_.status), status));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
```

`UserEntity_` is generated from `UserEntity` by the JPA static metamodel processor, which
`build-and-dependencies` declares on the annotation processor path — the artifact differs by Spring
Boot generation and is listed in
[generation differences](../../build-and-dependencies/references/generation-differences.md). This is
why `../SKILL.md` prefers the metamodel over raw attribute strings: rename `email` on the entity and
`root.get(UserEntity_.email)` stops compiling, while `root.get("email")` keeps compiling and starts
failing at runtime, on whichever request first reaches that filter.

Place reusable Specification types in `repository.specification`. Keep a one-off predicate with the
repository implementation or query that owns it instead of creating a reusable-looking type.

## Pagination

Pagination and scrolling rules:

- Enforce maximum page size at the REST boundary.
- Always sort deterministically with a unique tie-breaker.
- Use `Page` only when the caller needs a total and the count query is acceptably cheap.
- Use `Slice` when only next-page information is needed.
- Prefer keyset scrolling for deep or high-volume traversal when the API can represent a cursor.
- Keyset sort columns must be non-null, deterministic, and supported by an effective index.
- Never paginate or sort database-sized results in memory.
- Supply an explicit `countQuery` when a complex or native paged query cannot be derived correctly or efficiently.

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

SQL and index-performance rules:

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

## Read-side anti-patterns

Reject. The write-behavior and concurrency-policy groups are in
[write and locking examples](write-and-locking-examples.md).

**Entity shape.** Records used as entities; Lombok `@Data` on entities; lazy or mutable associations
in `equals`, `hashCode`, or `toString`; `CascadeType.ALL` without aggregate lifecycle ownership;
cascade remove from a child or shared reference to its parent.

**Fetch plans.** Blanket `FetchType.EAGER`; Open EntityManager in View and
`hibernate.enable_lazy_load_no_trans`; N+1 queries hidden in mappers, serializers, logging, loops, or
accessors; collection fetch joins combined with pagination; multiple collection fetch joins causing
cartesian multiplication; `distinct` used to hide an incorrect join or fetch plan.

**Query shape.** Unbounded repository reads, streams, association traversal, or `IN` predicates;
full-entity loading where a bounded projection suffices; query-per-row loops; optional-filter `OR`
queries and functions on indexed columns on hot paths without verified plans; leading-wildcard
searches on large tables without a search index; unsafe user-controlled sorting; missing,
ineffective, redundant, or speculative indexes.

**Schema and verification.** A mapping change merged without its migration; a mapping whose
constraint or index names differ from the migration's; H2-only persistence verification for another
production database.
