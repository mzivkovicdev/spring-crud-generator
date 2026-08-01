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
│   ├── rest/                      # REST TO <-> domain mapping
│   │   └── UserRestMapper.java
│   └── domain/                    # Entity <-> domain mapping
│       └── UserDomainMapper.java
├── service/                       # Business logic and application contracts
│   ├── UserService.java
│   └── impl/                      # Application service implementations
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
└── util/                          # Focused, stateless helpers only
    └── DateUtils.java
```

Use this layered layout consistently unless the repository already enforces a compatible layered variation. Application service interfaces define the inbound application contract; their Spring implementations belong in `service.impl`. Do not create interfaces for helpers or types without that responsibility.

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

Annotate the concrete Spring bean with `@Validated` so Spring method validation is activated at the target type. Do not repeat or strengthen the interface constraints on the overriding method:

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
    public Receipt transfer(
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

        return new Receipt(
                savedSourceAccount.getId(),
                savedTargetAccount.getId(),
                amount);
    }
}
```

`TransferService` is the application contract used by inbound adapters; one current implementation does not make that boundary redundant. Invoke validated methods through the Spring proxy because self-invocation bypasses proxy-based method validation and other advice. Apply the persistence, locking, and concurrency rules from the owning data skill to the real transfer implementation.

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
        @NotNull Duration connectTimeout,
        @NotNull Duration responseTimeout) {
}
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

## Rejected code

```java
// Wrong: entity exposure, repository access, business logic, and time in controller.
@PostMapping
CustomerEntity customersPost(@RequestBody CustomerEntity customer) {
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
