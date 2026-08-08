---
name: spring-boot-patterns
description: Production Spring Boot patterns for Java 21+ backend REST APIs, REST controllers, services, domain models, validation, transport objects (TOs), mapping, persistence, exception handling, configuration, security boundaries, observability, and feature packaging. Excludes server-side page rendering and UI views. Use for every new Spring Boot REST feature or modification to controllers, services, configuration, scheduled jobs, listeners, or API contracts.
---

# Spring Boot Patterns Skill

Implement vertical, tested features using the project's supported Spring Boot version. Preserve existing contracts, but do not copy legacy architecture or anti-patterns into new code.

## Coordination with other skills

Apply `modern-java-21` to every touched Java file. It owns Java language use, imports, local type
inference, Javadoc, nullability, exception mechanics, and source structure. Apply
`project-naming-conventions` to exception names; this skill owns REST error translation.

Apply `spring-boot-testing` whenever production behavior or tests change. It owns test scope,
realistic scenario selection, unit and integration structure, fixtures, isolation, and execution;
this skill owns the Spring contracts those tests must prove.

Apply `spring-data-jpa` whenever code touches entities, repositories, persistence queries, transactions, locking, migrations, or database performance. It owns persistence behavior; this skill owns the Spring Boot boundaries around it.

Apply `build-and-dependencies` whenever a build file, dependency, plugin, version, compiler setting, annotation processor, or test-selection configuration changes. It owns the build; this skill owns the application design the build serves.

Apply `observability-and-logging` whenever a change adds or alters logging, correlation context, metrics, tracing, actuator endpoints, or health indicators. It owns instrumentation; this skill owns the layers being instrumented.

Apply `application-security` whenever a change crosses a trust boundary or affects identity, authorization, confidential data, dangerous input, external systems, dependencies, deployment, messaging, jobs, or operational security. Apply `project-naming-conventions` whenever a name or escaped contract is created or changed.

Each owner skill is authoritative in its area. Follow a repository-enforced formatter or policy when the owner skill permits it, preserve compatible established contracts, and report an unresolved conflict instead of inventing a second standard here.

## Reference routing

Read only the references relevant to the task:

- Read [REST API examples](references/rest-api-examples.md) when creating or changing a REST controller, request/response TO, REST mapper, validation response, or `ProblemDetail` handler.
- Read [service and domain examples](references/service-domain-examples.md) when creating or changing a service, domain model, domain mapper, service parameter object, or repository boundary.
- Read [infrastructure examples](references/infrastructure-examples.md) when deciding package placement or changing method validation, custom exceptions, configuration properties, infrastructure beans, or code that resembles a listed anti-pattern.

The references contain focused examples. Treat the illustrated decisions and accompanying rules as
normative, but do not assume omitted members or configuration are complete. Do not load a reference
for unrelated work.

## REST-only scope

Build backend HTTP APIs only. In this skill, Spring MVC means the servlet-based REST infrastructure
used by `@RestController`; it does not imply server-rendered views. Using that infrastructure does
not authorize generating a server-rendered presentation layer.

- Use `@RestController` and `@RestControllerAdvice` so handlers write status, headers, and serialized response bodies directly.
- Return typed transport objects, `ProblemDetail`, an explicitly supported file/resource response, or an empty response with the correct status.
- Use machine-readable media types defined by the API contract, normally JSON.
- Do not generate `@Controller`, `Model`, `ModelMap`, `ModelAndView`, `View`, view-name return values, redirects to rendered pages, view resolvers, template directories, or server-side UI flows.
- Using `WebClient` for an outgoing call does not make the server reactive. Do not migrate the server from the established servlet stack to WebFlux solely because `WebClient` is present.

## Establish the project profile

This skill owns one small, durable record of the decisions every other skill inspects:
`docs/project-profile.md`.

Read it before coding. It must state, at minimum:

- Java release, Spring Boot version, build tool, and whether the project uses Lombok, per `build-and-dependencies`;
- relational database engine and major version, and the migration tool (Flyway or Liquibase);
- authentication model for this deployable service, per `application-security`;
- whether a cache is used and which technology, when one has been selected;
- whether the project uses the application-service interface plus `*ServiceImpl` convention;
- whether entities use fluent or `void` setters;
- test source-set and test-selection configuration, per `spring-boot-testing`;
- log format, correlation header, tracing, metrics registry, and exposed actuator endpoints, per `observability-and-logging`.

