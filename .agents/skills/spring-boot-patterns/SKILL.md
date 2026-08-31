---
name: spring-boot-patterns
description: Production Spring Boot patterns for Java 21+ backend REST APIs, REST controllers, services, domain models, validation, transport objects (TOs), mapping, persistence, exception handling, configuration, security boundaries, observability, and feature packaging. Excludes server-side page rendering and UI views. Use for every new Spring Boot REST feature or modification to controllers, services, configuration, scheduled jobs, listeners, or API contracts.
---

# Spring Boot Patterns Skill

Implement vertical, tested features using the project's supported Spring Boot version. Preserve existing contracts, but do not copy legacy architecture or anti-patterns into new code.

## Coordination with other skills

This skill owns the Spring Boot boundaries: controllers, TOs, services and their two levels, domain
models, mappers, validation, the error contract, configuration design, package responsibilities, and
where the transaction boundary sits.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what, and
it carries the precedence order for a genuine conflict. Read it there rather than from a copy in
this file. The seams this skill crosses most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| The project profile | the meaning of the rows this skill owns: service convention, aggregate roots, reliable delivery, resilience, timeouts, API base path, error catalog | `project-decision-profile` owns the file, the tokens, and whether a missing row blocks |
| Transactions | which method carries the annotation, and where the boundary sits | `spring-data-jpa` owns what the settings mean |
| Locking | which layer the retry annotation sits on | `spring-data-jpa` owns `@Version`, lock modes, and the retry mechanism |
| Repositories | where the boundary sits and what may cross it | `spring-data-jpa` owns repository and query design |
| The public contract | the TO, the `ProblemDetail`, the error catalog | `rest-api-contract` owns whether a change to them is breaking |
| Instrumentation | where it sits in the layers | `observability-and-logging` owns what is emitted and at what level |

Do not restate or fork an owner's rules here. Report an unresolved conflict instead of inventing a
second standard; when it is genuine and cannot wait, apply the precedence order in
[the ownership map](../_core/OWNERSHIP.md) and say in the handoff which rule was set aside.

## Reference routing

This body carries the decisions and the rules that decide most reviews. Detail sits in references —
**rule references** are normative and complete, **example references** show the rules applied. Read
only what the task needs, and do not load one for unrelated work.

| Read | When |
| --- | --- |
| [REST boundary rules](references/rest-boundary-rules.md) *(rules)* | Creating or changing a controller, a request/response TO, or a REST mapper |
| [Service layer rules](references/service-layer-rules.md) *(rules)* | Creating or changing a service, choosing its level, or producing an effect outside a transaction |
| [Outbound call rules](references/outbound-call-rules.md) *(rules)* | Adding or changing any call to another system: HTTP client, message producer, provider SDK |
| [REST API examples](references/rest-api-examples.md) | Controller, TO, and REST mapper code |
| [Service and domain examples](references/service-domain-examples.md) | Service, domain model, domain mapper, parameter object, repository-boundary code |
| [Error handling examples](references/error-handling-examples.md) | Adding or changing a caller-visible failure: a catalog constant, a custom exception, a handler, a validation response |
| [Infrastructure examples](references/infrastructure-examples.md) | Package placement, method validation, custom exceptions, configuration properties, infrastructure beans, idempotency placement, scheduled execution, or code resembling a listed anti-pattern |

Treat the illustrated decisions and the accompanying rules as normative, but do not assume omitted
members or configuration are complete.

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

`project-decision-profile` owns `docs/project-profile.md`, the `ASK` / `RESOLVE` / `UNDECIDED`
tokens, the gate that precedes production code, and the order of work for filling a row. Read it
whenever a decision this skill's work depends on is not recorded.

The rows this skill owns the *meaning* of are the architectural ones: service interface convention,
aggregate roots and their tables, the reliable-delivery mechanism for external effects, the
resilience library, the outbound timeout budget, the API base path, and the error catalog type. Each
is decided here and recorded there.

## Rules before coding

1. Confirm the project profile covers every decision the change touches, per the section above and the process `project-decision-profile` owns.
2. Inspect `pom.xml` or Gradle files, the configured Java and Spring Boot versions, existing package layout, tests, configuration, migrations, security, and API error format.
3. Read the full call path affected by the change: controller/listener, service, domain, persistence, cache, and external adapters.
4. Define acceptance cases, invalid input, missing data, conflicts, authorization, dependency failures, and transaction effects.
5. Design the smallest cohesive change. Do not perform unrelated modernization.
6. Implement production code and tests together.

## Spring Boot 3 and 4

Both generations are supported and **the architecture in this skill is identical on both**:
controllers, TOs, services, domain models, mappers, the transaction boundary, the error contract,
`ProblemDetail`, and validation do not change. What differs is what a few surrounding things are
called — the JSON library, the mapper bean, the serializer annotation, the message-converter
customizer, the retry engine — and `build-and-dependencies` owns that catalogue in
[generation differences](../build-and-dependencies/references/generation-differences.md). Read the
row there for the generation the profile records.

One Spring Boot 4 default belongs here, because it can change the public contract with no code
change: **every Jackson module on the classpath is registered automatically**, where Spring Boot 3
registered only well-known ones. A module arriving transitively can alter how a date, an optional, or
a domain type serializes — a contract change under `rest-api-contract` though no controller was
touched. Assert the serialized shape of every response TO in tests, and set
`spring.jackson.find-and-add-modules=false` when the project wants registration to be explicit.

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

