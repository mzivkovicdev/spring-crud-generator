---
name: spring-boot-patterns
description: Production Spring Boot patterns for Java 21+ backend REST APIs, REST controllers, services, domain models, validation, transport objects (TOs), mapping, persistence, exception handling, configuration, security boundaries, observability, and feature packaging. Excludes server-side page rendering and UI views. Use for every new Spring Boot REST feature or modification to controllers, services, configuration, scheduled jobs, listeners, or API contracts.
---

# Spring Boot Patterns Skill

Implement vertical, tested features using the project's supported Spring Boot version. Preserve existing contracts, but do not copy legacy architecture or anti-patterns into new code.

## Coordination with other skills

This skill owns the Spring Boot boundaries: controllers, TOs, services, domain models, the layer
structure, and the error contract. Everything else has an owner, and that owner is authoritative:

| Owner | Owns |
| --- | --- |
| `modern-java-21` | Java language use, imports, Javadoc, nullability, exception mechanics, source structure |
| `spring-boot-testing` | Test scope, scenario selection, fixtures, isolation, execution |
| `spring-data-jpa` | Entities, repositories, queries, transaction behavior inside the boundary, locking, migrations, database performance; this skill owns where that boundary sits |
| `application-security` | Trust boundaries, identity, authorization, confidential data, dangerous input, external systems |
| `rest-api-contract` | The public contract, its document, and whether a change is breaking |
| `observability-and-logging` | Logging, correlation context, metrics, tracing, actuator endpoints |
| `build-and-dependencies` | Build files, dependencies, plugins, compiler and processor configuration |
| `project-naming-conventions` | Every developer-owned name, including exception names |
| `spring-boot-code-review` | Review scope, evidence, severity, reporting |

Do not restate or fork an owner's rules here. Report an unresolved conflict instead of inventing a
second standard.

## Reference routing

Read only the references relevant to the task:

- Read [REST API examples](references/rest-api-examples.md) when creating or changing a REST controller, request/response TO, or REST mapper.
- Read [error handling examples](references/error-handling-examples.md) when adding or changing a caller-visible failure: an error catalog constant, a custom exception, a handler method, or a validation response shape.
- Read [service and domain examples](references/service-domain-examples.md) when creating or changing a service, domain model, domain mapper, service parameter object, or repository boundary.
- Read [project profile template](references/project-profile-template.md) when creating the profile or filling a missing decision.
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

## The project profile is a precondition

`docs/project-profile.md` is the record of the decisions every skill in this set reads instead of
guessing. This skill owns it, and [the template](references/project-profile-template.md) lists every
entry, its allowed values, and the skill that owns it.

**Do not write production code until the profile exists and records every decision the task
depends on.** This is a gate, not a preference. Without it each feature silently picks its own
database, service shape, accessor style, or contract direction, and the result is a codebase that
disagrees with itself in ways no review catches until much later.

Follow this order on every task:

1. Read `docs/project-profile.md`. If every decision the task needs is recorded, implement.
2. If the file is missing, create it from the template. If entries are missing, identify exactly which.
3. Fill what the repository already proves — a declared dependency, an applied migration, an existing package layout, a configured datasource.
4. **Ask the user, in one message, for everything still unresolved**, offering the template's allowed values so each answer is one word. Do not ask one question per skill, and do not ask again for something already recorded.
5. Write the answers into the profile, then implement.

Exceptions are narrow: documentation, comment, or formatting changes need no profile, and a task may
proceed on a partial profile as long as every decision *that task* touches is recorded. Never assume
a value, infer one from a test dependency or an example, or record a guess to unblock yourself.
`UNDECIDED` with a note is a legitimate entry; a fabricated value is not.

## Rules before coding

1. Confirm the project profile covers every decision the change touches, per the section above.
2. Inspect `pom.xml` or Gradle files, the configured Java and Spring Boot versions, existing package layout, tests, configuration, migrations, security, and API error format.
3. Read the full call path affected by the change: controller/listener, service, domain, persistence, cache, and external adapters.
4. Define acceptance cases, invalid input, missing data, conflicts, authorization, dependency failures, and transaction effects.
5. Design the smallest cohesive change. Do not perform unrelated modernization.
6. Implement production code and tests together.

