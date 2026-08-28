---
name: build-and-dependencies
description: Maven and Gradle build configuration and dependency governance for Java 21+ Spring Boot projects. Use when changing build files, dependencies, plugins, versions, compiler settings, annotation processors, test selection, or packaging; when adding or upgrading a dependency; when auditing for unnecessary or duplicated dependencies; and when configuring quality gates such as Checkstyle or Spotless.
---

# Build and Dependencies

A build file is production configuration. It decides what code exists, what runs at startup, what an
attacker can reach, and how long every engineer waits for feedback. Treat a dependency as a
long-term commitment, not a convenience.

This skill is build-tool neutral. Maven and Gradle are both fully supported, and every rule here
applies to whichever one the project uses.

## Coordination with other skills

This skill owns the build files, every dependency and plugin declaration, **all version selection**,
compiler and annotation-processor configuration, test-phase separation, and quality gates.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what, and
it carries the precedence order for a genuine conflict. Read it there rather than from a copy in
this file. The seams this skill crosses most often:

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| Versions | every version choice in the project | no other skill selects a version |
| Compiler settings | the configuration | `modern-java-21` owns the language rules it enables |
| Test phases | the plugin and source-set configuration | `spring-boot-testing` owns which suites must exist |
| Supply chain | the declarations | `application-security` owns provenance, SBOM, and vulnerability triage |

When the user asks for a review rather than a change, produce findings under
`spring-boot-code-review` and do not edit build files.

## Which build decisions block, and which do not

`spring-boot-patterns` owns `docs/project-profile.md` and defines the decision tokens `ASK`,
`RESOLVE`, and `UNDECIDED`. This skill owns the classification of every build and version decision
into those tokens, and no other skill reclassifies one.

| Decision | Token | Why |
| --- | --- | --- |
| Build tool | `ASK` | Maven and Gradle are both correct, and nothing in an empty repository decides between them |
| Spring Boot generation | `ASK` | 3 and 4 are both supported; the choice follows the platform, the team, and the upgrade appetite, not a lookup |
| Support model for the chosen branch | `ASK` | Whether the project relies on open-source updates or on a commercial subscription. This is a procurement fact, not a lookup, and it is what makes a branch past its OSS end date a decision rather than an accident |
| Uses Lombok | `ASK` | A project-wide style commitment, not a technical necessity |
| Java release | `RESOLVE` | A supported LTS at or above the floor is a correct answer that only needs looking up |
| Spring Boot version within the chosen generation | `RESOLVE` | The current stable release of that branch |
| Support end date of the chosen branch | `RESOLVE` | Published by the project; look it up rather than assuming the branch is current |
| Nullability enforcement | `ASK` | Whether the JSpecify contract is checked by IDE and review or by NullAway on Error Prone. Both are defensible; the second changes every compilation, so it is not a lookup |
| Every plugin and tool version | `RESOLVE` | Checkstyle, Spotless, MapStruct, the OpenAPI generator, the Lombok binding |

An `ASK` blocks the task until the user answers. A `RESOLVE` never blocks: look it up, record it with
the date in the profile's resolved-versions table, and state in the response what was chosen and why,
so the user overrides once instead of being asked every time.

## Determine the build tool before editing

- If the repository already contains `pom.xml` or `build.gradle`/`build.gradle.kts`, that is the answer. Record it in the profile if it is missing there.
- If the repository contains neither — an empty repository, or a skeleton with nothing generated yet — ask, per the `ASK` rule above. Do not pick one.
- Never introduce a second build tool, and never convert an existing project from one to the other unless conversion is the explicit task.

## Choose versions that are still supported

This skill owns version selection for the whole skill set. No other skill picks a version, and none
restates these rules.

