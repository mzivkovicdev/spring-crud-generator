---
name: spring-boot-patterns
description: Production Spring Boot patterns for Java 21+ backend REST APIs, REST controllers, services, domain models, validation, transport objects (TOs), mapping, persistence, exception handling, configuration, security boundaries, observability, and feature packaging. Excludes server-side page rendering and UI views. Use for every new Spring Boot REST feature or modification to controllers, services, configuration, scheduled jobs, listeners, or API contracts.
---

# Spring Boot Patterns Skill

Implement vertical, tested features using the project's supported Spring Boot version. Preserve existing contracts, but do not copy legacy architecture or anti-patterns into new code.

## Coordination with other skills

This skill owns the Spring Boot boundaries: controllers, TOs, services and their two levels, domain
models, mappers, validation, the error contract, configuration design, package responsibilities, and
where the transaction boundary sits. It also owns `docs/project-profile.md` and the decision tokens
every other skill reads.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what, and
it carries the precedence order for a genuine conflict. Read it there rather than from a copy in
this file. The seams this skill crosses most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| Transactions | which method carries the annotation, and where the boundary sits | `spring-data-jpa` owns what the settings mean |
| Locking | which layer the retry annotation sits on | `spring-data-jpa` owns `@Version`, lock modes, and the retry mechanism |
| Repositories | where the boundary sits and what may cross it | `spring-data-jpa` owns repository and query design |
| The public contract | the TO, the `ProblemDetail`, the error catalog | `rest-api-contract` owns whether a change to them is breaking |
| Instrumentation | where it sits in the layers | `observability-and-logging` owns what is emitted and at what level |

Do not restate or fork an owner's rules here. Report an unresolved conflict instead of inventing a
second standard; when it is genuine and cannot wait, apply the precedence order in
[the ownership map](../_core/OWNERSHIP.md) and say in the handoff which rule was set aside.

## Reference routing

This skill's body carries the decisions and the rules that decide most reviews. The detail sits in
references, split into two kinds: **rule references**, which are normative and complete, and
**example references**, which show the rules applied. Read only what the task needs.

Rule references:

- Read [REST boundary rules](references/rest-boundary-rules.md) when creating or changing a controller, a request/response TO, or a REST mapper.
- Read [service layer rules](references/service-layer-rules.md) when creating or changing a service, deciding which service level an operation belongs to, or producing an effect outside a transaction.
- Read [outbound call rules](references/outbound-call-rules.md) when adding or changing any call to another system: an HTTP client, a message producer, or a provider SDK.
- Read [filling the project profile](references/project-profile-template.md) when creating the profile or filling a missing decision. The template itself is [an asset](assets/project-profile-template.md) to be copied, not retyped.

Example references:

- Read [REST API examples](references/rest-api-examples.md) for controller, TO, and REST mapper code.
- Read [service and domain examples](references/service-domain-examples.md) for service, domain model, domain mapper, parameter object, and repository-boundary code.
- Read [error handling examples](references/error-handling-examples.md) when adding or changing a caller-visible failure: an error catalog constant, a custom exception, a handler method, or a validation response shape. It also carries the full error contract.
- Read [infrastructure examples](references/infrastructure-examples.md) when deciding package placement or changing method validation, custom exceptions, configuration properties, infrastructure beans, idempotency placement, scheduled execution, or code that resembles a listed anti-pattern.

Treat the illustrated decisions and accompanying rules as normative, but do not assume omitted
members or configuration are complete. Do not load a reference for unrelated work.

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
guessing. This skill owns it. [The template asset](assets/project-profile-template.md) lists every
entry, its allowed values, its decision token, and the skill that owns it; copy it rather than
retyping it, and read [filling the project profile](references/project-profile-template.md) for how
each entry is settled.

**Do not write production code until the profile exists and records every decision the task
depends on.** This is a gate, not a preference. Without it each feature silently picks its own
database, service shape, accessor style, or contract direction, and the result is a codebase that
disagrees with itself in ways no review catches until much later.

### Decision tokens

Not every unrecorded decision blocks the same way, and treating them alike either stalls trivial
work or invents architecture. This skill owns the vocabulary; every skill and every template in this
set uses exactly these three tokens and no synonym.

| Token | Who settles it | Does it block? |
| --- | --- | --- |
| `ASK` | The user, and only the user | **Yes**, unless the template row records a fallback. Stop and ask. There is no defensible default, and a wrong answer is expensive to reverse. |
| `RESOLVE` | The agent, by looking the answer up and recording it | **No.** Resolve it, record it, and state in the handoff what was chosen and why, so the user overrides once instead of being asked every time. |
| `UNDECIDED` | Deferred on purpose | **No**, unless the current task touches it. Record what will force the decision. |