## Java source rules

Apply the complete `modern-java-21` workflow to every created or modified `.java` file. Do not restate or fork its import, type-inference, Javadoc, exception, size, or source-hygiene rules in this skill. That skill's import order is a project standard without exceptions; run the narrowest relevant compile or static-analysis check before finishing.

## Architecture and boundaries

Use the repository's existing sound structure instead of performing a broad package migration. Keep these responsibilities explicit regardless of package names:

| Boundary | Responsibility |
| --- | --- |
| REST controller/listener | Parse and validate transport input, delegate, and map domain output to a TO |
| REST mapper | Map domain results to response TOs and, only for a justified focused input, map a request TO to a domain/service input |
| Application service | Own a use case that spans more than one aggregate: its transaction boundary and the order of the calls |
| Aggregate service | Own one aggregate: its repositories, its invariants, and every write to it |
| Domain | Represent business state, invariants, and decisions without REST or persistence dependencies |
| Domain mapper | Translate between persistence entities/projections and domain objects; map explicit creation values to a new entity when required |
| Repository/adapter | Encapsulate persistence or external-provider details |
| Configuration | Construct and configure infrastructure beans |

Use the established terminology consistently:

| Type | Boundary |
| --- | --- |
| `UserCreateTO`, `UserUpdateTO`, `UserTO` | REST/controller |
| `UserManagementApplicationService` | Use case, in the `applicationservice` package |
| `UserService`, `OrganizationService` | One aggregate root each, in the `service` package |
| `UserDomain`, focused service parameter objects | Domain/service |
| `UserEntity` | JPA persistence, in the `entity` package |
| `UserSummaryProjection` | Repository persistence projection |
| `UserRestMapper` | Domain to response TO; request TO to a focused domain input only when justified |
| `UserDomainMapper` | Entity/projection to domain; explicit creation values to entity |

Read the package layout in [infrastructure examples](references/infrastructure-examples.md) when placing new types.

## REST controllers

Keep REST controllers thin. They must not query repositories, mutate entities, implement business rules, manage transactions, catch generic exceptions, or prepare server-rendered views.

- Use `@RestController`; do not use a view-oriented `@Controller` for REST endpoints.
- `rest-api-contract` owns API versioning, deprecation, and the judgement of whether a change is breaking. Do not introduce, raise, or retire a version here.
- Declare the API base path exactly once in Java, as a constant such as `ApiPaths.API_V1`. Where each controller's own route constant lives depends on the authoring direction recorded in the project profile:
  - **Code-first:** declare it as a `public static final String` on the controller, so tests, security matchers, and `Location` construction reuse it instead of repeating the literal.
  - **Contract-first:** routes come from the generated API interface, so a controller has no route constant to expose. Declare the route constants in `ApiPaths` beside the base path, keep them equal to the document's Path Items, and have tests and security matchers reference those. Never repeat a literal, and never add a second `@RequestMapping` on the implementation.
- When the project publishes an OpenAPI document, the same prefix appears there only as `servers.url`, and Path Items stay resource-relative, such as `/users/{userId}`, so the version never reaches `operationId` or the handler method name. `project-naming-conventions` owns that derivation, and it applies to handler method names whether or not a document exists.
- Use nouns in resource paths and correct HTTP methods and status codes. Define or preserve supported media types, and return serialized bodies rather than view names.
- Validate path, query, header, and body input at the boundary, including collection bounds, string lengths, numeric bounds, and pagination limits for untrusted input.
- Declare each shared numeric bound once as a compile-time constant, for example `PaginationConstraints.MAXIMUM_PAGE_SIZE`, and reference it from every annotation that enforces it at the REST boundary and on the service contract. Never repeat the literal in a second annotation or in Javadoc.
- On Spring Framework 6.1+, prefer built-in REST handler method validation and do not place `@Validated` on the controller. On earlier supported versions, use type-level `@Validated` only when proxy-based controller method validation is required. Never place it on an individual handler method.
- When controller parameters can trigger both object and method validation, preserve Spring's standard handling or map both validation exception types into the same public error contract.
- For a synchronous operation creating an addressable resource, return `201 Created` with a server-owned `Location` URI. Do not force that combination on a POST without resource-creation semantics.
- Return typed response models, not entities, `Map<String, Object>`, or `ResponseEntity<?>`.
- A change to a public endpoint is not complete until the project's contract record reflects it, and compatibility in field names, enum values, requiredness, null behavior, statuses, and error shapes is `rest-api-contract`'s judgement, not this skill's.

