---
name: project-naming-conventions
description: Naming and safe renaming of developer-owned names in Java 21+ Spring Boot REST projects. Use when creating or renaming Java identifiers, packages, tests, REST paths and fields, OpenAPI components, database objects, configuration, cache keys, jobs, metrics, spans, or structured-log fields, and when resolving inconsistent terminology. Excludes infrastructure-resource naming.
---

# Project Naming Conventions

Choose names that preserve business meaning, architectural boundaries, compatibility, security, and operational clarity. Treat naming as part of the contract whenever another component, deployment, database, dashboard, or team consumes the name.

## Coordination with other skills

This skill owns naming vocabulary, identifier form, cross-boundary consistency, and rename safety.
Let the specialized skills own behavior.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what, and
it carries the precedence order for a genuine conflict. Read it there rather than from a copy in
this file. The seams this skill crosses most often:

**This skill owns the name; another skill owns the thing.** That split holds for Java types
(`modern-java-21` owns type design, `spring-boot-patterns` the architectural role), exceptions
(`spring-boot-patterns` owns the handling contract that decides which exception exists), database
objects (`spring-data-jpa` the mapping, `sql-database-migration` the migration), meters, spans and
log fields (`observability-and-logging` owns which must exist), contract names (`rest-api-contract`
owns compatibility, versioning, deprecation) and tests (`spring-boot-testing` owns scope and
execution). For contract names this skill additionally owns how one may be migrated.

Apply every relevant owner skill before choosing a name. **Never use naming to introduce** a new
architectural layer, CQRS terminology, interface, abstraction, database object, message type, metric,
feature flag, or infrastructure resource the design does not require.

Preserve the established terminology — `UserCreateTO`/`UserTO` for the REST contract, `UserDomain`
for a project-owned domain type, `UserEntity` for the JPA model, `UserSummaryProjection` for a
repository read projection, `UserRestMapper` and `UserDomainMapper` for the two mapping directions.
Never replace them with DTO, View, Model, Command, or Query terminology unless the project explicitly
adopts a different architecture and migrates to it. Apply the exact scope and exceptions of the
`<Concept>Domain` convention from
[Java, Spring, and test names](references/java-spring-and-test-names.md); do not redefine it
elsewhere.

## Keep application and platform naming separate

Apply this skill directly to names owned by application code or its public/runtime contracts:

- Java source, packages, modules, and tests;
- REST, JSON, OpenAPI, errors, database objects, migrations, and Spring configuration;
- application event types and schemas, publisher/consumer classes, logical destination properties, scheduled jobs, executors, cache names and keys, Micrometer meters, custom spans, and structured-log fields.

Defer physical infrastructure names to the repository's approved platform or DevOps standard:

- AWS and other cloud resources, IAM roles and policies, buckets, physical queues/topics, KMS aliases, and secret-store paths;
- Kubernetes, Helm, Terraform, CloudFormation, DNS, container repositories/tags, CI/CD jobs, infrastructure labels, and cost-allocation tags.

Ownership follows the artifact and repository policy, not a person's job title. When the same repository contains infrastructure as code, load its platform naming standard before editing those artifacts. If no approved standard is available, do not invent a universal physical naming scheme; ask for the missing convention. Still apply `application-security` to prevent confidential data from appearing in any name.

Application code should refer to physical resources through typed configuration named by application purpose. The platform supplies the environment-specific physical value. Use a provider-specific property namespace only when provider behavior is intentionally part of the application contract.

## Reference routing

- Read [Java, Spring, and test names](references/java-spring-and-test-names.md) for identifiers, packages, modules, architectural roles, Spring components, exceptions, tests, and acronyms.
- Read [API, data, and configuration names](references/api-data-and-configuration-names.md) for REST, JSON, OpenAPI, problem type URIs and internal error codes, database objects, migrations, configuration, environment variables, profiles, and feature flags.
- Read [application messaging and observability names](references/application-messaging-and-observability-names.md) for event/message types, publisher and consumer classes, logical destination properties, jobs, executors, cache keys, metrics, tags, custom spans, and structured-log fields. `observability-and-logging` owns what must be instrumented and how; this skill owns what those meters, spans, and fields are called.
- Load every reference whose resource type is created, renamed, serialized, persisted, published, monitored, or provisioned by the change. Avoid loading unrelated references for a narrow local rename.