When the file is missing or a required decision is absent — an empty repository, or a bare Spring
Initializr skeleton with nothing recorded — **ask the user for the missing decisions and write them
into `docs/project-profile.md` before implementing**. Ask once, in one message, for everything the
task depends on. Do not silently assume a database, an authentication model, a cache, or a service
convention. Existing repository evidence, such as a declared dependency or an applied migration,
is an acceptable answer; record it in the profile so later tasks do not ask again.

## Rules before coding

1. Inspect `pom.xml` or Gradle files, the configured Java and Spring Boot versions, existing package layout, tests, configuration, migrations, security, and API error format.
2. Read the full call path affected by the change: controller/listener, service, domain, persistence, cache, and external adapters.
3. Define acceptance cases, invalid input, missing data, conflicts, authorization, dependency failures, and transaction effects.
4. Design the smallest cohesive change. Do not perform unrelated modernization.
5. Implement production code and tests together.

## Java source rules

Apply the complete `modern-java-21` workflow to every created or modified `.java` file. Do not restate or fork its import, type-inference, Javadoc, exception, size, or source-hygiene rules in this skill. That skill's import order is a project standard without exceptions; run the narrowest relevant compile or static-analysis check before finishing.

## Architecture and boundaries

Use the repository's existing sound structure instead of performing a broad package migration. Keep these responsibilities explicit regardless of package names:

| Boundary | Responsibility |
| --- | --- |
| REST controller/listener | Parse and validate transport input, delegate, and map domain output to a TO |
| REST mapper | Map domain results to response TOs and, only for a justified focused input, map a request TO to a domain/service input |
| Service | Accept explicit method parameters or a justified parameter object, orchestrate the operation, return domain objects, and own the transaction boundary |
| Domain | Represent business state, invariants, and decisions without REST or persistence dependencies |
| Domain mapper | Translate between persistence entities/projections and domain objects; map explicit creation values to a new entity when required |
| Repository/adapter | Encapsulate persistence or external-provider details |
| Configuration | Construct and configure infrastructure beans |

Use the established terminology consistently:

| Type | Boundary |
| --- | --- |
| `UserCreateTO`, `UserUpdateTO`, `UserTO` | REST/controller |
| `UserDomain`, focused service parameter objects | Domain/service |
| `UserEntity` | JPA persistence |
| `UserSummaryProjection` | Repository persistence projection |
| `UserRestMapper` | Domain to response TO; request TO to a focused domain input only when justified |
| `UserDomainMapper` | Entity/projection to domain; explicit creation values to entity |

Read the package layout in [infrastructure examples](references/infrastructure-examples.md) when placing new types.

## REST controllers

Keep REST controllers thin. They must not query repositories, mutate entities, implement business rules, manage transactions, catch generic exceptions, or prepare server-rendered views.

- Use `@RestController`; do not use a view-oriented `@Controller` for REST endpoints.
- Keep API versioning consistent with the existing public contract; do not introduce versioning arbitrarily.
- Declare the API base path exactly once in Java, as a constant such as `ApiPaths.API_V1`, and build every controller's route constant from it. Declare each controller's own route as a `public static final String` on that controller so tests and `Location` construction reuse it instead of repeating the literal.
- The same prefix appears in the OpenAPI document only as `servers.url`. OpenAPI Path Items stay resource-relative, such as `/users/{userId}`, so the version never reaches `operationId` or the handler method name. `project-naming-conventions` owns that derivation.
- Use nouns in resource paths and correct HTTP methods/status codes.
- Define or preserve supported request and response media types. Return serialized bodies rather than view names.
- Validate path, query, header, and body input at the boundary.
- Define collection bounds, string lengths, numeric bounds, and pagination limits for untrusted input.
- Declare each shared numeric bound once as a `public static final` compile-time constant, for example a project-owned `PaginationConstraints.MAXIMUM_PAGE_SIZE`, and reference that constant from every annotation that enforces it at the REST boundary and on the service contract. Never repeat the literal value in a second annotation or in Javadoc.
- On Spring Framework 6.1+, prefer built-in REST handler method validation and do not place `@Validated` on the controller. On earlier supported versions, use type-level `@Validated` only when proxy-based controller method validation is required. Never place it on an individual handler method.
- When controller parameters can trigger both object and method validation, preserve Spring's standard handling or map both validation exception types into the same public error contract.
- For a synchronous operation that creates an addressable resource, return `201 Created` and a server-owned `Location` URI for that resource. Do not require this combination for a POST action that does not have resource-creation semantics; preserve the documented API contract.
- Return typed response models, not entities, `Map<String, Object>`, or `ResponseEntity<?>`.
- If the project has an OpenAPI contract, update and validate it with the implementation; do not allow endpoint, schema, status, or media-type drift.
- Preserve backward compatibility in field names, enum values, requiredness, null behavior, status codes, and error shapes.

