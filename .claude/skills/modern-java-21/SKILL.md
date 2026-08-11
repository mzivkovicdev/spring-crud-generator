---
name: modern-java-21
description: Modern Java 21+ coding standard for every creation, edit, refactor, bug fix, or review of a .java file. Use whenever Java source is touched. Enforces import cleanup and ordering, readability, immutability, Javadoc, exception handling, method and class size, null safety, and modern language features without copying legacy anti-patterns.
---

# Modern Java 21+ Coding Skill

Write production-grade Java that is easy to understand, test, change, and operate. Existing code is context for behavior, not automatic permission to repeat its design mistakes.

This skill is authoritative for Java source rules in every file, production and test. Three owners
sit beside it:

| Owner | Owns |
| --- | --- |
| `project-naming-conventions` | Identifier forms and suffixes; this skill owns Java type design |
| `spring-boot-testing` | Test scope, scenarios, fixtures, isolation, execution |
| `build-and-dependencies` | The compiler and quality-gate configuration that enforces these rules |

## Non-negotiable rule for every touched Java file

Whenever a `.java` file is created or modified, even for a one-line change:

1. Remove every unused, duplicate, and obsolete import from that file.
2. Add explicit imports for referenced types; do not use fully qualified names in normal code to avoid an import conflict unless the conflict is real.
3. Never introduce wildcard imports such as `java.util.*` or `import static ...*`.
4. Organize imports into the exact groups below. Sort every group lexicographically by the complete import statement.
5. Separate consecutive non-empty groups with exactly one blank line. Do not leave blank lines for empty groups.
6. This order is a project standard with no exceptions. Keep it even when a formatter, Checkstyle, Spotless, or IDE layout would produce a different one: report the conflicting configuration and offer to update it, rather than adopting the tool's layout.
7. Run the narrowest available compile or static-analysis check to confirm the imports are valid.

This order, the `this.` qualification rule, the `var` prohibition, the `final` rules, the parameter
limit, and the hard size limits are enforced by the project's quality gates and fail the build.
`build-and-dependencies` owns that configuration, including the committed editor settings that stop
an IDE from reverting the import order. Do not suppress a gate at the call site; if a rule does not
fit, change the rule and say so in review.

Use this group order:

1. all `import static ...` statements;
2. Java SE imports: `java.*`;
3. Jakarta EE imports: `jakarta.*`;
4. legacy Java EE/JDK extension imports: `javax.*`;
5. all `com.*` imports, including project and third-party imports;
6. all `org.*` imports, including Spring and other third-party imports;
7. all remaining imports, for example `io.*`, `reactor.*`, and `software.amazon.*`.

Group imports by their leading namespace. Do not separate the project's `com.*` imports from third-party `com.*` imports, and do not separate Spring imports from other `org.*` imports.

Correct default ordering:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.validation.Valid;

import javax.crypto.Cipher;
import javax.sql.DataSource;

import com.acme.customer.Customer;
import com.acme.customer.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3Client;

```

Incorrect:

```java
import java.util.*;                     // wildcard
import org.junit.jupiter.api.Test;
import java.time.Instant;               // not sorted
import com.acme.customer.Customer;
import java.util.Optional;              // duplicate java.util group
import static org.mockito.Mockito.*;    // wildcard static import
import java.time.Clock;                 // unused import
```

Do not reorganize imports across untouched files as part of an unrelated feature. The rule applies to every file actually touched by the change.

## Modernity and compatibility

- Inspect the configured Java release before coding. Java 21 is the minimum expected baseline, but use only stable features supported by the project.
- When the repository does not yet declare what the task needs — an empty repository, or a bare Spring Initializr skeleton with no decisions recorded — ask the user for the missing settings instead of assuming a default. Ask at minimum for the Java release, the Spring Boot version, and the build tool, plus anything else the task depends on. Record the answers in the project profile described by `spring-boot-patterns` so later tasks do not ask again.
- Do not enable preview features or change the Java version unless the task explicitly requires it.
- Prefer a modern construct when it makes the code clearer, safer, or more exhaustive; do not modernize merely to make syntax shorter.
- Preserve existing public behavior and serialized contracts unless the feature intentionally changes them.
- When nearby legacy code conflicts with this skill, keep compatibility at the boundary and make new internals clean. Do not expand the refactor outside the task.

## Type and data design

### Prefer immutable data

- Make dependencies and fields `final` unless mutation is part of the object's responsibility.
- Declare method and constructor parameters `final`.
- Declare local variables `final` when they are assigned once. Omit `final` only when reassignment is
  intentional and clearer than introducing another value.
- Return immutable snapshots or unmodifiable views at boundaries; never leak a mutable internal collection.
- Use records for immutable data carriers such as project TOs, domain values, query results, events, and value objects when their semantics fit.
- Do not use records as JPA entities.
- Validate record invariants in a compact constructor when they are intrinsic to the value.

```java
public record MoneyDomain(BigDecimal amount, Currency currency) {

