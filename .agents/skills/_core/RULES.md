# Core rules

The rules that hold in every task, in every file, whichever agent is running. This card exists so a
tool with a small instruction budget can carry the whole set's non-negotiables without loading
eleven skills.

**This card is a summary, never a substitute.** Each rule names its owner, and the owner is
authoritative for scope, exceptions, and detail. When this card and an owner skill disagree, the
owner wins and this card is the defect to fix. Read the owner skill before implementing anything it
covers; read the full map in [OWNERSHIP.md](OWNERSHIP.md).

An owner keeps its always-loaded body to the decisions and the rules that decide most reviews, and
routes the rest to its references — some rule sets live **only** there. Each skill's reference-routing
section says which file to open for which change; "read the owner skill" means following that routing,
not reading its `SKILL.md` and stopping.

## Before writing production code

1. **`docs/project-profile.md` must exist and record every decision the task touches.** No database,
   build tool, Spring Boot generation, service convention, accessor style, migration tool, contract
   direction, or version is assumed, inferred from a test dependency, or copied from an example.
   (`spring-boot-patterns`)
2. **Three decision tokens, no synonyms.** `ASK` blocks until the user answers, unless the row
   records a fallback — then apply the fallback, write it into the profile, and say so in the
   handoff. `RESOLVE` never blocks: look it up, record it with the date. `UNDECIDED` is a legitimate
   deferral that names what will force the decision. **Never write a version or coordinate from
   memory.** (`spring-boot-patterns`, `build-and-dependencies`)
3. **Read the whole affected path before changing it**: controller or listener, service, domain,
   repository, adapters, configuration, tests.

## Java source, every touched file

4. **Imports:** no wildcards, no unused, no duplicates. Group order is `static`, `java`, `jakarta`,
   `javax`, `com`, `org`, everything else; sorted within a group; exactly one blank line between
   non-empty groups. This order beats any tool's default. (`modern-java-21`)
5. **No `var`.** Local variable types are explicit everywhere, including tests and generated-source
   templates. (`modern-java-21`)
6. **`this.` qualifies every instance field and instance method access.** (`modern-java-21`)
7. **`final` on fields, parameters, and single-assignment locals.** Framework-assigned test fixtures
   (`@Mock`, `@InjectMocks`, `@MockitoBean`, a subject rebuilt in `@BeforeEach`) are the only
   exception. (`modern-java-21`)
8. **Constructor injection only.** No field injection, no service locators, no static mutable state.
   A generated MapStruct `INSTANCE` is the one allowed exception. (`modern-java-21`)
9. **Methods over 100 lines and classes over 1000 lines are refactored, not justified.** Eight or
   more parameters is a design warning, not a threshold to satisfy with a wrapper object.
   (`modern-java-21`)
10. **Inject `Clock`.** No `Instant.now()` in business logic, no `Thread.sleep` for coordination.
    (`modern-java-21`)
11. **Nullability is stated, not implied.** Every main-source package carries `@NullMarked` in its
    `package-info.java`; `@Nullable` marks the exceptions; the vocabulary is JSpecify on both Spring
    Boot generations and nothing else is mixed in. The annotations bind no runtime check, so
    `Objects.requireNonNull` and Bean Validation stay where they are. (`modern-java-21`)

## Layers

12. **No entity crosses the HTTP boundary in either direction, and no TO enters the service layer.**
    (`spring-boot-patterns`)
13. **Controllers are thin.** No repository, no entity mutation, no business rule, no transaction, no
    generic `catch`. One handler calls one service, at the level the operation belongs to.
    (`spring-boot-patterns`)
14. **Two service levels.** An aggregate service owns one aggregate root, holds its repositories, and
    holds no other service. An application service coordinates aggregate services, holds no
    repository, and never forwards a single call. (`spring-boot-patterns`)
15. **The transaction boundary is the highest service in the use case** — the application service
    when one exists, otherwise the aggregate service. No aggregate service overrides propagation or
    isolation to escape it. (`spring-boot-patterns`)
16. **A repository is reached only from the aggregate service that owns it.**
    (`spring-boot-patterns`)
17. **Never rely on self-invocation** for `@Transactional`, `@Async`, `@Cacheable`, retry, or method
    validation. (`spring-boot-patterns`)

