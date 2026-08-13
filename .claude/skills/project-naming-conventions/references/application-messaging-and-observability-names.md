# Application messaging and observability names

Use this reference for names created or consumed by Spring Boot application code: event and message types, publisher and consumer classes, logical destination properties, scheduled jobs, executors, cache names and cache keys, Micrometer meters and tags, custom spans, and structured-log fields. Apply `spring-boot-patterns` for component responsibilities and `application-security` for data classification, tenant isolation, telemetry, messaging, and secrets.

## Contents

1. [Keep the application/platform boundary](#keep-the-applicationplatform-boundary)
2. [Name events and messages](#name-events-and-messages)
3. [Name messaging components and destination properties](#name-messaging-components-and-destination-properties)
4. [Name scheduled jobs and executors](#name-scheduled-jobs-and-executors)
5. [Name caches and cache keys](#name-caches-and-cache-keys)
6. [Name metrics and tags](#name-metrics-and-tags)
7. [Name custom spans and structured logs](#name-custom-spans-and-structured-logs)
8. [Migrate application-owned operational names](#migrate-application-owned-operational-names)
9. [Review examples](#review-examples)

## Keep the application/platform boundary

Apply this reference to application-owned names:

- Java event/message records and classes;
- serialized event type or schema identifiers owned by the application contract;
- publisher, consumer, listener, handler, scheduled-job, and executor identifiers in source;
- canonical Spring configuration properties for messaging destinations and consumer groups;
- application-defined cache regions and key formats, for the cache technology recorded in `docs/project-profile.md`;
- source Micrometer meter names and tag keys/values;
- custom business span names and application structured-log fields.

Do not use this reference to define physical platform names:

- AWS SQS/SNS/EventBridge resources, Kafka cluster resources, broker namespaces, physical topics/queues/subscriptions, or dead-letter infrastructure;
- IAM roles/policies, KMS aliases, buckets, secret-store paths, DNS, Kubernetes, Helm, Terraform, CloudFormation, container repositories/tags, CI/CD jobs, or infrastructure tags.

Load the approved platform naming standard when those artifacts are in scope. If it is unavailable, ask for it instead of inventing a cross-provider format. Apply `application-security` even when another team owns the physical name.

Name application configuration by purpose and keep the environment-specific physical identifier in the configured value. Do not hardcode a physical ARN, queue URL, topic, bucket, or environment suffix into Java code.

## Name events and messages

Name an event fact in past tense:

```text
OrderCreatedEvent
PaymentAuthorizedEvent
ReservationExpiredEvent
```

Use the `Event` suffix consistently when the project uses it. Do not alternate between `OrderCreated`, `OrderCreatedEvent`, and `OrderCreatedMessage` for the same contract.

Name an imperative message with a domain action only when the architecture genuinely models an instruction:

```text
CreateShipment
CancelOrder
```

Do not introduce a command layer, command object, or CQRS solely to obtain that name. Name request, response, notification, snapshot, and integration-event messages according to their real semantics; do not call every message an event.

Prefer a specific fact over a generic update:

```text
OrderShippingAddressChangedEvent
OrderCancelledEvent
```

Avoid `OrderEvent`, `OrderUpdatedEvent`, `EventData`, `MessagePayload`, and `ProcessOrder` when the contract can state the actual business occurrence.

For a wire event type, follow the existing schema-registry or envelope convention. When the project uses CloudEvents, use its type convention and approved reverse-DNS namespace, for example:

```text
com.acme.order.created.v1
```

Do not invent a new wire-version scheme. Change an event type or version only according to the project's compatibility policy, and do not version every additive compatible field automatically.

Name the message key from the identity that actually controls partitioning, ordering, correlation, or idempotency:

```text
orderId
customerId
```

Do not use email addresses, tokens, personal data, mutable display values, or arbitrary payload fragments as message keys.

## Name messaging components and destination properties

Name the Java component from the contract it publishes or consumes:

```text
OrderEventPublisher
OrderCreatedEventConsumer
PaymentAuthorizedEventListener
```

Use `Publisher`, `Consumer`, or `Listener` according to the project's actual framework role. Do not use `Processor`, `Handler`, `Manager`, or `Service` when the class has a narrower messaging responsibility.

Name canonical Spring properties by application purpose:

```properties
messaging.order-events.destination=...
messaging.order-events.consumer-group=...
messaging.order-events.enabled=true
```

The configured value may be a topic, queue URL, ARN, subscription, or another provider identifier. The property name should remain stable when the physical resource changes. Use a provider-specific prefix only when provider semantics are intentionally exposed to the application.

If the application team owns a logical shared destination contract, use the organization's messaging convention. A possible established form is:

```text
orders-events-v1
```

Treat this only as an example, not a portable default. Brokers differ in naming, namespaces, and versioning. The platform standard owns the physical rendering and environment/account/region tokens.

Name retry and dead-letter configuration from the source contract, but do not provision or rename the physical resource under this skill alone.

## Name scheduled jobs and executors

Name the completed business or maintenance work plus its role:

```text
ExpiredReservationCleanupJob
InvoiceGenerationJob
CatalogSynchronizationJob
```

Name the method with the action:

```text
cleanUpExpiredReservations
generateInvoices
synchronizeCatalog
```

Do not call work `cleanup` when it archives, anonymizes, or permanently deletes data. Name the actual lifecycle effect.

Use one stable application job identifier across scheduler properties, application locks, metrics, traces, and logs:

```text
expired-reservation-cleanup
```

Name executors and worker pools from their workload:

```text
catalogSynchronizationExecutor
invoiceGenerationExecutor
```

Avoid `ScheduledTask1`, `BackgroundProcessor`, `AsyncExecutor2`, and `JobRunner`.

Do not use this reference to name Kubernetes CronJobs, platform schedulers, CI jobs, or cloud functions. Coordinate those physical names with the platform standard.

## Name caches and cache keys

Caching technology is recorded in `docs/project-profile.md`. Where this section says Redis, read it
as the selected cache or key-value store; Redis is the expected choice if one is adopted, and the
naming rules apply to any store. When no cache has been selected, do not introduce cache names or
key formats.

Name a cache from the lookup or result it stores:

```text
users-by-id
catalog-products-by-sku
permissions-by-role
```

Separate the cache name from the entry key. Define Redis key formats centrally rather than concatenating tokens throughout services.

Use a stable, bounded, and versionable logical form, for example:

```text
<application>:<cache>:v<schema>:<scope>:<identifier>
```

Examples:

```text
orders:users-by-id:v1:user:42
catalog:products-by-sku:v2:product:ABC-123
```

Rules:

- Include a representation version only when an incompatible serialized form can coexist.
- Include an approved opaque tenant scope when isolation requires it.
- Do not include email addresses, credentials, tokens, raw personal data, or unbounded payload fragments.
- Encode or delimit tokens so different inputs cannot create the same key.
- Keep the total key length bounded.
- Distinguish cache, lock, idempotency, and rate-limit key namespaces because their semantics and TTLs differ.
- Treat a cache-name or key-format change as a migration when old entries can coexist.

Apply the cache behavior, serializer, TTL, failure, and security rules from their owner skills. A well-named key does not make an unsafe cache design correct.

## Name metrics and tags

Use Micrometer lowercase dot notation for source meter names and let the registry naming convention translate them:

```text
orders.created
orders.processing.duration
catalog.client.requests
jobs.expired.reservation.cleanup.duration
```

Name the measured phenomenon, not a dashboard, Java method, or exporter. Keep the source name stable across implementation refactors.

Use the meter type and base unit rather than adding exporter-specific suffixes by habit. Check the target registry before adding `_total`, `_seconds`, or similar suffixes to the Micrometer source name.

Use bounded tag keys and values:

```text
operation=create
outcome=success
provider=catalog
status=timeout
```

Never use raw user, order, request, trace, session, token, or tenant identifiers; email addresses; exception messages; SQL; payload fragments; raw URLs; or unbounded external values as metric names or tags. Use HTTP route templates rather than concrete paths.

Do not encode a dynamic value into a meter name to bypass tag-cardinality rules. Apply `application-security` before recording any business or tenant dimension.

## Name custom spans and structured logs

Use standard instrumentation and OpenTelemetry semantic conventions when they exist. Do not replace framework-generated HTTP, database, or messaging span names with a custom project scheme merely for visual consistency.

For a custom business span, use a stable low-cardinality operation name:

```text
order.create
catalog.product.lookup
reservation.expire
```

Do not put IDs, raw URLs, query strings, payloads, or error messages in span names. Put approved contextual values in standard or project-approved attributes.

Use the existing structured-logging schema. When no project convention exists, use stable `lowerCamelCase` application field names:

```text
traceId
spanId
operation
outcome
errorCode
```

Identifiers such as `tenantId`, `userId`, and `orderId` may be useful fields, but `application-security` determines whether their values may be logged. The existence of a field name is not authorization to populate it.

Use stable machine-readable event codes when the application contract requires them:

```text
USER_CREATED
ORDER_CANCELLATION_REJECTED
CATALOG_REQUEST_TIMED_OUT
```

Keep an event code separate from the human-readable message and log severity. Use the declaring class as the logger category unless the project deliberately defines functional categories.

## Migrate application-owned operational names

Treat names used outside one source file as contracts:

| Name | Migration concerns |
| --- | --- |
| Event type or schema ID | Consumer compatibility, producer order, replay, rollback, and registry policy |
| Logical destination property | Configuration rollout, old/new application versions, platform mapping, and rollback |
| Job or application lock | Duplicate execution, old schedules, lock coexistence, metrics, and alerts |
| Cache or Redis key | Dual read/write or cold cutover, TTL, serialization, invalidation, and memory |
| Metric or tag | Dashboards, alerts, recording rules, retention overlap, and cardinality |
| Span or log field | Queries, parsers, alerts, retention, and support tooling |

Coordinate physical resource changes with the platform owner. Use a bounded compatibility window and removal criteria for every alias, dual write, or legacy read path.

## Review examples

| Weak | Prefer | Reason |
| --- | --- | --- |
| `OrderEvent` | `OrderCreatedEvent` | State the business fact |
| `MessageProcessor` | `OrderCreatedEventConsumer` | State contract and role |
| Hardcoded queue ARN in Java | `messaging.order-events.destination` property | Keep physical naming outside source |
| `ScheduledTask1` | `ExpiredReservationCleanupJob` | State the work |
| `cache1` | `users-by-id` | State the cached lookup |
| `user:{email}` | Approved opaque-ID key format | Avoid personal data and ambiguous tokens |
| `request.duration.{userId}` | Stable meter name with bounded tags | Avoid dynamic metrics |
| Custom `GET /users/42` span | Standard HTTP span using the route template | Preserve semantic conventions and cardinality |
| `eventMessage` log field | `errorCode` or another semantic field | State machine-readable meaning |

## Primary guidance

- [CloudEvents Specification](https://github.com/cloudevents/spec/blob/main/cloudevents/spec.md)
- [Micrometer: Naming Meters](https://docs.micrometer.io/micrometer/reference/concepts/naming.html)
- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [OpenTelemetry HTTP Span Names](https://opentelemetry.io/docs/specs/semconv/http/http-spans/)
