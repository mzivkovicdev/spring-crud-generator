# Messaging, observability, and cloud names

Use this reference for messages, events, queues, topics, subscriptions, jobs, Redis and cache keys, metrics, tags, spans, structured logs, AWS, and other provisioned resources. Apply `application-security` to every external or operational name.

## Contents

1. [Separate logical and physical names](#separate-logical-and-physical-names)
2. [Name events and messages](#name-events-and-messages)
3. [Name topics, queues, and subscriptions](#name-topics-queues-and-subscriptions)
4. [Name scheduled jobs and workers](#name-scheduled-jobs-and-workers)
5. [Name caches and Redis keys](#name-caches-and-redis-keys)
6. [Name metrics and tags](#name-metrics-and-tags)
7. [Name traces and structured logs](#name-traces-and-structured-logs)
8. [Name delivery and deployment resources](#name-delivery-and-deployment-resources)
9. [Name cloud and infrastructure resources](#name-cloud-and-infrastructure-resources)
10. [Name IAM roles and policies](#name-iam-roles-and-policies)
11. [Migrate operational and provisioned names](#migrate-operational-and-provisioned-names)
12. [Review examples](#review-examples)

## Separate logical and physical names

Define one stable logical vocabulary, then render it into a provider-valid physical name. Do not assume that Kafka, RabbitMQ, SQS, SNS, EventBridge, Redis, Prometheus, Kubernetes, DNS, object storage, IAM, and filesystems accept the same characters, length, case, uniqueness, or replacement behavior.

For every provisioned or published name, identify:

- business capability and resource purpose;
- producer, consumer, owner, and lifecycle;
- environment, region, account, cluster, or tenant scope when required;
- public, persisted, monitored, or provider-generated consumers;
- provider character, delimiter, case, length, uniqueness, reserved-prefix, and replacement rules;
- confidentiality classification.

Do not embed a confidential project codename, customer name, email, token, vulnerability, or business payload in an operational or cloud name. A name often appears in logs, billing, consoles, support exports, DNS, and audit trails.

## Name events and messages

Name a fact in past tense because it describes something that happened:

```text
OrderCreated
PaymentAuthorized
ReservationExpired
```

When the Java project uses explicit suffixes, keep them consistent:

```text
OrderCreatedEvent
OrderCreatedEventConsumer
OrderEventPublisher
```

Name an instruction with an imperative domain action only when the messaging contract is genuinely imperative:

```text
CreateShipment
CancelOrder
```

Do not introduce command objects, a command layer, or CQRS merely to follow this naming example. Use `Command` only when the architecture already models that semantic distinction.

Name request, response, notification, snapshot, and integration-event contracts according to their real delivery semantics. Do not call every message an event.

Avoid:

```text
OrderEvent
EventData
MessagePayload
ProcessOrder
OrderUpdated
```

Use a broad name such as `OrderUpdated` only when the contract deliberately represents a generic update and consumers do not need a more specific business fact. Prefer `OrderShippingAddressChanged` or `OrderCancelled` when that is the actual event.

For event type identifiers, use the project's schema-registry or event-envelope convention. Keep type and version stable:

```text
com.acme.order.order-created.v1
```

Do not encode schema version only in a Java package or class name if consumers resolve it from the wire type. Do not increment a version for every additive compatible field; follow the event compatibility policy.

Name message keys from the ordering, partitioning, or idempotency identity they actually represent:

```text
orderId
customerId
```

Do not use personal data or a mutable field as a message key merely because it is convenient.

## Name topics, queues, and subscriptions

Use a documented token order. A portable logical default is:

```text
<domain>-<message-family>-<version>
```

Examples:

```text
orders-events-v1
payments-events-v1
catalog-product-updates-v1
```

Adapt the delimiter and tokens to the broker's actual constraints and existing convention.

Name consumer-owned queues or subscriptions from the consumer capability and consumed contract:

```text
billing-order-created-v1
search-order-events-v1
```

Name dead-letter resources from their source:

```text
billing-order-created-v1-dlq
```

Rules:

- Keep environment or region in the physical name only when resources share a namespace or the platform standard requires it.
- Do not put producer implementation classes or team names in durable topic names.
- Distinguish topic, queue, subscription, and consumer group according to their actual semantics.
- Keep retry, delay, and dead-letter names tied to one source and policy.
- Make names searchable from producer configuration, consumer configuration, infrastructure code, dashboards, and runbooks.
- Keep tenant data out of resource names unless the architecture provisions isolated per-tenant resources and `application-security` approves the identifier.

## Name scheduled jobs and workers

Name the completed domain work plus its execution role:

```text
ExpiredReservationCleanupJob
InvoiceGenerationJob
CatalogSynchronizationJob
```

Name methods with the domain action:

```text
cleanUpExpiredReservations
generateInvoices
synchronizeCatalog
```

Use one stable logical job identifier across scheduler configuration, distributed locks, metrics, traces, alerts, and runbooks:

```text
expired-reservation-cleanup
```

Name worker pools and executors by workload, not by framework or number:

```text
catalogSynchronizationExecutor
invoiceGenerationExecutor
```

Avoid:

```text
ScheduledTask1
BackgroundProcessor
AsyncExecutor2
JobRunner
```

Do not call an operation `cleanup` when it archives, anonymizes, or permanently deletes data; name the actual lifecycle effect.

## Name caches and Redis keys

Separate the cache name from the entry key.

Name a cache from the stored lookup or result:

```text
users-by-id
catalog-products-by-sku
permissions-by-role
```

Name Redis keys with a centrally defined, versioned namespace and stable token order. Example logical form:

```text
<application>:<cache>:v<schema>:<scope>:<identifier>
```

Examples:

```text
orders:users-by-id:v1:user:42
catalog:products-by-sku:v2:product:ABC-123
```

Do not concatenate keys ad hoc across services. Use one key builder or serializer owned by the cache design.

Rules:

- Include schema version when serialized representation can change incompatibly.
- Include tenant scope when required for isolation; use an approved opaque tenant identifier.
- Do not embed email addresses, access tokens, secrets, raw personal data, or unbounded payload fragments.
- Use delimiters that cannot make two token sequences ambiguous, or encode tokens safely.
- Keep key length bounded.
- Name lock, idempotency, rate-limit, and cache keys differently so their semantics and TTL policies cannot be confused.
- Treat cache-name or key-format changes as compatibility migrations when old entries can coexist.

Apply the cache behavior and security policies from the owning skills; naming alone does not make caching correct.

## Name metrics and tags

Use Micrometer's lowercase dot notation for source meter names and let registry naming conventions adapt them:

```text
orders.created
orders.processing.duration
catalog.client.requests
jobs.expired.reservation.cleanup.duration
```

Name a metric from the measured phenomenon, not from a dashboard title or Java method. Keep it stable across refactors.

Use base units and the meter type rather than inventing inconsistent suffixes. Verify the target registry's convention before adding `_total`, `_seconds`, or another exporter-specific suffix at the Micrometer source.

Use low-cardinality tag keys:

```text
outcome
operation
provider
region
status
```

Use stable bounded tag values:

```text
success
failure
timeout
catalog
```

Never use these as metric tags:

- user, order, request, trace, session, token, or raw tenant identifiers;
- email addresses, URLs containing identifiers, exception messages, SQL, or payload fragments;
- arbitrary HTTP paths instead of route templates;
- uncontrolled provider messages or user input.

Distinguish measurement dimensions from separate metrics deliberately. Do not encode dynamic values into the meter name to bypass tag rules.

## Name traces and structured logs

Use low-cardinality span names that describe the operation:

```text
order.create
catalog.product.lookup
reservation.expire
```

Do not put IDs, query strings, payloads, or error messages in span names. Attach approved attributes according to OpenTelemetry and project conventions.

Use stable `lowerCamelCase` structured-log field names when the logging schema has no different standard:

```text
traceId
spanId
tenantId
userId
orderId
operation
outcome
errorCode
```

Apply `application-security` before recording any identifier. The availability of a field name does not authorize logging its value.

Name machine-searchable audit and application event codes with one documented convention:

```text
USER_CREATED
ORDER_CANCELLATION_REJECTED
CATALOG_REQUEST_TIMED_OUT
```

Keep event codes stable and separate from human-readable messages. Do not encode severity in the code when logging configuration already owns severity.

Use the declaring class as the logger category unless the project deliberately defines functional categories. Do not create arbitrary logger names that drift from code ownership.

## Name delivery and deployment resources

Name build and delivery automation by the verified outcome or environment:

```text
compile
unit-tests
database-integration-tests
api-contract-tests
security-scan
build-image
deploy-staging
production-smoke-tests
```

Avoid `build-2`, `test-everything`, `misc-checks`, `new-pipeline`, and names tied to one person. Keep CI job identifiers stable when branch protection, status checks, dashboards, or release automation consume them.

For non-Java scripts and project folders, follow the tool's required filename first. When no stronger convention exists, use lowercase kebab-case and name the operation:

```text
run-database-integration-tests.sh
generate-openapi-client.sh
local-development/
deployment/
```

Do not rename framework-reserved files or directories, Java source files, migration files, or operating-system entrypoints to satisfy this default.

Name container image repositories by stable application or capability:

```text
orders-api
catalog-worker
```

Keep release version, digest, environment, and build metadata in the image reference or provenance rather than the repository name. Do not use a mutable label such as `latest` as the only release identity.

Name Kubernetes, Helm, Terraform, CloudFormation, and similar logical resources from application plus component:

```text
orders-api
orders-worker
catalog-sync
```

Let resource kind, namespace, labels, and infrastructure module provide context; avoid redundant forms such as `orders-api-deployment-deployment`. Verify DNS-label, template-engine, state-address, and provider constraints before applying a generic format.

Use sanitized branch or change identifiers only for explicitly ephemeral preview resources. Add a bounded collision-resistant suffix when required, and remove the preview resource with its lifecycle. Never copy arbitrary branch text into DNS, IAM, logs, or cloud names without validation.

## Name cloud and infrastructure resources

Use an organization-approved token order. When no standard exists, start with a logical form and adapt it per provider:

```text
<organization>-<application>-<capability>-<resource>-<environment>
```

Examples of logical names:

```text
acme-orders-events-production
acme-orders-attachments-production
acme-catalog-sync-production
```

Do not copy these strings blindly. Verify global uniqueness, DNS rules, region/account scope, character limits, reserved suffixes, and replacement behavior for the actual service.

Include only tokens that disambiguate the resource in its namespace:

- omit organization when the account or project already provides an isolated namespace;
- include environment when environments share a namespace;
- include region only for a global inventory or when the platform standard requires it;
- include a random suffix only for a provider uniqueness requirement, and keep the stable logical identity in tags and infrastructure code;
- do not include a team name when ownership can change;
- do not include mutable implementation versions unless resources intentionally coexist by version.

Keep infrastructure-as-code logical identifiers stable even when a display name changes. Before changing a physical name, inspect the plan for replacement, data movement, policy reattachment, DNS or endpoint changes, downtime, and rollback.

Use consistent mandatory tags or labels from the organization standard. Common semantic keys include:

```text
application
component
environment
owner
cost-center
data-classification
managed-by
```

Do not invent mandatory tags in application code. Apply the platform policy and ensure tag values do not disclose confidential data.

## Name IAM roles and policies

Name identities from the workload and granted purpose:

```text
orders-api
orders-api-read-catalog
catalog-sync-publish-events
```

Avoid:

```text
admin-role
developer-access
john-policy
application-policy-2
full-access-temp
```

Make read, write, publish, consume, decrypt, and administrative purposes distinguishable. Do not claim least privilege through a narrow name while attaching broad permissions; `application-security` owns the actual policy.

Name service accounts, roles, policies, KMS aliases, and secret references consistently enough to trace:

```text
workload -> role -> policy -> resource -> audit event
```

Do not include secret values or protected customer identifiers in secret names, aliases, role session names, or resource paths.

## Migrate operational and provisioned names

Plan by resource type:

| Name type | Migration concerns |
|---|---|
| Event type or schema | Consumer compatibility, registry aliases, producer order, replay, and rollback |
| Topic or queue | Dual publish/consume, backlog drain, ordering, permissions, DLQ, dashboards, and cost |
| Job or lock | Duplicate execution, old schedules, lock coexistence, alerts, and runbook references |
| Cache or Redis key | Dual read/write or cold cutover, TTL, serializer compatibility, invalidation, and memory |
| Metric or tag | Dashboard and alert continuity, recording rules, retention overlap, and cardinality |
| Span or log field | Queries, parsers, SIEM rules, alerts, retention, and support tooling |
| Cloud resource | Replacement, data copy, endpoint and policy changes, DNS, downtime, billing, and rollback |
| IAM identity or policy | Trust relationships, references, sessions, audit continuity, and revocation |

Use dual operation only when the owning architecture and security policy approve it. Bound the compatibility window and define removal criteria.

## Review examples

| Weak | Prefer | Reason |
|---|---|---|
| `OrderEvent` | `OrderCreatedEvent` | State the business fact |
| `MessageProcessor` | `OrderCreatedEventConsumer` | State input and role |
| `events-2` | `orders-events-v1` | State domain, family, and contract version |
| `worker-1` | `expired-reservation-cleanup` | State operational work |
| `cache1` | `users-by-id` | State cached lookup |
| `user:{email}` | Opaque-ID key built by the approved key codec | Avoid personal data and ambiguity |
| `request.duration.{userId}` | `catalog.client.requests` with bounded tags | Avoid dynamic metric names |
| `http.request` span with an ID | Route or operation-based span name | Preserve low cardinality |
| `admin-role` | `orders-api-read-catalog` | State workload and purpose |
| `customer-a-orders-bucket` | Provider-valid opaque resource name plus controlled tags | Avoid customer disclosure |

## Primary guidance

- [CloudEvents Specification](https://github.com/cloudevents/spec/blob/main/cloudevents/spec.md)
- [Micrometer: Naming Meters](https://docs.micrometer.io/micrometer/reference/concepts/naming.html)
- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [AWS Tagging Best Practices](https://docs.aws.amazon.com/tag-editor/latest/userguide/tagging.html)