Read the controller example in [REST API examples](references/rest-api-examples.md).

## Request and response TOs

TO means transport object in this skill. Use records for immutable request and response TOs when compatible with the serializer and project conventions.

- Never accept or return a JPA entity as an HTTP/message TO.
- Delegate to exactly one service per handler, never to a repository. Which level that is follows from the operation: an operation confined to one aggregate calls that aggregate service directly; an operation that reads or writes more than one aggregate, publishes an event, or must order effects calls the application service that owns that use case.
- A handler that calls two services is doing coordination in the wrong place. Move that coordination into an application service and call it instead. The controller may inject both levels; a single handler may not mix them.
- Do not pass request or response TOs into the service layer.
- Map service results from `UserDomain` to `UserTO` in the REST mapper.
- Map a request TO to a focused domain input only when the service parameter-object rule justifies that input.
- Keep both mapping directions for one concept in that concept's single `<Concept>RestMapper`. Request-side and response-side mapping are two methods on one type, never two types: `UserRestMapper` owns both, and `UserRequestMapper` beside `UserResponseMapper` is a split with no benefit. The same applies to `<Concept>DomainMapper`.
- Split a concept's mapper only for a reason recorded in the change, such as a generated mapper the project does not own or a mapping that genuinely requires different collaborators. Volume alone is not a reason; a mapper that has grown large is usually a signal that a mapping carries logic that belongs in domain or service code.
- Keep transport validation on request TOs and business invariants in domain/service code.
- Do not put repositories or services in TOs or mappers.
- Normalize only when the contract permits it; do not silently change user data.
- Model PATCH semantics explicitly so absent, clear, and set are not confused.
- Use MapStruct for all structural REST and domain mapping with `unmappedTargetPolicy = ReportingPolicy.ERROR`. When part of a mapping is non-trivial, keep MapStruct for the structural portion and implement the rest through focused default methods or collaborators; replace the whole mapper by hand only when MapStruct is genuinely unsuitable, with the reason documented.
- Obtain a stateless, dependency-free mapper through its static `INSTANCE = Mappers.getMapper(...)` member. Do not register or inject it as a Spring bean unless it needs a documented container-managed capability.

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

The service layer has two levels. The split exists so that no service depends on another service at
the same level, which is what keeps transaction boundaries findable and prevents cyclic service
graphs.

### Aggregate services

One `<Aggregate>Service` per aggregate root, in the `service` package.

- Determine the aggregate by lifecycle ownership, not by table count and not by the reference graph. A row belongs to the aggregate when it cannot exist without the root and the root is what creates and deletes it. A row that has its own lifecycle, or that other features reference directly by its own identifier, is a separate aggregate.
- The service holds every repository of its aggregate and that aggregate's domain mapper. It holds no repository of another aggregate and no other service.
- It owns the invariants of its aggregate. Do not reduce it to a pass-through over the repository: a rule about the aggregate's own state belongs here, not in the caller.
- It reads and writes only its own aggregate. It refers to another aggregate by identifier and receives any value it needs from that aggregate as an explicit parameter.
- Annotate it `@Transactional` with the default propagation, so it joins the use case's transaction when one is open and opens its own when it is called without one. Its multi-repository writes are then atomic either way. Do not use `MANDATORY`: refusing to run without a caller-supplied transaction blocks legitimate direct use from a job or a migration task, and the layering rule below is what keeps the use-case boundary where it belongs.

