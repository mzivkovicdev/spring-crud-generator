# Review lenses

Use these lenses to trace the changed behavior. Apply only the sections relevant to the review scope and use the named owner skill for exact implementation rules.

## Contents

1. [Change design and blast radius](#change-design-and-blast-radius)
2. [REST contract and boundary](#rest-contract-and-boundary)
3. [Service, domain, and mapping](#service-domain-and-mapping)
4. [Spring proxies and advice](#spring-proxies-and-advice)
5. [Transactions and concurrency](#transactions-and-concurrency)
6. [Persistence and database behavior](#persistence-and-database-behavior)
7. [Security and confidentiality](#security-and-confidentiality)
8. [External systems, Redis, messaging, and jobs](#external-systems-redis-messaging-and-jobs)
9. [Configuration, observability, and operations](#configuration-observability-and-operations)
10. [Build, dependencies, and delivery](#build-dependencies-and-delivery)
11. [Tests and review completeness](#tests-and-review-completeness)

## Change design and blast radius

Reconstruct the intended execution path before reviewing individual lines.

- Identify synchronous, asynchronous, scheduled, administrative, and retry entry points.
- Trace the changed data from input through validation, service/domain decisions, persistence or external calls, mapping, and output.
- Check callers and consumers for changed method signatures, nullability, ordering, exceptions, side effects, and timing.
- Determine whether the change is additive, backward-compatible, intentionally breaking, or accidentally incompatible.
- Inspect feature flags, configuration defaults, migrations, event schemas, and rollout order that can make code versions coexist.
- Check both success and failure ordering. Look for state committed before a later step can fail, success returned before work completes, and compensation assumed but not implemented.
- Check whether a small-looking change creates unbounded work, repeated I/O, elevated cloud cost, or a new operational dependency.
- Review deletions as carefully as additions. Verify that removed validation, authorization, constraints, tests, metrics, or cleanup had no remaining responsibility.

Do not require a new abstraction merely because one could be introduced. Report design complexity only when it creates a concrete defect risk or contradicts an applicable owner skill.

## REST contract and boundary

Apply `spring-boot-patterns` and `application-security`.

Trace each changed REST endpoint as an external contract:

- Verify method, path, media type, status, headers, request and response TO shape, validation, and `ProblemDetail` behavior, including that the RFC 9457 `type` URI is the only machine-readable identifier in the body.
- Compare implementation, OpenAPI, examples, contract tests, clients, and gateway rules when they exist.
- Check compatibility of field names, requiredness, defaults, null versus absent values, enum values, numeric types, dates, pagination, sorting, and problem type URIs.
- Verify that a request TO remains at the REST boundary and that a response is mapped from a domain result rather than a JPA entity or provider model.
- Verify that `UserRestMapper`-style mapping does not hide business rules, database access, remote calls, authorization, or silent lossy conversion.
- Check that validation covers path, query, header, and body values and that bounds prevent oversized collections, pages, strings, files, and expensive filters.
- Trace malformed input, unknown fields, unsupported media types, duplicate requests, missing resources, conflicts, and downstream failures to the intended public error contract.
- Check cache and proxy behavior when status, headers, ETags, conditional requests, or sensitive responses change.

Do not flag a controller merely for being `public` or lacking Javadoc. Apply the selective Javadoc policy from `modern-java-21`.

## Service, domain, and mapping

Apply `spring-boot-patterns` for the normative TO–Domain–Entity architecture.

- Verify that the controller delegates and maps rather than implementing business or persistence logic.
- Verify that each service sits at the level `spring-boot-patterns` assigns it: an aggregate service holding only its own aggregate's repositories, an application service holding only services. A repository in an application service, or a service in an aggregate service, is a blocking finding.
- Verify that every controller handler calls one service and that the level matches the operation: single-aggregate operations reach the aggregate service directly, multi-aggregate ones go through an application service. A handler calling two services, or an application service method that only forwards, is a blocking finding — the first puts coordination in the controller, the second grows a facade.
- Verify that the use case's transaction boundary is the application service, and that no aggregate service widens or escapes it with `REQUIRES_NEW`, `NOT_SUPPORTED`, or a custom isolation level without a recorded reason. An aggregate service that writes several repositories and carries no `@Transactional` at all is a blocking finding: called without a caller-supplied transaction, each write commits separately.
- Verify that an aggregate's own rule is enforced in that aggregate's service rather than in its callers, and that the same rule is not enforced at both levels.
- Verify that the service accepts explicit parameters by default and uses a focused parameter object only when justified by the service signature policy.
- Verify that a service at either level returns a domain result such as `UserDomain`, not a REST TO, JPA entity, persistence projection, SDK response, or generic map.
- Verify that the domain object remains independent of REST, serialization, JPA, repositories, and Spring infrastructure.
- Check whether `UserDomainMapper`-style mapping runs while all required persistence state is valid and available.
- Check every mapper for omitted fields, wrong direction, privilege-bearing fields, mutable collection leakage, accidental lazy loading, and silent normalization.
- Verify mapper technology and update structure against the complete rules in `spring-boot-patterns`;
  do not restate or weaken those rules in review guidance.
- Check partial-update semantics carefully. Distinguish absent, clear, and set operations and ensure unchanged server-owned fields survive.
- Check exception translation at the owning boundary and verify that causes, stable error semantics, and rollback behavior remain correct.

Do not impose a different mapper construction strategy from `spring-boot-patterns`. Do not rename Domain objects to View, DTO, command, or query terminology.

## Spring proxies and advice

Apply `spring-boot-patterns`, `application-security`, and the feature-specific owner skill. Review proxy behavior for `@Transactional`, `@Async`, cache annotations, retry annotations, method validation, method security, and custom aspects when present.

- Trace the actual call site and verify that the invocation crosses the configured proxy or woven boundary.
- Check self-invocation, method and class visibility, final methods or classes, annotation placement, bean ownership, proxy mode, initialization timing, and direct construction outside the container.
- Check combinations and ordering of transaction, retry, cache, async, validation, security, and custom advice for changed failure semantics or duplicated work.
- Verify that async return types surface failures and that callers do not mistake scheduling for successful completion.
- Verify that cache advice uses the intended key, condition, result, transaction timing, and invalidation path without re-defining cache policy owned elsewhere.
- Exercise proxy-dependent behavior through a Spring-managed bean at the appropriate integration boundary; a directly constructed unit test does not prove interception.

Do not prescribe AspectJ, self-injection, or another proxy workaround by default. Report the missing behavior and let the relevant owner skill and project architecture determine the fix.

## Transactions and concurrency

Apply `spring-boot-patterns` and `spring-data-jpa`.

- Locate the real proxy-reached transaction boundary and trace self-invocation, visibility, bean ownership, and asynchronous execution.
- Check whether reads or writes occur outside the intended transaction because mapping, callbacks, futures, events, or lazy access happen later.
- Check whether remote calls, message publication, large loops, blocking waits, or expensive computation hold a database transaction open.
- Identify read-modify-write races, lost updates, check-then-act uniqueness races, inconsistent lock ordering, and stale state after bulk operations.
- Check idempotency across retries, duplicate HTTP requests, redelivered messages, scheduled overlaps, and process restarts.
- Verify that retry scope includes the complete safe operation and does not repeat a non-idempotent side effect.
- Check commit-time failures, rollback rules, after-commit actions, outbox or equivalent consistency mechanisms when applicable.
- Verify that every external effect uses the delivery mechanism `docs/project-profile.md` records for it. An effect the business cannot afford to lose, delivered only from an after-commit listener, is a blocking finding: the commit has already succeeded, so the loss leaves no trace and no retry.
- Require pessimistic locking, stronger isolation, or a new consistency mechanism only when a concrete invariant and concurrency scenario justify it.

## Persistence and database behavior

Apply `spring-data-jpa`; do not substitute database-specific folklore for its database-neutral rules.

- Compare entity mappings, converters, migrations, constraints, indexes, defaults, precision, lengths, nullability, and identifier strategy.
- Trace association ownership, cascade, orphan removal, fetch plans, serialization, equality, logging, and mapper access.
- Inspect generated SQL or query construction for changed repository methods, Specifications, entity graphs, projections, native SQL, bulk DML, and pagination.
- Look for query-per-row behavior, accidental lazy traversal, unbounded reads, incorrect collection fetch pagination, cartesian multiplication, and needless full-entity loading.
- Verify deterministic ordering, maximum page size, count-query necessity, cursor semantics, and sort allowlists.
- Check tenant, ownership, soft-delete, status, and lifecycle predicates across result and count queries.
- Check sargability and index fit only against a credible high-volume access path. Account for equality, range, join, and sort order rather than requesting indexes by intuition.
- For performance claims, capture the expected data volume, generated SQL, query count, and representative plan when the environment permits.
- Check flush timing, stale managed entities after bulk DML, batch size, persistence-context growth, lock waits, and connection-pool demand.
- Verify rolling-deployment compatibility for schema changes against `sql-database-migration`: a mapping change with no migration, an edited applied migration, or a breaking change not split into expand, migrate, and contract phases is a blocking finding.
- On Spring Boot 4, verify that the migration tool arrives through its Spring Boot starter. The raw library alone leaves the build green and no migration executed, so this failure is invisible in every other check.

Treat absent representative plans as a verification gap unless the code itself proves an unbounded query, per-row query, invalid mapping, or other deterministic defect.

## Security and confidentiality

Apply `application-security` and load only its references relevant to the changed trust boundaries.

- Classify affected data and identify actors, subject identity, tenant, ownership, privileges, dangerous sinks, and external destinations.
- Trace function, object, property, and tenant authorization through service and persistence paths rather than stopping at controller annotations.
- Check server-owned fields, mass assignment, identifier substitution, replay, duplicate delivery, and administrative bypasses.
- Check data exposure through response TOs, errors, logs, metrics, traces, caches, events, files, exports, test fixtures, and provider payloads.
- Check injection and resource-exhaustion paths for SQL, URLs, redirects, files, archives, parsers, deserialization, expressions, headers, and regular expressions as applicable.
- Check secrets, credentials, cryptography, TLS, CORS, CSRF, management endpoints, API documentation, dependency changes, cloud permissions, and production defaults when touched.
- Verify both positive and negative security tests at the boundary capable of enforcing the control.

Do not paste secret values or sensitive payloads into a finding. Identify the secret type and source location, redact evidence, and follow the project incident process.

## External systems, Redis, messaging, and jobs

Apply `spring-boot-patterns` and the relevant `application-security` references.

For WebClient or another outbound client:

- Trace destination selection, authentication, request construction, timeout layers, connection pooling, response status handling, body limits, deserialization, cancellation, and resource cleanup.
- Check retry ownership across client libraries, service code, gateways, queues, and schedulers. Detect stacked retries and retry amplification.
- Check whether an event-loop thread blocks, a servlet request waits without a bound, or an async error is discarded.
- Verify failure translation and whether downstream 4xx, 429, 5xx, malformed responses, timeouts, and partial responses produce intended behavior.

For Redis:

- Trace key construction, tenant scope, serializer/schema compatibility, TTL, missing values, invalidation, stampede behavior, and unavailable behavior.
- Verify that cached representation and invalidation match the architecture established by the other project skills.
- Check whether sensitive data, authorization decisions, mutable values, or stale negative results are cached beyond their safe lifetime.

For AWS or another cloud provider:

- Trace client lifecycle, region and credentials providers, least privilege, resource identifiers, encryption, network boundaries, pagination, timeouts, retries, request cost, and error translation.
- Verify that caller-controlled input cannot select unauthorized accounts, buckets, keys, queues, topics, roles, URLs, or regions.

For messages and jobs:

- Trace schema compatibility, producer and consumer deployment order, duplicate and out-of-order delivery, poison messages, retry and dead-letter policy, acknowledgement timing, idempotency, and tenant context.
- Verify scheduler overlap, distributed execution, clock behavior, bounded batches, progress checkpoints, cancellation, and restart safety.
- Check executor ownership, concurrency and queue bounds, rejection policy, error handling, shutdown behavior, and saturation impact.
- Verify explicit propagation or reconstruction of security, tenant, locale, logging, and tracing context across async boundaries. Do not assume thread-local state survives executor or virtual-thread transitions.

## Configuration, observability, and operations

- Verify that new behavior is configurable only where variability is real and that defaults are safe for production.
- Check typed configuration binding, validation, profile behavior, environment overrides, and missing or malformed configuration.
- Check startup ordering and fail-fast behavior for required dependencies. Do not require startup failure for an intentionally optional dependency.
- Verify logs at the boundary that owns operational handling; detect duplicate exception logs, payload logging, misleading success logs, and unstable or sensitive identifiers.
- Check metric and tracing cardinality, labels derived from untrusted input, context propagation, and visibility of new failure modes.
- Check health indicators, readiness, graceful shutdown, pool limits, queue depth, timeouts, and backpressure only where the change affects them.
- Inspect feature-flag lifecycle, default value, targeting, rollback path, and behavior while application versions coexist.
- Verify that the deployment can roll forward and back without incompatible API, event, configuration, cache, or schema states.

Report missing dashboards, alerts, or runbook changes as findings only when the feature creates an operational failure mode that cannot be detected or handled with the established observability.

## Build, dependencies, and delivery

Apply the supply-chain rules from `application-security` and the version-compatibility rules from each relevant owner skill.

- Inspect Maven or Gradle dependency, plugin, repository, annotation-processor, compiler, test, packaging, and container changes.
- Treat wrappers, build scripts, tests, plugins, annotation processors, container entrypoints, and code generators as executable review inputs; inspect their changes before running them.
- Verify that a new dependency has a concrete need, an approved source, compatible licensing and support posture, and no avoidable overlap with existing functionality.
- Check direct and transitive version alignment with the project's Spring Boot dependency management. Do not recommend a blind upgrade solely because a newer release exists.
- Treat dependency and secret scanners as evidence sources that require reachability, configuration, exploitability, and project-policy analysis.
- Check reproducibility, dependency locking or verification where the project uses it, and unexpected artifacts or repositories introduced by the change.
- Verify that generated sources, reflection metadata, native-image configuration, resource filtering, and annotation processing still run in the relevant build profiles.
- Inspect CI rules for skipped tests, changed test selection, permissive failure handling, removed quality gates, unsafe caching, credential exposure, and untrusted pull-request execution.
- Check packaging and deployment descriptors for Java runtime compatibility, health checks, least privilege, resource limits, environment propagation, and rollback compatibility.
- Verify that a release change updates the SBOM, provenance, vulnerability exception, or operational documentation only when the project baseline requires it.

Do not turn every outdated transitive dependency into a finding. Report the concrete policy violation, reachable vulnerability, incompatibility, or unsupported runtime risk.

## Tests and review completeness

Apply `spring-boot-testing` as the single owner of test levels, scenarios, fixtures, isolation, and
execution. Apply the behavior-owning skill to decide what the tests must prove.

- Verify that every required test boundary exists and that its assertions support the claim made for
  that boundary.
- Check whether a regression test would have failed before the production fix.
- Inspect build configuration and test selection so the relevant suites actually run.
- Treat missing supported-database, proxy, security, transaction, or external-boundary evidence
  according to the applicable owner skill rather than inventing a second test standard here.

Do not demand exhaustive tests for unchanged framework behavior. Name the missing scenario and the appropriate verification layer.
