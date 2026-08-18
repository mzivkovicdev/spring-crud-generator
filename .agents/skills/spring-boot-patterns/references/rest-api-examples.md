# REST API Examples

Use these examples when implementing or reviewing REST controllers, transport objects, REST mapping, validation responses, or API error handling. Apply all rules from `../SKILL.md`, `modern-java-21`, `project-naming-conventions`, and `application-security`; imports are omitted.

An example marked as an excerpt shows the decision under discussion, not a complete type. Generate
the omitted members rather than copying the excerpt verbatim.

Snippets here follow the worked-example rules in `modern-java-21`: every identifier a snippet uses is declared in that snippet or attributed to the example that declares it, and an excerpt names any omitted member that the code depends on.

## Contents

- [REST controller](#rest-controller)
- [Request and response TOs](#request-and-response-tos)
- [REST mapper](#rest-mapper)

## REST controller

The controller injects both service levels declared in
[service and domain examples](service-domain-examples.md), and each handler calls exactly one of
them. `usersPost` and `usersUserIdGet` touch the user and organization aggregates together, so they
go through `UserManagementApplicationService`. Listing, updating, and deleting stay inside the
`users` aggregate, so they call `UserService` directly rather than through a forwarding method.

```java
@RestController
@RequestMapping(UserController.USERS_PATH)
public class UserController {

    public static final String USERS_PATH = ApiPaths.API_V1 + "/users";

    private final UserManagementApplicationService userManagement;
    private final UserService userService;

    public UserController(
            final UserManagementApplicationService userManagement,
            final UserService userService) {

        this.userManagement = userManagement;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserTO> usersPost(@RequestBody @Valid final UserCreateTO body) {

        final UserDomain createdUser = this.userManagement.register(
                body.organizationId(), body.username(), body.email(), body.password()
        );
        final UserTO response = UserRestMapper.INSTANCE.mapUserDomainToUserTO(createdUser);
        final URI location = URI.create("%s/%d".formatted(USERS_PATH, response.id()));

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileTO> usersUserIdGet(@PathVariable final Long userId) {

        return ResponseEntity.ok(
                UserRestMapper.INSTANCE.mapUserProfileDomainToUserProfileTO(
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

The example relies on built-in controller method validation, available on every framework version the supported Spring Boot generations use. Do not place `@Validated` on individual handler methods; on a legacy branch that requires proxy-based controller method validation, place it at type level only.

## Request and response TOs

```java
public record UserCreateTO(
        @NotNull Long organizationId,
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
public record UserProfileTO(
        Long id,
        String username,
        String email,
        String organizationName) {
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

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    UserProfileTO mapUserProfileDomainToUserProfileTO(final UserProfileDomain profile);

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

## Error handling examples

The `ProblemDetail` handler, the error catalog, and the custom exception types live in
[error handling examples](error-handling-examples.md). Read that file when a change adds or alters a
failure the API exposes; a change that only adds a handler using existing error constants does not
need it.
