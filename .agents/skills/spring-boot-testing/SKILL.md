---
name: spring-boot-testing
description: Testing standard for Java 21+ Spring Boot REST applications. Use whenever production behavior is created or changed, and when writing or updating unit tests, Spring test slices, database-backed integration tests, fixtures, mocks, containers, or scheduler tests. Covers scenario selection, test scope, test data, isolation, and execution. Excludes end-to-end and UI testing.
---

# Spring Boot Testing

Prove production behavior with deterministic, maintainable tests. Test reachable scenarios and
publicly observable outcomes, not framework internals or invented edge cases.

## Coordination with other skills

This skill owns test scope, scenario filtering, test-level placement, doubles, fixtures, isolation,
and execution. Apply the specialized skills for the behavior being verified.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what, and
it carries the precedence order for a genuine conflict. Read it there rather than from a copy in
this file. The seams this skill crosses most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| Security scenarios | the level each runs at | `application-security` owns which are required |
| Contract assertions | the level the drift gate runs at | `rest-api-contract` owns what it asserts |
| Persistence scenarios | fixtures, isolation, execution | `spring-data-jpa` owns which JPA behavior needs proof |
| Suite execution | which suites must exist and run | `build-and-dependencies` owns the plugin and source-set configuration |

Resolve a conflict through the owning skill and the repository-enforced build configuration; when it
is genuine and cannot wait, apply the precedence order in [the ownership map](../_core/OWNERSHIP.md)
and say in the handoff which rule was set aside.

## Reference routing

- Read [unit test examples](references/unit-test-examples.md) when testing a service, domain rule,
  mapper behavior, validator, or focused Java component without a Spring context.
- Read [controller slice test examples](references/controller-slice-test-examples.md) whenever a REST
  controller is created or changed.
- Read [integration test examples](references/integration-test-examples.md) when testing an HTTP
  boundary, persistence, Spring configuration, security filter chain, transaction, migration, or
  external adapter with real infrastructure or a controlled substitute.
- Read `rest-api-contract` when the change touches a public endpoint. It owns the contract drift test
  and states what about the contract must be asserted; this skill owns the level it runs at.
- Read [scheduler test examples](references/scheduler-test-examples.md) whenever creating or changing
  a scheduled job, its trigger configuration, overlap protection, or scheduled side effects.

Load only applicable references for the changed behavior.

## Spring Boot 3 and 4 differ more in tests than anywhere else

Both generations are supported. `docs/project-profile.md` records which one the project uses; read
that row before writing a test, because the test infrastructure changed far more than the production
API did. `build-and-dependencies` owns the coordinates in
[generation differences](../build-and-dependencies/references/generation-differences.md); this
section owns what it means for a test.

| Concern | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Replacing a bean with a mock | `@MockBean` / `@SpyBean`, or `@MockitoBean` / `@MockitoSpyBean` from 3.4 onward | `@MockitoBean` / `@MockitoSpyBean` only; the old pair is **removed** |
| Shared mocks for several tests | `@MockBean` fields on a `@TestConfiguration` | not possible on a configuration class; declare `@MockitoBean(types = {...})` on the test class or a custom composed annotation |
| `@Mock` and `@Captor` | worked through Spring Boot's listener | need Mockito's own `MockitoExtension`; the listener is removed |
| `MockMvc` under `@SpringBootTest` | auto-configured | **not** provided; `@AutoConfigureMockMvc` is required |
| `TestRestTemplate` under `@SpringBootTest` | auto-configured | not provided; prefer `RestTestClient` with `@AutoConfigureRestTestClient` |
| `@WithMockUser`, `@WithUserDetails` | `spring-security-test` | need `spring-boot-starter-security-test` |
| Test dependencies | one `spring-boot-starter-test` | a `-test` starter per technology under test, each bringing the core stack transitively |

