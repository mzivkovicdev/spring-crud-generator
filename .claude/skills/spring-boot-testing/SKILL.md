---
name: spring-boot-testing
description: Testing standard for Java 21+ Spring Boot REST applications. Use whenever production behavior is created or changed, and when writing or updating unit tests, Spring test slices, database-backed integration tests, fixtures, mocks, containers, or scheduler tests. Covers scenario selection, test scope, test data, isolation, and execution. Excludes end-to-end and UI testing.
---

# Spring Boot Testing

Prove production behavior with deterministic, maintainable tests. Test reachable scenarios and
publicly observable outcomes, not framework internals or invented edge cases.

## Coordinate the project skills

Treat this skill as the owner of test scope, realistic-scenario filtering, test-level placement,
test doubles, fixtures, isolation, and execution. Apply the specialized skills for the behavior
being verified:

| Skill | Treat as owner of |
| --- | --- |
| `modern-java-21` | Java version, explicit types, import order, source hygiene, and Javadoc |
| `project-naming-conventions` | Test class, method, fixture, and test-data names |
| `spring-boot-patterns` | REST, service, domain, mapper, validation, error, and configuration contracts |
| `spring-data-jpa` | Database mappings, constraints, queries, transactions, locking, migrations, and production-database semantics |
| `application-security` | Required security scenarios, protected data, credentials, and trust-boundary controls |
| `spring-boot-code-review` | Review scope, evidence, severity, and reporting |
| `build-and-dependencies` | Build files, test plugins, source sets, and the configuration that decides which suites run in which phase |
| `observability-and-logging` | What about logging, metrics, tracing, and probes is worth asserting, and what is not |
| `rest-api-contract` | What about the OpenAPI contract must be asserted, including the document drift gate |

Do not redefine those standards here. Resolve a conflict through the owning skill and the
repository-enforced build configuration.

## Route the references

- Read [unit test examples](references/unit-test-examples.md) when testing a service, domain rule,
  mapper behavior, validator, or focused Java component without a Spring context.
- Read [controller slice test examples](references/controller-slice-test-examples.md) whenever a REST
  controller is created or changed.
- Read [integration test examples](references/integration-test-examples.md) when testing an HTTP
  boundary, persistence, Spring configuration, security filter chain, transaction, migration, or
  external adapter with real infrastructure or a controlled substitute.
- Read [scheduler test examples](references/scheduler-test-examples.md) whenever creating or changing
  a scheduled job, its trigger configuration, overlap protection, or scheduled side effects.

Load only applicable references for the changed behavior.

## Inspect before writing tests

1. Read `docs/project-profile.md` for the database engine and version, the authentication profile,
   the migration tool, the cleanup strategy, and the test-selection configuration. When a decision
   the tests depend on is missing, fill it through the process `spring-boot-patterns` owns before
   writing tests. Do not substitute H2 for an undecided database, and do not invent an
   authentication mechanism.
2. Inspect the Maven or Gradle configuration, supported Java and Spring Boot versions, test source
   sets, test plugins, naming suffixes, profiles, and CI commands.
3. Reuse the project's supported JUnit Jupiter version and assertion, mocking, data-generation,
   container, HTTP, and stub-server libraries. Do not override the Spring Boot dependency
   management merely to obtain a newer test API.
4. Inspect nearby sound tests, shared fixtures, container configuration, database cleanup, fixed
   clocks, custom annotations, and test factories before creating alternatives.
5. Trace the changed production path and identify its observable contract, transaction effects,
   security controls, and external side effects.
6. Derive test cases from requirements and reachable branches. Do not create cases that cannot
   occur through the tested public boundary or supported system state.

## Keep tests synchronized with every change

For every production-code change:

1. Inspect service unit, controller MVC slice, and full application integration coverage for the affected behavior.
2. Update every assertion and fixture affected by the contract change.
3. Add a test for every new reachable success, failure, boundary, or regression case.
4. Create any missing required service unit, controller MVC slice, and full application integration coverage.
5. Run the focused tests, then every relevant unit, MVC slice, and integration suite.

For a formatting, import-only, comment-only, or equivalent non-behavioral change, do not invent a
meaningless assertion or mechanically rewrite tests. Confirm that behavior is unchanged and run the
affected suites. Never weaken, disable, delete, or ignore a failing test to complete a change.

