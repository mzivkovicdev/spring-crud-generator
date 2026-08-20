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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
            "The server could not process the request."),
    INVALID_STATE(HttpStatus.CONFLICT, "Invalid resource state",
            "The operation is not allowed in the current resource state."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found",
            "The requested resource does not exist."),
    RESPONSE_VALIDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Response validation failed",
            "The server could not produce a valid response."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed",
            "The request contains invalid values.");

    private static final String PROBLEM_TYPE_BASE = "https://api.acme.example/problems/";

    private final String detail;
    private final HttpStatus status;
    private final String title;
    private final URI type;

    ApplicationError(final HttpStatus status, final String title, final String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
        this.type = URI.create(
                PROBLEM_TYPE_BASE + this.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    /**
     * Returns the stable internal identifier used in logs, events, and metrics.
     *
     * @return the internal error code; never {@code null}
     */
    public String code() {
        return this.name();
    }

    public String detail() {
        return this.detail;
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
`https://api.acme.example/problems/validation-failed`. Neither can drift from the other, and neither
can acquire a second spelling somewhere else in the codebase.

```java
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CORRELATION_ID_PROPERTY = "correlationId";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException exception) {
        return createProblem(ApplicationError.RESOURCE_NOT_FOUND, exception);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ProblemDetail handleInvalidState(final InvalidStateException exception) {
        return createProblem(ApplicationError.INVALID_STATE, exception);
    }

    // Project-owned category for caller-correctable validation failures.
    @ExceptionHandler(BusinessValidationException.class)
    public ProblemDetail handleBusinessValidation(final BusinessValidationException exception) {
        return createProblem(ApplicationError.VALIDATION_FAILED, exception);
    }

    // Required: without it, the catch-all below would turn a method-security denial into a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException exception) {
        return createProblem(ApplicationError.ACCESS_DENIED, exception);
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
        if (error.status().is5xxServerError()) {
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
controllers must not log the same exception before throwing it. Expected `4xx` conditions are logged
at `WARN` without a stack trace; unexpected `5xx` conditions are logged at `ERROR` with the
exception attached. The `errorCode` is attached as a structured field, not interpolated into the
message, so the message text stays a stable constant that groups across records.

Two handlers exist for reasons that are easy to miss:

- `AccessDeniedException` must be handled explicitly. Filter-level denials never reach an advice, but a method-security denial does, and the catch-all would otherwise report a `403` condition as a `500`.
- `@ExceptionHandler(Exception.class)` is the catch-all that guarantees every unexpected failure still produces a `ProblemDetail` rather than the default error page. The specific handlers inherited from `ResponseEntityExceptionHandler` take precedence over it, so framework exceptions keep their intended status.

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

Before adding handlers, inventory the exceptions that can cross each controller boundary and map every caller-visible category to a constant in the error catalog. Keep input-validation failures as `400`, but treat return-value validation as a server failure. Reuse shared exception categories when their public handling is identical, and add a catalog constant only for a condition with a distinct status, type, or response contract. Map `BusinessValidationException` to `400` only when it represents caller-correctable input, and never register a handler for `jakarta.validation.ValidationException`. If `ConstraintViolationException` can cross the boundary, distinguish argument violations from return-value or internal violations before choosing a status.

Normal REST TO responses use `application/json`; RFC 9457 error responses use
`application/problem+json`.

The catch-all `Exception` handler must not consume failures that Spring Security owns. Filter-level
authentication and access-denied failures never reach an advice: they are translated inside the
filter chain, so `401` responses and challenge headers remain the security configuration's
responsibility. Method-security denials do reach the advice, which is why `AccessDeniedException` is
handled explicitly above; without that handler the catch-all would report them as `500`. Apply
`application-security` for `401`, `403`, challenge headers, and any custom security body, and keep
the two contracts consistent so the same condition does not produce two different shapes.

Handle listener, job, messaging, and asynchronous failures at their owning boundary because they do
not pass through this advice, and log them there under the same one-record rule. Test each status,
problem type, content type, required header, and information-disclosure rule.

## The error contract in full

Use the project's existing error contract. For a new API on a supported Spring version, use RFC 9457
`ProblemDetail`.

The RFC 9457 `type` URI is the only machine-readable error identifier in the body. Never add a
parallel `code`, `errorCode`, or `errorId`: two identifiers for one condition guarantee that clients
branch on the wrong one. `title` and `detail` are human-readable and may change.
`project-naming-conventions` owns the URI form.

Declare every caller-visible failure once, as a constant in a single project-owned error catalog
carrying the status, the `type` URI, the title, the detail, and the internal code used in logs,
events, and metrics. One declaration keeps the public type and the internal code from drifting
apart; do not add a second holder for either.

`correlationId` is the one permitted extension member, because it identifies the request rather than
the failure and support workflows need it in the payload a caller pastes into a ticket. Keep
`traceId`, `spanId`, stack traces, exception class names, provider messages, internal hostnames,
SQL, internal endpoints, credentials, and personal data out of the body entirely.

- Map expected application failures explicitly, and let Spring's framework handler preserve standard REST error behavior where appropriate.
- A catch-all handler returns a generic message and logs the cause once. Never copy `exception.getMessage()` into a response unless that type guarantees a stable, user-safe message, and never log an expected 4xx as a server error.
- Name a project-owned validation exception unambiguously, for example `BusinessValidationException`. Never give a project exception the simple name of a framework type such as `jakarta.validation.ValidationException`, and never handle that framework type as if it were the project's category.
- Place custom exceptions in `exception` and MVC handler classes in `exception.handler`. Spring Security response handling belongs to the security configuration boundary, not this package.

The custom exception examples are in [infrastructure examples](infrastructure-examples.md).
