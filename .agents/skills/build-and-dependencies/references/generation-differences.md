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
5. [Persistence](#persistence)
6. [Spring Security](#spring-security)
7. [Testing](#testing)
8. [Configuration properties that were renamed](#configuration-properties-that-were-renamed)
9. [Removed in Spring Boot 4](#removed-in-spring-boot-4)
10. [Minor lines inside Spring Boot 4](#minor-lines-inside-spring-boot-4)
11. [The classic starters are a migration aid, not a target](#the-classic-starters-are-a-migration-aid-not-a-target)

## What actually changed

Spring Boot 4 is built on Spring Framework 7, Jakarta EE 11, and a Servlet 6.1 baseline, and it
carries major versions of the rest of the portfolio: Spring Security 7 and Jackson 3, plus a Spring
Data release train that advances with each minor line — see
[minor lines inside Spring Boot 4](#minor-lines-inside-spring-boot-4). Two changes account for most
of the day-to-day difference:

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
| Nullability annotations | `org.jspecify:jspecify`, **not** managed by this BOM, so the declaration carries a version | `org.jspecify:jspecify`, managed by the BOM and already on the classpath transitively through `spring-core` |
| SOAP web services | `spring-boot-starter-web-services` | `spring-boot-starter-webservices` |
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
| OpenAPI document producer, code-first only | `org.springdoc:springdoc-openapi-starter-webmvc-api` or `-ui`, on the springdoc 2.x line | the same two artifacts on the springdoc 3.x line; the springdoc major tracks the Spring Boot generation, so the coordinate is unchanged and the version is not |

The deprecated Spring Boot 4 names still resolve. Treat that as a grace period, not as permission: a project
that starts on Spring Boot 4 uses the new name from the first commit.

**springdoc is not managed by the Spring Boot BOM on either generation**, so its version is always
declared and always `RESOLVE`d. Choosing between the `-api` and `-ui` artifacts is not a taste
question: `-ui` bundles Swagger UI into the deployable, so a project whose profile records the
interactive UI as never exposed declares `-api` and has nothing to switch off later.
`rest-api-contract` owns whether a document exists at all, and under contract-first this dependency
is absent entirely — the committed document is the source, and the generator is configured with
`documentationProvider=none` precisely so it does not pull springdoc back in.

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
| Actuator security matcher | `org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest` | `org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest` |
| JPA static metamodel processor | `org.hibernate.orm:hibernate-jpamodelgen` (Hibernate 6) | `org.hibernate.orm:hibernate-processor` (Hibernate 7) |
| `TestRestTemplate` | `org.springframework.boot.test.web.client`, auto-configured | `org.springframework.boot.resttestclient`, and `@AutoConfigureTestRestTemplate` is required |
| Jackson component | `@JsonComponent` | `@JacksonComponent` |
| Jackson mixin | `@JsonMixin` | `@JacksonMixin` |
| Mapper builder customizer | `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |
| Customizing HTTP message converters | a `HttpMessageConverters` bean, or contributed converter beans | `ServerHttpMessageConvertersCustomizer`; the Boot type is deprecated and contributed converter beans are no longer picked up |
| Replacing the mapper bean | define an `ObjectMapper` bean | define a `JsonMapper` (or `XmlMapper`) bean |
| Nullability annotation on Spring's **own** API surface | `org.springframework.lang.Nullable` | JSpecify; the `org.springframework.lang` set is deprecated |
| Nullability annotation the **project** writes | `org.jspecify.annotations` | `org.jspecify.annotations` |
| Declarative retry | Spring Retry's `@Retryable` plus `@EnableRetry`, version-managed by the BOM | `org.springframework.resilience.annotation.Retryable` plus `@EnableResilientMethods`, in the framework |
| Programmatic retry | Spring Retry's `RetryTemplate` | `org.springframework.core.retry.RetryTemplate` and `RetryPolicy` — this package carries **no** annotation |
| Spring Retry itself | version-managed by the BOM | still usable, but no longer version-managed; pin it explicitly if the project keeps it |
| `EnvironmentPostProcessor` | `org.springframework.boot.env` | `org.springframework.boot` |
| `BootstrapRegistry` | `org.springframework.boot` | `org.springframework.boot.bootstrap` |

## Persistence

| Concern | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Specification and provider | Jakarta Persistence 3.1 with Hibernate 6.x | Jakarta Persistence 3.2 with Hibernate 7.x |
| Spring Data JPA | the 3.x line | a 4.x line, advancing with the Spring Boot 4 minor line |

`spring-data-jpa` owns what a provider major version means for a mapping or a query; this table owns
only which one the generation carries.

## Spring Security

| Concern | Spring Boot 3 (Spring Security 6) | Spring Boot 4 (Spring Security 7) |
| --- | --- | --- |
| Configuration style | lambda DSL preferred, `.and()` chaining deprecated | lambda DSL only; `.and()` and `authorizeRequests()` are **removed** |
| Path matching | `AntPathRequestMatcher` and `MvcRequestMatcher` available, Ant-style default | both removed; `PathPatternRequestMatcher`, with `PathPattern` semantics as the default |
| CSRF for a browser SPA | hand-rolled token repository and handler | `csrf(csrf -> csrf.spa())` |
| OAuth2 password grant | available | removed from the client library |
| Authorization Server version | tracked separately | versioned with Spring Security |

`application-security` owns what these mean for an enforced control — in particular that a matcher
change alters which requests a rule matches without breaking the compile.

## Testing

| Concern | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Shared mocks for several tests | `@MockBean` fields on a `@TestConfiguration` | not possible on a configuration class; declare `@MockitoBean(types = {...})` on the test class or a composed annotation |
| `MockMvc` under `@SpringBootTest` | auto-configured | not provided; `@AutoConfigureMockMvc` is required |

`spring-boot-testing` owns what each of these does to a failing test, which is where the cost is.

## Configuration properties that were renamed

| Concern | Spring Boot 3 | Spring Boot 4 |
| --- | --- | --- |
| Persistence exception translation | `spring.dao.exceptiontranslation.enabled` | `spring.persistence.exceptiontranslation.enabled` |
| Jackson read/write features | `spring.jackson.read.*`, `spring.jackson.write.*` | format-scoped as `spring.jackson.json.read.*` / `.write.*`; later 4.x lines also auto-configure the unscoped `spring.jackson.read.*` / `.write.*` again, so confirm which the project's line binds |
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
- Spring Retry's `@Recover`, which has no equivalent in the framework's resilience support. An exhausted retry is translated in the REST exception advice instead; `spring-data-jpa` owns that decision.

## Minor lines inside Spring Boot 4

**The generation is not the whole answer.** `docs/project-profile.md` records the generation because
the rules in this skill set branch on it, but Spring Boot 4's minor lines carry major versions of
other projects, so a row that is true for one 4.x line can be wrong for the next. Two things follow
from that, and both are rules rather than advice.

- **Record the minor line, not only the generation.** The profile's Spring Boot version row is what tells a later task which 4.x line it is looking at. `4` on its own does not decide which Spring Security or Spring Data version is in the build.
- **Read the portfolio versions from the effective dependency tree, never from this file.** The table below exists so a difference is expected rather than discovered; it is a written-down value and obeys the same rule as every other written-down value here.

| Portfolio project | Where to read it |
| --- | --- |
| Spring Framework | `./mvnw dependency:tree` or `./gradlew dependencies`, then the project's release notes |
| Spring Security | same; a major line brings API removals, so check its "What's New" before an upgrade |
| Spring Data release train | same; the train version and the module version differ, and both matter |
| Micrometer, Hibernate, Jackson | same |

Changes that arrived in a 4.x minor line after 4.0 and that the skills in this set care about, each
to be confirmed against the project's actual line:

| Change | Owner skill | Why it matters here |
| --- | --- | --- |
| An HTTP client address filter that blocks outbound calls to configured address ranges | `application-security` | A platform SSRF control. Prefer it to a hand-written allowlist where the line provides it, and keep the network-layer control either way. |
| `spring.security.oauth2.resourceserver.jwt.authorities-claim-expressions` | `application-security` | Replaces the claim-name and delimiter properties for deriving authorities. |
| Automatic context propagation for `@Async` | `observability-and-logging` | May make a project-owned MDC task decorator redundant. Verify before writing one. |
| `management.opentelemetry.enabled` and sampler configuration | `observability-and-logging` | Changes how tracing export is switched off in a deployment. |

Do not treat this list as complete. It records the ones that touch a rule in this skill set; the
release notes for the project's own line are the authority.

## The classic starters are a migration aid, not a target

`spring-boot-starter-classic` and `spring-boot-starter-test-classic` restore a Spring Boot 3-shaped
classpath where every auto-configuration is present. They exist to get an in-flight upgrade
compiling, and they are legitimate for exactly that.

- Never start a **new** Spring Boot 4 project on them. They defeat the modularization the generation exists to deliver.
- When an upgrade uses them, record the removal condition in the profile's deferred decisions. A classic starter with no removal date is a permanent dependency nobody chose.