## Select realistic scenarios

Start from the happy path. Place successful test methods before negative and exception cases in the
source file so the intended behavior is discovered first. This is source organization only: every
test must remain independent, and runtime ordering must not be required.

After the happy path, cover only applicable cases such as:

- invalid or boundary input accepted by the public method signature;
- missing resources, duplicate data, invalid state, or authorization failure;
- database constraints, locking, rollback, or transaction behavior;
- dependency timeout or failure when the application defines handling for it;
- scheduled execution, disabled scheduling, overlap, or restart behavior when the job contract makes
  that scenario reachable;
- a confirmed production defect through a failing-before, passing-after regression test.

Do not test impossible combinations, arbitrary random failures, private implementation branches,
or defensive behavior excluded by the public contract. Use parameterized tests when several inputs
exercise the same rule and need the same assertion.

## Write focused unit tests

Use the project's supported JUnit Jupiter version, JUnit 5 or newer. A unit test must:

- instantiate the subject directly without loading a Spring `ApplicationContext`;
- omit `@SpringBootTest`, Spring test slices, `SpringExtension`, and Spring-managed mocks unless the
  Spring mechanism itself is the subject of the test;
- mock or fake only dependencies outside the unit, not the subject or simple value objects;
- verify domain decisions, returned state, declared exceptions, and meaningful collaborator effects;
- assert the absence of a write, event, or external call when that absence is part of the behavior;
- avoid testing getters, setters, records, framework behavior, generated mapper code without custom
  logic, or private methods directly.

Every application service containing behavior requires direct unit tests. Cover its business
decisions, returned state, declared exceptions, repository writes, and prohibited interactions when
applicable; a test that proves only that a collaborator was invoked is insufficient. Full application
integration coverage does not replace this unit coverage.

### What is deliberately not unit tested

The rule is coverage of behavior, not coverage of files. These types have no direct unit test, and
their absence is correct rather than a gap:

| Type | Where it is proven instead |
| --- | --- |
| REST controllers | `@WebMvcTest` slice plus full application integration |
| MapStruct mappers with no hand-written logic | Through the service unit tests and integration tests that use them; a generated mapping is verified by the compiler and `ReportingPolicy.ERROR` |
| Request and response TOs, domain records, entities | Through the boundaries that serialize, validate, and persist them |
| Getters, setters, `equals`, `hashCode`, `toString` | Entity equality is proven where it matters, in a persistence test that puts instances in a collection across states |
| Spring configuration classes, `@ConfigurationProperties`, `SecurityConfig` | Full application integration, including startup failure on invalid configuration |
| Framework behavior itself | Not tested at all |

A mapper method that contains hand-written logic — a `default` method, a custom expression, a
qualifier, or a decorator — is behavior and does get a direct unit test. So does any static utility
with a real decision in it.

Everything else that contains a decision needs a unit test. Do not skip a service, domain rule,
validator, policy, or job because an integration test happens to exercise it.

Plain unit tests have no Spring context or security filter chain. Test a security policy as an
ordinary unit only when that policy is the subject; prove runtime authentication and authorization in
full application integration tests.

Use Mockito's JUnit Jupiter extension when Mockito is the established project library. Construct the
subject explicitly when that makes dependencies and test setup clearer. Do not use lenient stubbing
or broad `any()` matching to hide an inaccurate fixture.

Framework-assigned fixture fields — `@Mock`, `@Spy`, `@Captor`, `@InjectMocks`, `@MockitoBean`,
`@MockitoSpyBean`, and a subject rebuilt in `@BeforeEach` — are declared `private` and non-`final`.
`modern-java-21` names this an explicit exception to its `final`-field and field-injection rules,
because the framework assigns them after construction and the compiler would otherwise reject them.
Every other test collaborator, including `MockMvc`, `ObjectMapper`, repositories, and project-owned
test clients, stays `final` and constructor-injected.

## Test every REST controller with an MVC slice

A Spring slice test is neither a pure unit test nor a substitute for full integration coverage.
Every REST controller requires focused `@WebMvcTest` coverage with its services and other downstream
collaborators mocked through the mechanism supported by the project version. Prove every handler's
routing and delegation plus applicable validation, request and response serialization, status,
headers, and public error contract.