    public MoneyDomain {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }
}
```

### Use domain types

Prefer a meaningful project domain type, named according to `project-naming-conventions`, when it
prevents mixing values or centralizes a real invariant. Examples include `CustomerIdDomain`,
`EmailAddressDomain`, `MoneyDomain`, and `OrderNumberDomain`. Do not wrap every primitive without a
domain reason.

### Null and Optional

- Return empty collections, arrays, streams, or maps rather than `null`.
- Use `Optional<T>` mainly as a return type for a value that may legitimately be absent.
- Do not use `Optional` for fields, record components, entity attributes, collection elements, or method parameters.
- Do not call `Optional.get()` without proving presence. Prefer `orElseThrow`, `map`, `flatMap`, or an explicit branch.
- Make nullability explicit through validation and contracts. Do not scatter defensive null checks when null should be impossible.

## Language features

### Records

Use a record for transparent immutable data:

```java
public record CustomerRegistrationDetailsDomain(String name, String email) {}
```

Do not put mutable collections into records without making defensive copies:

```java
public record CustomerSnapshotDomain(UUID id, List<AddressDomain> addresses) {

    public CustomerSnapshotDomain {
        addresses = List.copyOf(addresses);
    }
}
```

### Pattern matching and switch expressions

Use exhaustive switch expressions for closed domain variants:

```java
return switch (paymentResult) {
    case PaymentSucceededDomain success -> this.receiptFor(success);
    case PaymentRejectedDomain rejected -> this.rejectionFor(rejected);
    case PaymentPendingDomain pending -> this.pendingFor(pending);
};
```

Use a sealed hierarchy only when the variants are intentionally closed and controlled by the same domain.

### Local variable type inference

Use explicit local variable types throughout project-controlled Java source, including production code, tests, examples, and generated-source templates:

```java
final CustomerDomain customer = this.customerRepository.getRequired(customerId);
final CalculationResultDomain result = this.calculate(input);
```

Do not use `var`. This is a deliberate project readability convention, not a claim that Java local-variable type inference is dynamically typed or universally incorrect. Java still resolves the type statically, but this codebase requires the declared type to remain visible. If a generator emits `var`, change its template or configuration instead of hand-editing generated output.

### Instance qualification

Qualify instance-field and instance-method access with `this.` throughout project-controlled Java
source. This makes instance state and behavior explicit and keeps production code, tests, and
examples consistent.

```java
this.customerRepository.save(customer);
return this.calculateTotal(order);
```

Do not use `this.` for parameters or local variables. Access a static member declared by another
type through that type, unless it is imported statically under the project's import policy. A static
member declared by the current type may remain unqualified.

### Streams

- Use streams for readable transformations, filtering, grouping, and aggregation.
- Use a loop when it is clearer, needs early exit, handles checked failures, or performs several stateful steps.
- Do not hide remote calls or repository calls inside stream operations.
- Do not use parallel streams in request handling or for blocking I/O.
- Avoid deeply nested stream pipelines and side effects in `map`, `filter`, or `peek`.

## Classes and methods

- Give each class one cohesive reason to change.
- Keep methods at one level of abstraction and name extracted operations by intent.
- Prefer guard clauses over deep nesting.
- A method up to 40 lines needs no size justification. A method from 41 through 60 lines requires
  scrutiny and a deliberate decision to keep it whole. A method from 61 through 100 lines must be
  refactored unless a concrete reason for keeping it intact is documented. A method over 100 lines
  must be refactored without exception. A class over 1000 lines must be refactored unless a concrete
  reason is documented.
- Allow up to seven declared parameters in project-owned methods and constructors when their names, order, and purpose remain clear. Treat eight or more as a design warning: first group values that form a cohesive domain concept or invariant into a focused parameter/value object, or document why the signature cannot be changed. Do not create a catch-all wrapper merely to hide unrelated parameters. Existing framework callbacks, overrides, and generated signatures are exempt when the project does not control them.
- Do not game size rules by extracting meaningless one-line methods. Utility classes are allowed
  when they are stateless, cohesive, and named for one focused responsibility; do not create generic
  `Utils` dumping grounds for unrelated behavior.
- Prefer composition over inheritance.
- Create an interface for a real boundary, multiple behavior, a plugin strategy, or a useful port.
  When `spring-boot-patterns` selects an application-service interface, treat it as that boundary; do
  not extend the convention mechanically to helpers or unrelated classes.

Framework-neutral example of cohesive behavior:

```java
public final class OrderTotalCalculator {