### Application services

One `<Capability>ApplicationService` per coherent use case group, in the `applicationservice`
package. It exists only where coordination exists. A feature whose every operation stays inside one
aggregate needs no application service at all, and adding an empty one is scaffolding.

- It owns the use case's transaction boundary: the transaction starts and ends with this method, and rollback is decided here.
- It depends only on aggregate services, ports, and adapters — never on a repository, an entity, or another application service. A repository dependency here means the aggregate service was bypassed and the aggregate now has two write paths.
- It coordinates: fetch from one aggregate service, pass explicit values to another, decide the order. Rules that belong to a single aggregate stay in that aggregate's service.
- Never add a method that only forwards to one aggregate service. A pass-through adds a second name for one operation and a second place to keep in sync, and it is the mechanism by which this class turns into a facade over the whole application. An entry point that needs a single-aggregate operation calls that aggregate service directly.
- Publish domain events through `ApplicationEventPublisher` and consume them with `@TransactionalEventListener(phase = AFTER_COMMIT)`. Never call a notification, message broker, or other external effect directly inside the transaction: a rollback after that call leaves the outside world believing something happened.
- Know what that buys and what it does not. After-commit delivery guarantees the effect never fires for work that rolled back. It does **not** guarantee the effect happens at all: the commit has already succeeded, so a crash, a redeploy, or a failure inside the listener loses the effect permanently, with no retry and no record that anything was owed. Spring also does not propagate an exception thrown in an after-commit listener back to the caller, so a silent loss looks identical to success from the outside.
- Decide per effect, and record the mechanism in `docs/project-profile.md`. Losing the effect is acceptable for a cache refresh or a best-effort metric, and the listener alone is then the right answer. Where losing it is not acceptable — payment, provisioning, a notification a person acts on, a message another system consumes — write the intent to an outbox table inside the same transaction as the business change, and deliver it from a separate process that retries until acknowledged. The after-commit listener may still trigger the first attempt; it is an optimization, not the guarantee.
- Whatever the mechanism, make failed delivery visible. Log the failure inside the listener and expose it through `observability-and-logging`, because a listener that throws produces no HTTP error, no rollback, and no caller-side signal.
- Annotate a read use case `@Transactional(readOnly = true)`. `spring-data-jpa` explains why the attribute only has an effect at this level.

### Both levels

- The service interface is optional and the convention is recorded in the project profile. Follow whichever it records, at both levels. With none recorded and no answer yet, default to concrete classes annotated `@Service`, and add an interface only for a concrete reason: a boundary another module crosses, more than one implementation, a port with a substitutable adapter, or a contract an external consumer implements. Wanting an `Impl` suffix, somewhere to put Javadoc, or a mockable type are not reasons — Mockito mocks a concrete class. Both shapes appear in [service and domain examples](references/service-domain-examples.md); do not mix them within a scope.
- When an interface exists, put caller-facing Javadoc and method-validation constraints on it, and `@Service`, `@Validated`, transactions, dependencies, and logic on the concrete class without duplicating the contract.
- Use Lombok constructor generation only when the profile records Lombok and the generated constructor remains obvious; otherwise write the constructor explicitly.
- Do not accept REST request/response TOs and do not return JPA entities.
- Return domain objects such as `UserDomain`; map entities to domain objects before crossing the service boundary.
- Apply entity mutations through the accessor style recorded in `docs/project-profile.md`. The examples use fluent setters that return the entity; plain `void` setters are equally acceptable when the profile records that choice. Use one style across the project.
- For update operations, load the entity inside the write transaction, apply explicit business or persistence mutations, call repository `save` exactly once, and map the returned saved entity to a domain object. This project requires the explicit repository write even when JPA dirty checking would persist a managed entity. Use `saveAndFlush` only when subsequent logic must observe immediate database synchronization for a documented reason. Do not use a MapStruct `@MappingTarget` method to mutate an existing entity.
- Prefer explicit separate parameters up to seven, while the signature stays clear. Treat eight or
  more as a design warning: group only values forming a cohesive domain concept or invariant into a
  focused parameter object, otherwise redesign the operation or document why the signature must
  remain. Never create a catch-all input class to conceal unrelated values or satisfy the threshold,
  and never split a naturally cohesive value object into scalars to satisfy it either.
