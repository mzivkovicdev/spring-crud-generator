# Technology and topology

Use this file when choosing where a cache lives, when a cache technology is selected or changed, and
when deciding anything about the Hibernate second-level cache. Apply every rule from `../SKILL.md`.

**This file carries rules, not only examples.** The topology decision, the property checklist, the
provider-pinning rule, the rules for designing before a technology exists, and the second-level
cache decision are stated here in full and nowhere else.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Three topologies](#three-topologies)
2. [The property checklist](#the-property-checklist)
3. [What each family answers](#what-each-family-answers)
4. [Pin the provider](#pin-the-provider)
5. [Designing before the technology exists](#designing-before-the-technology-exists)
6. [Changing technology later](#changing-technology-later)
7. [The Hibernate second-level cache](#the-hibernate-second-level-cache)

## Three topologies

"We use a cache" describes three different systems with different failure modes. The profile records
which, and it is a separate decision from the technology — several technologies can serve more than
one of these.

| | Local | Distributed | Near |
| --- | --- | --- | --- |
| Where entries live | In each instance's heap | In a separate store | Both: a local copy in front of a shared store |
| Read cost | A map lookup | A network round trip plus deserialization | A map lookup on a hit, otherwise the distributed cost |
| Shared between instances | No | Yes | Partly, and that is the difficulty |
| Survives a restart | No | Depends on the store's persistence | No, for the local half |
| Values copied or shared | **Shared by reference** | Copied, through serialization | Shared locally, copied remotely |
| Invalidation across instances | Not possible without broadcasting | Automatic | Needs the store's broadcast, and needs verifying |
| Memory | Multiplied by the instance count | Once, elsewhere | Both |
| Fails by | Not being there after a restart | Being unreachable | Either, independently |

- **Local is the right default for small, slowly changing, non-sensitive data** with a staleness budget at least as long as its TTL. It is the fastest and the simplest, and its one real limit — each instance is stale independently for up to the TTL — is a number the budget already approved.
- **Distributed earns its network hop** when entries are large, expensive to compute, must be consistent across instances, or must survive a restart. Note what the hop costs: a distributed cache in front of a fast indexed primary-key lookup is frequently *slower* than the query it replaces. Measure that specific comparison before assuming a cache is an improvement.
- **Near is the hardest**, because the local half reintroduces per-instance staleness on top of a shared store that looks authoritative. Choose it only when a measurement shows the network hop is the bottleneck, and only after establishing that the store's invalidation broadcast actually reaches the local copies.

**A near-cache can appear without being chosen.** Some client libraries keep a local copy by
default. Verify the configured client rather than trusting the profile row: if a local copy exists,
the near column applies whatever the row says.

## The property checklist

**This is the whole of what the technology decision changes here.** Fill it in when the technology
is selected, record it in the profile beside the technology row, and re-read the rules each answer
governs. An unanswered row is a rule applied on an assumption.

| Property | Why it decides a rule | Governs |
| --- | --- | --- |
| Are values copied or shared by reference? | A shared reference means a caller that mutates a cached value corrupts it for everyone | The immutability rule below |
| Is a TTL supported per entry, per cache, or not at all? | A design needing per-entry expiry cannot be built on a store that has neither | [TTL](cache-design.md#ttl) |
| Is there a server-side size bound, and what happens at the bound? | Evict, or refuse writes — and refusing writes turns a full cache into failing writes | [Size and eviction](cache-design.md#size-and-eviction) |
| Is a write on one instance visible to another? | Decides whether invalidation needs broadcasting | [More than one instance](invalidation-and-consistency.md#more-than-one-instance) |
| Does the store survive an application restart? A store restart? | Decides how often the cold-start load actually happens | [Warmup and cold start](cache-design.md#warmup-and-cold-start) |
| Is there atomic compute-if-absent across instances? | Decides whether cross-instance single-flight is available without a separate lock | [Stampede](cache-design.md#stampede) |
| What is thrown when the store is unreachable, and after how long? | Decides what the error handler catches and what timeout bounds it | The fail-open rules in `../SKILL.md` |
| What serializes the value, and is it configurable? | Decides the compatibility contract and whether Java serialization is even reachable | [Changing the shape](invalidation-and-consistency.md#changing-the-shape-of-a-cached-value) |

**The reference-versus-copy row is the one that silently changes correctness**, so it earns its own
rule: **cache immutable values only.** A record with no mutable components, or a defensive copy on
the way in. On a store that serializes, mutating a returned value is harmless because every caller
gets its own copy; on a local cache it corrupts the entry for every later caller. The same code is
correct on one technology and wrong on the other, and no test that runs against one will find the
other. Caching immutable values makes the row irrelevant, which is why it is the rule rather than
"be careful with local caches".

## What each family answers

Product-level behavior is verified, not assumed — the checklist above is filled from the chosen
technology's documentation and confirmed by a test. What can be said safely is what each *family*
does, and it is enough to eliminate most candidates early.

| Family | Examples | Copies values | Shared | Survives restart | Notes that change a design |
| --- | --- | --- | --- | --- | --- |
| **Plain in-process map** | Spring's `ConcurrentMapCacheManager` | No | No | No | **Unbounded and no expiry at all.** See the warning below |
| **In-process cache library** | Caffeine and equivalents | No | No | No | Bounded by size or weight, expiry after write or access, often refresh-after-write and per-entry expiry |
| **Remote key-value store** | Redis, Valkey, Memcached | Yes | Yes | Depends on persistence configuration | Network hop and serialization on every operation; a real availability dependency |
| **Distributed data grid** | Hazelcast, Infinispan | Yes, between members | Yes | Depends on configuration | Runs embedded in the application or client-server, and the answers differ between those two modes — record which |

**The plain in-process map is the trap in this table.** It is what Spring uses when caching is
enabled and no provider is on the classpath, so a project that adds the caching starter and
`@EnableCaching` and nothing else gets a cache that never expires anything and grows without bound.
It looks like a working cache in every test and is a memory leak in production. It is acceptable in
a test context and nowhere else.

Two more consequences worth knowing before a shortlist is drawn:

- **Not every store has first-class Spring support.** Some need a third-party adapter, which is a dependency with its own maintenance status — `build-and-dependencies` owns that judgement, and it belongs in the selection rather than after it.
- **An embedded grid puts a clustered component inside the application process.** It shares the heap, the JVM lifecycle, and the shutdown sequence with the application, and it changes what a rolling deploy does. That is an architecture decision, not a library choice.

## Pin the provider

```yaml
spring:
  cache:
    type: caffeine
```

Spring Boot picks a cache provider by scanning the classpath in a documented order. That means **a
transitive dependency can change which provider is active**, with no change to the application's own
declarations, and the symptom is not an error — it is a different set of properties silently taking
effect and a different one silently ignored.

- **Set `spring.cache.type` explicitly**, to the technology the profile records, from the first commit. It is one line, and it converts a silent provider swap into a startup failure.
- **Verify the active provider at startup rather than inferring it**, on the first setup and after any dependency change. `build-and-dependencies` states the general form of this rule for Spring Boot 4, where a missing module makes a feature inert; here the failure is the mirror image, an *unexpected* provider that works well enough to hide itself.
- **`spring.cache.type: none` is a legitimate value.** It disables caching while leaving the annotations in place, which is the right way to switch a cache off for an environment or a test — not deleting the annotations, and not an empty cache manager somebody wrote by hand.

## Designing before the technology exists

This is the normal case early in a project, and it does not block cache design. **Assume the
unfavourable answer to every row of the checklist**, and the design survives whichever technology
arrives:

- Assume values are **shared by reference** — so cache immutable values, which is the rule anyway.
- Assume there is **no cross-instance invalidation** — so give every entry a TTL inside the staleness budget, which is the rule anyway.
- Assume the store **can be unreachable** — so install the error handler and the timeout, which is the rule anyway.
- Assume **no per-entry TTL** — so group values with the same budget into the same cache, rather than relying on per-entry expiry.
- Assume **no cross-instance single-flight** — so accept one load per instance, or record that a distributed lock will be needed.

Note what that list demonstrates: every one of those assumptions leads to a rule this skill states
unconditionally. **A cache designed correctly needs the technology decision for its configuration,
not for its design** — which is why `Cache technology` can stay `UNDECIDED` while `Cache used` is
`no`, and why the design work is not blocked once it becomes `yes`.

What genuinely cannot be decided in advance is the configuration: the coordinate, the serializer,
the connection settings, the server-side eviction policy, and the operational runbook. Leave those
as the deferred decision they are.

## Changing technology later

Moving from one cache technology to another changes no call site, and that is exactly why it needs a
plan rather than a dependency swap.

1. **Re-fill the property checklist for the new technology** and re-read every rule an answer governs. A move from a serializing store to a local cache silently introduces the shared-reference problem; a move the other way silently introduces serialization of types that were never serializable.
2. **Re-derive the topology.** Distributed to local means every instance is now independently stale and no eviction crosses instances; the register's invalidation column becomes wrong for every cache at once.
3. **Treat the entries as disposable.** Do not migrate cache contents. Start the new store empty and let it fill — but check the cold-start question first, because this is a cold start on every instance at once.
4. **Change one cache at a time where the abstraction allows two managers to coexist**, so a problem identifies its cache.

## The Hibernate second-level cache

**The second-level cache and the query cache are caches**, and every rule in this skill applies to
them: they are gated on the profile row, they need a staleness budget, they need a register entry,
and they need the five tests. They are singled out here because they are enabled by a property that
reads like tuning, and because their failure mode is silence.

`spring-data-jpa` owns the mapping side — which entities and collections are cached, the region
configuration, and the provider settings. This skill owns the two decisions that decide whether it
is correct at all.

**First: the concurrency strategy is a correctness decision, not a performance one.** Each one
answers "what happens when this data is written while cached", and choosing the wrong one produces
wrong reads rather than slow ones.

| Strategy | Fits | Consequence |
| --- | --- | --- |
| Read-only | Data that is never updated after insert — reference tables, historical rows | An update throws. That is the point: it makes the assumption enforced rather than assumed |
| Read-write | Mutable data that must not be read stale within a transaction | Uses soft locks around the write; the safe general choice, and the more expensive one |
| Nonstrict read-write | Data that changes rarely and tolerates a brief stale read after a write | There is a window where a reader sees the old value. Only acceptable when the staleness budget says so, in writing |
| Transactional | Data requiring full transactional cache semantics | Needs a provider and a transaction manager that support it; verify before choosing, and do not choose it by default |

**Second: the query cache is a separate decision and usually the wrong one.** It caches identifiers,
not entities, so it is useless without the entity cache behind it; and it is invalidated by *any*
write to the tables a query touches, so on anything but near-static data it is invalidated faster
than it is used — paying the cost of maintaining it for a hit ratio near zero. Enable it only for a
specific query over data that is effectively static, and record the measurement.

Three more rules, each of which has surprised somebody:

- **Bulk DML bypasses it**, exactly as `spring-data-jpa` states bulk DML bypasses the optimistic version check. A `@Modifying` statement leaves the second-level cache holding the pre-update state, and the provider does not know. Either avoid caching entities the project updates in bulk, or evict the affected region explicitly after the statement.
- **An entity in the second-level cache is shared across sessions.** The immutability rule is not optional here: the provider stores a disassembled form, but a mutable component reached through it is reachable by every session that loads the entry.
- **Enabling it changes what a query-count test observes**, so the persistence tests `spring-data-jpa` requires have to be read with it in mind — a query that disappears from the count because it was served from the cache is not the same evidence as a query that was never needed.
