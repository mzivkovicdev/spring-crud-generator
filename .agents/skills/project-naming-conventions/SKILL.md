---
name: project-naming-conventions
description: Define, apply, review, and safely migrate developer-owned names across serious commercial Java 21+ Spring Boot REST projects. Use when creating or renaming Java identifiers, packages, modules, tests, REST paths and fields, OpenAPI components, database objects and migrations, Spring configuration, feature flags, cache keys, application messages, jobs, metrics, traces, or structured-log fields; when resolving inconsistent terminology; and when reviewing naming-related changes. Coordinate modern-java-21, spring-boot-patterns, spring-data-jpa, application-security, and spring-boot-code-review without redefining their rules. Defer physical cloud, IAM, Kubernetes, CI/CD, container, DNS, and infrastructure-resource naming to the approved platform standard.
---

# Project Naming Conventions

Choose names that preserve business meaning, architectural boundaries, compatibility, security, and operational clarity. Treat naming as part of the contract whenever another component, deployment, database, dashboard, or team consumes the name.

## Coordinate the owner skills

Treat this skill as the owner of naming vocabulary, identifier form, cross-boundary consistency, and rename safety. Let the specialized skills own behavior:

| Skill | Treat as owner of |
| --- | --- |
| `modern-java-21` | Java language use, source structure, imports, Javadoc, nullability, exceptions, and general tests |
| `spring-boot-patterns` | REST-only architecture, TO–Domain–Entity boundaries, service contracts, mapper responsibilities, configuration design, and package responsibilities |
| `spring-data-jpa` | Persistence semantics, mappings, queries, transactions, migrations, indexes, constraints, and database behavior |
| `application-security` | Confidentiality, sensitive data, identity and tenant safety, secrets, dangerous disclosure, and cloud or messaging security |
| `spring-boot-code-review` | Review scope, evidence, severity, reporting, and merge-readiness decisions |

Apply every relevant owner skill before choosing a name. Do not use naming to introduce a new architectural layer, CQRS terminology, interface, abstraction, database object, message type, metric, feature flag, or infrastructure resource that the design does not require.

Preserve the established project terminology:

| Name | Meaning |
| --- | --- |
| `UserCreateTO`, `UserUpdateTO`, `UserTO` | REST/controller contract |
| `UserDomain` | Domain/service result |
| `UserEntity` | JPA persistence model |
| `UserSummaryProjection` | Repository read projection |
| `UserRestMapper` | Domain → response TO; request TO → focused domain/service input only when `spring-boot-patterns` permits that input |
| `UserDomainMapper` | Entity/projection → domain; explicit creation values → new entity |

Do not replace these terms with DTO, View, Model, Command, Query, or similarly overlapping terminology unless the project explicitly adopts a different architecture and migration.

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

## Route the references

- Read [Java, Spring, and test names](references/java-spring-and-test-names.md) for identifiers, packages, modules, architectural roles, Spring components, exceptions, tests, and acronyms.
- Read [API, data, and configuration names](references/api-data-and-configuration-names.md) for REST, JSON, OpenAPI, error codes, database objects, migrations, configuration, environment variables, profiles, and feature flags.
- Read [application messaging and observability names](references/application-messaging-and-observability-names.md) for event/message types, publisher and consumer classes, logical destination properties, jobs, executors, Redis keys, metrics, tags, custom spans, and structured-log fields.
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

1. Inspect the glossary, API and event schemas, database migrations, configuration metadata, observability conventions, and nearby sound code.
2. Identify the business concept, its owner, lifecycle, scope, and whether the name is internal, public, persisted, externally provisioned, or operationally queried.
3. Reuse the approved domain term for the same concept across layers. Use different names only when the concepts or contracts genuinely differ.
4. Resolve synonyms and overloaded words with the domain owner. Do not guess between materially different business meanings.
5. Record a durable cross-team term in the existing project glossary or naming standard when the task includes that documentation. Otherwise, state the unresolved convention in the handoff.

Prefer domain language over framework language in business-facing types and operations. Use technical vocabulary for technical mechanisms. Do not hide an important domain distinction behind a generic technical name.

## Create intention-revealing names

