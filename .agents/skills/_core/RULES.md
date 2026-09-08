# Core rules

The rules that hold in every task, in every file, whichever agent is running. This card exists so a
tool with a small instruction budget can carry the whole set's non-negotiables without loading
twelve skills.

**This card is a summary, never a substitute.** Each rule names its owner, and the owner is
authoritative for scope, exceptions, and detail. When this card and an owner skill disagree, the
owner wins and this card is the defect to fix. Read the owner skill before implementing anything it
covers; read the full map in [OWNERSHIP.md](OWNERSHIP.md).

**A summary drifts in one direction: stricter.** Compressing a rule drops its scope and its
exceptions first, because those are the clauses that read as detail — and a card that forbids more
than its owner does is not a safe simplification, it is a rule nobody can follow and nobody owns.
So a change to any rule here is a change to two files: the owner, and this card. Verify the pair
against each other, in the same change.

An owner keeps its always-loaded body to the decisions and the rules that decide most reviews, and
routes the rest to its references — some rule sets live **only** there. Each skill's reference-routing
section says which file to open for which change; "read the owner skill" means following that routing,
not reading its `SKILL.md` and stopping.

## How strong each rule is

Every rule below is binding on a project that adopts this set. What differs is what happens when a
**second project** wants a different answer, and that difference is invisible if the rules are read
as one flat list.

This is a different axis from the **Must / Default / Avoid** vocabulary
`project-naming-conventions` defines for naming decisions. That one grades how binding a rule is on
*this* project; this one grades what a *different* project may change. A rule can be a Must here and
still be house style — `no var` is exactly that. Do not merge the two scales.

| Strength | What it means | What a second project may do |
| --- | --- | --- |
| **Correctness** | Breaking it produces wrong, unsafe, or unoperable software | Nothing. It is not a project preference on any project |
| **Architecture** | The shape the rest of this set assumes — layers, aggregates, the transaction boundary, the error contract | Choose differently, and accept that much of this set no longer applies. That is a fork, not a setting |
| **House style** | This organization's chosen convention, where more than one answer is defensible | Change it — but change the **rule and its gate together**, deliberately and once. Never suppress it at a call site, and never let two projects using this set disagree silently |

The house-style rules on this card are **4** (import order and grouping), **5** (no `var`), **6**
(`this.` qualification), **7** (`final` placement), **9** (the size thresholds), and the *vocabulary*
in **12** — `TO`, `Domain`, `Entity` are names this project chose, while the boundary those names
describe is architecture. Everything else here is correctness or architecture.

Reading this the wrong way is the expensive mistake, and it goes in both directions: treating a
house-style rule as correctness produces an argument nobody can win, and treating a correctness rule
as taste produces a defect nobody reports. When two rules genuinely conflict, the precedence order
resolves it, and it resolves it in this direction — style and vocabulary yield to correctness, never
the reverse.

## Before writing production code

1. **`docs/project-profile.md` must exist and record every decision the task touches.** No database,
   build tool, Spring Boot generation, service convention, accessor style, migration tool, contract
   direction, or version is assumed, inferred from a test dependency, or copied from an example.
   (`project-decision-profile`)
2. **Three decision tokens, no synonyms.** `ASK` blocks until the user answers, unless the row
   records a fallback — then apply the fallback, write it into the profile, and say so in the
   handoff. `RESOLVE` never blocks: look it up, record it with the date. `UNDECIDED` is a legitimate
   deferral that names what will force the decision. **Never write a version or coordinate from
   memory.** (`project-decision-profile`, `build-and-dependencies`)
3. **Read the whole affected path before changing it**: controller or listener, service, domain,
   repository, adapters, configuration, tests.

## Java source, every touched file

4. **Imports:** no wildcards, no unused, no duplicates. Group order is `static`, `java`, `jakarta`,
   `javax`, `com`, `org`, everything else; sorted within a group; exactly one blank line between
   non-empty groups. This order beats any tool's default. (`modern-java-21`)
5. **No `var`.** Local variable types are explicit everywhere, including tests and generated-source
   templates. (`modern-java-21`)
6. **`this.` qualifies every instance field and instance method access.** (`modern-java-21`)
7. **`final` on parameters, single-assignment locals, and fields**, except where mutation is the
   object's own responsibility — a JPA entity's mapped state — and except the framework-assigned
   test fixtures (`@Mock`, `@InjectMocks`, `@MockitoBean`, a subject rebuilt in `@BeforeEach`) that
   the owner names as an explicit exception. The gate checks parameters and locals only, because no
   static check can tell a dependency field from a mapped one. (`modern-java-21`)
8. **Constructor injection only.** No field injection, no service locators, no static mutable state.
   A generated MapStruct `INSTANCE` is the one allowed exception. (`modern-java-21`)
9. **A method over 100 lines and a class over 1000 lines are refactored; the build enforces both.**
   Between 41 and 100 lines a method is a judgement call the owner grades in two bands, and a
   documented reason can hold it open there — above 100, and for a class above 1000, it cannot,
   because the gate has no suppression. Eight or more parameters is a design warning, not a
   threshold to satisfy with a wrapper object. (`modern-java-21`)
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
21. **The exception advice answers no security denial; it declines both families.** Declare one
    handler for `AccessDeniedException` and one for `AuthenticationException`, each of which
    rethrows the exception unchanged and returns nothing, so `ExceptionTranslationFilter` still
    produces the `401` with its challenge or the `403`. Declining one and not the other leaves that
    family reaching the catch-all as `500`, which passes every test that always sends a token.
    (`spring-boot-patterns`, `application-security`)