- A parameter or value object is legitimate below the threshold when it already represents a stable
  domain concept or enforces an invariant. Keep the target resource's identifier a separate
  parameter and group the rest.
- Place a service parameter object at the domain/service boundary, name it for the operation or
  values it represents, and keep it independent of REST and JPA. Do not introduce `Command` or
  `View` terminology by default.
- Do not pass raw passwords, tokens, or secrets beyond the narrow boundary that hashes, encrypts, or exchanges them. Never persist or log their raw values.
- Keep business rules out of controller, mapper, repository, and entity callback code.
- Never rely on self-invocation for `@Transactional`, `@Async`, `@Cacheable`, or method validation. When a separate boundary is genuinely required, move it to another bean rather than working around the proxy.
- Keep transactions short. Do not make slow external calls while holding one unless the consistency design requires it.
- This skill decides only which method is transactional and which use cases are reads. `spring-data-jpa` owns what those settings mean and when they apply: `readOnly` semantics, propagation, isolation, flush timing, and locking. Read them there before overriding a default anywhere.

Read the service, parameter-object, mapper, and repository-boundary examples in
[service and domain examples](references/service-domain-examples.md).

## Repository boundary

`spring-data-jpa` is the sole owner of repository and query design: derived versus explicit queries,
`Optional` and `existsBy` usage, native SQL, projections, fetch plans, `@EntityGraph`, pagination,
bulk DML, locking, and transaction mechanics. Do not restate, weaken, or fork those rules here, and
resolve any question about them in that skill.

This skill owns only where the boundary sits and what may cross it:

- A repository is reached only from the aggregate service that owns it, never from an application service, controller, mapper, TO, domain model, or entity callback.
- Entities and persistence projections stop at the aggregate service. Map them to domain objects before it returns.
- A read that spans aggregates uses a dedicated projection or query type owned by one aggregate service, rather than a loop of calls across services. Reads may cross the boundary; writes may not.
- Repository types live in `repository`, persistence projections in `repository.projection`, and reusable Specification types in `repository.specification`. Add either subpackage only with its first type.

Read the repository-boundary example in [service and domain examples](references/service-domain-examples.md) only when a Spring Boot feature requires a repository change.

## Validation

Use Jakarta Bean Validation for structural constraints.

- Use `@Valid` for nested objects and method validation when the service can be called outside the REST boundary.
- Create a custom constraint only for reusable structural validation; database-dependent and business validation belong to a service or domain policy.
- Prefer separate request TOs per operation, such as `UserCreateTO` and `UserUpdateTO`, over Bean Validation groups, which make one type's contract depend on the caller. Use groups only when one TO genuinely serves several operations: define the group interfaces beside the TO, name them for the operation, and activate them explicitly with `@Validated(Group.class)` at the handler parameter rather than relying on `Default` inheritance.
- Constraint messages are human-readable text, never the machine-readable contract; clients branch on the problem type. Keep them stable and free of implementation class names or provider details.
- Localize messages only when the API contract requires it. If it does, resolve them through the project's `MessageSource` and Bean Validation message interpolation with explicit keys, drive the locale from the `Accept-Language` header with a configured default and a bounded set of supported locales, and never localize the problem type, HTTP status, or any stable identifier.

Read the method-validation example in [infrastructure examples](references/infrastructure-examples.md).

## Error handling

Use the project's existing error contract. For a new API on a supported Spring version, use RFC 9457
`ProblemDetail`.

The RFC 9457 `type` URI is the only machine-readable error identifier in the body. Never add a
parallel `code`, `errorCode`, or `errorId`: two identifiers for one condition guarantee that clients
branch on the wrong one. `title` and `detail` are human-readable and may change.
`project-naming-conventions` owns the URI form.