## Apply the rule hierarchy

Resolve naming decisions in this order:

1. Protect confidentiality and comply with legal, regulatory, provider, protocol, language, and tool constraints.
2. Preserve compatible public, persisted, asynchronous, operational, and deployment contracts.
3. Follow explicit organization standards, the repository glossary, approved architecture decisions, schemas, and API or event specifications.
4. Follow the semantics and boundaries defined by the applicable owner skills.
5. Apply this skill's defaults.
6. Follow local precedent only when it is deliberate, coherent, and compatible with the higher rules.

Do not silently preserve an unsafe or misleading name merely because it already exists. Do not silently break a contract merely to normalize style. Report the conflict and use a controlled migration.

Interpret requirement levels as follows:

- **Must**: required for correctness, compatibility, security, provider validity, or an explicit project standard.
- **Default**: use when the project has no stronger convention; preserve a coherent existing alternative.
- **Avoid**: require a concrete reason and make the trade-off visible.

Do not convert subjective readability advice into a blocking rule when multiple names communicate the same concept accurately.

## Establish the vocabulary

Before naming:

1. Inspect `docs/project-profile.md`, the glossary, API and event schemas, database migrations, configuration metadata, observability conventions, and nearby sound code. The profile records decisions that change names, such as the JPA accessor style, the service interface convention, and whether a cache exists.
2. Identify the business concept, its owner, lifecycle, scope, and whether the name is internal, public, persisted, externally provisioned, or operationally queried.
3. Reuse the approved domain term for the same concept across layers. Use different names only when the concepts or contracts genuinely differ.
4. Resolve synonyms and overloaded words with the domain owner. Do not guess between materially different business meanings.
5. Record a durable cross-team term in the existing project glossary or naming standard when the task includes that documentation. Otherwise, state the unresolved convention in the handoff.

Prefer domain language over framework language in business-facing types and operations. Use technical vocabulary for technical mechanisms. Do not hide an important domain distinction behind a generic technical name.

## Create intention-revealing names

These are readability **heuristics** — *Clean Code*'s naming guidance applied through the project's
domain language and the rule hierarchy above. A subjective example never overrides a real contract or
platform constraint, and the review rules below say when a heuristic becomes a finding.

- Name the concept or behavior, not its representation or temporary implementation, and keep the name accurate after behavior changes. A name that has outlived its behavior is disinformation, and it is worse than a vague one because it is trusted.
- **One word per concept, one concept per word.** Do not alternate between `customer`, `client`, and `user` unless they are genuinely different, and do not reuse one word for different concepts in the same bounded context.
- Make meaningful distinctions: reject numeric suffixes and noise words — `data`, `info`, `object`, `item`, `value`, `manager`, `processor`, `helper` — when they do not narrow meaning.
- Prefer pronounceable, searchable names, and only approved domain, protocol, vendor, and technical abbreviations. Avoid encodings: Hungarian notation, member or interface prefixes, unexplained implementation suffixes, embedded type names.
- Match length to scope — concise loop indices only in tiny conventional scopes, explicit names once a value crosses a boundary. Plural nouns for collections, singular for one value.
- Name booleans as positive predicates (`active`, `hasPermission`, `canRetry`); avoid double negatives.
- Include units only when a stronger type cannot express them, such as an unavoidable primitive `timeoutMillis`. Prefer `Duration timeout` where the owning skill permits it.
- Name symmetric concepts symmetrically, and lifecycle states from one coherent vocabulary.

Use Javadoc to explain a non-obvious contract, never to compensate for a vague name; follow the
selective Javadoc policy `modern-java-21` owns.

## Handle acronyms consistently

Default to treating acronyms as words in Java identifiers:

```text
HttpClient
UrlResolver
UuidGenerator
userId
apiResponse
AwsCredentialsProvider
S3ObjectClient
```

Preserve standard or explicitly approved project forms when compatibility or established terminology requires them. In this project, keep the `TO` suffix:

```text
UserTO
UserCreateTO
UserUpdateTO
```

Do not create competing forms such as `UserTo`, `UserDto`, and `UserTO`. Do not change serialized fields, database identifiers, event types, or configuration keys solely to normalize acronym capitalization.

## Name by responsibility

Use a role suffix only when the type performs that role. Prefer the specific responsibility over a generic suffix:

