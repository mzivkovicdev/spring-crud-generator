# Invalidation and consistency

Use this file whenever a write makes a cached value wrong, and for every question about ordering,
multi-instance invalidation, or a rolling deploy. Apply every rule from `../SKILL.md`.

**This file carries rules, not only examples.** The transaction ordering, the choice between
evicting and updating, distributed invalidation, the namespace-version migration, and the
rolling-deploy rules are stated here in full and nowhere else.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [The ordering problem](#the-ordering-problem)
2. [Evict, do not update](#evict-do-not-update)
3. [Which caches one write touches](#which-caches-one-write-touches)
4. [More than one instance](#more-than-one-instance)
5. [Changing the shape of a cached value](#changing-the-shape-of-a-cached-value)
6. [Rolling deploys](#rolling-deploys)
7. [Rejected forms](#rejected-forms)

## The ordering problem

There are two moments to invalidate around a write, and only one of them is correct.

| Order | What happens | Result |
| --- | --- | --- |
| **Evict, then commit** | Between the two, a concurrent reader misses, loads the **old** row, and repopulates | The cache holds the pre-write value with a fresh TTL. Permanently stale, silently |
| **Commit, then evict** | Between the two, a concurrent reader gets the old cached value | Briefly stale, then correct. Self-healing |

The second is the only acceptable one, and the first is what Spring does by default. `@CacheEvict`
runs in the cache interceptor around the method; when the same method is `@Transactional`, the
interceptor completes before the transaction commits. The code reads as though it invalidates and
does the opposite of what it looks like.

Two shapes fix it. Choose one for the project and use it consistently, because a codebase where some
evictions are transaction-aware and others are not has no rule anyone can apply.

- **An after-commit listener.** The write publishes an event, and a listener bound to the after-commit phase performs the eviction. This is the mechanism `spring-boot-patterns` already owns for external effects, it makes the ordering visible at both ends, and it is the shape to prefer where a write invalidates several caches or where the eviction is not a simple key removal.
- **A transaction-aware cache manager.** The cache manager is decorated so writes and evictions are deferred to after commit automatically. It is less code and it applies everywhere at once, which is its strength and its weakness: nothing at the call site says the ordering is handled, so the decorator itself becomes a piece of configuration nobody may remove.

[Spring cache examples](spring-cache-examples.md#invalidating-after-commit) carries both.

**What after-commit does not buy** is the same limit `spring-boot-patterns` states for external
effects, and it is why `../SKILL.md` requires a TTL behind every invalidation: the commit has
already succeeded when the listener runs, so a crash, a redeploy, or a failure inside the listener
loses the eviction permanently, with no rollback and no record that anything was owed. The entry is
then wrong until it expires. Nothing here removes that; the TTL bounds it.

## Evict, do not update

Two ways to make a cache reflect a write, and they are not equivalent.

| | Evict | Update in place |
| --- | --- | --- |
| What it writes | Nothing | The new value |
| Cost of getting it wrong | A miss, then a correct load | A wrong value with a full TTL |
| Concurrent writers | Both evict; the next read is correct | Two updates race; the loser's value can be the one that stays |
| Recomputation | Happens in the next read's own transaction | Happens outside the transaction that produced it |

**Evicting is the default**, because it is idempotent and cannot write a value that is wrong. An
eviction applied twice, out of order, or after an unrelated write costs one extra miss. An update
applied out of order leaves the older value in place, and nothing detects it.

Update in place only when the recomputation is genuinely expensive *and* the write path already has
the complete new value *and* the register records the decision. Even then, prefer computing the
value inside the same transaction and publishing it with the after-commit event, so what lands in
the cache is what committed rather than the result of a second read that may already have moved on.

## Which caches one write touches

An eviction that removes one key while three other caches still hold values derived from the same
row is the second most common invalidation defect, and it is invisible in the diff — the write path
has an eviction, so it looks handled.

- **Work from the register, not from the method.** For every write, read down the register and ask of each cache whether this write can change what it holds. That is a mechanical check and it is the only reliable one.
- **Derived and aggregate caches are the ones that get missed.** A cached count, a cached list, a cached "does this user have any active order" flag — all invalidated by a write that names none of them.
- **Where one write invalidates several caches, evict them together in one after-commit listener**, so the set is visible in one place and a new cache is added by editing one method.
- **A cache derived from another cache is a design error, not an invalidation problem.** Two layers of staleness compose in ways nobody can reason about. Derive from the source.

## More than one instance

**Whether an eviction on one instance is seen by another is a property of the topology, not of the
code.** The call site is identical either way, which is why this has to be decided rather than
assumed.

| Topology | An eviction on instance A | What the design must do |
| --- | --- | --- |
| Distributed store | Is the eviction, for everyone | Nothing extra |
| Local cache per instance | Affects instance A only | Either accept a staleness window equal to the TTL on the other instances, or broadcast the eviction |
| Near-cache: local in front of distributed | Clears A's local copy and the shared one; B's local copy survives | Requires the store's own invalidation broadcast, and requires verifying it actually works |

Three rules follow:

- **A local cache with a short TTL and no broadcast is a legitimate design**, and often the right one: it is simple, it has no network hop, and the staleness window is bounded by a number the budget already approved. Choose it deliberately and write the window into the register — do not arrive at it by forgetting that the other instances exist.
- **Broadcasting an eviction is a messaging problem**, with delivery guarantees the set does not currently own. `_core/README.md` records messaging mechanism as an open gap; a project that needs broadcast invalidation is choosing to solve that problem, and the TTL remains the backstop for every message that does not arrive.
- **A near-cache is the hardest of the three and the easiest to adopt by accident**, because some client libraries enable one by default. Verify whether the configured client keeps a local copy; if it does, the middle row of that table applies whatever the profile says the topology is.

## Changing the shape of a cached value

A cached entry written yesterday is read by the code deployed today. When the shape changed, the
read fails, or worse, succeeds into a value with a missing field.

**An incompatible shape change moves the key namespace version.** `project-naming-conventions` owns
the version segment in the key form; this skill owns when it has to move:

| Change | Compatible? | Action |
| --- | --- | --- |
| Add a field readers can ignore | Yes, if the serializer tolerates unknown fields — verify | No version change |
| Remove or rename a field | No | New version |
| Change a field's type or meaning | No | New version |
| Change the serializer or its configuration | No | New version |
| Change the JSON library major version | No | New version |

Moving the version is the whole migration: new instances write and read the new namespace, old
entries age out under their own TTL, and nothing has to be purged. That is expand-and-contract
applied to a cache, and it is why `sql-database-migration`'s framing transfers directly — the
difference is that a cache contracts itself, on the TTL, without a second release.

**Do not flush the whole cache instead.** A flush is a deliberate stampede against the source, at
deploy time, on every instance at once, and it does not help the instances still running the old
code.

## Rolling deploys

During a rolling deploy, two versions of the application read and write the same cache. Everything
above applies at once, so it is worth checking as a set:

- Old instances write the old shape; new instances must not read it. The namespace version is what separates them.
- New instances write the new shape; old instances must not read it either. The same version segment handles both directions.
- An eviction performed by a new instance must still invalidate what an old instance cached, or the old instance serves stale data for the rest of the rollout. With a shared namespace this is automatic; with a version change the old entries are simply abandoned, which is correct.
- A cache whose entries are large or numerous doubles its memory during the overlap, because both namespaces are live. Where that matters, the size bound has to allow for it.

**A Spring Boot generation upgrade is a rolling deploy with a serializer change in it**, which is the
one case where all of the above happens at once. Treat it as a namespace change, and verify what the
new instances actually read rather than assuming JSON is JSON — `build-and-dependencies` records
that the JSON library changes major version between the generations.

## Rejected forms

```java
// Wrong: the eviction runs inside the transaction. A concurrent reader repopulates from the
// pre-write row before the commit lands, and the cache is stale until the TTL expires.
@Transactional
@CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_SKU, key = "#tenantId + ':' + #sku")
public ProductDomain rename(final Long tenantId, final String sku, final String name) {
    ...
}
```

```java
// Wrong: invalidation with no TTL behind it. Correct until the first lost eviction, then
// wrong forever, and nothing reports it.
@Bean
CacheManager cacheManager() {
    return new ConcurrentMapCacheManager(CacheNames.PRODUCTS_BY_SKU);
}
```

```java
// Wrong: one cache evicted, three others left holding values derived from the same row.
// The write path has an eviction, so review sees the rule as satisfied.
@CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_SKU, key = "#tenantId + ':' + #sku")
public void deactivate(final Long tenantId, final String sku) {
    // PRODUCT_COUNT_BY_TENANT and ACTIVE_SKUS_BY_TENANT are now wrong.
}
```

```java
// Wrong: a flush at startup to deal with a shape change. Every instance empties the cache
// during the deploy and the source takes the entire read load at the worst moment.
@EventListener(ApplicationReadyEvent.class)
public void clearCaches() {
    this.cacheManager.getCacheNames()
            .forEach(name -> this.cacheManager.getCache(name).clear());
}
```

```java
// Wrong: the value written to the cache came from a read after the commit, not from the
// transaction that produced it, so a concurrent write can already have replaced it.
this.productRepository.save(product);
this.cacheManager.getCache(CacheNames.PRODUCTS_BY_SKU)
        .put(key, this.productService.getBySku(tenantId, sku));
```