Declare every caller-visible failure once, as a constant in a single project-owned error catalog
carrying the status, the `type` URI, the title, the detail, and the internal code used in logs,
events, and metrics. One declaration keeps the public type and the internal code from drifting
apart; do not add a second holder for either.

`correlationId` is the one permitted extension member, because it identifies the request rather than
the failure and support workflows need it in the payload a caller pastes into a ticket. Keep
`traceId`, `spanId`, stack traces, exception class names, provider messages, internal hostnames,
SQL, internal endpoints, credentials, and personal data out of the body entirely.

- Map expected application failures explicitly, and let Spring's framework handler preserve standard REST error behavior where appropriate.
- A catch-all handler returns a generic message and logs the cause once. Never copy `exception.getMessage()` into a response unless that type guarantees a stable, user-safe message, and never log an expected 4xx as a server error.
- Name a project-owned validation exception unambiguously, for example `BusinessValidationException`. Never give a project exception the simple name of a framework type such as `jakarta.validation.ValidationException`, and never handle that framework type as if it were the project's category.
- Place custom exceptions in `exception` and MVC handler classes in `exception.handler`. Spring Security response handling belongs to the security configuration boundary, not this package.

Read the error catalog and `ProblemDetail` handler examples in
[error handling examples](references/error-handling-examples.md) and the custom exception examples in
[infrastructure examples](references/infrastructure-examples.md).

## Outbound calls

An adapter that calls another system is where a remote failure becomes an application failure. These
rules apply to every HTTP client, message producer, and provider SDK the application uses.

- Configure connection and read timeouts explicitly on every client. Several widely used clients default to no read timeout at all, so an unconfigured client turns one unresponsive dependency into exhausted threads and a dead application. There is no acceptable outbound call without a bounded wait.
- Keep the timeouts inside the caller's budget. The sum of an operation's outbound waits, plus its own work, must stay below the request timeout the deployment enforces; otherwise the client gives up on a request the application still believes is running.
- Retry only an operation that is safe to repeat. A read may be retried; a write may not, unless the provider accepts an idempotency key or the operation is naturally idempotent. Bound the attempts, apply backoff with jitter, and record the policy where the client is configured.
- Keep retry in one layer. A client library, the adapter, a gateway, and a scheduler each retrying three times is twenty-seven calls to a system that is already failing. Choose the layer that owns the policy and disable retry in the others.
- Never hold a database transaction open across an outbound call unless the consistency design requires it, and never retry inside one: the transaction stays open for the whole backoff.
- Translate failure at the adapter boundary. A timeout, a connection reset, a 4xx, a 5xx, and a malformed body each become a project exception the caller can act on. Never let a client library's exception type, status object, or SDK response reach a service or a controller.
- Add circuit breaking, bulkheads, or rate limiting only when `docs/project-profile.md` records a resilience library. Do not hand-roll a breaker.

`observability-and-logging` owns what an outbound call must emit. `application-security` owns
credentials, destination validation, and response-size limits.

## Idempotency

`application-security` owns the idempotency policy: when a key is required, how it is bound to the
authenticated subject and request fingerprint, its format, retention, and abuse controls. This skill
owns where that policy lives in the layers, and
[infrastructure examples](references/infrastructure-examples.md) carries those placement rules.

## Configuration properties

Use type-safe, validated configuration instead of scattered `@Value` fields.

- Use environment variables or a secret manager for secrets; never commit credentials.
- Validate required configuration at startup.
- Do not hardcode environment URLs, AWS regions, bucket names, timeouts, or feature behavior in production code.
- Keep the main `@SpringBootApplication` class minimal; place feature configuration in focused classes.
- Configuration property types live in `config.properties`. Bean configuration types live directly in `config`. A `@ConfigurationProperties` record is bound data with no beans of its own, and a `@Configuration` class constructs beans, so separating them keeps the two visible without reading annotations.
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

- Keep each scheduled entry point thin and delegate business work to a service with an explicit transaction and failure policy.
- Make scheduled jobs idempotent and safe when multiple application instances run.
- Apply the scheduler unit and integration rules from `spring-boot-testing` whenever scheduled work or its configuration changes.

