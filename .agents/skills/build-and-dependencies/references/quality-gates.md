# Quality gates

Use this reference when setting up or changing the automated enforcement of project rules. Apply
every rule from `../SKILL.md`. The owner of each *rule* is the skill that defines it; this reference
owns only the configuration that makes the rule fail a build.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [What is gated and what is not](#what-is-gated-and-what-is-not)
2. [Layer 1: editor and formatter](#layer-1-editor-and-formatter)
3. [Layer 2: Checkstyle](#layer-2-checkstyle)
4. [Layer 3: dependency and runtime guards](#layer-3-dependency-and-runtime-guards)
5. [Wiring the gates into the build](#wiring-the-gates-into-the-build)
6. [No baseline, no suppressions](#no-baseline-no-suppressions)

## What is gated and what is not

A rule a tool can check must fail the build. A rule a tool cannot check belongs to
`spring-boot-code-review`, and must be labelled as such so nobody mistakes a green build for
compliance.

| Rule | Owner skill | Enforced by |
| --- | --- | --- |
| Import order, no wildcards, no unused imports | `modern-java-21` | Spotless + Checkstyle + IDE config |
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
- whether a name reveals intent;
- whether a method between 41 and 60 lines should have been split;
- whether a failure is logged exactly once;
- whether a test asserts real behavior rather than a hypothetical;
- whether a dependency has a real justification;
- whether a Javadoc sentence is useful.

Presence of Javadoc is intentionally not gated either. The policy in `modern-java-21` is conditional
— required on service contracts, not on TOs, records, or overrides — and Checkstyle cannot express
that distinction without producing noise that trains people to ignore it. Javadoc *correctness* is
gated; Javadoc *presence* is a review question.

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
    <!-- spotless.version is declared in the properties block in maven-configuration.md -->
    <version>${spotless.version}</version>
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

The file is the gate for the rules other skills define, and it is annotated with which skill owns
each block. Do not regenerate it from memory and do not trim it to make an existing codebase pass —
if a rule does not fit the project, change the rule in its owning skill and say so, per `../SKILL.md`.

The modules it enables, and who owns each rule:

| Block | Owning skill | Enforces |
| --- | --- | --- |
| Imports | `modern-java-21` | No star imports, no unused or redundant imports, the seven-group order |
| Explicitness | `modern-java-21` | `this.` qualification, `final` parameters and single-assignment locals, no `var` |
| Size and shape | `modern-java-21` | Method length, parameter count, one top-level class per file |
| Exceptions | `modern-java-21` | No catching `Error` or `Throwable`, no empty catch, `equals`/`hashCode` pairing |
| Identifier form | `project-naming-conventions` | Package, type, method, member, parameter, constant naming and abbreviation length |
| Log hygiene | `observability-and-logging` | No `System.out`, `System.err`, `printStackTrace`, or concatenation in a log call |
| Javadoc correctness | `modern-java-21` | Well-formed Javadoc where it exists; presence is deliberately not gated |

Configuration decisions worth knowing before someone "fixes" them:

- **`AbbreviationAsWordInName` is set to `1`** so that two consecutive capitals are permitted. That is what allows the project's `TO` suffix, `UserTO` and `UserCreateTO`, while still rejecting `HTTPClient`. `ignoreStaticFinal` keeps `UPPER_SNAKE_CASE` constants out of scope.
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

Four decisions in that block are deliberate:

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