Two of these fail in ways that mislead. A missing `@AutoConfigureMockMvc` on Spring Boot 4 reads as a
context-wiring defect rather than a missing annotation. A missing `spring-boot-starter-security-test`
makes `@WithMockUser` behave as though the request were unauthenticated, which looks exactly like an
authorization bug — and the tempting "fix" is to relax the very control the test exists to prove.
When a security test fails on Spring Boot 4, verify the dependency before touching the security
configuration.

Everything else in this skill is generation-neutral. Scenario selection, test levels, fixtures,
isolation, determinism, and what a test must assert do not change between 3 and 4.

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

1. Inspect aggregate-service unit, application-service unit, controller MVC slice, and full application integration coverage for the affected behavior.
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

Both service levels `spring-boot-patterns` defines require direct unit tests, and they prove
different things:

- An **aggregate service** test proves the aggregate's invariants: rejected state transitions, derived values recomputed after a change, and the writes that must and must not reach its repositories. Mock its repositories and its domain mapper.
- An **application service** test proves coordination: the order of calls across aggregate services, what is passed between them, and that a failure from one prevents the effects of the other. Mock every aggregate service; do not reach for a repository here, because the unit under test does not have one.

Proving only that a collaborator was invoked is insufficient at either level, and integration
coverage does not replace either. A rule tested at both levels is a signal that it sits at the wrong
one.

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
| An application service that coordinates nothing, or a handler that forwards a single call | Nothing at this level. `spring-boot-patterns` rejects the pass-through itself; test the aggregate service and the endpoint |
| Framework behavior itself | Not tested at all |

A mapper method with hand-written logic — `default` method, custom expression, qualifier, or
decorator — is behavior and gets a unit test, as does any static utility with a real decision.
Everything with a decision in it needs one: do not skip a service, domain rule, validator, policy,
or job because an integration test happens to exercise it. Test a security policy as a unit only
when the policy is the subject; runtime authentication and authorization are proven in integration
tests. Use Mockito's JUnit Jupiter extension when it is the established project library. Do not use
lenient stubbing or broad `any()` matching to hide an inaccurate fixture.

Framework-assigned fixture fields — `@Mock`, `@Spy`, `@Captor`, `@InjectMocks`, `@MockitoBean`,
`@MockitoSpyBean`, and a subject rebuilt in `@BeforeEach` — are `private` and non-`final`;
`modern-java-21` names this an explicit exception to its `final`-field and field-injection rules.
Every other test collaborator, including `MockMvc`, `ObjectMapper`, repositories, and project-owned
test clients, stays `final` and constructor-injected.

## Test every REST controller with an MVC slice

A Spring slice test is neither a pure unit test nor a substitute for full integration coverage.
Every REST controller requires focused `@WebMvcTest` coverage with its collaborators mocked through
the mechanism supported by the project version. Mock the services the controller actually injects:
under the layering `spring-boot-patterns` defines, a controller may hold both an application service
and an aggregate service, and each handler calls one of them. Prove every handler's
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

Use `@SpringBootTest` only when the scenario needs real Spring wiring. For a REST feature, send a
request through the controller and exercise the real service, mapper, repository, transaction,
serialization, error handling, and security filter chain.

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
- verify absence of messages, cache entries, files, or external calls when failure must prevent them, including effects deferred to `AFTER_COMMIT`, which must not fire when the use case rolls back;
- where the project records an outbox, verify that the outbox row is committed by the same transaction as the business change and that a rolled-back use case leaves none;
- run against a schema built by the project's migration tool, never one generated by Hibernate or created by a test-only script; `sql-database-migration` owns the clean-install and idempotency checks that sit beside these suites;
- avoid test-managed `@Transactional` on HTTP write tests when rollback would hide commit behavior;
- use the project's explicit database reset or cleanup strategy so tests remain isolated.

When the project publishes an OpenAPI document, the contract drift test is a required integration
test under every rule in this section, credentials included: the document endpoint sits behind the
same filter chain. `rest-api-contract` owns what it asserts.

