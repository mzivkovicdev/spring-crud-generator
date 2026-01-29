# Packages configuration

By default, the generator uses a predefined package structure for all generated code.  
If you want full control over package naming and organization, override it using the `packages` section.

## Default package structure

If the `packages` section is not defined, the generator uses:

```text
|- annotations
|- businessservices
|- configurations
|- controllers
|- enums
|- exceptions
|- generated
|- mappers
|- models
|- resolvers
|- repositories
|- services
|- transferobjects
```

## Custom package structure

You can define a custom package structure by adding the `packages` section to your configuration file.

If the `packages` section is **not provided**, the generator uses the default package structure.

```yaml
packages:
  annotations: annotation
  businessservices: businessservice
  configurations: configuration
  controllers: controller
  enums: enum
  exceptions: exception
  generated: generated
  mappers: mapper
  models: persistence.entity
  resolvers: graphql
  repositories: persistence.repository
  services: persistence.service
  transferobjects: transferobject
```

Each key represents a logical group of generated code, while the value defines the actual package name that will be used.

| Key                | Description                                                    |
| ------------------ | -------------------------------------------------------------- |
| `annotations`      | Custom and generated annotations                               |
| `businessservices` | Business-level service classes                                 |
| `configurations`   | Spring configuration classes                                   |
| `controllers`      | REST controllers                                               |
| `enums`            | Generated enum classes                                         |
| `exceptions`       | Custom and generated exceptions (including exception handlers) |
| `generated`        | Internal/generated support classes (OpenAPI codegen output)    |
| `mappers`          | Entity ↔ DTO mappers                                           |
| `models`           | Persistence models / entities                                  |
| `resolvers`        | GraphQL resolvers                                              |
| `repositories`     | Spring Data repositories                                       |
| `services`         | Service layer classes                                          |
| `transferobjects`  | DTOs / transfer objects                                        |

## Validation rules & notes

If the `packages` section is `not defined`, the generator uses the default package structure.
If the `packages` section is `defined` and `any core package is specified`, then all core packages must be defined.

Core packages:
- `businessservices`
- `controllers`
- `enums`
- `exceptions`
- `mappers`
- `models`
- `repositories`
- `services`
- `transferobjects`
- If `configuration.graphql.enabled: true` is set, the `resolvers` package is **required**.
- If `configuration.openApi.generateResources: true` is enabled, the `generated` package is required.
- If required packages are missing, the generator fails fast with an error explaining which packages are missing.

Partial customization of only core packages is `not supported` to avoid inconsistent package structures.