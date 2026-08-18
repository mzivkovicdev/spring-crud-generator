# Liquibase configuration

Read this only when `docs/project-profile.md` records Liquibase. Every rule in `../SKILL.md` applies
unchanged; this file covers what is specific to the tool.

## Contents

- [Dependencies](#dependencies)
- [Layout and properties](#layout-and-properties)
- [Change sets](#change-sets)
- [Contexts, labels, and preconditions](#contexts-labels-and-preconditions)
- [Recovering a failed change set](#recovering-a-failed-change-set)

## Dependencies

`build-and-dependencies` declares these and chooses the versions. What follows is the requirement.

**On Spring Boot 4**, the Spring Boot starter is mandatory, because auto-configuration no longer
ships in one jar:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-liquibase</artifactId>
</dependency>
```

**On Spring Boot 3**, the library alone is auto-configured:

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

Liquibase needs no separate database module; engine support ships with the core library.

Do not add the Liquibase Maven or Gradle plugin unless the project runs migrations outside
application startup for a stated reason. The plugin is a second execution path with its own
configuration, and two paths disagree eventually.

## Layout and properties

One master changelog includes the individual ones, in order, and is never reordered:

```text
src/main/resources/db/changelog/
    db.changelog-master.yaml
    changes/
        20260115103000-create-users-table.yaml
        20260116094500-add-user-status-column.yaml
```

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

- Pick one changelog format — YAML, XML, JSON, or SQL — and use it for the whole project. Mixed formats make the history harder to read than any one of them.
- The master changelog contains `include` or `includeAll` entries and nothing else. Do not put a change set directly in it.
- Prefer explicit `include` entries over `includeAll`. `includeAll` orders by filename, so a rename silently reorders history.
- Set `logicalFilePath` on each changelog when files may ever move. Liquibase identifies a change set by id, author, and file path together; moving a file without it makes an applied change set look new.

## Change sets

- One change set per logical change, with an id, an author, and a description. `project-naming-conventions` owns the form of the id.
- Never edit an applied change set. Liquibase stores a checksum in `DATABASECHANGELOG` and fails on mismatch, which is the behavior to rely on rather than work around.
- Do not use `runOnChange` for schema structure. It is the equivalent of a repeatable migration and belongs only on objects that are dropped and recreated in full, such as views and procedures.
- Prefer Liquibase's declarative change types over `sql` and `sqlFile` when one exists for the change. The declarative form carries the engine differences; raw SQL pins the changelog to one engine.
- Use raw SQL deliberately when the change has no declarative equivalent, and state the engine it targets.
- Do not write `rollback` blocks as a recovery plan. `../SKILL.md` requires forward-only recovery. Write one only where the project has decided to use it for a specific, tested operational procedure.

## Contexts, labels, and preconditions

- Contexts and labels exist to select change sets per environment. Use them sparingly: a schema that differs per environment is a schema no environment verifies. Never use a context to keep a change out of production while it runs everywhere else.
- Test data through a context is still test data in the schema history. `spring-boot-testing` owns fixtures.
- Preconditions are useful for adopting Liquibase into an existing database, and for guarding a change that must not run twice. `onFail` and `onError` are explicit decisions; record why in the change set description.
- Do not use a precondition to make a change set silently skip. A skipped change set produces an environment whose schema no history explains.

## Recovering a failed change set

- On an engine without transactional DDL, a failed change set leaves a partially applied schema. Repair the database state by hand only in a non-shared environment; in a shared one, follow the project's incident procedure.
- `changelogSync` marks change sets as applied without executing them. It is correct only when adopting Liquibase into a database that already has the structure, and wrong in every other case, because it makes the history claim work that never happened.
- `clearCheckSums` recomputes stored checksums. Use it only after a deliberate, reviewed change to an applied change set's formatting, never to silence a mismatch caused by an edit to its content.
- After any repair, run a clean install from an empty database, as `../SKILL.md` requires. That is the only proof the history is still coherent.