Read the controller example in [REST API examples](references/rest-api-examples.md).

## Request and response TOs

TO means transport object in this skill. Use records for immutable request and response TOs when compatible with the serializer and project conventions.

- Never accept or return a JPA entity as an HTTP/message TO.
- Do not pass request or response TOs into the service layer.
- Map service results from `UserDomain` to `UserTO` in the REST mapper.
- Map a request TO to a focused domain input only when the service parameter-object rule justifies that input.
- Keep transport validation on request TOs and business invariants in domain/service code.
- Do not put repositories or services in TOs or mappers.
- Normalize only when the contract permits it; do not silently change user data.
- Model PATCH semantics explicitly so absent, clear, and set are not confused.
- When MapStruct is an approved project dependency, use it for all structural REST and domain mapping and set `unmappedTargetPolicy = ReportingPolicy.ERROR`.
- Keep MapStruct for the structural portion when some mapping is non-trivial. Implement non-structural behavior through focused default/helper methods or focused collaborators; do not replace the whole mapper with a handwritten class merely for that reason.
- Use a fully handwritten mapper only when MapStruct is genuinely unsuitable, and document the concrete reason.
- Obtain a stateless, dependency-free mapper through its static `INSTANCE = Mappers.getMapper(...)` member; do not register or inject it as a Spring bean. Use the Spring component model only when the mapper genuinely requires a documented container-managed capability.

Read the TO and REST mapper examples in [REST API examples](references/rest-api-examples.md).

## Domain models

Domain models are independent of REST TOs and JPA entities. Services return domain models, controllers map them to response TOs, and repositories continue to work with persistence entities.

- Do not add JPA, HTTP, JSON, controller, repository, or Spring infrastructure concerns to a domain model.
- Keep the domain model immutable when practical.
- Put business invariants and behavior in the domain when they naturally belong to the represented business concept.
- Do not expose `UserEntity` outside the service/persistence boundary.
- Do not create a domain type that merely aliases a TO; the two models may look similar but belong to different boundaries and may evolve independently.
- Keep a domain-owned enum beside the related domain types. Keep transport-only or persistence-only enums inside their owning boundary package; never collect unrelated enums in a generic package.

Read the domain and domain mapper examples in [service and domain examples](references/service-domain-examples.md).

## Services

Services implement operations and own orchestration.

- The application-service interface is optional. Read `docs/project-profile.md` first: if it records
  the `<Capability>Service` plus `<Capability>ServiceImpl` convention, follow it for every new
  application service in that scope. If it records the concrete-service convention, follow that. If
  the profile is silent, ask the user once and record the answer.
- With no recorded convention and no answer yet, default to a single concrete `<Capability>Service`
  class annotated with `@Service`, and introduce an interface only when there is a concrete reason:
  a meaningful application boundary crossed by another module, more than one implementation, a port
  with a substitutable adapter, or a contract that an external consumer implements.
- Wanting an `Impl` suffix, wanting somewhere to put Javadoc, or wanting to mock the service in a
  unit test are not reasons. Mockito mocks a concrete class, and Javadoc belongs on the concrete
  service when no interface exists.
- Both shapes are shown in [service and domain examples](references/service-domain-examples.md).
  Do not mix them for services in the same scope.
- When an interface exists, put caller-facing Javadoc and method-validation constraints on it. Put
  `@Service`, `@Validated`, transactions, dependencies, and implementation logic on the concrete
  class without duplicating the contract.
