---
name: spring-boot-patterns
description: Production Spring Boot patterns for Java 21+ backend REST APIs, REST controllers, services, domain models, validation, transport objects (TOs), mapping, persistence, exception handling, configuration, security boundaries, observability, and feature packaging. Excludes server-side page rendering and UI views. Use for every new Spring Boot REST feature or modification to controllers, services, configuration, scheduled jobs, listeners, or API contracts.
---

# Spring Boot Patterns Skill

Implement vertical, tested features using the project's supported Spring Boot version. Preserve existing contracts, but do not copy legacy architecture or anti-patterns into new code.

## Coordination with other skills

Apply `modern-java-21` to every touched Java file. It owns Java language use, imports, local type inference, Javadoc, nullability, exceptions, source structure, and general Java tests.

Apply `spring-data-jpa` whenever code touches entities, repositories, persistence queries, transactions, locking, migrations, or database performance. It owns persistence behavior; this skill owns the Spring Boot boundaries around it.

Apply `application-security` whenever a change crosses a trust boundary or affects identity, authorization, confidential data, dangerous input, external systems, dependencies, deployment, messaging, jobs, or operational security. Apply `project-naming-conventions` whenever a name or escaped contract is created or changed.

Each owner skill is authoritative in its area. Follow a repository-enforced formatter or policy when the owner skill permits it, preserve compatible established contracts, and report an unresolved conflict instead of inventing a second standard here.

## Reference routing

Read only the references relevant to the task:

- Read [REST API examples](references/rest-api-examples.md) when creating or changing a REST controller, request/response TO, REST mapper, validation response, or `ProblemDetail` handler.
- Read [service and domain examples](references/service-domain-examples.md) when creating or changing a service, domain model, domain mapper, service parameter object, or repository boundary.
- Read [infrastructure examples](references/infrastructure-examples.md) when deciding package placement or changing method validation, custom exceptions, configuration properties, infrastructure beans, or code that resembles a listed anti-pattern.

The references contain complete examples and are normative where this file points to them. Do not load them for unrelated work.

## REST-only scope

Build backend HTTP APIs only. The application may use Spring Web's servlet infrastructure internally, but that does not authorize generating a server-rendered presentation layer.

- Use `@RestController` and `@RestControllerAdvice` so handlers write status, headers, and serialized response bodies directly.
- Return typed transport objects, `ProblemDetail`, an explicitly supported file/resource response, or an empty response with the correct status.
- Use machine-readable media types defined by the API contract, normally JSON.
- Do not generate `@Controller`, `Model`, `ModelMap`, `ModelAndView`, `View`, view-name return values, redirects to rendered pages, view resolvers, template directories, or server-side UI flows.
- Using `WebClient` for an outgoing call does not make the server reactive. Do not migrate the server from the established servlet stack to WebFlux solely because `WebClient` is present.

## Rules before coding

1. Inspect `pom.xml` or Gradle files, the configured Java and Spring Boot versions, existing package layout, tests, configuration, migrations, security, and API error format.
2. Read the full call path affected by the change: controller/listener, service, domain, persistence, cache, and external adapters.
3. Define acceptance cases, invalid input, missing data, conflicts, authorization, dependency failures, and transaction effects.
4. Design the smallest cohesive change. Do not perform unrelated modernization.
5. Implement production code and tests together.

## Java source rules

Apply the complete `modern-java-21` workflow to every created or modified `.java` file. Do not restate or fork its import, type-inference, Javadoc, exception, size, or source-hygiene rules in this skill. Run the repository formatter and the narrowest relevant compile or static-analysis check before finishing.

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
| `UserRestMapper` | Domain to response TO; request TO to a focused domain input only when justified |
| `UserDomainMapper` | Entity/projection to domain; explicit creation values to entity |

Read the package layout in [infrastructure examples](references/infrastructure-examples.md) when placing new types.

## REST controllers

Keep REST controllers thin. They must not query repositories, mutate entities, implement business rules, manage transactions, catch generic exceptions, or prepare server-rendered views.

