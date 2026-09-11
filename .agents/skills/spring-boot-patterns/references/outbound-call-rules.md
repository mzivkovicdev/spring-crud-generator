# Outbound call rules

Use this reference whenever a change adds or modifies a call to another system: an HTTP client, a
message producer, or a provider SDK. Apply every rule from `../SKILL.md`.

**This file carries rules, not only examples.** The three waits an outbound call has, where each one
is configured, the connection-pool bounds and how they are sized, retry placement, and the rejected
shapes are stated here in full and nowhere else.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

An adapter that calls another system is where a remote failure becomes an application failure. Every
rule here exists because the default behavior of some widely used client is unsafe, not because the
rule is stylistically preferred. **Assume every default is wrong until the change sets it**, because
the two that matter most — the read timeout and the wait for a pooled connection — are unset and
three minutes respectively on the client Spring Boot picks first.

## Contents

1. [Three waits, not two](#three-waits-not-two)
2. [Where the settings are applied](#where-the-settings-are-applied)
3. [The connection pool is a bound like every other](#the-connection-pool-is-a-bound-like-every-other)
4. [Sizing the pool](#sizing-the-pool)
5. [Retry](#retry)
6. [Transactions and outbound calls](#transactions-and-outbound-calls)
7. [Translating failure at the boundary](#translating-failure-at-the-boundary)
8. [Resilience patterns](#resilience-patterns)
9. [Proving it](#proving-it)
10. [Rejected shapes](#rejected-shapes)

## Three waits, not two

Every pooled outbound call can block in three places, and a project that bounds two of them has an
unbounded call. `docs/project-profile.md` records all three under **Performance and capacity**.

| Wait | What it bounds | Profile row |
| --- | --- | --- |
| **Acquisition** | Waiting for a free connection from the client's pool, before anything is sent | `Outbound connection acquisition wait` |
| **Connect** | Opening the socket and completing the TLS handshake, when the pool had none to reuse | `Outbound timeouts` |
| **Read** | Waiting for the response after the request was written — see below, because what it bounds differs by client | `Outbound timeouts` |

**Acquisition is the one that gets missed, and it is the same failure `spring-data-jpa` describes for
the database pool one layer down.** A long acquisition wait does not prevent pool exhaustion; it hides
it, by turning a fast failure into a stalled request that still holds a server thread. Keep it short
on purpose, so a saturated pool shows up as a bounded failure the caller and the metrics both see.

**Count acquisition beside connect in the arithmetic**, not instead of it. In steady state one of the
two is effectively zero — either the pool had a connection or it opened one — but the worst case is a
caller that queues for the full acquisition wait and *then* opens a new connection. The profile
template's worked arithmetic adds them that way.

**A read timeout does not mean the same thing on every client, so confirm what it bounds on the
configured factory.** On a socket-based factory it bounds the wait for the *next* bytes, so a response
delivered in a slow trickle can outlive it many times over. On the JDK client Spring Boot maps the same
setting to the request's own deadline, which bounds the whole exchange. One number, two different
guarantees, and only one of them is a deadline.

Where the design needs a deadline the factory does not give it, that is a time limiter from the
resilience library — available only where the profile records one, and hand-rolling it is the
prohibition below. Where it records `none`, say so in the change rather than assuming the read timeout
covers it.

## Where the settings are applied

**Configure the auto-configured builder, never a client constructed by hand.** Spring Boot applies the
recorded timeouts to the `RestClient.Builder` and `RestTemplateBuilder` it exposes; a client created
with `new RestTemplate()` or `RestClient.create()` bypasses that entirely and carries the underlying
library's defaults. That is the most common way a project with correct configuration still ships an
unbounded call — the properties are right, and this one client never reads them.

Three rules follow, and each is about a boundary rather than a value:

- **One prefix carries the settings for every imperative client, and a second for the reactive one.** `build-and-dependencies` owns the property names per generation in [generation differences](../../build-and-dependencies/references/generation-differences.md); read the row for the generation the profile records rather than copying a prefix from an example, because it changed between them.
- **Below Spring Boot 3.4 no such property exists.** On those lines the project builds the request factory itself and sets the timeouts on it, in one `@Configuration` class, and the profile row still records the numbers. Verify which line the build resolves before assuming a property will bind — a property that binds to nothing reports nothing.
- **Per-destination settings are a deliberate exception.** Where one dependency genuinely needs a different budget, give it its own builder or its own settings group and record why, rather than raising the project-wide value until it fits. That is the same rule `spring-data-jpa` applies to a report query that needs longer than the statement timeout.

Spring Boot selects the request factory from what is on the classpath, in a fixed order, so **the
client a project gets is decided by its dependencies unless it says otherwise**. Record the choice in
`Outbound HTTP client` and set the factory explicitly; a transitive dependency that adds a client
library then cannot silently change which one is in use, along with every default in the table below.

## The connection pool is a bound like every other

The pool belongs to the client library, not to Spring, so the defaults are the library's and none of
them was chosen for a blocking REST service. Confirm each row against the version the build resolves
before relying on it; these are written-down values like any coordinate.

| Client | Pool bounds | Acquisition wait, unset | Read timeout, unset | Idle eviction, unset |
| --- | --- | --- | --- | --- |
| Apache HttpClient 5 | `setMaxTotal` and `setDefaultMaxPerRoute` on the pooling connection manager | `connectionRequestTimeout`, **3 minutes** | `responseTimeout` is **null** — no read timeout at all | Configure `evictIdleConnections` and a validate-after-inactivity window |
| Reactor Netty | `maxConnections` on the `ConnectionProvider`, default `max(2 × cores, 16)`; `pendingAcquireMaxCount` defaults to twice that | `pendingAcquireTimeout`, **45 seconds** | none | `maxIdleTime` and `maxLifeTime` are **unset**, and background eviction is off |
| JDK `HttpClient` | No per-destination limit, and the pool is tuned only by JVM system properties | none | Carried per request | Keep-alive by system property |

Three consequences are rules rather than notes:

- **`defaultMaxPerRoute` is the bound that bites first.** It limits connections to *one destination*, and the shipped default is a small single-digit number. A service on virtual threads with two hundred requests in flight, all calling one dependency, gets that many connections and queues the rest — for three minutes each, on the default acquisition wait. The symptom is latency with an idle CPU and a healthy dependency, which is why it is diagnosed late.
- **An unset read timeout is not a long read timeout.** On Apache HttpClient 5 the response timeout is null by default, so an unresponsive dependency holds the connection and its caller until something else gives up. There is nothing else.
- **A pool with no idle eviction eventually hands out a dead connection.** The far side — the server, a proxy, a load balancer, a NAT table — closes an idle connection without telling the client, and the failure arrives as an unexplained reset on an unrelated request. This is the same reasoning `spring-data-jpa` applies to the database pool's maximum lifetime.

## Sizing the pool

The pool is sized from the decisions already recorded, not from a guess, and it is re-derived whenever
any of them changes.

- **Per destination**, size it from the number of requests that can be inside the application at once and the share of them that call that destination. On platform threads the server pool caps that number; on virtual threads nothing does, so the outbound pool becomes one of the bounds that replaces it — `spring-boot-patterns` states that in [runtime and request budget](runtime-and-request-budget.md#what-moves-when-the-model-changes).
- **In total**, at least the sum over destinations, so one busy dependency cannot starve the others out of the shared pool.
- **Against the other side.** Pool size × the recorded instance count is what the callee sees. That number has to fit whatever it accepts — a rate limit, a connection limit, a licence — and the callee's limit wins, because exceeding it converts a local sizing decision into someone else's incident.
- **Bigger is not faster.** Past the point the dependency can serve concurrently, a larger pool moves queuing from the application, where the acquisition wait bounds it and a metric shows it, into the dependency, where neither is true.

## Retry

- Retry only an operation that is safe to repeat. A read may be retried; a write may not, unless the provider accepts an idempotency key or the operation is naturally idempotent. Bound the attempts, apply backoff with jitter, and record the policy where the client is configured.
- Choose the layer that owns the retry policy and disable retry in every other one, **including client-library defaults that are on unless switched off**. A client library, an adapter, a gateway, and a scheduler each retrying three times is twenty-seven calls to a system that is already failing.
- **Retry multiplies the budget, so it is part of the arithmetic.** An operation with two retries and a three-second read timeout can consume nine seconds plus backoff. Either the retried worst case fits the request budget, or the retry belongs behind an asynchronous mechanism instead.
- Never retry a timeout that may have been delivered. A read timeout means the response was not received, not that the request was not processed; retrying it without an idempotency key is how one call becomes two effects.

## Transactions and outbound calls

- Never hold a database transaction open across an outbound call unless the consistency design requires it, and never retry inside one: the transaction stays open for the whole backoff, and every wait above is added to a transaction that is holding a database connection.

## Translating failure at the boundary

- Translate failure at the adapter boundary. A timeout, a connection reset, a 4xx, a 5xx, and a malformed body each become a project exception the caller can act on. Never let a client library's exception type, status object, or SDK response reach a service or a controller.
- **Distinguish the three waits in the translation**, because they mean different things to the caller and to the operator: a failed acquisition says this application is saturated, a failed connect says the dependency is unreachable, and a read timeout says it is slow or wedged. Collapsing them into one "dependency unavailable" exception discards the only signal that says which side is at fault.

## Resilience patterns

- Add circuit breaking, bulkheads, or rate limiting only when `docs/project-profile.md` records a resilience library. Do not hand-roll a breaker.
- Where one is recorded, a bulkhead and the connection pool bound the same thing from two sides. Set them from one number rather than two, or the smaller one silently becomes the limit and the larger one reads as the configuration.

## Proving it

Each bound here fails silently when it is wrong, so each is proven by executing it rather than by
reading the configuration. `spring-boot-testing` owns the level; these are the scenarios.

- **Read timeout:** point the adapter at a stub that accepts the request and never answers, and assert the call fails within the recorded timeout rather than hanging. Assert the bound, not merely the failure.
- **Acquisition wait:** hold every pooled connection against a stub, issue one more call, and assert it fails within the recorded acquisition wait.
- **Settings actually reached the client:** assert the configured values on the bean the application injects. This is the test that catches a client constructed by hand, which no other test distinguishes from a configured one.
- **Translation:** assert that each of the three waits produces its own project exception, and that no client-library type escapes the adapter.

## Rejected shapes

```java
// Wrong: bypasses the auto-configured builder, so none of the recorded settings apply. The
// properties in application.yaml are correct and this client reads none of them.
private final RestClient client = RestClient.create("https://payments.example.com");
```

```java
// Wrong: a pool left at the library's defaults. A small per-destination limit and a
// multi-minute acquisition wait turn a busy dependency into queued requests that still hold
// server threads, with no failure anywhere to show it.
@Bean
public CloseableHttpClient paymentsHttpClient() {
    return HttpClients.custom()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();
}
```

```yaml
# Wrong: two of the three waits. The pool acquisition wait is not here, so a saturated pool
# stalls for the library's default rather than failing inside the budget.
connect-timeout: 1s
read-timeout: 3s
```

```java
// Wrong: retry inside the transaction, so the database connection is held for the call, the
// backoff, and the next attempt.
@Transactional
public void settle(final String orderId) {
    this.paymentsAdapter.retryingCharge(orderId);
}
```

```java
// Wrong: one exception for three conditions. The operator cannot tell a saturated local pool
// from an unreachable dependency, which are fixed in different places by different people.
catch (final IOException failure) {
    throw new DependencyUnavailableException(failure);
}
```

`observability-and-logging` owns what an outbound call must emit — a timer, an error counter, and a
client-side span — and requires the three conditions above to be separable there for the same reason.
`application-security` owns credentials, destination validation, and response-size limits.
