---
name: sql-database-migration
description: Relational schema migration ownership for Spring Boot applications using Flyway or Liquibase. Use whenever a change adds, alters, or removes a table, column, constraint, index, sequence, view, or routine; whenever a migration file is created, edited, ordered, or reviewed; when backfilling data, seeding reference data, resolving a migration conflict between branches, or verifying migrations in CI. Covers both tools and both Spring Boot generations. Excludes NoSQL data stores.
---

# SQL database migration

Every change to a relational schema is a migration file, reviewed like code and executed by the
tool, in every environment including a developer laptop.

This skill covers relational databases only. A NoSQL store has no linear version history, no
checksum over an applied script, and a different expand-and-contract shape; do not stretch these
rules to cover one.

## Coordination with other skills

| Owner | Owns |
| --- | --- |
| `spring-data-jpa` | Entity mappings, associations, queries, locking, and what the schema has to look like for them to work |
| `project-naming-conventions` | The form of every name: migration identifiers, tables, columns, constraints, indexes, sequences |
| `build-and-dependencies` | Declaring the dependency and the plugin in the build file, and the version chosen for each |
| `spring-boot-testing` | Test levels, fixtures, isolation, and how suites execute |
| `observability-and-logging` | Whether a startup gate appears in readiness, and what startup emits |
| `application-security` | Credentials for the migration connection, and data classification of anything a migration touches |
| `spring-boot-code-review` | Review scope, evidence, severity, and reporting |

This skill states what a migration must contain and when it must exist. It does not name files, does
not declare dependencies, and does not define test levels.

## The migration tool is a recorded decision

`docs/project-profile.md` records Flyway or Liquibase. Both are correct choices and nothing in this
skill prefers one.

- Read the profile first. When it records neither and the repository contains no migration directory and no migration dependency, **ask the user which tool the project will use.** This is an `ASK` decision in the token vocabulary `spring-boot-patterns` defines, so it blocks: nothing in the repository decides it, so do not pick one, and do not infer a tool from an unrelated dependency.
- When the repository already contains a migration directory or an applied history table, that is the answer, whatever the profile says. Correct the profile rather than the repository.
- Never run two migration tools against one schema. Each keeps its own history table and neither describes the full schema, so a clean install reproduces something no environment has. Converting from one tool to the other is a project of its own, never a side effect of a feature.

## Reference routing

- Read [Flyway configuration](references/flyway-configuration.md) when the project uses Flyway.
- Read [Liquibase configuration](references/liquibase-configuration.md) when the project uses Liquibase.

Read one. The rules in this file apply to both, and neither reference restates them.

## The migration tool owns the schema

- The migration history is the only source of truth for schema shape. Any structure the application depends on exists because a migration created it.
- Set Hibernate schema generation to `validate` or `none`. Never `create`, `create-drop`, or `update` against any shared environment, and never `update` at all: it silently diverges from the migration history and produces a schema no environment can reproduce.
- `validate` is useful precisely because it fails at startup when mappings and schema disagree. Treat that failure as a missing migration, not as a reason to loosen the setting.
- Do not create or alter schema objects from application code, from a data-loading script outside the tool, or by hand in a shared environment. A change applied by hand exists in one database and nowhere in the history.
- Entities and migrations change in the same commit. A mapping merged ahead of its migration breaks every environment that deploys it; a migration merged ahead of its mapping is dead weight nobody can review.

## Every migration is forward-only and immutable

- A migration that has been applied to any shared environment is frozen. Do not edit its content, rename it, renumber it, or delete it. The tool stores a checksum, and changing an applied file fails startup in every environment that already ran it.
- Correct a mistake with a new migration. This holds even when the mistake is one day old and even when the fix is trivial.
- Editing a migration is allowed only while it exists on a feature branch and has been applied nowhere but the author's own database. Once it merges, the previous rule applies.
- Do not write down-migrations, and do not rely on tool-generated rollback. Recovery from a bad schema change is a new forward migration plus, where data was destroyed, a restore. A rollback script that has never been executed is not a recovery plan.

## Ordering and branch conflicts

Two developers on two branches will pick the same version number. That is a routine event, not an
incident, and the project needs one rule for it.