- Look up the current release before choosing, rather than reusing a number from documentation, from a tutorial, or from memory. Any number written down is stale within months; the lookup is cheap and the answer is authoritative.
- **When the lookup is impossible** — no network, no registry, or a result you cannot confirm — record `UNDECIDED` with the reason and say so in the handoff. Do not write a remembered number into a build file or the profile. A guessed version is worse than a missing one: it looks resolved, so nobody checks it again, and it can silently name an end-of-life branch.
- Verify that the branch is still receiving updates, and record the version, the date its support ends, and the support model in `docs/project-profile.md`.
- **A branch past its open-source end date is a decision, never a default.** It takes no open-source security patches, so a project on one is relying on a commercial subscription — which is a real and common arrangement, and is fine when someone has actually bought it. Establish that before writing the version down: ask the user whether the project has commercial support for that branch, record the answer in the profile's support-model row, and say so in the handoff. Where nobody can confirm it, the branch is unsupported and the answer is a supported branch instead.
- **Both generations of this skill set stay valid regardless.** Support status decides which branch a given project should sit on; it does not remove a generation from the set. A project on an older branch — because the platform pins it, because a subscription covers it, or because the upgrade is scheduled — is a first-class case here, and every rule with a per-generation form states both.
- **Support dates are written-down values, so they go stale exactly like version numbers.** Look up the current support table at setup and again before any upgrade decision. Never repeat a support date from memory, from this skill, or from a project that was set up earlier.
- **Java.** Java 21 is the floor this skill set is written against, and everything here works on it. Prefer the current LTS release when nothing constrains the project — a supported framework version, a platform image, or a customer requirement — and record the chosen release in the profile. Do not exceed what the chosen Spring Boot generation supports.
- **Spring Boot.** The generation is the user's answer; the version within it is a lookup. When an existing project sits on an older supported branch, stay there and raise the upgrade separately; do not change the generation as a side effect of an unrelated task.
- When the project already records versions, use them. This section governs the choice, not a re-litigation of a choice already made.

## Both Spring Boot generations are supported

The set is written for Spring Boot 3.x and 4.x, and the profile records which one applies. Where a
rule genuinely differs, the skill that owns the topic states both cases; nothing here assumes a
generation silently.

- **This skill owns the catalogue of what each thing is called in each generation.** [Generation differences](references/generation-differences.md) carries starter and module coordinates, relocated annotations, renamed properties, and the per-minor-line notes. Other skills state the behavior they own and link there; none repeats the table.
- **The generation is not the whole answer.** Spring Boot 4's minor lines carry major versions of Spring Security, Spring Data, and the rest of the portfolio, so a coordinate that holds on one 4.x line can differ on the next. Record the minor line in the profile, and read portfolio versions from the effective dependency tree.
- **The catalogue is a written-down value, so it obeys the same rule as a version number.** Verify a row against the effective dependency tree and the upstream migration guide before relying on it for the first time, and again before any generation upgrade. Never treat a row as current because a coordinate resolves — a renamed artifact can keep publishing under its old name for a whole release line.
- **The most expensive Spring Boot 4 trap:** because auto-configuration is modularized, a third-party library without its Spring Boot module is inert. The application starts, the build stays green, the tests pass, and the feature never runs. Verify wiring by observing behavior, never by observing that a dependency resolves.
- A snippet in these references is written against the generation it names; when it names none, it holds for both. Treat a generation change as its own task with its own verification.

## Reference routing

Read only what the change requires:

- Read [Maven configuration](references/maven-configuration.md) when the project uses Maven.
- Read [Gradle configuration](references/gradle-configuration.md) when the project uses Gradle.
- Read [dependency audit and removal](references/dependency-audit.md) when adding, replacing, or removing a dependency, and whenever the user asks which dependencies do not belong.
- Read [quality gates](references/quality-gates.md) when setting up or changing Checkstyle, Spotless, editor configuration, dependency enforcement, nullability checking, or any other automated check.
- Read [generation differences](references/generation-differences.md) when the project is on Spring Boot 4, when a declaration or property does not resolve as a Boot 3 example suggests, or when an upgrade between generations is the task.

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
- Declare `org.jspecify:jspecify` as a direct dependency on both generations, because `modern-java-21` requires its annotations in every main-source package. **Whether it carries a version depends on the generation**: the Spring Boot 4 BOM manages it, so declare it without one; the Spring Boot 3 BOM does not, so declare it with a `RESOLVE`d version recorded in the profile. On Spring Boot 4 it also arrives transitively through `spring-core`, which is not a reason to leave it undeclared — a direct dependency is declared directly. Declare it in the **default compile scope on both generations**, as a deliberate exception to the annotation-only-library rule above: the annotations have runtime retention, Spring already puts the artifact on the Spring Boot 4 runtime classpath, and narrowing it to `compileOnly` on Spring Boot 3 alone would make the two generations differ for no benefit.
- Do not run a blanket "upgrade everything". `application-security` owns upgrade triage.

