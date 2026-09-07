# Infrastructure Examples

Use these examples when deciding package placement or implementing method validation, custom exceptions, configuration properties, infrastructure beans, or anti-pattern remediation. Apply all rules from `../SKILL.md` and `modern-java-21`; imports are omitted.

An example marked as an excerpt shows the decision under discussion, not a complete type. Generate
the members it omits — accessors, constructors, and the rest of the contract — rather than copying
the excerpt verbatim into production code. When an omitted member is required for the code to
compile, such as a MapStruct-visible creation path, the example says so explicitly.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

- [Package layout](#package-layout)
- [Shared route and bound constants](#shared-route-and-bound-constants)
- [Method validation](#method-validation)
- [Custom exceptions](#custom-exceptions)
- [Configuration properties and beans](#configuration-properties-and-beans)
- [Rejected code](#rejected-code)
- [Idempotency placement](#idempotency-placement)
- [Scheduled and asynchronous execution](#scheduled-and-asynchronous-execution)

## Package layout

```text
src/main/java/com/example/myapp/
├── MyAppApplication.java          # @SpringBootApplication
├── applicationservice/            # Use cases; owns the transaction boundary
│   ├── UserManagementApplicationService.java
│   └── impl/                      # Implementations when the project uses this convention
│       └── UserManagementApplicationServiceImpl.java
├── config/                        # Bean configuration classes
│   ├── ApplicationTimeConfiguration.java  # The Clock bean
│   ├── CatalogClientConfiguration.java    # Constructs the outbound client
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   └── properties/                # @ConfigurationProperties types
│       └── CatalogClientProperties.java   # Outbound integration, not a domain type
├── controller/                    # REST controllers
│   ├── ApiPaths.java              # Single declaration of the API base path
│   └── UserController.java
├── mapper/
│   ├── rest/                      # Domain -> response TO; justified request TO -> service input
│   │   └── UserRestMapper.java
│   └── domain/                    # Entity/projection -> domain; creation values -> new entity
│       └── UserDomainMapper.java
├── service/                       # One service per aggregate root
│   ├── UserService.java           # Owns users + user_address
│   ├── OrganizationService.java   # Owns organization
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
subpackage only with its first type, never as empty scaffolding. The `impl` subpackages are shown
because this example assumes that project convention. Follow the service interface decision from
`../SKILL.md`; do not create interfaces for helpers or types without a real
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

Declare method constraints once on the service contract. The example is an **application service**,
because a transfer spans two `Account` aggregates and coordination is what that level is for.

`AccountService` is the aggregate service for the `Account` root, with `withdraw` and `deposit`
operations that enforce its own invariants; `ReceiptDomain` is a domain record and
`InsufficientFundsException` a project exception. All three belong to this example rather than to the
`user` vocabulary used elsewhere in this file, because banking makes the two-aggregate case obvious
in a way a user and an organization do not.

```java
public interface TransferApplicationService {

    /**
     * Transfers the requested amount between two accounts.
     *
     * @param sourceAccountId source account identifier
     * @param targetAccountId target account identifier
     * @param amount          amount to transfer; must be positive
     * @return the immutable {@link ReceiptDomain} for the completed transfer
     * @throws ConstraintViolationException when an argument violates a structural constraint
     * @throws InsufficientFundsException when the source account cannot cover the transfer
     */
    ReceiptDomain transfer(
            @NotNull final Long sourceAccountId,
            @NotNull final Long targetAccountId,
            @NotNull @Positive final BigDecimal amount);
}
```

This example assumes the service-interface and `*Impl` convention, so the implementation below
carries `@Validated`, the dependencies, and the bodies, while the interface above carries the
constraints and the Javadoc. Do not repeat constraints on the overriding method.

```java
@Service
@Validated
public class TransferApplicationServiceImpl implements TransferApplicationService {

    private final AccountService accountService;

    public TransferApplicationServiceImpl(final AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    @Transactional
    public ReceiptDomain transfer(
            final Long sourceAccountId,
            final Long targetAccountId,
            final BigDecimal amount) {

        this.accountService.withdraw(sourceAccountId, amount);
        this.accountService.deposit(targetAccountId, amount);

        return new ReceiptDomain(sourceAccountId, targetAccountId, amount);
    }
}
```

**With the concrete-service convention there is no second place to split**: the constraints, the
caller-facing Javadoc, `@Validated`, `@Service`, the transaction, and the bodies all sit on one class,
and nothing else about the example changes. Follow whichever convention
`docs/project-profile.md` records, and do not mix the two within a scope.

Four things in that pair are the rules it exists to show:

- **The constraints are on the interface, once.** They are the caller-facing contract, so they sit with the Javadoc; repeating them on the implementation creates two contracts that drift.
- **`@Validated` is on the implementation**, because that is the bean the proxy wraps. On the interface it does nothing.
- **The application service holds no repository**, and this one holds none. Both accounts are written through `AccountService`, the aggregate service that owns the `Account` root and its invariants — "may this account go below zero" is a rule about an account, not about a transfer. A repository here would give the aggregate a second write path that bypasses those invariants.
- **Invoke the service through the Spring proxy**, or neither the validation nor the transaction advice applies. Self-invocation defeats both.

**This excerpt shows method validation, and nothing else — do not read it as a transfer
implementation.** A transfer between two accounts is the canonical concurrency problem and the code
above has none of the answer: no `@Version`, no lock, no conditional `UPDATE`, and the two accounts
touched in the order the caller supplied, which is the shape that deadlocks as soon as anything does
lock. `spring-data-jpa` owns that decision and its examples: choose the strategy per operation, and
where more than one row is locked, derive the order from a stable value rather than from the request.

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

`BusinessValidationException` is the **carrier** shape described in
[error handling examples](error-handling-examples.md#two-exception-shapes-and-when-each-applies): it
takes a catalog constant rather than a message, so one handler serves every business rule while each
rule keeps its own status and problem type.

```java
public class BusinessValidationException extends RuntimeException {

    private final ApplicationError error;

    public BusinessValidationException(final ApplicationError error) {
        super(Objects.requireNonNull(error, "error must not be null").code());
        this.error = error;
    }

    /**
     * Returns the catalog entry that decides this failure's public contract.
     *
     * @return the error catalog constant
     */
    public ApplicationError error() {
        return this.error;
    }
}
```

The message is the internal code, not free text: it is what appears in a stack trace an operator
reads, and it already matches the structured `errorCode` field the advice logs. Never add a
constructor that takes a message, a status, or a URI — the constant is the only input, or the catalog
stops being the single declaration.

```java
public class ConcurrentModificationConflictException extends RuntimeException {

    public ConcurrentModificationConflictException(
            final Object identifier, final Throwable cause) {

        super("Concurrent modification of resource with identifier: %s".formatted(
                Objects.requireNonNull(identifier, "identifier must not be null")), cause);
    }
}
```

This one is the **fixed-mapping** shape: it means exactly one thing, so the handler resolves
`ApplicationError.CONCURRENT_MODIFICATION` itself. `spring-data-jpa` throws it when the optimistic
retry policy is exhausted.

Name the project's validation category `BusinessValidationException`. Do not name it
`ValidationException`: that simple name collides with `jakarta.validation.ValidationException`, and
an unnoticed import of the framework type turns one focused handler into a catch-all for every
Bean Validation failure.

## Configuration properties and beans

`CatalogClientProperties` is a configuration property type, so it lives in `config.properties`.
`CatalogClientConfiguration` constructs beans, so it lives directly in `config`. The catalog client
is deliberately outside this example application's own vocabulary: it stands for any external system
the application calls, and its naming follows the outbound-client rule in
`project-naming-conventions` rather than the domain vocabulary used by the `user` types elsewhere in
this file.

```java
@ConfigurationProperties("clients.catalog")
@Validated
public record CatalogClientProperties(
        @NotNull URI baseUrl,
        @NotNull @DurationMin(millis = 1) Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) Duration readTimeout) {
}
```

Validate required timeouts as strictly positive so invalid configuration fails during startup. The
two names match the outbound timeout budget the project profile records, so a configuration key, a
property component, and a profile row all say the same word.

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogClientProperties.class)
class CatalogClientConfiguration {

    @Bean
    CatalogClient catalogClient(
            final RestClient.Builder restClientBuilder,
            final CatalogClientProperties properties) {

        return new CatalogClient(restClientBuilder
                .baseUrl(properties.baseUrl().toString())
                .build());
    }
}
```

**This bean is incomplete as written, deliberately.** The connection and read timeouts are applied to
the request factory the builder uses, and the type that carries them differs by Spring Boot
generation; `build-and-dependencies` owns that coordinate in
[generation differences](../../build-and-dependencies/references/generation-differences.md). Read it
and configure both timeouts from `properties` before this client goes anywhere near a running system
— [outbound call rules](outbound-call-rules.md) states why a client without a read timeout takes the
application down rather than the dependency.

A configuration class is named for what it constructs. Unrelated infrastructure beans get their own
class rather than a spare `@Bean` method in the nearest existing one:

```java
@Configuration(proxyBeanMethods = false)
class ApplicationTimeConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
```

## Rejected code

`../SKILL.md` names the six shapes that most often survive review. This is the full catalogue it
points at. Every entry restates a rule stated there or in a rule reference; the value of the list is
recognition, not novelty.

**Boundary violations.** Fat controllers; entities in API contracts or returned from services; REST
TOs passed into services; an outbound client with no read timeout; a provider exception or SDK
response type reaching a service or controller; remote I/O inside long transactions; unbounded
collection endpoints; generic `Map` responses; a repository injected into an application service; a
controller handler calling two services; an application service method that only forwards to one
aggregate service; one aggregate service depending on another; a JPA association crossing an
aggregate boundary.

**Structure.** Field injection; an aggregate service that only forwards to its repository while its
invariants live in callers; an application service holding a rule that belongs to one aggregate;
empty or responsibility-free service interfaces; parameter objects that hide unrelated values or
mechanically satisfy the size rule `modern-java-21` owns; generic `enums` packages; REST exception
handlers in the custom-exception package; handwritten structural mappers where approved MapStruct can
express the mapping; a separate mapper per mapping direction for one concept.

**Error contract.** A `code` or `errorCode` member beside the RFC 9457 `type`; a problem type URI or
internal error code declared outside the error catalog; `traceId`, `spanId`, or a stack trace in a
`ProblemDetail` body; the same failure logged by both the service that threw it and the advice that
handles it; a project exception whose simple name collides with a framework type such as
`ValidationException`; generic exception swallowing.

**Process.** Implementing against a decision `docs/project-profile.md` does not record, in breach of the gate `project-decision-profile` owns; hardcoded
configuration or secrets; self-invocation assumptions for proxy annotations; an external effect fired
inside the transaction instead of after commit.

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

## Idempotency placement

- Accept the idempotency key at the REST boundary as an explicit, validated, bounded header or field. Do not read it from arbitrary request state.
- Pass it into the application service as an ordinary explicit parameter or as part of the focused service input. Never pass the request TO.
- Claim the key, execute the effect, and record the outcome inside the use case's transaction, so the claim and the effect commit or roll back together.
- Keep the claim store behind a repository or adapter like any other persistence concern. Do not put it in a controller, mapper, or entity callback.
- Return the recorded original outcome for a repeated key through the same response mapping as the first call, so the public contract is identical.
- Test simultaneous duplicates and retry-after-timeout at the integration boundary, per `spring-boot-testing`.

## Scheduled and asynchronous execution

- Use a distributed lock or database claim pattern when a job must run once across the cluster.
- Bound batches and memory usage; persist progress or checkpoints for large work.
- Configure executors explicitly where concurrency matters.
- Propagate context intentionally and handle failures; never fire-and-forget critical work silently.
- Evaluate virtual threads only after confirming blocking model, pinning, connection pools, and operational behavior.
