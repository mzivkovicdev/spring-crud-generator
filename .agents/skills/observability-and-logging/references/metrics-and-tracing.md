# Metrics and tracing

Use this reference when adding Micrometer meters, choosing tags, instrumenting an operation with an
observation, or enabling and propagating distributed tracing. Apply every rule from `../SKILL.md`,
`project-naming-conventions` for meter and span names, and `build-and-dependencies` for the registry
and exporter declarations; imports are omitted.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Registry and exporter](#registry-and-exporter)
2. [Choose the right meter](#choose-the-right-meter)
3. [Tag cardinality](#tag-cardinality)
4. [Instrumenting an operation](#instrumenting-an-operation)
5. [Scheduled jobs and degradation paths](#scheduled-jobs-and-degradation-paths)
6. [Tracing](#tracing)
7. [Testing instrumentation](#testing-instrumentation)

## Registry and exporter

Application code depends on `MeterRegistry` only. The backend is a dependency and a property.

Micrometer is the instrumentation API on every supported Spring Boot generation. What changes with
the generation is the export wiring, not a single rule about meters, tags, or spans.

**On Spring Boot 3**, add the registry artifact for the backend:

| Backend | Registry artifact | Model |
| --- | --- | --- |
| Prometheus, including a Grafana stack | `micrometer-registry-prometheus` | Scraped from an actuator endpoint |
| OpenTelemetry collector, vendor-neutral | `micrometer-registry-otlp` | Pushed to the collector |
| Elastic-based stack | `micrometer-registry-elastic`, or OTLP through a collector | Pushed |

**On Spring Boot 4**, prefer the OpenTelemetry starter, which carries metrics, traces, and logs over
OTLP from one dependency instead of a registry artifact per signal and per backend. A
Prometheus scrape endpoint remains a valid choice when the platform pulls rather than receives; that
is a deployment decision, recorded in the profile. Reporting metrics through Micrometer stays the
recommendation either way — do not move application code onto the OpenTelemetry metrics API because
the starter is present, or those meters stop behaving like the rest.

Rules:

- Never import a vendor SDK in a service, a controller, or a domain type. `MeterRegistry` and `Observation` are the only instrumentation APIs application code sees.
- Choosing the registry later costs one dependency and one property. Choosing it never costs the instrumentation, so instrument now.
- HTTP server, HTTP client, datasource pool, JVM, and, where present, cache metrics come from auto-configuration. Do not reimplement them, and do not wrap them in project-specific meters.
- Add common tags such as application and environment through a `MeterRegistryCustomizer`, not on individual meters.

## Choose the right meter

| Meter | Use for | Do not use for |
| --- | --- | --- |
| Counter | Events that only increase: failures, retries, cache misses, fallbacks | Anything with a duration |
| Timer | Duration and rate of an operation, together | A value that is not a duration |
| Gauge | A current value sampled on scrape: queue depth, last successful run age | Events; a gauge samples and loses everything between samples |
| DistributionSummary | Non-time distributions: payload size, batch size | Durations, which belong in a timer |

A timer already records count, so a separate counter beside a timer for the same operation is
duplication.

## Tag cardinality

This is a hard limit, not a guideline. Each distinct combination of tag values creates a time
series, and an unbounded tag will exhaust the metrics backend. It is the most common way an
application takes down a monitoring stack.

Never use as a tag value:

- an identifier of any kind: user, order, request, correlation, session;
- an email address, username, or any personal data;
- a tenant, unless the tenant set is small, fixed, and approved;
- a raw URL containing path variables or query parameters;
- an exception message, a free-text reason, or a timestamp;
- an unbounded enumeration such as a country plus city plus device combination.

Use instead:

- the templated route, for example `/api/v1/users/{userId}`;
- a bounded outcome such as `success`, `client_error`, `server_error`;
- a bounded error category, not the exception message;
- the exception simple name only when the set of exceptions is known and small.

If you cannot state the maximum number of distinct values a tag can take, it does not belong on a
meter. Put it in a log field instead: logs are searchable at high cardinality, metrics are not.

## Instrumenting an operation

Prefer an observation, which produces a timer and a trace span from one instrumentation point.

This is an excerpt of the `UserService` declared in
[`spring-boot-patterns` → service and domain examples](../../spring-boot-patterns/references/service-domain-examples.md#service-contract-and-implementation).
It adds one constructor dependency and wraps the existing `create` body, which moves unchanged into
the private `createUser`; the class annotations and every other method stay as declared there.

```java
    private static final String OUTCOME_KEY = "outcome";
    private static final String USER_CREATION_OBSERVATION = "user.creation";

    private final ObservationRegistry observationRegistry;

    @Transactional
    public UserDomain create(final String username, final String email, final String rawPassword) {
        final Observation observation =
                Observation.createNotStarted(USER_CREATION_OBSERVATION, this.observationRegistry)
                        .start();

        try (Observation.Scope ignoredScope = observation.openScope()) {
            final UserDomain createdUser = this.createUser(username, email, rawPassword);

            observation.lowCardinalityKeyValue(OUTCOME_KEY, "success");

            return createdUser;
        } catch (final RuntimeException exception) {
            observation.lowCardinalityKeyValue(OUTCOME_KEY, "failure");
            observation.error(exception);

            throw exception;
        } finally {
            observation.stop();
        }
    }
```

The outcome tag is set **after** the operation completes, in the branch that knows what happened.
Setting it before execution, or setting it once on the builder, records `success` even when the
operation throws. That is not a cosmetic defect: it produces a metric that reports a healthy service
during an incident, which is the exact failure this instrumentation exists to prevent. Verify the
failure branch with a test.

`observe(...)` with a lambda is acceptable only when the observation needs no outcome tag at all,
because it gives no place to set one after the fact. It still records the error automatically.

`lowCardinalityKeyValue` becomes a metric tag and a span attribute. `highCardinalityKeyValue`
becomes a span attribute only, which is where an identifier belongs: a trace can carry a user
identifier, a metric cannot.

Opening a scope is what makes the observation the current one, so nested instrumentation and log
correlation attach to it. Closing it in a try-with-resources and stopping in `finally` is required;
an observation that is started and never stopped leaks and never records.

For a simple duration where a span adds nothing, a timer is enough:

The excerpt below is a method of an outbound adapter that holds `MeterRegistry` and the billing
client as constructor-injected `final` fields — `this.meterRegistry` and `this.billingClient` — plus
the constant shown with it. `OUTCOME_KEY` is the same constant declared in the observation example
above.

```java
private static final String BILLING_LOOKUP_TIMER = "billing.client.lookups";

final Timer.Sample sample = Timer.start(this.meterRegistry);
String outcome = "failure";
try {
    final BillingAccount account = this.billingClient.lookup(request);

    outcome = "success";

    return account;
} finally {
    sample.stop(this.meterRegistry.timer(BILLING_LOOKUP_TIMER, OUTCOME_KEY, outcome));
}
```

The meter is named for the phenomenon being measured, `billing.client.lookups`, not for the layer or
the mechanism. `project-naming-conventions` owns that form: name the measured phenomenon, keep the
name stable across refactors, and let the registry add the exporter's unit suffix rather than
writing `duration` or `_seconds` into the source name. Note that the outcome is again resolved
before the meter is recorded, for the same reason as above.

`@Observed` on a method is acceptable when the project declares an `ObservedAspect` bean, but it
does not apply to self-invocation and it hides the instrumentation from the reader. Prefer the
explicit form in a service.

Declare meter names as constants, per `project-naming-conventions`. A name typed twice in two places
is a metric that silently splits into two series.

## Scheduled jobs and degradation paths

A job that fails silently is worse than a job that does not exist. Every scheduled job records:

- a counter of executions tagged with a bounded outcome;
- a timer of the execution duration;
- a gauge of the time since the last successful run, which is what an alert actually needs.

Every path that hides a failure from the caller must increment a counter: a retry, a fallback, a
circuit-breaker rejection, a cache miss that triggers a slow path, an idempotency replay. Silent
degradation with no counter is invisible until it becomes an outage.

## Tracing

Tracing is optional and recorded in the project profile. When enabled:

- On Spring Boot 3, use Micrometer Tracing with an OpenTelemetry bridge and an OTLP exporter unless the platform requires otherwise. On Spring Boot 4, the OpenTelemetry starter covers the same path with one dependency. The exporter is a dependency and a property; the instrumentation is identical either way.
- Propagation is W3C `traceparent` by default. Do not invent a custom propagation header.
- Auto-instrumentation covers inbound HTTP, outbound `RestClient` and `WebClient`, and scheduled tasks. Add manual spans only for a meaningful internal operation that auto-instrumentation cannot see.
- Sampling is a deployed configuration decision. Do not hardcode a sampling probability in application code.
- **Tracing enabled internally but not propagated outward is the failure mode to avoid.** Verify that outbound calls carry the trace context, and that message producers and consumers do too.
- The correlation identifier stays alongside `traceId`. They serve different purposes: one is quoted by a human, the other joins spans.
- Never put a secret, a credential, or personal data in a span attribute. Span attributes are exported to the same backends as everything else.

## Testing instrumentation

Assert that instrumentation exists, not that it produced a particular number.

```java
@Test
void create_whenRequestIsValid_recordsCreationTimer() {
    this.userService.create("ana", "ana@example.com", "raw-password");

    assertThat(this.meterRegistry.find("user.creation")
            .tag("outcome", "success")
            .timer())
        .isNotNull()
        .extracting(Timer::count)
        .isEqualTo(1L);
}
```

- Use `SimpleMeterRegistry` in unit tests, and the application's registry in integration tests.
- Assert the meter name and the tags, because those are the contract a dashboard and an alert depend on.
- Do not assert timing values; they are nondeterministic.
- Assert that a failure path increments its counter, and that a failing operation is tagged `outcome=failure`. Those two assertions catch silent degradation and the mis-set outcome tag, and they are the ones most often missing.