## Persistence

22. **Entities: no records, no Lombok `@Data`, explicit `equals`/`hashCode`** by stable natural key
    or by identifier with a constant class-derived hash. Never lazy or mutable state in either.
    (`spring-data-jpa`)
23. **To-one associations are `LAZY`; `open-in-view` is off; no blanket `EAGER`.** Fetching is a
    query decision. (`spring-data-jpa`)
24. **An association never crosses an aggregate boundary.** Reference another root by identifier.
    (`spring-boot-patterns`, `spring-data-jpa`)
25. **Every read that can grow with production data is bounded**, deterministically sorted, and
    paginated with an enforced maximum. (`spring-data-jpa`)
26. **Optimistic locking with `@Version` is the default**, and contention is absorbed by the
    project's composed `@OptimisticLockingRetry` annotation at the use-case boundary, never by a
    hand-written loop and never by the caller. The annotation is project-owned on both Spring Boot
    generations and its call sites are identical; only the retry engine it composes differs.
    A structurally blocking invariant needs a mechanism stronger than retry without waiting for a
    measurement — a pessimistic lock, or a single conditional `UPDATE` where the whole rule fits one
    row and one `WHERE` clause. Only buying a lock for throughput needs evidence.
    (`spring-data-jpa`)
27. **Contention reaches the caller through the REST advice, from the framework's own exception
    types.** An exhausted optimistic retry is `409`; a lock timeout, deadlock victim, serialization
    failure, or a statement the database cancelled while it waited is `503` with `Retry-After`,
    derived from the recorded lock timeout rather than written down again. Neither is caught in a
    service, and neither may fall through to the catch-all as `500`.
    (`spring-boot-patterns`, `spring-data-jpa`)
28. **Every wait is bounded by a mechanism the configured engine actually honours** — statement,
    transaction, connection acquisition, and lock — with the numbers read from the profile and the
    connection-level settings delivered through the one channel a pool has. A default of "no limit"
    reports nothing, an ignored setting throws nothing, and both look exactly like a bound that
    works, so each one is proven by executing it. Migrations do not inherit the request-sized
    bounds. (`spring-data-jpa`)
29. **Every schema change is a migration file, committed with the mapping change**, forward-only, and
    frozen once it has been applied to any **shared** environment — never edited, renamed,
    renumbered, or deleted after that. Editing is allowed only while it sits on a feature branch and
    has run nowhere but the author's own database. Hibernate `ddl-auto` is `validate` or `none`.
    (`sql-database-migration`)

## Outbound and security

30. **The concurrency model is recorded, and changing it moves the limit rather than removing it.**
    Virtual threads or a sized platform pool; either way the database pool, the per-caller limits,
    and the outbound client pools are re-derived in the same change, because with the server's
    thread pool gone they are the only bounds left. No synchronous request has an in-process
    timeout, so the recorded request budget is held by the arithmetic of its parts and by the
    ingress. (`spring-boot-patterns`)
31. **No outbound call without explicit connection and read timeouts**, inside the caller's budget.
    Retry policy exists in exactly one layer. Provider exceptions are translated at the adapter.
    (`spring-boot-patterns`)
32. **Authorization is enforced in the service and persistence path**, not only at the controller.
    Identity, tenant, and ownership come from the authenticated context, never from a request field.
    (`application-security`)
33. **Never log or hardcode credentials, tokens, personal data, full request or response bodies, or
    SQL with parameters.** No secret in a build file, a migration, or a test fixture.
    (`application-security`)

## Observability

34. **A failure is logged once, at the boundary that handles it**, with the internal error code and
    the correlation identifier. Never catch, log, and rethrow. (`observability-and-logging`)
35. **Metric tag values are bounded.** Never an identifier, email, tenant, raw URL, timestamp, or
    exception message. (`observability-and-logging`)

## Tests

36. **Production behavior and its tests ship together.** Never weaken, disable, or delete a test to
    make a change pass, and never relax a security control to make a test pass.
    (`spring-boot-testing`)
37. **Each level proves a different boundary:** aggregate and application services get Spring-free
    unit tests, every controller gets a `@WebMvcTest` slice with filters disabled, and integration
    tests run against the real database engine with migrations applied and the real filter chain.
    (`spring-boot-testing`)
38. **Integration tests must actually run, once, in the right phase.** On Maven the suffix also
    matches Surefire's default pattern, so an unexcluded suite runs twice; on Gradle nothing runs it
    until a suite is registered, and an unrun suite looks exactly like a green build.
    (`spring-boot-testing`, `build-and-dependencies`)

## Dependencies

39. **Every dependency names the requirement it satisfies**, duplicates no existing capability, and
    carries no version when the Spring Boot BOM manages it. Every plugin version is pinned.
    (`build-and-dependencies`)
40. **Quality gates fail the build** and run before the tests. A suppression file, a baseline, or a
    `@SuppressWarnings` used to silence a **project rule** is prohibited; if a rule does not fit,
    change the rule and say so in review. This is written for a greenfield project, which is what
    the set assumes — adopting the set into a repository that already has code is a different
    problem, and the owner states how. (`build-and-dependencies`)

## Worked examples

41. **Every snippet in this set is a pattern to adapt, not a file to copy.** A request for a product
    service produces `ProductService` written from scratch, not a renamed `UserService`.
    (`modern-java-21`)
