# Cache design

Use this file when adding a cache or changing what one stores. Apply every rule from `../SKILL.md`.

**This file carries rules, not only examples.** The cache register, the key-identity derivation, TTL
and size selection, negative caching, stampede protection, and warmup are stated here in full and
nowhere else.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [The cache register](#the-cache-register)
2. [What is worth caching](#what-is-worth-caching)
3. [Key identity](#key-identity)
4. [TTL](#ttl)
5. [Size and eviction](#size-and-eviction)
6. [Negative caching](#negative-caching)
7. [Stampede](#stampede)
8. [Warmup and cold start](#warmup-and-cold-start)

## The cache register

**Every cache in the application is declared once, in one place**, the way `spring-boot-patterns`
requires of caller-visible failures. Without it, a TTL is a number in a properties file that nobody
can justify and nobody dares change, and a cache nobody can describe is a cache nobody can remove.

The register lives in `docs/project-profile.md`, under Caching. One row per cache:

| Column | What it records |
| --- | --- |
| Name | The cache name, in the form `project-naming-conventions` owns |
| Contents | What one entry holds, as a type and a sentence |
| Justification | Structural, or measured with the measurement named |
| Staleness budget | A duration **and** the consequence of exceeding it |
| TTL | At or below the budget |
| Invalidation | The write that triggers it, or `none — TTL only` |
| Key inputs | Every input that varies the value, tenant and subject included |
| Size bound | Entries or bytes, and where the bound is enforced |

Two rules about the register itself:

- **A cache that is not in it does not exist.** Adding one to the code and not to the register is the same defect as an unrecorded profile decision, and `spring-boot-code-review` reports it as one.
- **Removing a cache removes its row, its configuration, its keys, and its meters.** A cache switched off but left declared is a trap for the next person, who will assume it is load-bearing.

## What is worth caching

`../SKILL.md` states the two admissible justifications. This is how to tell whether a candidate has
one, in order — stop at the first that applies.

1. **Is the source expensive because it is broken?** A read that fans out into N queries, scans without an index, or returns rows the caller discards is not expensive; it is defective. `spring-data-jpa` owns all three. Fix it, measure again, and most candidates disappear here.
2. **Is the value the same for many callers?** A cache pays for itself through sharing. A value keyed so specifically that each entry is read once — a per-request computation, a per-subject list nobody re-requests — costs a write and a miss and returns nothing. Check the expected hit ratio *before* building it: an entry read once has a hit ratio of zero however fast the store is.
3. **Does it change less often than it is read?** The ratio, not the absolute rate, is what decides. A value read a thousand times a second and written a thousand times a second caches badly no matter how hot it is.
4. **Can the application be correct with a value that is up to the budget old?** If the honest answer is no, stop. This is the question that kills more candidates than performance ever does.

Good candidates share a shape: reference and lookup data, the results of derivations over slowly
changing inputs, responses from rate-limited or slow external providers, and permission or
configuration structures read on nearly every request. Poor candidates share a shape too: anything a
user just wrote and expects to see, anything a monetary or inventory decision reads, and anything
whose staleness produces a wrong answer rather than an old one.

## Key identity

The key is a function of every input that varies the value. Derive it, do not invent it.

1. **List the inputs the method actually reads** — its parameters, and the ambient state it consults: the tenant, the authenticated subject, the locale, the API version, a feature flag.
2. **Remove the ones that cannot change the result.** A parameter used only for logging is not identity.
3. **Everything left is in the key.** If that makes the key unusably specific, the value is not cacheable in that shape — cache something less specific and derive the rest per call.

```java
@Cacheable(cacheNames = CacheNames.PRODUCTS_BY_SKU, key = "#tenantId + ':' + #sku")
public ProductDomain getBySku(final Long tenantId, final String sku) {
    return this.productRepository.findByTenantIdAndSku(tenantId, sku)
            .map(ProductDomainMapper.INSTANCE::mapProductEntityToProductDomain)
            .orElseThrow(() -> new ResourceNotFoundException("Product", sku));
}
```

Rules:

- **Never rely on the default key generator for a method with more than one parameter.** It composes the arguments in declaration order, so adding, removing, or reordering a parameter silently changes every key — old entries become unreachable, which is the harmless case, and two different calls can collide, which is not. Write the key expression, and it becomes a diff when the signature changes.
- **A key expression may reference only parameters.** Reaching into ambient state from the expression puts identity in a string nobody tests. Where the tenant or the subject is ambient, pass it as a parameter to the cached method — that is what makes it part of the identity rather than a hidden input.
- **Do not key on a mutable object.** The key is computed from `toString` or `hashCode`; if the object changes, the same logical call produces a different key. Key on the identifier.
- **The tenant is not optional and not implied.** A cache whose key omits the tenant is correct exactly until the second tenant exists, and then it is a data leak that no test with one tenant can see.

Rejected:

```java
// Wrong: the result is filtered by who asked, and the key says only which order it is.
// The first caller's permitted view is served to every later caller.
@Cacheable(cacheNames = CacheNames.ORDER_LINES, key = "#orderId")
public List<OrderLineDomain> visibleLines(final Long orderId, final Long subjectId) {
    return this.authorizationPolicy.filterForSubject(this.orderService.lines(orderId), subjectId);
}
```

```java
// Wrong: identity taken from ambient state inside the expression. Nothing in the signature says
// the result is tenant-specific, so the next caller passes a different tenant and gets this one.
@Cacheable(cacheNames = CacheNames.PRODUCTS_BY_SKU,
        key = "T(com.example.myapp.security.TenantContext).current() + ':' + #sku")
public ProductDomain getBySku(final String sku) { ... }
```

## TTL

The TTL follows from the staleness budget and nothing else. It is not a tuning knob and it is not
"long enough to get a good hit ratio".

- **TTL ≤ staleness budget**, always. When the hit ratio at that TTL is disappointing, the answer is a different cache or no cache, never a longer TTL — a longer TTL is a unilateral change to a business decision.
- **Set a TTL even where invalidation is explicit.** `../SKILL.md` gives the three reasons: a lost invalidation after commit, a failed eviction, and an instance that never received it. The TTL is the only mechanism that repairs all three, and it does so without anyone noticing.
- **Prefer expiry after write over expiry after access** for anything derived from a source that changes. Expiry after access keeps a hot entry alive indefinitely, so the hottest key — the one most callers see — is the one that gets stalest. That is the opposite of what the budget intends.
- **Spread the expiry of entries populated together.** A reference set loaded in one pass expires in one pass, and every caller misses at the same instant. Where the technology supports it, add a small random jitter per entry; where it does not, that is a reason to prefer refresh over expiry for that cache.
- **Per-entry TTL is not available everywhere.** Some stores carry a TTL per entry, others only per cache or region. The property checklist in [technology and topology](technology-and-topology.md#the-property-checklist) records which, and a design needing per-entry TTL on a store that has none needs a different design, not a workaround.

## Size and eviction

- **A local cache always declares a maximum**, in entries or in weight. An unbounded in-process cache is a memory leak that survives every review because it looks like configuration. There is no exception: a set that is "obviously small" is one product decision away from not being.
- **Size from the working set, not from the total.** The point is to hold what is actually re-read. A cache large enough for every possible entry is spending memory on entries with a hit ratio of zero.
- **A distributed store enforces its own bound server-side**, under a policy the platform configures. That is infrastructure, not application code — but the application still has to know what happens when the bound is reached, because the answer differs by store: evict the least recently used, evict only entries with a TTL, or refuse writes. Record which; a store that refuses writes turns a full cache into failing writes, and `../SKILL.md`'s fail-open rule then decides what the caller sees.
- **Eviction pressure is a signal, not a routine event.** A high eviction rate beside a good hit ratio means the cache is thrashing: it is holding the wrong entries and paying to replace them. `observability-and-logging` owns the meter; what this skill requires is that somebody looks at eviction rate and hit ratio together, because either alone is misleading.

## Negative caching

Caching "this does not exist" prevents a storm of identical misses, and introduces two problems the
positive path does not have.

- **A negative entry needs its own, shorter TTL.** The cost of a stale negative is worse than a stale positive: a resource that now exists is invisible until the entry expires, which reads as a bug in creation rather than in caching.
- **Creation must invalidate the negative entry**, under the same after-commit rule as every other invalidation. A create path that populates the positive cache and forgets the negative one produces a resource that exists and cannot be found.
- **Bound what can be negatively cached.** An endpoint that accepts a caller-supplied identifier and negatively caches every miss lets anyone fill the cache with entries for identifiers that will never exist. `application-security` owns that abuse case and its limits; this skill's part is that unbounded negative caching is a design defect and not merely a security one.
- Where the miss is cheap, do not cache it at all. Negative caching earns its complexity only when the lookup that returns nothing is itself expensive.

## Stampede

When a hot entry expires, every concurrent caller misses at once and every one of them runs the
expensive load. The source then receives a burst precisely when it was being shielded.

- **Single-flight within an instance is one annotation attribute**, and it is the cheapest useful protection: the first caller loads and the others wait for that load. [Spring cache examples](spring-cache-examples.md#single-flight) shows it.
- **It coordinates one JVM only.** With `n` instances, `n` loads still run. Say so in the register rather than assuming the attribute solved the problem — on a small deployment `n` loads may be perfectly acceptable, and knowing that is the point.
- **Cross-instance single-flight needs a shared lock**, which is a distributed lock with all the costs `spring-data-jpa` describes for a pessimistic one: it must be bounded, it must not be held across a slow call, and its failure mode must be to load anyway rather than to fail the request. Reach for it only when a measurement shows the concurrent loads actually hurt.
- **Refreshing before expiry avoids the problem instead of managing it.** Where the technology supports refresh-after-write, a stale-but-valid entry is served while one caller repopulates, so no caller ever waits on a miss. This is the better answer for a small set of very hot keys, and it costs a slightly larger staleness window — which the budget has to allow.
- **The worst stampede is a cold start**, and it is not solved by any of the above. See below.

## Warmup and cold start

An empty cache sends every request to the source. That happens on every deploy, every restart, every
scale-out, and every failover of a distributed store that does not persist.

- **Decide whether the source survives a cold cache**, and record it. This is the load-bearing question `../SKILL.md` raises: if the source cannot take the full read load, the cache is part of the availability design and the deployment has to account for it — a rolling restart rather than a simultaneous one, or a warmup step before the instance reports ready.
- **Where warmup is needed, it belongs before readiness, not after startup.** An instance that is routed to while its cache is empty is an instance serving slow requests, and `observability-and-logging` owns the readiness probe that prevents it. Note how its rule and this one fit together rather than conflicting: that skill keeps a dependency **with** a graceful fallback out of readiness, and a source that cannot carry the load is precisely a cache that has no graceful fallback. Answering the cold-start question is therefore what decides whether the probe rule applies.
- **Keep warmup bounded and idempotent.** It is startup work like any other: it needs a time limit, it must not fail the instance when the source is briefly unavailable, and it must be safe to run on every instance at once — or it becomes its own stampede.
- **Do not warm a cache the application can serve without.** Warmup that exists to improve a benchmark adds a startup dependency for no operational benefit.
