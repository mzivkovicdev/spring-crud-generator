---
name: modern-java-21
description: Modern Java 21+ coding standard for every creation, edit, refactor, bug fix, or review of a .java file. Use whenever Java source is touched. Enforces import cleanup and ordering, readability, immutability, Javadoc, exception handling, method and class size, null safety, and modern language features without copying legacy anti-patterns.
---

# Modern Java 21+ Coding Skill

Write production-grade Java that is easy to understand, test, change, and operate. Existing code is context for behavior, not automatic permission to repeat its design mistakes.

Apply `spring-boot-testing` whenever production behavior or tests change. It owns test scope,
scenario selection, fixtures, isolation, and execution; this skill remains authoritative for Java
source rules in production and test files.

## Non-negotiable rule for every touched Java file

Whenever a `.java` file is created or modified, even for a one-line change:

1. Remove every unused, duplicate, and obsolete import from that file.
2. Add explicit imports for referenced types; do not use fully qualified names in normal code to avoid an import conflict unless the conflict is real.
3. Never introduce wildcard imports such as `java.util.*` or `import static ...*`.
4. Organize imports into the exact groups below. Sort every group lexicographically by the complete import statement.
5. Separate consecutive non-empty groups with exactly one blank line. Do not leave blank lines for empty groups.
6. If the repository has an enforced formatter, Checkstyle, Spotless, or IDE import layout that conflicts with this order, follow the build-enforced layout and report the conflict instead of repeatedly fighting the formatter.
7. Run the formatter or the narrowest available compile/static-analysis check to confirm imports are valid.

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
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }
}
```

### Use domain types

Prefer a meaningful type such as `CustomerId`, `EmailAddress`, `Money`, or `OrderNumber` when it prevents mixing values or centralizes a real invariant. Do not wrap every primitive without a domain reason.

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
public record CustomerRegistrationDetails(String name, String email) {}
```

Do not put mutable collections into records without making defensive copies:

```java
public record CustomerSnapshot(UUID id, List<Address> addresses) {

    public CustomerSnapshot {
        addresses = List.copyOf(addresses);
    }
}
```

### Pattern matching and switch expressions

Use exhaustive switch expressions for closed domain variants:

```java
return switch (paymentResult) {
    case PaymentSucceeded success -> receiptFor(success);
    case PaymentRejected rejected -> rejectionFor(rejected);
    case PaymentPending pending -> pendingFor(pending);
};
```

Use a sealed hierarchy only when the variants are intentionally closed and controlled by the same domain.

### Local variable type inference

Use explicit local variable types throughout project-controlled Java source, including production code, tests, examples, and generated-source templates:

```java
final Customer customer = this.customerRepository.getRequired(customerId);
final CalculationResult result = this.calculate(input);
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

Do not use `this.` for parameters or local variables. Access static members through their declaring
type, except unqualified static constants or methods imported according to the project's import
policy.

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
- A method over roughly 40 lines requires scrutiny. A method from 61 through 100 lines must be
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

- public and protected APIs;
- extension points and interfaces implemented outside the package;
- non-obvious invariants, preconditions, side effects, blocking behavior, concurrency guarantees, transaction requirements, retry behavior, and failure modes;
- methods whose contract cannot be understood from their signature and type names.

Do not add Javadoc by default to:

- self-explanatory DTOs, TOs, records, enum constants, exceptions, constructors, getters, setters, and accessors;
- routine framework adapters, generated-code contracts, mappers, repositories, dependency-injection configuration, and wiring classes whose behavior is clear from types and annotations;
- overriding methods when the inherited contract is accurate;
- private methods and tests whose purpose is clear from names, types, and structure.

Javadoc should explain the contract and the reason, not narrate the implementation. A public or protected method Javadoc is incomplete unless it contains every applicable tag:

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
 * @return        a {@link Reservation} containing the reserved quantities and reservation identifier;
 *                never {@code null}
 * @throws InsufficientInventoryException when any requested item cannot be reserved
 * @throws InventoryUnavailableException when the inventory provider cannot be reached
 */
Reservation reserve(final OrderId orderId, final List<OrderLine> lines);
```

Complete generic-type example:

```java
/**
 * Returns a page of values matching the supplied query.
 *
 * @param <T>         the immutable result element type
 * @param query       the query criteria; must not be {@code null}
 * @param pageRequest the zero-based page request including deterministic sorting; must not be
 *                    {@code null}
 * @return             a {@link Page} of matching values; never {@code null}
 * @throws InvalidQueryException when the query contains an unsupported filter or sort field
 */
<T> Page<T> search(final SearchQuery query, final PageRequest pageRequest);
```

When a record requires Javadoc under this policy, document every component with `@param`. For public classes/interfaces, document responsibility, invariants, thread-safety, and lifecycle where relevant. An overriding method automatically inherits missing Javadoc from its supertype. Omit its Javadoc when the inherited contract is complete; do not add a comment containing only `{@inheritDoc}`. Use `{@inheritDoc}` when extending the inherited text with meaningful caller-visible guarantees or behavior, and only when the inherited contract remains accurate.

Do not add Javadoc such as "Gets the name" to a self-explanatory accessor. Remove stale comments when the implementation changes.

## Logging

- Use parameterized logging rather than string concatenation.
- Log stable identifiers and outcomes, not entire objects or payloads.
- Never log credentials, tokens, cookies, authorization headers, secrets, or unnecessary personal data.
- Use `ERROR` for failures that require action, `WARN` for degraded/expected exceptional conditions, `INFO` for significant lifecycle/business events, and `DEBUG` for diagnostic detail.
- Avoid duplicate logging of the same exception across layers.

## Tests are part of the code change

Apply the complete `spring-boot-testing` workflow. Every touched test file must also follow this
skill, including explicit local types, import order, source hygiene, Javadoc, and nondeterminism
rules. Do not introduce a Java test pattern that conflicts with the testing owner skill.

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
- [ ] The configured formatter and relevant static checks were run.
- [ ] New code uses the project's Java version and no unapproved preview feature.
- [ ] Methods and classes remain cohesive and reasonably sized.
- [ ] Nullability, exceptions, time, and mutability are explicit.
- [ ] Required Javadoc documents contracts and non-obvious behavior.
- [ ] Tests cover new or changed behavior and pass.
- [ ] The diff contains no forbidden pattern listed above.