Focused MVC slice tests do not exercise or verify the Spring Security filter chain. Use
`@AutoConfigureMockMvc(addFilters = false)` for this project and do not use mock users, mock tokens,
authorities, or CSRF request post-processors in the slice. This setting excludes all servlet filters
from `MockMvc`, so the slice proves the controller and MVC contract only. Verify security and any
other filter-owned contract in focused filter tests when useful and in full application integration
tests. Keep validation, error-handler, serialization, and delegation coverage in the MVC slice.

Do not use an MVC slice as evidence for transaction, database, or other full-application behavior
excluded from that slice. Conversely, do not omit required MVC slice coverage because a full HTTP
integration test exercises the same route. The overlap is intentional: each level proves a different
boundary.

## Write focused persistence slice tests

Use `@DataJpaTest` only when a custom query, mapping, converter, projection, constraint, ordering,
pagination, locking, flush behavior, entity equality across persistence states, or database-specific
persistence rule needs direct proof. Use the actual supported database and migration configuration;
replacement would change the semantics. Do not create a persistence slice for inherited
`JpaRepository` CRUD behavior merely because a repository exists — a full application integration
test already provides sufficient evidence for a simple path.

## Write application integration tests

Use `@SpringBootTest` only when the scenario needs the application context and real Spring wiring.
For a REST feature, prefer an application integration test that sends a request through the
controller and exercises the real service, mapper, repository, transaction, serialization, error
handling, and applicable security filter chain.

Choose the web mode deliberately:

- combine the full context with `MockMvc` or the project's supported mock-server client when an
  in-process servlet boundary is sufficient;
- use a random-port client only when a real embedded server is required;
- do not use a defined port or call a separately deployed environment; that is end-to-end scope.

Integration tests must:

- when the scenario touches SQL persistence, run schema migrations and use the same relational
  database engine and relevant major version as production through Testcontainers or the project's
  equivalent isolated environment;
- avoid H2-only evidence for persistence behavior when production uses another database;
- keep the real entry point, service, relevant adapters, transaction configuration, serialization,
  and security controls involved in the tested path;
- for successful and authorization-policy scenarios, obtain a valid credential through the issuance
  profile recorded for the service, as described under "Obtain a valid token per issuance profile";
- for bearer-protected APIs, send the valid access token in the `Authorization: Bearer` header;
- for token-validation failures that approved issuance cannot produce, use a controlled invalid
  token or isolated provider configuration that traverses the real filter chain and configured
  decoder; do not replace that path with a mock token, security request post-processor, mocked
  decoder, or forged authentication;
- replace only true external systems with controlled stubs, fakes, emulators, or containers;
- verify the response or other public result and the committed database state after success;
- verify the public error contract and prove that invalid or rejected data was not persisted after
  every negative write scenario;
- verify absence of messages, cache entries, files, or external calls when failure must prevent them;
- avoid test-managed `@Transactional` on HTTP write tests when rollback would hide commit behavior;
- use the project's explicit database reset or cleanup strategy so tests remain isolated.

Choose one cleanup strategy for the whole project and record it in `docs/project-profile.md`. In
preference order:

1. **Truncate all tables after each test method**, through one project-owned JUnit extension or
   `@AfterEach` in a shared base class. It reads table names from the JDBC metadata or the migration
   schema, disables and restores referential integrity for the operation, and resets sequences.
   Preferred because it is deterministic, independent of test order, and does not depend on any
   test knowing which rows a request created.
2. **A per-class container** when a suite genuinely needs an isolated database, accepting the
   startup cost.

Do not use `@Transactional` rollback on HTTP write tests, do not delete only the rows a test
believes it created, and do not rely on one test's inserts as another's fixture. Seed reference data
that every test needs through migrations or a documented seeding step that runs after cleanup, not
from an arbitrary earlier test.

Full application integration coverage must prove applicable affected real wiring, transactions,
persistence, migrations, concurrency behavior, and committed database state. Cover each item only
when the feature can exercise it; do not invent concurrency cases for a path with no concurrency
contract. Important scenarios may overlap with service unit or MVC slice tests when the integration
test proves a different boundary. Integration coverage never replaces either required lower level.

Do not access live production or shared staging services. Do not mock the business path in a test
whose purpose is to prove that the complete application path works.

## Test scheduled jobs at both levels

Every scheduled job must have:

- a unit test that invokes the job directly, without Spring, and proves its delegation,
  orchestration, and applicable failure behavior;
