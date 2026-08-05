# Integration Test Examples

Use these examples for application and database-backed integration tests. Apply every rule from
`../SKILL.md`, `spring-boot-patterns`, `spring-data-jpa`, `application-security`,
`modern-java-21`, and `project-naming-conventions`. Imports are omitted.

These tests complement, and never replace, direct unit tests for behavioral application services or
the required `@WebMvcTest` for each REST controller.

## Contents

- [HTTP application integration test](#http-application-integration-test)
- [Authenticated request helper](#authenticated-request-helper)
- [Container and database rules](#container-and-database-rules)
- [Focused persistence integration test when justified](#focused-persistence-integration-test-when-justified)
- [Rejected integration tests](#rejected-integration-tests)

## HTTP application integration test

The example assumes the project selected stateless bearer authentication, JSON problem responses,
an isolated supported-database container, and test-data reset between methods. Adapt those contract
details to the service's selected model. It intentionally omits `@Transactional`: the request must
commit through the real service transaction before repository verification.

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {

    private static final String BEARER_SCHEME = "Bearer";
    private static final String USERS_PATH = "/api/v1/users";

    private final AccessTokenTestClient accessTokenTestClient;
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
        this.accessTokenTestClient = new AccessTokenTestClient(mockMvc, objectMapper);
    }

    @Test
    void usersPost_whenRequestIsValid_createsUser() throws Exception {
        final String accessToken = this.accessTokenTestClient.obtainAccessToken(
                AuthenticationTestData.userWithUsersWriteAccess());
        final UserCreateTO request = UserTestData.validUserCreateTO();

        this.mockMvc.perform(post(USERS_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "%s %s".formatted(BEARER_SCHEME, accessToken))
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
        final String accessToken = this.accessTokenTestClient.obtainAccessToken(
                AuthenticationTestData.userWithUsersWriteAccess());
        final UserCreateTO request = UserTestData.userCreateTOWithInvalidEmail();
        final long initialUserCount = this.userRepository.count();

        this.mockMvc.perform(post(USERS_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "%s %s".formatted(BEARER_SCHEME, accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(this.userRepository.count()).isEqualTo(initialUserCount);
        assertThat(this.userRepository.existsByEmail(request.email())).isFalse();
    }

    @Test
    void usersPost_whenTokenIsMissing_returnsUnauthorizedAndDoesNotPersist() throws Exception {
        final UserCreateTO request = UserTestData.validUserCreateTO();
        final long initialUserCount = this.userRepository.count();

        this.mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        startsWith(BEARER_SCHEME)))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        assertThat(this.userRepository.count()).isEqualTo(initialUserCount);
        assertThat(this.userRepository.existsByEmail(request.email())).isFalse();
    }
}
```

`AuthenticationTestData.userWithUsersWriteAccess()` represents an isolated synthetic identity with
the public OAuth scope `users:write`. Under Spring's default mapping, the resource server derives the
authority `SCOPE_users:write`. Keep token fixtures expressed in public scopes rather than derived
Spring authority names, and do not hardcode credentials in the test method.

Match status, `Location`, error code, and schema to the actual API contract. Keep the real security
filter chain enabled. For every negative write case, verify both the public error and the absence of
prohibited database state. Extend full application coverage to applicable affected wiring,
transactions, migrations, concurrency contracts, and committed state. Overlap an important scenario
with a unit or MVC slice test when this test proves a different boundary.

## Authenticated request helper

Obtain a valid bearer access token through the project's supported isolated authentication flow.
This excerpt applies only when the application owns `/api/v1/auth/token`; adapt the request and
response TOs to the actual contract without bypassing token issuance or validation.

```java
final class AccessTokenTestClient {

    private static final String TOKEN_PATH = "/api/v1/auth/token";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    AccessTokenTestClient(
            final MockMvc mockMvc,
            final ObjectMapper objectMapper) {

        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    String obtainAccessToken(final LoginTO login) throws Exception {
        final MvcResult result = this.mockMvc.perform(post(TOKEN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(login)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        final AccessTokenTO token = this.objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                AccessTokenTO.class);

        return token.accessToken();
    }
}
```

When authentication is externally owned, use its supported protocol against an approved isolated
test provider or container. For valid-token scenarios, do not add a test-only endpoint to production
code, mint tokens directly in the API test, use a mock-token post-processor, or call a live identity
provider. For validation failures that normal issuance cannot produce, a controlled invalid token or
isolated provider configuration may create the invalid input, but it must traverse the real filter
chain and configured decoder. Omit CSRF tokens only when the tested filter chain is stateless bearer
and uses no ambient browser credential.

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

## Focused persistence integration test when justified

Use a repository-focused test when a query, constraint, mapping, lock, or projection needs direct
proof. Also use it for custom JPQL or native SQL, converters, deterministic ordering, pagination,
flush-time failures, and database-specific behavior. The actual supported database remains
mandatory.

Do not create this test for inherited `JpaRepository` CRUD behavior or as a mandatory companion to
every HTTP integration test. When the application-level test already proves a simple persistence
path and no JPA-specific risk remains, it is sufficient.

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
