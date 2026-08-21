# Spring Boot 3 and 4: declarations, coordinates, and properties

Use this reference whenever a change touches a starter, a module coordinate, an annotation that moved
package, or a configuration property whose name differs between generations. Apply every rule from
`../SKILL.md`.

This file is the single catalogue of **what a thing is called** in each generation. It deliberately
carries no behavioral rules: the skill that owns a topic states the behavior, and links here for the
coordinate. `docs/project-profile.md` records the generation, and that one row is what every skill
reads to decide which column applies.

## Contents

1. [What actually changed](#what-actually-changed)
2. [Starter and module coordinates](#starter-and-module-coordinates)
3. [Test starters](#test-starters)
4. [Annotations and types that moved](#annotations-and-types-that-moved)
5. [Configuration properties that were renamed](#configuration-properties-that-were-renamed)
6. [Removed in Spring Boot 4](#removed-in-spring-boot-4)
7. [The classic starters are a migration aid, not a target](#the-classic-starters-are-a-migration-aid-not-a-target)

## What actually changed

Spring Boot 4 is built on Spring Framework 7, Jakarta EE 11, and a Servlet 6.1 baseline, and it
carries major versions of the rest of the portfolio: Spring Security 7, Spring Data 2025.1, and
Jackson 3. Two changes account for most of the day-to-day difference:

1. **Modularization.** Auto-configuration was split into per-technology modules. Every module is `spring-boot-<technology>` with root package `org.springframework.boot.<technology>`, every starter is `spring-boot-starter-<technology>`, and every test starter is `spring-boot-starter-<technology>-test`. A third-party library on the classpath without its Spring Boot module is now inert: the application starts, the build is green, and the feature silently does nothing. This is the most dangerous failure mode in the whole generation change, because no check reports it.
2. **Jackson 3.** The group and package moved from `com.fasterxml.jackson` to `tools.jackson`, except `jackson-annotations`, which keeps the `com.fasterxml.jackson.core` group and the `com.fasterxml.jackson.annotation` package.

Neither is a design change. A controller, a service, an entity, or a test written to this skill set
compiles and behaves the same way on both generations once the declarations are right.

## Starter and module coordinates

Read the column for the generation the profile records. Spring Boot 3 coordinates are unchanged from what
the project already uses; the table exists so a Spring Boot 4 project does not silently keep a Spring Boot 3 name
that no longer wires anything.

| Capability | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Spring MVC | `spring-boot-starter-web` | `spring-boot-starter-webmvc` (`-web` remains but is deprecated) |
| Validation | `spring-boot-starter-validation` | unchanged |
| Spring Data JPA | `spring-boot-starter-data-jpa` | unchanged |
| JDBC | `spring-boot-starter-jdbc` | unchanged |
| Flyway | `flyway-core` alone auto-configures | `spring-boot-starter-flyway` **required** |
| Liquibase | `liquibase-core` alone auto-configures | `spring-boot-starter-liquibase` **required** |
| Jackson | arrives with the web starter | `spring-boot-starter-jackson` |
| Caching | `spring-boot-starter-cache` | unchanged |
| Quartz | `spring-boot-starter-quartz` | unchanged |
| AspectJ | `spring-boot-starter-aop` | `spring-boot-starter-aspectj` |
| Spring Security | `spring-boot-starter-security` | unchanged |
| OAuth2 resource server | `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |
| OAuth2 client | `spring-boot-starter-oauth2-client` | `spring-boot-starter-security-oauth2-client` |
| Authorization server | `spring-boot-starter-oauth2-authorization-server` | `spring-boot-starter-security-oauth2-authorization-server` |
| Actuator | `spring-boot-starter-actuator` | unchanged |
| Micrometer registry | registry artifact, e.g. `micrometer-registry-prometheus` | `spring-boot-starter-micrometer-metrics`, or `spring-boot-starter-opentelemetry` for the OTLP path |
| Imperative HTTP client | part of the web starter | `spring-boot-starter-restclient` |

The deprecated Spring Boot 4 names still resolve. Treat that as a grace period, not as permission: a project
that starts on Spring Boot 4 uses the new name from the first commit.

## Test starters

On Spring Boot 4 each technology has a test companion, and **every test starter brings
`spring-boot-starter-test` transitively**, so declaring that one separately is redundant. Declare the
test starters for the technologies under test instead.

| Capability under test | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Core test stack | `spring-boot-starter-test` | comes transitively with any `-test` starter |
| Spring MVC | in `spring-boot-starter-test` | `spring-boot-starter-webmvc-test` |
| Spring Data JPA | in `spring-boot-starter-test` | `spring-boot-starter-data-jpa-test` |
| Spring Security test support | `spring-security-test` | `spring-boot-starter-security-test` |
| Flyway / Liquibase | none needed | `spring-boot-starter-flyway-test` / `-liquibase-test` |

`@WithMockUser` and `@WithUserDetails` need `spring-boot-starter-security-test` on Spring Boot 4. Without it
they fail in ways that look like an authorization defect rather than a missing dependency, which is
how a real control ends up being "fixed" by weakening it. `spring-boot-testing` owns what those tests
must prove; this skill owns the declaration that makes them run.

## Annotations and types that moved

| Concern | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Mocking a bean in a test | `@MockBean`, `@SpyBean` | `@MockitoBean`, `@MockitoSpyBean` (the old pair is **removed**) |
| Entity scanning | `org.springframework.boot.autoconfigure.domain.EntityScan` | `org.springframework.boot.persistence.autoconfigure.EntityScan` |
| Jackson component | `@JsonComponent` | `@JacksonComponent` |
| Jackson mixin | `@JsonMixin` | `@JacksonMixin` |
| Mapper builder customizer | `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |
| Replacing the mapper bean | define an `ObjectMapper` bean | define a `JsonMapper` (or `XmlMapper`) bean |
| Nullability annotation | `org.springframework.lang.Nullable` | `org.jspecify.annotations.Nullable` |
| Retry | Spring Retry, with its own dependency management | `org.springframework.core.retry` in the framework; Spring Retry needs an explicit version |
| `EnvironmentPostProcessor` | `org.springframework.boot.env` | `org.springframework.boot` |
| `BootstrapRegistry` | `org.springframework.boot` | `org.springframework.boot.bootstrap` |

## Configuration properties that were renamed

| Concern | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Persistence exception translation | `spring.dao.exceptiontranslation.enabled` | `spring.persistence.exceptiontranslation.enabled` |
| Jackson read/write features | `spring.jackson.read.*`, `spring.jackson.write.*` | `spring.jackson.json.read.*`, `spring.jackson.json.write.*` |
| Jackson parser features | `spring.jackson.parser.*` | `spring.jackson.json.read.*` where an equivalent read feature exists |
| Module registration | well-known modules only | all classpath modules, unless `spring.jackson.find-and-add-modules=false` |
| Health probes | opt in | enabled by default; disable with `management.endpoint.health.probes.enabled=false` |

When upgrading an existing application, add `spring-boot-properties-migrator` at `runtime` scope for
one cycle. It reports and temporarily remaps renamed properties at startup. Remove it once the
report is clean; leaving it in production hides the very drift it was added to find.

## Removed in Spring Boot 4

Do not carry these into a Spring Boot 4 project, and do not restore them:

- Undertow, including the starter and embedded-server support, because it is not Servlet 6.1 compatible.
- `@MockBean` and `@SpyBean`.
- `MockitoTestExecutionListener`, so `@Mock` and `@Captor` need Mockito's own `MockitoExtension`.
- Fully executable jars produced by embedded launch scripts.
- The classic uber-jar loader configuration.
- Optional dependencies in uber jars, unless `includeOptional` is set deliberately.
- Dependency management for Spring Retry and for Spring Authorization Server, which is now versioned by Spring Security.

## The classic starters are a migration aid, not a target

`spring-boot-starter-classic` and `spring-boot-starter-test-classic` restore a Spring Boot 3-shaped
classpath where every auto-configuration is present. They exist to get an in-flight upgrade
compiling, and they are legitimate for exactly that.

- Never start a **new** Spring Boot 4 project on them. They defeat the modularization the generation exists to deliver.
- When an upgrade uses them, record the removal condition in the profile's deferred decisions. A classic starter with no removal date is a permanent dependency nobody chose.
