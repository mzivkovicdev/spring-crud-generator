# Error handling examples

Worked `ProblemDetail` examples for the REST error contract owned by `../SKILL.md`. Identifiers and
imports follow the same conventions as [REST API examples](rest-api-examples.md); the controller,
TO, and mapper examples live there.

## ProblemDetail exception handling

One enum is the single catalog of caller-visible failures. Each constant carries everything that
one condition needs: the HTTP status, the RFC 9457 `type` URI, the human-readable title and detail,
and the internal code used in logs, events, and metrics. Deriving the URI from the constant name
makes the one-to-one relationship between the internal code and the public type structural rather
than a convention someone has to remember.

```java
public enum ApplicationError {

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied",
            "The authenticated caller is not allowed to perform this operation."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Concurrent modification",
            "The resource was modified concurrently. Retry the operation."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Email already registered",
            "An account already exists for the supplied email address."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
            "The server could not process the request."),
    INVALID_STATE(HttpStatus.CONFLICT, "Invalid resource state",
            "The operation is not allowed in the current resource state."),
    LOCK_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "Resource temporarily locked",
            "The resource is busy. Retry after the interval in the Retry-After header.", true),
    ORGANIZATION_CLOSED(HttpStatus.CONFLICT, "Organization closed to new members",
            "The organization does not accept new members."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found",
            "The requested resource does not exist."),
    RESPONSE_VALIDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Response validation failed",
            "The server could not produce a valid response."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required",
            "The request did not carry a valid credential."),
    USER_NOT_MODIFIABLE(HttpStatus.CONFLICT, "User not modifiable",
            "The user is not in a state that accepts this change."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed",
            "The request contains invalid values.");

    private static final String PROBLEM_TYPE_BASE = "https://api.example.com/problems/";

    private final String detail;
    private final boolean expected;
    private final HttpStatus status;
    private final String title;
    private final URI type;

    ApplicationError(final HttpStatus status, final String title, final String detail) {
        this(status, title, detail, false);
    }

    ApplicationError(
            final HttpStatus status,
            final String title,
            final String detail,
            final boolean expected) {

        this.status = status;
        this.title = title;
        this.detail = detail;
        this.expected = expected;
        this.type = URI.create(
                PROBLEM_TYPE_BASE + this.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    /**
     * Returns the stable internal identifier used in logs, events, and metrics.
     *
     * @return the internal error code
     */
    public String code() {
        return this.name();
    }

    public String detail() {
        return this.detail;
    }

    /**
     * Returns whether this condition is a normal operating outcome rather than a defect.
     *
     * @return {@code true} when the condition is expected, which decides the log level
     */
    public boolean expected() {
        return this.expected;
    }

    public HttpStatus status() {
        return this.status;
    }

    public String title() {
        return this.title;
    }

    public URI type() {
        return this.type;
    }
}
```

`VALIDATION_FAILED` therefore always produces the code `VALIDATION_FAILED` and the type
`https://api.example.com/problems/validation-failed`. Neither can drift from the other, and neither
can acquire a second spelling somewhere else in the codebase.

