# Integration Test Examples

Use these examples for application and database-backed integration tests. Apply every rule from
`../SKILL.md`, `spring-boot-patterns`, `spring-data-jpa`, `application-security`,
`modern-java-21`, and `project-naming-conventions`. Imports are omitted.

**This file carries rules, not only examples.** The web-mode choice, the container and database
rules, the cleanup strategy, the deferred-effect and outbox scope, and test selection and suite
execution are stated here in full and nowhere else; `../SKILL.md` routes to them rather than
repeating them. Treat those sections as binding.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

These tests complement, and never replace, direct unit tests for behavioral application services or
the required `@WebMvcTest` for each REST controller.

## Contents

- [Choosing the web mode](#choosing-the-web-mode)
- [HTTP application integration test](#http-application-integration-test)
- [Authenticated request helper](#authenticated-request-helper)
- [Deferred effects, outbox, and concurrency scope](#deferred-effects-outbox-and-concurrency-scope)
- [Container and database rules](#container-and-database-rules)
- [Focused persistence integration test when justified](#focused-persistence-integration-test-when-justified)
- [Rejected integration tests](#rejected-integration-tests)
- [Obtaining a valid token per issuance profile](#obtaining-a-valid-token-per-issuance-profile)
- [Test selection and suite execution](#test-selection-and-suite-execution)

## Choosing the web mode

Choose the web mode deliberately:

- combine the full context with `MockMvc` or the project's supported mock-server client when an in-process servlet boundary is sufficient;
- use a random-port client only when a real embedded server is required, and prefer `RestTestClient` with `@AutoConfigureRestTestClient` over `TestRestTemplate` for a new test;
- do not use a defined port and do not call a separately deployed environment; that is end-to-end scope, which `../SKILL.md` excludes.

## HTTP application integration test

The example assumes stateless bearer authentication, RFC 9457 `ProblemDetail` responses for MVC
errors, an isolated supported-database container, and the project cleanup strategy between methods.
It intentionally omits `@Transactional`: the request must commit through the real service
transaction before repository verification.

Routes and problem identifiers come from the project's route constant and the `ApplicationError`
catalog, never from repeated literals. `UserController.USERS_PATH` is the code-first form; under
contract-first the same tests reference the corresponding `ApiPaths` constant. Error assertions use the `type` URI; the body has no `code`
member.

The example carries `@AutoConfigureMockMvc` explicitly. On Spring Boot 3 that is good practice; on
Spring Boot 4 it is mandatory, because `@SpringBootTest` no longer contributes `MockMvc` on its own.
The injected JSON mapper is `ObjectMapper` here; on Spring Boot 4 that type comes from Jackson 3, so
verify the import resolves to the mapper the application actually configures rather than to a
Jackson 2 type left on the classpath by a transitive dependency.

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {

    private static final String BEARER_SCHEME = "Bearer";

    private final AccessTokenTestClient accessTokenTestClient;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    UserApiIntegrationTest(
            @Autowired final AccessTokenTestClient accessTokenTestClient,
            @Autowired final MockMvc mockMvc,
            @Autowired final ObjectMapper objectMapper,
            @Autowired final PasswordEncoder passwordEncoder,
            @Autowired final UserRepository userRepository) {

        this.accessTokenTestClient = accessTokenTestClient;
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Test
    void usersPost_whenRequestIsValid_createsUser() throws Exception {
        final String accessToken = this.accessTokenTestClient.obtainAccessTokenFor(
                AuthenticationTestData.identityWithUsersWriteScope());
        final UserCreateTO request = UserTestData.validUserCreateTO();

        this.mockMvc.perform(post(UserController.USERS_PATH)
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
        final String accessToken = this.accessTokenTestClient.obtainAccessTokenFor(
                AuthenticationTestData.identityWithUsersWriteScope());
        final UserCreateTO request = UserTestData.userCreateTOWithInvalidEmail();
        final long initialUserCount = this.userRepository.count();

        this.mockMvc.perform(post(UserController.USERS_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "%s %s".formatted(BEARER_SCHEME, accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ApplicationError.VALIDATION_FAILED.type().toString()));

        assertThat(this.userRepository.count()).isEqualTo(initialUserCount);
        assertThat(this.userRepository.existsByEmail(request.email())).isFalse();
    }

    @Test
    void usersPost_whenTokenIsMissing_returnsUnauthorizedAndDoesNotPersist() throws Exception {
        final UserCreateTO request = UserTestData.validUserCreateTO();
        final long initialUserCount = this.userRepository.count();

        this.mockMvc.perform(post(UserController.USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        startsWith(BEARER_SCHEME)));

        assertThat(this.userRepository.count()).isEqualTo(initialUserCount);
        assertThat(this.userRepository.existsByEmail(request.email())).isFalse();
    }

    @Test
    void usersPost_whenWriteScopeIsMissing_returnsForbiddenAndDoesNotPersist() throws Exception {
        final String accessToken = this.accessTokenTestClient.obtainAccessTokenFor(
                AuthenticationTestData.identityWithoutUsersWriteScope());
        final UserCreateTO request = UserTestData.validUserCreateTO();
        final long initialUserCount = this.userRepository.count();

        this.mockMvc.perform(post(UserController.USERS_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "%s %s".formatted(BEARER_SCHEME, accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden());

        assertThat(this.userRepository.count()).isEqualTo(initialUserCount);
        assertThat(this.userRepository.existsByEmail(request.email())).isFalse();
    }
}
```

`AuthenticationTestData.identityWithUsersWriteScope()` describes an isolated synthetic identity
holding the public scope `users:write`; the second fixture describes an authenticated identity
without it. Keep token fixtures expressed in the public scope vocabulary owned by
`application-security`, and do not hardcode credentials in the test method.

`AccessTokenTestClient` hides the issuance profile from the test. Whichever profile the service
uses, the test seeds the identity, asks for a token, and sends it; the assertions do not change.

**It is injected, not constructed.** Its own collaborators differ per issuance profile — the Profile A
version below needs a `TestIdentitySeeder`, the temporary-issuer version needs a token factory — so a
test that builds it by hand has to be edited every time the profile changes, which is precisely what
this indirection exists to prevent. Register it once as a test-scoped bean in a shared
`@TestConfiguration`, and every test takes it as a constructor parameter like any other collaborator.

Match status, `Location`, problem type URI, and schema to the actual API contract. Keep the real security
filter chain enabled. The default bearer response may contain only the required status and challenge;
assert a custom `ProblemDetail` body only when the public contract defines one. For every negative
write case, verify both the public error and the absence of prohibited database state. Extend full
application coverage to applicable affected wiring, transactions, migrations, concurrency
contracts, and committed state. Overlap an important scenario with a unit or MVC slice test when
this test proves a different boundary.

## Authenticated request helper

The helper is the only place that knows how tokens are issued. Tests call one method, so switching
issuance profiles later changes this class and nothing else.

A protected endpoint cannot be used to create the identity that will authenticate against it. Seed
the synthetic identity directly — through the repository, a migration, or a SQL fixture — and then
obtain a token through the real issuance path. Never relax a production route, and never add a
test-only production endpoint, to break that circle.

### Profile A: the service issues its own tokens

This excerpt applies when the application owns `AuthController.TOKEN_PATH`. Adapt the request and
response TOs to the actual contract without bypassing token issuance or validation. The selected
security configuration must expose that endpoint through its explicit public authentication policy
and applicable abuse controls.

```java
final class AccessTokenTestClient {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final TestIdentitySeeder testIdentitySeeder;

    AccessTokenTestClient(
            final MockMvc mockMvc,
            final ObjectMapper objectMapper,
            final TestIdentitySeeder testIdentitySeeder) {

        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.testIdentitySeeder = testIdentitySeeder;
    }

    String obtainAccessTokenFor(final TestIdentity identity) throws Exception {
        final LoginTO login = this.testIdentitySeeder.seed(identity);
        final MvcResult result = this.mockMvc.perform(post(AuthController.TOKEN_PATH)
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

`TestIdentitySeeder` writes the synthetic user and its authorities straight into the database with
an encoded password from the application's own `PasswordEncoder`, and returns the credentials the
token endpoint expects. It lives in test sources only.

### Profile B: an external identity provider issues tokens

Run an approved isolated provider — a container or an in-test authorization server — point the
resource server's issuer configuration at it, and obtain the token through its real protocol
endpoint. Seed the identity through the provider's own administrative interface rather than through
the application. Do not call a live identity provider.

### Before either issuance path exists

Authentication is frequently built after the first endpoints, and integration tests must not wait
for it. Configure a temporary test-only issuer: an in-test signing key registered as the configured
issuer, with a project-owned token factory that mints tokens carrying the same claim set the real
issuer will produce.

```java
final class AccessTokenTestClient {

    private final TestTokenFactory testTokenFactory;

    AccessTokenTestClient(final TestTokenFactory testTokenFactory) {
        this.testTokenFactory = testTokenFactory;
    }

    String obtainAccessTokenFor(final TestIdentity identity) {
        return this.testTokenFactory.signedTokenFor(identity);
    }
}
```

The token still traverses the real filter chain, the real configured `JwtDecoder`, the real issuer,
audience, expiry, and signature validators, and the real authorization rules. Only the key source is
temporary. Keep it in test sources, record it as a known gap, and replace it when the profile is
implemented — the test methods do not change, because they only call `obtainAccessTokenFor`.

This is not permission to use `@WithMockUser`, a security request post-processor, a mocked
`JwtDecoder`, or a forged `Authentication` object. Those skip the chain the test exists to prove and
remain prohibited in every profile.

### Invalid-token fixtures

For validation failures that valid issuance cannot produce — a wrong issuer, a wrong audience, an
expired token, an unapproved algorithm — build the invalid token in isolated test infrastructure
with test-only keys and claims, or configure the isolated provider to issue it. It must reach the
real configured decoder. Never reuse a production key and never apply this exception to valid-token
tests.

With a stateless bearer chain, where clients send the `Authorization` header and no ambient browser
credential exists, keep CSRF disabled consistently and add no CSRF tokens. For cookie, session, or
mixed credential models, test the applicable CSRF behavior instead.

## Deferred effects, outbox, and concurrency scope

- Verify the absence of messages, cache entries, files, or external calls when failure must prevent them, including effects deferred to `AFTER_COMMIT`, which must not fire when the use case rolls back.
- Where the project records an outbox, verify that the outbox row is committed by the same transaction as the business change, and that a rolled-back use case leaves none.
- Cover wiring, transactions, persistence, migrations, concurrency, and committed state only where the feature can exercise them; do not invent concurrency cases for a path with no concurrency contract.

## Container and database rules

Whenever the scenario touches SQL persistence, run schema migrations and use the same relational
database engine and relevant major version as production, through Testcontainers or the project's
equivalent isolated environment. H2-only evidence never proves persistence behavior when production
uses another database. Reuse one project-owned container configuration instead of declaring a
different database per test class, and pin the image to the production engine and approved major or
exact version. Prefer the Spring Boot service-connection mechanism when the supported project
version provides it; otherwise register dynamic properties through the project's existing pattern.

Run the schema through the project's migration tool — Flyway or Liquibase — in the integration
context, never one Hibernate generates or a test-only script creates, because that schema is part of
what these tests verify; `sql-database-migration` owns the clean-install and idempotency checks that
sit beside these suites. Keep the container isolated from production and shared environments, and
never put real credentials in container configuration.

Run the project cleanup strategy recorded in `docs/project-profile.md` outside the HTTP request
transaction, and avoid test-managed `@Transactional` on HTTP write tests, where rollback would hide
commit behavior. Prefer truncating every table after each test method through one project-owned
JUnit extension, shared `@AfterEach`, or base class — reading table names from JDBC metadata or the
migration schema, temporarily relaxing referential integrity, and resetting sequences — because that
is deterministic and order-independent; use a per-class container only when a suite genuinely needs
an isolated database. Do not delete only the rows a test believes it created, and never use one
test's inserts as another test's fixture: reference data required by every test comes from
migrations or a documented seeding step that runs after cleanup. Do not rely on method ordering. If
tests run in parallel, allocate independent data or disable parallelism for that infrastructure
explicitly.

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

```java
// Wrong: the request never reaches the real filter chain, decoder, or authorization rules,
// so this proves nothing about the security configuration it appears to test.
this.mockMvc.perform(post(UserController.USERS_PATH)
        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_users:write"))));
```

## Obtaining a valid token per issuance profile

`../SKILL.md` requires a real credential for successful and authorization-policy scenarios. The
issuance profile recorded in `docs/project-profile.md` determines only how the test obtains one.

- **Profile A, application-issued.** Seed a synthetic identity directly through the repository, a migration, or a SQL fixture, then call the service's real token endpoint. Seeding breaks the bootstrap circle, because the endpoint that creates identities is itself protected. Never relax a protected endpoint or add a test-only production endpoint to avoid seeding.
- **Profile B, externally issued.** Run an approved provider container or isolated in-test authorization server, point the issuer configuration at it, and obtain the token through its real protocol endpoint.
- **Before either exists.** Do not block, skip, or mock. Use a documented temporary test-only issuer: an in-test signing key registered as the configured issuer, minting the claim set the real issuer will produce. Only the key source is temporary; the token still traverses the real decoder, validators, and authorization rules. Record it as a known gap and replace it when the profile is implemented.

## Test selection and suite execution

`*IntegrationTest` requires explicit lifecycle configuration in either build tool, and the two fail
in opposite ways. Maven Surefire's default `**/*Test.java` pattern also matches the suffix, so
without an exclusion those tests run in the `test` phase and then again in Failsafe: the suite
executes twice, the first time in the wrong phase and without the container lifecycle around it.
Gradle has no default integration task at all, so an unregistered suite simply never runs, which
looks identical to a green build. Three requirements, whichever tool the project uses:

- unit and slice tests run in the fast phase, integration tests in a separate later phase or task;
- the verification lifecycle fails when an integration test fails, so a separate phase is not one nobody runs;
- `docs/project-profile.md` records the resulting commands, so "run the relevant suites" is unambiguous.

`build-and-dependencies` carries the worked Maven and Gradle configuration. Verify it before relying
on a green build, and fix it as part of the change when it is missing.
