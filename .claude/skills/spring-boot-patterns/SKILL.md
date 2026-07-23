---
name: spring-boot-patterns
description: Production Spring Boot patterns for Java 21+ backend REST APIs, REST controllers, services, domain models, validation, transport objects (TOs), mapping, persistence, exception handling, configuration, security boundaries, observability, and feature packaging. Excludes server-side page rendering and UI views. Use for every new Spring Boot REST feature or modification to controllers, services, configuration, scheduled jobs, listeners, or API contracts.
---

# Spring Boot Patterns Skill

Implement vertical, tested features using the project's supported Spring Boot version. Preserve existing contracts, but do not copy legacy architecture or anti-patterns into new code.

## Relationship to modern-java-21

Apply the repository skill `modern-java-21` to every Java file touched by this workflow. Its rules for explicit types, complete Javadoc, imports, null safety, exceptions, class/method size, and tests are mandatory here as well.

`modern-java-21` owns Java language and source-style rules; this skill owns Spring Boot architecture and framework usage. The stricter compatible rule wins. In particular, never use `var`. Add Javadoc only for actual APIs and non-obvious contracts, as required by `modern-java-21`; the `public` or `protected` modifier alone does not require Javadoc.

This skill may retain established project choices such as explicit `final` parameters and local variables, `this.` for field access, TO terminology, and a layered package layout. Those choices supplement `modern-java-21`; they do not override it.

## REST-only scope

Build backend HTTP APIs only. The application may use Spring Web's servlet infrastructure internally, but that does not authorize generating a server-rendered presentation layer.

- Use `@RestController` and `@RestControllerAdvice` so handlers write status, headers, and serialized response bodies directly.
- Return typed transport objects, `ProblemDetail`, an explicitly supported file/resource response, or an empty response with the correct status.
- Use machine-readable media types defined by the API contract, normally JSON.
- Do not generate `@Controller`, `Model`, `ModelMap`, `ModelAndView`, `View`, view-name return values, redirects to rendered pages, view resolvers, template directories, or server-side UI flows.
- Using `WebClient` for an outgoing call does not make the server reactive. Do not migrate the server from the established servlet stack to WebFlux solely because `WebClient` is present.

## Rules before coding

1. Inspect `pom.xml` or Gradle files, the configured Java and Spring Boot versions, existing package layout, tests, configuration, migrations, security, and API error format.
2. Read the full call path affected by the change: controller/listener, service, domain, persistence, cache, and external adapters.
3. Define the acceptance cases, invalid input, missing data, conflicts, authorization, dependency failures, and transaction effects.
4. Design the smallest cohesive change. Do not perform unrelated modernization.
5. Implement production code and tests together.

## Mandatory Java file hygiene

For every `.java` file created or modified:

- remove unused and duplicate imports;
- never use wildcard imports;
- organize imports into sorted groups in this order: static, `java.*`, `jakarta.*`, `javax.*`, the current project's base package, `org.springframework.*`, then all remaining third-party imports;
- place exactly one blank line between non-empty groups and sort each group by the complete import statement;
- never use the `var` keyword; write the explicit local variable type;
- run the formatter or compile/static check before finishing.

## Project Structure

```
src/main/java/com/example/myapp/
├── MyAppApplication.java          # @SpringBootApplication
├── config/                        # Configuration classes
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── controller/                    # REST controllers
│   └── UserController.java
├── mapper/
│   ├── rest/                      # REST TO <-> domain mapping
│   │   └── UserRestMapper.java
│   └── domain/                    # Entity <-> domain mapping
│       └── UserDomainMapper.java
├── service/                       # Business logic
│   ├── UserService.java
│   └── impl/                      # Only for a justified interface/implementation split
│       └── UserServiceImpl.java
├── domain/                        # Framework-independent business models
│   └── UserDomain.java
├── repository/                    # Data access
│   └── UserRepository.java
├── model/                         # Persistence entities
│   └── UserEntity.java
├── transferobject/               # Transfer objects
│   ├── request/
│   │   ├── UserCreateTO.java
│   │   └── UserUpdateTO.java
│   └── response/
│       ├── PageTO.java
│       └── UserTO.java
├── exception/                     # Custom exceptions
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
└── util/                          # Focused, stateless helpers;
    └── DateUtils.java
```

If the repository uses a sound layered structure, integrate consistently without a broad package migration. Keep these responsibilities explicit regardless of package names:

