# Runtime and request budget

Use this reference when choosing or changing how the application serves concurrent requests, when
deciding what a request may cost in wall-clock time, and when a read path is worth answering without
sending its representation again. Apply every rule from `../SKILL.md`.

**This file carries rules, not only examples.** The concurrency-model decision, what moves when it
changes, how a request budget is actually held, the conditional-read rules, and the shutdown rules
are stated here in full and nowhere else.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

Everything here is about the half of a request that happens **above** the database.
`spring-data-jpa` owns every bound below it — statement, transaction, connection wait, pool size —
and those numbers are sized against the decisions on this page rather than independently of them.

## Contents

1. [The request budget is a number nothing enforces in-process](#the-request-budget-is-a-number-nothing-enforces-in-process)
2. [Choose the concurrency model](#choose-the-concurrency-model)
3. [What moves when the model changes](#what-moves-when-the-model-changes)
4. [Answering without sending the representation again](#answering-without-sending-the-representation-again)
5. [Compression](#compression)
6. [Shutdown](#shutdown)
7. [Proving it](#proving-it)
8. [Rejected shapes](#rejected-shapes)

## The request budget is a number nothing enforces in-process

`docs/project-profile.md` records a request budget, and every outbound timeout and every database
bound fits inside it. It is worth being exact about what that number is, because the usual
assumption is wrong: **Spring MVC applies no timeout to a synchronous request.** A handler that
blocks forever blocks forever. `spring.mvc.async.request-timeout` bounds only the asynchronous
return types — a `Callable`, a `DeferredResult`, a `CompletableFuture`, a streaming body — and a
project whose handlers return values directly is not covered by it at all.

So the budget is held in three places, none of which is a single switch:

| Where | What it bounds | Owner |
| --- | --- | --- |
| The sum of the parts | Every outbound wait plus every database bound, arithmetic that has to come out below the budget | `spring-boot-patterns` for outbound, `spring-data-jpa` for the database |
| The ingress | The wall-clock ceiling the platform enforces and the client actually experiences | The deployment, recorded in the profile |
| The client | Its own timeout, which is the number the caller feels regardless of anything here | Outside this project |

**Do the arithmetic and record the result.** A budget nobody added up is a number that agrees with
nothing. When the parts do not fit, the fix is a smaller part, not a larger budget — and when the
ingress ceiling is lower than the parts sum to, every request that reaches the ceiling leaves work
still running behind it, which is the case the statement and transaction bounds exist to stop.

Do not simulate a request timeout by wrapping handlers in an executor with a deadline. That adds a
thread hand-off, loses the request context, and leaves the abandoned work running anyway. The
bounds above are where a request is actually made to end.

## Choose the concurrency model

**This is a recorded decision, not a default**, and it belongs in the profile before the first
performance question is asked. The two models differ in what limits concurrency, which is the only
property that matters downstream.

| | Platform threads | Virtual threads |
| --- | --- | --- |
| Requests in flight | Capped by the server's thread pool | Effectively uncapped by the server |
| What limits concurrency | The thread pool, implicitly | Nothing, until something is made to |
| Blocking I/O costs | A platform thread for its whole duration | A carrier thread only while it is running |
| Requires | Nothing | Java 21 or later, and Spring Boot 3.2 or later |
| Fits | A predictable, already-sized workload; a project that has not yet decided | A blocking stack whose threads sit waiting on I/O, which is what this architecture is |

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Virtual threads suit this architecture, because a servlet stack over JDBC spends most of a request
blocked on I/O and that is exactly the cost they remove. Three things about them are conditions
rather than caveats:

- **Verify pinning behaviour on the configured Java release.** A virtual thread that blocks inside a `synchronized` region pins its carrier, which converts the benefit into a shortage of carriers under load. Later releases removed most of that; the release the profile records decides whether it still applies here, and a library the project does not control is the usual source. `build-and-dependencies` owns the release; this decision reads it.
- **Nothing is pooled, so nothing is reused — including thread-local state.** `observability-and-logging` owns correlation propagation and already requires it to be explicit rather than inherited; that requirement does not change, but the assumption "a pooled thread carries context" stops being available as a fallback.
- **Do not use them to run CPU-bound work in parallel.** They are for waiting, not for computing. Work that is genuinely CPU-bound belongs on a bounded executor sized to the available cores, whatever the request model is.

Where the project stays on platform threads, the pool is sized deliberately and recorded, not left
at the server's default:

```yaml
server:
  tomcat:
    threads:
      max: 100
    max-connections: 4000
    accept-count: 100
```

## What moves when the model changes

**Removing the thread-count ceiling does not remove the limit; it moves it to whatever is next, and
next is usually the database pool.** That is the single most important consequence of the decision
above, and it is why the decision is recorded rather than switched on.

With platform threads the server's pool is an implicit admission control: a hundred threads means at
most a hundred requests can be inside the application, so the database pool only ever sees a hundred
callers. With virtual threads there is no such number, and every request that arrives goes straight
to whatever it needs. Three bounds then become load-bearing, and each belongs to another skill:

| What now limits concurrency | Owner | What to re-derive |
| --- | --- | --- |
| The database connection pool and the wait for a connection | `spring-data-jpa`, in [resource bounds](../../spring-data-jpa/references/resource-bounds.md#pool-size-and-the-wait-for-a-connection) | The pool is unchanged in size, but it is now the admission control. The short connection-acquisition wait is what turns excess arrivals into fast failures instead of a growing queue |
| Per-caller rate limits and quotas | `application-security` | These were never a substitute for the thread pool and now have to stand on their own |
| Outbound client connection pools | `spring-boot-patterns`, in [outbound call rules](outbound-call-rules.md) | A client pool sized against the old thread count becomes the new queue |

State the re-derivation in the change. A model switch that leaves those three numbers untouched has
not been made safely, however green the build is.

Request and payload size limits are `application-security`'s, and they do not change with the model.
Do not restate them here.

## Answering without sending the representation again

A read that returns the same representation the caller already holds is the cheapest request to make
faster, because the fastest response is the one with no body. This is worth doing on read paths that
are requested repeatedly by the same caller and change rarely — a resource a client polls, a
reference list, a detail page behind a cache.

**Validate before building the representation, not after.** The difference decides whether the
saving is bandwidth or work:

```java
@GetMapping("/{userId}")
public ResponseEntity<UserTO> usersUserIdGet(
        @PathVariable final Long userId,
        final WebRequest request) {

    final UserVersionDomain version = this.userService.getVersion(userId);

    if (request.checkNotModified(version.entityTag())) {
        return null;
    }

    return ResponseEntity.ok()
            .eTag(version.entityTag())
            .body(UserRestMapper.INSTANCE.mapUserDomainToUserTO(this.userService.getById(userId)));
}
```

`getVersion` is a bounded read of the aggregate's version or last-modified column — the value
`spring-data-jpa` already maintains for optimistic locking — rather than a load of the whole
aggregate. When the caller's validator matches, `checkNotModified` sets `304` and the handler
returns `null` without ever loading, mapping, or serializing the resource. That is the saving.

Rules:

- **Derive the validator from state the aggregate already keeps.** A version column or a last-modified timestamp is stable, cheap to read, and changes exactly when the resource does. Do not compute a validator by hashing a representation you had to build first.
- **A filter that hashes the rendered response saves bandwidth and no server work at all.** It buffers the whole response, hashes it, and then decides not to send it — every query, every mapping, every serialization already happened. Use it only when saving bytes on the wire is the actual goal, and never as a substitute for the handler-level check above.
- **The `ETag`, the `304`, and the conditional request headers are part of the contract.** `rest-api-contract` owns whether they are documented and whether adding them is breaking; this skill owns only where the check sits in the handler.
- **Do not mix this with `If-Match`.** A conditional *read* uses `If-None-Match` and answers `304`; the stale-write protection `spring-data-jpa` records uses `If-Match` and answers `412`. They are two mechanisms over one header family, and a project that adopts both states which endpoints use which.
- **`Cache-Control` on a response is a security decision before it is a performance one.** `application-security` owns what may be cached and by whom; add a directive only under its rules.

## Compression

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/problem+json
    min-response-size: 2KB
```

- **Prefer compressing at the ingress where one exists.** A proxy or gateway in front of the application does this more cheaply than the application does, and it does it for every service at once. Enable it in-process only when nothing in front is doing it.
- **It trades CPU for bytes, so it is a decision, not a default.** On a small JSON response the compression costs more than the transfer saves, which is what the size floor is for. Leave the floor in place rather than compressing everything.
- **Name the media types the API actually returns**, including the problem media type, or error responses go out uncompressed while success responses do not.

## Shutdown

An instance that stops accepting a request it already accepted turns a deploy into errors the caller
sees, and it is one property away from not doing that.

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s
```

- **Keep the shutdown grace period above the request budget**, so an in-flight request has time to finish rather than being cut off at the point the platform stops routing.
- **Keep the platform's own termination grace period above this one.** Where the orchestrator kills the process before Spring finishes the phase, the setting has no effect, and nothing reports that.
- **Readiness comes out first.** `observability-and-logging` owns the probe; what matters here is that the instance stops being routed to before it stops serving, not at the same moment.

## Proving it

- **The concurrency model:** a load test at a concurrency above the recorded database pool size, asserting that excess callers fail fast on connection acquisition rather than queueing without bound. That is the assertion that proves admission control moved to where the table above says it did.
- **The conditional read:** two requests, the second carrying the validator from the first, asserting `304`, an empty body, **and** that the expensive read did not run — a query count, or a mocked service the test asserts was not called. Asserting only the status passes just as happily when the handler built the whole representation and threw it away.
- **Shutdown:** send a request, trigger shutdown while it is in flight, assert it completes.
- **The budget arithmetic** is checked in review, not by a test. `spring-boot-code-review` owns that.

Where the load test does not exist, say so. `_core/README.md` records that nothing in this set owns
performance *measurement*: these rules bound what a request may cost, and none of them is evidence
that the application is fast.

## Rejected shapes

```yaml
# Wrong: read as a request timeout. It bounds only asynchronous return types, so a project whose
# handlers return values directly has no timeout at all and a line of configuration saying it does.
spring:
  mvc:
    async:
      request-timeout: 10s
```

```yaml
# Wrong: virtual threads enabled without re-deriving anything below them. The server now admits
# unlimited requests into a pool of ten connections, and the acquisition wait is where the whole
# load lands.
spring:
  threads:
    virtual:
      enabled: true
```

```java
// Wrong: the validator is computed from the representation, so every 304 costs a full load, a full
// mapping, and a full serialization. The caller saves bytes; the server saves nothing.
final UserTO body = UserRestMapper.INSTANCE.mapUserDomainToUserTO(this.userService.getById(userId));

if (request.checkNotModified(Integer.toHexString(body.hashCode()))) {
    return null;
}
```

```java
// Wrong: a deadline wrapped around a handler. The caller gets a timeout, the work carries on
// behind it holding its connection, and the request context did not cross the hand-off.
return CompletableFuture
        .supplyAsync(() -> this.userService.getById(userId), this.executor)
        .orTimeout(10L, TimeUnit.SECONDS)
        .join();
```