Record one project-wide cleanup strategy in `docs/project-profile.md`. Prefer truncating all tables
after each test method through one project-owned JUnit extension or shared `@AfterEach`, reading
table names from JDBC metadata or the migration schema, restoring referential integrity, and
resetting sequences; it is deterministic and order-independent. Use a per-class container only when
a suite genuinely needs an isolated database.

- Do not delete only the rows a test believes it created, and do not use one test's inserts as another's fixture.
- Seed shared reference data through migrations or a documented seeding step that runs after cleanup.
- Cover wiring, transactions, persistence, migrations, concurrency, and committed state only where the feature can exercise them; do not invent concurrency cases for a path with no concurrency contract.
- Overlap with unit or slice tests is fine when the integration test proves a different boundary, but never replaces either level.
- Do not access production or shared staging, and do not mock the business path the test exists to prove.

## Test scheduled jobs at both levels

Every scheduled job must have:

- a unit test that invokes the job directly, without Spring, and proves its delegation, orchestration, and applicable failure behavior;
- a scheduler-specific integration test that loads the required Spring context, enables the real trigger with test-only timing, and proves an observable application effect.

[Scheduler test examples](references/scheduler-test-examples.md) carries the execution rules for
both levels.

## Generate and control test data

Use the project's established test-data solution. Do not add a second generator without a clear
need; when none exists, create a focused factory in test sources rather than scattering object
construction across test classes.

- Factories provide valid defaults and scenario overrides: build the complete object, then vary only the field the case is about.
- Keep generation reproducible: deterministic seeds, fixed `Clock` values, unique generated natural keys for database tests.
- Leave generated identifiers and version fields unset when a persistence fixture represents a new entity.
- Do not hardcode complete object graphs, credentials, personal data, secrets, or repeated arbitrary business values in test methods.
- Explicit values stay when they are the subject of the assertion: boundary numbers, enum states, route constants, error codes, HTTP statuses, malformed inputs.

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

`application-security` owns the security model, authorities, and required scenarios. Prove runtime
authentication, authorization, and filter-chain behavior only in full application integration tests;
a focused policy component may have unit tests for its decisions, but those do not prove enforcement.
Exercise the real filter chain, the service and database scope for object or tenant authorization,
the serializer for data exposure, and the provider adapter for outbound restrictions.

With a stateless bearer chain where clients send the `Authorization` header and no ambient browser
credential exists, keep CSRF disabled consistently and add no CSRF tokens. For cookie, session, or
mixed credential models, test the applicable CSRF behavior instead.

Use synthetic identities and isolated test credentials only. Never use production tokens, customer
data, live identity providers, or production endpoints.

### Obtain a valid token per issuance profile

`application-security` records the issuance profile in `docs/project-profile.md`. It determines only
how the test gets a token; everything after that is identical, because the filter chain is the same
in both profiles. [Integration test examples](references/integration-test-examples.md) carries the
procedure for Profile A, Profile B, and the temporary case where neither exists yet.

Never substitute `@WithMockUser`, a security request post-processor, a mocked `JwtDecoder`, or a
forged `Authentication`; those bypass the chain the test exists to prove. Controlled invalid-token
fixtures are allowed only for validation failures that valid issuance cannot produce, and must
exercise the real configured decoder.

## Configure test selection to match the naming convention

`*IntegrationTest` requires explicit lifecycle configuration in either build tool, and the two fail in
opposite ways. Maven Surefire's default `**/*Test.java` pattern also matches the suffix, so without an
exclusion those tests run in the `test` phase and then again in Failsafe: the suite executes twice,
the first time in the wrong phase and without the container lifecycle around it. Gradle has no
default integration task at all, so an unregistered suite simply never runs, which looks identical to
a green build. Three requirements, whichever tool the project uses:

- unit and slice tests run in the fast phase, integration tests in a separate later phase or task;
- the verification lifecycle fails when an integration test fails, so a separate phase is not one nobody runs;
- `docs/project-profile.md` records the resulting commands, so "run the relevant suites" is unambiguous.

`build-and-dependencies` carries the worked Maven and Gradle configuration. Verify it before relying
on a green build, and fix it as part of the change when it is missing.

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
