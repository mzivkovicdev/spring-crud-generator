# API Security and Abuse Prevention

## Contents

1. [API security profile](#api-security-profile)
2. [Inventory and lifecycle](#inventory-and-lifecycle)
3. [Authorization at every API dimension](#authorization-at-every-api-dimension)
4. [Resource and financial limits](#resource-and-financial-limits)
5. [Sensitive business flows](#sensitive-business-flows)
6. [Idempotency and replay](#idempotency-and-replay)
7. [HTTP contracts, caching, and host handling](#http-contracts-caching-and-host-handling)
8. [Callbacks and webhooks](#callbacks-and-webhooks)
9. [External and internal API consumption](#external-and-internal-api-consumption)
10. [Verification checklist](#verification-checklist)

## API security profile

Use the project's approved security profile. If none exists, recommend `docs/security/security-profile.md` with:

- the pinned OWASP ASVS version and applicable requirement set;
- versioned requirement identifiers such as `v5.0.0-1.2.5`;
- application and API risk classification;
- authentication and credential models;
- actor, role, tenant, ownership, and administrative boundaries;
- data classification and applicable project-specific obligations;
- verification evidence for each applicable control;
- approved exceptions with rationale, owner, expiry, compensating controls, and residual risk.

Propose baseline migration as a separate reviewed change, because identifiers, applicability, implementation, and evidence all change with it.

OWASP Top 10 documents are awareness resources, not substitutes for a security profile or control verification.

## Inventory and lifecycle

`rest-api-contract` owns the compatibility procedure: whether a change is breaking, how a version is
raised, how deprecation and sunset are declared, and how consumers are notified. This section owns
what exists, who owns it, where it is reachable, and whether obsolete surface is actually gone.
Record the dates here; do not restate or fork the procedure that produces them.

Maintain an authoritative inventory of:

- public, partner, internal, management, callback, webhook, and machine-to-machine APIs;
- host, environment, base path, protocol, owner, audience, data classification, and authentication method;
- deployed and supported versions;
- contract location, or a recorded statement that the service publishes no contract document;
- internet, partner-network, private-network, and management-network exposure;
- upstream and downstream dependencies;
- deprecation date, sunset date, and removal status.

- Register every new endpoint and version through the project's API governance process.
- Treat internal and non-production APIs as protected assets; network location is not authorization.
- Remove or disable obsolete versions, debug routes, temporary bypasses, sample endpoints, and abandoned environments. A version past its sunset date that is still reachable is a security finding, not a documentation lapse.
- Confirm that a route absent from the reviewed contract is also absent from the deployment. `rest-api-contract` owns the drift gate that proves document and implementation agree; this section owns the case the gate cannot see, where a route exists but no contract describes it.
- Restrict interactive API documentation and schema endpoints according to their exposure and information sensitivity.
- Do not expose internal hostnames, security schemes, examples with real data, or administrative operations through public API documentation.
- Inventory management, gateway routes, load balancers, service discovery, DNS, deployment manifests, and application routes must agree.

## Authorization at every API dimension

Evaluate authorization at four dimensions:

| Dimension | Required decision |
| --- | --- |
| Function | May this actor invoke this operation? |
| Object | May this actor access this specific resource? |
| Property | May this actor read or change each requested field? |
| Tenant | Does the authenticated tenant own or share this data under an explicit policy? |

- Derive subject and tenant from verified authentication context.
- Do not trust request-supplied user, tenant, role, permission, ownership, status, price, discount, approval, or administrative fields.
- Scope repository lookups and mutations as [Spring Security authorization](spring-security-authorization.md) requires; authorization rules are not repeated here.
- Use explicit input allowlists. Do not reflectively copy request properties onto domain or entity objects.
- Construct response TOs from an explicit field contract; successful object authorization does not grant every property.
- Apply the same checks to bulk operations, exports, nested resources, file downloads, search, count, existence, history, and metadata endpoints.
- Prevent enumeration through response bodies, timing, status differences, pagination totals, and sequential identifiers where the threat model requires concealment.
- Re-evaluate permission before executing delayed or long-lived operations when identity, ownership, or policy may have changed.

## Resource and financial limits

Define limits from the operation's real cost, not only request frequency:

- request bytes, field lengths, collection elements, nesting depth, multipart count, and file size;
- batch size and operations per request;
- page size, maximum offset, sort fields, filters, joins, and export rows;
- CPU, memory, thread, process, file-descriptor, connection, and queue use;
- database execution time, rows scanned, locks, and concurrent expensive queries;
- response bytes, streaming duration, remote response size, and decompressed size;
- retries, backoff, concurrent jobs, and pending work per actor or tenant;
- emails, SMS messages, identity checks, document conversions, model calls, object-storage transfer, and other billable provider operations;
- daily or monthly provider spending limits and cost alerts.

Rate-limit by the identities that matter: IP only when appropriate, plus authenticated subject, tenant, API key, device, operation, resource, or destination. One global request counter is rarely sufficient.

- Apply stricter controls to authentication, recovery, OTP, invitation, export, search, file processing, bulk, and expensive provider endpoints.
- Count operations inside batch or GraphQL-style requests, not only HTTP requests.
- Define behavior when the distributed limiter or quota store is unavailable. Choose fail-open or fail-closed from business impact and document it.
- Return stable limit responses without exposing internal capacity or enabling account enumeration.
- Monitor sustained near-limit activity, bypass attempts, distributed sources, unusual cost, and queue growth.

## Sensitive business flows

Identify operations whose technically valid automation can harm users or the business, for example:

- account creation, invitation, verification, recovery, or credential testing;
- inventory reservation, ticket purchase, limited-stock ordering, or coupon redemption;
- votes, likes, comments, reviews, messaging, or referral rewards;
- scraping, search, pricing, export, report generation, or document conversion;
- payment initiation, refund, payout, transfer, credit, or approval;
- email, SMS, push notification, biometric, identity, or address-verification requests.

For each flow:

1. define acceptable human and machine usage;
2. identify actor, tenant, resource, device, sequence, velocity, and financial signals;
3. enforce operation-specific rate, quota, concurrency, and state-transition rules;
4. use step-up authentication or explicit approval for high-impact changes where required;
5. detect impossible or non-human sequences without treating one signal as proof;
6. protect machine-to-machine APIs with scoped credentials and contractual quotas;
7. define alerting, containment, manual review, and customer-support procedures.

CAPTCHA or IP blocking alone is not sufficient. Avoid controls that create disproportionate accessibility or privacy harm.

## Idempotency and replay

Use idempotency when duplicate execution can create additional state, cost, messages, or external effects.

- Bind an idempotency key to authenticated subject, operation, a canonical request fingerprint, and the tenant wherever the recorded tenancy model has one.
- Give keys a documented format, maximum length, retention window, and data classification.
- Atomically claim the key and request fingerprint, and persist the final outcome under the same record. Atomicity is a database unique constraint, never an application existence check.
- **A concurrent duplicate reaches a defined outcome, never a second execution and never a generic failure.** Which defined outcome depends on the claim shape `spring-boot-patterns` owns and `docs/project-profile.md` records: under `single-phase` the duplicate waits on the constraint and then replays the recorded outcome or wins the claim, and under `two-phase` it reads the in-progress claim and is told to come back. This skill requires that the behavior is defined, bounded, and tested; that skill decides which of the two the project uses and states what each returns.
- Return the original compatible outcome for the same key and payload.
- Reject reuse of the same key with a different payload or operation, as a condition distinct from a replay.
- Where the recorded shape is `two-phase`, an in-progress claim is bounded by a lease and an expired one is reclaimable. An unbounded in-progress state is a denial-of-service against the caller's own key: one crash makes that key permanently unusable, and a caller that cannot retry a payment is an availability incident, not a hygiene issue.
- Do not use a caller-controlled key as a cache key without canonicalization, hashing where appropriate, length limits, and tenant scoping.
- `spring-boot-patterns` owns which delivery mechanism an external effect uses. This skill's concern is what an attacker can do with it: a replayed or duplicated delivery must not produce a second charge, a second grant, or a second notification, so require provider idempotency keys or compensating behavior for effects a caller can trigger repeatedly.
- Distinguish transport retries from replay attacks. Signatures, timestamps, nonces, sequence numbers, or event IDs may be required at external boundaries.
- Test simultaneous duplicates, retry after timeout, partial failure, expired keys, and mismatched payloads.

## HTTP contracts, caching, and host handling

- Accept only documented HTTP methods, media types, encodings, and schema shapes.
- Return explicit media types. [Spring Security for REST](spring-security-rest.md) owns the response security headers.
- Use `Cache-Control: no-store` for credentials, tokens, recovery data, one-time values, and sensitive responses that must not be retained by browsers or intermediaries.
- For cacheable personalized responses, define private/shared cache behavior, authorization separation, cache keys, `Vary`, invalidation, and data classification explicitly.
- Never place credentials, tokens, secrets, or avoidable personal data in URLs or query strings.
- Reject unsupported content types rather than attempting permissive parser fallback.
- Do not generate password-reset, invitation, callback, or canonical absolute URLs from an untrusted `Host` or forwarding header.
- Configure allowed public origins or base URLs and trusted reverse proxies explicitly.
- Normalize duplicate and conflicting headers through the supported server/proxy stack; do not implement ad hoc request-smuggling parsing in application code.
- Keep security headers aligned with the actual browser and deployment model; do not copy obsolete headers.

## Callbacks and webhooks

- Authenticate webhook configuration, destination changes, key rotation, replay, and manual redelivery as separate privileged operations.
- Verify delivery signatures over the exact raw bytes and protocol fields required by the provider before parsing or mutating state.
- Allowlist signature algorithms and key identifiers; never retrieve a key from an arbitrary request-supplied URL.
- Validate timestamp and allowed clock skew and compare shared-secret digests in constant time.
- Reject stale events and prevent replay with a provider event ID, nonce, or another protocol-defined unique value.
- Make processing idempotent and safe under concurrent duplicate delivery.
- Validate content type, schema, event type, object ownership, tenant, size, and business state after signature verification.
- Return minimal errors that do not reveal signature-validation detail or create an oracle.
- Do not log complete signatures, secrets, or sensitive payloads.
- For application-managed outbound webhook destinations, verify ownership, allowlist allowed schemes and ports, defend against SSRF and DNS rebinding, revalidate redirects, and avoid forwarding unrelated credentials.
- Bound delivery attempts, backoff, concurrency, payload size, retention, and customer-triggered replay.

## External and internal API consumption

Treat partner, internal, and third-party responses as untrusted:

- authenticate the remote endpoint and verify TLS;
- use configured destinations and validate every redirect;
- validate response schema, type, bounds, and business meaning;
- never pass remote strings directly into SQL, logs, headers, templates, paths, expressions, or commands;
- do not forward inbound credentials or sensitive headers to another destination;
- use timeouts, bounded retries, circuit behavior, body limits, and cancellation;
- minimize sent data and confirm the provider is approved for its classification;
- separate provider errors from the public API contract;
- monitor provider behavior, certificate or key rotation, latency, response-size changes, and unexpected redirects.

Follow [untrusted input and dangerous sinks](untrusted-input-and-dangerous-sinks.md) for SSRF and sink-specific controls.

## Verification checklist

- [ ] Every deployed API, host, environment, version, owner, and exposure is inventoried.
- [ ] Deprecated, debug, temporary, and undocumented endpoints are removed or explicitly governed.
- [ ] Function, object, property, and tenant authorization have positive and negative tests.
- [ ] Request, batch, response, concurrency, provider, and financial limits match real operation cost.
- [ ] Sensitive business flows resist technically valid automated abuse.
- [ ] Idempotency is actor-scoped, payload-bound, atomic, expiring, and concurrency-tested, and a concurrent duplicate reaches the defined outcome the recorded claim shape specifies.
- [ ] Sensitive responses and absolute URLs use safe cache, host, and proxy behavior.
- [ ] Callbacks and webhooks verify configuration authority, signatures, time, replay, tenant, schema, size, idempotency, and destinations.
- [ ] Third-party and internal API responses receive the same trust-boundary validation as user input.
- [ ] OpenAPI and deployed behavior agree on paths, methods, schemas, statuses, media types, and security.

## References

- [OWASP API Security Top 10: 2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)
- [OWASP API4: Unrestricted Resource Consumption](https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/)
- [OWASP API6: Unrestricted Access to Sensitive Business Flows](https://owasp.org/API-Security/editions/2023/en/0xa6-unrestricted-access-to-sensitive-business-flows/)
- [OWASP API9: Improper Inventory Management](https://owasp.org/API-Security/editions/2023/en/0xa9-improper-inventory-management/)
- [OWASP API10: Unsafe Consumption of APIs](https://owasp.org/API-Security/editions/2023/en/0xaa-unsafe-consumption-of-apis/)
