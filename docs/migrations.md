# Migrations behavior (Flyway)

The generator supports automatic database schema migrations for SQL databases using Flyway.

Migrations are generated based on differences between:

- the current configuration file (e.g `crud-spec.yaml` / `.json`)
- the previous generator state, stored on disk

This enables safe, incremental schema evolution without manual SQL scripting in most cases.

## How migration generation works

For SQL databases, the generator:

1. Analyzes the current entity configuration
2. Compares it with the previous generation state
3. Generates Flyway migration scripts accordingly

To track schema changes, the generator creates and maintains:

```
.crud-generator/migration-state.json
```

The migration-state.json file stores metadata about:

- previously generated entities
- their fields and types
- table and column mappings

It allows the generator to:

- detect newly added entities
- detect added fields
- detect removed fields
- generate correct Flyway migration scripts

> This file should be committed to version control to ensure consistent migrations across environments.

## Supported migration scenarios

### Adding a new entity

When a new entity is added:

- generator detects that a new table is added
- a Flyway `CREATE TABLE` migration is created

### Adding a new field

When a field is added:

- generator detects the change
- a Flyway `ALTER TABLE ADD COLUMN` migration is created

### Removing a field

When a field is removed:

- generator detects the removal
- a Flyway `ALTER TABLE DROP COLUMN` migration is created

## ⚠️ Important limitation: no table drop

Dropping database tables is **not** supported.

- If an entity is removed from the configuration file → no `DROP TABLE` migration is generated
- Table removal must be handled manually

This design choice is intentional to prevent accidental data loss.
