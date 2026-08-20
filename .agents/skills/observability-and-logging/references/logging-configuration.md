# Logging configuration

Use this reference when configuring log output, building correlation context, propagating it across
threads and HTTP clients, or deciding what to log at a given layer. Apply every rule from
`../SKILL.md`, `modern-java-21`, and `application-security`; imports are omitted.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Structured output](#structured-output)
2. [Correlation filter](#correlation-filter)
3. [Propagation to outbound calls](#propagation-to-outbound-calls)
4. [Propagation to asynchronous work](#propagation-to-asynchronous-work)
5. [What to log at each layer](#what-to-log-at-each-layer)
6. [Rejected logging](#rejected-logging)

## Structured output

Log JSON to standard output. The platform collects it. The format is one property, so switching
between an ECS-based stack, a Loki-based stack, or an OpenTelemetry collector changes no code.

```yaml
logging:
  structured:
    format:
      console: ecs
  level:
    root: INFO
    com.acme.myapp: INFO
```

Rules:

- Select the format the collector expects and record it in `docs/project-profile.md`. `ecs` suits an Elasticsearch-based stack; `logstash` and `gelf` suit other collectors. Any of them is consumable by a Loki-based stack, which indexes labels and stores the line as-is.
- Keep a human-readable console format in the local development profile only. Never make it the deployed default.
- Do not add a network appender. An application that ships its own logs loses them exactly when it is unhealthy, and it couples the service to one backend.
- Do not build JSON by hand inside log messages.
- Set levels per package, never per class in committed configuration, and never `DEBUG` at root in a deployed profile.
- Add durable structured fields through MDC, not by appending them to the message.
- `logging.structured.format` exists on both supported generations, so this configuration is generation-neutral. One default is not: on Spring Boot 4 Logback writes files as UTF-8 and takes the console charset from the console when one is available, whereas Spring Boot 3 followed the platform default. On an upgrade, confirm the collector reads the encoding it now receives; a non-ASCII field silently mangled in the pipeline is easy to miss and impossible to reconstruct later.

Declare loggers consistently:

```java
private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
```

The logger is `private static final`, named for its own class, and obtained from SLF4J. Do not use a
different name in different classes, and do not declare a logger on a class that never logs.

## Correlation filter

The filter must run before Spring Security, or authentication and authorization failures are logged
without correlation. Spring Security's chain is ordered at `-100`, so use highest precedence.

```java
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private static final int MAXIMUM_CORRELATION_ID_LENGTH = 64;
    private static final Pattern ALLOWED_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final String correlationId = resolveCorrelationId(request);

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private static String resolveCorrelationId(final HttpServletRequest request) {
        final String suppliedCorrelationId = request.getHeader(CORRELATION_ID_HEADER);

        if (suppliedCorrelationId != null
                && suppliedCorrelationId.length() <= MAXIMUM_CORRELATION_ID_LENGTH
                && ALLOWED_CORRELATION_ID.matcher(suppliedCorrelationId).matches()) {
            return suppliedCorrelationId;
        }

        return UUID.randomUUID().toString();
    }
}
```

Everything outside the correlation infrastructure reads the identifier through one small accessor,
never through `MDC` directly. That keeps the storage mechanism replaceable and stops unrelated
packages from depending on the filter.

```java
public final class CorrelationContext {

    private CorrelationContext() {
    }

    /**
     * Returns the correlation identifier of the current request.
     *
     * @return the correlation identifier, or {@code null} when no request context is established
     */
    public static String correlationId() {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
    }
}
```

`spring-boot-patterns` uses this accessor in the REST exception advice to place `correlationId` in
an error body. Place `CorrelationContext` where both the correlation filter and the exception
handler may depend on it, and do not widen it into a general MDC facade.

```java
@Bean
FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
    final FilterRegistrationBean<CorrelationIdFilter> registration =
            new FilterRegistrationBean<>(new CorrelationIdFilter());

    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

    return registration;
}
```

Why each part is there:

- The supplied header is untrusted input that will appear in every log line, in the response, and in the error body. Bounding its length and character set prevents log injection and unbounded field values. A value that fails validation is replaced, not rejected: a malformed header is not worth failing a request over.
- The `finally` block in the filter above is what `../SKILL.md` requires; it is shown because omitting it is the most common way this filter is written wrong.
- Setting the response header lets a user quote the identifier in a support ticket. The REST exception advice additionally copies it from MDC into the `correlationId` member of an error body, because a caller pasting a failed response into a ticket rarely includes the headers. `spring-boot-patterns` owns that decision; `traceId` and `spanId` never go into a response body.
- The filter is registered with explicit order rather than annotated as a component, so the ordering relative to the security chain is visible and reviewable.

When Micrometer Tracing is enabled, `traceId` and `spanId` appear in MDC automatically. Keep the
correlation identifier alongside them; it is the value a human reads and quotes.

## Propagation to outbound calls

Correlation stops at the process boundary unless it is sent explicitly.

```java
@Bean
RestClientCustomizer correlationIdRestClientCustomizer() {
    return restClientBuilder -> restClientBuilder.requestInterceptor(
            (request, body, execution) -> {
                final String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

                if (correlationId != null) {
                    request.getHeaders().add(
                            CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
                }

                return execution.execute(request, body);
            });
}
```

Apply the equivalent for every outbound client the project uses, including message producers, which
carry the identifier as a message header. A service that traces internally but drops the identifier
on the way out is the most common reason a distributed failure cannot be reconstructed.

## Propagation to asynchronous work

MDC is thread-local. `@Async` and `@Scheduled` work runs on a different thread with empty context.

```java
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(final Runnable runnable) {
        final Map<String, String> callerContext = MDC.getCopyOfContextMap();

        return () -> {
            if (callerContext != null) {
                MDC.setContextMap(callerContext);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

Register it on the executor the application actually uses. `MDC.clear()` in `finally` is required
for the same pooling reason as in the filter.

A scheduled job has no inbound request, so it generates its own correlation identifier at the start
of each execution and clears it at the end. Do not let a job run without one.

## Fields, not interpolated values

`project-naming-conventions` defines a vocabulary of structured field names: `correlationId`,
`traceId`, `spanId`, `operation`, `outcome`, `errorCode`, and approved identifiers such as
`tenantId` and `userId`. Those are **fields**, and they must never also be interpolated into the
message text. A value that exists both as a field and inside the sentence is indexed twice, queried
inconsistently, and destroys message grouping.

The rule is mechanical:

| Value | Where it goes | How |
| --- | --- | --- |
| Named in the structured-field vocabulary, request-scoped | MDC | Written once in a filter or decorator, cleared in `finally` |
| Named in the structured-field vocabulary, record-scoped | Key-value on the record | SLF4J fluent API `addKeyValue` |
| Not in the vocabulary, useful only for a human reading one line | Message placeholder | SLF4J `{}` |
| Anything at all | Never | String concatenation |

The message itself is always a stable constant sentence, so records of the same kind group together
regardless of their field values.

```java
LOGGER.atInfo()
        .addKeyValue("operation", "user.status.change")
        .addKeyValue("userId", userId)
        .addKeyValue("outcome", "success")
        .log("User status changed");
```

Spring Boot's structured logging emits MDC entries and SLF4J key-value pairs as top-level JSON
fields in every supported format, so this record is queryable by field in any backend without a
parser rule. `correlationId`, and `traceId` and `spanId` when tracing is enabled, are already in MDC
and are attached automatically; never add them by hand.

Classic parameterized logging remains correct for values outside the vocabulary:

```java
LOGGER.debug("Resolved fetch plan for query template={}", queryTemplate);
```

## What to log at each layer

| Layer | Level | Content |
| --- | --- | --- |
| Inbound request and response | `DEBUG` | Method, templated route, status, duration. Never bodies. |
| Expected client failure, `4xx` | `WARN` | `errorCode` field. No stack trace. Emitted by the REST exception advice only. |
| Unexpected server failure, `5xx` | `ERROR` | `errorCode` field and the exception attached as the cause. Emitted by the REST exception advice only. |
| Service business decision | `INFO` | The state transition, as fields, when an operator could not otherwise reconstruct it. |
| Outbound call | `DEBUG` | Target system, operation, duration, outcome. |
| Outbound failure | `WARN` | Target system, error category, whether a retry or fallback occurred. |
| Persistence | — | Nothing. The service layer already logged the operation. |
| Mapper, TO, domain record | — | Nothing. |

**Caller-visible failures are logged in exactly one place: the `@RestControllerAdvice` that owns the
error contract.** That advice already knows the error catalog constant, so it emits the `errorCode`
field, chooses the level from the HTTP status, and attaches the exception only for `5xx`.
`spring-boot-patterns` shows the implementation. A service that logs an exception and then throws it
produces the same failure twice, at two levels, with two different field sets. Do not do it.

Failures on boundaries the advice does not cover — listeners, scheduled jobs, messaging consumers,
asynchronous work — are logged at their own boundary, once, following the same rule.

Guard a log call only when producing its arguments is genuinely expensive:

```java
if (LOGGER.isDebugEnabled()) {
    LOGGER.debug("Resolved fetch plan plan={}", describeFetchPlan(query));
}
```

A guard is unnecessary around the fluent API, which evaluates nothing when the level is disabled.

A guard around a call whose arguments are already-computed values adds noise and no benefit.

## Rejected logging

```java
// Wrong: concatenation runs even when the level is disabled, and the message
// cannot be grouped because every record has a different text.
LOGGER.debug("Loading user " + userId + " for tenant " + tenantId);
```

```java
// Wrong: errorCode is in the structured-field vocabulary, so interpolating it into the
// message indexes it twice and makes the message text vary per record.
LOGGER.error("Request failed errorCode={}", error.code(), exception);
```

```java
// Wrong: the same stack trace is emitted at every layer that rethrows.
try {
    return this.userRepository.findById(userId);
} catch (final DataAccessException exception) {
    LOGGER.error("Failed to load user", exception);
    throw exception;
}
```

```java
// Wrong: one record per element floods the backend and hides the outcome.
for (final UserEntity user : users) {
    LOGGER.info("Processing user {}", user.getId());
}
```

```java
// Wrong: MDC is never cleared, so a pooled thread carries the value into the next request.
MDC.put("tenantId", tenantId);
filterChain.doFilter(request, response);
```

```java
// Wrong: full bodies and credentials. Prohibited by application-security regardless of level.
LOGGER.debug("Request body={} authorization={}", requestBody, authorizationHeader);
```

Also rejected: `CommonsRequestLoggingFilter` with payload inclusion enabled, any request-logging
library configured to capture bodies by default, `System.out`, `System.err`, and `printStackTrace`.
