---
name: build-and-dependencies
description: Build configuration and dependency governance for Java 21+ Spring Boot REST projects using Maven or Gradle. Use when creating or changing build files, dependency or plugin declarations, versions, BOMs, wrappers, compiler settings, annotation processors, test selection and source sets, packaging, or build profiles; when adding, replacing, or upgrading a dependency; and when auditing a project for unnecessary, duplicated, misscoped, or obsolete dependencies. Owns the dependency justification gate and the removal workflow.
---

# Build and Dependencies

A build file is production configuration. It decides what code exists, what runs at startup, what an
attacker can reach, and how long every engineer waits for feedback. Treat a dependency as a
long-term commitment, not a convenience.

This skill is build-tool neutral. Maven and Gradle are both fully supported, and every rule here
applies to whichever one the project uses.

## Coordination with other skills

| Skill | Treat as owner of |
| --- | --- |
| `spring-boot-patterns` | The project profile, architecture, and Spring feature design; this skill owns the build files that realize it |
| `modern-java-21` | Java language level use and source rules; this skill owns the compiler configuration that enables them |
| `spring-boot-testing` | Test levels, scenarios, and execution; this skill owns the plugin and source-set configuration that makes those suites actually run |
| `spring-data-jpa` | Persistence behavior; this skill owns the driver, migration-tool, and annotation-processor declarations it depends on |
| `application-security` | Supply-chain risk, provenance, SBOM, vulnerability triage, and CI/CD protection; this skill owns the declarations those rules evaluate |
| `project-naming-conventions` | Module and artifact names |
| `spring-boot-code-review` | Review scope, evidence, severity, and reporting when the task is review-only |

Do not restate those rules here. When the user asks for a review rather than a change, produce
findings under `spring-boot-code-review` and do not edit build files.

## Determine the build tool before editing

Read `docs/project-profile.md`, which `spring-boot-patterns` owns. It records the build tool, the
Java release, and the Spring Boot version.

- If the repository already contains `pom.xml` or `build.gradle`/`build.gradle.kts`, that is the answer. Record it in the profile if it is missing there.
- If the repository contains neither — an empty repository, or a skeleton with nothing generated yet — **ask the user which build tool the project will use** along with the Java release and Spring Boot version. Do not pick one.
- Never introduce a second build tool, and never convert an existing project from one to the other unless conversion is the explicit task.

## Reference routing

Read only what the change requires:

- Read [Maven configuration](references/maven-configuration.md) when the project uses Maven.
- Read [Gradle configuration](references/gradle-configuration.md) when the project uses Gradle.
- Read [dependency audit and removal](references/dependency-audit.md) when adding, replacing, or removing a dependency, and whenever the user asks which dependencies do not belong.

## The dependency justification gate

Every dependency must pass all five questions before it is declared. If any answer is missing, do
not add it.

1. **What concrete requirement does it satisfy?** Name the behavior in this project, not the library's feature list.
2. **Can the JDK, Spring Boot, or an already-declared dependency do it?** The starters already bring Jackson, validation, logging, the servlet container, transaction management, and the test stack. Most "small helper" libraries duplicate something already present.
3. **Is the cost proportionate?** A dependency added for one utility method is not proportionate. Write the method.
4. **Is it maintained and appropriately licensed?** Check the last release, open critical issues, the license against project policy, and whether it is a single-maintainer package.
5. **Is it managed by the Spring Boot BOM?** If yes, declare it without a version. If not, pin it explicitly and record why the project needs it outside dependency management.

Additional rules:

- Prefer the Spring Boot starter over assembling its parts by hand. A starter is one declaration with a tested, version-managed set behind it.
- Never declare two libraries for one capability: one JSON mapper, one mocking framework, one assertion library, one HTTP client, one test-data generator, one logging facade and one backend.
- Never add a dependency to make a single failing test compile. Fix the test or the design.
- Never add a dependency speculatively, "for later", or because an example on the internet used it.
- Do not declare a dependency that only a transitive path needs. Do declare one that your own source imports directly, even when it arrives transitively today, because a transitive path can disappear in any upgrade.
- Scope correctly: `test` for test-only libraries, `runtime` for drivers and backends the source never imports, `provided`/`compileOnly` for annotation-only libraries, `annotationProcessor` for processors.

## Versions and dependency management

- Use the Spring Boot BOM as the single source of managed versions, through the parent POM, the Spring Boot Gradle plugin, or an imported BOM.
- Do not pin a version for an artifact the BOM already manages. Overriding it silently desynchronizes an entire tested dependency set.
- When an override is genuinely required, override the BOM property rather than the individual dependency version, and record the reason and a removal condition next to it.
- Pin every plugin version explicitly. An unpinned plugin makes builds non-reproducible.
- Keep version properties in one place, named for the artifact.
- Do not run a blanket "upgrade everything". `application-security` owns upgrade triage.

## Compiler configuration

The compiler settings are what make `modern-java-21` and the mapper architecture work.

- Set the Java release explicitly to the version recorded in the project profile, using the release flag rather than separate source and target settings.
- Enable `-parameters`. Spring uses parameter names for constructor binding, `@ConfigurationProperties`, and query derivation. The Spring Boot parent and Gradle plugin enable it; verify it if the project uses neither.
- Do not enable preview features unless the task explicitly requires them.
- Treat compiler warnings as information, not noise. Do not suppress them globally to make a build quiet.

### Annotation processors are order-sensitive

This is the single most common way to break this stack.