Use the established terminology consistently: `UserCreateTO`/`UserTO` at the REST boundary,
`UserManagementApplicationService` in `applicationservice`, `UserService` in `service` (one aggregate
root each), `UserDomain` and focused parameter objects at the domain/service boundary, `UserEntity`
in `entity`, `UserSummaryProjection` from a repository, `UserRestMapper` and `UserDomainMapper` for
the two mapping directions. `project-naming-conventions` owns the forms; read the package layout in
[infrastructure examples](references/infrastructure-examples.md) when placing new types.

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

Services return domain models, controllers map them to response TOs, repositories work with entities.

- Do not add JPA, HTTP, JSON, controller, repository, or Spring infrastructure concerns to a domain model, and do not expose an entity outside the service/persistence boundary.
- Keep the domain model immutable when practical, and put business invariants in it when they belong to the represented concept.
- Do not create a domain type that merely aliases a TO. The two may look similar, belong to different boundaries, and evolve independently.
- Keep a domain-owned enum beside the related domain types, and a transport-only or persistence-only enum inside its owning boundary package. Never collect unrelated enums in a generic package.

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

- Use `@Valid` for nested objects, and method validation when the service can be called outside the REST boundary.
- Create a custom constraint only for reusable structural validation. Database-dependent and business validation belong to a service or domain policy.
- Prefer separate request TOs per operation over Bean Validation groups, which make one type's contract depend on the caller.
- Constraint messages are human-readable text, never the machine-readable contract; clients branch on the problem type. Keep them free of implementation class names and provider details.

[REST boundary rules](references/rest-boundary-rules.md) carries the validation-group and
message-localization rules in full; read it before using either. The method-validation example is in
[infrastructure examples](references/infrastructure-examples.md).

## Error handling

Use the project's existing error contract. For a new API on a supported Spring version, use RFC 9457
`ProblemDetail`.

Three rules carry the weight:

- **The `type` URI is the only machine-readable error identifier in the body.** Never add a parallel `code`, `errorCode`, or `errorId`; two identifiers for one condition guarantee that clients branch on the wrong one. `project-naming-conventions` owns the URI form.
- **Every caller-visible failure is declared once**, in a single project-owned error catalog carrying the status, the `type` URI, the title, the detail, and the internal code used in logs, events, and metrics. One declaration is what stops the public type and the internal code from drifting apart.
- **`correlationId` is the one permitted extension member.** Keep `traceId`, `spanId`, stack traces, exception class names, provider messages, internal hostnames, SQL, internal endpoints, credentials, and personal data out of the body entirely.
- **The advice translates framework exceptions too, not only project ones.** Concurrency is the case that matters: `spring-data-jpa` absorbs contention with a retry annotation and lets the failure surface from the transaction interceptor, so what reaches the advice is a Spring `OptimisticLockingFailureException` or `PessimisticLockingFailureException`. Without handlers for those two parent types, every real conflict becomes a `500` while the code reads as though the contract were implemented.

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

## Security and observability boundaries

Both are owned elsewhere; this skill owns only where they sit in the layers.

- `application-security` owns authentication, authorization, CSRF, CORS, confidentiality, and security verification. Preserve the service and repository boundaries defined here while applying those controls, and never weaken production security to make a test pass.
- `observability-and-logging` owns every logging, metric, tracing, and health rule. Placement in the layers is the part this skill decides: a service records the operation, an adapter records the outbound call, and a controller records nothing beyond what the framework already emits. Do not infer a level, a meter name, or a cardinality limit from this skill.

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

Six shapes account for most of what this skill rejects, and each one **compiles, passes its tests,
and survives review** unless it is recognised by name:

- a controller handler calling two services, or an application service method that only forwards to one aggregate service;
- a repository injected into an application service, or one aggregate service depending on another;
- a JPA association crossing an aggregate boundary;
- an external effect fired inside the transaction rather than after commit, or after commit where the profile records that losing it is unacceptable;
- an outbound client with no read timeout, or a provider exception reaching a service or controller;
- a second machine-readable error identifier beside the RFC 9457 `type`;
- an exception advice that handles only project exception types, so contention arrives from the framework and the catch-all reports a routine `409` or `503` condition as a `500`.

[Infrastructure examples](references/infrastructure-examples.md) carries the full rejected list —
boundary, structure, error-contract, and process — with the code. Read it when reviewing or replacing
suspicious existing code.

## Completion checklist

- [ ] The profile gate `project-decision-profile` owns was satisfied for every decision this change relied on.
- [ ] Controller/listener is a thin transport boundary, and each handler calls one service at the level the operation belongs to.
- [ ] Each business rule sits at the level that owns it.
- [ ] The transaction boundary is the highest service the use case enters, and no aggregate service overrides propagation or isolation to escape it.
- [ ] No application service holds a repository or a pass-through method; no aggregate service holds another service.
- [ ] Each external effect uses the delivery mechanism the profile records, and failed delivery is logged rather than dropped.
- [ ] Every outbound client sets both timeouts, and retry exists in exactly one layer.
- [ ] TOs are explicit, validated, controller-owned, and separate from domain models and entities.
- [ ] Service signatures satisfy the size rule `modern-java-21` owns.
- [ ] Mappers preserve the TO–Domain–Entity boundaries; MapStruct with `ReportingPolicy.ERROR`; one REST and one domain mapper per concept.
- [ ] Projections, Specifications, enums, exceptions, handlers, and configuration types sit in their owning packages without empty scaffolding.
- [ ] Error responses are stable and safe, the `type` URI is their only machine-readable identifier, and every failure comes from the single catalog.
- [ ] The advice maps the framework contention types, so an exhausted retry returns `409` and a lock timeout returns `503` with `Retry-After`, each logged at the level its expectedness deserves.
- [ ] Shared numeric bounds are declared once and referenced.
- [ ] Configuration is type-safe, externalized, and validated.
- [ ] `spring-boot-testing` and `modern-java-21` were applied, and the gates and suites pass.
