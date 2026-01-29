# Spring CRUD Generator

Spring CRUD Generator is a Maven plugin that generates production-ready Spring Boot CRUD layers (REST + optional GraphQL), database migrations, Docker resources, tests, API docs, and more — from a single YAML/JSON specification file.

## What is it good for?

- Starting a new backend fast without copy/pasting boilerplate
- Keeping generation deterministic and transparent (your spec → your code)
- Iterating safely with incremental generation and migration tracking

## Quick links

- Getting started: [Getting started](getting-started.md)
- Configuration reference: [Configuration reference](configuration.md)
- Package configuration: [Packages configuration](packages.md)
- Entity schema: [Entities & fields](entities.md)
- Migrations: [Migrations](migrations.md)
- Incremental generation/state: [Incremental generation](incremental-generation.md)

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

See the full example spec: [docs/examples/crud-spec-full.yaml](./examples/crud-spec-full.yaml)