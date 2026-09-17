---
name: application-caching
description: Server-side caching design for Java 21+ Spring Boot applications, independent of the cache technology. Use when adding, changing, or reviewing a cache — the Spring cache abstraction and @Cacheable, cache keys, TTL and staleness, invalidation and its ordering against a transaction, eviction and sizing, serialization of cached values, stampede and negative caching, behavior when the cache is unavailable, and the Hibernate second-level cache. Covers local, distributed, and near-cache topologies and any store, chosen or not yet chosen. Excludes HTTP response caching, CDN behavior, and the database's own buffer cache.
---

# Application Caching

A cache is a **correctness decision before it is a performance one**. Every cache trades freshness
for speed, and the only question that matters first is how stale this data may be and who decided
that. A cache added without that answer is not an optimization; it is an undated copy of the truth
that nobody owns.

This skill owns cache design. It does not own whether the project has a cache at all — that is a
recorded decision — and it does not own what may be cached, which is a classification question.

## This skill is cache-technology-agnostic

The project may end up on Redis, Hazelcast, Caffeine, Infinispan, Memcached, something else, or
nothing. **Write against Spring's cache abstraction, and decide against the chosen technology's
actual properties.** The abstraction makes the call sites identical everywhere, and that is
precisely the trap: it hides consequences that are not abstract. Whether a cached value is copied
or shared by reference, whether an entry can carry its own TTL, whether one instance's write is
visible to another, and what happens when the store is unreachable all differ by technology, and
each of them changes a rule in this skill.

So the shape of the work is fixed and the answers are not:

- **Before a technology is chosen**, design the cache as if every property were unfavourable — assume values are shared by reference, assume no cross-instance invalidation, assume the store can vanish. A design that survives those assumptions survives every technology.
- **When one is chosen**, fill in the property checklist in [technology and topology](references/technology-and-topology.md#the-property-checklist) and re-read the rules it changes. That checklist is the whole of what the technology decision alters here.
- **Never infer the technology from a dependency or an example.** `docs/project-profile.md` records it, and `application-security` and `project-naming-conventions` both say the same thing where they mention a store by name.

## Coordination with other skills

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what, and it
carries the precedence order for a genuine conflict. Read it there rather than from a copy in this
file. The seams this skill crosses most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| What is in a key | the **identity**: every input that varies the value, including the tenant and the authorizing subject | `project-naming-conventions` owns the cache name and the key's textual form |
| What may be cached | nothing | `application-security` owns classification, the values that may never be cached, and abuse limits on cache growth |
| Where a cache sits | the read-through boundary and the point invalidation is triggered from | `spring-boot-patterns` owns the layers, the after-commit mechanism, and proxy semantics |
| Cache instrumentation | which cache behaviors must be visible | `observability-and-logging` owns the meters and what they are called |
| The Hibernate second-level cache | that it is a cache and obeys every rule here | `spring-data-jpa` owns the mapping, the region configuration, and the provider settings |
| HTTP caching | nothing — a `304` is not this skill's | `spring-boot-patterns` owns conditional reads, `application-security` owns `Cache-Control` |
| The dependency and its version | the requirement | `build-and-dependencies` owns the coordinate, the starter, and the version |
| Test levels | which cache scenarios need proof | `spring-boot-testing` owns the level each runs at |

## Reference routing

Read only what the change requires.

| Read | When |
| --- | --- |
| [Cache design](references/cache-design.md) *(rules)* | Adding a cache, or changing what one stores: the staleness budget, key identity, TTL, sizing, negative caching, stampede, and the cache register |
| [Invalidation and consistency](references/invalidation-and-consistency.md) *(rules)* | Any write that makes a cached value wrong, and every question about ordering, distributed invalidation, or a rolling deploy |
| [Technology and topology](references/technology-and-topology.md) *(rules)* | Choosing local, distributed, or near-cache; filling the property checklist when a technology is selected; deciding the Hibernate second-level cache |
| [Spring cache examples](references/spring-cache-examples.md) | The worked code: annotations, configuration, the after-commit eviction, the error handler, serialization, and the rejected forms |

## No cache exists until the profile says so

**While `Cache used` is `no` or `UNDECIDED` in `docs/project-profile.md`, do not introduce a cache, a
cache annotation, a cache dependency, or a cache abstraction "for later".** This is the same gate
`project-decision-profile` applies to every unrecorded decision, and it holds for the Hibernate
second-level cache and the query cache exactly as it does for an explicit one — both are caches, and
both are switched on by a property that reads like tuning.

When the row is `yes`, three more rows must be answered before the first cache is written: the
technology, the topology, and the failure mode. The template records them, and
[technology and topology](references/technology-and-topology.md) states what each one changes.

## A cache needs a reason, and there are only two kinds

`spring-data-jpa` grades a pessimistic lock the same way, and for the same reason: a mechanism with
a permanent cost needs a justification that is either structural or measured, and "it seemed
faster" is neither.

| Justification | What it looks like | Evidence needed |
| --- | --- | --- |
| **Structural** | The source is intrinsically expensive or limited: a rate-limited external provider, a computation over an unchanging reference set, a permission derivation that fans out across aggregates | None. The property is a fact about the source, not about load |
| **Measured** | A specific read path is hot and the source is the bottleneck | A profile or a production measurement naming the path, its share of load, and the source's contribution to latency |

Anything else is not a reason. In particular, a cache is **never** the answer to an N+1 query, a
missing index, an unbounded read, or a query that returns more than the caller needs —
`spring-data-jpa` owns all four, each has a fix that removes the cost rather than hiding it, and a
cache in front of a broken query preserves the defect and adds staleness to it.

Record the justification beside the cache in the register. A cache whose reason nobody can restate
is a cache nobody can safely remove.

## The staleness budget is the first decision

**Every cache entry is a value that was true at some point in the past.** How far in the past it may
be is a business decision, not a technical one, and it is the input to every other decision here:
it sets the TTL, it decides whether invalidation is required or optional, and it decides whether
the topology can be local.

- **Record a staleness budget per cache**, in the register, as a duration and the consequence of exceeding it. "Up to five minutes; a user sees an old display name" is a budget. "Short" is not.
- **A budget of zero means no cache.** Data that must be exactly current cannot be cached, and building a cache that is invalidated on every write to make it appear current gives up the benefit while keeping the risk.
- **The TTL is never longer than the budget**, and it is set even when the cache is explicitly invalidated. Invalidation can be lost — a crash after commit, a message that never arrives, an instance that was partitioned — and the TTL is the only thing that eventually repairs an entry nobody will ever evict. A cache with invalidation and no TTL is a cache that is correct until the first failure and wrong forever afterwards.
- **Different values from one source can have different budgets.** Do not give a whole aggregate one cache and one TTL because it came from one table.

## The key is the complete identity of the value

**Every input that changes the result belongs in the key.** This is the rule whose failure is worst,
because the symptom is not a stale value — it is one caller receiving another caller's data.

- **The tenant belongs in the key** wherever `Tenancy model` in `docs/project-profile.md` records anything other than `single-tenant`, without exception — the Hibernate second-level cache regions included, where a shared region serves one tenant's entity to another. `application-security` owns that row and `project-naming-conventions` owns the form the scope takes.
- **If the value was filtered by who asked, the authorizing subject belongs in the key.** A list a service trimmed to what this caller may see is not a property of the resource; it is a property of the resource *and the caller*. Caching it under the resource identifier alone serves the first caller's permitted set to everyone.
- **Prefer caching the unfiltered value and applying authorization after the read.** It keys smaller, it shares better, and it removes the class of bug above entirely. Cache a per-subject result only when the filtering itself is the expensive part, and then say so in the register.
- **A cache is never a source of truth for an authorization decision.** Cache the data an authorization check reads, with a budget that reflects how quickly a revocation must take effect; do not cache the decision and skip the check.
- Locale, currency, API version, feature-flag state, and any other request-scoped value that changes the result are inputs like any other. A key that omits one produces a value that is correct for whoever populated it.

[Cache design](references/cache-design.md#key-identity) carries the derivation rules and the
rejected forms.

## Invalidation happens after the commit, never inside it

This is the rule most often written wrong, and the wrong version is worse than no invalidation at
all, because it produces an entry that is permanently stale rather than briefly stale.

Spring's `@CacheEvict` runs in the interceptor around the method. On a method that is also
`@Transactional`, that means the eviction happens **before the commit** — and in the window between
the two, a concurrent reader misses, loads the old row from the database, and repopulates the cache.
The commit then lands, and the cache holds the pre-write value with a fresh TTL. Nothing throws,
nothing is logged, and the entry stays wrong until it expires.

- **Trigger every invalidation after commit**, through the same after-commit mechanism `spring-boot-patterns` owns for external effects, or through a transaction-aware cache decorator. [Invalidation and consistency](references/invalidation-and-consistency.md) states both shapes and when each fits.
- **Know what after-commit does not buy**, exactly as `spring-boot-patterns` states for external effects: the commit has already succeeded, so a crash between the commit and the eviction loses it permanently. That is the second reason every entry carries a TTL.
- **Prefer evicting over updating in place.** An eviction is idempotent and cannot write a wrong value; a write-through update recomputes a value outside the transaction that produced it and can race with another writer. Update in place only where the recomputation is genuinely expensive and the register records the choice.
- **On more than one instance, an eviction is local unless the technology makes it shared.** A local cache on four instances needs four evictions, and nothing in the code says so. This is a topology consequence, not a coding one; [technology and topology](references/technology-and-topology.md) states which topologies have it.

## The cached form is a compatibility contract

A cached entry outlives the instance that wrote it and, during a rolling deploy, is read by a
version of the application that did not write it. That makes the serialized shape a contract in the
same sense `rest-api-contract` means it, with the same consequence: a change that old readers cannot
parse is breaking, and it breaks in production during the deploy rather than at compile time.

- **Never use Java serialization for cached values.** It is a deserialization sink `application-security` rejects, and it couples the entry to the class's private shape, so an unrelated refactor invalidates the format.
- **Change the key namespace version when the serialized shape changes incompatibly** rather than editing the shape in place. `project-naming-conventions` owns the version segment; this skill owns when it has to move. Old entries then age out under their own TTL while new readers populate the new namespace, which is expand-and-contract applied to a cache.
- **Cache a type the project owns and controls** — a domain record or a purpose-built cached-value record — never an entity, never a framework type, never a type from a library that may change under a dependency upgrade. An entity in a cache is also a detached entity with lazy associations, which is a second defect on its own.
- **A Spring Boot generation change is a serialization event.** The JSON library changes major version between the supported generations, so the serializer that writes cache values is not the same component before and after. Treat the upgrade as a namespace change and verify what the new instances read, rather than assuming a JSON payload is a JSON payload.

## The cache must fail open, and failing open must be bounded

An unavailable cache must not take the application with it. That is easy to state and has two
consequences people miss.

- **A cache miss caused by an error is still a miss.** Install an error handler so a failure to read, write, or evict logs once and lets the operation continue against the source. Without one, the default behavior propagates the exception and a cache outage becomes an application outage — the exact inversion of why the cache exists.
- **Bound every cache operation with a timeout well inside the request budget.** A cache that hangs is worse than a cache that is down: the request waits for the cache *and then* does the work it was meant to avoid. `spring-boot-patterns` owns the request budget; this operation fits inside it like any other wait.
- **An eviction that fails is not a miss.** Failing open on a read is safe; failing open on an invalidation leaves a stale entry behind. Count those separately and let the TTL be the repair — this is the third reason a TTL is not optional.
- **Decide whether the source survives losing the cache, and record the answer.** A cache that absorbs most of the read load is a dependency the capacity plan has to know about: when it empties, the source takes everything at once. If the source cannot take it, the cache is not an optimization, it is a load-bearing component, and it needs the availability design of one.

## Testing a cache proves behavior, not the cache

`spring-boot-testing` owns the levels. What this skill requires is that these five scenarios exist
wherever a cache does, because each of them is a defect that passes an ordinary test suite:

1. A second identical read does not reach the source, and returns the same value.
2. A write makes the next read return the new value — through the real transaction, so the after-commit ordering is what is under test.
3. Two callers who may see different data do not share an entry. This is the key-identity test, and it is the one that catches the cross-subject leak.
4. The operation still succeeds when the cache is unavailable.
5. An entry expires within its TTL.

Assert the observable outcome and the number of source invocations, never the cache's internal
state. A test that reads the cache directly proves the test can reach the cache.

## Anti-patterns

Reject:

- a cache with no recorded staleness budget, or no reason beyond "it is faster";
- a cache in front of an N+1 query, a missing index, or an unbounded read, instead of the fix;
- a key missing the tenant, the authorizing subject, or another input that varies the value;
- a cached authorization decision, rather than cached data the check still reads;
- `@CacheEvict` on a `@Transactional` method, evicting before the commit;
- invalidation with no TTL behind it;
- an unbounded local cache — a memory leak with a nice name;
- Java serialization, a JPA entity, or a library type as the cached value;
- an incompatible change to a cached shape without a namespace version change;
- a cache failure that propagates to the caller, or a cache call with no timeout;
- `@Cacheable` reached by self-invocation, where the proxy never runs and the cache silently never applies;
- the Hibernate second-level cache or query cache switched on without the profile recording a cache at all;
- a cache introduced to make a load test pass, with the staleness cost discovered in production.

## Completion checklist

- [ ] The profile records a cache, its technology, its topology, and its failure mode, and this change stayed inside them.
- [ ] Every cache the change adds is in the register with its contents, justification, staleness budget, TTL, invalidation trigger, and size bound.
- [ ] The key carries every input that varies the value, including tenant and — where the value is filtered — the authorizing subject.
- [ ] Every write that makes a cached value wrong invalidates it after commit, and every entry has a TTL as the backstop.
- [ ] The cached type is project-owned, the serialization is not Java serialization, and an incompatible shape change moved the namespace version.
- [ ] Cache failures are handled and bounded, and the source can carry the load if the cache empties.
- [ ] The five required scenarios are tested, including the two-caller key-identity test.
- [ ] `observability-and-logging` has what it needs to see hit ratio, evictions, and cache errors.
- [ ] Nothing in the change caches a value `application-security` classifies as uncacheable.

## Primary guidance

- [Spring Framework: Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Boot: Caching](https://docs.spring.io/spring-boot/reference/io/caching.html)
- [Spring Framework: Transaction Synchronization](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