## Errors and contracts

18. **RFC 9457 `ProblemDetail`, and the `type` URI is its only machine-readable identifier.** No
    parallel `code` member. `correlationId` is the one permitted extension. No stack trace, exception
    class name, provider message, SQL, or `traceId` in the body. (`spring-boot-patterns`)
19. **Every caller-visible failure is declared once in the project error catalog**, with its status,
    `type` URI, title, detail, and internal code. (`spring-boot-patterns`)
20. **A public endpoint change is not complete until the contract reflects it** — every path, status,
    header, field, and error condition, including those the shared advice produces.
    (`rest-api-contract`)

## Persistence

21. **Entities: no records, no Lombok `@Data`, explicit `equals`/`hashCode`** by stable natural key
    or by identifier with a constant class-derived hash. Never lazy or mutable state in either.
    (`spring-data-jpa`)
22. **To-one associations are `LAZY`; `open-in-view` is off; no blanket `EAGER`.** Fetching is a
    query decision. (`spring-data-jpa`)
23. **An association never crosses an aggregate boundary.** Reference another root by identifier.
    (`spring-boot-patterns`, `spring-data-jpa`)
24. **Every read that can grow with production data is bounded**, deterministically sorted, and
    paginated with an enforced maximum. (`spring-data-jpa`)
25. **Optimistic locking with `@Version` is the default**, and contention is absorbed by the
    project's composed `@OptimisticLockingRetry` annotation at the use-case boundary, never by a
    hand-written loop and never by the caller. The annotation is project-owned on both Spring Boot
    generations and its call sites are identical; only the retry engine it composes differs.
    Structurally blocking invariants take a pessimistic lock without needing a measurement; only
    buying a lock for throughput does. (`spring-data-jpa`)
26. **Every schema change is a migration file, committed with the mapping change**, forward-only, and
    never edited after it is applied anywhere. Hibernate `ddl-auto` is `validate` or `none`.
    (`sql-database-migration`)

## Outbound and security

27. **No outbound call without explicit connection and read timeouts**, inside the caller's budget.
    Retry policy exists in exactly one layer. Provider exceptions are translated at the adapter.
    (`spring-boot-patterns`)
28. **Authorization is enforced in the service and persistence path**, not only at the controller.
    Identity, tenant, and ownership come from the authenticated context, never from a request field.
    (`application-security`)
29. **Never log or hardcode credentials, tokens, personal data, full request or response bodies, or
    SQL with parameters.** No secret in a build file, a migration, or a test fixture.
    (`application-security`)

## Observability

30. **A failure is logged once, at the boundary that handles it**, with the internal error code and
    the correlation identifier. Never catch, log, and rethrow. (`observability-and-logging`)
31. **Metric tag values are bounded.** Never an identifier, email, tenant, raw URL, timestamp, or
    exception message. (`observability-and-logging`)

## Tests

32. **Production behavior and its tests ship together.** Never weaken, disable, or delete a test to
    make a change pass, and never relax a security control to make a test pass.
    (`spring-boot-testing`)
33. **Each level proves a different boundary:** aggregate and application services get Spring-free
    unit tests, every controller gets a `@WebMvcTest` slice with filters disabled, and integration
    tests run against the real database engine with migrations applied and the real filter chain.
    (`spring-boot-testing`)
34. **Integration tests must actually run, once, in the right phase.** On Maven the suffix also
    matches Surefire's default pattern, so an unexcluded suite runs twice; on Gradle nothing runs it
    until a suite is registered, and an unrun suite looks exactly like a green build.
    (`spring-boot-testing`, `build-and-dependencies`)

## Dependencies

35. **Every dependency names the requirement it satisfies**, duplicates no existing capability, and
    carries no version when the Spring Boot BOM manages it. Every plugin version is pinned.
    (`build-and-dependencies`)
36. **Quality gates fail the build**, run before the tests, and are never silenced with a suppression
    file, a baseline, or `@SuppressWarnings`. (`build-and-dependencies`)

## Worked examples

37. **Every snippet in this set is a pattern to adapt, not a file to copy.** A request for a product
    service produces `ProductService` written from scratch, not a renamed `UserService`.
    (`modern-java-21`)