- a scheduler-specific integration test that loads the required Spring context, enables the real
  trigger with test-only timing, and proves an observable application effect.

Keep scheduling disabled in unrelated tests through the project's explicit test configuration when
background execution could interfere with their state. In scheduler integration tests, use the real
scheduler and a generous bounded wait; do not call the scheduled method manually, sleep for an exact
interval, or assert an exact invocation count for a repeating trigger. Test disabled scheduling,
overlap protection, distributed locking, time zones, and retry behavior only when they are part of
the actual job contract.

## Generate and control test data

Use the project's established Instancio, Podam, factory, builder, fixture, or equivalent test-data
solution. Do not introduce a second generator or a new dependency without a clear project need.
When no solution exists, create a focused factory in test sources rather than scattering object
construction across test classes.

Test-data factories must provide valid defaults and scenario-specific overrides. Generate complete
objects through the factory, then vary only the field relevant to the case. Use deterministic seeds
when generation is random, fixed `Clock` values for time, and unique generated natural keys for
database tests. Keep generated failure output reproducible. Leave generated identifiers and version
fields unset when a persistence fixture represents a new entity.

Do not hardcode complete object graphs, credentials, personal data, secrets, or repeated arbitrary
business values in test methods. Explicit contract values remain appropriate when they are the
subject of the assertion, including boundary numbers, enum states, route constants, error codes,
HTTP statuses, and malformed inputs. Name fixtures by scenario under
`project-naming-conventions`.

## Assert observable behavior

- Keep each test focused on one behavior and include all assertions needed to prove that behavior.
- Assert values, state transitions, stable error types/codes, persistence, and externally visible
  side effects rather than implementation details.
- Assert an exception's stable contract; do not couple tests to an incidental raw message.
- Verify collaborator calls only when the interaction is part of the contract, such as no repository
  write after validation failure or one event after a committed transition.
- Prefer exact assertions over broad non-null, non-empty, or invocation-only checks.
- Do not duplicate the production algorithm inside the expected-value calculation.

## Preserve determinism and isolation

- Make tests independent of execution order and shared mutable state.
- Do not use `@TestMethodOrder` to make one test prepare another.
- Use fixed time, controlled randomness, and explicit locale and time zone when relevant.
- Do not use `Thread.sleep`; use bounded polling such as the project's Awaitility setup for genuine
  asynchronous behavior.
- Keep parallel execution disabled for infrastructure that is not proven parallel-safe; otherwise,
  isolate database rows, ports, destinations, and mutable resources per test.
- Avoid `@DirtiesContext` as routine cleanup because it defeats context caching. Correct the leaking
  state or use a focused reset mechanism.

## Prove security controls

Apply `application-security` as the owner of the security model, authorities, and required security
scenarios. Prove runtime authentication, authorization, and security filter-chain behavior only in
full application integration tests. A focused authorization or policy component may also have plain
unit tests for its decisions, but those tests do not prove runtime enforcement. Exercise the real
filter chain, service and database scope for object or tenant authorization, serializer for data
exposure, and provider adapter for outbound restrictions.

Use the authentication and credential model selected for the deployable service by
`application-security`. For a stateless bearer filter chain in which clients explicitly send the
`Authorization` header and no ambient browser credential authenticates requests, keep CSRF disabled
consistently and do not add CSRF tokens to integration requests. For cookie, session, or mixed
credential models, test the applicable CSRF behavior instead.

Use synthetic identities and isolated test credentials only. Never use production tokens, customer
data, live identity providers, or production endpoints.

### Obtain a valid token per issuance profile

`application-security` records the service's issuance profile in `docs/project-profile.md`. It
determines only how the test gets a token; everything after that is identical, because the filter
chain is the same in both.

**Profile A, application-issued tokens.** Seed a synthetic identity directly through the repository,
a migration, or a SQL fixture, then call the service's real token endpoint and use the returned
access token. Seeding is what breaks the bootstrap circle: the identity must exist before a token
can be issued, and the endpoint that creates identities is itself protected. Never relax a protected
endpoint, and never add a test-only production endpoint, to avoid seeding.

**Profile B, externally issued tokens.** Run an approved identity-provider container or an isolated
in-test authorization server, point the resource server's issuer configuration at it, and obtain the
token through its real protocol endpoint.

