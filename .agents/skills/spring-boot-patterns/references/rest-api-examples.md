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
    private static final UserRestMapper USER_REST_MAPPER = UserRestMapper.INSTANCE;

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserTO> usersPost(@RequestBody @Valid final UserCreateTO body) {
        
        final UserDomain createdUser = this.userService.create(
                body.username(), body.email(), body.password()
        );
        final UserTO response = USER_REST_MAPPER.mapUserDomainToUserTO(createdUser);
        final URI location = URI.create("%s/%d".formatted(USERS_PATH, response.id()));

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserTO> usersUserIdGet(@PathVariable final Long userId) {
        
        return ResponseEntity.ok(
                USER_REST_MAPPER.mapUserDomainToUserTO(
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
                USER_REST_MAPPER.mapUserPageToUserPageTO(users)
        );
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserTO> usersUserIdPut(@PathVariable final Long userId,
            @RequestBody @Valid final UserUpdateTO body) {

        return ResponseEntity.ok(
                USER_REST_MAPPER.mapUserDomainToUserTO(
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

This project uses URI major versioning under `/api/v1`. Keep controller routes, OpenAPI, gateways, and tests aligned when introducing a new version. Handler names must exactly match their OpenAPI `operationId`; exclude the common `/api/v1` prefix when deriving the name.

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

When a focused request input is justified by the service contract, the REST mapper may map the request TO to that domain/service input. It must not pass the TO itself to the service or hide business behavior in generated mapping.

## ProblemDetail exception handling

```java
@RestControllerAdvice
final class ApiExceptionHandler {

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

    @ExceptionHandler({
            ValidationException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class
    })
    ProblemDetail handleValidation() {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "The request contains invalid values.",
                "VALIDATION_FAILED");
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

Use shared exception categories when failures intentionally have the same public handling. Add a condition-specific handler only when it requires a distinct status, stable code, or response contract. Do not copy raw exception messages into responses.