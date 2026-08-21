# REST boundary rules

Use this reference whenever a REST controller, a request or response TO, or a REST mapper is created
or changed. Apply every rule from `../SKILL.md`. Code for everything here is in
[REST API examples](rest-api-examples.md).

## Contents

1. [Controllers](#controllers)
2. [Route constants and the base path](#route-constants-and-the-base-path)
3. [Validation at the boundary](#validation-at-the-boundary)
4. [Request and response TOs](#request-and-response-tos)
5. [Mappers](#mappers)

## Controllers

`../SKILL.md` states the boundary rules that decide most reviews. This file carries the detail
behind them and the rules that need more than one line; it does not repeat them.

- Use `@RestController`; do not use a view-oriented `@Controller` for REST endpoints.

## Route constants and the base path

- Declare the API base path exactly once in Java, as a constant such as `ApiPaths.API_V1`. The name carries a version only when `rest-api-contract` records path-based versioning; with a header, query-parameter, or media-type strategy the constant is the unversioned base and the version never appears in a route literal. Where each controller's own route constant lives depends on the authoring direction recorded in the project profile:
  - **Code-first:** declare it as a `public static final String` on the controller, so tests, security matchers, and `Location` construction reuse it instead of repeating the literal.
  - **Contract-first:** routes come from the generated API interface, so a controller has no route constant to expose. Declare the route constants in `ApiPaths` beside the base path, keep them equal to the document's Path Items, and have tests and security matchers reference those. Never repeat a literal, and never add a second `@RequestMapping` on the implementation.
- When the project publishes an OpenAPI document, the same prefix appears there only as `servers.url`, and Path Items stay resource-relative, such as `/users/{userId}`, so the version never reaches `operationId` or the handler method name. `project-naming-conventions` owns that derivation, and it applies to handler method names whether or not a document exists.
- Use nouns in resource paths and correct HTTP methods and status codes. Define or preserve supported media types, and return serialized bodies rather than view names.

## Validation at the boundary

- Validate path, query, header, and body input at the boundary, including collection bounds, string lengths, numeric bounds, and pagination limits for untrusted input.
- Declare each shared numeric bound once as a compile-time constant, for example `PaginationConstraints.MAXIMUM_PAGE_SIZE`, and reference it from every annotation that enforces it at the REST boundary and on the service contract. Never repeat the literal in a second annotation or in Javadoc.
- Use built-in REST handler method validation and do not place `@Validated` on the controller. Every Spring Boot generation this skill set supports runs on Spring Framework 6.1 or later, where this is the default path. Only on a legacy branch below that does proxy-based controller method validation apply, and then `@Validated` goes at type level. Never place it on an individual handler method.
- When controller parameters can trigger both object and method validation, preserve Spring's standard handling or map both validation exception types into the same public error contract.
- For a synchronous operation creating an addressable resource, return `201 Created` with a server-owned `Location` URI. Do not force that combination on a POST without resource-creation semantics.
- Return typed response models, not entities, `Map<String, Object>`, or `ResponseEntity<?>`.
- A change to a public endpoint is not complete until the project's contract record reflects it, and compatibility in field names, enum values, requiredness, null behavior, statuses, and error shapes is `rest-api-contract`'s judgement, not this skill's.

Read the controller example in [REST API examples](rest-api-examples.md).

## Request and response TOs

TO means transport object in this skill. Use records for immutable request and response TOs when compatible with the serializer and project conventions.

- Never accept or return a JPA entity as an HTTP/message TO.
- Never call a repository from a handler.
- Do not pass request or response TOs into the service layer.
- Map service results from `UserDomain` to `UserTO` in the REST mapper.
- Map a request TO to a focused domain input only when the service parameter-object rule justifies that input.

## Mappers

- Keep both mapping directions for one concept in that concept's single `<Concept>RestMapper`. Request-side and response-side mapping are two methods on one type, never two types: `UserRestMapper` owns both, and `UserRequestMapper` beside `UserResponseMapper` is a split with no benefit. The same applies to `<Concept>DomainMapper`.
- Split a concept's mapper only for a reason recorded in the change, such as a generated mapper the project does not own or a mapping that genuinely requires different collaborators. Volume alone is not a reason; a mapper that has grown large is usually a signal that a mapping carries logic that belongs in domain or service code.
- Keep transport validation on request TOs and business invariants in domain/service code.
- Do not put repositories or services in TOs or mappers.
- Normalize only when the contract permits it; do not silently change user data.
- Model PATCH semantics explicitly so absent, clear, and set are not confused.
- Use MapStruct for all structural REST and domain mapping with `unmappedTargetPolicy = ReportingPolicy.ERROR`. When part of a mapping is non-trivial, keep MapStruct for the structural portion and implement the rest through focused default methods or collaborators; replace the whole mapper by hand only when MapStruct is genuinely unsuitable, with the reason documented.
- Obtain a stateless, dependency-free mapper through its static `INSTANCE = Mappers.getMapper(...)` member. Do not register or inject it as a Spring bean unless it needs a documented container-managed capability.

Read the TO and REST mapper examples in [REST API examples](rest-api-examples.md).
