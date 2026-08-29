# `_core`

Two shared files that the twelve skills link to instead of duplicating.

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

Three topics are incompletely owned. They are listed here so an absent rule stays a visible decision
rather than looking like a settled one.

A gap here means **no skill states how to build the thing**. It does not mean the topic is
unregulated: a topic can be fully covered for security, naming, and testing while nothing describes
the mechanism. Each entry below names what is owned before it names what is missing, because
treating an owned rule as absent is how a project ends up with a second, weaker version of it.

- **Caching.** `docs/project-profile.md` records whether a cache exists and which technology it uses,
  and `application-security` owns what may be cached and under what conditions. Nothing owns cache
  design: key format, TTL, invalidation, eviction ordering relative to a transaction, or serialization.
  While `Cache used` is `no` or `UNDECIDED`, do not introduce a cache, a cache annotation, or a cache
  dependency.

- **Asynchronous messaging.** Much of this is owned, and the owned parts are not gaps:

  | Already owned | Owner |
  | --- | --- |
  | Whether an effect may use an after-commit listener at all, and what that guarantees | `spring-boot-patterns` |
  | Where a listener sits in the layers — a thin boundary that delegates, like a controller | `spring-boot-patterns` |
  | That a listener's failure is handled and logged at its own boundary, not through the REST advice | `spring-boot-patterns`, `observability-and-logging` |
  | Treating every message as untrusted: publisher and consumer authorization, envelope and schema validation, tenant derived from verified claims, payload minimization | `application-security` |
  | That duplicate, delayed, reordered, and replayed delivery must be assumed unless the platform proves otherwise, and that consumer effects must be idempotent | `application-security` |
  | Dead-letter access, retention, audit, and the rule against blind redrive | `application-security` |
  | Event type, publisher, consumer, destination property, and message key names | `project-naming-conventions` |
  | That an outbox row commits with the business change, and that a rolled-back use case leaves none | `spring-boot-testing` |
  | Counters for retry, fallback, and idempotency-replay paths | `observability-and-logging` |

  What is genuinely missing is the **mechanism**, and only the mechanism:

  - the outbox implementation — table shape, the relay that drains it, its ordering and at-least-once behaviour, and retention of delivered rows;
  - the broker-native transaction implementation, where the profile records one;
  - consumer wiring — which listener abstraction, how retry and dead-lettering are configured, and how poison messages are separated from transient failures;
  - the deduplication store behind idempotent consumption, which is the same claim-store problem `spring-boot-patterns` already describes for HTTP idempotency, applied to a different entry point;
  - partitioning and ordering design, where ordering is part of the contract.

  A project that needs one of those needs a rule that does not exist yet. **While `Message broker` is
  `none` or `UNDECIDED` in the profile, do not introduce a broker, a listener, or a messaging
  dependency** — the same guard the caching entry above applies, for the same reason.

- **Code generation.** Generated output must pass the same quality gates as hand-written code, and a
  defect in it is fixed in the template rather than the output. No skill owns generator or template
  design beyond that.
