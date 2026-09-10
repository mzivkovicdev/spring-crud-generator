# Language feature examples

The code behind the type-design, record, pattern-matching, qualification, and cohesion rules in
`../SKILL.md`. The rules are there; this file only shows them applied, so read it when a rule's shape
is not obvious from its statement.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](worked-example-rules.md).

## Contents

1. [Records as value carriers](#records-as-value-carriers)
2. [Pattern matching and switch expressions](#pattern-matching-and-switch-expressions)
3. [Explicit local types and `this.` qualification](#explicit-local-types-and-this-qualification)
4. [A cohesive class](#a-cohesive-class)
5. [Translating an exception at a boundary](#translating-an-exception-at-a-boundary)

## Records as value carriers

A record validates the invariants that are intrinsic to the value, in a compact constructor:

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

A record with no invariant needs no body:

```java
public record CustomerRegistrationDetailsDomain(String name, String email) {}
```

A record holding a collection copies it defensively. Without the copy the record is immutable in name
only: the caller keeps a reference to the same list and can mutate it after construction.

```java
public record CustomerSnapshotDomain(UUID id, List<AddressDomain> addresses) {

    public CustomerSnapshotDomain {
        addresses = List.copyOf(addresses);
    }
}
```

## Pattern matching and switch expressions

An exhaustive switch expression over a closed set of domain variants. Exhaustiveness is the point:
adding a fourth variant to the sealed hierarchy fails compilation here rather than falling through at
runtime.

```java
return switch (paymentResult) {
    case PaymentSucceededDomain success -> this.receiptFor(success);
    case PaymentRejectedDomain rejected -> this.rejectionFor(rejected);
    case PaymentPendingDomain pending -> this.pendingFor(pending);
};
```

Use a sealed hierarchy only when the variants are intentionally closed and controlled by the same
domain.

## Explicit local types and `this.` qualification

```java
final CustomerDomain customer = this.customerRepository.getRequired(customerId);
final CalculationResultDomain result = this.calculate(input);
```

```java
this.customerRepository.save(customer);
return this.calculateTotal(order);
```

Parameters and local variables are never qualified with `this.`. A static member declared by another
type is reached through that type unless it is statically imported; a static member declared by the
current type may remain unqualified.

## A cohesive class

Framework-neutral, one reason to change, dependencies `final` and constructor-injected, each method
at one level of abstraction:

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

## Translating an exception at a boundary

The cause is preserved, and the provider's exception type stops here rather than reaching a caller
that cannot act on it:

```java
try {
    return this.paymentClient.charge(request);
} catch (final PaymentProviderException exception) {
    throw new PaymentUnavailableException(orderId, exception);
}
```
