# REST API Examples

Use these examples when implementing or reviewing REST controllers, transport objects, REST mapping, validation responses, or API error handling. Apply all rules from `../SKILL.md`, `modern-java-21`, `project-naming-conventions`, and `application-security`; imports are omitted.

An example marked as an excerpt shows the decision under discussion, not a complete type. Generate
the omitted members rather than copying the excerpt verbatim.

Snippets here follow the worked-example rules in `modern-java-21`: every identifier a snippet uses is declared in that snippet or attributed to the example that declares it, and an excerpt names any omitted member that the code depends on.

## Contents

- [REST controller](#rest-controller)
- [Request and response TOs](#request-and-response-tos)
- [REST mapper](#rest-mapper)
- [ProblemDetail exception handling](#problemdetail-exception-handling)

## REST controller

The controller depends on `UserManagementApplicationService`, the use-case level declared in
[service and domain examples](service-domain-examples.md). It never injects an aggregate service or
a repository, so every request enters the domain through one place.

```java
@RestController
@RequestMapping(UserController.USERS_PATH)
public class UserController {

    public static final String USERS_PATH = ApiPaths.API_V1 + "/users";

    private final UserManagementApplicationService userManagement;
    private final UserService userService;

    public UserController(final UserManagementApplicationService userManagement,
                          final UserService userService) {
        this.userManagement = userManagement;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserTO> usersPost(@RequestBody @Valid final UserCreateTO body) {

        final UserDomain createdUser = this.userService.create(
                body.username(), body.email(), body.password()
        );
        final UserTO response = UserRestMapper.INSTANCE.mapUserDomainToUserTO(createdUser);
        final URI location = URI.create("%s/%d".formatted(USERS_PATH, response.id()));

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserTO> usersUserIdGet(@PathVariable final Long userId) {

        return ResponseEntity.ok(
                UserRestMapper.INSTANCE.mapUserDomainToUserTO(
                    this.userManagement.getProfile(userId)
                )
        );
    }

    @GetMapping
    public ResponseEntity<PageTO<UserTO>> usersGet(
            @RequestParam(defaultValue = "0")
            @PositiveOrZero final Integer pageNumber,
            @RequestParam(defaultValue = PaginationConstraints.DEFAULT_PAGE_SIZE)
            @Min(1) @Max(PaginationConstraints.MAXIMUM_PAGE_SIZE) final Integer pageSize) {

        final PageDomain<UserDomain> users = this.userService.getAll(pageNumber, pageSize);
        return ResponseEntity.ok(
                UserRestMapper.INSTANCE.mapUserPageToUserPageTO(users)
        );
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserTO> usersUserIdPut(@PathVariable final Long userId,
            @RequestBody @Valid final UserUpdateTO body) {

        return ResponseEntity.ok(
                UserRestMapper.INSTANCE.mapUserDomainToUserTO(
                    this.userService.updateById(userId, body.username(), body.email())
                )
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> usersUserIdDelete(@PathVariable final Long userId) {

        this.userService.deleteById(userId);
        return ResponseEntity.noContent().build();
    }
}
```

**These controller examples assume the code-first authoring direction**, where the controller owns its
route. Under contract-first the route lives on the generated API interface, the controller declares
no route constant and no `@RequestMapping`, and `ApiPaths` holds the constants that tests and
security matchers reference. `rest-api-contract` owns that decision; everything below the controller
— mapper, service, domain, error contract — is identical either way.

This project uses URI major versioning under `/api/v1`. The prefix is declared exactly once in Java,
as `ApiPaths.API_V1`, and under code-first each controller builds its own `public static final String`
route from it.
Tests, `Location` construction, and security matchers reuse those constants instead of repeating the
literal. Because `USERS_PATH` is a compile-time constant, `@RequestMapping(UserController.USERS_PATH)`
resolves at compile time; the qualified form is required here only because the annotation precedes
the field declaration.

In the OpenAPI document, the same prefix appears only in `servers.url`. Path Items stay
resource-relative, such as `/users/{userId}`, so the version never reaches `operationId` or the
handler method name: `GET /users/{userId}` maps to `usersUserIdGet`, never `apiV1UsersUserIdGet`.
Keep controller routes, OpenAPI, gateways, and tests aligned when introducing a new version.

`PaginationConstraints.MAXIMUM_PAGE_SIZE` is the single declaration of that bound. The service
contract references the same constant, so the REST boundary and the service contract cannot drift.

The mapper is stateless and dependency-free, so the controller uses its static MapStruct instance rather than DI. This POST creates an addressable resource, so `201 Created` and its server-owned `Location` URI are intentional; other POST semantics may use a different documented status.

The example intentionally relies on Spring Framework 6.1+ built-in controller method validation. Do not place `@Validated` on individual handler methods. If the supported framework version requires proxy-based controller method validation, place `@Validated` at type level only.

## Request and response TOs

```java
public record UserCreateTO(
        @NotBlank @Size(max = 120) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 128) String password) {
}
```

```java
public record UserUpdateTO(
        @NotBlank @Size(max = 120) String username,
        @NotBlank @Email @Size(max = 254) String email) {
}
```

```java
public record UserTO(
        Long id,
        String username,
        String email) {
}
```

```java
public record PageTO<T>(
        List<T> items,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages) {

    public PageTO {
        items = List.copyOf(items);
    }
}
```

The maximum password length is an input-resource bound, not a complete password policy. Apply the authentication policy from `application-security`; never persist or log the raw value.

## REST mapper

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserRestMapper {

    UserRestMapper INSTANCE = Mappers.getMapper(UserRestMapper.class);

    UserTO mapUserDomainToUserTO(final UserDomain domain);

    List<UserTO> mapUserDomainsToUserTOs(final List<UserDomain> domains);

    default PageTO<UserTO> mapUserPageToUserPageTO(final PageDomain<UserDomain> page) {
        return new PageTO<>(
                this.mapUserDomainsToUserTOs(page.items()),
                page.pageNumber(),
                page.pageSize(),
                page.totalElements(),
                page.totalPages());
    }
}
```

The default page method demonstrates example-specific composition inside the approved MapStruct
mapper. Apply the complete mapping policy from `../SKILL.md` before extending this mapper.

When a focused request input is justified by the service contract, the REST mapper may map the
request TO to that domain/service input. It must not pass the TO itself to the service or hide
business behavior in generated mapping.

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
