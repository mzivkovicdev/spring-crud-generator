# API, data, and configuration names

Use this reference for REST paths and parameters, JSON fields, OpenAPI components, error identifiers, database objects, migrations, Spring configuration, environment variables, profiles, and feature flags. Apply `spring-boot-patterns`, `spring-data-jpa`, and `application-security` for behavior and safety.

## Contents

1. [Preserve contracts before style](#preserve-contracts-before-style)
2. [Name REST resources and paths](#name-rest-resources-and-paths)
3. [Name parameters, fields, and TOs](#name-parameters-fields-and-tos)
4. [Name OpenAPI operations and schemas](#name-openapi-operations-and-schemas)
5. [Name errors and problem types](#name-errors-and-problem-types)
6. [Name database objects](#name-database-objects)
7. [Name schema migrations](#name-schema-migrations)
8. [Name configuration properties](#name-configuration-properties)
9. [Name environment variables and profiles](#name-environment-variables-and-profiles)
10. [Name feature flags](#name-feature-flags)
11. [Migrate escaped names](#migrate-escaped-names)
12. [Review examples](#review-examples)

## Preserve contracts before style

Treat these application-owned names as contracts once consumed outside one atomically deployable unit:

- URL paths, parameter names, headers, media types, JSON fields, enum wire values, and problem type URIs;
- OpenAPI `operationId`, schema, security-scheme, and component names used by generators or policy;
- tables, columns, constraints, indexes, sequences, triggers, views, stored routines, and migration identifiers;
- configuration keys, directly bound environment variables, profile names, and feature flags.

Do not rename them with a source-only refactor. Inventory clients, migrations, data, deployment configuration, generated code, gateways, policies, dashboards, and support tooling first.

Use an explicit API, schema, or configuration convention from the repository when one exists. Preserve a coherent existing external convention even when it differs from this reference's default.

## Name REST resources and paths

Default path style:

- use lowercase ASCII;
- use hyphens between words in literal path segments;
- use nouns for resources;
- use plural collection names consistently;
- use stable domain identifiers rather than database implementation details;
- keep hierarchy shallow and express only a real containment or ownership relationship.

Examples:

```text
GET    /users
GET    /users/{userId}
POST   /users
PUT    /users/{userId}
DELETE /users/{userId}
GET    /users/{userId}/permissions
```

Avoid:

```text
/getUsers
/create-user
/userManagement/getById
/users/{id}/doUpdate
/v1/user_data
```

Do not force every business operation into CRUD when that would obscure the domain. Prefer a meaningful resource or state-transition contract when natural:

```text
POST /orders/{orderId}/cancellation
```

Use an action-oriented endpoint only when the approved API design requires it. Name the actual domain operation rather than `execute`, `process`, or `action`. Do not introduce command objects or CQRS layers merely because an endpoint represents an action.

Place API versions according to the project's versioning strategy. Do not add `/v1` speculatively or rename an existing versioning mechanism. Use lowercase stable path variable names in OpenAPI and corresponding `lowerCamelCase` Java parameters:

```text
{userId}
{orderId}
```

Do not alternate between `{id}`, `{userId}`, and `{userIdentifier}` for the same public contract.

## Name parameters, fields, and TOs

Use `lowerCamelCase` for JSON fields, path variables, and query parameters unless an established external contract requires another form:

```text
userId
pageNumber
pageSize
createdAt
emailVerified
```

Keep the same business term across path, query, JSON, OpenAPI, TO, and service input unless the layers represent different concepts.

Preserve project TO names:

```text
UserCreateTO
UserUpdateTO
UserTO
```

Do not introduce parallel `CreateUserRequest`, `UserResponse`, or `UserDto` types without an explicit project-wide terminology change. A serialized schema name may follow an externally required convention, but map it deliberately rather than renaming internal boundary types casually.

Name pagination and sorting fields consistently with the approved API contract. Do not mix `page`, `pageNumber`, and `pageIndex`, or `size`, `pageSize`, and `limit`, in one API family.

Use positive boolean fields:

```text
active
emailVerified
includeArchived
```

Avoid negative flags such as `disableValidation` or `excludeInactive` when a positive contract is clearer. Do not rename an established public flag without migration.

Define enum wire values explicitly when stability matters. Treat case or spelling changes as contract changes. Do not let Java enum renaming silently change JSON or database values.

Use standard HTTP header names when the standard defines the semantics. Name organization-specific headers from a documented namespace and compatibility policy; do not create `X-*` headers by habit or leak internal topology in names.

## Name OpenAPI operations and schemas

Treat every OpenAPI `operationId` as a public tooling contract. It must be unique, stable, case-consistent, deterministic from the API path, and suitable for use as a generated client or controller method name.

For every new operation, derive `operationId` from the OpenAPI path and HTTP method:

```text
{normalizedPath}{HttpMethod}
```

For standard item-resource paths, this is equivalent to:

```text
{resourcePath}{IdParameter}{HttpMethod}
```

Apply these normalization rules in order:

1. Use the path declared by the OpenAPI Path Item. Do not include the server URL or base URL.
2. Remove `/`, `{`, and `}` delimiters while preserving path-segment order.
3. Convert literal path segments and path-parameter names into camel-case words.
4. Keep the first path segment lower-camel-cased.
5. Capitalize every subsequent path segment and path parameter.
6. Append the HTTP method in `UpperCamelCase`, such as `Get`, `Post`, `Put`, `Patch`, or `Delete`.
7. Do not include query parameters, headers, request bodies, media types, controller names, or API version text unless they are actual path segments.
8. Do not add generic verbs such as `find`, `list`, `create`, `update`, or `delete` independently of the HTTP-method suffix.

For URI major versioning, declare the common `/api/v1` prefix in the OpenAPI `servers.url` and keep Path Items resource-relative, such as `/users/{userId}`. The version prefix therefore does not become part of `operationId`.

Examples:

| HTTP operation | `operationId` |
| --- | --- |
| `GET /users/{userId}` | `usersUserIdGet` |
| `GET /users` | `usersGet` |
| `POST /users` | `usersPost` |
| `PUT /users/{userId}` | `usersUserIdPut` |
| `PATCH /users/{userId}` | `usersUserIdPatch` |
| `DELETE /users/{userId}` | `usersUserIdDelete` |
| `GET /users/{userId}/permissions` | `usersUserIdPermissionsGet` |
| `POST /orders/{orderId}/cancellation` | `ordersOrderIdCancellationPost` |

The controller handler method name must exactly match the corresponding `operationId`:

```java
@Operation(operationId = "usersUserIdGet")
@GetMapping("/{userId}")
public ResponseEntity<UserTO> usersUserIdGet(@PathVariable final Long userId) {
    return ResponseEntity.ok(
            UserRestMapper.INSTANCE.mapUserDomainToUserTO(
                    this.userService.getById(userId)
            )
    );
}
```

Apply the same rule to every controller operation:

```java
usersGet(...)
usersPost(...)
usersUserIdGet(...)
usersUserIdPut(...)
usersUserIdPatch(...)
usersUserIdDelete(...)
usersUserIdPermissionsGet(...)
```

Do not use competing controller method names for these operations:

```text
getUserById
listUsers
createUser
updateUserById
deleteUserById
```

If the project uses contract-first generated interfaces, configure the generator so the generated Java method name matches `operationId`. Do not manually edit generated source.

If two different paths normalize to the same `operationId`, resolve the collision with a stable domain-specific path qualifier. Do not use numeric suffixes such as `usersGet2`.

Name schemas from the established TO or public-contract vocabulary. Keep reusable parameter, response, header, and security-scheme names semantic:

```text
UserTO
PageTO
ValidationProblem
bearerAuth
```

Do not rename an existing `operationId` or component name casually. Generated clients, controller interfaces, gateways, tests, policy engines, documentation tooling, monitoring, and external consumers may depend on these names even when the HTTP path remains unchanged.

Apply this convention automatically to new operations. Treat changes to existing consumed names as contract migrations: inventory consumers, regenerate affected clients, verify compatibility, and coordinate rollout before removing the old name.

## Name errors and problem types

This project has exactly one public, machine-readable error identifier: the RFC 9457 `type` URI in
the `ProblemDetail` body. Clients branch on it. `title` and `detail` are human-readable text and
carry no contract.

```text
https://api.acme.example/problems/resource-not-found
https://api.acme.example/problems/duplicate-email
https://api.acme.example/problems/invalid-order-transition
```

Rules for the type URI:

- Use a stable absolute URI under one project-owned base. Do not build it from the deployment hostname, so the identifier survives environment and infrastructure changes.
- Use lowercase kebab-case in the final segment and name the condition, not the exception class, HTTP status, provider, or layer.
- Make it dereferenceable only when project policy requires published problem documentation. An unresolvable but stable URI is still a valid identifier under RFC 9457.
- Do not include dynamic values such as identifiers, tenant names, field names, or counts.
- Do not add a parallel `code`, `errorCode`, or `errorId` member to the response body. Two identifiers for one condition guarantee that some client branches on the wrong one. `spring-boot-patterns` owns that decision.

Internal error codes use `UPPER_SNAKE_CASE` and appear in structured logs, audit events, metrics,
and application events only:

```text
RESOURCE_NOT_FOUND
DUPLICATE_EMAIL
INVALID_ORDER_TRANSITION
```

Each internal code and its problem type are one and the same condition under two names, so declare
them together in the single error catalog that `spring-boot-patterns` requires, and derive the
type's final segment from the code as its kebab-case form. Deriving rather than typing the URI is
what makes the pairing structural: an operator moves between a log line and the public contract
without a lookup table, and neither identifier can be changed in isolation. Never declare a problem
type URI or an internal error code anywhere else.

Apply `application-security` to error details and identifiers. A stable name must not reveal a
secret, internal host, vulnerable component, or protected customer information.

## Name database objects

Follow `spring-data-jpa` and the migration baseline. Default to lowercase `snake_case` physical identifiers when the database and project permit it.

| Object | Default | Example |
| --- | --- | --- |
| Table | plural domain noun | `users`, `order_items` |
| Column | attribute noun | `email`, `created_at` |
| Primary key column | `id` when unambiguous | `id` |
| Foreign key column | referenced singular concept plus `_id` | `customer_id` |
| Join table | both concepts in stable order | `user_roles` |
| Sequence | `<table>_seq` | `users_seq` |
| Primary key constraint | `pk_<table>` | `pk_users` |
| Unique constraint/index | `uk_<table>_<columns-or-purpose>` | `uk_users_email` |
| Foreign key constraint | `fk_<child>_<parent-or-column>` | `fk_orders_customer` |
| Non-unique index | `ix_<table>_<columns-or-purpose>` | `ix_orders_customer_id_created_at` |
| Check constraint | `ck_<table>_<rule>` | `ck_orders_total_non_negative` |

Use `ix` or the project's established `idx` form consistently; do not alternate. Keep names within the shortest supported target limit and ensure truncation cannot create collisions.

Rules:

- Align entity annotations, migration names, database definitions, and native SQL.
- Use domain terms rather than Java class suffixes: table `users`, not `user_entities`.
- Use explicit stable constraint and index names when the migration convention supports them so failures and plans remain searchable.
- Name composite indexes from their leading columns or business purpose without claiming they enforce a rule they do not enforce.
- Do not call a non-unique index `uk_*` or a normal index `pk_*`.
- Avoid reserved words and quoted mixed-case identifiers unless an existing schema requires them.
- Account for database case folding, identifier length, schemas, catalogs, and vendor-specific object namespaces.
- Apply tenant identifiers only where the relational model requires them; never embed actual tenant or customer names in object names.

Do not create or rename an index, constraint, table, or column merely to satisfy naming style. `spring-data-jpa` must establish the semantic and performance need.

## Name schema migrations

Follow the configured migration tool and repository chronology. Keep migration identifiers immutable after application to a shared environment.

For Flyway versioned SQL migrations, use the configured prefix and separator. A readable default is:

```text
V202607251200__add_order_status.sql
V202607251215__create_order_items_index.sql
```

Use the project's numeric version strategy when it already exists. Do not switch between timestamp and sequence formats midstream.

For repeatable migrations, name the recreated object or purpose precisely:

```text
R__order_summary_view.sql
```

For Liquibase, keep change-set IDs stable and unique in their scope. Prefer a durable ticket or migration purpose plus sequence over a person's name:

```text
order-status-001
```

Describe what the migration changes, not an implementation step such as `fix_db` or `changes_2`. Do not rename an already executed migration; add a corrective migration.

## Name configuration properties

Use Spring Boot canonical lowercase kebab-case. Group properties by a stable subsystem or capability:

```properties
clients.catalog.base-url=https://catalog.example
clients.catalog.connect-timeout=2s
clients.catalog.read-timeout=5s
features.order-cancellation.enabled=false
jobs.expired-reservation-cleanup.batch-size=500
```

Name the corresponding type from its property group:

```java
@ConfigurationProperties("clients.catalog")
public record CatalogClientProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout) {
}
```

Apply `modern-java-21` to the complete source and imports.

Rules:

- Use one canonical key; rely on Spring relaxed binding only for supported input forms, not as permission to mix styles in source.
- Name by behavior or capability, not the current implementation class.
- Include units in the value and type when Spring can bind them; avoid keys such as `timeout-ms` with `Duration`.
- Keep related keys under one stable prefix.
- Use positive booleans ending in `.enabled` only for actual enablement controls.
- Avoid ambiguous keys such as `mode`, `type`, `value`, `url`, or `timeout` without a meaningful parent prefix.
- Do not put secrets or secret values in key names. Follow `application-security` for secret storage and references.
- Do not rename a property without alias, deprecation, deployment, and removal handling appropriate to its consumers.

## Name environment variables and profiles

When Spring Boot binds an environment variable directly to a canonical property, derive it using the documented rules: replace dots with underscores, remove dashes, and convert to uppercase.

```text
clients.catalog.base-url                 -> CLIENTS_CATALOG_BASEURL
clients.catalog.connect-timeout          -> CLIENTS_CATALOG_CONNECTTIMEOUT
features.order-cancellation.enabled      -> FEATURES_ORDERCANCELLATION_ENABLED
```

Verify the exact binding of list indices and other complex keys rather than guessing.

Prefer the canonical Spring property as the application contract. A platform may expose a differently named deployment variable and map it explicitly, for example:

```text
CATALOG_CLIENT_ID
CATALOG_CLIENT_SECRET
```

Treat that alias and any secret-store path as platform-owned naming. Do not claim that a custom alias binds directly through Spring relaxed binding unless the deployment mapping proves it. The name may describe a secret's purpose; it must never contain the secret value. Avoid personal names, ticket numbers, and temporary labels in long-lived configuration.

Use a small approved profile vocabulary:

```text
local
test
staging
production
```

Preserve the organization's environment names when they differ. Do not use profiles as a substitute for typed feature or subsystem configuration, and do not create combinatorial profiles such as `production-aws-eu-new-feature`.

Do not define Kubernetes Secret names, cloud secret-store paths, Helm value names, or CI/CD variable namespaces in this reference. Use the approved platform standard and map them to the application's canonical properties.

## Name feature flags

Name a feature flag from the user-visible or operational capability it controls:

```text
features.order-cancellation.enabled
features.catalog-fallback.enabled
```

Avoid:

```text
new-flow
use-v2
temporary-fix
johns-test
enable-feature
```

Rules:

- Use a positive predicate and make true/false behavior unambiguous.
- Do not include confidential launch names or customer names unless the approved flag system and access policy require controlled targeting metadata.
- Distinguish release, experiment, permission, operational, and kill-switch flags according to the project's flag platform.
- Keep the name stable during rollout; record owner and retirement criteria in the system that owns flag lifecycle.
- Remove code, configuration, tests, and observability for a retired flag in a controlled change.
- Treat provider-side project, environment, segment, and targeting-resource names as platform-owned; this reference owns the application flag key and its semantics.

## Migrate escaped names

Use the appropriate migration:

| Name type | Safe migration direction |
| --- | --- |
| REST path or field | Add the new contract, deprecate the old, support both for the agreed window, migrate consumers, then remove |
| `operationId` or schema | Regenerate and verify consumers; preserve aliases or versions when tooling permits |
| Database object | Use forward migration, compatible application rollout, data backfill where needed, and rollback/roll-forward plan |
| Configuration key | Bind old and new temporarily, define precedence, warn without exposing values, update deployments, then remove |
| Directly bound environment variable or explicit alias | Coordinate every deployment source and platform mapping before removal |
| Feature flag | Migrate targeting and telemetry, then retire old evaluation and cleanup |
| Problem type URI | Version or accept both where clients branch on the identifier; keep the internal error code aligned with it |

Test mixed-version deployment when old and new application versions can coexist.

## Review examples

| Weak | Prefer | Reason |
| --- | --- | --- |
| `/getUsers` | `/users` with `GET` | Use HTTP method plus resource noun |
| `/user_data/{id}` | `/users/{userId}` | Use consistent resource, delimiter, and identifier |
| `UserDto` | `UserTO` | Preserve project terminology |
| `get_user` JSON field | `userId` or the actual field | Use the API's `lowerCamelCase` convention |
| `getUser` for `GET /users/{userId}` | `usersUserIdGet` | Derive the controller and OpenAPI operation name from the Path Item and HTTP method |
| `user_entity` | `users` | Keep persistence name independent of Java suffix |
| `idx1` | `ix_orders_customer_id_created_at` | Make operational purpose searchable |
| `fk_123` | `fk_orders_customer` | Name relationship |
| `catalog.timeout-ms` | `clients.catalog.read-timeout` | State subsystem, phase, and bindable duration |
| `use-new-flow` | `features.order-cancellation.enabled` | Name stable capability, not rollout age |

## Primary guidance

- [Spring Boot: Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [OpenAPI Specification: Operation Object](https://spec.openapis.org/oas/latest.html#operation-object)
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110)
- [Flyway: Migration Naming](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-validate-migration-naming)