| Prefer | Avoid without a specific responsibility |
| --- | --- |
| `UserService` | `UserManager` |
| `UserRepository` | `UserDataAccess` |
| `UserRestMapper` | `UserConverterUtil` |
| `CatalogClient` | `CatalogHelper` |
| `ExpiredReservationCleanupJob` | `ReservationProcessor` |
| `ResourceNotFoundException` | `OrderException` |

Exception names follow the handling contract, not the resource. An order is a resource, so a missing
order is a `ResourceNotFoundException`; introduce `OrderNotFoundException` only when that condition
needs a different status, problem type, or recovery from every other missing resource. See
[Name exceptions](references/java-spring-and-test-names.md#name-exceptions).

Follow the service-interface decision recorded in the project profile and owned by `spring-boot-patterns`. Use
`<Name>Impl` when the user selected that convention or the repository already applies it
coherently; otherwise, `Impl` is not required. Do not create an interface only to produce an `Impl`
class. When multiple implementations differ by stable behavior or mechanism, prefer names such as
`HttpCatalogClient` and `InMemoryCatalogClient`.

## Validate every proposed name

Check that the name:

- uses the approved domain term and correct architectural role;
- follows the convention for its resource type;
- remains unambiguous in logs, stack traces, dashboards, generated clients, database tools, and broker consoles used by the application team;
- is valid for every target compiler, serializer, database, broker, registry, and operating system that consumes it;
- respects case folding, reserved words, delimiter, character, and length limits;
- does not expose secrets, personal data, tenant names, customer names, internal vulnerabilities, or other confidential information;
- avoids collisions after normalization by frameworks, providers, exporters, and case-insensitive filesystems;
- remains stable under scaling, deployment, and multi-environment operation;
- can be discovered with an exact repository, log, metric, or contract search.

Use automated formatters, compiler checks, schema validators, migration validators, OpenAPI validation, and project linters when they enforce the convention. Do not claim a name is provider-valid or backward-compatible without checking the actual target and project version.

## Rename safely

**Treat a rename as a migration whenever the name can escape the local compilation unit**, and assume
consumers cannot upgrade atomically unless deployment evidence proves otherwise. A direct rename is
correct only for a fully internal, atomically deployable name.

Everything else — the inventory-and-classify procedure, the compatibility mechanisms, alias removal
criteria, the update order, the mixed-version verification, and the list of names that are never
renamed for aesthetics alone — is in
[Migrate escaped names](references/api-data-and-configuration-names.md#migrate-escaped-names). Read
it before renaming anything that leaves the file it is declared in.

Do not mix an otherwise mechanical rename with unrelated behavior changes. Where separation is
impractical, make the behavioral delta explicit and test it independently.

## Review naming changes

During code review:

- report a naming issue as a defect only when it violates an applicable must-rule, misrepresents behavior, creates ambiguity with concrete risk, breaks compatibility, leaks protected information, or prevents reliable operation;
- treat a clearer but equally accurate alternative as a suggestion, not a blocking finding;
- inspect all affected representations rather than reviewing the Java rename in isolation;
- reject broad naming churn that expands risk without a defined benefit and migration;
- identify pre-existing inconsistency separately from risk introduced by the change;
- use `spring-boot-code-review` for evidence, severity, and final reporting.

## Completion checklist

- [ ] The relevant owner skills and resource references were applied.
- [ ] The approved business term and architectural role are clear.
- [ ] The name follows the relevant Java, API, data, configuration, messaging, cache, or observability convention.
- [ ] Acronyms, singular/plural form, predicates, suffixes, and delimiters are consistent.
- [ ] Confidential values and unbounded-cardinality identifiers are absent.
- [ ] Compiler, schema, broker/exporter, and normalization constraints were checked where applicable.
- [ ] Definitions, consumers, generated artifacts, tests, documentation, and operational dependencies were updated.
- [ ] Compatibility, deployment order, rollback, and temporary alias removal were handled for escaped names.
- [ ] The final change avoids unrelated naming churn and preserves the established TO–Domain–Entity terminology.

## Primary guidance

- [Google Java Style Guide: Naming](https://google.github.io/styleguide/javaguide.html#s5-naming)
- [Spring Boot: Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- [Micrometer: Naming Meters](https://docs.micrometer.io/micrometer/reference/concepts/naming.html)