- Use `@RestController`; do not use a view-oriented `@Controller` for REST endpoints.
- Keep API versioning consistent with the existing public contract; do not introduce versioning arbitrarily.
- Use nouns in resource paths and correct HTTP methods/status codes.
- Define or preserve supported request and response media types. Return serialized bodies rather than view names.
- Validate path, query, header, and body input at the boundary.
- Define collection bounds, string lengths, numeric bounds, and pagination limits for untrusted input.
- On Spring Framework 6.1+, prefer built-in REST handler method validation and do not place `@Validated` on the controller. On earlier supported versions, use type-level `@Validated` only when proxy-based controller method validation is required. Never place it on an individual handler method.
- When controller parameters can trigger both object and method validation, preserve Spring's standard handling or map both validation exception types into the same public error contract.
- For a synchronous operation that creates an addressable resource, return `201 Created` and a server-owned `Location` URI for that resource. Do not require this combination for a POST action that does not have resource-creation semantics; preserve the documented API contract.
- Return typed response models, not entities, `Map<String, Object>`, or `ResponseEntity<?>`.
- If the project has an OpenAPI contract, update and validate it with the implementation; do not allow endpoint, schema, status, or media-type drift.
- Preserve backward compatibility in field names, enum values, requiredness, null behavior, status codes, and error shapes.

Read the complete controller example in [REST API examples](references/rest-api-examples.md).

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
- When MapStruct is already approved, use it for structural mapping and fail the build for unintended unmapped targets. Obtain a stateless, dependency-free mapper through its static `INSTANCE = Mappers.getMapper(...)` member; do not register or inject it as a Spring bean. Use the Spring component model only when the mapper genuinely requires container-managed collaborators, decorators, object factories, or another documented DI capability. Use handwritten mapping for behavior, non-trivial normalization, or mapping that is clearer without generated code.

Read the TO and REST mapper examples in [REST API examples](references/rest-api-examples.md).

## Domain models

Domain models are independent of REST TOs and JPA entities. Services return domain models, controllers map them to response TOs, and repositories continue to work with persistence entities.

- Do not add JPA, HTTP, JSON, controller, repository, or Spring infrastructure concerns to a domain model.
- Keep the domain model immutable when practical.
- Put business invariants and behavior in the domain when they naturally belong to the represented business concept.
- Do not expose `UserEntity` outside the service/persistence boundary.
- Do not create a domain type that merely aliases a TO; the two models may look similar but belong to different boundaries and may evolve independently.

Read the domain and domain mapper examples in [service and domain examples](references/service-domain-examples.md).

## Services

Services implement operations and own orchestration.

- Treat application services used by inbound adapters as intentional application contracts. Define a `<Capability>Service` interface and a `<Capability>ServiceImpl` Spring bean.
- Put caller-facing Javadoc and method-validation constraints on the service interface. Put `@Service`, `@Validated`, transactions, dependencies, and implementation logic on the implementation class without duplicating the contract.
- Do not create interface/implementation pairs for internal helpers, stateless utilities, or types with no application-service contract.
- Use Lombok constructor generation only when Lombok is an established project dependency and the generated constructor remains obvious; otherwise write the constructor explicitly.
- Do not accept REST request/response TOs and do not return JPA entities.
- Return domain objects such as `UserDomain`; map entities to domain objects before crossing the service boundary.
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

Read the complete service, parameter-object, mapper, and repository-boundary examples in [service and domain examples](references/service-domain-examples.md).

## Repository boundary

Apply `spring-data-jpa` for detailed repository and query rules.

- Use derived queries when they remain readable and unambiguous.
- Return `Optional` for an optional single result; never accept `Optional` as a parameter or entity field.
- Use `existsBy` instead of loading an entity for existence checks.
- Avoid native queries unless JPQL, Criteria, or a repository abstraction cannot express the required behavior clearly.
- Use `@EntityGraph`, projections, or explicit fetch joins to solve measured fetch-plan problems; do not default every association to eager loading.

Read the repository-boundary example in [service and domain examples](references/service-domain-examples.md) only when a Spring Boot feature requires a repository change.

## Validation

Use Jakarta Bean Validation for structural constraints.

- Use `@Valid` for nested object validation.
- Use method validation when the service can be called outside the REST request boundary.
- Create a custom constraint only for reusable structural validation; keep database-dependent and business validation in a service/domain policy.
- Error messages exposed to users must be stable and safe. Do not expose implementation class names or SQL/provider details.

Read the method-validation example in [infrastructure examples](references/infrastructure-examples.md).

## Error handling

