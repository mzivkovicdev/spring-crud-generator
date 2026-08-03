# Integration Test Examples

Use these examples for application and database-backed integration tests. Apply every rule from
`../SKILL.md`, `spring-boot-patterns`, `spring-data-jpa`, `application-security`,
`modern-java-21`, and `project-naming-conventions`. Imports are omitted.

## Contents

- [HTTP application integration test](#http-application-integration-test)
- [Container and database rules](#container-and-database-rules)
- [Focused persistence integration test](#focused-persistence-integration-test)
- [Rejected integration tests](#rejected-integration-tests)

## HTTP application integration test

The example assumes the project supplies an isolated supported-database container and resets test
data between methods. It intentionally omits `@Transactional`: the request must commit through the
real service transaction before repository verification.

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {

    private static final String USERS_PATH = "/api/v1/users";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    UserApiIntegrationTest(
            @Autowired final MockMvc mockMvc,
            @Autowired final ObjectMapper objectMapper,
            @Autowired final PasswordEncoder passwordEncoder,
            @Autowired final UserRepository userRepository) {

        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Test
    void usersPost_whenRequestIsValid_createsUser() throws Exception {
        final UserCreateTO request = UserTestData.validUserCreateTO();

        this.mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, not(emptyString())))
                .andExpect(jsonPath("$.username").value(request.username()))
                .andExpect(jsonPath("$.email").value(request.email()));

        final UserEntity persistedUser = this.userRepository.findByEmail(request.email())
                .orElseThrow();
        assertThat(persistedUser.getUsername()).isEqualTo(request.username());
        assertThat(this.passwordEncoder.matches(
                request.password(), persistedUser.getPasswordHash())).isTrue();
    }

    @Test
    void usersPost_whenEmailIsInvalid_returnsValidationProblemAndDoesNotPersist() throws Exception {
        final UserCreateTO request = UserTestData.userCreateTOWithInvalidEmail();
        final long initialUserCount = this.userRepository.count();

        this.mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(this.userRepository.count()).isEqualTo(initialUserCount);
        assertThat(this.userRepository.existsByEmail(request.email())).isFalse();
    }
}
```

Match status, `Location`, error code, and schema to the actual API contract. Apply authentication and
authorization helpers required by `application-security`; do not disable the filter chain. For every
negative write case, verify both the public error and the absence of prohibited database state.

## Container and database rules

Reuse one project-owned container configuration instead of declaring a different database per test
class. Pin the image to the production database engine and approved major or exact version. Prefer
the Spring Boot service-connection mechanism when the supported project version provides it;
otherwise register dynamic properties through the project's existing pattern.

Run Flyway or Liquibase migrations in the integration context. Do not let Hibernate create a schema
that bypasses the migration path being verified. Keep the container isolated from production and
shared environments, and never put real credentials in container configuration.

Use a deterministic, project-owned cleanup mechanism outside the HTTP request transaction. Cleanup
must respect foreign keys and sequences required by assertions. Do not rely on method ordering or
another test's inserts. If tests run in parallel, allocate independent data or disable parallelism for
that infrastructure explicitly.

## Focused persistence integration test

Use a repository-focused test when a query, constraint, mapping, lock, or projection needs direct
proof. The actual supported database remains mandatory.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIntegrationTest {

    private final TestEntityManager entityManager;
    private final UserRepository userRepository;

    UserRepositoryIntegrationTest(
            @Autowired final TestEntityManager entityManager,
            @Autowired final UserRepository userRepository) {

        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }

    @Test
    void findByEmail_whenUserExists_returnsUser() {
        final UserEntity existingUser = UserTestData.validUserEntity();
        this.entityManager.persistAndFlush(existingUser);
        this.entityManager.clear();

        final Optional<UserEntity> result = this.userRepository.findByEmail(
                existingUser.getEmail());

        assertThat(result)
                .hasValueSatisfying(foundUser -> {
                    assertThat(foundUser.getId()).isEqualTo(existingUser.getId());
                    assertThat(foundUser.getEmail()).isEqualTo(existingUser.getEmail());
                });
    }

    @Test
    void save_whenEmailAlreadyExists_throwsDataIntegrityViolationException() {
        final UserEntity existingUser = UserTestData.validUserEntity();
        final UserEntity duplicateEmailUser = UserTestData.userEntityWithEmail(
                existingUser.getEmail());
        this.entityManager.persistAndFlush(existingUser);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> this.userRepository.saveAndFlush(duplicateEmailUser));
    }
}
```

Flush explicitly only when the test must force the database to evaluate a constraint or generated
effect at that point. Apply `spring-data-jpa` for transaction, equality, locking, pagination, and
query-plan assertions.

## Rejected integration tests

```java
// Wrong: the service and repository are mocked, so this does not prove integration.
@SpringBootTest
class UserApiIntegrationTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;
}
```

```java
// Wrong: rollback can hide whether the HTTP request committed real database state.
@SpringBootTest
@Transactional
class UserApiIntegrationTest {
}
```