    private final DiscountPolicy discountPolicy;
    private final TaxPolicy taxPolicy;

    public OrderTotalCalculator(
            final DiscountPolicy discountPolicy,
            final TaxPolicy taxPolicy) {

        this.discountPolicy = discountPolicy;
        this.taxPolicy = taxPolicy;
    }

    public MoneyDomain calculate(final OrderDomain order) {
        final MoneyDomain subtotal = order.subtotal();
        final MoneyDomain discountedSubtotal = this.discountPolicy.apply(subtotal, order.customerType());
        return this.taxPolicy.addTax(discountedSubtotal, order.shippingAddress());
    }
}
```

## Dependency injection

- Use constructor injection.
- Do not use field injection, static mutable dependencies, or service locators.
- Prefer an explicit constructor. Lombok `@RequiredArgsConstructor` is acceptable only when Lombok is already approved by the project and the generated constructor does not hide an oversized dependency list.
- Many constructor dependencies usually indicate too many responsibilities; split the class by behavior instead of hiding them.

A stateless, dependency-free MapStruct mapper obtained through its generated static
`INSTANCE = Mappers.getMapper(...)` member is an explicit, deliberate exception to the
service-locator prohibition, because the mapper holds no state, performs no I/O, and is generated
rather than resolved at runtime from a mutable registry. `spring-boot-patterns` owns that decision.
Do not report it as a service-locator or static-dependency violation, and do not extend the
exception to any other collaborator.

## Exceptions

- Throw a domain/application exception that communicates the failure to its caller.
- Preserve the original cause when translating infrastructure failures.
- Catch an exception only when adding context, translating at a boundary, compensating, retrying under an explicit policy, or producing a stable external response.
- Never swallow an exception or return fake success.
- Do not catch `Throwable`; avoid broad `Exception` catches except at a true top-level boundary.
- Do not log and rethrow the same failure at every layer. Log once at the boundary that owns operational handling.
- Exception messages must be actionable but must not expose secrets or sensitive personal data.

```java
try {
    return this.paymentClient.charge(request);
} catch (final PaymentProviderException exception) {
    throw new PaymentUnavailableException(orderId, exception);
}
```

## Time, IDs, and nondeterminism

- Inject `Clock` rather than calling `Instant.now()` or `LocalDateTime.now()` throughout business logic.
- Inject an ID generator when deterministic testing or provider-specific formats matter.
- Use `Instant` for a point on the timeline, `LocalDate` for a calendar date, and an explicit `ZoneId` for business-zone conversion.
- Store and exchange timestamps with an explicit UTC/offset policy.
- Do not use `Thread.sleep` for coordination.

## Javadoc and comments

Javadoc is contract documentation, not a coverage metric. Do not add it based only on `public` or `protected`; a declaration can be publicly accessible for framework, proxy, serialization, code-generation, or testing reasons without being a published Java API.

Add complete Javadoc to:

- published or externally consumed Java API contracts;
- intentional extension points and interfaces implemented outside the package;
- non-obvious invariants, preconditions, side effects, blocking behavior, concurrency guarantees, transaction requirements, retry behavior, and failure modes;
- methods whose contract cannot be understood from their signature and type names.

Do not add Javadoc by default to:

- self-explanatory TOs, records, enum constants, exceptions, constructors, getters, setters, and accessors;
- routine framework adapters, generated-code contracts, mappers, repositories, dependency-injection configuration, and wiring classes whose behavior is clear from types and annotations;
- overriding methods when the inherited contract is accurate;
- private methods and tests whose purpose is clear from names, types, and structure.

Javadoc should explain the contract and the reason, not narrate the implementation. When a
declaration requires Javadoc under this policy, include every applicable tag:

- `@param parameterName` for every method or constructor parameter, including semantic meaning, accepted range/format, nullability, units, and ownership when relevant;
- `@param <T>` for every generic type parameter;
- `@return` for every non-`void` method, describing the returned value/type, nullability, mutability/ownership, and important state guarantees;
- `@throws ExceptionType` for every checked exception and every runtime exception that is part of the public contract, with the exact condition that causes it;
- `@deprecated` with the replacement and migration direction whenever `@Deprecated` is used.

Do not add `@return` to constructors or `void` methods. Do not document internal implementation exceptions that cannot escape the API. Keep tags in the order: type parameters, value parameters in signature order, return value, exceptions, then optional `@since`, `@see`, or `@deprecated` metadata.

```java
/**
 * Reserves inventory for the supplied order.
 *
 * <p>The operation is idempotent for the same order identifier. A successful return guarantees
 * that the reservation is visible to subsequent inventory reads.
 *
 * @param orderId the unique identifier of the order requesting inventory; must not be {@code null}
 * @param lines   the non-empty immutable list of order lines to reserve; must not be {@code null}
 *                and must not contain {@code null} elements
 * @return        a {@link ReservationDomain} containing the reserved quantities and reservation
 *                identifier; never {@code null}
 * @throws InsufficientInventoryException when any requested item cannot be reserved
 * @throws InventoryUnavailableException when the inventory provider cannot be reached
 */