**Fallbacks live in one place: the `Fallback` column of the template.** Some `ASK` rows have a
sanctioned safe answer — concrete service classes, no Lombok, Swagger UI never exposed, server-side
retry only. For those, and only those, an unanswered decision does not block: apply the fallback,
write it into the profile as the value, and **state in the handoff that a fallback was applied**, so
the user overrides once instead of being asked every time. An `ASK` row with an empty fallback
blocks, with no exception.

No skill may introduce a fallback in its own prose. If a rule elsewhere in this set reads like a
default for a profile decision, the template is authoritative and that prose is the defect to fix.
This is what keeps three different agents from reaching three different answers on the same empty
repository.

A decision is `ASK` when nothing in the repository or the ecosystem points to one answer over
another: the build tool, the database engine, the migration tool, whether a contract document
exists, the authoring direction, the service interface convention, the entity accessor style. A
decision is `RESOLVE` when a correct answer exists and only needs looking up: the current supported
release of a framework, a plugin, or a tool.

**When a `RESOLVE` cannot be completed** — no network access, no registry, an ambiguous result —
record `UNDECIDED` with the reason and say so in the handoff. Never write a version number, a
coordinate, or any other value from memory into the profile or a build file. A remembered version is
a guess wearing a specific-looking number, and it is the one failure mode this whole mechanism
exists to prevent.

The template marks every row with its token. `build-and-dependencies` owns which build and version
decisions carry which token; do not reclassify one here.

### Order of work on every task

1. Read `docs/project-profile.md`. If every decision the task needs is recorded, implement.
2. If the file is missing, create it from [the template asset](assets/project-profile-template.md). If entries are missing, identify exactly which.
3. Fill what the repository already proves — a declared dependency, an applied migration, an existing package layout, a configured datasource.
4. Complete every `RESOLVE` the task touches, without asking.
5. Apply the template's `Fallback` value for every unresolved `ASK` row that has one, record it, and note it in the handoff.
6. **Ask the user, in one message, for every remaining `ASK` with no fallback**, offering the template's allowed values so each answer is one word. Do not ask one question per skill, and do not ask again for something already recorded.
7. Write the answers into the profile, then implement.

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

## Spring Boot 3 and 4

Both generations are supported, and `docs/project-profile.md` records which one applies. The
architecture in this skill is identical on both: controllers, TOs, services, domain models, mappers,
the transaction boundary, and the error contract do not change. Four things around them do.

| Concern | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| JSON library | Jackson 2 (`com.fasterxml.jackson`) | Jackson 3 (`tools.jackson`), except `jackson-annotations` |
| Replacing the mapper bean | define an `ObjectMapper` bean | define a `JsonMapper` bean; an `ObjectMapper` bean no longer replaces it |
| Custom serializer registration | `@JsonComponent` | `@JacksonComponent` |
| Customizing HTTP message converters | a `HttpMessageConverters` bean or contributed converter beans | `ServerHttpMessageConvertersCustomizer`; the Boot type is deprecated and contributed converter beans are no longer picked up |
| Declarative retry | Spring Retry | `org.springframework.core.retry` in the framework |

`build-and-dependencies` owns the coordinates in
[generation differences](../build-and-dependencies/references/generation-differences.md); do not
restate them here.

One Spring Boot 4 default deserves attention because it can change the public contract without a code
change: **every Jackson module on the classpath is registered automatically**, where Spring Boot 3
registered only well-known ones. A module arriving transitively can alter how a date, an optional, or
a domain type serializes, which is a contract change under `rest-api-contract` even though no
controller was touched. Assert the serialized shape of every response TO in tests, and set
`spring.jackson.find-and-add-modules=false` when the project wants registration to be explicit.

`ProblemDetail`, Bean Validation, method validation, and `@RestController` behave the same on both
generations. Nullability annotations in signatures follow `modern-java-21`.

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

## REST controllers and transport objects

Keep REST controllers thin. A controller must not query a repository, mutate an entity, implement a
business rule, manage a transaction, catch a generic exception, or prepare a server-rendered view.

The rules that decide most reviews:

- **One service per handler, at the level the operation belongs to.** An operation confined to one aggregate calls that aggregate service directly; one that reads or writes more than one aggregate, publishes an event, or must order effects calls the application service that owns the use case. A handler calling two services is doing coordination in the wrong place. The controller may inject both levels; a single handler may not mix them.
- **No entity crosses the HTTP boundary**, in either direction, and no request or response TO enters the service layer.
- **The base path is declared once**, as a constant. Where each route constant lives depends on the authoring direction the profile records: on the controller under code-first, in `ApiPaths` under contract-first, because generated interfaces already carry the mapping.
- **Each shared numeric bound is one compile-time constant**, referenced by every annotation that enforces it. Never a repeated literal.
- **One mapper per concept, both directions.** `UserRestMapper` owns request-side and response-side mapping; splitting it into `UserRequestMapper` and `UserResponseMapper` buys nothing. Use MapStruct with `unmappedTargetPolicy = ReportingPolicy.ERROR`.
- **`rest-api-contract` judges whether a change is breaking**, and owns versioning and deprecation. Do not introduce, raise, or retire a version here.

