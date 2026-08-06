# Infrastructure Examples

Use these examples when deciding package placement or implementing method validation, custom exceptions, configuration properties, infrastructure beans, or anti-pattern remediation. Apply all rules from `../SKILL.md` and `modern-java-21`; imports are omitted.

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
│   ├── UserDomain.java
│   └── UserStatus.java            # Domain-owned enum
├── repository/                    # Data access
│   ├── UserRepository.java
│   ├── projection/                # Persistence projections, when introduced
│   │   └── UserSummaryProjection.java
│   └── specification/             # Reusable JPA Specifications, when introduced
│       └── UserSpecifications.java
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

```java
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(final String resource, final Long id) {
        super("%s not found with id: %d".formatted(
                Objects.requireNonNull(resource, "resource must not be null"),
                Objects.requireNonNull(id, "id must not be null")));
    }
}
```

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