- Version identifiers are strictly increasing and never reused. `project-naming-conventions` owns their form.
- The branch that merges second renumbers its migration to follow the merged one, then reruns its tests from an empty database. Never merge two migrations with the same identifier, and never resolve the conflict by editing the one that merged first.
- Prefer an identifier scheme that makes collisions rare, such as a UTC timestamp, over a sequential counter. Record the scheme once; do not mix schemes in one project.
- Migrations are applied in one order, so a migration may depend only on the state left by earlier ones. Never assume a migration from a parallel branch has run.
- Keep one migration per logical change. A file that adds a table, backfills it, and drops another is impossible to review, to reason about on failure, and to reorder.

## Writing a migration

- Write explicit SQL, or the tool's declarative format, for the target engine recorded in the profile. Do not generate a migration from the entity model and commit it unread: generated DDL carries defaults, name choices, and drops that nobody decided.
- Name every constraint and index explicitly rather than accepting a generated name. An unnamed constraint gets a different name in each environment, and a later migration cannot drop it.
- Make each migration idempotent where the tool does not guarantee it, and never depend on a partially applied migration being retried cleanly.
- Prefer one DDL statement per migration on engines where DDL is not transactional. On such an engine, a multi-statement migration that fails halfway leaves the schema in a state the history does not describe, and manual repair is the only way out.
- State `NOT NULL`, defaults, precision, scale, and length explicitly. A column added without them is a decision deferred to the engine.
- Adding a column with a default rewrites the table on some engines and versions. Check the behavior of the recorded engine and version before adding one to a large table.
- Do not put credentials, personal data, or environment-specific values in a migration. It is committed source, readable by everyone with repository access.

## Expand and contract

Any change that is not backward compatible with the currently deployed application is split into
phases across releases. This is not optional on a system that deploys without downtime, and it is
the rule that most often gets skipped because the single-step version works on a laptop.

1. **Expand.** Add the new structure alongside the old. New column nullable, new table empty, new constraint absent. Deploy. The running application ignores it.
2. **Migrate.** Write to both old and new from the application, and backfill existing rows. Deploy.
3. **Contract.** Stop reading the old structure, then in a later release drop it and add any constraint that could not exist while both were live.

- A rename is a drop plus an add. Never rename a column or table in one step on a live system: the previously deployed instance still references the old name for the length of the rollout.
- Narrowing a type, adding `NOT NULL` to a populated column, and adding a unique constraint are all breaking. Each needs the backfill to complete before the constraint arrives.
- Dropping anything requires evidence that nothing reads it: the current code, the previously deployed version, reports, downstream consumers, and any external integration. `project-naming-conventions` treats a database name consumed outside the deployable unit as a contract.
- Record which phase a migration belongs to in the change description. A contract-phase migration merged before its migrate phase has completed is a production incident, and the phase is the only thing that makes that visible in review.

## Data changes

- Separate a data backfill from the schema change that enables it. They fail differently, they take different amounts of time, and they need different recovery.
- Do not run an unbounded backfill inside a startup migration. It holds the deployment, it holds locks, and a timeout leaves it half applied. Batch it, make it resumable, and run it as a job or a separately triggered migration when the volume warrants.
- Before writing a backfill, state the row count, the expected duration, the locks taken, and what happens when it is interrupted. If those are unknown, that is the first task.
- A backfill is written to be safe to run twice.

## Seed and reference data

- Reference data the application cannot start or function without — lookup tables, enum rows, a default tenant, a system account — belongs in migrations, in the same history as the schema.
- Test fixtures, demo records, and developer convenience data do not. `spring-boot-testing` owns test data; a migration that inserts a fixture puts it into production.
- Make reference-data migrations idempotent and keyed by a stable business key, never by a generated identifier that differs per environment.
- A system account created by a migration carries no usable credential. `application-security` owns how its credential is provisioned.

## Wiring by Spring Boot generation

The migration files and every rule above are identical across generations. The dependency is not,
and getting it wrong fails silently.

