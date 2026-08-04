---
name: spring-boot-testing
description: Production testing standard for Java 21+ Spring Boot REST applications. Use whenever production behavior, APIs, services, persistence, security, configuration, scheduled jobs, schedulers, listeners, caches, or external adapters are created or changed; when creating or updating JUnit unit tests, Spring test slices, database-backed integration tests, fixtures, mocks, test containers, or test infrastructure; and when verifying a bug fix or refactor. Covers realistic scenario selection, unit and integration scope, generated test data, negative persistence verification, scheduler testing, isolation, and mandatory test execution. Excludes end-to-end and UI testing.
---

# Spring Boot Testing

Prove production behavior with deterministic, maintainable tests. Test reachable scenarios and
publicly observable outcomes, not framework internals or invented edge cases.

## Coordinate the project skills

Treat this skill as the owner of test scope, scenario selection, test doubles, fixtures, isolation,
and execution. Apply the specialized skills for the behavior being verified:

| Skill | Treat as owner of |
| --- | --- |
| `modern-java-21` | Java version, explicit types, import order, source hygiene, and Javadoc |
| `project-naming-conventions` | Test class, method, fixture, and test-data names |
| `spring-boot-patterns` | REST, service, domain, mapper, validation, error, and configuration contracts |
| `spring-data-jpa` | Database mappings, constraints, queries, transactions, locking, migrations, and production-database semantics |
| `application-security` | Required security scenarios, protected data, credentials, and trust-boundary controls |
| `spring-boot-code-review` | Review scope, evidence, severity, and reporting |

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

Load only the reference required by the changed behavior.

## Inspect before writing tests

1. Inspect the Maven or Gradle configuration, supported Java and Spring Boot versions, test source
   sets, test plugins, naming suffixes, profiles, and CI commands.
2. Reuse the project's supported JUnit Jupiter version and assertion, mocking, data-generation,
   container, HTTP, and stub-server libraries. Do not override the Spring Boot dependency
   management merely to obtain a newer test API.
3. Inspect nearby sound tests, shared fixtures, container configuration, database cleanup, fixed
   clocks, custom annotations, and test factories before creating alternatives.
4. Trace the changed production path and identify its observable contract, transaction effects,
   security controls, and external side effects.
5. Derive test cases from requirements and reachable branches. Do not create cases that cannot
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

Use Mockito's JUnit Jupiter extension when Mockito is the established project library. Construct the
subject explicitly when that makes dependencies and test setup clearer. Do not use lenient stubbing
or broad `any()` matching to hide an inaccurate fixture.

## Test every REST controller with an MVC slice

A Spring slice test is neither a pure unit test nor a substitute for full integration coverage.
Every REST controller requires focused `@WebMvcTest` coverage with its services and other downstream
collaborators mocked through the mechanism supported by the project version. Prove every handler's
routing and delegation plus applicable validation, request and response serialization, status,
headers, security behavior, and public error contract.

Use `@DataJpaTest` for focused mapping and repository behavior when its database replacement and
migration configuration still match the scenario. Use another slice only when the inspected project
version supports it and the slice proves the required boundary.

Do not use an MVC slice as evidence for transaction, database, or other full-application behavior
excluded from that slice. Conversely, do not omit required MVC slice coverage because a full HTTP
integration test exercises the same route. The overlap is intentional: each level proves a different
boundary.

Do not create a repository integration test for inherited CRUD behavior merely because a repository
exists. Add focused persistence coverage when custom queries, mappings, converters, projections,
constraints, ordering, pagination, locking, flush behavior, or database-specific semantics require
direct proof. A full application integration test may already provide sufficient persistence
evidence for a simple path.

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
- replace only true external systems with controlled stubs, fakes, emulators, or containers;
- verify the response or other public result and the committed database state after success;
- verify the public error contract and prove that invalid or rejected data was not persisted after
  every negative write scenario;
- verify absence of messages, cache entries, files, or external calls when failure must prevent them;
- avoid test-managed `@Transactional` on HTTP write tests when rollback would hide commit behavior;
- use the project's explicit database reset or cleanup strategy so tests remain isolated.

Full application integration coverage must prove affected real wiring, transactions, persistence,
migrations, concurrency behavior, and committed database state. Cover each item when the feature can
exercise it; do not invent concurrency cases for a path with no concurrency contract. Important
scenarios may overlap with service unit or MVC slice tests when the integration test proves a
different boundary. Integration coverage never replaces either required lower level.

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

Apply `application-security` to select cases. Exercise the actual enforcement boundary: filter chain
for HTTP access, service and database scope for object or tenant authorization, serializer for data
exposure, and provider adapter for outbound restrictions. Do not disable filters, CSRF, or
authorization merely to make integration tests pass. Use synthetic identities and never use real
tokens, credentials, customer data, or production endpoints.

## Execute and report verification

Run the narrowest changed test first for quick feedback. Then run all relevant unit and integration
tests using the repository's Maven or Gradle lifecycle, including the integration-test source set or
plugin phase. Run the formatter, compiler, and static analysis required by the project.

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
- [ ] Every affected REST controller has `@WebMvcTest` coverage for its complete public MVC contract.
- [ ] Full application integration tests prove affected real wiring, transactions, persistence, migrations, concurrency, and committed state.
- [ ] Overlap across levels proves different boundaries; no level was omitted because another exists.
- [ ] Every changed scheduler has direct unit coverage and a real-trigger integration test.
- [ ] Negative write scenarios prove that prohibited data was not persisted.
- [ ] Test data uses the established generator or focused factory and is deterministic.
- [ ] Tests are independent, secure, and free from arbitrary sleeps and live dependencies.
- [ ] Every touched Java test follows `modern-java-21`, including the project import order.
- [ ] Focused and relevant complete unit and integration suites pass.

## Primary guidance

- [Spring Boot: Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Boot: Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Spring Framework: Testing](https://docs.spring.io/spring-framework/reference/testing.html)
- [Spring Framework: Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [JUnit User Guide](https://docs.junit.org/current/user-guide/)
- [Testcontainers for Java](https://java.testcontainers.org/)