`ACCESS_DENIED` and `UNAUTHENTICATED` are in this catalog even though **no handler in this advice
builds a response from them** — the security filter chain emits both, for the reason stated under
[security denials](#security-denials-are-the-filter-chains-contract-not-this-advices). They belong
here regardless of which layer writes the response, because the rule is one declaration per
caller-visible failure, not one declaration per emitter. A security response built from its own
inline status and message is a second catalog, and `rest-api-contract` publishes both problem types
like any other.

```java
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CORRELATION_ID_PROPERTY = "correlationId";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Retry-After is a header value, so a String, and it is derived once at startup from the
    // one place the lock timeout is recorded. LockingProperties is the configuration record
    // declared in spring-data-jpa's lock-timeout section and bound in config.properties like
    // every other @ConfigurationProperties type. See "Deriving Retry-After from the lock timeout".
    private final String lockRetryAfterSeconds;

    public ApiExceptionHandler(final LockingProperties lockingProperties) {
        this.lockRetryAfterSeconds = Long.toString(
                lockingProperties.pessimisticLockTimeout().plusNanos(999_999_999L).toSeconds());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException exception) {
        return createProblem(ApplicationError.RESOURCE_NOT_FOUND, exception);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ProblemDetail handleInvalidState(final InvalidStateException exception) {
        return createProblem(ApplicationError.INVALID_STATE, exception);
    }

    // Carrier exception: the constant travels with the failure, so one handler serves
    // every business-rule condition without a handler per rule.
    @ExceptionHandler(BusinessValidationException.class)
    public ProblemDetail handleBusinessValidation(final BusinessValidationException exception) {
        return createProblem(exception.error(), exception);
    }

    @ExceptionHandler(ConcurrentModificationConflictException.class)
    public ProblemDetail handleConcurrentModification(
            final ConcurrentModificationConflictException exception) {

        return createProblem(ApplicationError.CONCURRENT_MODIFICATION, exception);
    }

    // Required: an exhausted retry propagates the last framework exception, so without this
    // handler the catch-all below reports a routine conflict as 500. Covers both generations.
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ProblemDetail handleOptimisticConflict(final RuntimeException exception) {
        return createProblem(ApplicationError.CONCURRENT_MODIFICATION, exception);
    }

    // A lock wait that timed out, a deadlock victim, a serialization failure, or a statement
    // cancelled by the statement timeout while it waited for a lock. Nothing about the resource
    // conflicts with the request, so this is 503 with Retry-After, never 409.
    @ExceptionHandler({PessimisticLockingFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<ProblemDetail> handleLockTimeout(final DataAccessException exception) {

        return ResponseEntity.status(ApplicationError.LOCK_TIMEOUT.status())
                .header(HttpHeaders.RETRY_AFTER, this.lockRetryAfterSeconds)
                .body(createProblem(ApplicationError.LOCK_TIMEOUT, exception));
    }

    // The two declining handlers. Both are required and both deliberately produce no response:
    // they hand the exception back to the security filter chain, which is the only place that can
    // tell an unauthenticated caller (401 with a challenge) from an authorized-but-forbidden one
    // (403). Without them the catch-all below reports either as 500. See "Security denials are the
    // filter chain's contract, not this advice's".
    @ExceptionHandler(AccessDeniedException.class)
    public void rethrowAccessDenied(final AccessDeniedException exception)
            throws AccessDeniedException {

        throw exception;
    }

    @ExceptionHandler(AuthenticationException.class)
    public void rethrowAuthentication(final AuthenticationException exception)
            throws AuthenticationException {

        throw exception;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception exception) {
        return createProblem(ApplicationError.INTERNAL_ERROR, exception);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception,
            final HttpHeaders headers,
            final HttpStatusCode ignoredStatus,
            final WebRequest request) {

        final ProblemDetail problem = createProblem(ApplicationError.VALIDATION_FAILED, exception);

        return this.handleExceptionInternal(
                exception, problem, headers, ApplicationError.VALIDATION_FAILED.status(), request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            final HandlerMethodValidationException exception,
            final HttpHeaders headers,
            final HttpStatusCode ignoredStatus,
            final WebRequest request) {

        final ApplicationError error = exception.isForReturnValue()
                ? ApplicationError.RESPONSE_VALIDATION_FAILED
                : ApplicationError.VALIDATION_FAILED;
        final ProblemDetail problem = createProblem(error, exception);

        return this.handleExceptionInternal(
                exception, problem, headers, error.status(), request);
    }

    private static ProblemDetail createProblem(
            final ApplicationError error, final Exception exception) {

        logProblem(error, exception);

        final ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(error.status(), error.detail());
        final String correlationId = CorrelationContext.correlationId();

        problem.setType(error.type());
        problem.setTitle(error.title());
        if (correlationId != null) {
            problem.setProperty(CORRELATION_ID_PROPERTY, correlationId);
        }

        return problem;
    }

    private static void logProblem(final ApplicationError error, final Exception exception) {
        if (error.status().is5xxServerError() && !error.expected()) {
            LOGGER.atError()
                    .addKeyValue("errorCode", error.code())
                    .setCause(exception)
                    .log("Request failed with an unexpected error");
        } else {
            LOGGER.atWarn()
                    .addKeyValue("errorCode", error.code())
                    .log("Request rejected");
        }
    }
}
```

This advice is the one place where a caller-visible failure is logged. `observability-and-logging`
requires exactly one log record per failure, at the boundary that handles it, so services and
controllers must not log the same exception before throwing it, and it decides which level each
condition gets.

What this catalog owns is the input to that decision: each constant carries `expected`, so the
answer is declared once beside the condition instead of being re-derived in every handler.
`LOCK_TIMEOUT` is why the flag exists — it is a `5xx` the system produces by design, and a
status-class check would misread it. A constant is unexpected unless it says otherwise, so the flag
can only narrow the set of `ERROR` records, never widen it.

The `errorCode` is attached as a structured field, not interpolated into the message, so the message
text stays a stable constant that groups across records.

Five handlers exist for reasons that are easy to miss:

- **`AccessDeniedException` and `AuthenticationException` both need a handler that rethrows.** Spring Security has two denial families, not one, and both can be raised inside the dispatch where the catch-all would report them as `500`. Rethrowing stops that without answering here, and lets the filter chain give the answer. The section below states why that split is not optional, and why one handler is not enough.
- **The two contention handlers are what stop a normal concurrent request from being reported as a server fault.** They are covered in full below, because getting them wrong is invisible until the system is under load.
- `@ExceptionHandler(Exception.class)` is the catch-all that guarantees every unexpected failure still produces a `ProblemDetail` rather than the default error page. The specific handlers inherited from `ResponseEntityExceptionHandler` take precedence over it, so framework exceptions keep their intended status.

### Contention reaches this advice as a framework exception

`spring-data-jpa` absorbs contention with the composed `@OptimisticLockingRetry` annotation, and a
lock failure surfaces from the transaction interceptor. **Neither of those produces a project
exception.** When the retry policy is exhausted the last original exception propagates — Spring
Framework 7 documents this for `@Retryable`, and Spring Retry behaves the same way without a
`@Recover` method — so an advice that handles only project types sends every real conflict through
the catch-all as a `500`. The two handlers above are the fix, and they are required on **both**
Spring Boot generations, not only where `@Recover` is unavailable.

Take the parent types, not the concrete ones. Spring's DAO hierarchy places both families under
`ConcurrencyFailureException` as siblings, so each handler covers its own family and neither
swallows the other:

| Handled type | Also covers | Never catches |
| --- | --- | --- |
| `OptimisticLockingFailureException` | `ObjectOptimisticLockingFailureException` | anything pessimistic |
| `PessimisticLockingFailureException` | `CannotAcquireLockException`, `CannotSerializeTransactionException`, `DeadlockLoserDataAccessException` | anything optimistic |
| `QueryTimeoutException` | a statement the database cancelled on its own timeout, including one cancelled while waiting for a lock | anything that completed |

**`QueryTimeoutException` is listed beside the pessimistic family, not under it.** Spring places it
under `TransientDataAccessException` as a *sibling* of `ConcurrencyFailureException`, so
`PessimisticLockingFailureException` does not cover it. That matters wherever the engine bounds a
lock wait through the statement timeout rather than through a lock timeout — PostgreSQL is the
common case, and `spring-data-jpa` names it — because the caller then waits, the database cancels
the statement, and without this type in the list the routine contention this section exists to
handle arrives at the catch-all as `500`. The handler parameter widens to `DataAccessException`,
the narrowest type that covers both families.

Spring's own javadoc recommends handling `PessimisticLockingFailureException` rather than its
subclasses, which is also what keeps a deadlock victim and a serialization failure from falling
through to the catch-all. `jakarta.persistence.OptimisticLockException` is listed beside the Spring
type because it can still surface on paths where exception translation did not run.

**The two conditions get different statuses, and the difference is not cosmetic.**

| Condition | Status | Why |
| --- | --- | --- |
| Optimistic conflict, retry exhausted | `409` | The resource really did change under the caller. RFC 9110 defines `409` as a conflict with the current state of the target resource, in situations where the caller might resolve it and resubmit — re-reading and re-submitting is exactly the available action |
| Lock wait timed out, deadlock victim, serialization failure | `503` + `Retry-After` | Nothing about the resource conflicts with the request, and there is nothing for the caller to resolve. The same request succeeds once the holder commits. RFC 9110 defines `503` as a temporary condition the server expects to be alleviated after a delay, and `Retry-After` is the field that carries that delay |

Do not collapse the two into one status because both are "contention". A `409` tells a client to
inspect and fix something that is not wrong, and it hides the load problem behind a code that reads
as a data problem. Equally, do not report either as `500`: a caller that retries a `500` is guessing,
and an operator seeing one is paged for a request that behaved exactly as designed.

`503` is a server-error status, so it counts against an error budget and can trip outlier detection
or a circuit breaker. That is a reason to alert on a **sustained rate** rather than on individual
occurrences, and a reason `Retry-After` is mandatory rather than optional — it is not a reason to
mislabel the condition as `409`. `observability-and-logging` owns the meter; the advice logs it
once like every other failure.

### Deriving Retry-After from the lock timeout

A `Retry-After` shorter than the lock timeout invites the caller back before the holder can plausibly
have finished, which turns one queue into two. The way to guarantee it is never shorter is to stop
writing it down: the advice takes the **Pessimistic lock timeout** the profile records — the same
value `spring-data-jpa` applies to the database — and converts it, rounding **up** to whole seconds.

```java
this.lockRetryAfterSeconds = Long.toString(
        lockingProperties.pessimisticLockTimeout().plusNanos(999_999_999L).toSeconds());
```

Adding one nanosecond short of a second before `toSeconds()` is ceiling division: a 2500 ms timeout
yields `3`, not the `2` that truncation would give and that would send the caller back early. The
nanosecond form is deliberate — `plusMillis(999)` reads more simply but truncates anything below a
millisecond, and Spring binds `ns` and `us` durations, so `3000000001ns` would round to `3` and
reintroduce exactly the too-short header this derivation exists to prevent. A timeout that is
already a whole number of seconds is unaffected either way.

`LockingProperties` is the record declared under
[lock timeouts](../../spring-data-jpa/references/locking-and-retry-examples.md#lock-timeouts) in
`spring-data-jpa`, which owns the value and how it reaches the database on each engine. It is an
ordinary `@ConfigurationProperties` type in `config.properties`, not a persistence bean — this
advice depends on the project's configuration, not on its persistence layer. This skill owns only
the header, and takes the number from that property rather than repeating it.

Assert the relationship in a test: a `503` from a timed-out lock must carry a `Retry-After` no
smaller than the configured timeout. The two values live in different layers and nothing else keeps
them in step.

The optimistic handler declares `RuntimeException` rather than `Exception`: it is the narrowest type
that covers both listed exceptions, since `jakarta.persistence.OptimisticLockException` and the
Spring type share no closer ancestor. `modern-java-21` allows a broad type only at a true top-level
boundary, and taking the narrowest one that compiles is the habit that keeps the exception honest.
Returning a header requires `ResponseEntity<ProblemDetail>`; a bare `ProblemDetail` return carries
the status but no headers, which is why that one handler has a different return type from the rest.

### Two exception shapes, and when each applies

The catalog is the single declaration, but exceptions reach it two ways. Choose per condition and keep
both shapes in the project; neither replaces the other.

| Shape | Example | Use when |
| --- | --- | --- |
| **Fixed mapping** — one exception type, one constant, resolved in the handler | `ResourceNotFoundException` → `RESOURCE_NOT_FOUND`, `ConcurrentModificationConflictException` → `CONCURRENT_MODIFICATION` | The condition is one thing. The exception carries only the context needed for the log line. |
| **Carrier** — one exception type, several constants, the constant passed at throw site | `BusinessValidationException(ApplicationError.USER_NOT_MODIFIABLE)` | Several business rules share one handling contract but need distinct problem types. A handler per rule would be a handler per business rule, which is how an advice grows without bound. |

A carrier exception is the reason `BusinessValidationException` is not hard-mapped to `400`: its
status comes from the constant it carries, so `USER_NOT_MODIFIABLE` produces `409` and a
caller-correctable input rule produces `400`, from the same type and the same handler. Never let a
carrier accept a status, a title, or a URI directly — it accepts a catalog constant and nothing else,
or the catalog stops being the single declaration.

The response body carries exactly one machine-readable error identifier, the `type` URI, plus the
`correlationId` extension member. `correlationId` is not a second error identifier: it identifies
the request, not the failure, and support workflows need it in the payload a caller copies into a
ticket. `traceId` and `spanId` stay out of the body; they are internal correlation values that
belong in logs and in the trace backend.

`CorrelationContext` is the small read accessor that `observability-and-logging` defines for the
current request's correlation identifier. The advice reads it through that accessor rather than
touching `MDC` or the correlation filter directly, so the error contract does not depend on how
request context is stored or on the package the filter lives in.

Declare the catalog once, in the `exception` package. Do not add a parallel constants holder for
problem type URIs or internal error codes; a second declaration is what allows one condition to
acquire two identities.

Use one project-owned `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` as the MVC
error-contract owner. It preserves Spring MVC handling for malformed requests, unsupported methods
and media types, binding failures, and other framework exceptions; override only cases that require
the project's stable problem contract. Preserve a coherent existing alternative instead of adding a
second overlapping global handler.

Place this advice in `<base-package>.exception.handler`. Keep the exceptions it handles in
`<base-package>.exception`; do not place the advice directly beside them.

Before adding handlers, inventory the exceptions that can cross each controller boundary and map every caller-visible category to a constant in the error catalog. Keep input-validation failures as `400`, but treat return-value validation as a server failure. Reuse shared exception categories when their public handling is identical, and add a catalog constant only for a condition with a distinct status, type, or response contract. Never register a handler for `jakarta.validation.ValidationException`. If `ConstraintViolationException` can cross the boundary, distinguish argument violations from return-value or internal violations before choosing a status.

Normal REST TO responses use `application/json`; RFC 9457 error responses use
`application/problem+json`.

### Security denials are the filter chain's contract, not this advice's

**A denial that reaches this advice must leave it again.** The two handlers above rethrow the
original exception and return nothing, which looks like a no-op and is the whole point.

The reason is a distinction only the filter chain can make. `ExceptionTranslationFilter` asks whether
the current authentication is anonymous or remember-me: if it is, the caller never authenticated, so
the request goes to the `AuthenticationEntryPoint` and comes back **`401` with a
`WWW-Authenticate` challenge**; if the caller is fully authenticated and simply lacks the authority,
it goes to the `AccessDeniedHandler` and comes back **`403`**. An advice that answers the exception
itself never lets it reach that filter, so every anonymous caller receives `403` with no challenge —
a wrong status, a missing protocol header, and a client that cannot tell "log in" from "you may not
do this".

That is why the handlers exist but do not respond. Rethrowing **the original exception** from an
`@ExceptionHandler` is a supported path, not a trick: Spring's `ExceptionHandlerExceptionResolver`
recognises that the thrown exception is the one it was resolving, logs nothing, and continues with
default processing — which is what carries it out to the filter chain. Throwing anything *else* from
a handler is the accident that path is guarding against, and it does get logged.

#### One handler is not enough, because there are two families

`ExceptionTranslationFilter` searches the cause chain for an `AccessDeniedException` **and** for an
`AuthenticationException`, and it checks the authentication family first. Declining only the first
leaves the second going through the catch-all as `500`, which is the same defect one layer over.
Both families are raised inside the dispatch, where only this advice stands between them and the
filter:

| Raised inside the dispatch by | Type | Reaches the filter as |
| --- | --- | --- |
| Method security denying an authenticated caller | `AuthorizationDeniedException`, an `AccessDeniedException` | `403` from the `AccessDeniedHandler` |
| Method security with no `Authentication` in the context — the normal case when the chain disables anonymous authentication | `AuthenticationCredentialsNotFoundException` | `401` with the challenge |
| Method security requiring full authentication from an anonymous caller | `InsufficientAuthenticationException` | `401` with the challenge |

Both types are `RuntimeException` subclasses, so the `throws` clause on each handler is
documentation rather than a compiler requirement. Keep it: it is what says the method exists to
propagate rather than to return.

#### The one case that does not use this path

**A service that calls `AuthenticationManager` itself translates the failure at its own boundary.**
Under the application-issued token profile `application-security` defines, the token endpoint's
service invokes authentication directly, so a `BadCredentialsException` there is not a denial of
*this* request by the chain — it is a provider failure inside a use case, and the outbound rule this
skill already states applies: translate it into a project exception carrying its own catalog
constant, and let the ordinary handler answer it. Relying on the declining handler instead sends the
caller a bearer challenge for an endpoint that does not take bearer tokens.

The test that separates the two is the one worth writing: a wrong password on the token endpoint
must produce the catalog's own credential-failure contract, while a missing token on a protected
route must produce `401` from the chain.

What the filter chain does with a declined exception — which component answers, what the body looks
like, and how it is tested — is `application-security`'s rule, stated in
[let the filter chain answer every denial](../../application-security/references/spring-security-authorization.md#let-the-filter-chain-answer-every-denial).
This skill's part ends at declining the exception.

Do not "simplify" this by deleting either rethrowing handler: the catch-all immediately reclaims the
exception and reports that family's denials as `500`. Do not simplify it the other way either, by
answering `401` or `403` here: that is the anonymous-caller defect above, and it passes every test
written with an authenticated fixture.

Handle listener, job, messaging, and asynchronous failures at their owning boundary because they do
not pass through this advice, and log them there under the same one-record rule. Test each status,
problem type, content type, required header, and information-disclosure rule.

## Handler rules

`../SKILL.md` states the three rules that decide the contract — the `type` URI as the only
machine-readable identifier, one declaration per caller-visible failure, and `correlationId` as the
one permitted extension. They are not repeated here. What follows applies them.

- Map expected application failures explicitly, and let Spring's framework handler preserve standard REST error behavior where appropriate.
- A catch-all handler returns a generic message and logs the cause once. Never copy `exception.getMessage()` into a response unless that type guarantees a stable, user-safe message, and never log an expected 4xx as a server error.
- Name a project-owned validation exception unambiguously, for example `BusinessValidationException`. Never give a project exception the simple name of a framework type such as `jakarta.validation.ValidationException`, and never handle that framework type as if it were the project's category.
- Place custom exceptions in `exception` and MVC handler classes in `exception.handler`. Spring Security response handling belongs to the security configuration boundary, not this package.

The custom exception examples are in [infrastructure examples](infrastructure-examples.md).
