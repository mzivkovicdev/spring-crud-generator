---
name: sql-database-migration
description: Relational schema migration ownership for Spring Boot applications using Flyway or Liquibase. Use whenever a change adds, alters, or removes a table, column, constraint, index, sequence, view, or routine; whenever a migration file is created, edited, ordered, or reviewed; when backfilling data, seeding reference data, resolving a migration conflict between branches, or verifying migrations in CI. Covers both tools and both Spring Boot generations. Excludes NoSQL data stores.
---

# SQL database migration

Every change to a relational schema is a migration file, reviewed like code and executed by the
tool, in every environment including a developer laptop.

Relational databases only. A NoSQL store has no linear version history, no checksum over an applied
script, and a different expand-and-contract shape; do not stretch these rules to cover one.

## Coordination with other skills

This skill states what a migration must contain and when it must exist. It does not name files,
declare dependencies, or define test levels.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what and carries
the precedence order for a genuine conflict. Read it there rather than from a copy here. The seams
this skill crosses most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| Schema shape | the migration that creates it | `spring-data-jpa` owns what the mapping needs |
| Wiring | that migrations must actually run | `build-and-dependencies` owns the dependency that runs them |
| Names | nothing | `project-naming-conventions` owns migration, table, column, constraint, and index names |
| Startup gate | that a gate exists | `observability-and-logging` owns whether it appears in readiness |

## The migration tool is a recorded decision

`docs/project-profile.md` records Flyway or Liquibase. Both are correct and nothing here prefers one.

- Read the profile first. When it records neither and the repository contains no migration directory and no migration dependency, **ask the user which tool the project will use.** This is an `ASK` decision in the token vocabulary `project-decision-profile` defines, so it blocks: nothing in the repository decides it, so do not pick one, and do not infer a tool from an unrelated dependency.
- An existing migration directory or applied history table is the answer, whatever the profile says. Correct the profile, not the repository.
- Never run two migration tools against one schema. Each keeps its own history table and neither describes the full schema, so a clean install reproduces something no environment has. Converting from one to the other is a project of its own, never a side effect of a feature.

## Reference routing

Read [Flyway configuration](references/flyway-configuration.md) or
[Liquibase configuration](references/liquibase-configuration.md) — whichever the profile records,
and only that one. Each carries its tool's dependency declarations, layout, properties, migration or
change-set forms, and repair procedures. The rules here apply to both, and neither reference
restates them.

Read [incompatible changes, backfills, and seed data](references/incompatible-changes-and-data.md)
when a change is not purely additive, when a migration moves data, or when it inserts reference
rows. It is tool-neutral, and it carries rules stated nowhere else.

## The migration tool owns the schema

- The migration history is the only source of truth for schema shape. Any structure the application depends on exists because a migration created it.
- Set Hibernate schema generation to `validate` or `none`. Never `create`, `create-drop`, or `update` against a shared environment, and never `update` at all: it silently diverges from the history and produces a schema no environment can reproduce.
- `validate` is useful precisely because it fails at startup when mappings and schema disagree. Treat that failure as a missing migration, not as a reason to loosen the setting.
- Never create or alter schema objects from application code, from a data-loading script outside the tool, or by hand in a shared environment. A hand-applied change exists in one database and nowhere in the history.
- Entities and migrations change in the same commit. A mapping merged ahead of its migration breaks every environment that deploys it; a migration merged ahead of its mapping is dead weight nobody can review.

## Every migration is forward-only and immutable

- A migration applied to any shared environment is frozen: never edit its content, rename it, renumber it, or delete it. The tool stores a checksum, so an edited file fails startup in every environment that already ran it.
- Correct a mistake with a new migration, even when the mistake is one day old and the fix is trivial.
- Editing is allowed only while the migration sits on a feature branch and has been applied nowhere but the author's own database. Once it merges, the rule above applies.
- Never write down-migrations or rely on tool-generated rollback. Recovery from a bad schema change is a new forward migration plus, where data was destroyed, a restore. A rollback script that has never been executed is not a recovery plan.

## Ordering and branch conflicts

Two developers on two branches will pick the same version number. That is routine, not an incident,
and the project needs one rule for it.

- Version identifiers are strictly increasing and never reused. `project-naming-conventions` owns their form.
- The branch that merges second renumbers its migration to follow the merged one, then reruns its tests from an empty database. Never merge two migrations with the same identifier, and never resolve the conflict by editing the one that merged first.
- Prefer an identifier scheme that makes collisions rare, such as a UTC timestamp, over a sequential counter. Record the scheme once; never mix schemes in one project.
- Migrations apply in one order, so a migration may depend only on the state earlier ones left. Never assume a migration from a parallel branch has run.
- One migration per logical change. A file that adds a table, backfills it, and drops another is impossible to review, to reason about on failure, and to reorder.

## Writing a migration

- Write explicit SQL, or the tool's declarative format, for the engine the profile records. Never generate a migration from the entity model and commit it unread: generated DDL carries defaults, name choices, and drops that nobody decided.
- Name every constraint and index explicitly. A generated name differs in every environment, so a later migration cannot drop it.
- Make each migration idempotent where the tool does not guarantee it, and never depend on a partially applied migration being retried cleanly.
- Prefer one DDL statement per migration on engines where DDL is not transactional. There, a multi-statement migration that fails halfway leaves the schema in a state the history does not describe, and manual repair is the only way out.
- State `NOT NULL`, defaults, precision, scale, and length explicitly. A column added without them defers the decision to the engine.
- Adding a column with a default rewrites the table on some engines and versions. Check the recorded engine and version before adding one to a large table.
- Never put credentials, personal data, or environment-specific values in a migration. It is committed source, readable by everyone with repository access.