| Boundary | Responsibility |
|---|---|
| REST Controller/listener | Parse and validate transport input, delegate, and map domain output to a TO |
| REST mapper | Translate between REST TOs and domain objects when object mapping is required |
| Service | Accept explicit method parameters or a justified parameter object, orchestrate the operation, return domain objects, and own the transaction boundary |
| Domain | Represent business state, invariants, and decisions without REST or persistence dependencies |
| Domain mapper | Translate between persistence entities and domain objects |
| Repository/adapter | Translate persistence or external-provider details |
| Configuration | Construct and configure infrastructure beans |

## REST Controller pattern

REST controllers must be thin. They must not query repositories, mutate entities, implement business rules, manage transactions, catch generic exceptions, or prepare server-rendered views.

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
    @Validated
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
                pageObject.getTotalPages(),
                pageObject.getTotalElements(),
                pageObject.getSize(),
                pageObject.getNumber(),
                this.userMapper.mapUserDomainToUserTOSimple(pageObject.getContent())
            )
        );
    }

    @PutMapping("/{id}")
    @Validated
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

Controller rules:

- Use `@RestController`; do not use a view-oriented `@Controller` for REST endpoints.
- Keep API versioning consistent with the existing public contract; do not introduce versioning arbitrarily.
- Use nouns in resource paths and correct HTTP methods/status codes.
- Define or preserve supported request and response media types. Return serialized bodies rather than view names.
- Validate path, query, headers, and body at the boundary.
- Define collection bounds, string lengths, numeric bounds, and pagination limits for untrusted input.
- On Spring Framework 6.1+, prefer built-in REST handler method validation and do not place `@Validated` on the controller. On earlier supported versions, use type-level `@Validated` only when proxy-based controller method validation is required. Never place it on an individual handler method.
- When controller parameters can trigger both object and method validation, preserve Spring's standard handling or map both validation exception types into the same public error contract.
- Use a `Location` header for resource creation when applicable.
- Return typed response models, not entities, `Map<String, Object>`, or `ResponseEntity<?>`.
- If the project has an OpenAPI contract, update and validate it with the implementation; do not allow endpoint, schema, status, or media-type drift.
- Preserve backward compatibility in field names, enum values, requiredness, null behavior, status codes, and error shapes.

## Request and response TOs

TO means transport object in this skill. Use records for immutable request and response TOs when compatible with the serializer and project conventions.

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

### MapStruct Mapper
```java
@Mapper
public interface UserRestMapper {

    UserTO mapUserDomainToUserTO(final UserDomain domain);

    List<UserTO> mapUserDomainToUserTOSimple(final List<UserDomain> domains);
}
```

```java
@Mapper
public interface UserDomainMapper {

    UserDomain mapUserEntityToUserDomain(final UserEntity entity);

    List<UserDomain> mapUserEntitiesToUserDomains(final List<UserEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    UserEntity mapToUserEntity(
            final String username,
            final String email,
            final String password);
}
```

When a service operation would otherwise require seven or more parameters, a focused domain parameter object is allowed:

```java
public record UserUpdateDomain(
        String username,
        String email,
        String password,
        Details details,
        List<String> roles,
        List<String> permissions) {
}
```

```java
public interface UserService {

    UserDomain updateById(
            final Long userId,
            final UserUpdateDomain userUpdate);
}
```

In this example, `userId` plus the six update values would otherwise produce a seven-parameter method. The REST mapper may map the corresponding request TO to `UserUpdateDomain`; the service and domain mapper must not depend on that TO.

- Never accept or return a JPA entity as an HTTP/message TO.
- Do not pass request or response TOs into the service layer.
- Map service results from `UserDomain` to `UserTO` in the REST mapper.
- Map `UserEntity` to `UserDomain` in the domain mapper used by the service.
- Keep transport validation on request TOs and business invariants in domain/service code.
- Do not put repositories or services in TOs/mappers.
- Normalize only when the contract permits it; do not silently change user data.
- Model PATCH semantics explicitly so absent, clear, and set are not confused.
- Use MapStruct as the default for structural mapping when it is available in the project. Use handwritten mapping for behavior, non-trivial normalization, or mapping that is clearer without generated code.

## Domain model

Domain models are independent of REST TOs and JPA entities. Services return domain models, controllers map them to response TOs, and repositories continue to work with persistence entities.

```java
public record UserDomain(
        Long id,
        String username,
        String email) {
}
```

- Do not add JPA, HTTP, JSON, controller, repository, or Spring infrastructure concerns to a domain model.
- Keep the domain model immutable when practical.
- Put business invariants and behavior in the domain when they naturally belong to the represented business concept.
- Do not expose `UserEntity` outside the service/persistence boundary.
- Do not create a domain type that merely aliases a TO; the two models may look similar but belong to different boundaries and may evolve independently.

