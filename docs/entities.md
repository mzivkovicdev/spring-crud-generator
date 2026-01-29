# Entities & fields

Entities are defined under the `entities` section. Each entity describes:

- a generated Java model/entity class
- its database storage representation (table name)
- its fields, constraints, and relationships
- optional metadata used for documentation (Javadoc, OpenAPI descriptions, etc.)

---

## Entity schema

```yaml
entities:
  - name: ProductModel
    storageName: product_table
    description: "Represents a product"
    audit:
      enabled: true
      type: Instant
    fields: []
```

| Property      | Type   | Required | Description                                                     |
| ------------- | ------ | -------- | ----------------------------------------------------------------|
| `name`        | string | ✅       | Java class name of the entity/model                             |
| `storageName` | string | ✅       | Database table name (storage name).                             |
| `description` | string | optional | Used to generate Javadoc and enrich API docs (where applicable) |
| `audit`       | object | optional | Audit configuration for `created_at` / `updated_at` columns     |
| `fields`      | list   | ✅       | List of fields for the entity                                   |

> If `description` is provided, the generator can produce Javadoc for entities/fields.

---

## Audit configuration

The audit block controls automatic creation of audit columns on the entity’s table (typically `created_at` and `updated_at`).

```yaml
audit:
  enabled: true
  type: INSTANT
```

| Property  | Type    | Required | Description                                                                           |
| --------- | ------- | -------- | --------------------------------------------------------------------------------------|
| `enabled` | boolean | optional | Enables audit columns (`created_at`, `updated_at`) for this entity (default: `false`) |
| `type`    | enum    | optional | Underlying temporal type used for audit columns                                       |

Possible `type` values:

- `Instant`
- `LocalDate`
- `LocalDateTime`

---

## Field schema

```yaml
fields:
  - name: id
    type: Long
    description: "Primary key"
    id:
      strategy: IDENTITY
```

| Property      | Type   | Required            | Description                                                                                          |
| ------------- | ------ | ------------------- | ---------------------------------------------------------------------------------------------------- |
| `name`        | string | ✅                  | Java field name                                                                                      |
| `type`        | string | ✅                  | Java type (e.g. `String`, `Long`, `UUID`, `LocalDate`, `Enum`, `JSON<Type>`, entity name for relations, or `List<BasicType>` / `Set<BasicType>`) |
| `description` | string | optional            | Used for Javadoc and API documentation                                                               |
| `id`          | object | optional            | Marks the field as primary key and defines generation strategy                                       |
| `column`      | object | optional            | Column constraints (unique, nullable, insertable, updateable, length etc.)                           |
| `relation`    | object | optional            | Relationship definition (JPA-style)                                                                  |
| `values`      | list   | required for `Enum` | Enum constant values (only when `type: Enum`)                                                        |

- Supported basic types are: String, Character, Integer, Long, Boolean, Double, Float, Short, Byte, UUID, BigDecimal, BigInteger, LocalDate, LocalDateTime, OffsetDateTime, Instant

---

## Simple collections (ElementCollection)

Only supported for basic types:

- `List<BasicType>`
- `Set<BasicType>`

Example:

```yaml
- name: phoneNumbers
  type: List<String>
- name: tags
  type: Set<Long>
```

For `List<BasicType>` / `Set<BasicType>`, the generator creates a separate collection table named `${storageName}_${snake_case(fieldName)}` and links it to the owner entity via `<entity>_id`.

---

## Primary key: `id.strategy`

```yaml
- name: id
  type: Long
  description: "The unique identifier for the entity"
  id:
    strategy: IDENTITY
```

Supported strategies:

- `TABLE`
- `SEQUENCE`
- `UUID`
- `IDENTITY`
- `AUTO`

| Property          | Type   | Required | Applies to          | Description                                                                         |
| ----------------- | ------ | -------- | ------------------- | ------------------------------------------------------------------------------------|
| `strategy`        | enum   | ✅       | all                 | ID generation strategy                                                              |
| `generatorName`   | string | optional | `SEQUENCE`, `TABLE` | DB object name. For `SEQUENCE`: DB sequence name. For `TABLE`: generator table name.|
| `allocationSize`  | number | optional | `SEQUENCE`, `TABLE` | Allocation size for sequence/table generators (defaults to `50`).                   |
| `initialValue`    | number | optional | `SEQUENCE`, `TABLE` | Initial value for ID generation (defaults to `1`).                                  |
| `pkColumnName`    | string | optional | `TABLE`             | Name of the “segment key” column in generator table (defaults to `gen_name`).       |
| `valueColumnName` | string | optional | `TABLE`             | Name of the “counter” column in generator table (defaults to `gen_value`).          |

### Sequence-Based ID Example

```yaml
- name: id
  type: Long
  id:
    strategy: SEQUENCE
    generatorName: product_id_seq  # optional, default: <table>_id_seq
    allocationSize: 10             # optional, default: 50
    initialValue: 1                # optional, default: 1
```

### Table-Based ID Example

