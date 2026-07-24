# Service and Domain Examples

Use these examples when implementing or reviewing domain models, domain mappers, service contracts, service implementations, parameter objects, or the repository boundary. Apply all rules from `../SKILL.md`, `modern-java-21`, and `spring-data-jpa`; imports are omitted.

## Contents

- [Domain model](#domain-model)
- [Domain mapper](#domain-mapper)
- [Focused service parameter object](#focused-service-parameter-object)
- [Service contract and implementation](#service-contract-and-implementation)
- [Repository boundary](#repository-boundary)

## Domain model

```java
public record UserDomain(
        Long id,
        String username,
        String email) {
}
```

## Domain mapper

```java
@Mapper
public interface UserDomainMapper {

    UserDomain mapUserEntityToUserDomain(final UserEntity entity);

    List<UserDomain> mapUserEntitiesToUserDomains(final List<UserEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    UserEntity mapToUserEntity(
            final String username,
            final String email,
            final String password);
}
```

Use the mapper to create a new entity from explicit creation values and to map persistence results to domain objects. Do not use a MapStruct `@MappingTarget` method to mutate an existing entity during an update.

## Focused service parameter object

When a service operation would otherwise require seven or more parameters, use a focused domain/service parameter object when the values are cohesive:

```java
public record UserUpdateDomain(
        String username,
        String email,
        String password,
        Details details,
        List<String> roles,
        List<String> permissions) {
}
```

```java
public interface UserService {

    UserDomain updateById(final Long userId, final UserUpdateDomain userUpdate);
}
```

In this example, `userId` plus the six update values would otherwise produce a seven-parameter method. The REST mapper may map the corresponding request TO to `UserUpdateDomain`; the service and domain mapper must not depend on that TO.

## Service contract and implementation

The interface/implementation example assumes `UserService` is an intentional service boundary. If it is not, use one concrete `UserService` class and remove the interface and `impl` package.

```java
public interface UserService {

    /**
     * Returns a user by identifier.
     *
     * @param id user identifier; must not be {@code null}
     * @return   the matching user; never {@code null}
     * @throws ResourceNotFoundException when no user exists for the supplied identifier
     */
    UserDomain getById(final Long id);

    /**
     * Creates a user.
     *
     * @param username username; must satisfy the validated API contract
     * @param email    email address; must satisfy the validated API contract
     * @param password raw password; must satisfy the validated API contract
     * @return         the created domain user; never {@code null}
     */
    UserDomain create(
            final String username,
            final String email,
            final String password);

    /**
     * Returns a page of users.
     *
     * @param pageNumber zero-based page number; must not be negative
     * @param pageSize   requested page size; must be positive and within the supported maximum
     * @return           the requested page of users; never {@code null}
     */
    Page<UserDomain> getAll(final Integer pageNumber, final Integer pageSize);

    /**
     * Updates a user.
     *
     * @param id       user identifier; must not be {@code null}
     * @param username username; must satisfy the validated API contract
     * @param email    email address; must satisfy the validated API contract
     * @param password raw password; must satisfy the validated API contract
     * @return         the updated user; never {@code null}
     * @throws ResourceNotFoundException when no user exists for the supplied identifier
     */
    UserDomain updateById(
            final Long id,
            final String username,
            final String email,
            final String password);

    /**
     * Deletes a user by identifier.
     *
     * @param id user identifier; must not be {@code null}
     * @throws ResourceNotFoundException when no user exists for the supplied identifier
     */
    void deleteById(final Long id);
}
```

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserDomainMapper userMapper = Mappers.getMapper(UserDomainMapper.class);
    private final UserRepository userRepository;

    @Override
    public UserDomain getById(final Long id) {
        return this.userRepository.findById(id)
            .map(this.userMapper::mapUserEntityToUserDomain)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional
    public UserDomain create(
            final String username,
            final String email,
            final String password) {

        final UserEntity user = this.userMapper.mapToUserEntity(
                username, email, password
        );
        final UserEntity saved = this.userRepository.saveAndFlush(user);

        return this.userMapper.mapUserEntityToUserDomain(saved);
    }

    @Override
    public Page<UserDomain> getAll(final Integer pageNumber, final Integer pageSize) {

        final Pageable pageable = PageRequest.of(pageNumber, pageSize);

        return this.userRepository.findAll(pageable)
            .map(this.userMapper::mapUserEntityToUserDomain);
    }

    @Override
    @Transactional
    public UserDomain updateById(
            final Long id,
            final String username,
            final String email,
            final String password) {

        final UserEntity existing = this.getEntityById(id);

        existing.setUsername(username)
            .setEmail(email)
            .setPassword(password);

        final UserEntity updated = this.userRepository.saveAndFlush(existing);
        return this.userMapper.mapUserEntityToUserDomain(updated);
    }

    @Override
    @Transactional
    public void deleteById(final Long id) {

        final UserEntity existing = this.getEntityById(id);
        this.userRepository.delete(existing);
    }

    private UserEntity getEntityById(final Long id) {
        return this.userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
```

## Repository boundary

```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(final String email);

    @Query("SELECT u FROM UserEntity u WHERE u.department.id = :departmentId")
    List<UserEntity> findByDepartmentId(@Param("departmentId") final Long departmentId);

    @Query(
            value = "SELECT * FROM users WHERE created_at > :date",
            nativeQuery = true)
    Page<UserEntity> findRecentUsers(
            @Param("date") final LocalDate date,
            final Pageable pageable);

    boolean existsByEmail(final String email);
}
```

The example includes a derived query, explicit JPQL, a native query used sparingly, and an existence check. Apply `spring-data-jpa` before copying or extending any repository pattern.