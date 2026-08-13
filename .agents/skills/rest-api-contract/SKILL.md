---
name: rest-api-contract
description: Public API contract ownership and evolution for Java 21+ Spring Boot REST APIs. Use when creating or changing a public endpoint, request or response shape, status code, header, enum value, or error contract; when deciding whether a change is breaking; when versioning, deprecating, or retiring an API; and when producing, reviewing, or publishing an OpenAPI document. Covers projects with an OpenAPI document, code-first or contract-first, and projects with none. Excludes GraphQL, gRPC, asynchronous event contracts, and gateway configuration.
---

# REST API Contract

The contract is the API's public interface: the paths, payloads, statuses, headers, and errors a
consumer builds against. Once a consumer depends on it, changing it is a deployment of someone
else's software. Treat the contract as the deliverable and the controller as its implementation, not
the other way round.

A contract exists whether or not it is written down. A project with no OpenAPI document still has
one, in the form of what consumers observe and rely on; it simply has no artefact to review, diff,
or verify against.

## Coordination with other skills

| Skill | Treat as owner of |
| --- | --- |
| `project-naming-conventions` | The names themselves: paths, fields, enum values, `operationId`, schema names, problem type URIs |
| `spring-boot-patterns` | TO shape, thin controllers, `ProblemDetail`, validation, pagination, the error catalog |
| `application-security` | The authentication and authorization model the contract describes |
| `build-and-dependencies` | The springdoc or generator dependency, plugin, and build wiring |
| `spring-boot-testing` | Test levels; this skill owns what about the contract must be asserted |
| `spring-boot-code-review` | Review scope, evidence, severity, and reporting |

This skill owns the document, its quality, and its evolution. It does not rename anything, redesign
a TO, or change an error contract; it decides how those appear in the contract and whether a change
to them is safe to ship.

## Decide whether the contract has a document

The first decision is not code-first versus contract-first. It is whether this service publishes a
machine-readable contract document at all.

| Decision | Value | What applies |
| --- | --- | --- |
| Contract document | OpenAPI | This whole skill |
| Contract document | none | Compatibility, versioning, deprecation, and ownership only |

**Record it in `docs/project-profile.md`. When it is not recorded, ask the user before writing the
first endpoint.** Do not assume a document exists because springdoc is on the classpath, and do not
add one because an example shows it.

The rules that hold in both cases are the majority of this skill: what makes a change breaking, the
request/response asymmetry, required versus nullable versus absent, versioning, deprecation and
sunset, and who approves a contract change. Those are properties of the API, not of the document.

What is lost without a document is verification. There is no artefact to diff in a pull request, no
drift gate, and no generated client. **Breaking-change judgement becomes entirely a review
responsibility, with no automated check behind it.** Choose `none` deliberately, knowing that; it is
reasonable for an internal service with one known consumer and expensive for anything else.

When the value is `none`, read [compatibility and evolution](references/compatibility-and-evolution.md)
and stop. The other two references describe an artefact this project does not produce.

## Choose the authoring direction

This applies only when the contract document is OpenAPI. Two directions are supported. They are not
interchangeable, and a project uses exactly one per deployable service.

| | Code-first | Contract-first |
| --- | --- | --- |
| Source of truth | Java controllers and TOs | The OpenAPI document |
| Document produced by | springdoc, at runtime or build time | Written by hand, reviewed, then generating code |
| Strength | Nothing to keep in sync; fast for a single team | Contract reviewable before implementation; consumers can start immediately |
| Cost | Contract cannot be reviewed before the code exists | Generated types collide with project naming unless configured |
| Fits | One service, one team owning both sides | Multiple services, external consumers, parallel teams |

**Record the choice in `docs/project-profile.md`. When it is not recorded, ask the user before
writing the first endpoint.** Do not infer it from a dependency, and do not switch direction as a
side effect of another task; switching is its own project.

If the user has no preference, present the trade-off rather than choosing silently: code-first is
the lower-friction default for a single service whose consumers are in the same organization;
contract-first earns its cost when a second service, an external consumer, or a separate frontend
team needs the contract before the implementation exists.

The direction has two consequences that must be settled at the same time, not discovered later.

**Generated type names.** Under contract-first the generator produces model types whose names will
not match this project's `TO` suffix and `transferobject` package unless it is configured to.
[Contract-first generation](references/contract-first-generation.md) covers the configuration and
the alternatives.

**Where route constants live.** Under contract-first the generated interface carries the routes, so
a controller declares none, and the constants that tests and security matchers reference live in
`ApiPaths` and equal the document's Path Items. Under code-first the direction is reversed and each
controller owns its own route constant. `spring-boot-patterns` owns that rule; follow whichever the
project profile records, and never mix the two.

## Reference routing

- Read [compatibility and evolution](references/compatibility-and-evolution.md) when changing an existing endpoint, deciding whether a change is breaking, or versioning, deprecating, or retiring an API. This applies whatever the project profile records, including `none`.
- Read [OpenAPI document](references/openapi-document.md) only when the profile records an OpenAPI document, and the task produces, structures, or improves it.
- Read [contract-first generation](references/contract-first-generation.md) only when the profile records OpenAPI **and** contract-first.

## The contract is part of the change

With a document, a change to a public endpoint is not complete until the document reflects it. That
includes paths, methods, status codes, headers, request and response shapes, enum values,
required-ness, and every error condition the endpoint can produce.

