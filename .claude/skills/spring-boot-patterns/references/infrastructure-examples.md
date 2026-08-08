# Infrastructure Examples

Use these examples when deciding package placement or implementing method validation, custom exceptions, configuration properties, infrastructure beans, or anti-pattern remediation. Apply all rules from `../SKILL.md` and `modern-java-21`; imports are omitted.

An example marked as an excerpt shows the decision under discussion, not a complete type. Generate
the members it omits — accessors, constructors, and the rest of the contract — rather than copying
the excerpt verbatim into production code. When an omitted member is required for the code to
compile, such as a MapStruct-visible creation path, the example says so explicitly.

Snippets here follow the worked-example rules in `modern-java-21`: every identifier a snippet uses is declared in that snippet or attributed to the example that declares it, and an excerpt names any omitted member that the code depends on.

## Contents

- [Package layout](#package-layout)
- [Method validation](#method-validation)
- [Custom exceptions](#custom-exceptions)
- [Configuration properties and beans](#configuration-properties-and-beans)
- [Rejected code](#rejected-code)

## Package layout

```text
src/main/java/com/example/myapp/
├── MyAppApplication.java          # @SpringBootApplication
├── config/                        # Configuration classes
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── controller/                    # REST controllers
│   ├── ApiPaths.java              # Single declaration of the API base path
│   └── UserController.java
├── mapper/
│   ├── rest/                      # Domain -> response TO; justified request TO -> service input
│   │   └── UserRestMapper.java
│   └── domain/                    # Entity/projection -> domain; creation values -> new entity
│       └── UserDomainMapper.java
├── service/                       # Business logic and application contracts
│   ├── UserService.java
│   └── impl/                      # Implementations when the project uses this convention
│       └── UserServiceImpl.java
├── domain/                        # Framework-independent business models
│   ├── PaginationConstraints.java # Shared bounds used by REST and service contracts
│   ├── UserDomain.java
│   └── UserStatus.java            # Domain-owned enum
├── repository/                    # Data access
│   ├── UserRepository.java
│   ├── projection/                # Persistence projections, when introduced
│   │   └── UserSummaryProjection.java
│   └── specification/             # Reusable JPA Specifications, when introduced
│       └── UserSpecifications.java
├── entity/                        # JPA persistence entities
│   └── UserEntity.java
├── transferobject/               # Transfer objects
│   ├── request/
│   │   ├── UserCreateTO.java
│   │   └── UserUpdateTO.java
│   └── response/
│       ├── PageTO.java
│       └── UserTO.java
├── exception/                     # Custom exceptions and the error catalog
│   ├── ApplicationError.java      # Status, problem type, title, detail, internal code
│   ├── BusinessValidationException.java
│   ├── InvalidStateException.java
│   ├── ResourceNotFoundException.java
│   └── handler/                   # MVC REST exception handlers and advice
│       └── ApiExceptionHandler.java
└── util/                          # Focused, stateless helpers only
    └── DateRangeUtils.java
```

Use this layered layout consistently unless the repository already enforces a compatible layered
variation; do not migrate a coherent layout unless migration is explicitly in scope. The projection
and specification subpackages appear because the example contains corresponding types. Create either
subpackage only with its first type, never as empty scaffolding. Application service interfaces
and `service.impl` are shown because this example assumes that project convention. Follow the service
interface decision from `../SKILL.md`; do not create interfaces for helpers or types without a real
contract. A `util` package is valid for cohesive stateless utilities, but it must not become a
dumping ground for unrelated behavior.

## Shared route and bound constants

Declare the API base path once and build every controller route from it. Declare each shared numeric
bound once, in a framework-independent holder that both the REST boundary and the service contract
can reference, because Bean Validation annotations require compile-time constants.

```java
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    private ApiPaths() {
    }
}
```

```java
public final class PaginationConstraints {

    // Declared as text because @RequestParam(defaultValue = ...) accepts only a String constant.
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final int MAXIMUM_PAGE_SIZE = 100;

    private PaginationConstraints() {
    }
}
```

`PaginationConstraints` sits in `domain` because the controller and the service contract both depend
on that package already, and neither may depend on the other's boundary. Do not copy either value
into a second annotation, a Javadoc sentence, or a test literal.

## Method validation

Declare application-service method constraints once on the service contract:

```java
public interface TransferService {

    /**
     * Transfers the requested amount between accounts.
     *
     * @param sourceAccountId source account identifier; must not be {@code null}
     * @param targetAccountId target account identifier; must not be {@code null}
     * @param amount          amount to transfer; must be positive
     * @return the immutable {@link ReceiptDomain} for the completed transfer; never {@code null}
     * @throws ConstraintViolationException when an argument violates a structural constraint
     * @throws InsufficientFundsException when the source account cannot cover the transfer
     */
    ReceiptDomain transfer(
            @NotNull final Long sourceAccountId,
            @NotNull final Long targetAccountId,
            @NotNull @Positive final BigDecimal amount);
}
```

This example assumes that the project uses the service-interface and `*ServiceImpl` convention. Keep validation constraints on the interface and place `@Validated` on the concrete Spring bean. Do not repeat constraints on the overriding method.

With the concrete-service convention, there is no second place to split: declare the constraints and
the caller-facing Javadoc on the `@Service` class itself and annotate that class with `@Validated`.

```java
@Service
@Validated
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;

    public TransferServiceImpl(final AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public ReceiptDomain transfer(
            final Long sourceAccountId,
            final Long targetAccountId,
            final BigDecimal amount) {

        final AccountEntity sourceAccount = this.accountRepository.findById(sourceAccountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account", sourceAccountId));
        final AccountEntity targetAccount = this.accountRepository.findById(targetAccountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account", targetAccountId));

        sourceAccount.withdraw(amount);
        targetAccount.deposit(amount);

        final AccountEntity savedSourceAccount = this.accountRepository.save(sourceAccount);
        final AccountEntity savedTargetAccount = this.accountRepository.save(targetAccount);

        return new ReceiptDomain(
                savedSourceAccount.getId(),
                savedTargetAccount.getId(),
                amount);
    }
}
```

The explicit `save` calls are intentional; do not replace them with dirty-checking-only persistence. Apply `spring-data-jpa` and the project’s consistency rules for locking and concurrency. Invoke the service through the Spring proxy so validation and transaction advice are applied.

## Custom exceptions

Choose exception granularity from the handling contract, not from the number of failing rules.
`project-naming-conventions` owns that decision: a shared `ResourceNotFoundException` covers every
missing resource, and a more specific type appears only when the recovery, status, or problem type
genuinely differs.

The identifier is declared as `Object` so the same exception serves `Long`, `UUID`, `String`, and
composite identifiers without a second constructor per type.

```java
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(final String resource, final Object identifier) {
        super("%s not found with identifier: %s".formatted(
                Objects.requireNonNull(resource, "resource must not be null"),
                Objects.requireNonNull(identifier, "identifier must not be null")));
    }
}
```

Whether the identifier may appear in the exception message is a project decision recorded in the
security profile. It is safe when identifiers are opaque and non-enumerable, and unsafe when an
identifier is itself personal data such as an email address. The message stays internal in either
case; `ApiExceptionHandler` never copies it into the response body.

```java
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(final String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
    }
}
```

Name the project's validation category `BusinessValidationException`. Do not name it
`ValidationException`: that simple name collides with `jakarta.validation.ValidationException`, and
an unnoticed import of the framework type turns one focused handler into a catch-all for every
Bean Validation failure.

## Configuration properties and beans

```java
@ConfigurationProperties("clients.catalog")
@Validated
public record CatalogClientProperties(
        @NotNull URI baseUrl,
        @NotNull @DurationMin(millis = 1) Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) Duration responseTimeout) {
}
```

Validate required timeouts as strictly positive so invalid configuration fails during startup.

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

## Rejected code

```java
// Wrong: entity exposure, repository access, business logic, and time in controller.
@PostMapping
CustomerEntity customersPost(@RequestBody final CustomerEntity customer) {
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
// Wrong: empty application contract with no operation or responsibility.
interface CustomerService {
}

@Service
class CustomerServiceImpl implements CustomerService {
}
```