**Before either issuance path exists.** Authentication is often decided or built after the first
endpoints. Until then, do not block or skip integration tests, and do not reach for a mock token.
Write them against the same real filter chain with a documented, temporary test-only issuer: an
in-test signing key registered as the configured issuer, used exclusively by a project-owned test
token factory that mints tokens with the same claim set the real issuer will produce. The token
still traverses the real decoder, the real validators, and the real authorization rules, so only the
key source is temporary. Record it as a known gap, keep it in test sources only, and replace it with
the real issuance path as soon as the profile is implemented. This is not permission to use
`@WithMockUser`, a security request post-processor, a mocked `JwtDecoder`, or a forged
`Authentication`; those bypass the chain the test exists to prove.

Controlled invalid-token fixtures are allowed only for token-validation failures that valid issuance
cannot produce, and must exercise the real configured decoder.

## Configure test selection to match the naming convention

`project-naming-conventions` names full application and persistence tests `*IntegrationTest`. That
suffix matches no default in either build tool, so the build must be configured explicitly or the
tests will run in the wrong phase — or not at all. Verify the configuration before relying on a
green build, and fix it as part of the change when it is missing.

`build-and-dependencies` owns the build files themselves and contains the worked configuration for
both tools. The requirement here is only that the phases are separated and enforced.

For Maven, unit tests run in Surefire and integration tests in Failsafe:

- Surefire includes `**/*Test.java` and **excludes** `**/*IntegrationTest.java`, otherwise every container-backed test runs in the `test` phase.
- Failsafe includes `**/*IntegrationTest.java` and is bound to `integration-test` and `verify`.
- Because `*IntegrationTest` also matches Surefire's default `*Test` pattern, the exclusion is required, not optional.

For Gradle, declare a separate `integrationTest` source set or a `Test` task filtered on the same
pattern, make `check` depend on it, and keep unit tests out of it.

Whichever tool is used, `docs/project-profile.md` records the resulting commands so that "run the
relevant suites" is unambiguous.

## Execute and report verification

Run the narrowest changed test first for quick feedback. Then run all relevant unit, MVC slice,
persistence slice, and full integration tests using the repository's Maven or Gradle lifecycle,
including the integration-test source set or plugin phase. Run the formatter, compiler, and static
analysis required by the project.

Fix the production code or the test when a failure reveals a defect. Treat flaky tests as defects;
identify and remove their nondeterminism instead of rerunning until green. Do not claim that tests
pass unless the commands completed successfully. If infrastructure prevents execution, report the
exact unrun suites and blocker; the change remains incomplete.

## Excluded scope

Do not create browser, UI, multi-service deployed-environment, or other end-to-end tests under this
skill. A request sent to a single service's in-process or random-port application context with
controlled dependencies is an integration test, not an end-to-end test.

## Completion checklist

- [ ] Tests cover the happy path first, then every applicable reachable negative case.
- [ ] Every affected behavioral application service has direct focused unit coverage without Spring.
- [ ] Every affected REST controller has security-disabled `@WebMvcTest` coverage for its complete public MVC contract.
- [ ] Full application integration tests prove applicable affected real wiring, transactions, persistence, migrations, concurrency, and committed state.
- [ ] Successful protected integration requests obtain and send a valid credential through the service's approved isolated authentication flow.
- [ ] Overlap across levels proves different boundaries; no level was omitted because another exists.
- [ ] Every changed scheduler has direct unit coverage and a real-trigger integration test.
- [ ] Negative write scenarios prove that prohibited data was not persisted.
- [ ] Test data uses the established generator or focused factory and is deterministic.
- [ ] Tests are independent, secure, and free from arbitrary sleeps and live dependencies.
- [ ] Every touched Java test follows `modern-java-21`, including the project import order.
- [ ] Every relevant unit, MVC slice, persistence slice, and full integration suite passes.
- [ ] Test selection is configured so integration tests actually run, in the correct phase.
- [ ] Database cleanup follows the project strategy and no test depends on another test's data.

## Primary guidance

- [Spring Boot: Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Boot: Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Spring Framework: Testing](https://docs.spring.io/spring-framework/reference/testing.html)
- [Spring Framework: Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [JUnit User Guide](https://docs.junit.org/current/user-guide/)
- [Testcontainers for Java](https://java.testcontainers.org/)