- Do not create an empty or responsibility-free interface merely to obtain an `Impl` class. Do not
  prohibit `*Impl` when it is the selected project convention.
- Use Lombok constructor generation only when Lombok is an established project dependency and the generated constructor remains obvious; otherwise write the constructor explicitly.
- Do not accept REST request/response TOs and do not return JPA entities.
- Return domain objects such as `UserDomain`; map entities to domain objects before crossing the service boundary.
- Apply entity mutations through the accessor style recorded in `docs/project-profile.md`. The examples use fluent setters that return the entity; plain `void` setters are equally acceptable when the profile records that choice. Use one style across the project.
- For update operations, load the entity inside the write transaction, apply explicit business or persistence mutations, call repository `save` exactly once, and map the returned saved entity to a domain object. This project requires the explicit repository write even when JPA dirty checking would persist a managed entity. Use `saveAndFlush` only when subsequent logic must observe immediate database synchronization for a documented reason. Do not use a MapStruct `@MappingTarget` method to mutate an existing entity.
- Prefer explicit separate parameters when a project-owned service method has up to seven declared parameters and the signature remains clear.
- Treat eight or more declared parameters as a design warning. Group only values that form a cohesive domain concept or invariant into a focused parameter/value object; otherwise redesign the operation or document why the signature must remain. Do not create one catch-all input class merely to conceal unrelated values or satisfy the threshold.
- A real parameter or value object may still be used below the threshold when it already represents a stable domain concept or enforces an invariant; do not create a custom input type for every service method.
- Keep an identifier as a separate parameter when it identifies the target resource; group the remaining values in the parameter object.
- Place a service parameter object in the domain/service model boundary, name it for the represented operation or values, and keep it independent of REST and JPA. Do not introduce `Command` or `View` terminology by default.
- Do not split naturally cohesive value objects such as `Details` into scalar parameters merely to satisfy the parameter rule.
- Do not pass raw passwords, tokens, or secrets beyond the narrow boundary that hashes, encrypts, or exchanges them. Never persist or log their raw values.
- Keep business rules out of controller, mapper, repository, and entity callback code.
- Place `@Transactional` on public service methods invoked through the Spring proxy.
- Do not rely on self-invocation for `@Transactional`, `@Async`, `@Cacheable`, method validation, or other proxy advice.
- Keep database transactions short. Do not make slow external calls while holding a transaction unless the consistency design explicitly requires it.
- Use `readOnly = true` for read services when it is compatible with the persistence implementation; treat it as an optimization hint, not security.
- Do not add `@Transactional` mechanically to every service class.

Read the service, parameter-object, mapper, and repository-boundary examples in
[service and domain examples](references/service-domain-examples.md).

## Repository boundary

`spring-data-jpa` is the sole owner of repository and query design: derived versus explicit queries,
`Optional` and `existsBy` usage, native SQL, projections, fetch plans, `@EntityGraph`, pagination,
bulk DML, locking, and transaction mechanics. Do not restate, weaken, or fork those rules here, and
resolve any question about them in that skill.

This skill owns only where the boundary sits and what may cross it:

- The repository is reached from the service, never from a controller, mapper, TO, domain model, or entity callback.
- Entities and persistence projections stop at the service. Map them to domain objects before the service returns.
- Repository types live in `repository`, persistence projections in `repository.projection`, and reusable Specification types in `repository.specification`. Add either subpackage only with its first type.

Read the repository-boundary example in [service and domain examples](references/service-domain-examples.md) only when a Spring Boot feature requires a repository change.

## Validation

Use Jakarta Bean Validation for structural constraints.

- Use `@Valid` for nested object validation.
- Use method validation when the service can be called outside the REST request boundary.
- Create a custom constraint only for reusable structural validation; keep database-dependent and business validation in a service/domain policy.
- Prefer separate request TOs per operation, such as `UserCreateTO` and `UserUpdateTO`, over Bean Validation groups. Groups make one type's contract depend on the caller and are easy to apply to the wrong boundary.
- Use validation groups only when one TO genuinely serves several operations and duplicating it would be worse. Then define the group interfaces in the transport boundary beside the TO, name them for the operation, and activate them explicitly with `@Validated(Group.class)` at the handler parameter. Do not rely on `Default` group inheritance to make a constraint apply.
- Error messages exposed to users must be stable and safe. Do not expose implementation class names or SQL/provider details.
- Constraint messages are human-readable text, not the machine-readable contract. Clients branch on the problem type, never on message text.
- Localize messages only when the API contract requires it. If it does, resolve them through the project's `MessageSource` and Bean Validation message interpolation with explicit keys, drive the locale from the `Accept-Language` header with a configured default and a bounded set of supported locales, and never localize the problem type, HTTP status, or any stable identifier.