Use Robert C. Martin's *Clean Code* naming guidance as readability heuristics: reveal intent, avoid disinformation and encodings, make meaningful distinctions, use pronounceable and searchable words, and keep one word per concept. Apply those heuristics through the project's domain language and the rule hierarchy above; do not let a subjective example override a real contract or platform constraint.

Apply these readability principles:

- Name the concept or behavior, not its representation or temporary implementation.
- Use one word consistently for one concept. Do not alternate between `customer`, `client`, and `user` unless they are different concepts.
- Do not reuse one word for different concepts in the same bounded context.
- Make meaningful distinctions. Reject numeric suffixes and noise words such as `data`, `info`, `object`, `item`, `value`, `manager`, `processor`, or `helper` when they do not narrow meaning.
- Prefer pronounceable and searchable names. Use only approved domain, protocol, vendor, and technical abbreviations.
- Avoid encodings such as Hungarian notation, member prefixes, interface prefixes, implementation suffixes, or embedded type names that add no semantic information.
- Match length to scope. Use concise loop indices only in tiny conventional scopes; use explicit names when values live longer or cross boundaries.
- Name collections with plural nouns and individual values with singular nouns.
- Name booleans as positive predicates such as `active`, `hasPermission`, `canRetry`, or `isExpired`. Avoid double negatives.
- Include units or representation in a name only when a stronger type cannot express them, such as an unavoidable primitive `timeoutMillis`. Prefer `Duration timeout` when the owning skill permits it.
- Name symmetric concepts symmetrically and lifecycle states from one coherent vocabulary.
- Keep names accurate after behavior changes. Rename misleading identifiers within the authorized scope, subject to compatibility rules.

Use comments or Javadoc to explain a non-obvious contract, not to compensate for a vague name. Follow the selective Javadoc policy from `modern-java-21`.

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
| `OrderNotFoundException` | `OrderException` |

Use the application-service interface and `*ServiceImpl` convention owned by `spring-boot-patterns`. Do not create an interface only to produce an `Impl` class for helpers or types outside that boundary. When multiple implementations differ by stable behavior or mechanism, name the distinction, such as `HttpCatalogClient` and `InMemoryCatalogClient`.

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

Treat a rename as a migration when the name can escape the local compilation unit.

1. Inventory definitions and consumers with exact searches, generated-code inspection, schema or infrastructure references, and runtime configuration.
2. Classify the name as internal source, public API, serialized data, persisted schema, configuration, message contract, cache namespace, or observability contract.
3. Determine whether consumers can upgrade atomically. Assume they cannot unless deployment evidence proves otherwise.
4. Choose direct rename only for fully internal, atomically deployable names.
5. For contracts, use an approved compatibility mechanism: additive alias, deprecation, expand-and-contract migration, dual read/write, versioned schema, or resource replacement plan.
6. Define removal criteria and an owner for every temporary alias. Do not leave compatibility names indefinitely.
7. Update code, tests, schemas, documentation, generated clients, migrations, dashboards, alerts, configuration, and consumers in the required order. Coordinate platform-owned changes instead of editing them implicitly.
8. Verify old, mixed-version, rollback, and new-only states when rolling deployment is possible.

Avoid mixing an otherwise mechanical rename with unrelated behavior changes. If separation is impractical, make the behavioral delta explicit and test it independently.

Do not rename:

- a public field, endpoint, error code, event type, logical destination, configuration key, or metric merely for aesthetic consistency;
- a table, column, constraint, or index outside a migration;
- a physical infrastructure resource under this skill alone; use the approved platform standard and replacement plan;
- a security-sensitive identifier without applying `application-security`.

## Review naming changes

During code review:

- report a naming issue as a defect only when it violates an applicable must-rule, misrepresents behavior, creates ambiguity with concrete risk, breaks compatibility, leaks protected information, or prevents reliable operation;
- treat a clearer but equally accurate alternative as a suggestion, not a blocking finding;
- inspect all affected representations rather than reviewing the Java rename in isolation;
- reject broad naming churn that expands risk without a defined benefit and migration;
- identify pre-existing inconsistency separately from risk introduced by the change;
- use `spring-boot-code-review` for evidence, severity, and final reporting.

## Complete the naming task

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
