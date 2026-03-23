# Migrations

The generator supports automatic database migration generation for both SQL and MongoDB databases.

Migrations are generated based on differences between:

- the current configuration file (e.g. `crud-spec.yaml` / `.json`)
- the previous generator state, stored on disk

This enables safe, incremental schema evolution without manual scripting in most cases.

Migration generation is **opt-in** via the spec:

```yaml
configuration:
  migrationScripts: true
```

When `migrationScripts: false` (default), no migration files are generated for either Flyway or Mongock.

---

## SQL databases — Flyway

For SQL databases (`postgresql`, `mysql`, `mariadb`, `mssql`), the generator produces **Flyway migration scripts**.

### How it works

1. Analyzes the current entity configuration
2. Compares it with the previous generation state
3. Generates Flyway `.sql` migration scripts accordingly

To track schema changes, the generator creates and maintains:

```
.crud-generator/migration-state.json
```

The `migration-state.json` file stores metadata about:

- previously generated entities
- their fields and types
- table and column mappings
- the last migration version number

Migration scripts are written to:

```
src/main/resources/db/migration/
```

Flyway naming convention is used: `V{n}__{description}.sql`

### Supported Flyway scenarios

#### Adding a new entity

When a new entity is added:

- the generator detects that a new table is needed
- a Flyway `CREATE TABLE` migration is generated

#### Adding a new field

When a field is added to an existing entity:

- the generator detects the change
- a Flyway `ALTER TABLE ADD COLUMN` migration is generated

#### Removing a field

When a field is removed from an existing entity:

- the generator detects the removal
- a Flyway `ALTER TABLE DROP COLUMN` migration is generated

#### Enabling / disabling soft delete

Soft delete is controlled per-entity via:

```yaml
softDelete: true
```

When `softDelete` is toggled, the generator detects the change by comparing the previous state in `.crud-generator/migration-state.json`.

This results in a migration being generated that updates the table to match the soft delete strategy (for example, adding/removing the required soft-delete column(s), constraints, or indexes — depending on the database).

Notes:
- Soft delete changes are also reflected in the entity fingerprint used for diffing, so the generator reliably detects toggles even if the rest of the schema is unchanged.
- The generator does not drop tables when disabling soft delete; it only applies the schema changes defined by the migration templates.

### ⚠️ No table drop

Dropping SQL tables is **not supported**.

- If an entity is removed from the spec → no `DROP TABLE` migration is generated
- Table removal must be handled manually

This is intentional to prevent accidental data loss.

---

## MongoDB — Mongock

For MongoDB (`database: mongodb`), the generator produces **Mongock `@ChangeUnit` Java classes**.

### How it works

1. Analyzes the current entity configuration
2. Compares it with the previous generation state
3. Generates Mongock `@ChangeUnit` classes accordingly

To track schema changes, the generator creates and maintains:

```
.crud-generator/mongock-state.json
```

The `mongock-state.json` file stores metadata about:

- previously generated collections
- their fields and BSON types
- the last migration version number

Migration classes are written to the `migration` sub-package of the generated output, e.g.:

```
src/main/java/com/example/myapp/migration/
├── V001__Create_Products_Collection.java
├── V002__Create_Users_Collection.java
└── V003__Add_FieldsTo_Product.java   ← incremental, on next run
```

Naming convention: `V{nnn}__{Action}_{Target}.java` — version is zero-padded to 3 digits.

### Supported Mongock scenarios

#### Adding a new entity

When a new entity is added:

- the generator detects that a new collection is needed
- a `@ChangeUnit` class is generated that creates the collection and its indexes

#### Adding a new field

When a field is added to an existing entity:

- the generator detects the change
- a `@ChangeUnit` class is generated using `$set` with an `exists: false` guard (idempotent)

#### Removing a field

When a field is removed from an existing entity:

- the generator detects the removal
- a `@ChangeUnit` class is generated using `$unset`

### Index generation

Unique indexes are automatically generated for fields with `validation.email: true`.

### Required project dependencies

Add Mongock to the target project's `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.mongock</groupId>
        <artifactId>mongock-springboot</artifactId>
        <version>5.5.0</version>
    </dependency>
    <dependency>
        <groupId>io.mongock</groupId>
        <artifactId>mongodb-springdata-v4-driver</artifactId>
        <version>5.5.0</version>
    </dependency>
</dependencies>
```

Use `mongodb-springdata-v3-driver` for Spring Boot 2.x / Spring Data 3.x.

### Required application configuration

Add to `application.properties`:

```properties
mongock.migration-scan-package=com.example.myapp.migration
mongock.enabled=true
```

### ⚠️ No collection drop

Dropping MongoDB collections is **not supported**.

- If an entity is removed from the spec → no collection drop migration is generated
- Collection removal must be handled manually

This is intentional to prevent accidental data loss.
