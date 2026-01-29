# Incremental generation & state files

The generator produces two internal state files:

## `generator-state.json`

Tracks generated entities using fingerprints (SHA) to avoid unnecessary regeneration.

Location:

```
.crud-generator/generator-state.json
```

### What is stored

- a configuration SHA (hash of the global configuration)
- an entity SHA for each generated entity

### How change detection works

#### Entity-level changes

- For each entity, the generator computes a fingerprint (SHA).
- If an entity’s definition has not changed, it is not regenerated.
- If it changed, it is included in generation.

#### Configuration-level changes

- The generator also computes a fingerprint (SHA) for the global configuration.
- If the configuration SHA changes, **all entities are regenerated**, regardless of individual entity changes.

This ensures consistency when global settings (e.g. caching, Swagger/OpenApi, GraphQL, tests, etc.) are modified.

## `migration-state.json`

Tracks schema changes to safely generate Flyway migration scripts.

Location:

```
.crud-generator/migration-state.json
```

## Interaction with `forceRegeneration`

If the following flag is enabled in `pom.xml`:

```xml
<forceRegeneration>true</forceRegeneration>
```

Then:

- the generator ignores `generator-state.json`
- all non-ignored entities are regenerated unconditionally

Useful when:

- refactoring templates
- debugging generator behavior
- forcing a clean regeneration

### Ignored entities

Entities marked with:

```yaml
ignore: true
```

- are excluded from generation
- are not tracked for regeneration
- do not affect the generator state