Read the method-validation example in [infrastructure examples](references/infrastructure-examples.md).

## Error handling

Use the project's existing error contract. For a new API on a supported Spring version, use RFC 9457
`ProblemDetail`.

The public, machine-readable identifier of an error is the RFC 9457 `type` URI, and nothing else.
Do not add a parallel `code`, `errorCode`, or `errorId` extension member to the response body;
two identifiers for one condition guarantee that clients branch on the wrong one. Clients branch on
`type`; `title` and `detail` are human-readable and may change. `project-naming-conventions` owns
the URI form.

Declare every caller-visible failure once, as a constant in a single project-owned error catalog
that carries the status, the `type` URI, the title, the detail, and the internal code used in logs,
events, and metrics. One declaration is what keeps the public type and the internal code from
drifting apart. Do not add a second holder for either.

`correlationId` is the one permitted extension member. It identifies the request rather than the
failure, so it is not a second error identifier, and support workflows need it in the payload a
caller copies into a ticket. Keep `traceId`, `spanId`, stack traces, exception class names, provider
messages, and internal hostnames out of the body entirely.

- Map expected application failures explicitly.
- Let Spring's framework handler preserve standard REST error behavior where appropriate.
- Avoid a catch-all handler that leaks exception messages. If a top-level handler is required, return a generic message and log the cause once.
- Do not copy `exception.getMessage()` into a response unless that exception type guarantees a stable, user-safe message.
- Do not log expected 4xx validation/not-found failures as server errors.
- Name a project-owned validation exception unambiguously, for example `BusinessValidationException`. Never declare a project exception whose simple name collides with a framework type such as `jakarta.validation.ValidationException`, and never register a handler for that framework type as if it were the project's own category.
- Never include stack traces, SQL, internal endpoints, credentials, or personal data in responses.
- Place custom exceptions in `exception` and MVC REST exception-handler classes in
  `exception.handler`. Spring Security response handling belongs to the selected security
  configuration boundary, not this package.

Read the `ProblemDetail` handler example in
[REST API examples](references/rest-api-examples.md) and the custom exception examples in
[infrastructure examples](references/infrastructure-examples.md).

## Idempotency

`application-security` owns the idempotency policy: when a key is required, how it is bound to the
authenticated subject and request fingerprint, its format, retention, and abuse controls. This skill
owns where that policy lives in the layers.

- Accept the idempotency key at the REST boundary as an explicit, validated, bounded header or field. Do not read it from arbitrary request state.
- Pass it into the service as an ordinary explicit parameter or as part of the focused service input. Never pass the request TO.
- Claim the key, execute the effect, and record the outcome inside the service transaction that owns the operation, so the claim and the effect commit or roll back together.
- Keep the claim store behind a repository or adapter like any other persistence concern. Do not put it in a controller, mapper, or entity callback.
- Return the recorded original outcome for a repeated key through the same response mapping as the first call, so the public contract is identical.
- Test simultaneous duplicates and retry-after-timeout at the integration boundary, per `spring-boot-testing`.

## Configuration properties

Use type-safe, validated configuration instead of scattered `@Value` fields.

- Use environment variables or a secret manager for secrets; never commit credentials.
- Validate required configuration at startup.
- Do not hardcode environment URLs, AWS regions, bucket names, timeouts, or feature behavior in production code.
- Keep the main `@SpringBootApplication` class minimal; place feature configuration in focused classes.
- Avoid `proxyBeanMethods = true` unless inter-bean method proxying is required.

Read the configuration records and bean example in [infrastructure examples](references/infrastructure-examples.md).

## Security boundary

