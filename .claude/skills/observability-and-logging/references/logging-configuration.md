# Logging configuration

Use this reference when configuring log output, building correlation context, propagating it across
threads and HTTP clients, or deciding what to log at a given layer. Apply every rule from
`../SKILL.md`, `modern-java-21`, and `application-security`; imports are omitted.

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

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
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

- The supplied header is untrusted input that will appear in every log line and in the response. Bounding its length and character set prevents log injection and unbounded field values. A value that fails validation is replaced, not rejected: a malformed header is not worth failing a request over.
- `finally` is mandatory. Servlet threads are pooled, so a key left in MDC reappears in an unrelated request and attributes one user's activity to another.
- Setting the response header lets a user quote the identifier in a support ticket.
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

## What to log at each layer

| Layer | Level | Content |
| --- | --- | --- |
| Inbound request and response | `DEBUG` | Method, templated route, status, duration. Never bodies. |
| Expected client failure, `4xx` | `INFO` or `WARN` | Internal error code, problem type, correlation identifier. No stack trace. |
| Unexpected server failure, `5xx` | `ERROR` | Internal error code, correlation identifier, and the exception passed as the last argument. |
| Service business decision | `INFO` | The state transition and its inputs by identifier, when an operator could not otherwise reconstruct it. |
| Outbound call | `DEBUG` | Target system, operation, duration, outcome. |
| Outbound failure | `WARN` | Target system, error category, whether a retry or fallback occurred. |
| Persistence | — | Nothing. The service layer already logged the operation. |
| Mapper, TO, domain record | — | Nothing. |

```java
LOGGER.info("User status changed userId={} fromStatus={} toStatus={}",
        userId, previousStatus, newStatus);
```

```java
LOGGER.error("Unhandled failure while creating user errorCode={} problemType={}",
        InternalErrorCodes.USER_CREATION_FAILED, ProblemTypes.INTERNAL_ERROR, exception);
```

The message is a stable constant and the variable parts are placeholders, so the two records group
together in any backend. The exception is the last argument, without a placeholder, which is how
SLF4J captures the stack trace.

Guard a log call only when producing its arguments is genuinely expensive:

```java
if (LOGGER.isDebugEnabled()) {
    LOGGER.debug("Resolved fetch plan plan={}", describeFetchPlan(query));
}
```

A guard around a call whose arguments are already-computed values adds noise and no benefit.

## Rejected logging

```java
// Wrong: concatenation runs even when the level is disabled, and the message
// cannot be grouped because every record has a different text.
LOGGER.debug("Loading user " + userId + " for tenant " + tenantId);
```

```java
// Wrong: the same stack trace is emitted at every layer that rethrows.
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