- **On Spring Boot 3**, the third-party library on the classpath is enough. Auto-configuration for both tools lives in `spring-boot-autoconfigure`, which every application already has.
- **On Spring Boot 4**, it is not. Auto-configuration was split into per-technology modules, so the raw library gives you a working library with nothing wiring it: the application starts, the build is green, the tests pass, and no migration ever runs. Depend on `spring-boot-starter-flyway` or `spring-boot-starter-liquibase`, which bring both the library and its auto-configuration module.
- Flyway additionally requires a database module for the target engine on current versions, such as `flyway-database-postgresql`, alongside the starter. Missing it fails loudly rather than silently.
- `build-and-dependencies` owns how these are declared and which versions are chosen, and its [generation differences](../build-and-dependencies/references/generation-differences.md) reference carries the coordinates for both generations. State the requirement to it; do not edit the build file from a rule here.
- Whichever generation, prove wiring rather than assuming it: a clean start against an empty database must apply every migration and record them in the history table.

## Migrations run the same way everywhere

- The tool runs at application startup, against every environment, including local development. A schema created any other way is a schema no other environment has.
- Never point a migration run at a shared environment from a developer machine.
- Production credentials for the migration connection are provisioned as `application-security` requires. The migration user may need rights the application user does not have; when the project separates them, record both in the profile.
- `observability-and-logging` decides whether a startup migration gate appears in readiness. It does. An instance that has not finished migrating is not ready to serve.

## Verification

A migration is verified by executing it, never by reading it.

- Every change that includes a migration is verified by a clean run: start from an empty database of the recorded engine and version, apply the full history, and confirm the application starts with schema validation enabled. This is the only check that catches a migration that depends on state some environment happens to have.
- Run the same history a second time against the already-migrated database. Nothing should be applied and nothing should fail.
- `spring-boot-testing` requires integration tests to run against the real engine with migrations applied. That suite is what proves the schema and the mappings agree; this skill adds only the clean-install and idempotency checks above.
- CI runs the clean install on every change, not only when a migration file changed. A mapping change with no migration is exactly the defect this catches.
- Never disable migrations in a test profile to make a suite pass. A suite that runs against a schema the tool did not build proves nothing about production.

## Anti-patterns

Reject:

**Source of truth.** `ddl-auto: update` anywhere; schema changes applied by hand to a shared
environment; a table created by application code; a migration generated from entities and committed
unread.

**History.** Editing, renaming, or deleting an applied migration; reusing a version identifier;
merging two migrations with the same identifier; a down-migration presented as a recovery plan;
resolving a branch conflict by renumbering the migration that merged first.

**Compatibility.** A one-step rename on a live system; `NOT NULL` or a unique constraint added in
the same migration as the backfill that makes it valid; a drop with no evidence that nothing reads
it; a contract-phase migration shipped before its migrate phase completed.

**Operations.** An unbounded backfill inside a startup migration; a migration carrying credentials
or personal data; test fixtures seeded through a migration; migrations disabled in a test profile.

**Wiring.** On Spring Boot 4, the raw `flyway-core` or `liquibase-core` dependency without its
starter — the silent failure this skill exists to prevent.

## Completion checklist

- [ ] The tool comes from the profile or from the user, never from a guess.
- [ ] Every schema change the code depends on exists as a migration, committed with the mapping change.
- [ ] No applied migration was edited, renamed, renumbered, or deleted.
- [ ] The version identifier is unique against the merge target, and any conflict was resolved by renumbering the later branch.
- [ ] Constraints and indexes are explicitly named; nullability, defaults, precision, and length are explicit.
- [ ] Any incompatible change is split into expand, migrate, and contract phases, and the phase is stated in the change description.
- [ ] Backfills are separate, batched, resumable, and safe to run twice, with row count and duration stated.
- [ ] Only reference data the application requires is seeded, keyed by a business key.
- [ ] On Spring Boot 4, the Spring Boot starter is present, not only the third-party library.
- [ ] A clean install from an empty database was executed, and re-running the history applies nothing.

## Primary guidance

- Flyway documentation: https://documentation.red-gate.com/flyway
- Liquibase documentation: https://docs.liquibase.com
- Spring Boot data initialization: https://docs.spring.io/spring-boot/how-to/data-initialization.html
- Spring Boot 4.0 migration guide: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