## Compiler configuration

The compiler settings are what make `modern-java-21` and the mapper architecture work.

- Set the Java release explicitly to the version recorded in the project profile, using the release flag rather than separate source and target settings.
- Enable `-parameters`. Spring uses parameter names for constructor binding, `@ConfigurationProperties`, and query derivation. The Spring Boot parent and Gradle plugin enable it; verify it if the project uses neither.
- Do not enable preview features unless the task explicitly requires them.
- Treat compiler warnings as information, not noise. Do not suppress them globally to make a build quiet.

### Annotation processors are order-sensitive

This is the single most common way to break this stack, and three of its failures are silent.

- **Declare every processor in one explicit processor path.** On Maven, declaring `annotationProcessorPaths` disables classpath discovery entirely, so a processor listed only as a dependency stops running and generates nothing.
- **Lombok is optional.** Nothing here requires it, and the decision belongs in `docs/project-profile.md`. Never introduce it because an example shows it, and never remove it from a project that uses it coherently. Where the profile is silent and the repository has no Lombok dependency, apply the template's fallback — no Lombok — and record it.
- **With Lombok and MapStruct together the order is Lombok, then `lombok-mapstruct-binding`, then the MapStruct processor.** Without the binding, MapStruct runs before Lombok generates accessors and either fails or quietly produces mappers that ignore fields.
- **After any change to a processor, its version, or an entity or mapper it reads, rebuild and inspect the generated sources.** A green compile does not prove the generator produced what you expected.

The per-tool references carry the processor-path declarations, which artifacts the BOM manages, and
the MapStruct policy flag: [Maven](references/maven-configuration.md) or
[Gradle](references/gradle-configuration.md).

## Test execution configuration

`spring-boot-testing` states which suites must exist, how they are separated, and what must be
recorded in `docs/project-profile.md`. This skill implements that separation in the build. Read the
requirements there; do not reinterpret them here.

Implementation facts that belong to the build tool:

- On Maven, `*IntegrationTest` also matches Surefire's default patterns, so excluding it from Surefire is required, not optional. Without that exclusion the integration suite runs twice, once in the wrong phase.
- On Gradle, the suffix matches no default task, so the separate suite must be registered and wired into the check lifecycle explicitly.
- Do not skip tests in any committed configuration or profile, and do not configure a build to ignore test failures.

## Quality gates

A rule a tool can check must fail the build; a rule a tool cannot check belongs to
`spring-boot-code-review`. This skill owns the configuration that makes the first group real.

- Configure the gates before the first feature, not after. Retrofitting `RequireThis` or an import order onto an existing codebase is expensive; applying it from the first commit costs nothing.
- Run them in order: formatter, then static analysis, then compile, then tests. A gate that runs after the test suite wastes the slowest part of the cycle.
- Set every gate to fail the build. A warning nobody must fix is not a gate.
- Commit the editor configuration alongside the formatter configuration. Checkstyle reports a violated import order; it does not stop an IDE from reintroducing it on the next "Optimize Imports".
- Use no baseline file and no suppressions. This project rejects legacy code, so there is nothing to grandfather, and a suppression file is where a standard goes to die. If a rule does not fit, change the rule and say so in review.
- Label which rules are gated and which are review-only, so nobody mistakes a green build for compliance.
- Nullability checking is the one gate this skill treats as a project decision rather than a default. The profile's `Nullability enforcement` row records it; the reference states what each level costs and catches. Never add Error Prone as a side effect of another task.

[Quality gates](references/quality-gates.md) explains the mapping from each project rule to the tool
that enforces it, the dependency enforcement rules, and the Spotless setup. The configurations
themselves are files, not snippets: copy [`assets/checkstyle.xml`](assets/checkstyle.xml),
[`assets/editorconfig`](assets/editorconfig), and the two IDE code-style assets rather than
regenerating them. A regenerated near-copy is the usual reason a gate has to be weakened later.

