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
└── util/                          # Focused, stateless helpers only
    └── DateUtils.java
```

Use this layout only when it matches the repository's established, sound layered structure. Preserve the responsibilities from `../SKILL.md` even when package names differ. Do not create an interface/implementation pair solely to mirror this package layout.

## Method validation

Use a service interface when it represents a meaningful application contract, module boundary, replaceable strategy, or operation with multiple implementations. Declare method constraints once on that contract:

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
public class AccountTransferService implements TransferService {

    private final AccountRepository accountRepository;

    public AccountTransferService(final AccountRepository accountRepository) {
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

This example treats `TransferService` as a justified application boundary. If the project has one local implementation and no meaningful contract boundary, use one concrete `@Service` class instead of creating an interface mechanically. In both cases, invoke validated methods through the Spring proxy; self-invocation bypasses proxy-based method validation and other advice. Apply the persistence, locking, and concurrency rules from the owning data skill to the real transfer implementation.

## Custom exceptions

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
interface CustomerService {
}

@Service
class CustomerServiceImpl implements CustomerService {
}
```