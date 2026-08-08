---
name: observability-and-logging
description: Logging, metrics, tracing, and operational endpoints for Java 21+ Spring Boot REST applications. Use when adding or changing log statements, log configuration, correlation or request context, MDC, Micrometer meters, tracing, actuator endpoints, health indicators, or readiness and liveness probes; when a change makes a failure hard to diagnose; and when verifying that a feature is operable in production. Backend-neutral: the same instrumentation serves ELK, Grafana with Loki and Prometheus, OpenTelemetry collectors, or a managed platform.
---

# Observability and Logging

An unobservable feature is unfinished. When it fails at 3 a.m., the only things that exist are the
logs, the metrics, and the trace. Instrument for that moment, not for the demo.

## Backend neutrality

This skill instruments the application, not the platform. The application emits structured JSON logs
on standard output, Micrometer meters, and W3C trace context. Every common stack consumes that
contract, so the choice between ELK, Grafana with Loki and Prometheus, an OpenTelemetry collector,
or a managed platform does not change application code.

Only two declarations touch the backend, and both are single lines recorded in the project profile:

| Decision | What changes |
| --- | --- |
| Log JSON format | One property selecting ECS, Logstash, or GELF. No code changes. |
| Metrics and trace export | The Micrometer registry artifact and the exporter, per `build-and-dependencies`. |

Do not couple application code to a backend. No vendor SDK in a service, no appender that ships logs
over the network from inside the application, no log format assembled by hand for one collector.
Write to standard output and let the platform collect it.

## Coordination with other skills

| Skill | Treat as owner of |
| --- | --- |
| `application-security` | What must never be logged, data classification, masking, redaction, retention, and securing management endpoints |
| `project-naming-conventions` | The names of meters, tags, spans, log fields, and internal error codes |
| `spring-boot-patterns` | Layer responsibilities and the error contract this skill records |
| `build-and-dependencies` | Registry, exporter, and encoder declarations |
| `spring-boot-testing` | Test levels; this skill owns what about observability is worth asserting |
| `spring-boot-code-review` | Review scope, evidence, severity, and reporting |

Out of scope: log shipping, retention infrastructure, dashboards, alert rules, and SLO definitions.
Those follow the approved platform standard.

## Record the decisions before instrumenting

`docs/project-profile.md` must state:

- the log JSON format, and whether local development uses a human-readable console format instead;
- the correlation header name and the MDC keys the project uses;
- whether distributed tracing is enabled, which bridge and exporter, and the sampling policy;
- the metrics registry, and whether metrics are scraped or pushed;
- the management port and which actuator endpoints are exposed.

When a decision is missing, ask once and record the answer. Until the observability backend is
chosen, still emit structured JSON and Micrometer meters: that work is not wasted, because it is
what every candidate backend consumes.

## Reference routing

- Read [logging configuration](references/logging-configuration.md) for log format, correlation context, MDC, propagation across threads and HTTP clients, and what to log at each layer.
- Read [metrics and tracing](references/metrics-and-tracing.md) for Micrometer meters, tag cardinality, observations, and trace propagation.
- Read [operational endpoints](references/operational-endpoints.md) for actuator exposure, health indicators, and liveness and readiness probes.

## Correlation context is the backbone

Without a correlation identifier, a distributed failure cannot be reconstructed. This is the first
thing to build and the easiest to get subtly wrong.

- Accept the configured correlation header when the caller supplies it, and generate one when it is absent. Validate its format and bound its length; it is untrusted input that ends up in every log line.
- Put it into MDC in a filter that runs **before the security filter chain**, so authentication and authorization failures are correlated too. A correlation filter ordered after security silently loses every `401` and `403`.
- Always clear MDC in a `finally` block. Threads are pooled: a key left behind reappears in an unrelated request and attributes one user's activity to another.
- Return the correlation identifier in a response header on every response, and additionally as the `correlationId` member of an error body, so a caller can quote it in a support ticket. `spring-boot-patterns` owns the error contract; `traceId` and `spanId` never appear in a response body.
- Propagate it on every outbound call through a client interceptor, and into `@Async` and `@Scheduled` work through a task decorator. MDC is thread-local and does not cross a thread boundary by itself.
- When tracing is enabled, Micrometer places `traceId` and `spanId` in MDC automatically. Keep the correlation identifier as well: it is the value a human can read, quote, and search for.

## Log deliberately, at one place

