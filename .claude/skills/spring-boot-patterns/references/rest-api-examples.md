# REST API Examples

Use these examples when implementing or reviewing REST controllers, transport objects, REST mapping, validation responses, or API error handling. Apply all rules from `../SKILL.md`, `modern-java-21`, `project-naming-conventions`, and `application-security`; imports are omitted.

An example marked as an excerpt shows the decision under discussion, not a complete type. Generate
the omitted members rather than copying the excerpt verbatim.

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

    public static final String USERS_PATH = ApiPaths.API_V1 + "/users";

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

This project uses URI major versioning under `/api/v1`. The prefix is declared exactly once in Java,
as `ApiPaths.API_V1`, and each controller builds its own `public static final String` route from it.
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

```java
public final class ProblemTypes {

    private static final String PROBLEM_BASE = "https://api.acme.example/problems/";

    public static final URI INVALID_STATE = URI.create(PROBLEM_BASE + "invalid-state");
    public static final URI RESOURCE_NOT_FOUND = URI.create(PROBLEM_BASE + "resource-not-found");
    public static final URI RESPONSE_VALIDATION_FAILED =
            URI.create(PROBLEM_BASE + "response-validation-failed");
    public static final URI VALIDATION_FAILED = URI.create(PROBLEM_BASE + "validation-failed");

    private ProblemTypes() {
    }
}
```

```java
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound() {
        return createProblem(
                HttpStatus.NOT_FOUND,
                ProblemTypes.RESOURCE_NOT_FOUND,
                "Resource not found",
                "The requested resource does not exist.");
    }

    @ExceptionHandler(InvalidStateException.class)
    public ProblemDetail handleInvalidState() {
        return createProblem(
                HttpStatus.CONFLICT,
                ProblemTypes.INVALID_STATE,
                "Invalid resource state",
                "The operation is not allowed in the current resource state.");
    }

    // Project-owned category for caller-correctable validation failures.
    @ExceptionHandler(BusinessValidationException.class)
    public ProblemDetail handleBusinessValidation() {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.VALIDATION_FAILED,
                "Validation failed",
                "The request contains invalid values.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception,
            final HttpHeaders headers,
            final HttpStatusCode ignoredStatus,
            final WebRequest request) {

        final ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.VALIDATION_FAILED,
                "Validation failed",
                "The request contains invalid values.");

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
                responseValidationFailed
                        ? ProblemTypes.RESPONSE_VALIDATION_FAILED
                        : ProblemTypes.VALIDATION_FAILED,
                responseValidationFailed ? "Response validation failed" : "Validation failed",
                responseValidationFailed
                        ? "The server could not produce a valid response."
                        : "The request contains invalid values.");

        return this.handleExceptionInternal(
                exception, problem, headers, responseStatus, request);
    }

    private static ProblemDetail createProblem(
            final HttpStatus status,
            final URI type,
            final String title,
            final String detail) {

        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);

        return problem;
    }
}
```

The RFC 9457 `type` URI is the only machine-readable identifier in the response body. There is no
parallel `code` member: two identifiers for one condition guarantee that some client branches on the
wrong one, and `title` and `detail` are human-readable text that may change without a contract
change. Declare every type URI once in `ProblemTypes` so the same condition cannot acquire two
spellings. `project-naming-conventions` owns the URI form and its migration rules.

Use one project-owned `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` as the MVC
error-contract owner. It preserves Spring MVC handling for malformed requests, unsupported methods
and media types, binding failures, and other framework exceptions; override only cases that require
the project's stable problem contract. Preserve a coherent existing alternative instead of adding a
second overlapping global handler.

Place this advice in `<base-package>.exception.handler`. Keep the exceptions it handles in
`<base-package>.exception`; do not place the advice directly beside them.

Before adding handlers, inventory the exceptions that can cross each controller boundary and map every caller-visible category to the correct HTTP status and stable problem type. Keep input-validation failures as `400`, but treat return-value validation as a server failure. Reuse shared exception categories when their public handling is identical, and add a condition-specific handler only for a distinct status, problem type, or response contract. Map `BusinessValidationException` to `400` only when it represents caller-correctable input, and never register a handler for `jakarta.validation.ValidationException`. If `ConstraintViolationException` can cross the boundary, distinguish argument violations from return-value or internal violations before choosing a status.

Normal REST TO responses use `application/json`; RFC 9457 error responses use
`application/problem+json`. The example intentionally omits a broad `Exception` handler. Route
unknown failures through the project's approved top-level error path; it must return a safe `500`
without consuming authentication or access-denied failures owned by Spring Security. Apply
`application-security` for `401`, `403`, challenge headers, and any custom security body. Handle
listener, job, messaging, and asynchronous failures at their owning boundary because they do not
pass through this advice. Test each status, problem type, content type, required header, and
information-disclosure rule.