Apply `application-security` as the single owner of authentication, authorization, CSRF, CORS,
confidentiality, and security verification. Preserve the service and repository boundaries defined
by this skill while applying those controls. Do not weaken production security to make tests pass;
follow `spring-boot-testing` for which test levels include the security filter chain.

## Observability

- Log significant boundaries and outcomes with correlation identifiers.
- Add metrics/traces around slow or failure-prone external boundaries and important business outcomes.
- Avoid high-cardinality metric tags such as raw user ID, order ID, URL, exception message, or email.
- Do not log every method entry/exit.
- Health indicators must reflect actionable dependency state and must not expose secrets.

## Scheduled and asynchronous work

- Keep each scheduled entry point thin and delegate business work to a service with an explicit
  transaction and failure policy.
- Make scheduled jobs idempotent and safe when multiple application instances run.
- Use a distributed lock or database claim pattern when a job must run once across the cluster.
- Bound batches and memory usage; persist progress/checkpoints for large work.
- Configure executors explicitly where concurrency matters.
- Propagate context intentionally and handle failures; never fire-and-forget critical work silently.
- Evaluate virtual threads only after confirming blocking model, pinning, connection pools, and operational behavior.
- Apply the scheduler unit and integration rules from `spring-boot-testing` whenever scheduled work
  or its configuration changes.

## Tests required with every feature

Apply the complete `spring-boot-testing` workflow whenever production behavior changes. That skill is
the single owner of required test levels, realistic-scenario filtering, security participation by
test level, fixtures, isolation, and execution. Behavior-specific cases come from their owner skills.

## Anti-patterns

Reject:

- fat controllers;
- entities in API contracts;
- entities returned from services;
- REST TOs passed into services;
- field injection;
- empty or responsibility-free service interfaces;
- parameter objects that merely hide unrelated values or mechanically satisfy a numeric threshold;
- project-owned service methods with eight or more declared parameters and no cohesive grouping, redesign, or documented justification;
- generic `Map` responses;
- a parallel `code` or `errorCode` member in a `ProblemDetail` body alongside the RFC 9457 `type`;
- a second declaration of a problem type URI or an internal error code outside the error catalog;
- `traceId`, `spanId`, or a stack trace in a `ProblemDetail` body;
- the same failure logged by both the service that threw it and the advice that handles it;
- a project exception whose simple name collides with a framework type such as `ValidationException`;
- assuming a database, authentication model, cache, or service convention that `docs/project-profile.md` does not record;
- generic `enums` packages;
- handwritten structural REST or domain mappers when approved MapStruct can express the mapping;
- REST exception handlers placed directly in the custom-exception package;
- hardcoded configuration or secrets;
- self-invocation assumptions for proxy annotations;
- generic exception swallowing;
- unbounded collection endpoints;
- remote I/O inside long transactions;
- test changes that violate `spring-boot-testing`;
- source changes that violate `modern-java-21`.

Read the rejected code examples in [infrastructure examples](references/infrastructure-examples.md) when reviewing or replacing suspicious existing code.

## Completion checklist

- [ ] `docs/project-profile.md` records every decision the change relied on, and nothing was assumed.
- [ ] Controller/listener is a thin transport boundary.
- [ ] Business rules are in service/domain code.
- [ ] TOs are explicit, validated, controller-owned, and separate from domain models and entities.
- [ ] Service signatures use clear explicit parameters up to seven; signatures with eight or more were redesigned, cohesively grouped, or explicitly justified.
- [ ] REST and domain mappers preserve the TO–Domain–Entity boundaries.
- [ ] Approved MapStruct handles structural mapping with `ReportingPolicy.ERROR`; any handwritten mapper exception is documented.
- [ ] Projections, reusable Specifications, enums, exceptions, and exception handlers use their owning packages without empty scaffolding.
- [ ] Transactions and security ownership are explicit.
- [ ] Error responses are stable and safe, the RFC 9457 `type` URI is their only machine-readable error identifier, and every caller-visible failure comes from the single error catalog.
- [ ] Shared numeric bounds such as the maximum page size are declared once and referenced, not repeated.
- [ ] Configuration is type-safe, externalized, and validated.
- [ ] The complete `spring-boot-testing` workflow was applied and required tests pass.
- [ ] Every touched Java file complies with `modern-java-21`.
- [ ] Relevant formatter, tests, and build checks pass.
