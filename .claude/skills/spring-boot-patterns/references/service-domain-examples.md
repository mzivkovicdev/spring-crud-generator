# Service and Domain Examples

Use these examples when implementing or reviewing domain models, domain mappers, service contracts, service implementations, focused parameter objects, or repository boundaries. Apply all rules from `../SKILL.md`, `modern-java-21`, `spring-data-jpa`, `application-security`, and `project-naming-conventions`; imports are omitted.

## Contents

- [Domain models](#domain-models)
- [Domain mapper](#domain-mapper)
- [Focused service parameter object](#focused-service-parameter-object)
- [Service contract and implementation](#service-contract-and-implementation)
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

`PageDomain` prevents Spring Data's `Page` from becoming a service or REST contract. A project may use a differently named framework-independent page result, but it must keep pagination semantics explicit and stable.

## Domain mapper

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserDomainMapper {

    UserDomainMapper INSTANCE = Mappers.getMapper(UserDomainMapper.class);

    UserDomain mapUserEntityToUserDomain(final UserEntity entity);

    List<UserDomain> mapUserEntitiesToUserDomains(final List<UserEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserEntity mapToNewUserEntity(
            final String username,
            final String email,
            final String passwordHash,
            final UserStatus status,
            final Instant createdAt);
}
```

Use the mapper to create a new entity from explicit, already-decided creation values and to map persistence results to domain objects. Hashing, authorization, defaults with business meaning, normalization, and other behavior belong before mapping. Do not use a MapStruct `@MappingTarget` method to mutate an existing entity during an update.

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

```java
/**
 * Defines user application operations used by inbound adapters.
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

    /**
     * Returns one bounded page of users.
     *
     * @param pageNumber zero-based page number; must not be {@code null}
     * @param pageSize   page size from 1 through 100; must not be {@code null}
     * @return           a framework-independent page result; never {@code null}
     * @throws ConstraintViolationException when an argument violates a structural constraint
     */
    PageDomain<UserDomain> getAll(
            @NotNull @PositiveOrZero final Integer pageNumber,
            @NotNull @Min(1) @Max(100) final Integer pageSize);

    /**
     * Updates the editable user profile fields.
     *
     * @param userId   user identifier; must not be {@code null}
     * @param username new username
     * @param email    new email address
     * @return         the updated user state; never {@code null}
     * @throws ConstraintViolationException when an argument violates a structural constraint
     * @throws ResourceNotFoundException    when no user exists for the supplied identifier
     */
    UserDomain updateById(
            @NotNull final Long userId,
            @NotBlank @Size(max = 120) final String username,
            @NotBlank @Email @Size(max = 254) final String email);

    /**
     * Deletes a user by identifier.
     *
     * @param userId user identifier; must not be {@code null}
     * @throws ConstraintViolationException when the identifier violates a structural constraint
     * @throws ResourceNotFoundException    when no user exists for the supplied identifier
     */
    void deleteById(@NotNull final Long userId);
}
```

```java
@Service
@Validated
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final UserDomainMapper USER_DOMAIN_MAPPER = UserDomainMapper.INSTANCE;

    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserServiceImpl(
            final Clock clock,
            final PasswordEncoder passwordEncoder,
            final UserRepository userRepository) {

        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public UserDomain getById(final Long userId) {
        return this.userRepository.findById(userId)
            .map(USER_DOMAIN_MAPPER::mapUserEntityToUserDomain)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Override
    @Transactional
    public UserDomain create(
            final String username,
            final String email,
            final String rawPassword) {

        final String passwordHash = this.passwordEncoder.encode(rawPassword);
        final UserEntity newUser = USER_DOMAIN_MAPPER.mapToNewUserEntity(
                username,
                email,
                passwordHash,
                UserStatus.PENDING_VERIFICATION,
                Instant.now(this.clock));
        final UserEntity savedUser = this.userRepository.save(newUser);

        return USER_DOMAIN_MAPPER.mapUserEntityToUserDomain(savedUser);
    }

    @Override
    public PageDomain<UserDomain> getAll(final Integer pageNumber, final Integer pageSize) {
        final Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Order.asc("id"))
        );
        final Page<UserEntity> users = this.userRepository.findAll(pageable);
        final List<UserDomain> items = USER_DOMAIN_MAPPER.mapUserEntitiesToUserDomains(
                users.getContent()
        );

        return new PageDomain<>(
                items,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    @Override
    @Transactional
    public UserDomain updateById(
            final Long userId,
            final String username,
            final String email) {

        final UserEntity existingUser = this.getEntityById(userId);
        existingUser.setUsername(username)
                .setEmail(email);
        final UserEntity savedUser = this.userRepository.save(existingUser);

        return USER_DOMAIN_MAPPER.mapUserEntityToUserDomain(savedUser);
    }

    @Override
    @Transactional
    public void deleteById(final Long userId) {
        final UserEntity existingUser = this.getEntityById(userId);
        this.userRepository.delete(existingUser);
    }

    private UserEntity getEntityById(final Long userId) {
        return this.userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
```

The interface is the single source for service Javadoc and validation constraints; do not duplicate them on implementation methods. `updateById` deliberately calls `save` after explicit mutations and maps the returned entity. Do not replace it with dirty-checking-only persistence or `saveAndFlush` without a documented immediate-flush requirement. Translate expected persistence failures into the stable application error contract and test the real database constraint.

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