Use the project's existing error contract. For a new API on a supported Spring version, prefer RFC 9457 `ProblemDetail` with stable application error codes.

- Map expected application failures explicitly.
- Let Spring's framework handler preserve standard REST error behavior where appropriate.
- Avoid a catch-all handler that leaks exception messages. If a top-level handler is required, return a generic message and log the cause once.
- Do not copy `exception.getMessage()` into a response unless that exception type guarantees a stable, user-safe message.
- Do not log expected 4xx validation/not-found failures as server errors.
- Never include stack traces, SQL, internal endpoints, credentials, or personal data in responses.

Read the complete `ProblemDetail` handler in [REST API examples](references/rest-api-examples.md) and the custom exception examples in [infrastructure examples](references/infrastructure-examples.md).

## Configuration properties

Use type-safe, validated configuration instead of scattered `@Value` fields.

- Use environment variables or a secret manager for secrets; never commit credentials.
- Validate required configuration at startup.
- Do not hardcode environment URLs, AWS regions, bucket names, timeouts, or feature behavior in production code.
- Keep the main `@SpringBootApplication` class minimal; place feature configuration in focused classes.
- Avoid `proxyBeanMethods = true` unless inter-bean method proxying is required.

Read the configuration records and bean example in [infrastructure examples](references/infrastructure-examples.md).

## Security boundary

- Authentication is not authorization. Enforce resource/tenant ownership in the use case or repository query, not only in the controller.
- Use method or request authorization consistent with the project's Spring Security model.
- Never trust tenant/user identifiers supplied in a body when the authenticated principal determines them.
- Apply least privilege to data access, AWS clients, and operational endpoints.
- Do not disable CSRF, CORS, authentication, or security filters just to make a test pass.
- Do not log tokens, cookies, authorization headers, or sensitive payloads.

## Observability

- Log significant boundaries and outcomes with correlation identifiers.
- Add metrics/traces around slow or failure-prone external boundaries and important business outcomes.
- Avoid high-cardinality metric tags such as raw user ID, order ID, URL, exception message, or email.
- Do not log every method entry/exit.
- Health indicators must reflect actionable dependency state and must not expose secrets.

## Scheduled and asynchronous work

- Make scheduled jobs idempotent and safe when multiple application instances run.
- Use a distributed lock or database claim pattern when a job must run once across the cluster.
- Bound batches and memory usage; persist progress/checkpoints for large work.
- Configure executors explicitly where concurrency matters.
- Propagate context intentionally and handle failures; never fire-and-forget critical work silently.
- Evaluate virtual threads only after confirming blocking model, pinning, connection pools, and operational behavior.

## Tests required with every feature

Decide and implement every relevant level:

- pure unit test for domain/service decisions;
- REST controller slice test for routing, serialization, content type, validation, status, security, and errors; use `@WebMvcTest` with `MockMvc` or `MockMvcTester` for the servlet stack already used by the project;
- persistence integration test for queries, constraints, transaction behavior, and the configured production database semantics;
- client integration test for external failure, timeout, retry, and response handling.

Every bug fix needs a regression test that demonstrates the previous failure.

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
- hardcoded configuration or secrets;
- self-invocation assumptions for proxy annotations;
- generic exception swallowing;
- unbounded collection endpoints;
- remote I/O inside long transactions;
- features without tests;
- touched Java files with unused, wildcard, duplicate, or unordered imports.

Read the rejected code examples in [infrastructure examples](references/infrastructure-examples.md) when reviewing or replacing suspicious existing code.

## Completion checklist

- [ ] Controller/listener is a thin transport boundary.
- [ ] Business rules are in service/domain code.
- [ ] TOs are explicit, validated, controller-owned, and separate from domain models and entities.
- [ ] Service signatures use clear explicit parameters up to seven; signatures with eight or more were redesigned, cohesively grouped, or explicitly justified.
- [ ] REST and domain mappers preserve the TO–Domain–Entity boundaries.
- [ ] Transactions and security ownership are explicit.
- [ ] Error responses are stable and safe.
- [ ] Configuration is type-safe, externalized, and validated.
- [ ] Tests cover successful and unsuccessful behavior.
- [ ] Every touched Java file has clean, correctly ordered imports.
- [ ] Relevant formatter, tests, and build checks pass.
