# Outbound call rules

Use this reference whenever a change adds or modifies a call to another system: an HTTP client, a
message producer, or a provider SDK. Apply every rule from `../SKILL.md`.

An adapter that calls another system is where a remote failure becomes an application failure. Every
rule here exists because the default behavior of some widely used client is unsafe, not because the
rule is stylistically preferred.

## Contents

1. [Timeouts and the caller's budget](#timeouts-and-the-callers-budget)
2. [Retry](#retry)
3. [Transactions and outbound calls](#transactions-and-outbound-calls)
4. [Translating failure at the boundary](#translating-failure-at-the-boundary)
5. [Resilience patterns](#resilience-patterns)

## Timeouts and the caller's budget

An adapter that calls another system is where a remote failure becomes an application failure. These
rules apply to every HTTP client, message producer, and provider SDK the application uses.

- Configure connection and read timeouts explicitly on every client. Several widely used clients default to no read timeout at all, so an unconfigured client turns one unresponsive dependency into exhausted threads and a dead application. There is no acceptable outbound call without a bounded wait.
- Keep the timeouts inside the caller's budget. The sum of an operation's outbound waits, plus its own work, must stay below the request timeout the deployment enforces; otherwise the client gives up on a request the application still believes is running.

## Retry

- Retry only an operation that is safe to repeat. A read may be retried; a write may not, unless the provider accepts an idempotency key or the operation is naturally idempotent. Bound the attempts, apply backoff with jitter, and record the policy where the client is configured.
- Choose the layer that owns the retry policy and disable retry in every other one, including client-library defaults that are on unless switched off.

## Transactions and outbound calls

- Never hold a database transaction open across an outbound call unless the consistency design requires it, and never retry inside one: the transaction stays open for the whole backoff.

## Translating failure at the boundary

- Translate failure at the adapter boundary. A timeout, a connection reset, a 4xx, a 5xx, and a malformed body each become a project exception the caller can act on. Never let a client library's exception type, status object, or SDK response reach a service or a controller.

## Resilience patterns

- Add circuit breaking, bulkheads, or rate limiting only when `docs/project-profile.md` records a resilience library. Do not hand-roll a breaker.

`observability-and-logging` owns what an outbound call must emit. `application-security` owns
credentials, destination validation, and response-size limits.
