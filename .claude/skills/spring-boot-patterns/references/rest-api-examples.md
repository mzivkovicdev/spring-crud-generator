# REST API Examples

Use these examples when implementing or reviewing REST controllers, transport objects, REST mapping, validation responses, or API error handling. Apply all rules from `../SKILL.md`, `modern-java-21`, `project-naming-conventions`, and `application-security`; imports are omitted.

## Contents

- [REST controller](#rest-controller)
- [Request and response TOs](#request-and-response-tos)
- [REST mapper](#rest-mapper)
- [ProblemDetail exception handling](#problemdetail-exception-handling)

## REST controller

```java
@RestController
@RequestMapping(UserController.USERS_PATH)
public class UserController {

    private static final String USERS_PATH = "/api/v1/users";
    private static final int MAXIMUM_PAGE_SIZE = 100;
    private final UserService userService;

    public UserController(final UserService userService) {
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
                    this.userService.getById(userId)
                )
        );
    }

    @GetMapping
    public ResponseEntity<PageTO<UserTO>> usersGet(
            @RequestParam(defaultValue = "0") @PositiveOrZero final Integer pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAXIMUM_PAGE_SIZE) final Integer pageSize) {

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

This project uses URI major versioning under `/api/v1`. Declare that common prefix in the OpenAPI `servers.url`, define Path Items as resource paths such as `/users/{userId}`, and derive both `operationId` and controller method name from the Path Item plus HTTP method. Therefore, `GET /users/{userId}` maps to `usersUserIdGet`. Keep controller routes, OpenAPI, gateways, and tests aligned when introducing a new version.

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

```java
@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound() {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "The requested resource does not exist.",
                "RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler(InvalidStateException.class)
    ProblemDetail handleInvalidState() {
        return createProblem(
                HttpStatus.CONFLICT,
                "Invalid resource state",
                "The operation is not allowed in the current resource state.",
                "INVALID_STATE");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthenticationRequired() {
        return createProblem(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "Authentication is required to access this resource.",
                "AUTHENTICATION_REQUIRED");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied() {
        return createProblem(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "The authenticated principal cannot perform this operation.",
                "ACCESS_DENIED");
    }

    // Project-owned category for caller-correctable validation failures.
    @ExceptionHandler(ValidationException.class)
    ProblemDetail handleValidation() {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "The request contains invalid values.",
                "VALIDATION_FAILED");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception,
            final HttpHeaders headers,
            final HttpStatusCode ignoredStatus,
            final WebRequest request) {

        final ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "The request contains invalid values.",
                "VALIDATION_FAILED");

        return this.handleExceptionInternal(
                exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            final HandlerMethodValidationException exception,
            final HttpHeaders headers,
            final HttpStatusCode ignoredStatus,
            final WebRequest request) {

        final boolean responseValidationFailed = exception.isForReturnValue();
        final HttpStatus responseStatus = responseValidationFailed
                ? HttpStatus.INTERNAL_SERVER_ERROR
                : HttpStatus.BAD_REQUEST;
        final ProblemDetail problem = createProblem(
                responseStatus,
                responseValidationFailed ? "Response validation failed" : "Validation failed",
                responseValidationFailed
                        ? "The server could not produce a valid response."
                        : "The request contains invalid values.",
                responseValidationFailed
                        ? "RESPONSE_VALIDATION_FAILED"
                        : "VALIDATION_FAILED");

        return this.handleExceptionInternal(
                exception, problem, headers, responseStatus, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(final Exception exception) {
        LOGGER.error("Unhandled REST request failure", exception);
        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "The request could not be completed.",
                "INTERNAL_SERVER_ERROR");
    }

    private static ProblemDetail createProblem(
            final HttpStatus status,
            final String title,
            final String detail,
            final String code) {

        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);

        return problem;
    }
}
```

Use one project-owned `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` as the MVC
error-contract owner. It preserves Spring MVC handling for malformed requests, unsupported methods
and media types, binding failures, and other framework exceptions; override only cases that require
the project's stable problem contract. Preserve a coherent existing alternative instead of adding a
second overlapping global handler.

Place this advice in `<base-package>.exception.handler`. Keep the exceptions it handles in
`<base-package>.exception`; do not place the advice directly beside them.

Before adding handlers, inventory the exceptions that can cross each controller boundary and map every caller-visible category to the correct HTTP status and stable code. Keep input-validation failures as `400`, but treat return-value validation as a server failure. Reuse shared exception categories when their public handling is identical, and add a condition-specific handler only for a distinct status, code, or response contract. Map the project-owned `ValidationException` to `400` only when it represents caller-correctable input; do not catch `jakarta.validation.ValidationException` broadly. If `ConstraintViolationException` can cross the boundary, distinguish argument violations from return-value or internal violations before choosing a status.

Normal REST TO responses use `application/json`; RFC 9457 error responses use
`application/problem+json`. The final `Exception` handler is a safe fallback, not a substitute for
known mappings. Log unexpected failures under the security logging policy and never expose raw
exception messages or stack traces. The security handlers above cover failures that reach MVC
advice; failures raised in the Spring Security filter chain require security-owned response handlers
with the same public problem format. Handle listener, job, messaging, and asynchronous failures at
their owning boundary because they do not pass through this advice. Test each status, code, content
type, and information-disclosure rule.