[Infrastructure examples](references/infrastructure-examples.md) carries the clustering, batching,
executor, context-propagation, and virtual-thread rules.

## Tests required with every feature

Apply the complete `spring-boot-testing` workflow whenever production behavior changes. That skill is
the single owner of required test levels, realistic-scenario filtering, security participation by
test level, fixtures, isolation, and execution. Behavior-specific cases come from their owner skills.

## Anti-patterns

Reject:

**Boundary violations.** Fat controllers; entities in API contracts or returned from services; REST
TOs passed into services; an outbound client with no read timeout; a provider exception or SDK
response type reaching a service or controller; remote I/O inside long transactions; unbounded collection endpoints;
generic `Map` responses; a repository injected into an application service; a controller handler
calling two services; an application service method that only forwards to one aggregate service; one
aggregate service depending on another; a JPA association crossing an aggregate boundary.

**Structure.** Field injection; an aggregate service that only forwards to its repository while its
invariants live in callers; an application service holding a rule that belongs to one aggregate;
empty or responsibility-free service interfaces; parameter objects
that hide unrelated values or mechanically satisfy a numeric threshold; eight or more parameters
with no grouping, redesign, or documented justification; generic `enums` packages; REST exception
handlers in the custom-exception package; handwritten structural mappers where approved MapStruct
can express the mapping; a separate mapper per mapping direction for one concept.

**Error contract.** A `code` or `errorCode` member beside the RFC 9457 `type`; a problem type URI or
internal error code declared outside the error catalog; `traceId`, `spanId`, or a stack trace in a
`ProblemDetail` body; the same failure logged by both the service that threw it and the advice that
handles it; a project exception whose simple name collides with a framework type such as
`ValidationException`; generic exception swallowing.

**Process.** Implementing against a decision `docs/project-profile.md` does not record; hardcoded
configuration or secrets; self-invocation assumptions for proxy annotations; an external effect
fired inside the transaction instead of after commit.

Read the rejected code examples in [infrastructure examples](references/infrastructure-examples.md) when reviewing or replacing suspicious existing code.

## Completion checklist

- [ ] `docs/project-profile.md` existed before implementation and records every decision the change relied on. Nothing was assumed, inferred, or guessed.
- [ ] Controller/listener is a thin transport boundary.
- [ ] Business rules are in service/domain code, and each rule sits at the level that owns it.
- [ ] The transaction boundary is the application service; no aggregate service overrides propagation or isolation to escape it.
- [ ] No application service holds a repository or a pass-through method, and no aggregate service holds another service.
- [ ] Every controller handler calls one service, at the level the operation belongs to.
- [ ] Each external effect uses the delivery mechanism the profile records for it, and a failed delivery is logged rather than silently dropped.
- [ ] Every outbound client sets connection and read timeouts, and any retry policy exists in exactly one layer.
- [ ] TOs are explicit, validated, controller-owned, and separate from domain models and entities.
- [ ] Service signatures use clear explicit parameters up to seven; signatures with eight or more were redesigned, cohesively grouped, or explicitly justified.
- [ ] REST and domain mappers preserve the TO–Domain–Entity boundaries.
- [ ] Approved MapStruct handles structural mapping with `ReportingPolicy.ERROR`; any handwritten mapper exception is documented.
- [ ] Each concept has one REST mapper and one domain mapper; any split is documented in the change.
- [ ] Projections, Specifications, enums, exceptions, handlers, bean configuration, and configuration property types sit in their owning packages without empty scaffolding.
- [ ] Transactions and security ownership are explicit.
- [ ] Error responses are stable and safe, the `type` URI is their only machine-readable error identifier, and every caller-visible failure comes from the single error catalog.
- [ ] Shared numeric bounds such as the maximum page size are declared once and referenced.
- [ ] Configuration is type-safe, externalized, and validated.
- [ ] The owner skills were applied: `spring-boot-testing` for tests, `modern-java-21` for every touched file, and the quality gates and test suites pass.