## Verify a Spring Boot 4 project deliberately

Most generation defects compile, start, and pass the build, so a green build is not evidence. When
the project is on Spring Boot 4, check these explicitly rather than assuming the build would have
caught them:

- A removed annotation still in the source: `@MockBean`, `@SpyBean`, `@JsonComponent`, `@JsonMixin`, a Spring Security matcher that no longer exists.
- A renamed starter still declared under its Spring Boot 3 name.
- A configuration property still written under its Boot 3 key, where it now binds to nothing and reports nothing.
- A third-party library declared without its Spring Boot module, which is the silent-wiring case.

[Generation differences](references/generation-differences.md) lists what each is called in each
generation. Route every finding to the skill that owns the topic rather than fixing it from here.

## Build integrity and packaging

- Commit the wrapper and pin the distribution it downloads. Review any change to the wrapper, its distribution URL, or its checksum as executable code.
- Use only approved artifact repositories over authenticated TLS. Do not add a repository to resolve one artifact without approval.
- Keep the build reproducible: no timestamp-dependent, network-dependent, or machine-dependent behavior in normal builds.
- Do not put credentials, tokens, or environment URLs in build files. Use the platform's secret mechanism.
- Keep packaging aligned with deployment: correct main class, correct layering for container images, correct runtime Java version.
- Do not add code generation, obfuscation, shading, or bytecode manipulation without a concrete requirement; each one breaks stack traces, debugging, or agents.

## Auditing a project for dependencies that do not belong

Run this only when the user asks which dependencies are unnecessary or asks to clean up the build —
removing a dependency is a behavioral change, never a side effect of another task. The procedure, the
per-tool commands, and the false-positive list are in
[dependency audit and removal](references/dependency-audit.md).

Two rules hold wherever the audit runs: **verify every candidate against the false-positive list
first** — drivers, migration tools, logging backends, annotation processors, and
auto-configuration-only starters are used without ever being imported, and every static analyzer
calls them unused — and **report candidates with evidence and let the user decide**, then remove one
at a time so a failure identifies the responsible dependency.

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
- a Checkstyle suppression file, a baseline, or a `@SuppressWarnings` used to silence a project rule;
- a full source formatter that reformats the codebase to its own conventions;
- credentials, tokens, or environment-specific URLs in build files;
- an unreviewed repository, wrapper, or distribution URL change;
- a blanket dependency upgrade without triage;
- bulk dependency removal based on static-analysis output alone, without runtime verification;
- converting the project to the other build tool as a side effect of an unrelated change.

## Completion checklist

- [ ] The build tool came from the repository or from the user, never from a guess.
- [ ] The Java release and Spring Boot version come from the profile, or were resolved to a supported release and recorded there with the date that branch loses support.
- [ ] Every added dependency passed the justification gate and duplicates no existing capability.
- [ ] Managed artifacts carry no version; unmanaged ones are pinned with a recorded reason.
- [ ] Every plugin version is pinned.
- [ ] The Java release and `-parameters` are configured explicitly.
- [ ] The annotation processor path lists every processor in the correct order, matches the project's recorded Lombok decision, and the generated sources were inspected after the build.
- [ ] Unit, slice, and integration suites each run in their intended phase, and the verification lifecycle fails on integration-test failure.
- [ ] Quality gates run before the tests, fail the build, and were not weakened by a suppression or baseline.
- [ ] The build fails when `docs/project-profile.md` is absent or the JDK is below the recorded release.
- [ ] No credentials, unapproved repositories, or unreviewed wrapper changes were introduced.
- [ ] Any removal was user-approved, applied one dependency at a time, and verified by a full build including integration tests and application startup.

## Primary guidance

- [Spring Boot: Build Systems](https://docs.spring.io/spring-boot/reference/using/build-systems.html)
- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/maven-plugin/index.html)
- [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/gradle-plugin/index.html)
- [Maven Compiler Plugin: Annotation Processing](https://maven.apache.org/plugins/maven-compiler-plugin/examples/pass-compiler-arguments.html)
- [MapStruct: Setting up](https://mapstruct.org/documentation/installation/)
- [Gradle: JVM Test Suite Plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html)
