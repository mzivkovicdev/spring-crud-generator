# Flyway configuration

Read this only when `docs/project-profile.md` records Flyway. Every rule in `../SKILL.md` applies
unchanged; this file covers what is specific to the tool.

## Contents

- [Dependencies](#dependencies)
- [Layout and properties](#layout-and-properties)
- [Versioned and repeatable migrations](#versioned-and-repeatable-migrations)
- [Callbacks and Java migrations](#callbacks-and-java-migrations)
- [Recovering a failed migration](#recovering-a-failed-migration)

## Dependencies

`build-and-dependencies` declares these and chooses the versions. What follows is the requirement.

**On Spring Boot 4**, the Spring Boot starter is mandatory, because auto-configuration no longer
ships in one jar:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**On Spring Boot 3**, the library alone is auto-configured:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Both snippets show PostgreSQL. Replace the database module with the one for the engine recorded in
the profile — the module name follows the engine, and Flyway fails at startup with an explicit
message when it is missing.

Do not add the Flyway Maven or Gradle plugin unless the project runs migrations outside application
startup for a stated reason. The plugin is a second execution path with its own configuration, and
two paths disagree eventually.

## Layout and properties

Default location, which this skill set does not change:

```text
src/main/resources/db/migration/
    V20260115103000__create_users_table.sql
    V20260116094500__add_user_status_column.sql
    R__user_active_view.sql
```

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false
```

- `validate-on-migrate` stays on. It is the check that catches an edited applied migration, which is the failure this tool exists to prevent.
- `baseline-on-migrate` stays off on a project that starts with Flyway. Turn it on only to adopt Flyway into a database that already has a schema, and record why in the profile.
- `out-of-order` stays off. Allowing a lower version to apply after a higher one makes the history order environment-dependent, so a clean install and a long-lived database stop matching.
- `clean` is disabled by default in current Flyway versions. Leave it disabled; nothing in a normal workflow needs it.

## Versioned and repeatable migrations

- A versioned migration (`V…`) runs once and is recorded with a checksum. This is the default and covers every schema change.
- A repeatable migration (`R…`) reruns whenever its checksum changes, after all pending versioned ones. Use it only for objects that can be dropped and recreated in full: views, functions, procedures, triggers. Never for tables, columns, or data.
- A repeatable migration must be written as `CREATE OR REPLACE`, or an explicit drop followed by a create. One that fails on second execution defeats its own purpose.
- Repeatable migrations have no order among themselves beyond description. When one view depends on another, that dependency is not expressed; put both in one file.

## Callbacks and Java migrations

- Prefer SQL. Reach for a Java migration only when the change genuinely cannot be expressed in SQL, such as a backfill that has to decrypt and re-encrypt values.
- A Java migration is application code that runs before the context is ready. It cannot inject beans, cannot use repositories, and must not depend on the entity model — the model describes the schema after every migration, not the schema at this point in the history.
- Use callbacks sparingly and never for schema changes. A callback is invisible in the history table, so a schema object created by one exists in no version.

## Recovering a failed migration

- On an engine without transactional DDL, a failed migration leaves a partially applied schema and a failed entry in the history table. Fix the database state to a known point by hand only in a non-shared environment; in a shared one, follow the project's incident procedure.
- Do not repair the history table to make a failure disappear. `flyway repair` exists to reconcile checksums and remove failed entries, and using it to hide a real failure leaves the schema and the history permanently disagreeing.
- After any repair, run a clean install from an empty database, as `../SKILL.md` requires. That is the only proof the history is still coherent.