## Changes that are not backward compatible

**Any change that is not backward compatible with the deployed application is split into expand,
migrate, and contract phases across releases**, and the change description names which phase a
migration belongs to. This is not optional where deployment happens without downtime, and it is the
rule most often skipped because the single-step version works on a laptop. A rename is a drop plus an
add; narrowing a type, adding `NOT NULL` to a populated column, and adding a unique constraint are
all breaking.

The phases, the evidence a drop requires, the backfill rules, and the seed-data rules are in
[incompatible changes, backfills, and seed data](references/incompatible-changes-and-data.md). Read
it before writing any migration that is not purely additive, moves data, or inserts rows.

## Wiring by Spring Boot generation

The migration files and every rule above are identical across generations. The dependency is not,
and getting it wrong fails silently.

- **On Spring Boot 3** the third-party library on the classpath is enough: auto-configuration for both tools lives in `spring-boot-autoconfigure`, which every application already has.
- **On Spring Boot 4 the tool's Spring Boot starter is mandatory.** Auto-configuration was split into per-technology modules, so the raw library leaves nothing wiring it: the application starts, the build is green, the tests pass, and no migration ever runs. The starter brings both the library and its auto-configuration module.
- `build-and-dependencies` owns how these are declared and which versions are chosen; its [generation differences](../build-and-dependencies/references/generation-differences.md) reference carries the coordinates for both generations, and this skill's tool reference shows them in place, including the engine module Flyway additionally needs. State the requirement; never edit the build file from a rule here.
- Prove wiring on either generation rather than assuming it: a clean start against an empty database must apply every migration and record it in the history table.

## Migrations run the same way everywhere

- The tool runs at application startup, against every environment, including local development. A schema created any other way is a schema no other environment has.
- Never point a migration run at a shared environment from a developer machine.
- Production credentials for the migration connection are provisioned as `application-security` requires. The migration user may need rights the application user does not; when the project separates them, record both in the profile.
- `observability-and-logging` decides whether a startup migration gate appears in readiness. It does: an instance that has not finished migrating is not ready to serve.

## Verification

A migration is verified by executing it, never by reading it.

- Verify every change that includes a migration with a clean run: from an empty database of the recorded engine and version, apply the full history and confirm the application starts with schema validation enabled. This is the only check that catches a migration depending on state some environment happens to have.
- Run the same history a second time against the already-migrated database. Nothing should be applied and nothing should fail.
- `spring-boot-testing` requires integration tests against the real engine with migrations applied, and that suite proves the schema and the mappings agree. This skill adds only the clean-install and idempotency checks above.
- CI runs the clean install on every change, not only when a migration file changed. A mapping change with no migration is exactly the defect this catches.
- Never disable migrations in a test profile to make a suite pass. A suite running against a schema the tool did not build proves nothing about production.

## Anti-patterns

Each of these is a rule above, in the shape it usually reaches review. Reject:

- **Source of truth.** `ddl-auto: update` anywhere; a change applied by hand to a shared environment; a table created by application code; a migration generated from entities and committed unread.
- **History.** Editing, renaming, or deleting an applied migration; reusing a version identifier; merging two migrations with the same identifier; resolving that collision by touching the one that merged first; a down-migration presented as a recovery plan.
- **Compatibility.** A one-step rename on a live system; `NOT NULL` or a unique constraint added in the same migration as the backfill that makes it valid; a drop with no evidence that nothing reads it; a contract-phase migration shipped before its migrate phase completed.
- **Operations.** An unbounded backfill inside a startup migration; a migration carrying credentials or personal data; test fixtures seeded through a migration; migrations disabled in a test profile.
- **Wiring.** On Spring Boot 4, the raw third-party library without its Spring Boot starter — the silent failure this skill exists to prevent.

## Completion checklist

- [ ] The tool came from the profile or the user, never a guess.
- [ ] Every schema change the code depends on exists as a migration, committed with the mapping change.
- [ ] No applied migration was edited, renamed, renumbered, or deleted.
- [ ] The version identifier is unique against the merge target; any collision was resolved by renumbering the later branch.
- [ ] Constraints and indexes are named; nullability, defaults, precision, and length are explicit.
- [ ] Incompatible changes are split into expand, migrate, and contract phases, with the phase stated in the change description.
- [ ] Backfills are separate, batched, resumable, and safe to run twice, with row count and duration stated.
- [ ] Only reference data the application requires is seeded, keyed by a business key.
- [ ] On Spring Boot 4, the Spring Boot starter is present, not only the third-party library.
- [ ] A clean install from an empty database ran, and re-running the history applied nothing.

## Primary guidance

- Flyway documentation: https://documentation.red-gate.com/flyway
- Liquibase documentation: https://docs.liquibase.com
- Spring Boot data initialization: https://docs.spring.io/spring-boot/how-to/data-initialization.html
- Spring Boot 4.0 migration guide: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
