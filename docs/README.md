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
- Need a complete example? → [crud-spec-full.yaml](./examples/crud-spec-full.yaml)

## What you will find here

- Setup and first run instructions
- Full configuration reference
- Entity and field schema
- Package customization
- Migration and incremental generation behavior
- Schema and example specs

## Minimal example

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

See the full example spec: [crud-spec-full.yaml](./examples/crud-spec-full.yaml)

Back to the main project page: [README](../Readme.MD)
