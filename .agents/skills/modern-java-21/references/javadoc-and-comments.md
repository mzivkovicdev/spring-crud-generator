# Javadoc and comments

Use this reference whenever a touched file declares a published API contract, an extension point, or
a method whose contract is not obvious from its signature — and whenever deciding *not* to write
Javadoc, because the decision to omit it is part of the rule. Apply every rule from `../SKILL.md`.

## Contents

1. [Where Javadoc belongs, and where it does not](#where-javadoc-belongs-and-where-it-does-not)
2. [Tags and their order](#tags-and-their-order)
3. [Worked example](#worked-example)
4. [Records, generics, and overrides](#records-generics-and-overrides)

## Where Javadoc belongs, and where it does not

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

## Tags and their order

Javadoc explains the contract and the reason, not the implementation. When a declaration requires
it, include every applicable tag:

- `@param parameterName` for every method or constructor parameter, including semantic meaning, accepted range/format, nullability, units, and ownership when relevant;
- `@param <T>` for every generic type parameter;
- `@return` for every non-`void` method, describing the returned value/type, nullability, mutability/ownership, and important state guarantees;
- `@throws ExceptionType` for every checked exception and every runtime exception that is part of the public contract, with the exact condition that causes it;
- `@deprecated` with the replacement and migration direction whenever `@Deprecated` is used.

Do not add `@return` to constructors or `void` methods. Do not document internal implementation exceptions that cannot escape the API. Keep tags in the order: type parameters, value parameters in signature order, return value, exceptions, then optional `@since`, `@see`, or `@deprecated` metadata.

## Worked example

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

## Records, generics, and overrides

A generic method adds `@param <T>` first, describing the element type, before the value parameters.

A record documents every component with `@param`. A public class or interface documents
responsibility, invariants, thread-safety, and lifecycle where relevant. An overriding method
inherits missing Javadoc automatically: omit it when the inherited contract is complete, never write
a comment containing only `{@inheritDoc}`, and use `{@inheritDoc}` only to extend an inherited
contract that remains accurate.

Do not write Javadoc such as "Gets the name" on a self-explanatory accessor, and remove stale
comments when the implementation changes.