Read [REST boundary rules](references/rest-boundary-rules.md) before changing a controller, a TO, or
a mapper, and [REST API examples](references/rest-api-examples.md) for the code.

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

The service layer has two levels, and the split exists so that no service depends on another service
at the same level. That is what keeps transaction boundaries findable and prevents cyclic service
graphs.

| Level | Package | Owns | Never holds |
| --- | --- | --- | --- |
| `<Aggregate>Service` | `service` | One aggregate root: its repositories, its invariants, every write to it | A repository of another aggregate, or any other service |
| `<Capability>ApplicationService` | `applicationservice` | One coherent use case group: the transaction boundary and the order of calls | A repository, an entity, or another application service |

**The transaction boundary is the highest service the use case enters.** When an application service
exists it opens the transaction and the aggregate services it calls join it, so the whole use case
commits or rolls back together. When the operation stays inside one aggregate and no application
service exists, that aggregate service is the boundary and opens its own transaction. Both levels
are therefore annotated `@Transactional` with the default propagation, which is what makes either
arrangement correct without changing a line. Do not create an application service purely to hold a
transaction, and do not weaken an aggregate service to `SUPPORTS` or `MANDATORY` to force one.

Determine the aggregate by **lifecycle ownership**, not by table count and not by the reference
graph: a row belongs to the aggregate when it cannot exist without the root and the root is what
creates and deletes it.

An application service exists only where coordination exists. A feature whose every operation stays
inside one aggregate needs none, and adding an empty one is scaffolding. A method that only forwards
to one aggregate service is how this class turns into a facade over the whole application.

The rule most often got wrong: **an external effect published after commit is not guaranteed to
happen.** After-commit delivery guarantees the effect never fires for rolled-back work, and nothing
more. Where losing it is unacceptable, the profile must record an outbox or a broker-native
transaction instead.

Read [service layer rules](references/service-layer-rules.md) before creating or changing a service,
and [service and domain examples](references/service-domain-examples.md) for the code. This skill
decides only which method is transactional; `spring-data-jpa` owns what those settings mean.

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

Three rules carry the weight:

- **The `type` URI is the only machine-readable error identifier in the body.** Never add a parallel `code`, `errorCode`, or `errorId`; two identifiers for one condition guarantee that clients branch on the wrong one. `project-naming-conventions` owns the URI form.
- **Every caller-visible failure is declared once**, in a single project-owned error catalog carrying the status, the `type` URI, the title, the detail, and the internal code used in logs, events, and metrics. One declaration is what stops the public type and the internal code from drifting apart.
- **`correlationId` is the one permitted extension member.** Keep `traceId`, `spanId`, stack traces, exception class names, provider messages, internal hostnames, SQL, internal endpoints, credentials, and personal data out of the body entirely.

Read [error handling examples](references/error-handling-examples.md) for the catalog, the
`ProblemDetail` handler, the catch-all rules, and exception naming and placement.

## Outbound calls

Every call to another system is bounded, translated, and retried in exactly one layer. The three
rules that matter most, because their defaults are unsafe:

- **No outbound call without an explicit connection and read timeout.** Several widely used clients default to no read timeout at all, so one unresponsive dependency exhausts the thread pool and kills the application.
- **Retry lives in one layer only.** A client library, an adapter, a gateway, and a scheduler each retrying three times is twenty-seven calls to a system that is already failing.
- **Failure is translated at the adapter boundary.** No client library exception type, status object, or SDK response reaches a service or a controller.

Read [outbound call rules](references/outbound-call-rules.md) before adding or changing any client.
`observability-and-logging` owns what an outbound call must emit, and `application-security` owns
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

`observability-and-logging` owns every logging, metric, tracing, and health rule, including levels,
placement, correlation context, and tag cardinality. This skill owns only where instrumentation sits
in the layers: a service records the operation, an adapter records the outbound call, and a
controller records nothing beyond what the framework already emits. Do not infer a level, a meter
name, or a cardinality limit from this skill.

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
- [ ] The transaction boundary is the highest service the use case enters: the application service when one exists, otherwise the aggregate service. No aggregate service overrides propagation or isolation to escape it.
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
