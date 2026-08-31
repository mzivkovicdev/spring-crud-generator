---
name: spring-boot-testing
description: Testing standard for Java 21+ Spring Boot REST applications. Use whenever production behavior is created or changed, and when writing or updating unit tests, Spring test slices, database-backed integration tests, fixtures, mocks, containers, or scheduler tests. Covers scenario selection, test scope, test data, isolation, and execution. Excludes end-to-end and UI testing.
---

# Spring Boot Testing

Prove production behavior with deterministic, maintainable tests. Test reachable scenarios and
publicly observable outcomes, not framework internals or invented edge cases.

## Coordination with other skills

This skill owns test scope, scenario filtering, test-level placement, doubles, fixtures, isolation,
and execution; apply the specialized skills for the behavior being verified.
[The ownership map](../_core/OWNERSHIP.md) is canonical for who owns what and carries the precedence
order for a genuine conflict — read it there, not from a copy here. The seams crossed most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| Security scenarios | the level each runs at | `application-security` owns which are required |
| Contract assertions | the level the drift gate runs at | `rest-api-contract` owns what it asserts |
| Persistence scenarios | fixtures, isolation, execution | `spring-data-jpa` owns which JPA behavior needs proof |
| Suite execution | which suites must exist and run | `build-and-dependencies` owns the plugin and source-set configuration |

Resolve a conflict through the owning skill and the repository-enforced build configuration; when it
is genuine and cannot wait, apply that precedence order and name the rule set aside in the handoff.

## Reference routing

Load only the references applicable to the changed behavior.

- [Unit test examples](references/unit-test-examples.md): a service, domain rule, mapper behavior, validator, or focused component without a Spring context; also the test-data rules and the types that deliberately have no unit test.
- [Controller slice test examples](references/controller-slice-test-examples.md): any REST controller created or changed.
- [Integration test examples](references/integration-test-examples.md): an HTTP boundary, persistence, Spring configuration, security filter chain, transaction, migration, or external adapter with real infrastructure or a controlled substitute; also container, cleanup, and suite-execution rules.
- [Scheduler test examples](references/scheduler-test-examples.md): a scheduled job, its trigger configuration, overlap protection, or scheduled side effects.
- `rest-api-contract`: any change touching a public endpoint. It owns the contract drift test and states what about the contract must be asserted; this skill owns the level it runs at.

## Spring Boot 3 and 4 differ more in tests than anywhere else

Both generations are supported; `docs/project-profile.md` records which one applies. Read that row
before writing a test, because the test infrastructure changed far more than the production API did.
Mock-bean annotations, `@Mock` and `@Captor` support, `MockMvc` and `TestRestTemplate`
auto-configuration, security-test support, and the test starters themselves all differ;
`build-and-dependencies` owns those coordinates in
[generation differences](../build-and-dependencies/references/generation-differences.md), sections
"Testing", "Test starters", "Annotations and types that moved", and "Removed in Spring Boot 4". This
section owns what they mean for a test.

Two of them fail in ways that mislead. A missing `@AutoConfigureMockMvc` on Spring Boot 4 reads as a
context-wiring defect rather than a missing annotation. A missing `spring-boot-starter-security-test`
makes `@WithMockUser` behave as though the request were unauthenticated, which looks exactly like an
authorization bug — and the tempting "fix" is to relax the very control the test exists to prove.
When a security test fails on Spring Boot 4, verify the dependency before touching the security
configuration.

Everything else here is generation-neutral: scenario selection, test levels, fixtures, isolation,
determinism, and what a test must assert are the same on 3 and 4.

## Inspect before writing tests

1. Read `docs/project-profile.md` for the database engine and version, authentication profile, migration tool, cleanup strategy, and test-selection configuration. Fill a missing decision the tests depend on through the process `project-decision-profile` owns before writing tests; never substitute H2 for an undecided database, and never invent an authentication mechanism.
2. Inspect the Maven or Gradle configuration: supported Java and Spring Boot versions, test source sets, plugins, naming suffixes, profiles, CI commands.
3. Reuse the project's supported JUnit Jupiter version and its assertion, mocking, data-generation, container, HTTP, and stub-server libraries; do not override Spring Boot dependency management to obtain a newer test API.
4. Inspect nearby sound tests, shared fixtures, container configuration, database cleanup, fixed clocks, custom annotations, and factories before creating alternatives.
5. Trace the changed production path: observable contract, transaction effects, security controls, external side effects.
6. Derive cases from requirements and reachable branches; do not create cases that cannot occur through the tested public boundary or supported system state.

