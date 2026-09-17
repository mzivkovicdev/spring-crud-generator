# Nullability rules

Use this reference when adding a package, deciding whether a declaration may be null, annotating an
array or a generic type, or migrating an existing package to `@NullMarked`. Apply every rule from
`../SKILL.md`, which states the convention itself; this file carries what follows from it.

**This file carries rules, not only examples.** Annotation placement, array and generic syntax, which
layer of protection catches what, the boundaries the annotations cannot describe, and the migration
procedure are stated here in full and nowhere else. Treat those sections as binding.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](worked-example-rules.md).

## Contents

- [Type-use binding, and the fourth annotation](#type-use-binding-and-the-fourth-annotation)
- [What the marked package file looks like](#what-the-marked-package-file-looks-like)
- [Where an annotation may and may not go](#where-an-annotation-may-and-may-not-go)
- [Arrays and generics](#arrays-and-generics)
- [Which layer catches what](#which-layer-catches-what)
- [Boundaries the annotations do not cover](#boundaries-the-annotations-do-not-cover)
- [Reading Spring's own signatures](#reading-springs-own-signatures)
- [Migrating an existing package](#migrating-an-existing-package)
- [Rejected patterns](#rejected-patterns)

## Type-use binding, and the fourth annotation

`@Nullable` and `@NonNull` are **type-use** annotations: each binds to the type immediately to its
right, not to the declaration as a whole. That single fact decides every array and generic case
below, and it is why position is not a matter of taste.

`org.jspecify.annotations` carries a fourth annotation the skill body does not mention:
`@NullUnmarked`, which suspends `@NullMarked` for a scope. Use it only to land a large package
migration in stages, and only with a removal condition recorded in the change description. A
`@NullUnmarked` scope with no removal condition is an unmarked package wearing a marked package's
clothes.

## What the marked package file looks like

```java
@NullMarked
package com.example.myapp.user.domain;

import org.jspecify.annotations.NullMarked;
```

The file contains the annotation, the package declaration, and the import — nothing else. It is the
only file in the project where an annotation precedes a `package` statement, which is why an IDE that
reorders imports sometimes moves it; check it after a bulk reformat.

## Where an annotation may and may not go

- **On a declaration, not a local variable.** `@Nullable` belongs on a field, a parameter, a return type, or a record component. The nullness of a local is inferred from what is assigned to it, so annotating one states nothing the compiler did not already know.
- **Never on a primitive.** `@Nullable int` does not compile, and `@Nullable long` is a sign the field should be a boxed type carrying a real absence.
- **On a record component directly.** The canonical constructor inherits it. Do not add a second nullable representation such as an `Optional` component beside it.
- **On an entity field, follow what the field can hold in Java, which is usually the column's nullability.** A non-null field over a nullable column is a production `NullPointerException` waiting for the first legacy row; a nullable field over a `NOT NULL` column pushes checks into every caller for a case the database already ruled out.
  - The exception is the fields the provider assigns. A generated `@Id` and a `@Version` are null on a new instance that has not been persisted yet, even though both columns are `NOT NULL`. Annotate them `@Nullable` and let the column stay `NOT NULL`: the annotation describes the Java field across the whole entity lifecycle, and the constraint describes the row, which only exists after the insert. This is the reason `spring-data-jpa` requires `Long` rather than `long` for `@Version`.
- **On an override, only in the widening direction.** A method may widen a return to `@Nullable` when the supertype already declares it, and may never narrow a parameter the supertype declared `@Nullable`. The reverse breaks every caller holding the supertype.

## Arrays and generics

Because the annotation binds to the type on its right, position carries meaning:

```java
@Nullable Object[] a;            // non-null array, elements may be null
Object @Nullable [] b;           // array may be null, elements are not
@Nullable Object @Nullable [] c; // both may be null

List<@Nullable String> d;        // non-null list, elements may be null
@Nullable List<String> e;        // list may be null, elements are not
```

Reach for the element-nullable forms rarely. A collection whose elements may be null is almost always
a design defect that the empty-collection rule in `../SKILL.md` already rules out — the forms exist
for reading third-party signatures more often than for writing project ones.

Generic type parameters are the one place where the tooling lags the specification, which
`build-and-dependencies` documents with the rest of the checker's limits. Write the annotation
correctly regardless of what the build says about it.

## Which layer catches what

`../SKILL.md` requires all three layers. This is the division of labour between them, which decides
where a given failure should have been caught:

| Layer | Mechanism | Catches | Runs at |
| --- | --- | --- | --- |
| Contract | `@NullMarked` / `@Nullable` | A caller inside the project passing or dereferencing wrongly | Compile time, in the IDE and in the checker if the project runs one |
| Construction | `Objects.requireNonNull` in a constructor or compact constructor | A null arriving from outside the checked world — deserialization, reflection, a generated mapper, an unchecked dependency | Runtime, as the object is built |
| Boundary | Bean Validation on request TOs and configuration properties | A null in untrusted input, reported as a contract violation rather than a crash | Runtime, at the boundary |

Read the table in both directions. A null that reached the domain from JSON was never the contract
layer's to catch, so tightening annotations will not prevent the next one; and a defensive check
inside a marked package, on a value the same method just constructed, is covered by the contract layer
and is the check `../SKILL.md` asks you to remove.

## Boundaries the annotations do not cover

At these seams the contract stops, and only the runtime layers protect:

- **Jackson deserialization.** A record component declared non-null still arrives null when the JSON omits the field. Bean Validation on the request TO rejects it; the compact constructor stops it reaching the domain if validation is bypassed.
- **Generated mappers.** MapStruct output is not marked, so a mapping that drops a field produces a null no annotation saw. The build-level unmapped-target policy that `build-and-dependencies` requires is the gate here.
- **Repository returns.** Spring Data returns `Optional` for a single result by design, so a repository signature is read, not annotated. A query method returning a collection returns an empty one.
- **Configuration properties.** A missing property binds to null regardless of annotation. Required properties are validated, not assumed.
- **The published contract.** `rest-api-contract` owns a separate trichotomy for the payload — required, optional, nullable — and it does not map one-to-one onto the Java field. A contract-optional field is a `@Nullable` component; a contract-required one is *not* automatically non-null in Java, because a malformed request still deserializes to null before validation runs.
- **Unmarked third-party libraries.** An unmarked dependency states nothing at all — which is different from stating non-null. Check at the seam, once, rather than repeating the check inward.

## Reading Spring's own signatures

The project writes the same annotations on both generations, so nothing here changes how code is
written. One consequence is worth knowing while reading: Spring's own API surface carries JSpecify
metadata only from Spring Framework 7 onward. On a Spring Boot 3 project an IDE therefore infers
nothing from a Spring signature that may return null, and the marked package will not warn about it —
check that method's javadoc instead of assuming the marking covered it.

`build-and-dependencies` owns which annotation set each generation's Spring APIs carry and how the
artifact is declared; its
[generation differences](../../build-and-dependencies/references/generation-differences.md) is the
single catalogue for both.

## Migrating an existing package

One package at a time, never repository-wide in one change.

1. Add the `package-info.java` shown above.
2. Compile, and read what the IDE or the checker reports.
3. For each report decide which is true: the declaration really may be null, so annotate it; or it may not, and the code that assumed otherwise is the defect. **Do not annotate to silence a report you have not understood** — that writes the bug into the contract, where the next reader will trust it.
4. Remove the defensive checks the marking has made unreachable; keep every check covering a seam from the section above.
5. Use `@NullUnmarked` only to stage a large package, with its removal condition recorded.

A migration that only adds annotations until the build goes quiet has produced a documented set of
wrong claims, which is worse than the unmarked package it replaced.

## Rejected patterns

```java
// Wrong: three different vocabularies, none of them the project's.
import javax.annotation.Nullable;
import org.springframework.lang.Nullable;
import lombok.NonNull;
```

```java
// Wrong: @NonNull inside a @NullMarked package. It is already the default,
// and writing it here implies the fields beside it are ambiguous.
@NonNull
private final String username;
```

```java
// Wrong: the field itself can still be null, so this has three states where the
// declaration below has two.
private Optional<String> middleName;

// Right:
private @Nullable String middleName;
```

```java
// Wrong: the annotation contradicts the column. The database allows null here,
// so this declaration is a promise the schema does not keep.
@Column(name = "middle_name", nullable = true)
private String middleName;
```

```java
// Wrong: dropping the runtime check because the parameter is non-null by default.
// Jackson builds this through reflection and never consulted the annotation.
public record UserCreateTO(String username, String email) {

    public UserCreateTO {
        // Objects.requireNonNull removed "because @NullMarked covers it" — it does not.
    }
}
```
