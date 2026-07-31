# Cloud, Messaging, and Jobs

## Contents

1. [Shared cloud responsibility](#shared-cloud-responsibility)
2. [Identity, secrets, and network boundaries](#identity-secrets-and-network-boundaries)
3. [Object storage and temporary access](#object-storage-and-temporary-access)
4. [Messages, events, and consumers](#messages-events-and-consumers)
5. [Scheduled jobs and workers](#scheduled-jobs-and-workers)
6. [Serverless and metadata services](#serverless-and-metadata-services)
7. [Audit, resilience, and incident readiness](#audit-resilience-and-incident-readiness)
8. [Verification checklist](#verification-checklist)

## Shared cloud responsibility

Inspect the actual provider, services, regions, accounts, environments, deployment model, infrastructure-as-code source, organization policies, and project security profile before changing cloud code.

- Treat application code, SDK configuration, IAM, resource policies, network policy, encryption, logging, backups, and deployment as one security boundary.
- Prefer organization-enforced guardrails over conventions that each application must remember.
- Separate development, test, staging, and production accounts or equivalent trust boundaries.
- Apply least privilege to human, workload, CI/CD, cross-account, support, and break-glass identities.
- Do not claim a managed service is secure by default; verify its project configuration.
- Do not put account IDs, resource names, internal endpoints, policies, findings, or production data into unapproved external systems.

Use provider documentation for current service behavior. Do not copy a generic cloud policy without verifying its effects.

## Identity, secrets, and network boundaries

- Prefer workload identities and short-lived credentials, such as AWS IAM roles with STS, over static access keys.
- Give every workload a dedicated identity; do not share one broad role across unrelated services or environments.
- Scope actions, resources, conditions, regions, accounts, tags, source networks, and session duration as narrowly as practical.
- Review both identity policies and resource-based policies. An apparently narrow role can still gain access through a permissive resource policy.
- Use an approved secret manager and rotation path. Environment-variable injection can deliver a secret but is not itself a secret-management system.
- Keep secrets out of user data, launch templates, images, layers, source, CI logs, command lines, tags, URLs, and diagnostic dumps.
- Restrict east-west and outbound network access. Application allowlists supplement, not replace, network egress controls.
- Require encrypted transport and verified service identities. Use private endpoints where they materially reduce exposure.
- Separate key administration from data access where the risk requires it.

For AWS, review IAM best practices, permission boundaries or organization policies where used, cross-account trust, `ExternalId` or equivalent confused-deputy controls, and credential rotation.

## Object storage and temporary access

- Block public access by default and verify bucket/container plus object-level policies.
- Separate upload, processing, quarantine, and published locations when untrusted files are accepted.
- Enforce ownership, tenant scope, allowed object-key prefixes, operation, content bounds, encryption, retention, and deletion.
- Do not place secrets or avoidable personal data in bucket names, object keys, metadata, tags, or URLs.
- Use server-generated object keys and treat original filenames as untrusted metadata.
- Encrypt according to classification and verify the KMS/key policy, grants, rotation, deletion protection, and cross-account access.
- Scope presigned URLs to the minimum method, object, content constraints, and lifetime.
- Do not treat an unexpired presigned URL as revocable unless the underlying credential or policy design makes it so.
- Prevent content-type confusion, executable inline rendering, overwrite of another tenant's object, and unauthorized listing.
- Log and alert on policy changes, public exposure, unusual download volume, failed decryption, and cross-account access.

Follow the file rules in [untrusted input and dangerous sinks](untrusted-input-and-dangerous-sinks.md).

## Messages, events, and consumers

Treat every message as untrusted, including messages from an internal broker.

- Authenticate and authorize publishers, consumers, administrators, and redrive operations.
- Give producers and consumers least-privilege access to explicit queues, topics, schemas, and environments.
- Validate envelope, event type, schema version, field bounds, payload size, business invariants, and data classification before use.
- Derive trusted tenant and actor context from authenticated producer or verified message claims; do not trust arbitrary payload fields.
- Use a schema-evolution policy with backward and forward compatibility appropriate to the consumers.
- Minimize payloads and avoid secrets, raw tokens, authentication objects, or unnecessary personal data.
- Encrypt transport and storage according to classification; review key access separately from broker access.
- Assume duplicate, delayed, reordered, and replayed delivery unless the platform contract and configuration prove otherwise.
- Make consumer side effects idempotent and atomic at the correct boundary.
- Define ordering, deduplication, visibility or acknowledgement timeout, retry count, backoff, poison-message handling, and concurrency limits.
- Bound message age and reject stale events when their operation is no longer valid.
- Protect dead-letter queues with the same classification, access, retention, and audit controls as the source.
- Do not automatically redrive an entire dead-letter queue into production without validating the cause, compatibility, rate, and side effects.
- Follow [API security and abuse prevention](api-security-and-abuse-prevention.md#callbacks-and-webhooks) for HTTP callbacks and webhooks.

Do not deserialize arbitrary classes or enable permissive polymorphic typing for event payloads.

## Scheduled jobs and workers

- Give each job or worker an explicit owner, workload identity, tenant scope, permissions, schedule, timeout, concurrency policy, retry policy, and alert.
- Do not assume a scheduled or internal invocation is trusted merely because it bypasses the public controller.
- Re-check authorization or approved service authority before executing delayed user-requested operations.
- Prevent overlapping executions when they can duplicate effects; use a distributed lock, uniqueness constraint, claim/checkpoint protocol, or idempotent design appropriate to the operation.
- Store checkpoints atomically and make restart behavior explicit.
- Bound input range, batch size, page size, execution time, memory, remote calls, and cost.
- Separate transient failure, permanent invalid data, policy denial, and poison work.
- Avoid infinite retries and retry storms. Add jitter and a terminal state or dead-letter path.
- Ensure cancellation, deployment shutdown, and timeout do not leave state falsely marked successful.
- Do not log complete job payloads or sensitive records. Use correlation, job, tenant, and checkpoint identifiers only when justified.
- Protect manual job triggers, redrive, backfill, replay, and repair tools as administrative operations with audit evidence.

## Serverless and metadata services

- Use a dedicated least-privilege execution role for each function or cohesive function group.
- Validate event sources, resource policies, and cross-account invocation.
- Bound reserved concurrency, recursion, retries, event age, payload size, temporary storage, and provider cost.
- Keep secrets out of deployment packages, layers, environment snapshots, and logs.
- Patch or replace vulnerable runtimes, layers, and dependencies within the project remediation policy.
- Protect instance or task metadata services from SSRF through current provider controls and network design.
- Do not expose metadata credentials to application users, untrusted containers, file processors, or outbound proxies.
- Restrict function URLs and administrative invocation; public invocation requires the same authentication and authorization rigor as any REST endpoint.

## Audit, resilience, and incident readiness

- Enable provider audit trails for identity, policy, key, network, secret, object-storage, queue/topic, and administrative changes.
- Protect audit destinations from alteration by the workloads being audited.
- Alert on public exposure, privilege expansion, disabled logging, key-policy changes, secret access anomalies, cross-account changes, repeated message failures, DLQ growth, and unusual spend.
- Define backup, restore, region/account recovery, key recovery, credential revocation, queue drain, replay, and rollback procedures.
- Test restoration and failover; the existence of a backup is not evidence that recovery works.
- Preserve idempotency and tenant isolation during replay, redrive, migration, restore, and disaster recovery.
- Document which cloud controls cannot be verified from application code and require deployment evidence.

## Verification checklist

- [ ] Workloads use dedicated short-lived identities and least-privilege policies.
- [ ] Identity and resource policies, cross-account trust, keys, secrets, and network paths are reviewed together.
- [ ] Object storage is non-public by default and presigned access is tightly scoped.
- [ ] Message publishers and consumers validate identity, schema, size, tenant, replay, duplicate, ordering, and retry behavior.
- [ ] DLQ and redrive preserve security, idempotency, rate, retention, and audit requirements.
- [ ] Jobs and workers have bounded authority, concurrency, retries, cost, checkpoints, and safe restart behavior.
- [ ] Serverless functions resist recursion, metadata theft, public invocation, and dependency drift.
- [ ] Audit, alerting, restore, failover, replay, and credential-revocation procedures are verified.

## References

- [AWS Well-Architected Security Pillar](https://docs.aws.amazon.com/wellarchitected/latest/security-pillar/welcome.html)
- [AWS IAM security best practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [Amazon S3 Block Public Access](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html)
- [Amazon SQS dead-letter queues](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html)
- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