## Keep tests synchronized with every change

For every production-code change:

1. Inspect aggregate-service unit, application-service unit, controller MVC slice, and full application integration coverage of the affected behavior, and create every required level that is missing.
2. Update every assertion and fixture affected by the contract change.
3. Add a test for every new reachable success, failure, boundary, or regression case.

For a formatting, import-only, comment-only, or equivalent non-behavioral change, do not invent a
meaningless assertion or mechanically rewrite tests: confirm behavior is unchanged and run the
affected suites. Never weaken, disable, delete, or ignore a failing test to complete a change.

## Select realistic scenarios

Start from the happy path, and place successful test methods before negative and exception cases in
the source file so the intended behavior is discovered first. That is source organization only:
runtime ordering must never be required.

After the happy path, cover only applicable cases such as:

- invalid or boundary input accepted by the public method signature;
- missing resources, duplicate data, invalid state, or authorization failure;
- database constraints, locking, rollback, or transaction behavior;
- dependency timeout or failure when the application defines handling for it;
- scheduled execution, disabled scheduling, overlap, or restart behavior when the job contract makes that scenario reachable;
- a confirmed production defect, through a failing-before, passing-after regression test.

Do not test impossible combinations, arbitrary random failures, private implementation branches, or
defensive behavior excluded by the public contract. Use parameterized tests when several inputs
exercise the same rule and need the same assertion.

## Write focused unit tests

Use the project's supported JUnit Jupiter version, JUnit 5 or newer. A unit test must:

- instantiate the subject directly, with no Spring `ApplicationContext` and no `@SpringBootTest`, test slice, `SpringExtension`, or Spring-managed mock, unless the Spring mechanism is itself the subject;
- mock or fake only dependencies outside the unit, never the subject or simple value objects;
- verify domain decisions, returned state, declared exceptions, and meaningful collaborator effects;
- assert the absence of a write, event, or external call when that absence is part of the behavior;
- not test getters, setters, records, framework behavior, generated mapper code without custom logic, or private methods directly.

Both service levels `spring-boot-patterns` defines need direct unit tests, and they prove different
things:

- an **aggregate service** test proves the aggregate's invariants — rejected state transitions, derived values recomputed after a change, and the writes that must and must not reach its repositories; mock its repositories;
- an **application service** test proves coordination — call order across aggregate services, what is passed between them, and that a failure from one prevents the effects of the other; mock every aggregate service, and do not reach for a repository, because the unit under test has none.

Proving only that a collaborator was invoked is insufficient at either level, and integration
coverage replaces neither. A rule tested at both levels sits at the wrong one.

**Mock what the subject injects, and nothing else.** A structural mapper is not a dependency under
the architecture `spring-boot-patterns` defines: it is reached through its generated static
`INSTANCE`, and `modern-java-21` names that an explicit exception to its service-locator rule. It
therefore cannot be mocked, and it must not be turned into a Spring bean so that it can be. It runs
for real in the unit test, which is what makes the test assert the mapped result the caller actually
receives instead of a stub's stand-in.

