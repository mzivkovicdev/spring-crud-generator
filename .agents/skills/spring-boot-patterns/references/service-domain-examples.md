# Service and Domain Examples

Use these examples when implementing or reviewing domain models, domain mappers, service contracts, service implementations, focused parameter objects, or repository boundaries. Apply all rules from `../SKILL.md`, `modern-java-21`, `spring-data-jpa`, `application-security`, and `project-naming-conventions`; imports are omitted.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

- [Domain models](#domain-models)
- [Domain mapper](#domain-mapper)
- [Focused service parameter object](#focused-service-parameter-object)
- [Service contract and implementation](#service-contract-and-implementation)
- [Aggregate service: owning an invariant](#aggregate-service-owning-an-invariant)
- [Application service: owning a use case](#application-service-owning-a-use-case)
- [Repository boundary](#repository-boundary)

## Domain models

```java
public record UserDomain(
        Long id,
        String username,
        String email) {
}
```

```java
public record PageDomain<T>(
        List<T> items,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages) {

    public PageDomain {
        items = List.copyOf(items);
    }
}
```

```java
public record UserProfileDomain(
        UserDomain user,
        String organizationName) {
}
```

`UserProfileDomain` is the result of a use case that reads from two aggregates; it belongs to the
application service layer, not to either aggregate.

`PageDomain` prevents Spring Data's `Page` from becoming a service or REST contract. A project may use a differently named framework-independent page result, but it must keep pagination semantics explicit and stable.

## Domain mapper

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserDomainMapper {

    UserDomainMapper INSTANCE = Mappers.getMapper(UserDomainMapper.class);

    UserDomain mapUserEntityToUserDomain(final UserEntity entity);

    List<UserDomain> mapUserEntitiesToUserDomains(final List<UserEntity> entities);

    UserEntity mapToNewUserEntity(
            final String username,
            final String email,
            final String passwordHash,
            final UserStatus status,
            final Instant createdAt);
}
```

This mapper demonstrates the directions owned by `../SKILL.md`: persistence output to domain and
explicit, already-decided creation values to a new entity. Hashing, authorization, normalization,
and business defaults happen before structural mapping.

`mapToNewUserEntity` targets the single public constructor of the `UserEntity` shown in
`spring-data-jpa`. Because `id` and `version` are provider-owned and have no constructor parameter
and no setter, they are not writable target properties at all, so no `@Mapping(target = ..., ignore = true)`
entry is needed or valid for them. Adding one would fail the build with an unknown-target-property
error. Keep `ReportingPolicy.ERROR` and run annotation processing so any other incompatibility
between mapper and entity fails the build rather than a request.

## Focused service parameter object

Use separate parameters by default when a project-owned method has up to seven declared parameters and the signature remains clear. The following seven-parameter signature is acceptable; it does not need a custom input class solely because it is near the limit:

```java
public interface UserProfileService {

    UserDomain updateProfile(
            final Long userId,
            final String username,
            final String displayName,
            final String email,
            final Locale locale,
            final ZoneId timeZone,
            final boolean emailNotificationsEnabled);
}
```

When an eighth project-owned parameter would be required, first group only values that already form a cohesive domain concept or enforce an invariant. Keep the target identifier separate:

```java
public record UserProfileDetailsDomain(
        String username,
        String displayName,
        String email,
        Locale locale,
        ZoneId timeZone,
        boolean emailNotificationsEnabled,
        boolean productUpdatesEnabled) {
}
```

```java
public interface UserProfileService {

    UserDomain updateProfile(
            final Long userId,
            final UserProfileDetailsDomain profileDetails);
}
```

Do not introduce a catch-all input class to hide unrelated values, and do not create a custom input class for every service method. A focused parameter/value object may still be appropriate below the threshold when it is already a stable domain concept. A REST mapper may map a request TO to the focused domain input, but the service must not depend on that TO.

## Service contract and implementation

The service interface is optional and the decision is recorded in
`docs/project-profile.md`. Both shapes appear below. Use exactly one of them across the project.

### Shape A: concrete service, no interface

This is the default when the profile records no interface convention and no concrete reason for a
boundary exists. Everything the interface would have carried — caller-facing Javadoc, method
validation constraints, `@Validated`, `@Service`, transactions — lives on the one class. Mockito
mocks this class directly, so unit testing is unaffected.

```java
/**
 * Implements user application operations used by inbound adapters.
 */
@Service
@Validated
@Transactional(readOnly = true)
public class UserService {

    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(
            final Clock clock,
            final PasswordEncoder passwordEncoder,
            final UserRepository userRepository) {

        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    /**
     * Returns a user by identifier.
     *
     * @param userId user identifier; must not be {@code null}
     * @return       the matching user; never {@code null}
     * @throws ConstraintViolationException when the identifier violates a structural constraint
     * @throws ResourceNotFoundException    when no user exists for the supplied identifier
     */
    public UserDomain getById(@NotNull final Long userId) {
        return this.userRepository.findById(userId)
            .map(UserDomainMapper.INSTANCE::mapUserEntityToUserDomain)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
```

`getById` is shown in full. The same class also declares `create`, `getAll`, `updateById`, and
`deleteById` — the operations the `users` aggregate owns and the ones the controller example calls
directly. They follow the same shape: `@Transactional` on the writes, entities mapped to domain
objects before returning, and `ResourceNotFoundException` for a missing identifier. `create` appears
in full under [Shape B](#shape-b-interface-plus-implementation).

Do not introduce `UserService` plus an empty `UserServiceImpl` in order to reach Shape B. An
interface with one implementation, no external implementor, and no substitution requirement adds a
file and a jump without adding a contract.

### Shape B: interface plus implementation

Use this shape when `docs/project-profile.md` records the `*ServiceImpl` convention, or when a real
boundary exists: another module implements the contract, more than one implementation is deployed,
or the type is a port with substitutable adapters.

Only the difference from Shape A is shown. The interface carries the contract; `getAll`,
`updateById`, and `deleteById` follow the same form and are omitted.

```java
/**
 * Defines user operations for the user aggregate.
 */
public interface UserService {

    /**
     * Returns a user by identifier.
     *
     * @param userId user identifier; must not be {@code null}
     * @return       the matching user; never {@code null}
     * @throws ConstraintViolationException when the identifier violates a structural constraint
     * @throws ResourceNotFoundException    when no user exists for the supplied identifier
     */
    UserDomain getById(@NotNull final Long userId);

    /**
     * Creates a user.
     *
     * @param username    username that satisfies the application contract
     * @param email       email address that satisfies the application contract
     * @param rawPassword raw password accepted only at the hashing boundary
     * @return            the created user; never {@code null}
     * @throws ConstraintViolationException when an argument violates a structural constraint
     */
    UserDomain create(
            @NotBlank @Size(max = 120) final String username,
            @NotBlank @Email @Size(max = 254) final String email,
            @NotBlank @Size(max = 128) final String rawPassword);
}
```

The implementation carries the annotations, the dependencies, and the bodies from Shape A, with no
Javadoc and no validation constraints repeated. One method shows the pattern; the rest are identical
to Shape A apart from `@Override`:

```java
@Service
@Validated
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // Remaining fields, constructor, and methods are those of Shape A.

    @Override
    public UserDomain getById(final Long userId) {
        return this.userRepository.findById(userId)
                .map(UserDomainMapper.INSTANCE::mapUserEntityToUserDomain)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
```

In Shape B, the interface is the single source for service Javadoc and validation constraints.
Overriding methods inherit that documentation automatically; omit implementation Javadoc unless it
adds meaningful caller-visible detail. In Shape A, the same Javadoc and constraints sit on the
concrete class instead. `updateById` deliberately calls `save` after explicit mutations and maps the
returned entity.

`existingUser.setUsername(username).setEmail(email)` uses the fluent entity setters shown in
`spring-data-jpa`. Plain `void` setters are equally acceptable; the choice is recorded in
`docs/project-profile.md` and applied consistently, and with `void` setters the same code becomes
two statements.

`UserDomainMapper.INSTANCE` is the generated static MapStruct member. `modern-java-21` names this an
explicit exception to its service-locator rule because the mapper is stateless, generated, and
performs no I/O; do not inject it, and do not extend the exception to any other collaborator. Do not replace it with dirty-checking-only persistence or `saveAndFlush` without a documented immediate-flush requirement. Translate expected persistence failures into the stable application error contract and test the real database constraint.

## Aggregate service: owning an invariant

`UserService` above is an aggregate service. Its aggregate is the `users` root plus its
`user_address` children: an address cannot exist without its user, nothing references an address by
its own identifier, and "exactly one address is primary" is a rule about the user, not about the
address row.

This excerpt adds one method and one field to the `UserService` declared under Shape A; the class
annotations, the existing constructor parameters, and the existing methods are unchanged.
`UserAddressRepository` is the second repository of the same aggregate, which is why this service
holds both and no other service does. `UserEntity.addAddress` appends the address and clears any
previous primary flag — that method is where the invariant is actually enforced.
`NewAddressDomain` is a focused parameter object declared like the one under
[focused service parameter object](#focused-service-parameter-object). `ApplicationError` is the
error catalog and `BusinessValidationException` the project validation exception, both described in
`../SKILL.md`. `UserStatus` is the domain enum used by the Shape A example above.
`UserAddressEntity` is the child entity of this aggregate; its single public constructor takes the
owning `UserEntity` and the address values, following the entity creation rule in `spring-data-jpa`.
`UserAddressRepository` is a `JpaRepository` for it.

```java
    private final UserAddressRepository userAddressRepository;

    /**
     * Adds an address to a user and applies the single-primary-address invariant.
     *
     * <p>Joins the caller's transaction when one is open. The address row and the updated user are
     * written together in every case.
     *
     * @param userId     user identifier; must not be {@code null}
     * @param newAddress address values to store; must not be {@code null}
     * @return           the user including the stored address; never {@code null}
     * @throws ResourceNotFoundException   when no user exists for the supplied identifier
     * @throws BusinessValidationException when the user is not in a state that accepts addresses
     */
    @Override
    @Transactional
    public UserDomain addAddress(
            @NotNull final Long userId,
            @NotNull final NewAddressDomain newAddress) {

        final UserEntity user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessValidationException(ApplicationError.USER_NOT_MODIFIABLE);
        }

        final UserAddressEntity address = new UserAddressEntity(
                user, newAddress.street(), newAddress.city(), newAddress.primary());

        this.userAddressRepository.save(address);
        user.addAddress(address);
        this.userRepository.save(user);

        return UserDomainMapper.INSTANCE.mapUserEntityToUserDomain(user);
    }
```

The `@Transactional` here uses default propagation. Called from a use case it joins that
transaction; called directly by a job it opens its own, so the two writes are never split. The
use-case boundary is still the application service — this annotation only removes the failure mode
where a direct caller commits each write separately.

## Application service: owning a use case

`UserManagementApplicationService` coordinates two aggregates and owns the transaction the use case
runs in. It holds no repository: one here would give the `users` aggregate a second write path that
bypasses `UserService` and its invariant.

`OrganizationService` is the aggregate service for the `organization` root and the membership rows
it owns. This example uses three of its operations: `getJoinable` returns an organization that
currently accepts members or throws, `addMember` records the membership, and `getByMemberId` returns
the organization a user belongs to or throws. `OrganizationDomain` is its domain record with `id()`
and `displayName()`. `UserProfileDomain` is a domain record combining a `UserDomain` with that
display name, and `UserRegisteredEvent` is a project event record.

```java
@Service
@Transactional
public class UserManagementApplicationService {

    private final UserService userService;
    private final OrganizationService organizationService;
    private final ApplicationEventPublisher eventPublisher;

    // Constructor omitted; all three dependencies are required and assigned to final fields.

    /**
     * Registers a user as a member of an organization.
     *
     * <p>Defines the transaction for the use case. When the organization does not accept members,
     * or the membership cannot be recorded, the user is not created either. The event is published
     * for delivery after commit, so no notification is sent for a registration that rolled back.
     *
     * @param organizationId organization the user joins; must not be {@code null}
     * @param username       requested username; must not be {@code null}
     * @param email          requested email address; must not be {@code null}
     * @param rawPassword    plain password, hashed inside {@code UserService} and never stored raw
     * @return               the created user; never {@code null}
     * @throws ResourceNotFoundException   when the organization does not exist
     * @throws BusinessValidationException when the organization does not accept new members
     */
    public UserDomain register(
            final Long organizationId,
            final String username,
            final String email,
            final String rawPassword) {

        final OrganizationDomain organization = this.organizationService.getJoinable(organizationId);
        final UserDomain user = this.userService.create(username, email, rawPassword);
        this.organizationService.addMember(organization.id(), user.id());

        this.eventPublisher.publishEvent(new UserRegisteredEvent(user.id(), organization.id()));
        return user;
    }

    /**
     * Returns a user together with the display name of the organization they belong to.
     *
     * @param userId user identifier; must not be {@code null}
     * @return       the combined profile; never {@code null}
     * @throws ResourceNotFoundException when the user or its organization no longer exists
     */
    @Transactional(readOnly = true)
    public UserProfileDomain getProfile(final Long userId) {
        final UserDomain user = this.userService.getById(userId);
        final OrganizationDomain organization = this.organizationService.getByMemberId(userId);

        return new UserProfileDomain(user, organization.displayName());
    }
}
```

`register` writes to two aggregates, and neither aggregate service knows about the other. That is
exactly why the boundary sits here: a failure in `addMember` must leave no user behind, and only the
method that spans both can guarantee it. Note also what is *not* here — the rule about which
organizations accept members lives in `getJoinable`, inside the aggregate that owns it, so it is not
repeated by every caller that creates a user.

`UserRegisteredEvent` is consumed by a `@TransactionalEventListener(phase = AFTER_COMMIT)` listener
rather than by a direct call to a notification component, so a rollback cannot leave a message
already sent. That is the whole of what after-commit delivery guarantees here. This example assumes
a profile that records the listener alone as sufficient for this effect; if a lost registration
notice were unacceptable, `../SKILL.md` requires an outbox row written by this same transaction.

`getProfile` composes one read from each aggregate, and `readOnly` takes effect because this is the
outermost transactional method.

Note what this class does **not** contain. Listing, updating, and deleting a user stay inside the
`users` aggregate, so they have no method here — the controller calls `UserService` for those. A
forwarding method would add a second name for one operation and a second place to keep in sync, and
repeated across a few features it turns this class into a facade over the whole application.

Both examples follow the `get` and `find` distinction in `project-naming-conventions`: `getById`,
`getJoinable`, and `getByMemberId` return a value or throw, while a method that may legitimately
return nothing is named `find...` and returns `Optional`.

## Repository boundary

```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(final String email);

    @Query("""
            select userEntity
            from UserEntity userEntity
            where userEntity.department.id = :departmentId
            order by userEntity.id asc
            """)
    Slice<UserEntity> findByDepartmentId(
            @Param("departmentId") final Long departmentId,
            final Pageable pageable
    );

    boolean existsByEmail(final String email);
}
```

Every collection query is bounded and deterministically ordered. Apply `spring-data-jpa` before copying or extending a repository pattern; use an explicit projection when a read path does not need a complete entity.
