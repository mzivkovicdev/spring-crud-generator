# Documentation

Spring CRUD Generator is a Maven plugin that generates production-ready Spring Boot CRUD layers (REST + optional GraphQL), database migrations, Docker resources, tests, API docs, and more — from a single YAML/JSON specification file.

This directory contains the full project documentation.

## Start here

- New to the project? → [Getting started](./getting-started.md)
- Looking for all available options? → [Configuration reference](./configuration.md)
- Working with entities and fields? → [Entities & fields](./entities.md)
- Need package customization? → [Packages configuration](./packages.md)
- Want migration behavior explained? → [Migrations](./migrations.md)
- Need incremental generation details? → [Incremental generation](./incremental-generation.md)
- Need a neutral product comparison? → [Spring CRUD Generator vs Bootify](./comparison-with-bootify.md)
- Want autocomplete/validation? → [crud-spec.schema.json](./schema/crud-spec.schema.json)
- Need a complete SQL example? → [crud-spec-full.yaml](./examples/crud-spec-full.yaml)
- Need a complete MongoDB example? → [mongo-crud-spec-full.yaml](./examples/mongo-crud-spec-full.yaml)

## What you will find here

- Setup and first run instructions
- Full configuration reference
- Entity and field schema
- Package customization
- Migration and incremental generation behavior
- Schema and example specs

## Minimal examples

**SQL (PostgreSQL / MySQL / MariaDB / MSSQL)**

```yaml
configuration:
  database: postgresql
entities:
  - name: ProductModel
    storageName: product_table
    fields:
      - name: id
        type: Long
        id:
          strategy: IDENTITY
      - name: name
        type: String
        column:
          nullable: false
          unique: true
```

**MongoDB**

```yaml
configuration:
  database: mongodb
entities:
  - name: ProductModel
    storageName: products
    fields:
      - name: id
        type: String
        id: true
      - name: name
        type: String
        validation:
          required: true
          notBlank: true
```

Full examples: [SQL spec](./examples/crud-spec-full.yaml) · [MongoDB spec](./examples/mongo-crud-spec-full.yaml)

Back to the main project page: [README](../Readme.MD)