- **Log once, at the boundary that handles the failure.** Catching, logging, and rethrowing produces the same stack trace three times and triples the cost of every incident.
- Log expected `4xx` failures at `INFO` or `WARN` with the internal error code, never at `ERROR`. Reserve `ERROR` for unexpected server failures, and always include the exception so the stack trace is captured.
- Never log inside a loop per element. Log the aggregate.
- Use parameterized placeholders, never string concatenation. Concatenation runs even when the level is disabled.
- Emit every caller-visible failure from the single REST exception advice that owns the error contract, with the catalog constant's internal `errorCode` as a structured field. That code and the public problem type are the same condition under two names, so an operator moves between a log line and the public contract without a lookup table.
- Keep the message a stable, searchable constant. Values named in the project's structured-field vocabulary go into MDC or SLF4J key-value pairs, never into the message text; other values may use `{}` placeholders. A message assembled from values cannot be grouped, and a value that appears both as a field and inside the sentence is indexed twice.
- Log a state transition that matters to the business at `INFO`. Log a decision an operator could not otherwise reconstruct. Do not log method entry and exit.

`application-security` decides what may appear in a log at all. Never log credentials, tokens,
personal data, full request or response bodies, or full SQL with parameters.

## Instrument what a feature needs to be operable

Every new feature is instrumented before it is considered complete:

| Element | Required instrumentation |
| --- | --- |
| Service operation performing I/O | A timer, tagged with the outcome |
| Call to another service or external system | A timer and an error counter, plus a client-side trace span |
| Scheduled job | Success and failure counters, and the timestamp of the last successful run |
| Idempotency, retry, or fallback path | A counter, so silent degradation is visible |
| Cache, when the project has one | Hit ratio and eviction metrics |

HTTP server metrics, datasource pool metrics, and JVM metrics come from auto-configuration. Do not
reimplement them.

**Tag cardinality is a hard limit.** Never use an identifier, email address, tenant, raw URL, free
text, timestamp, or exception message as a tag value. Every distinct value creates a time series;
an unbounded tag will exhaust the metrics backend, and it is the fastest way to take down a
monitoring stack. Use a templated route, a bounded outcome, and a bounded error category.

Because cardinality is a runtime property that no static check can see, back the rule with a
`MeterFilter` that caps allowable values per tag key and denies the meter beyond the cap, so an
accidental unbounded tag degrades one meter instead of the monitoring backend.
`build-and-dependencies` shows the configuration. The filter is a safety net, not permission to
relax the rule.

## Test what is worth testing

Assert the contract, not the prose.

- Assert that the correlation identifier is present in the response header and survives across the layers involved in the request.
- Assert that a meter exists with the expected name and tags after the operation, using the project's test registry.
- Assert health endpoint groups and their statuses at the integration level.
- Do not assert log message text as a matter of course; it is brittle and proves nothing about behavior. Capture output only where a log record is the sole observable effect, such as a fire-and-forget failure path, and then assert the field, not the sentence.

## Anti-patterns

Reject:

- `System.out`, `System.err`, or `printStackTrace`;
- string concatenation or expensive computation in a log call without a level guard;
- catch, log, and rethrow;
- MDC written without a `finally` cleanup, or a correlation filter ordered after the security filter chain;
- logging a full request or response body, full SQL with parameters, credentials, tokens, or personal data;
- an unbounded metric tag such as an identifier, email, tenant, or raw URL;
- a logger field with an inconsistent name, a non-`static` logger, or a logger declared on the wrong class;
- tracing enabled inside the service but not propagated to outbound calls;
- an external dependency check in the liveness probe;
- a vendor logging or metrics SDK called from a service, or an appender shipping logs from inside the application;
- observability added as a follow-up task after the feature merges.

## Completion checklist

- [ ] The profile records the log format, correlation header, tracing decision, registry, and exposed endpoints.
- [ ] Correlation context is created or accepted, bounded, placed in MDC before security, cleared in `finally`, returned to the caller, and propagated to outbound calls and asynchronous work.
- [ ] Each failure is logged once, at the level its category deserves, with the internal error code and correlation identifier.
- [ ] No prohibited data reaches any log, per `application-security`.
- [ ] The feature's required meters exist, and every tag value is bounded.
- [ ] Tracing, when enabled, spans the operation and crosses every outbound boundary.
- [ ] Health groups reflect real dependencies, with external checks in readiness only.
- [ ] Correlation propagation and meter registration are covered by tests.

## Primary guidance

- [Spring Boot: Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
- [Spring Boot: Structured Logging](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured)
- [Spring Boot: Metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [Spring Boot: Tracing](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
- [Micrometer: Concepts](https://docs.micrometer.io/micrometer/reference/concepts.html)
- [Micrometer: Observation](https://docs.micrometer.io/micrometer/reference/observation.html)
- [Kubernetes Probes with Spring Boot](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes)
