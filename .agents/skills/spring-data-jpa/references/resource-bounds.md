# Resource bounds

Use this file when configuring or reviewing how long a statement, a transaction, or a wait for a
connection may last, and how the pool is sized. Apply every rule from `../SKILL.md`.

**This file carries rules, not only examples.** The three levels a bound can be applied at, the
delivery channel each engine offers, the rule that one channel carries every connection-level
setting, the migration exception, and the tests that prove a bound is real are stated here in full
and nowhere else. `../SKILL.md#resource-budgets` states the decisions and routes here for the
mechanism.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

The numbers in every snippet are illustrative. `docs/project-profile.md` records them under
**Performance and capacity**; read them from there, and never copy a number out of this file into a
project.

## Contents

1. [Three levels, and which one is the bound](#three-levels-and-which-one-is-the-bound)
2. [One connection, several settings](#one-connection-several-settings)
3. [Delivering the settings, by engine](#delivering-the-settings-by-engine)
4. [Migrations must not inherit the application's bounds](#migrations-must-not-inherit-the-applications-bounds)
5. [Transaction timeout](#transaction-timeout)
6. [Pool size and the wait for a connection](#pool-size-and-the-wait-for-a-connection)
7. [Proving a bound is real](#proving-a-bound-is-real)
8. [Rejected shapes](#rejected-shapes)

## Three levels, and which one is the bound

A wait can be bounded in three places, and they are not interchangeable. Getting this wrong is what
produces a configuration that reads as bounded and is not.

| Level | Mechanism | Covers | Enforced by |
| --- | --- | --- | --- |
| **Connection** | An engine setting applied to the session | **Every** statement on that connection: queries, flushes, native SQL, whatever a library issues | The database server |
| **Transaction** | Spring's transaction timeout | The whole unit of work, including the gaps between statements | The application, and — where the provider propagates it — each statement's remaining budget |
| **Query** | The `jakarta.persistence.query.timeout` hint on one query | That one query | The JDBC driver, through `Statement.setQueryTimeout` |

**The connection level is the bound; the other two are refinements.** Only the engine setting covers
statements the application never sees — a flush the provider decided to issue, a native query in a
library, a lock wait inside an `UPDATE`. Set it first, then use the transaction timeout to bound the
unit and the query hint only where one query legitimately needs a different number.

Two properties of the query hint decide why it is not the primary mechanism, and both are easy to
discover too late:

- **It is expressed in milliseconds and enforced in seconds.** `Statement.setQueryTimeout` takes whole seconds, so a sub-second value either rounds or is lost. A "500 ms" query timeout is not a thing the JDBC API can express.
- **It is implemented by the driver, not the server.** On several drivers — PostgreSQL among them — the driver starts a timer and, when it fires, opens a *second* connection to issue a cancel request. Under the pool exhaustion these bounds exist to prevent, that second connection is exactly what is unavailable.

Use the hint deliberately, for a report query that may legitimately run longer than the
connection-level ceiling, and record that the value is now compile-time. Do not use it as the
project's statement timeout.

## One connection, several settings

**A pooled connection carries more than one engine setting, and they share one delivery channel.**
The statement timeout is one, the pessimistic lock timeout that
[lock timeouts](locking-and-retry-examples.md#lock-timeouts) requires is another, and an
idle-in-transaction bound is a third where the engine has one. They are three separate decisions
with three separate rows in the profile, and they arrive over the same wire.

That matters because `spring.datasource.hikari.connection-init-sql` is **one string per pool**. A
project that delivers the lock timeout through it and later adds a statement timeout the same way
either overwrites the first setting or concatenates statements into a value whose behaviour depends
on the driver's multi-statement handling. Both failures are silent: the pool starts, the
application serves, and one of the two bounds is simply absent.

**Prefer a channel that takes several settings as data.** Where the driver offers connection
properties that the server applies at session start, declare each setting as its own property and
leave `connection-init-sql` unused. Where it does not, `connection-init-sql` is correct — but then it
is the single place every connection-level setting is declared, and adding one means editing that
string rather than adding a second mechanism beside it.

State in the change which channel the project uses. A project with settings arriving through two
channels has no single place to read the effective bounds, which is the condition this rule exists
to prevent.

## Delivering the settings, by engine

Confirm the mechanism against the engine and driver version `docs/project-profile.md` records
before relying on any row. **A setting the server silently ignores throws nothing**, which is the
same failure mode [lock timeouts](locking-and-retry-examples.md#lock-timeouts) describes for the JPA
lock hint.

| Engine | Statement bound | Idle-in-transaction bound | How the settings reach the session |
| --- | --- | --- | --- |
| PostgreSQL | `statement_timeout`, milliseconds | `idle_in_transaction_session_timeout`, milliseconds | The driver's `options` connection property, as `-c name=value` pairs; or a connection-init statement |
| MySQL / InnoDB | `max_execution_time`, milliseconds, and **read-only `SELECT` statements only** | none equivalent; use the transaction timeout and `wait_timeout` | The driver's `sessionVariables` connection property; or a connection-init statement |
| SQL Server | The driver's own query timeout property, seconds | none equivalent | Driver connection properties |
| Oracle | No session-level statement timeout. The bound is the query hint, or Resource Manager | none equivalent | The query hint, or database-side Resource Manager policy |

Three consequences follow, and each is a rule rather than a note:

- **MySQL's bound does not cover writes.** `max_execution_time` applies to read-only `SELECT` statements, so a long `UPDATE` is bounded only by the transaction timeout and the lock timeout. Record that gap rather than assuming the row is equivalent to PostgreSQL's.
- **Oracle has no connection-level equivalent**, so on Oracle the query hint stops being a refinement and becomes the mechanism — with the second-granularity limit above. Where that is not good enough, the bound belongs in Resource Manager, which is a database-administration decision and not this skill's.
- **An engine not in this table is verified, not assumed.** Add its row to the project's own documentation when you establish it.

PostgreSQL, through driver properties — every connection-level setting in one place, and
`connection-init-sql` left free:

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        # Values come from docs/project-profile.md. Every statement on this connection is
        # cancelled past statement_timeout; a transaction left open past
        # idle_in_transaction_session_timeout has its session terminated, which is what stops
        # an abandoned request from holding a connection indefinitely.
        options: "-c statement_timeout=5000 -c lock_timeout=3000 -c idle_in_transaction_session_timeout=10000"
```

MySQL, the same three decisions where the engine has them:

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        sessionVariables: "max_execution_time=5000,innodb_lock_wait_timeout=3"
```

Note the unit change between the two: `innodb_lock_wait_timeout` is in **whole seconds**, while
every PostgreSQL value above is in milliseconds. A lock timeout recorded as `3s` in the profile
therefore renders differently per engine, which is one more reason the value is read from the
profile and converted at the point of use rather than retyped.

## Migrations must not inherit the application's bounds

A statement timeout sized for a request will cancel a schema change on a large table, and an
idle-in-transaction bound will terminate a long migration mid-flight. Both failures happen at
deploy time, on the environment that has the most data.

`sql-database-migration` requires the tool to run at startup against every environment, and by
default it runs on the application's own datasource — so by default it inherits every setting above.
Resolve it deliberately, and record which was chosen:

- **Give the migration tool its own datasource**, configured with the migration credentials the profile records and without the request-sized bounds. This is the shape to prefer: the two workloads have genuinely different budgets, and separating them also separates the pools.
- **Or scope the application's bounds to the transactions that need them**, per-transaction rather than per-connection, and leave the pool's connections unbounded at the session level. That costs a statement per transaction and reintroduces the risk that a call site forgets, which is why it is the second choice.

Never resolve it by raising the application's statement timeout until migrations fit. That removes
the bound from every request to accommodate an operation that runs once.

## Transaction timeout

The connection-level bound limits each statement; it does not limit a transaction that issues many
short ones, or one that sits between statements while the application does something slow. That is
what the transaction timeout is for, and Spring applies it declaratively.

```yaml
spring:
  transaction:
    default-timeout: 10s
```

```java
@Transactional(timeout = 3)
public void reserve(final String sku, final int quantity) {
    // A tighter bound than the project default, for a use case that holds a lock.
}
```

Rules:

- **Set the project-wide default rather than annotating every method.** A default that applies everywhere is a bound nobody can forget; the per-method attribute is for the use cases that need a tighter one, and `@Transactional(timeout = ...)` is in whole seconds.
- **Keep it at or below the request budget `spring-boot-patterns` records, and never below the statement timeout.** A transaction timeout under the statement timeout means the transaction gives up while a statement it started is still allowed to run.
- **Where the provider propagates the remaining transaction time to each statement, this bound also caps statements** — which is useful, and is not a reason to skip the connection-level setting, because it covers only statements issued inside a Spring-managed transaction. Verify the propagation on the configured provider rather than assuming it; the connection-level bound is what holds when it does not.
- **A read use case needs one too.** `readOnly = true` is an optimization hint, not a bound, and an unbounded read holds its connection exactly as long as an unbounded write.

## Pool size and the wait for a connection

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      connection-timeout: 2s
      max-lifetime: 30m
```

- **`maximum-pool-size` × instance count must fit inside what the engine accepts**, with headroom for migrations, operators, and whatever else connects. `../SKILL.md` states why a larger pool is not faster; this is where the number is applied.
- **`connection-timeout` is the wait for a pooled connection, and it is short on purpose.** It is the property that turns pool exhaustion into a fast failure the caller and the metrics both see, instead of a stalled request nobody times out. Keep it well inside the request budget.
- **`max-lifetime` retires a connection before something in the network path does.** Set it below the shortest idle timeout on the path — the engine's own, a proxy's, a firewall's — or the pool eventually hands out a connection the other end has already closed. This is the one setting here that is about the infrastructure rather than about the application's own budget.
- **The concurrency model in front of the pool decides how much demand it sees.** `spring-boot-patterns` owns that decision in [runtime and request budget](../../spring-boot-patterns/references/runtime-and-request-budget.md); this skill owns only the pool that absorbs it. Re-derive the pool size whenever that model changes, because a model that removes the thread-count ceiling makes this pool the only limit left.
- **`REQUIRES_NEW` doubles the demand** for the duration of the inner transaction, as `../SKILL.md` states. Account for it here or do not use it.

## Proving a bound is real

Every rule in this file fails silently when it is wrong, so each one is proven by executing it, never
by reading the configuration.

- **Statement timeout:** run a statement the engine will not finish in time — a deliberate sleep function, or a query over a generated series — and assert that it fails within the configured bound. Assert the *bound*, not merely that it failed, because a test that waits for the driver's own default passes for the wrong reason.
- **Idle-in-transaction bound, where the engine has one:** open a transaction, do nothing for longer than the bound, and assert the next statement fails.
- **Transaction timeout:** drive a transaction past the bound and assert the caller receives the failure rather than the result.
- **Connection wait:** hold every pooled connection, request one more, and assert it fails within `connection-timeout` rather than blocking until the test framework gives up.
- **Migration separation:** the clean-install run `sql-database-migration` requires must apply the full history without hitting the application's statement timeout. A history that only passes on an empty database is not evidence.

Each of these belongs at the level `spring-boot-testing` places it: against the real engine, in the
integration suite, never against a substitute whose defaults differ.

## Rejected shapes

```yaml
# Wrong: the lock timeout was here first and this replaces it. One string, one setting, and the
# loss is silent - the pool starts and the lock wait is unbounded again.
spring:
  datasource:
    hikari:
      connection-init-sql: "set statement_timeout = '5000ms'"
```

```yaml
# Wrong: a second channel beside the first. The effective bounds now depend on which mechanism the
# driver applies last, and no single place states them.
spring:
  datasource:
    hikari:
      connection-init-sql: "set lock_timeout = '3000ms'"
      data-source-properties:
        options: "-c statement_timeout=5000"
```

```yaml
# Wrong: a statement timeout raised until the migration fits. Every request now waits three
# minutes for a database that has stopped responding.
spring:
  datasource:
    hikari:
      data-source-properties:
        options: "-c statement_timeout=180000"
```

```java
// Wrong: the project's statement timeout expressed as a per-query hint. It reaches only queries
// the application issues through the provider, it rounds to whole seconds, and on several drivers
// it needs a spare connection to cancel - the one thing an exhausted pool does not have.
@QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
List<OrderEntity> findByStatus(final OrderStatus status);
```

```java
// Wrong: a read left unbounded because it is a read. readOnly is a hint to the provider, not a
// limit on anything, and this holds its connection for as long as the query runs.
@Transactional(readOnly = true)
public List<ReportRowDomain> buildAnnualReport() {
    return this.reportRepository.everything();
}
```