## Service pattern

Services implement operations and own orchestration. The following interface/implementation example assumes `UserService` is an intentional service boundary. If that is not true in the current project, use one concrete `UserService` class and remove the interface and `impl` package.

```java
// Interface
public interface UserService {

    /**
     * Returns a user by identifier.
     *
     * @param id user identifier; must not be {@code null}
     * @return   the matching user; never {@code null}
     * @throws ResourceNotFoundException when no user exists for the supplied identifier
     */
    UserDomain getById(final Long id);

    /**
     * Creates a user.
     *
     * @param username username; must satisfy the validated API contract
     * @param email    email address; must satisfy the validated API contract
     * @param password raw password; must satisfy the validated API contract
     * @return         the created domain user; never {@code null}
     */
    UserDomain create(final String username, final String email, final String password);

    /**
     * Returns a page of users.
     *
     * @param pageNumber zero-based page number; must not be negative
     * @param pageSize   requested page size; must be positive and within the supported maximum
     * @return           the requested page of users; never {@code null}
     */
    Page<UserDomain> getAll(final Integer pageNumber, final Integer pageSize);

    /**
     * Updates a user.
     *
     * @param id       user identifier; must not be {@code null}
     * @param username username; must satisfy the validated API contract
     * @param email    email address; must satisfy the validated API contract
     * @param password raw password; must satisfy the validated API contract
     * @return         the updated user; never {@code null}
     * @throws ResourceNotFoundException when no user exists for the supplied identifier
     */
    UserDomain updateById(
            final Long id,
            final String username,
            final String email,
            final String password);

    /**
     * Deletes a user by identifier.
     *
     * @param id user identifier; must not be {@code null}
     * @throws ResourceNotFoundException when no user exists for the supplied identifier
     */
    void deleteById(final Long id);

}

// Implementation
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserDomainMapper userMapper = Mappers.getMapper(UserDomainMapper.class);
    private final UserRepository userRepository;

    /** {@inheritDoc} */
    @Override
    public UserDomain getById(final Long id) {
        return this.userRepository.findById(id)
            .map(this.userMapper::mapUserEntityToUserDomain)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional  // Write transaction
    public UserDomain create(
            final String username,
            final String email,
            final String password) {

        final UserEntity user = this.userMapper.mapToUserEntity(
                username, email, password
        );
        final UserEntity saved = this.userRepository.saveAndFlush(user);

        return this.userMapper.mapUserEntityToUserDomain(saved);
    }

    /** {@inheritDoc} */
    @Override
    public Page<UserDomain> getAll(final Integer pageNumber, final Integer pageSize) {

        final Pageable pageable = PageRequest.of(pageNumber, pageSize);

        return this.userRepository.findAll(pageable)
            .map(this.userMapper::mapUserEntityToUserDomain);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UserDomain updateById(
            final Long id,
            final String username,
            final String email,
            final String password) {

        final UserEntity existing = this.getEntityById(id);

        existing.setUsername(username)
            .setEmail(email)
            .setPassword(password);

        final UserEntity updated = this.userRepository.saveAndFlush(existing);

        return this.userMapper.mapUserEntityToUserDomain(updated);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteById(final Long id) {

        final UserEntity existing = this.getEntityById(id);
        this.userRepository.delete(existing);
    }

    private UserEntity getEntityById(final Long id) {
        return this.userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
```

Service rules:

- Create an interface only for a meaningful port, multiple implementations, strategy, or external boundary.
- Use Lombok constructor generation only when Lombok is an established project dependency and the generated constructor remains obvious; otherwise write the constructor explicitly.
- Do not accept REST request/response TOs and do not return JPA entities.
- Return domain objects such as `UserDomain`; map entities to domain objects before crossing the service boundary.
- For update operations, load the existing entity, apply changes through explicit entity setters in the service, call `saveAndFlush`, and map the saved entity to a domain object. Do not use a MapStruct `@MappingTarget` method to mutate an existing entity.
- Prefer explicit separate parameters for service operations. Do not create a parameter object merely to wrap fewer than seven method parameters.
- Count every declared method parameter, including identifiers, collections, and existing value objects. When an operation would require seven or more parameters, a cohesive parameter object is allowed and normally preferred.
- Keep an identifier as a separate parameter when it identifies the target resource; group the remaining values in the parameter object.
- Place a service parameter object in the domain/service model boundary, name it for the represented operation or values, and keep it independent of REST and JPA. Do not introduce `Command` or `View` terminology by default.
- Do not split naturally cohesive value objects such as `Details` into scalar parameters merely to satisfy this rule.
- Do not pass raw passwords, tokens, or secrets beyond the narrow boundary that hashes, encrypts, or exchanges them. Never persist or log their raw values.
- Keep business rules out of controller, mapper, repository, and entity callback code.
- Place `@Transactional` on public service methods invoked through the Spring proxy.
- Do not rely on self-invocation for `@Transactional`, `@Async`, `@Cacheable`, method validation, or other proxy advice.
- Keep database transactions short. Do not make slow external calls while holding a transaction unless the consistency design explicitly requires it.
- Use `readOnly = true` for read services when it is compatible with the persistence implementation; treat it as an optimization hint, not security.
- Do not add `@Transactional` mechanically to every service class.

