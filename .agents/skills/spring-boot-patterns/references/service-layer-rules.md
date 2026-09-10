# Service layer rules

Use this reference whenever a service is created or changed, whenever deciding which of the two
service levels an operation belongs to, and whenever an operation produces an effect outside its own
transaction. Apply every rule from `../SKILL.md`.

The two-level split is not ceremony. It exists so that no service depends on another service at the
same level, which is what keeps transaction boundaries findable and prevents cyclic service graphs.
Code examples for everything here are in
[service and domain examples](service-domain-examples.md).

## Contents

1. [Aggregate services](#aggregate-services)
2. [Application services](#application-services)
3. [External effects and what after-commit delivery does not buy](#external-effects-and-what-after-commit-delivery-does-not-buy)
4. [Rules for both levels](#rules-for-both-levels)
   - [The explicit write, by entity state](#the-explicit-write-by-entity-state)
5. [Service parameter objects](#service-parameter-objects)

## Aggregate services

One `<Aggregate>Service` per aggregate root, in the `service` package.

- Determine the aggregate by lifecycle ownership, not by table count and not by the reference graph. A row belongs to the aggregate when it cannot exist without the root and the root is what creates and deletes it. A row that has its own lifecycle, or that other features reference directly by its own identifier, is a separate aggregate.
- The service holds every repository of its aggregate and that aggregate's domain mapper. It holds no repository of another aggregate and no other service.
- It owns the invariants of its aggregate. Do not reduce it to a pass-through over the repository: a rule about the aggregate's own state belongs here, not in the caller.
- It reads and writes only its own aggregate. It refers to another aggregate by identifier and receives any value it needs from that aggregate as an explicit parameter.
- Annotate it `@Transactional` with the default propagation, so it joins the use case's transaction when one is open and opens its own when it is called without one. Its multi-repository writes are then atomic either way. Do not use `MANDATORY`: refusing to run without a caller-supplied transaction blocks legitimate direct use from a job or a migration task, and the layering rule below is what keeps the use-case boundary where it belongs.

## Application services

One `<Capability>ApplicationService` per coherent use case group, in the `applicationservice`
package. It exists only where coordination exists. A feature whose every operation stays inside one
aggregate needs no application service at all, and adding an empty one is scaffolding.

- It owns the use case's transaction boundary **when it exists**: annotate it `@Transactional`, so the transaction starts and ends with this method, the aggregate services it calls join it, and rollback is decided here. When a use case has no application service because it never leaves one aggregate, the boundary is that aggregate service and nothing needs to change: both levels use the default propagation precisely so either arrangement is correct. Never introduce an application service only to relocate a transaction.
- It depends only on aggregate services, ports, and adapters — never on a repository, an entity, or another application service. A repository dependency here means the aggregate service was bypassed and the aggregate now has two write paths.
- It coordinates: fetch from one aggregate service, pass explicit values to another, decide the order. Rules that belong to a single aggregate stay in that aggregate's service.
- Never add a method that only forwards to one aggregate service. A pass-through adds a second name for one operation and a second place to keep in sync, and it is the mechanism by which this class turns into a facade over the whole application. An entry point that needs a single-aggregate operation calls that aggregate service directly.
- A method that adds a failure policy is not a pass-through. Deciding how often an operation is retried, how long it may take in total, and what a caller sees when it gives up is use-case behavior, and it belongs here even when only one aggregate is involved.

## External effects and what after-commit delivery does not buy

- Publish domain events through `ApplicationEventPublisher` and consume them with `@TransactionalEventListener(phase = AFTER_COMMIT)`. Never call a notification, message broker, or other external effect directly inside the transaction: a rollback after that call leaves the outside world believing something happened.
- Know what that buys and what it does not. After-commit delivery guarantees the effect never fires for work that rolled back. It does **not** guarantee the effect happens at all: the commit has already succeeded, so a crash, a redeploy, or a failure inside the listener loses the effect permanently, with no retry and no record that anything was owed. Spring also does not propagate an exception thrown in an after-commit listener back to the caller, so a silent loss looks identical to success from the outside.
- Decide per effect, and record the mechanism in `docs/project-profile.md`. Losing the effect is acceptable for a cache refresh or a best-effort metric, and the listener alone is then the right answer. Where losing it is not acceptable — payment, provisioning, a notification a person acts on, a message another system consumes — write the intent to an outbox table inside the same transaction as the business change, and deliver it from a separate process that retries until acknowledged. The after-commit listener may still trigger the first attempt; it is an optimization, not the guarantee.
- Whatever the mechanism, make failed delivery visible. Log the failure inside the listener and expose it through `observability-and-logging`, because a listener that throws produces no HTTP error, no rollback, and no caller-side signal.
- Annotate a read use case `@Transactional(readOnly = true)`. `spring-data-jpa` explains why the attribute only has an effect at this level.

## Rules for both levels

- The service interface is optional and the convention is recorded in the project profile. Follow whichever it records, at both levels. With none recorded and no answer yet, apply the fallback the template records for that row, write it into the profile, and add an interface only for a concrete reason: a boundary another module crosses, more than one implementation, a port with a substitutable adapter, or a contract an external consumer implements. Wanting an `Impl` suffix, somewhere to put Javadoc, or a mockable type are not reasons — Mockito mocks a concrete class. Both shapes appear in [service and domain examples](service-domain-examples.md); do not mix them within a scope.
- When an interface exists, put caller-facing Javadoc and method-validation constraints on it, and `@Service`, `@Validated`, transactions, dependencies, and logic on the concrete class without duplicating the contract.
- Use Lombok constructor generation only when the profile records Lombok and the generated constructor remains obvious; otherwise write the constructor explicitly.
- Do not accept REST request/response TOs and do not return JPA entities.
- Return domain objects such as `UserDomain`; map entities to domain objects before crossing the service boundary.
- Apply entity mutations through the accessor style recorded in `docs/project-profile.md`. The examples use fluent setters that return the entity; plain `void` setters are equally acceptable when the profile records that choice. Use one style across the project.
- For update operations, load the entity inside the write transaction, apply explicit business or persistence mutations, call repository `save` exactly once, and map the returned saved entity to a domain object. Use `saveAndFlush` only when subsequent logic must observe immediate database synchronization for a documented reason. Do not use a MapStruct `@MappingTarget` method to mutate an existing entity.

### The explicit write, by entity state

**This project requires the explicit repository write even where JPA dirty checking would persist a
managed entity, and that is house style rather than correctness** — the strength
[`_core/RULES.md`](../../_core/RULES.md#how-strong-each-rule-is) defines. Dirty checking would produce
the same row. What it would not produce is a line in the diff: with an explicit `save`, the write is
visible at the point the decision was made, a reviewer can see which method persists and which only
reads, and a method that stopped writing shows up as a deleted line rather than as silence. That is
worth the call, and it is why the rule stands — but state it as the reason, because a reader told it
is a correctness requirement will eventually find out it is not, and then discount the rules around
it too.

Apply it per entity state; the four cases are not the same operation:

| State of the entity | What the service does | Why |
| --- | --- | --- |
| **New** | `save` | There is no other way to persist it |
| **Managed**, loaded in this transaction | Mutate, then `save` once | The house rule above. The provider would flush it anyway, so `save` adds visibility, not persistence |
| **Detached**, rebuilt from client input | Do not save it as an update at all | Every field the client did not send is written too, so an omitted field becomes a silent overwrite. `spring-data-jpa` states this and the alternative |
| **Bulk**, many rows by one statement | Neither — use the bulk path | `spring-data-jpa` owns it, including that bulk DML bypasses the optimistic version check |

One consequence is worth naming because it is invisible at the call site: **`save` on a managed
instance is a `merge`, so it follows the association's cascade settings.** The `UserEntity` that
`spring-data-jpa` declares maps its child collection with `PERSIST` and `MERGE`, so saving the root
reaches the children. That is intended for an aggregate — the root is the only write path into it —
but it means `save` is not the no-op it looks like on a graph, and a cascade added to a mapping
changes what every existing `save` call does. `spring-data-jpa` owns cascade design; this rule only
requires that the call be there.

## Service parameter objects

- **`modern-java-21` owns the signature-size rule** — how many declared parameters are acceptable, when the count becomes a design warning, and what a legitimate grouping is. Read it there; it is not repeated here, and a change to it must not have to be made twice.
- What this skill adds is where the resulting object lives and what it may hold. Keep the target
  resource's identifier a separate parameter and group the rest, so the operation still reads as
  "act on this resource, with these values".
- Place a service parameter object at the domain/service boundary, name it for the operation or
  values it represents, and keep it independent of REST and JPA. Do not introduce `Command` or
  `View` terminology by default.
- Do not pass raw passwords, tokens, or secrets beyond the narrow boundary that hashes, encrypts, or exchanges them. Never persist or log their raw values.
- Keep business rules out of controller, mapper, repository, and entity callback code.
- Never rely on self-invocation for `@Transactional`, `@Async`, `@Cacheable`, or method validation. When a separate boundary is genuinely required, move it to another bean rather than working around the proxy.
- Keep transactions short. Do not make slow external calls while holding one unless the consistency design requires it.
- This skill decides only which method is transactional and which use cases are reads. `spring-data-jpa` owns what those settings mean and when they apply: `readOnly` semantics, propagation, isolation, flush timing, and locking. Read them there before overriding a default anywhere.

Read the service, parameter-object, mapper, and repository-boundary examples in
[service and domain examples](service-domain-examples.md).
