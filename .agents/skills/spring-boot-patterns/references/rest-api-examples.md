# REST API Examples

Use these examples when implementing or reviewing REST controllers, transport objects, REST mapping, validation responses, or API error handling. Apply all rules from `../SKILL.md` and `modern-java-21`; imports are omitted.

## Contents

- [REST controller](#rest-controller)
- [Request and response TOs](#request-and-response-tos)
- [REST mapper](#rest-mapper)
- [ProblemDetail exception handling](#problemdetail-exception-handling)

## REST controller

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRestMapper userMapper = Mappers.getMapper(UserRestMapper.class);
    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserTO> usersPost(@RequestBody @Valid final UserCreateTO body) {

        return ResponseEntity.ok(
            this.userMapper.mapUserDomainToUserTO(
                this.userService.create(
                    body.username(), body.email(), body.password()
                )
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserTO> usersIdGet(@PathVariable final Long id) {

        return ResponseEntity.ok(
            this.userMapper.mapUserDomainToUserTO(
                this.userService.getById(id)
            )
        );
    }

    @GetMapping
    public ResponseEntity<PageTO<UserTO>> usersGet(
            @RequestParam final Integer pageNumber,
            @RequestParam final Integer pageSize) {

        final Page<UserDomain> pageObject = this.userService.getAll(
                pageNumber, pageSize
        );

        return ResponseEntity.ok().body(
            new PageTO<>(
                pageObject.getTotalPages(), pageObject.getTotalElements(),
                pageObject.getSize(), pageObject.getNumber(),
                this.userMapper.mapUserDomainToUserTOSimple(pageObject.getContent())
            )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserTO> usersIdPut(
            @PathVariable final Long id,
            @RequestBody @Valid final UserUpdateTO body) {

        return ResponseEntity.ok(
            this.userMapper.mapUserDomainToUserTO(
                this.userService.updateById(
                    id, body.username(), body.email(), body.password()
                )
            )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> usersIdDelete(@PathVariable final Long id) {

        this.userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

The example intentionally relies on Spring Framework 6.1+ built-in controller method validation. Do not place `@Validated` on individual handler methods. If the supported framework version requires proxy-based controller method validation, place `@Validated` at type level only.

## Request and response TOs

```java
public record UserCreateTO(
        @NotBlank @Size(max = 120) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank String password) {
}
```

```java
public record UserTO(
        Long id,
        String username,
        String email) {
}
```

## REST mapper

```java
@Mapper
public interface UserRestMapper {

    UserTO mapUserDomainToUserTO(final UserDomain domain);

    List<UserTO> mapUserDomainToUserTOSimple(final List<UserDomain> domains);
}
```

When an operation would otherwise require seven or more service parameters, the REST mapper may map the request TO to the focused domain/service parameter object defined for that operation. It must not pass the TO itself to the service.

## ProblemDetail exception handling

```java
@RestControllerAdvice
final class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(final CustomerNotFoundException exception) {

        final ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Customer not found");
        problem.setDetail("The requested customer does not exist.");
        problem.setProperty("code", "CUSTOMER_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
```