- Declare every processor the project needs in one explicit processor path. For this skill set that is the MapStruct processor and the Spring Boot configuration processor.
- **Lombok is optional.** Nothing here requires it, and the decision belongs in `docs/project-profile.md`. Do not introduce it because an example shows it, and do not remove it from a project that already uses it coherently. If the profile is silent and the repository has no Lombok dependency, the project does not use Lombok.
- When the project does use Lombok alongside MapStruct, the processor order is **Lombok, then `lombok-mapstruct-binding`, then the MapStruct processor**. Without the binding, MapStruct runs before Lombok generates accessors, and it either fails or silently produces mappers that ignore fields.
- Omit a version for any processor the Spring Boot BOM manages, and pin only the artifacts it does not, such as the MapStruct processor. Confirm the build tool actually resolves managed versions on the processor path before relying on it; the reference for each tool states the condition.
- On Maven, declaring `annotationProcessorPaths` disables classpath processor discovery entirely, so every processor must appear in that list. A processor declared only as a dependency stops running.
- Configure the MapStruct unmapped-target policy at the build level so it cannot be forgotten on an individual mapper.
- After any change to a processor, its version, or an entity or mapper it reads, rebuild and **inspect the generated sources**. Do not assume generation succeeded because compilation did.

## Test execution configuration

`spring-boot-testing` requires unit tests, MVC slice tests, persistence slice tests, and full
integration tests, and names the latter `*IntegrationTest`. That suffix matches no default in either
tool, so it must be configured or the suites run in the wrong phase — or silently do not run at all.

- Unit and slice tests run in the fast phase. Integration tests run in a separate, later phase or task.
- On Maven, `*IntegrationTest` also matches Surefire's default patterns, so excluding it from Surefire is required, not optional.
- Make the verification lifecycle fail on integration-test failure. A separate phase that nobody runs is worse than no separation.
- Record the resulting commands in `docs/project-profile.md` so "run the relevant suites" is unambiguous.
- Do not skip tests in any committed configuration or profile, and do not configure a build to ignore test failures.

## Build integrity and packaging

- Commit the wrapper and pin the distribution it downloads. Review any change to the wrapper, its distribution URL, or its checksum as executable code.
- Use only approved artifact repositories over authenticated TLS. Do not add a repository to resolve one artifact without approval.
- Keep the build reproducible: no timestamp-dependent, network-dependent, or machine-dependent behavior in normal builds.
- Do not put credentials, tokens, or environment URLs in build files. Use the platform's secret mechanism.
- Keep packaging aligned with deployment: correct main class, correct layering for container images, correct runtime Java version.
- Do not add code generation, obfuscation, shading, or bytecode manipulation without a concrete requirement; each one breaks stack traces, debugging, or agents.

## Auditing a project for dependencies that do not belong

Run this workflow when the user asks which dependencies are unnecessary, or asks to clean up the
build. Read [dependency audit and removal](references/dependency-audit.md) for the full procedure,
the tool commands per build tool, and the false-positive list.

The short form:

1. Produce the full dependency picture, including transitive paths and scopes.
2. Classify every **declared** dependency into: used directly, used only at runtime, used only in tests, duplicated capability, obsolete, or no evidence of use.
3. Verify each candidate against the false-positive list before proposing removal. Drivers, migration tools, logging backends, annotation processors, and auto-configuration-only starters are used without ever being imported, and every static analyzer reports them as unused.
4. Report the candidates with evidence and let the user decide. Never remove dependencies unasked.
5. Remove approved candidates one at a time, running the full verification after each, so a failure identifies the responsible dependency.

## Anti-patterns

Reject:

- a dependency with no named requirement, or added "for later";
- two libraries providing one capability;
- a version pinned for an artifact the Spring Boot BOM already manages;
- an unpinned build plugin;
- an annotation processor declared as an ordinary dependency, or a processor path missing Lombok's MapStruct binding when both are present;
- Lombok introduced because an example showed it, rather than because the project profile records it;
- integration tests that run in the unit-test phase, or that no phase runs at all;
- skipped tests, ignored test failures, or disabled quality gates in committed configuration;
- credentials, tokens, or environment-specific URLs in build files;
- an unreviewed repository, wrapper, or distribution URL change;
- a blanket dependency upgrade without triage;
- bulk dependency removal based on static-analysis output alone, without runtime verification;
- converting the project to the other build tool as a side effect of an unrelated change.

## Completion checklist

- [ ] The build tool, Java release, and Spring Boot version come from the project profile, or were asked for and recorded.
- [ ] Every added dependency passed the justification gate and duplicates no existing capability.
- [ ] Managed artifacts carry no version; unmanaged ones are pinned with a recorded reason.
- [ ] Every plugin version is pinned.
- [ ] The Java release and `-parameters` are configured explicitly.
- [ ] The annotation processor path lists every processor in the correct order, matches the project's recorded Lombok decision, and the generated sources were inspected after the build.
- [ ] Unit, slice, and integration suites each run in their intended phase, and the verification lifecycle fails on integration-test failure.
- [ ] No credentials, unapproved repositories, or unreviewed wrapper changes were introduced.
- [ ] Any removal was user-approved, applied one dependency at a time, and verified by a full build including integration tests and application startup.

## Primary guidance

- [Spring Boot: Build Systems](https://docs.spring.io/spring-boot/reference/using/build-systems.html)
- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/maven-plugin/index.html)
- [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/gradle-plugin/index.html)
- [Maven Compiler Plugin: Annotation Processing](https://maven.apache.org/plugins/maven-compiler-plugin/examples/pass-compiler-arguments.html)
- [MapStruct: Setting up](https://mapstruct.org/documentation/installation/)
- [Gradle: JVM Test Suite Plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html)