ReservationDomain reserve(
        final OrderIdDomain orderId,
        final List<OrderLineDomain> lines);
```

A generic method adds `@param <T>` first, describing the element type, before the value parameters.

When a record requires Javadoc, document every component with `@param`. For public classes and
interfaces, document responsibility, invariants, thread-safety, and lifecycle where relevant. An
overriding method inherits missing Javadoc automatically: omit it when the inherited contract is
complete, never write a comment containing only `{@inheritDoc}`, and use `{@inheritDoc}` only to
extend an inherited contract that remains accurate.

Do not write Javadoc such as "Gets the name" on a self-explanatory accessor, and remove stale
comments when the implementation changes.

## Logging

- Use parameterized logging rather than string concatenation.
- Log stable identifiers and outcomes, not entire objects or payloads.
- Never log credentials, tokens, cookies, authorization headers, secrets, or unnecessary personal data.
- Use `ERROR` for failures that require action, `WARN` for degraded/expected exceptional conditions, `INFO` for significant lifecycle/business events, and `DEBUG` for diagnostic detail.
- Avoid duplicate logging of the same exception across layers.

## Worked examples in these skills

Every code, configuration, and build snippet in this skill set is a **pattern to adapt, not a file to
copy**. An agent asked for a product service writes `ProductService` from scratch; it does not rename
`UserService` and keep the rest.

**Self-containment.** A snippet must declare every identifier it uses, or name where the identifier
comes from. Concretely:

- Every constant referenced in a snippet is declared in that same snippet, unless the snippet states which example or type declares it.
- Every build property referenced as `${...}` is declared in the same file, or the file says where it is declared.
- Every type referenced across skills is named with the reference that defines it, so the reader can find it.
- Omit imports, and omit members that are irrelevant to the decision being shown — but never omit something the snippet itself refers to.

An undeclared identifier is the easiest defect to miss, because the surrounding code reads correctly
and fails only on the reader's machine.

**Excerpts.** A snippet marked as an excerpt shows one decision, not a complete type. Generate the
members it omits rather than copying it verbatim. When an omitted member is required for the code to
work at all — an accessible constructor for a mapper, a bean registration for a filter — the example
says so explicitly instead of leaving it implied.

**Verification.** Check a snippet against the versions the project profile records. When part of it
cannot be verified, say which part rather than presenting it with the same confidence as the rest.

## Tests are part of the code change

Apply the complete `spring-boot-testing` workflow. Every touched test file must also follow this
skill, including explicit local types, import order, source hygiene, Javadoc, and nondeterminism
rules. Do not introduce a Java test pattern that conflicts with the testing owner skill.

Test fixtures declared by a test framework are an explicit exception to the `final`-field and
field-injection rules, because the framework itself assigns them after construction:

- fields annotated with Mockito's `@Mock`, `@Spy`, `@Captor`, or `@InjectMocks`;
- fields annotated with a Spring test bean override such as `@MockitoBean` or `@MockitoSpyBean`;
- the subject under test when it is rebuilt in `@BeforeEach` for isolation.

Declare those fields non-`final` and `private`. Keep every other test collaborator `final` and
constructor-injected, including `MockMvc`, `ObjectMapper`, repositories, and project-owned test
clients. Do not use `@Autowired` on a field to avoid this rule.

## Forbidden patterns

- wildcard or unused imports;
- opaque or repository-inconsistent local type inference;
- field injection;
- `Optional` fields or parameters;
- `null` collections;
- methods over 100 lines;
- God classes and generic utility dumping grounds;
- business logic in controllers or persistence callbacks;
- broad exception swallowing;
- mutable global state;
- hardcoded secrets or environment values;
- copying a legacy pattern without evaluating it;
- unverified generated contracts or custom generated behavior.

## Completion checklist

Before finishing any Java task:

- [ ] Every touched Java file has clean, correctly ordered imports.
- [ ] Imports follow the project order exactly, and the relevant compile or static checks were run.
- [ ] New code uses the project's Java version and no unapproved preview feature.
- [ ] Methods and classes remain cohesive and reasonably sized.
- [ ] Nullability, exceptions, time, and mutability are explicit.
- [ ] Required Javadoc documents contracts and non-obvious behavior.
- [ ] Tests cover new or changed behavior and pass.
- [ ] The diff contains no forbidden pattern listed above.