## Repository Patterns

### JPA Repository
```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Derived query
    /**
     * Finds a user by email address.
     *
     * @param email normalized email address; must not be {@code null}
     * @return the matching user, or empty when no user exists; never {@code null}
     */
    Optional<UserEntity> findByEmail(final String email);

    // Custom query
    /**
     * Finds users belonging to a department.
     *
     * @param departmentId department identifier; must not be {@code null}
     * @return matching users; never {@code null}
     */
    @Query("SELECT u FROM UserEntity u WHERE u.department.id = :departmentId")
    List<UserEntity> findByDepartmentId(@Param("departmentId") final Long departmentId);

    // Native query (use sparingly)
    /**
     * Finds users created after the supplied date.
     *
     * @param date lower creation-date boundary; must not be {@code null}
     * @param pageable requested page and sort; must not be {@code null}
     * @return matching users; never {@code null}
     */
    @Query(
            value = "SELECT * FROM users WHERE created_at > :date",
            nativeQuery = true)
    Page<UserEntity> findRecentUsers(@Param("date") final LocalDate date, final Pageable pageable);

    // Exists check (more efficient than findBy)
    /**
     * Checks whether a user exists with the supplied email address.
     *
     * @param email normalized email address; must not be {@code null}
     * @return {@code true} when a matching user exists; otherwise {@code false}
     */
    boolean existsByEmail(final String email);

}
```

### Repository Best Practices

- Use derived queries when they remain readable and unambiguous.
- Return `Optional` for an optional single result; never accept `Optional` as a parameter or entity field.
- Use `existsBy` instead of loading an entity for existence checks.
- Avoid native queries unless JPQL, Criteria, or a repository abstraction cannot express the required behavior clearly.
- Use `@EntityGraph`, projections, or explicit fetch joins to solve measured fetch-plan problems; do not default every association to eager loading.

## Validation

Use Jakarta Bean Validation for structural constraints:

```java
@Validated
public interface TransferService {

    /**
     * Transfers the requested amount between accounts.
     *
     * @param sourceAccountId source account identifier; must not be {@code null}
     * @param targetAccountId target account identifier; must not be {@code null}
     * @param amount          amount to transfer; must be positive
     * @return the immutable {@link Receipt} for the completed transfer; never {@code null}
     * @throws ConstraintViolationException when an argument violates a structural constraint
     * @throws InsufficientFundsException when the source account cannot cover the transfer
     */
    Receipt transfer(
            @NotNull final Long sourceAccountId,
            @NotNull final Long targetAccountId,
            @NotNull @Positive final BigDecimal amount);
}
```

- Use `@Valid` for nested object validation.
- Use method validation when the service can be called outside the REST request boundary.
- Create a custom constraint only for reusable structural validation; keep database-dependent and business validation in a service/domain policy.
- Error messages exposed to users must be stable and safe. Do not expose implementation class names or SQL/provider details.

## Error handling with ProblemDetail

Use the project's existing error contract. For a new API on a supported Spring version, prefer RFC 9457 `ProblemDetail` with stable application error codes.

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

- Map expected application failures explicitly.
- Let Spring's framework handler preserve standard REST error behavior where appropriate.
- Avoid a catch-all handler that leaks exception messages. If a top-level handler is required, return a generic message and log the cause once.
- Do not copy `exception.getMessage()` into a response unless that exception type guarantees a stable, user-safe message.
- Do not log expected 4xx validation/not-found failures as server errors.
- Never include stack traces, SQL, internal endpoints, credentials, or personal data in responses.