```yaml
- name: id
  type: Long
  id:
    strategy: TABLE
    generatorName: product_id_gen  # optional, default: <table>_id_gen
    pkColumnName: my_gen_name      # optional, default: gen_name
    valueColumnName: my_gen_val    # optional, default: gen_value
    allocationSize: 5              # optional, default: 50
    initialValue: 1000             # optional, default: 1
```
- For `TABLE` strategy, the `pkColumnValue` is automatically set to the entity's `storageName` (e.g. `user_table`), so you normally don't need to configure it manually.

### Note
- Not all databases support all ID strategies

---

## Column constraints: `column.*`

Use the column block to control column-level constraints:

```yaml
- name: name
  type: String
  column:
    nullable: false
    updateable: true
    unique: true
    length: 255
```

| Property       | Type    | Description                               |
|---------------|---------|--------------------------------------------|
| `nullable`    | boolean | Whether the column can be null             |
| `unique`      | boolean | Whether the column must be unique          |
| `length`      | number  | Column length (primarily for strings)      |
| `insertable`  | boolean | Whether the column can be inserted         |
| `updateable`  | boolean | Whether the column can be updated          |

---

## Relationships: `relation.*`

Relationships are defined via the relation block. The type must be one of:
- `OneToOne`
- `OneToMany`
- `ManyToOne`
- `ManyToMany`

### One-to-one

```yaml
- name: product
  type: ProductModel
  relation:
    type: OneToOne
    joinColumn: product_id
    fetch: EAGER
    cascade: MERGE
```

### One-to-many

```yaml
- name: users
  type: UserEntity
  relation:
    type: OneToMany
    joinColumn: product_id
    fetch: LAZY
    cascade: MERGE
```

### Many-to-many

```yaml
- name: users
  type: UserEntity
  relation:
    type: ManyToMany
    fetch: LAZY
    cascade: MERGE
    joinTable:
      name: order_user_table
      joinColumn: order_id
      inverseJoinColumn: user_id
```

| Property     | Type   | Description                                                                  |
| ------------ | ------ | ---------------------------------------------------------------------------- |
| `type`       | string | Relationship type (`OneToOne`, `OneToMany`, `ManyToOne`, `ManyToMany`)       |
| `fetch`      | string | Fetch type (`EAGER`, `LAZY`)                                                 |
| `cascade`    | string | Cascade type (e.g. `MERGE`, `ALL`, `PERSIST`, etc.)                          |
| `joinColumn` | string | Join column name (for `OneToOne`, `OneToMany`, `ManyToOne` where applicable) |
| `joinTable`  | object | Join table config (used in `ManyToMany`)                                     |

---

## Enums: `type: Enum` + `values`

To define an enum field:
```yaml
- name: status
  type: Enum
  description: "The status of the product"
  values:
    - ACTIVE
    - INACTIVE
```

- `values` is required when type is Enum.

---

## JSON fields: `JSON<Type>`

```yaml
- name: details
  type: JSON<Details>
```

Where Details is another entity-like schema definition (often used as an embedded structure):

```yaml
- name: Details
  description: "Represents a user details"
  fields:
    - name: firstName
      type: String
    - name: lastName
      type: String
```

- JSON types are useful for storing structured data inside a single database column (database support and mapping strategy depend on the selected SQL database).

---

## Validation

Validation rules are defined per-field using the validation block.

### Validation schema
```yaml
  - name: email
    type: String
    column:
      nullable: false
      unique: true
    validation:
      required: true
      notBlank: true
      email: true
      minLength: 5
      maxLength: 255
  - name: tags
    type: Set<String>
    validation:
      minItems: 1
      maxItems: 10
```

| Property    | Type    | Applies to          | Description                                    |
| ----------- | ------- | ------------------- | ---------------------------------------------- |
| `required`  | boolean | all                 | Field must be present (non-null)               |
| `notBlank`  | boolean | String              | String must contain non-whitespace characters  |
| `notEmpty`  | boolean | String, collections | Must not be empty (`""` / `[]`)                |
| `minLength` | integer | String              | Minimum string length                          |
| `maxLength` | integer | String              | Maximum string length                          |
| `min`       | decimal | numbers             | Minimum numeric value                          |
| `max`       | decimal | numbers             | Maximum numeric value                          |
| `minItems`  | integer | collections         | Minimum number of elements (`List<>`, `Set<>`) |
| `maxItems`  | integer | collections         | Maximum number of elements (`List<>`, `Set<>`) |
| `email`     | boolean | String              | Must be a valid e-mail address                 |

> min / max map well to numeric validations (e.g. BigDecimal, Integer, Long, Double).
> minItems / maxItems apply to List<BasicType> and Set<BasicType>.

### Validation rules

- `required` is about nullability. `notBlank` / `notEmpty` are about content (and non-null for the respective type).
- If a validation field is used on an unsupported type, it will be ignored.

---

## Ignoring an entity

To skip generation for a specific entity:

```yaml
- name: SomeEntity
  ignore: true
  fields: []
```
