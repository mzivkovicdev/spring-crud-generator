# Quality gates

Use this reference when setting up or changing the automated enforcement of project rules. Apply
every rule from `../SKILL.md`. The owner of each *rule* is the skill that defines it; this reference
owns only the configuration that makes the rule fail a build.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [What is gated and what is not](#what-is-gated-and-what-is-not)
2. [Generated code and the gates](#generated-code-and-the-gates)
3. [Layer 1: editor and formatter](#layer-1-editor-and-formatter)
4. [Layer 2: Checkstyle](#layer-2-checkstyle)
5. [Modules to verify before relying on them](#modules-to-verify-before-relying-on-them)
6. [Layer 3: dependency and runtime guards](#layer-3-dependency-and-runtime-guards)
7. [Nullability checking](#nullability-checking)
8. [Wiring the gates into the build](#wiring-the-gates-into-the-build)
9. [No baseline, no suppressions](#no-baseline-no-suppressions)

## What is gated and what is not

A rule a tool can check must fail the build. A rule a tool cannot check belongs to
`spring-boot-code-review`, and must be labelled as such so nobody mistakes a green build for
compliance.

| Rule | Owner skill | Enforced by |
| --- | --- | --- |
| Import order, no wildcards, no unused imports | `modern-java-21` | Spotless + Checkstyle + IDE config |
| Blank line between the static block and the first type group | `modern-java-21` | Spotless + IDE config only — Checkstyle's `ImportOrder` does not check this boundary |
| Logger field named `LOGGER` | `observability-and-logging` | Checkstyle regex |
| Qualify instance access with `this.` | `modern-java-21` | Checkstyle `RequireThis` |
| No `var` | `modern-java-21` | Checkstyle regex |
| `final` parameters and single-assignment locals | `modern-java-21` | Checkstyle `FinalParameters`, `FinalLocalVariable` |
| At most seven parameters | `modern-java-21` | Checkstyle `ParameterNumber` |
| Method over 100 lines, class over 1000 lines | `modern-java-21` | Checkstyle `MethodLength`, `FileLength` |
| No `System.out`, `System.err`, `printStackTrace` | `observability-and-logging` | Checkstyle regex |
| No concatenation in a log call | `observability-and-logging` | Checkstyle regex |
| Identifier naming form | `project-naming-conventions` | Checkstyle naming modules |
| Integration tests run in their own phase | `spring-boot-testing` | Surefire/Failsafe or Gradle suites |
| Banned and duplicated dependencies, JDK version, profile presence | `build-and-dependencies` | `maven-enforcer-plugin` or Gradle constraints |
| Metric tag cardinality | `observability-and-logging` | `MeterFilter` at runtime |

Deliberately **not** gated:

- layer boundaries, entity leakage into the transport layer, field injection, and annotation placement. A per-file static check cannot observe a dependency between classes, so these remain a review responsibility under `spring-boot-patterns` and `spring-boot-code-review`. Check them deliberately rather than assuming a green build covered them;
- whether a `@Nullable` annotation is *correct*. A checker proves the code agrees with the annotations; only review proves the annotations agree with reality, and an annotation added to silence a warning is exactly the case a green build cannot catch;
- whether a package carries `@NullMarked` at all, unless the project deliberately turns on the optional check described under [Nullability checking](#nullability-checking). This is the one rule in this file that a project may choose to gate or not, and the reason is stated there;
- whether a name reveals intent;
- whether a method between 41 and 60 lines should have been split;
- whether a failure is logged exactly once;
- whether a test asserts real behavior rather than a hypothetical;
- whether a dependency has a real justification;
- whether a Javadoc sentence is useful.

Presence of Javadoc is intentionally not gated either, with one narrow exception noted under
[Nullability checking](#nullability-checking), where a Javadoc module is borrowed to prove a *file*
exists rather than to require documentation. The policy in `modern-java-21` is conditional
— required on service contracts, not on TOs, records, or overrides — and Checkstyle cannot express
that distinction without producing noise that trains people to ignore it. Javadoc *correctness* is
gated; Javadoc *presence* is a review question.

## Generated code and the gates

**This is the canonical rule for how generated sources are gated.** Every skill that produces or
consumes generated output links here instead of stating its own version; a second statement is how a
project ends up gating generated code in one task and exempting it in the next.

Generated output is not exempt from verification, and it is not subject to human-style formatting
rules. Split the gates by what they prove:

| Gate | On generated sources | Why |
| --- | --- | --- |
| Compilation | **Required** | A generated source that does not compile is a broken build, whoever wrote it |
| Contract, schema, or document validation | **Required** | The input the generator read is the artefact under review; validate it before generating |
| Banned dependencies, forbidden imports, security enforcement | **Required** | An unsafe dependency is unsafe regardless of who declared it |
| Deterministic regeneration | **Required** | Two runs from one input produce identical output, or the artefact is not reproducible |
| Checkstyle, Spotless, import order, Javadoc, line length | **May be excluded** | These encode how a person writes Java. The project does not control the generator's formatter, so a failure here names no action anyone can take |
| Hand-editing the output | **Forbidden** | It is overwritten on the next generation, silently |

Two consequences follow, and both are rules:

- **Excluding a directory from the formatter is not excluding it from the build.** A project that
  points Spotless and Checkstyle away from the generated source root still compiles it, still runs
  the enforcer against its dependencies, and still fails when regeneration is not deterministic.
- **A defect in generated output is fixed at its source, and the source depends on who owns the
  generator.** For a project-owned template, the fix is in the template. For a third-party generator
  such as the OpenAPI generator, the project owns no template: the fix is in the input document or
  in the generator configuration, and `rest-api-contract` states that in full. "Fix it in the
  template" is not a universal instruction, and following it literally against a third-party
  generator leads to editing the output — the one thing the table forbids.

Generated sources belong in the build output directory and are not committed. What *is* committed is
the input the generator reads, and that input is reviewed like source.

## Layer 1: editor and formatter

Checkstyle reports a violated import order. It does not stop an IDE from reintroducing it on every
"Optimize Imports". Commit the editor configuration, or the standard is undone faster than it is
enforced.

### Copy the editor configuration, do not retype it

Three files, all committed, all project-scoped, none containing personal settings:

| Asset | Copy to | What it does |
| --- | --- | --- |
| [`editorconfig`](../assets/editorconfig) | `.editorconfig` | Charset, line endings, indentation, trailing whitespace |
| [`idea-codeStyleConfig.xml`](../assets/idea-codeStyleConfig.xml) | `.idea/codeStyles/codeStyleConfig.xml` | Turns on per-project code style |
| [`idea-Project.xml`](../assets/idea-Project.xml) | `.idea/codeStyles/Project.xml` | The seven-group import layout and the on-demand thresholds |

Copy them verbatim. Retyping or regenerating them produces a configuration that is *nearly* the
one this skill set enforces, and a near-miss here is worse than nothing: the build fails on files the
IDE just "fixed", and the first response is usually to weaken the gate rather than to correct the
editor.

The two on-demand thresholds set to 999 in `idea-Project.xml` are what prevent the IDE from
collapsing imports into `java.util.*`. That single setting is the most common reason a project's
import order silently degrades, so verify it survived the copy.

### Spotless

Spotless applies import order and source hygiene automatically, so nobody argues about it in review.

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <!-- Declared in the properties block in maven-configuration.md. Not managed by the
         Spring Boot parent, so this version is required. -->
    <version>${spotless-plugin.version}</version>
    <configuration>
        <java>
            <importOrder>
                <order>\#,java,jakarta,javax,com,org,</order>
            </importOrder>
            <removeUnusedImports/>
            <trimTrailingWhitespace/>
            <endWithNewline/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>validate</phase>
        </execution>
    </executions>
</plugin>
```

```kotlin
spotless {
    java {
        importOrder("\\#", "java", "jakarta", "javax", "com", "org", "")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

Notes:

- `\#` is Spotless's marker for static imports; the trailing empty entry is the catch-all group for everything that is not `java`, `jakarta`, `javax`, `com`, or `org`. That is exactly the order `modern-java-21` requires.
- Do **not** add `googleJavaFormat()`, `palantirJavaFormat()`, or `eclipse()`. A full formatter reformats the whole codebase to its own indentation and wrapping, which conflicts with the conventions these skills already assume, and it will fight the IDE configuration above. Spotless is used here for import order and hygiene only.
- `spotless:check` runs in `validate`, so it fails before compilation. `spotless:apply` is the local fix command; record both in `docs/project-profile.md`.

## Layer 2: Checkstyle

Copy [`checkstyle.xml`](../assets/checkstyle.xml) to `config/checkstyle/checkstyle.xml`. Severity is
`error` everywhere: a warning nobody has to fix is not a gate.

Both versions are pinned, and they are two different artifacts:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <!-- Both properties are declared in maven-configuration.md. Neither the plugin nor the
         tool is managed by the Spring Boot parent. -->
    <version>${checkstyle-plugin.version}</version>
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>${checkstyle.version}</version>
        </dependency>
    </dependencies>
    <configuration>
        <configLocation>config/checkstyle/checkstyle.xml</configLocation>
        <includeTestSourceDirectory>true</includeTestSourceDirectory>
        <violationSeverity>error</violationSeverity>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle-validate</id>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>validate</phase>
        </execution>
    </executions>
</plugin>
```

Without the `<dependencies>` block the plugin runs whichever Checkstyle it was built against, which
is usually well behind the version the configuration was written for. That is the usual reason a
`record`-related module behaves differently on a developer machine and in CI while the configuration
file is byte-identical.

`includeTestSourceDirectory` is on because the rules in
[`checkstyle.xml`](../assets/checkstyle.xml) apply to test sources too — `modern-java-21` says so
explicitly, and a gate that skips `src/test/java` enforces the standard on half the codebase.

The file is the gate for the rules other skills define, and it is annotated with which skill owns
each block. Do not regenerate it from memory and do not trim it to make an existing codebase pass —
if a rule does not fit the project, change the rule in its owning skill and say so, per `../SKILL.md`.

The modules it enables, and who owns each rule:

| Block | Owning skill | Enforces |
| --- | --- | --- |
| Imports | `modern-java-21` | No star imports, no unused or redundant imports, the seven-group order and the blank lines between type groups |
| Explicitness | `modern-java-21` | `this.` qualification, `final` parameters and single-assignment locals, no `var` |
| Size and shape | `modern-java-21` | Method length, parameter count, one top-level class per file |
| Exceptions | `modern-java-21` | No catching `Error` or `Throwable`, no empty catch, `equals`/`hashCode` pairing |
| Identifier form | `project-naming-conventions` | Package, type, method, member, parameter, constant naming and abbreviation length |
| Log hygiene | `observability-and-logging` | No `System.out`, `System.err`, `printStackTrace`, or concatenation in a log call, and the logger field is named `LOGGER` |
| Javadoc correctness | `modern-java-21` | Well-formed Javadoc where it exists; presence is deliberately not gated |

Configuration decisions worth knowing before someone "fixes" them:

- **`AbbreviationAsWordInName` is set to `1`** so that two consecutive capitals are permitted. That is what allows the project's `TO` suffix, `UserTO` and `UserCreateTO`, while still rejecting `HTTPClient`. `ignoreStaticFinal` keeps `UPPER_SNAKE_CASE` constants out of scope.
- **`ImportOrder`'s `separated` covers type groups only.** Its own documentation scopes it to type import groups, so the blank line between the static block and the first type group is not checked. `separatedStaticGroups` is **not** the fix — it separates static groups from one another and does nothing while `staticGroups` is unset. That boundary is enforced by Spotless, which formats it, and preserved by the committed IDE configuration; the table above records that division so nobody reads a green Checkstyle run as proof of the whole rule.
- **The `var` gates are anchored at the start of a statement**, which is why they read oddly. `ignoreComments` excludes comments but not string literals, so an unanchored pattern fires on a fixture whose *data* contains `var x = 1`. A gate that fails correct code is worse than one with a known blind spot, because the first response to it is to weaken it. The trade is deliberate: a `var` written mid-line, inside a single-line block, slips past the gate and is caught in review instead.
- **The logger-name gate exists to keep the concatenation gate honest.** The concatenation pattern matches the literal identifier `LOGGER`, so a logger named `log` would be silently exempt from it. Enforcing the name is what makes the second gate mean something. It also matches the single logger declaration form `observability-and-logging` requires.
- **`IllegalCatch` deliberately allows `RuntimeException`.** Catching it to tag an observation, increment a failure counter, or record an outcome and then rethrow is a required pattern in `observability-and-logging`. `Error` and `Throwable` stay banned.
- **`MagicNumber` is not enabled.** In a Spring project it fires mostly on validation annotations and produces more noise than value; the real rule — shared bounds declared once — is covered by review and by the constants the skills already require.
- **The log-concatenation regex is a heuristic.** It catches the common case and will not catch every one. It is a gate, not a proof.
- **`RequireThis` with `validateOnlyOverlapping=false`** is what makes the `this.` rule real. It is the single noisiest module on an existing codebase and the single most valuable one on a new project, which is why it goes in before the first feature.

## Modules to verify before relying on them

The configuration above is the core. The following modules are left out because they behave in ways
that depend on the installed Checkstyle version and on constructs this project uses heavily. Verify
each on the first real run, then adopt or discard it deliberately.

| Module | What to verify | Why it is uncertain |
| --- | --- | --- |
| `VisibilityModifier` (omitted) | Its behaviour on `record` components and on Mockito fixture fields before adding it | Records declare implicitly private final fields, and older versions reported them. The rule it would enforce is already covered by `modern-java-21` in review. |
| `HideUtilityClassConstructor` (omitted) | Whether it fires on `@Configuration` classes that declare only static `@Bean` methods | Such a class is not a utility class, but it matches the module's shape. `ApiPaths` and `PaginationConstraints` already declare private constructors by convention. |
| `MatchXpath` for `var` (omitted) | Whether a query such as `//VARIABLE_DEF/TYPE/IDENT[@text='var']` matches every declaration form the project uses, including enhanced-for and try-with-resources | It reads the syntax tree, so it has none of the regexes' string-literal blind spot and is the better rule if it works. The risk is the opposite one: a query that matches nothing fails **open** and the gate quietly stops enforcing anything. Verify against real violations before replacing the regexes, not after. |
| `JavadocPackage` (omitted) | Whether the project wants a build failure for a package with no `package-info.java`, and that the fileset excludes test sources | It is the only Checkstyle module that proves the file `modern-java-21` requires for `@NullMarked` exists. It is a Javadoc-presence module, which this file otherwise leaves to review, so adopting it is a deliberate exception rather than an oversight. [Nullability checking](#nullability-checking) states when it is worth it. |
| `FinalLocalVariable` (enabled) | Its behaviour on enhanced-for variables and try-with-resources on the project's Checkstyle version | `validateEnhancedForLoopVariable` is on, which is what `modern-java-21` wants, but the module's treatment of resource variables and of locals assigned in every branch of a conditional has moved between versions. It is enabled because the rule matters from the first commit; confirm it behaves as expected on the first real run rather than after the first argument about it. |

Adopt a module by moving it into the main configuration and running a full build. Do not adopt one
because it sounds useful; a gate that produces false positives teaches people to ignore the tool,
which costs more than the rule was worth.

## Layer 3: dependency and runtime guards

### Enforcer rules

One plugin declaration, one execution, one `<rules>` block. Do not split these across several
executions; a second floating `<rules>` block is the most common way this configuration ends up
half-applied.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <!-- Declared in maven-configuration.md. The Spring Boot parent does not manage this
         plugin, so without a version the build is not reproducible. -->
    <version>${enforcer-plugin.version}</version>
    <executions>
        <execution>
            <id>enforce-project-rules</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <requireJavaVersion>
                        <version>[${java.version},)</version>
                    </requireJavaVersion>
                    <requireFilesExist>
                        <files>
                            <file>${maven.multiModuleProjectDirectory}/docs/project-profile.md</file>
                        </files>
                        <message>docs/project-profile.md is missing. Create it from the template
                                 before building; see the project README.</message>
                    </requireFilesExist>
                    <dependencyConvergence/>
                    <requireUpperBoundDeps/>
                    <bannedDependencies>
                        <excludes>
                            <exclude>com.google.code.gson:gson</exclude>
                            <exclude>org.modelmapper:modelmapper</exclude>
                            <exclude>com.github.dozermapper:*</exclude>
                            <exclude>org.powermock:*</exclude>
                            <exclude>junit:junit</exclude>
                            <exclude>commons-logging:commons-logging</exclude>
                            <exclude>log4j:log4j</exclude>
                        </excludes>
                    </bannedDependencies>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Five decisions in that block are deliberate:

- **The `<version>`.** It is easy to assume the Spring Boot parent manages every Maven plugin. It manages a short list — the compiler and Failsafe among them — and Enforcer, Surefire, Checkstyle, and Spotless are not on it. Verify with `./mvnw help:effective-pom` rather than by whether the build happens to work.

- **`${java.version}` rather than a literal.** The Java release is recorded in the project profile and declared once as a property, per [Maven configuration](maven-configuration.md). A literal here would be a second source of truth that silently disagrees with the compiler setting. Note that this rule checks the JDK **running Maven**, which is a different thing from `maven.compiler.release`; both matter, because a toolchain mismatch produces different bytecode with no visible failure.
- **`${maven.multiModuleProjectDirectory}` rather than `${project.basedir}`.** The profile lives once at the repository root. `project.basedir` resolves per module, so in a multi-module build every submodule would look for its own copy and fail.
- **The message names a path, not a skill.** A developer reading a build failure has no idea what `spring-boot-patterns` is; skills are agent-facing, build output is human-facing. Point at something a person can open.
- **The profile rule proves existence only**, not that the file is filled in correctly. That stays a review responsibility. It is still worth having, because a missing profile is exactly the case that silently produces an inconsistent codebase.

The exclusion list encodes the duplicated-capability rule from
[dependency audit and removal](dependency-audit.md): one JSON library, one mapping library, one
mocking framework, one JUnit, one logging facade. Extend it whenever an audit removes a duplicate,
so the same library cannot return.

### Metric tag cardinality

Cardinality is a runtime property, so no static tool can enforce it. A `MeterFilter` can, and it
degrades one meter instead of the monitoring backend:

```java
public class MetricsConfiguration {

    private static final int MAXIMUM_ALLOWED_TAG_VALUES = 100;
    private static final String ALL_METERS = "";

    @Bean
    MeterFilter boundedTagsMeterFilter() {
        return MeterFilter.maximumAllowableTags(
                ALL_METERS, "userId", MAXIMUM_ALLOWED_TAG_VALUES, MeterFilter.deny());
    }
}
```

The empty meter-name prefix applies the cap to every meter. `userId` stands for any tag key that
could plausibly grow; register one filter per such key, and choose the cap from what the metrics
backend can carry, not from what the application currently produces.

Apply it per tag key that could plausibly grow, and pair it with a denied-meter alert so an
accidental unbounded tag is visible rather than silent. This is a safety net, not permission to
relax the rule in `observability-and-logging`.

## Nullability checking

`modern-java-21` requires JSpecify annotations on every main-source package. How hard that contract
is enforced is a project decision recorded in `docs/project-profile.md` as **Nullability
enforcement**. Both rows below are legitimate long-term answers; the template's `Fallback` column
says which one applies while the row is unanswered.

| Enforcement | What it costs | What it catches |
| --- | --- | --- |
| IDE and review | nothing; IntelliJ IDEA understands JSpecify out of the box, Eclipse needs configuration | mistakes the author sees while typing, and whatever review notices |
| NullAway on Error Prone | an Error Prone compiler plugin in the build, and a first pass of real fixes | every violation, on every build, for everyone |

Two configuration points decide whether NullAway is usable rather than merely present:

- Set `NullAway:OnlyNullMarked=true`. Without it the checker reports on unmarked code as well, which on a partially migrated repository produces a backlog nobody reads.
- Leave `NullAway:JSpecifyMode` off unless the build runs on a JDK 22 or later toolchain, which is what that mode requires. The Java *release* stays whatever the profile records; this is a statement about the JDK the build runs on, not about the bytecode it emits.

Whichever row the project picks, record it, and never introduce Error Prone as a side effect of
another task — it changes every compilation in the build and belongs in a change of its own.

Neither row proves that a package was *marked*: NullAway with `OnlyNullMarked=true` deliberately
ignores unmarked code, so a package missing its `package-info.java` is silently exempt from the very
check meant to cover it. Checkstyle's `JavadocPackage` closes that hole — it fails when a package has
no `package-info.java` — but enable it only if the project accepts what it implies. It is a
Javadoc-presence module, and this set otherwise leaves Javadoc presence to review on purpose; here it
is used for file presence, not for documentation, and it is scoped to main sources to match the
marking rule `modern-java-21` states. Where the project does not enable it, a missing
`package-info.java` is a review item, and it belongs on the review checklist rather than being
assumed.

Nullability of generic types and generic methods is not yet fully checked by NullAway. A clean
NullAway run is not evidence that a generic signature is annotated correctly.

## Wiring the gates into the build

Order matters: format first, then static analysis, then compile, then tests. A build that runs
Checkstyle after the tests wastes the slowest part of the cycle on code that was never going to
merge.

**Maven.** Bind `spotless:check` to `validate` and `checkstyle:check` to `validate` as well, with
`failOnViolation` true and `violationSeverity` set to `error`. Point it at
`config/checkstyle/checkstyle.xml` and include test sources. Do not bind either to `verify`.

**Gradle.** Apply the `checkstyle` plugin with `toolVersion` pinned, `maxWarnings = 0`,
`ignoreFailures = false`, and `configDirectory` set to `config/checkstyle`. Spotless and Checkstyle
tasks are already wired into `check`.

Record the resulting commands in `docs/project-profile.md`, including the local auto-fix command, so
the first response to a failed gate is to run the fixer rather than to disable the gate.

## No baseline, no suppressions

On a greenfield project every gate goes to `error` from the first commit. There is no baseline file,
no `suppressions.xml`, and no `@SuppressWarnings("checkstyle:...")`.

This is deliberate. A suppression file is where a standard goes to die: it starts as three
justified entries, becomes the default response to a failing gate, and within a year nobody knows
which entries were ever justified. Because this project also rejects legacy code, there is nothing
to grandfather, so the usual argument for a baseline does not apply.

If a rule genuinely does not fit the project, change the rule in the configuration and say so in
review. Do not suppress it at the call site.
