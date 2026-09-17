# Incompatible changes, backfills, and seed data

Read this when a change is not backward compatible with the deployed application, when a migration
moves data rather than structure, or when reference data has to exist for the application to
function. Every rule in `../SKILL.md` applies unchanged; this file carries the three rule sets that
only a minority of migrations need, so the always-loaded body stays about the migrations every
project writes.

This file is tool-neutral. Flyway and Liquibase specifics stay in their own references.

## Contents

1. [Expand and contract](#expand-and-contract)
2. [Data changes](#data-changes)
3. [Seed and reference data](#seed-and-reference-data)

## Expand and contract

Any change that is not backward compatible with the deployed application is split into phases across
releases. This is not optional where deployment happens without downtime, and it is the rule most
often skipped, because the single-step version works on a laptop.

1. **Expand.** Add the new structure alongside the old: new column nullable, new table empty, new constraint absent. Deploy; the running application ignores it.
2. **Migrate.** Write to both old and new from the application, and backfill existing rows. Deploy.
3. **Contract.** Stop reading the old structure, then in a later release drop it and add any constraint that could not exist while both were live.

- **A rename is a drop plus an add.** Never rename a column or table in one step on a live system: the previously deployed instance still references the old name for the length of the rollout.
- Narrowing a type, adding `NOT NULL` to a populated column, and adding a unique constraint are all breaking. Each needs the backfill complete before the constraint arrives.
- **Dropping anything requires evidence that nothing reads it**: current code, the previously deployed version, reports, downstream consumers, and any external integration. `project-naming-conventions` treats a database name consumed outside the deployable unit as a contract.
- **Record which phase a migration belongs to in the change description.** A contract-phase migration merged before its migrate phase completed is a production incident, and the phase is the only thing that makes that visible in review.

## Data changes

- **Separate a data backfill from the schema change that enables it.** They fail differently, take different amounts of time, and need different recovery.
- **Never run an unbounded backfill inside a startup migration.** It holds the deployment, it holds locks, and a timeout leaves it half applied. Batch it, make it resumable, and run it as a job or a separately triggered migration when the volume warrants.
- Before writing a backfill, state the row count, the expected duration, the locks taken, and what happens when it is interrupted. If those are unknown, that is the first task.
- A backfill is written to be safe to run twice.

## Seed and reference data

- Reference data the application cannot start or function without — lookup tables, enum rows, a default tenant, a system account — belongs in migrations, in the same history as the schema.
- Test fixtures, demo records, and developer convenience data do not. `spring-boot-testing` owns test data; a migration that inserts a fixture puts it into production.
- Make reference-data migrations idempotent and keyed by a stable business key, never by a generated identifier that differs per environment.
- A system account created by a migration carries no usable credential. `application-security` owns how its credential is provisioned.