### Custom Exceptions
```java
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(final String resource, final Long id) {
        super("%s not found with id: %d".formatted(
                        Objects.requireNonNull(resource, "resource must not be null"),
                        Objects.requireNonNull(id, "id must not be null")));
    }
}

public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(final String code, final String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public String code() {
        return this.code;
    }
}
```


## Configuration properties

Use type-safe, validated configuration instead of scattered `@Value` fields.

```java
@ConfigurationProperties("clients.catalog")
@Validated
public record CatalogClientProperties(
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration responseTimeout) {}
```

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogClientProperties.class)
class CatalogClientConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
```

- Use environment variables or a secret manager for secrets; never commit credentials.
- Validate required configuration at startup.
- Do not hardcode environment URLs, AWS regions, bucket names, timeouts, or feature behavior in production code.
- Keep the main `@SpringBootApplication` class minimal; place feature configuration in focused classes.
- Avoid `proxyBeanMethods = true` unless inter-bean method proxying is required.

## Security boundary

- Authentication is not authorization. Enforce resource/tenant ownership in the use case or repository query, not only in the controller.
- Use method or request authorization consistent with the project's Spring Security model.
- Never trust tenant/user identifiers supplied in a body when the authenticated principal determines them.
- Apply least privilege to data access, AWS clients, and operational endpoints.
- Do not disable CSRF, CORS, authentication, or security filters just to make a test pass.
- Do not log tokens, cookies, authorization headers, or sensitive payloads.

## Observability

- Log significant boundaries and outcomes with correlation identifiers.
- Add metrics/traces around slow or failure-prone external boundaries and important business outcomes.
- Avoid high-cardinality metric tags such as raw user ID, order ID, URL, exception message, or email.
- Do not log every method entry/exit.
- Health indicators must reflect actionable dependency state and must not expose secrets.

## Scheduled and asynchronous work

- Make scheduled jobs idempotent and safe when multiple application instances run.
- Use a distributed lock or database claim pattern when a job must run once across the cluster.
- Bound batches and memory usage; persist progress/checkpoints for large work.
- Configure executors explicitly where concurrency matters.
- Propagate context intentionally and handle failures; never fire-and-forget critical work silently.
- Evaluate virtual threads only after confirming blocking model, pinning, connection pools, and operational behavior.

## Tests required with every feature

At minimum, decide and implement the relevant levels:

- pure unit test for domain/service decisions;
- REST controller slice test for routing, serialization, content type, validation, status, security, and errors; use `@WebMvcTest` with `MockMvc` or `MockMvcTester` for the servlet stack already used by the project;
- persistence integration test for queries, constraints, transaction behavior, and MySQL semantics;
- client integration test for external failure, timeout, retry, and response handling;

Every bug fix needs a regression test that demonstrates the previous failure.

## Anti-patterns

```java
// Wrong: entity exposure, repository access, business logic, and time in controller.
@PostMapping
CustomerEntity create(@RequestBody CustomerEntity customer) {
    customer.setCreatedAt(Instant.now());
    return customerRepository.save(customer);
}
```

```java
// Wrong: field injection.
@Autowired
private CustomerRepository customerRepository;
```

```java
// Wrong: meaningless interface/implementation pair with no boundary.
interface CustomerService {}
@Service
class CustomerServiceImpl implements CustomerService {}
```

Reject:

- fat controllers;
- entities in API contracts;
- entities returned from services;
- REST TOs passed into services;
- field injection;
- service interfaces created by habit;
- wrapper parameter objects introduced solely to reduce a service method with fewer than seven parameters;
- service methods with seven or more unrelated parameters when a cohesive parameter object would make the contract clearer;
- generic `Map` responses;
- hardcoded configuration/secrets;
- self-invocation assumptions for proxy annotations;
- generic exception swallowing;
- unbounded collection endpoints;
- remote I/O inside long transactions;
- features without tests;
- touched Java files with unused, wildcard, duplicate, or unordered imports.

## Completion checklist

- [ ] Controller/listener is a thin transport boundary.
- [ ] Business rules are in service/domain code.
- [ ] TOs are explicit, validated, controller-owned, and separate from domain models and entities.
- [ ] Services accept explicit parameters, use a parameter object only for justified 7+ parameter operations, and return domain models.
- [ ] REST and domain mappers preserve the TO–Domain–Entity boundaries.
- [ ] Transactions and security ownership are explicit.
- [ ] Error responses are stable and safe.
- [ ] Configuration is type-safe, externalized, and validated.
- [ ] Tests cover successful and unsuccessful behavior.
- [ ] Every touched Java file has clean, correctly ordered imports.
- [ ] Relevant formatter, tests, and build checks pass.