- Every operation declares every status it can return, including the error statuses produced by the shared exception advice. An endpoint that documents only `200` is documenting a fiction.
- Error responses reference the shared problem schema and name the problem type URIs the endpoint can produce. `spring-boot-patterns` owns the error catalog; the contract exposes it.
- Security requirements appear per operation, not only globally, whenever an operation's requirement differs from the default.
- Do not document an endpoint that does not exist, and do not leave a removed endpoint in the document.

Without a document, the same completeness still applies, but it has to be established another way:
state in the change description every status, header, and field the endpoint produces, and prove
each one with a test. A status that no test covers and no document declares is not part of the
contract by accident — it is an undocumented behaviour a consumer will discover in production.

## Required, nullable, and absent are three different things

This is the most common source of silent incompatibility, because the three are routinely treated as
one.

| Concept | Meaning in the contract | Consumer must handle |
| --- | --- | --- |
| Required | The key is always present in the payload | Nothing; may rely on it |
| Optional | The key may be absent entirely | Absence, distinctly from null |
| Nullable | The key is present and its value may be `null` | A null value |

Rules:

- Decide required-ness deliberately for every field, in both directions. A field that is required in a response is a promise; a field that is required in a request is a demand.
- Never make a field both optional and nullable unless the two states genuinely mean different things, and then say what each means.
- Keep the contract aligned with what the code actually does: what Jackson serializes, what Bean Validation enforces, and what the mapper produces. A field documented as required that the code can leave null is a defect in the contract, not a documentation nicety.
- With a document, express nullability the way the project's OpenAPI version does, and keep one style throughout; [OpenAPI document](references/openapi-document.md) covers which applies. Without one, the three states must still be decided per field and pinned by tests, because nothing else records them.

## Keep the contract honest

A document that has drifted from the implementation is worse than no document, because consumers
trust it. This section applies when the profile records a document; with `none`, the equivalent
protection is the test suite and the review, and there is no automated gate.

- **Code-first:** generate the document in the build, compare it against the committed copy, and fail the build on an unexplained difference. A regenerated document that differs is either an intended contract change to be reviewed, or a bug.
- **Contract-first:** validate that the implementation satisfies the committed document, and never hand-edit generated code.
- Commit the document either way. A document that exists only at runtime cannot be diffed in a pull request, and a contract change that cannot be seen in review will not be reviewed.
- Treat a diff in the document as the most important diff in the change. It is the only part a consumer sees.

## Document quality

This section applies when the profile records a document. Consumers read it, not the controller, and
the optional-looking fields are what make it usable:

- Every operation has a `summary` written for a caller, not restating the method name.
- Every non-obvious field has a `description` that says what it means, not what type it is.
- Every request body and every non-trivial response has at least one realistic example. Examples carry no real data, no production identifiers, and no personal data.
- Constraints that exist in code — lengths, ranges, patterns, allowed values — appear in the schema too, or callers discover them through `400` responses.
- Pagination, sorting, and filtering parameters are documented with their defaults and bounds.

## Contract ownership

- Name an owner for the contract. A contract everyone can change is a contract nobody maintains. This matters more, not less, when there is no document, because there is no artefact to notice a change in.
- A breaking change requires confirmation from known consumers before it merges, not after.
- Maintain a list of known consumers in the project profile. With no document, that list is the only mechanism by which a breaking change reaches the people it affects.
- With a document, publish it where consumers can reach it and record that location in the project profile.
- Interactive documentation such as Swagger UI is a development and internal tool. Record in the project profile which environments expose it, if any; `never` is a valid answer and the safe default. Wherever it is exposed, `application-security` owns how it is protected, exactly as it does for actuator endpoints.

## Anti-patterns

Reject:

- an endpoint shipped without its contract change;
- an operation documenting only its success status;
- a contract that describes a shape the code cannot produce;
- a document assumed to exist because springdoc is on the classpath, or introduced because an example showed one;
- `none` chosen by omission rather than as a recorded decision;
- required-ness or nullability copied from an adjacent field rather than decided;
- a new API version introduced for cleanup, naming, or aesthetics;
- deprecation with no sunset date and no successor;
- a breaking change shipped inside a minor release because "no one uses that field";
- hand-edited generated code, or generated code committed as if hand-written;
- mixing code-first and contract-first in one service;
- Swagger UI exposed in production by default.

## Completion checklist

- [ ] The contract-document decision, and the authoring direction when there is a document, are recorded in the project profile and were followed.
- [ ] The contract, in whatever form the project records it, reflects every path, status, header, field, and error the change introduced or altered.
- [ ] Required-ness and nullability were decided per field and match what the code does.
- [ ] Every operation lists its error statuses and their problem types.
- [ ] With a document: the committed copy matches the implementation and the build proves it. Without one: every status, header, and field the change introduced is covered by a test.
- [ ] With a document: summaries, descriptions, examples, and constraints are present and free of real data.
- [ ] Any breaking change was identified as such and handled through versioning or deprecation, not shipped silently.

## Primary guidance

- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- [springdoc-openapi](https://springdoc.org/)
- [OpenAPI Generator: Spring](https://openapi-generator.tech/docs/generators/spring/)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [RFC 8594: The Sunset HTTP Header Field](https://www.rfc-editor.org/rfc/rfc8594)
