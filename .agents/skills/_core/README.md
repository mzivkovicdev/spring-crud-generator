# `_core`

Two shared files that the eleven skills link to instead of duplicating.

| File | What it is | Authority |
| --- | --- | --- |
| [`RULES.md`](RULES.md) | A one-page card of the non-negotiable rules, each naming its owner | **Summary only.** When it disagrees with an owner skill, the owner wins and this card is the defect to fix |
| [`OWNERSHIP.md`](OWNERSHIP.md) | The canonical ownership map, the split-topic seams, and the conflict precedence order | **Authoritative.** When a skill's local seam table disagrees with it, this file wins |

`_core` is not a skill. It has no frontmatter and nothing triggers it. It is reference material the
skills point at, and a place for an agent with a small instruction budget to load the essentials
from.

Neither file replaces reading the owner skill before implementing something it covers. `RULES.md`
states *that* a rule exists; the owner skill states its scope, its exceptions, and what to do at the
edges — and the edges are where the expensive mistakes live.

## Known gaps in this skill set

Three topics have no owner. They are listed here so an absent rule stays a visible decision rather
than looking like a settled one.

- **Caching.** `docs/project-profile.md` records whether a cache exists and which technology it uses,
  and `application-security` owns what may be cached and under what conditions. Nothing owns cache
  design: key format, TTL, invalidation, eviction ordering relative to a transaction, or serialization.
  While `Cache used` is `no` or `UNDECIDED`, do not introduce a cache, a cache annotation, or a cache
  dependency.
- **Asynchronous messaging.** The profile offers an outbox table and a broker-native transaction as
  reliable-delivery mechanisms, but no reference describes implementing either, and nothing covers the
  consumer side: idempotent consumption, dead-letter handling, poison messages, or ordering. Treat a
  project that needs one of those as needing a rule that does not exist yet.
- **Code generation.** Generated output must pass the same quality gates as hand-written code, and a
  defect in it is fixed in the template rather than the output. No skill owns generator or template
  design beyond that.