Coverage is of behavior, not of files: everything with a decision in it needs a unit test, so do not
skip a service, domain rule, validator, policy, or job because an integration test happens to
exercise it. The types that deliberately have none, where each is proven instead, and the mapper and
utility exceptions are in
[types that are deliberately not unit tested](references/unit-test-examples.md#types-that-are-deliberately-not-unit-tested).

Use Mockito's JUnit Jupiter extension when it is the established project library; never use lenient
stubbing or broad `any()` matching to hide an inaccurate fixture. Framework-assigned fixture fields —
`@Mock`, `@Spy`, `@Captor`, `@InjectMocks`, `@MockitoBean`, `@MockitoSpyBean`, and a subject rebuilt
in `@BeforeEach` — are `private` and non-`final`, the explicit exception `modern-java-21` names to
its `final`-field and field-injection rules; every other test collaborator, `MockMvc`,
`ObjectMapper`, repositories, and project-owned test clients included, stays `final` and
constructor-injected.

## Test every REST controller with an MVC slice

A slice test is neither a pure unit test nor a substitute for full integration coverage. Every REST
controller requires focused `@WebMvcTest` coverage with its collaborators mocked through the
mechanism the project version supports. Mock the services the controller actually injects: under the
layering `spring-boot-patterns` defines, a controller may hold both an application service and an
aggregate service, and each handler calls one of them. Prove every handler's routing and delegation
plus applicable validation, request and response serialization, status, headers, and public error
contract.

Run the slice with `@AutoConfigureMockMvc(addFilters = false)`, and do not omit required MVC slice
coverage because a full HTTP integration test exercises the same route: the overlap is intentional,
and each level proves a different boundary.
[Controller slice test examples](references/controller-slice-test-examples.md) carries the rest
whole: what that setting excludes and what therefore never belongs in a slice, which contracts move
to focused filter tests and full integration tests, and what a slice may not be used as evidence
for.

## Write focused persistence slice tests

Use `@DataJpaTest` only when a custom query, mapping, converter, projection, constraint, ordering,
pagination, locking, flush behavior, entity equality across persistence states, or database-specific
persistence rule needs direct proof, and run it against the actual supported database and migration
configuration, because replacement would change the semantics. Do not create a persistence slice for
inherited `JpaRepository` CRUD behavior merely because a repository exists — a full application
integration test is already sufficient evidence for a simple path.

## Write application integration tests

Use `@SpringBootTest` only when the scenario needs real Spring wiring. For a REST feature, send a
request through the controller and exercise the real service, mapper, repository, transaction,
serialization, error handling, and security filter chain. Choose the web mode deliberately:
[integration test examples](references/integration-test-examples.md#choosing-the-web-mode) carries
that decision, the client to prefer for a new test, and what falls outside this skill's scope.

Integration tests must:

- keep the real entry point, service, relevant adapters, transaction configuration, serialization, and security controls of the tested path, replacing only true external systems with controlled stubs, fakes, emulators, or containers;
- obtain a valid credential through the issuance profile recorded for the service for successful and authorization-policy scenarios, and send it in the `Authorization: Bearer` header on bearer-protected APIs, under "Prove security controls";
- verify the response or other public result and the committed database state after success;
- verify the public error contract after every negative write scenario, and prove that invalid or rejected data was not persisted;
- verify the deferred effects, outbox behavior, and concurrency scope stated in [integration test examples](references/integration-test-examples.md#deferred-effects-outbox-and-concurrency-scope);
- never access production or shared staging, and never mock the business path the test exists to prove.

When the project publishes an OpenAPI document, the contract drift test is a required integration
test under every rule in this section, credentials included: the document endpoint sits behind the
same filter chain. `rest-api-contract` owns what it asserts.

Record one project-wide cleanup strategy in `docs/project-profile.md` and follow it so tests stay
isolated. [Integration test examples](references/integration-test-examples.md#container-and-database-rules)
carries the container and database rules whole: the production engine and version through
Testcontainers, the schema built by the project's migration tool, the default truncation procedure
and why it is deterministic and order-independent, what may never serve as another test's fixture,
how shared reference data is seeded, and the only case for a per-class container.

## Test scheduled jobs at both levels

Every scheduled job must have:

- a unit test that invokes the job directly, without Spring, and proves its delegation, orchestration, and applicable failure behavior;
- a scheduler-specific integration test that loads the required Spring context, enables the real trigger with test-only timing, and proves an observable application effect.

[Scheduler test examples](references/scheduler-test-examples.md) carries the execution rules for both
levels.

## Generate and control test data

Use the project's established test-data solution; do not add a second generator without a clear need,
and when none exists create a focused factory in test sources rather than scattering object
construction across test classes. [Test data rules](references/unit-test-examples.md#test-data-rules)
carries the rest whole: factory defaults and overrides, reproducibility, unset identifiers, what must
never be hardcoded in a test method, and which explicit values belong in the test.

## Assert observable behavior

- Keep each test focused on one behavior, with every assertion needed to prove it.
- Assert values, state transitions, stable error types and codes, persistence, and externally visible side effects rather than implementation details.
- Assert an exception's stable contract; do not couple tests to an incidental raw message.
- Verify collaborator calls only when the interaction is part of the contract, such as no repository write after validation failure or one event after a committed transition.
- Prefer exact assertions over broad non-null, non-empty, or invocation-only checks.
- Do not duplicate the production algorithm inside the expected-value calculation.

## Preserve determinism and isolation

- Make tests independent of execution order and shared mutable state, and never use `@TestMethodOrder` to make one test prepare another.
- Use fixed time, controlled randomness, and explicit locale and time zone when relevant.
- Do not use `Thread.sleep`; use bounded polling such as the project's Awaitility setup for genuine asynchronous behavior.
- Keep parallel execution disabled for infrastructure not proven parallel-safe; otherwise isolate database rows, ports, destinations, and mutable resources per test.
- Avoid `@DirtiesContext` as routine cleanup because it defeats context caching; correct the leaking state or use a focused reset mechanism.

## Prove security controls

`application-security` owns the security model, authorities, and required scenarios. Prove runtime
authentication, authorization, and filter-chain behavior only in full application integration tests;
a focused policy component may have unit tests for its decisions, but those do not prove enforcement.
Exercise the real filter chain, the service and database scope for object or tenant authorization,
the serializer for data exposure, and the provider adapter for outbound restrictions.

The issuance profile `application-security` records in `docs/project-profile.md` determines only how
a test obtains a token; everything after that is identical, because the filter chain is the same in
both profiles. [Integration test examples](references/integration-test-examples.md#obtaining-a-valid-token-per-issuance-profile)
carries the procedure for Profile A, Profile B, and the temporary case where neither exists yet.

Never substitute `@WithMockUser`, a security request post-processor, a mocked `JwtDecoder`, or a
forged `Authentication`; those bypass the chain the test exists to prove. Controlled invalid-token
fixtures are allowed only for validation failures that valid issuance cannot produce, and must
exercise the real configured decoder.

Which CSRF behavior a test must exercise follows from the credential model, and
[integration test examples](references/integration-test-examples.md#invalid-token-fixtures) carries
that rule for the stateless bearer chain and for cookie, session, or mixed models. Use synthetic
identities and isolated test credentials only: never production tokens, customer data, live identity
providers, or production endpoints.

## Execute and report verification

Run the narrowest changed test first for quick feedback, then every relevant unit, MVC slice,
persistence slice, and full integration test through the repository's Maven or Gradle lifecycle,
including the integration-test source set or plugin phase, plus the formatter, compiler, and static
analysis the project requires. A suffix such as `*IntegrationTest` runs only when the build is
configured for it, and a suite nobody runs looks exactly like a green build:
[integration test examples](references/integration-test-examples.md#test-selection-and-suite-execution)
carries the selection rules, both build tools' failure modes, and the commands the profile must
record.

Fix the production code or the test when a failure reveals a defect. Treat flaky tests as defects:
identify and remove their nondeterminism instead of rerunning until green. Do not claim that tests
pass unless the commands completed successfully. If infrastructure prevents execution, report the
exact unrun suites and blocker; the change remains incomplete.

## Excluded scope

Do not create browser, UI, multi-service deployed-environment, or other end-to-end tests under this
skill. A request sent to a single service's in-process or random-port application context with
controlled dependencies is an integration test, not an end-to-end test.

## Completion checklist

- [ ] Happy path first, then every applicable reachable negative case.
- [ ] Every affected aggregate service: direct Spring-free unit coverage of its own invariants and of the writes that must and must not reach its repositories.
- [ ] Every affected application service: direct Spring-free unit coverage of call order across aggregate services and of the effects a failure must prevent.
- [ ] Every affected controller: security-disabled `@WebMvcTest` coverage of its complete public MVC contract.
- [ ] Integration tests prove the affected real wiring, transactions, persistence, migrations, concurrency, and committed state.
- [ ] Protected integration requests carry a valid credential from the approved isolated authentication flow.
- [ ] Overlap across levels proves different boundaries; no level omitted because another exists.
- [ ] Every changed scheduler: direct unit coverage plus a real-trigger integration test.
- [ ] Negative write scenarios prove prohibited data was not persisted.
- [ ] Test data comes from the established generator or a focused factory and is deterministic.
- [ ] Tests are independent and secure: no arbitrary sleeps, no live dependencies.
- [ ] Every touched Java test follows `modern-java-21`, import order included.
- [ ] Every relevant unit, MVC slice, persistence slice, and integration suite passes.
- [ ] Test selection runs integration tests in the correct phase.
- [ ] Cleanup follows the project strategy; no test depends on another test's data.

## Primary guidance

- [Spring Boot: Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Boot: Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Spring Framework: Testing](https://docs.spring.io/spring-framework/reference/testing.html)
- [Spring Framework: Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [JUnit User Guide](https://docs.junit.org/current/user-guide/)
- [Testcontainers for Java](https://java.testcontainers.org/)
