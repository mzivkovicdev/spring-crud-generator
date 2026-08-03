# Java, Spring, and test names

Use this reference for Java identifiers, packages, modules, Spring components, architectural roles,
exceptions, and tests. Apply `modern-java-21` for source formatting, imports, language use, and
Javadoc. Apply `spring-boot-testing` for test scope, scenarios, fixtures, isolation, and execution.
Apply `spring-boot-patterns` for layer responsibilities.

## Contents

1. [Preserve project terminology](#preserve-project-terminology)
2. [Name packages, modules, and folders](#name-packages-modules-and-folders)
3. [Name Java types](#name-java-types)
4. [Name Spring and project roles](#name-spring-and-project-roles)
5. [Name methods](#name-methods)
6. [Name fields, parameters, and local variables](#name-fields-parameters-and-local-variables)
7. [Name constants and enum values](#name-constants-and-enum-values)
8. [Name exceptions](#name-exceptions)
9. [Name tests and test data](#name-tests-and-test-data)
10. [Recognize weak names](#recognize-weak-names)
11. [Review examples](#review-examples)

## Preserve project terminology

Use these established forms consistently:

```text
UserCreateTO
UserUpdateTO
UserTO
UserDomain
UserEntity
UserSummaryProjection
UserRestMapper
UserDomainMapper
UserService
UserRepository
```

Keep TOs at the REST boundary, domain objects at the service boundary, entities and projections at the repository boundary, and mapper names aligned with their mapping responsibility. Do not use naming to blur those boundaries.

Prefer `id` and `userId` consistently. Do not alternate between `id`, `identifier`, `key`, and `userKey` for the same identifier. Use a different term when the value has a genuinely different meaning, such as `externalUserId` or `authenticationSubject`.

## Name packages, modules, and folders

Use lowercase package and Java module names. Use the organization's approved reverse-domain root and stable capability vocabulary:

```text
com.acme.myapp.controller
com.acme.myapp.mapper.rest
com.acme.myapp.mapper.domain
com.acme.myapp.service
com.acme.myapp.service.impl
com.acme.myapp.domain
com.acme.myapp.repository
com.acme.myapp.model
com.acme.myapp.transferobject.request
com.acme.myapp.transferobject.response
com.acme.myapp.exception
com.acme.myapp.config
```

This project uses a layered package layout. Keep controllers, services, domain models, repositories, persistence models, mappers, transfer objects, exceptions, and configuration in their established layer packages. Add a deeper package only when it represents a coherent responsibility with enough types to justify it. Do not create missing packages before a type with that responsibility exists.

Rules:

- Use the deployable microservice's base package as the root and organize application code by the established technical layers beneath it.
- Keep each layer focused on one architectural responsibility.
- Keep package segments lowercase ASCII without underscores or camel case.
- Use a singular or plural capability form consistently; prefer the established ubiquitous-language form.
- Avoid package names tied to temporary initiatives, team names, ticket numbers, people, or deployment environments.
- Avoid dumping grounds such as `common`, `misc`, `general`, `stuff`, `helpers`, or a broad `util` package.
- When an established layout contains `util`, keep it limited to focused stateless helpers with meaningful type names. Prefer a specific responsibility package for new code when that is clearer; do not migrate packages solely to remove the word `util`.
- Put application service implementations in `service.impl` and name them `*ServiceImpl`; do not use `impl` as a dumping ground for unrelated types.
- Keep Java source filenames identical to their top-level public type names.

Name Maven or Gradle modules by durable capability or deployable responsibility:

```text
order-api
order-domain
order-persistence
catalog-client
```

Do not split modules or rename folders merely to satisfy these examples. Apply architecture and build ownership first.

## Name Java types

Use `UpperCamelCase`.

| Kind | Default form | Examples |
| --- | --- | --- |
| Class or record | Noun or noun phrase | `UserDomain`, `OrderSummary`, `RetryPolicy` |
| Interface | Role, capability, or contract | `CatalogClient`, `Clock`, `AuthorizationPolicy` |
| Enum type | Singular concept | `OrderStatus`, `PaymentMethod` |
| Annotation | Noun or adjective describing its meaning | `Audited`, `InternalApi` |
| Exception | Cause, violated condition, or failed outcome plus `Exception` | `OrderNotFoundException` |
| Test class | Subject plus test scope suffix | `UserServiceTest`, `UserRepositoryIntegrationTest` |

Do not prefix interfaces with `I`. Do not suffix every interface with `Interface`. Do not create `Default`, `Base`, `Abstract`, or `Impl` names unless the modifier communicates a real, stable distinction.

When multiple implementations exist, expose the distinguishing behavior:

```text
HttpCatalogClient
InMemoryCatalogClient
CachedCatalogClient
```

Use `Abstract...` only for an intentional extensible base-class contract, not for a partially implemented class that should be composed differently.

Name generic type parameters:

- Use `T`, `E`, `K`, `V`, or `R` for a short, conventional generic scope.
- Use an intention-revealing type name such as `Request`, `Response`, or `AggregateId` when multiple parameters or a longer contract makes single letters ambiguous.
- Avoid meaningless sequences such as `T1`, `T2`, and `T3`.

## Name Spring and project roles

Use a suffix only when the type owns that responsibility:

| Role | Form | Example |
| --- | --- | --- |
| REST controller | `<Resource>Controller` | `UserController` |
| Application service contract | `<Capability>Service` | `UserService` |
| Application service implementation | `<Capability>ServiceImpl` | `UserServiceImpl` |
| JPA repository | `<Aggregate>Repository` | `UserRepository` |
| JPA entity | `<Concept>Entity` | `UserEntity` |
| REST transfer object | Established `<Concept>TO` form | `UserCreateTO`, `UserTO` |
| Domain/service result | `<Concept>Domain` | `UserDomain` |
| Repository projection | `<Purpose>Projection` | `UserSummaryProjection` |
| REST mapper | `<Concept>RestMapper` | `UserRestMapper` maps domain → response TO and, only for a justified focused input, request TO → service/domain input |
| Entity/domain mapper | `<Concept>DomainMapper` | `UserDomainMapper` maps entity/projection → domain and explicit creation values → new entity |
| Configuration properties | `<Subsystem>Properties` | `CatalogClientProperties` |
| Bean configuration | `<Subsystem>Configuration` | `CatalogClientConfiguration` |
| Outbound client | `<ProviderOrCapability>Client` | `CatalogClient` |
| Converter | `<Source><Target>Converter` when one conversion is its responsibility | `StringCurrencyConverter` |
| Validator | `<RuleOrSubject>Validator` | `OrderTransitionValidator` |
| Listener or consumer | Name the input and mechanism | `OrderCreatedEventConsumer` |
| Publisher | Name the published contract | `OrderEventPublisher` |
| Scheduled work | Name the completed work plus `Job` | `ExpiredReservationCleanupJob` |

Do not rename `UserController` to `UserRestController` merely because the application is REST-only when the package and project convention already make that clear.

Do not use `Manager`, `Coordinator`, `Processor`, `Handler`, `Helper`, `Utils`, or `Facade` as default escape hatches. Use them only when the pattern and exact responsibility are real and documented by the owning architecture.

Use `UserServiceImpl` for the standard application-service implementation. When multiple implementations differ by stable behavior or mechanism, use distinguishing names such as `CachedCatalogService` instead of several ambiguous `*Impl` classes.

## Name methods

Use `lowerCamelCase` and start behavioral methods with a verb or verb phrase.

Preserve established service forms when their contracts match:

```text
getById
getAll
create
updateById
deleteById
```

Choose verbs by semantics, not habit:

| Intent | Prefer |
| --- | --- |
| Find a possibly absent value | `findByEmail` returning `Optional` |
| Require an existing value or fail | `getById` when that is the established service contract |
| Test existence without loading | `existsByEmail` |
| Create and persist | `create` |
| Change an existing aggregate | `updateById` |
| Remove by identity | `deleteById` |
| Map a service result to a response TO | `mapUserDomainToUserTO` |
| Map persistence output to domain | `mapUserEntityToUserDomain` |
| Map explicit creation values to a new entity | `mapToNewUserEntity` |
| Validate and throw on failure | `validateOrderTransition` or a domain-specific verb |
| Boolean query | `isActive`, `hasPermission`, `canRetry` |

Do not use `get` for a boolean predicate. Do not use `check`, `process`, `handle`, `execute`, `perform`, or `doWork` when a domain verb can state the behavior.

Make side effects visible when confusion is likely. A name must not imply a pure calculation if it persists, publishes, deletes, charges, or performs remote I/O.

Avoid repeating context already supplied by the receiver:

```java
user.activate();
order.cancel();
reservation.expire();
```

Prefer these over `user.activateUser()` or `order.cancelOrder()`.

For repository methods, follow Spring Data parsing semantics and keep derived names readable:

```text
findByEmail
existsByEmail
findByStatusOrderByCreatedAtDescIdDesc
```

Move a complex query to an explicitly named repository method or custom repository implementation according to `spring-data-jpa`; do not encode an unreadable query solely to avoid `@Query`.

For this project, make every controller handler method name exactly match its OpenAPI `operationId`, using the deterministic path-and-HTTP-method convention from [API, data, and configuration names](api-data-and-configuration-names.md#name-openapi-operations-and-schemas). For example, `GET /users/{userId}` maps to `usersUserIdGet`. Treat both names as public tooling contracts and migrate existing consumers before renaming either one.

## Name fields, parameters, and local variables

Use `lowerCamelCase`.

- Name a value by its business meaning: `reservationExpiry`, not `date`.
- Use singular nouns for one value and plural nouns for collections: `role`, `roles`.
- Use `userId` when multiple identifiers are in scope; allow `id` when the owner is unambiguous.
- Name maps by both sides when useful: `usersById`, `permissionByCode`.
- Name bounds explicitly: `minimumAmount`, `maximumPageSize`.
- Use positive boolean predicates: `active`, `emailVerified`, `hasPermission`.
- Avoid `notDisabled`, `isNotInvalid`, and other double negatives.
- Avoid generic names such as `data`, `info`, `obj`, `temp`, `result`, `response`, or `list` when the concrete concept is known.
- Allow `result`, `request`, or `response` in a tiny scope only when the method and type make the meaning unambiguous.
- Do not encode a mutable implementation in the name, such as `userArrayList`; use `users`.
- Keep lambda parameters short only when the stream remains immediately readable. Prefer `user` over `u` outside a trivial expression.
- Use `ignored` only for a deliberately unused value and when the language or API requires a parameter.

Do not add `this.` or `final` based on this reference; follow `modern-java-21`.

## Name constants and enum values

Use `UPPER_SNAKE_CASE` for true constants and enum constants:

```text
DEFAULT_PAGE_SIZE
MAXIMUM_RETRY_ATTEMPTS
ACTIVE
PENDING_APPROVAL
```

Do not make a mutable static field look constant. Do not encode a value that can drift:

```text
BAD: THIRTY_DAY_RETENTION
BETTER: DEFAULT_RETENTION
```

Use the type or configuration to hold the actual duration. When a protocol or external contract defines a recognized name, preserve that spelling and document the mapping where required.

Treat enum renames as compatibility and data migrations when values are serialized, stored, queried, or exposed. Do not rely unintentionally on `Enum.name()` as a public wire format.

## Name exceptions

Choose exception granularity from the handling contract, not from the number of validation rules or state transitions.

- Reuse an existing JDK, framework, or established project exception when it already represents the condition.
- Use a shared category such as `ValidationException`, `InvalidStateException`, or `ResourceNotFoundException` when multiple failures intentionally have the same handling and error contract.
- Create a more specific exception only when the condition needs different recovery, translation, error code, or context.
- Do not create one custom exception per validation rule or state transition.
- Use broad base exceptions such as `BusinessException` only for a deliberate hierarchy; avoid vague concrete names such as `ApplicationException`, `ServiceException`, or `SomethingWentWrongException`.
- Do not leak provider-specific names through a service contract unless callers are expected to handle that provider condition.

## Name tests and test data

Keep one coherent test naming style within a module. Preserve a sound established style. When no convention exists, use:

```text
<methodUnderTest>_when<Condition>_<ExpectedOutcome>
```

Examples:

```java
@Test
void updateById_whenUserExists_updatesAndReturnsUserDomain() {
}

@Test
void updateById_whenUserDoesNotExist_throwsResourceNotFoundException() {
}
```

Use `@DisplayName` when a readable behavior sentence adds information, parameterized cases need labels, or reports are consumed by non-developers. Do not duplicate a clear method name mechanically.

Name test classes by tested boundary:

```text
UserServiceTest
UserControllerTest
UserRepositoryIntegrationTest
UserApiIntegrationTest
ExpiredReservationCleanupJobTest
ExpiredReservationCleanupJobIntegrationTest
```

Do not call an integration test `*Test` when the project distinguishes test phases by suffix. Match Maven or Gradle test selection.

Name fixtures and test data by scenario:

```text
existingUser
activeCustomer
expiredReservation
orderWithoutItems
```

Avoid `user1`, `user2`, `testData`, and `mockObject` unless the numbering itself is the tested distinction. Name a test double by its role, such as `userRepository`; use `mockUserRepository` only when distinguishing it from another repository instance is necessary.

## Recognize weak names

Reject or question:

- names that contradict behavior or return type;
- the same concept named differently across controller, service, repository, message, and schema without a contract reason;
- one name reused for business identity, database identity, and authentication identity when those differ;
- noise words and numeric suffixes;
- framework or provider names leaking into domain types;
- package names that mirror an organization chart or temporary project;
- `*Impl` outside an application-service contract or without a real responsibility;
- accessors or predicates with misleading verbs;
- names that expose credentials, customer data, tenant names, or vulnerabilities;
- broad renames with no consumer inventory or compatibility plan.

## Review examples

| Weak | Prefer | Reason |
| --- | --- | --- |
| `UserData` | `UserDomain` or `UserTO` | State the actual boundary role |
| Empty `UserService` paired with `UserServiceImpl` | An operation-bearing `UserService` contract and `UserServiceImpl` | Keep the application boundary meaningful |
| `processUser` | `activateUser` | State the domain action |
| `checkEmail` | `existsByEmail` or `validateEmail` | State whether the method queries or validates |
| `flag` | `emailVerified` | State the predicate |
| `list` | `users` | State element meaning and plurality |
| `map` | `usersById` | State key and value meaning |
| `date` | `reservationExpiry` | State business meaning |
| `MAX_RETRIES_3` | `MAXIMUM_RETRY_ATTEMPTS` | Keep the value out of the name |
| `UserDto` | `UserTO` | Preserve project terminology |

## Primary guidance

- [Google Java Style Guide: Naming](https://google.github.io/styleguide/javaguide.html#s5-naming)
- [JUnit User Guide: Display Names](https://docs.junit.org/current/user-guide/#writing-tests-display-names)
- [Spring Framework Code Style](https://github.com/spring-projects/spring-framework/wiki/Code-Style)
