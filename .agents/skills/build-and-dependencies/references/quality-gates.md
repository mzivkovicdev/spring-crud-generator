# Quality gates

Use this reference when setting up or changing the automated enforcement of project rules. Apply
every rule from `../SKILL.md`. The owner of each *rule* is the skill that defines it; this reference
owns only the configuration that makes the rule fail a build.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [What is gated and what is not](#what-is-gated-and-what-is-not)
2. [Generated code and the gates](#generated-code-and-the-gates)
3. [Gated but not proven](#gated-but-not-proven)
4. [Layer 1: editor and formatter](#layer-1-editor-and-formatter)
5. [Layer 2: Checkstyle](#layer-2-checkstyle)
6. [Modules to verify before relying on them](#modules-to-verify-before-relying-on-them)
7. [Layer 3: dependency and build guards](#layer-3-dependency-and-build-guards)
   - [Maven: enforcer rules](#maven-enforcer-rules)
   - [Gradle: the same four rules](#gradle-the-same-four-rules)
8. [Nullability checking](#nullability-checking)
9. [Wiring the gates into the build](#wiring-the-gates-into-the-build)
10. [No baseline, no suppressions](#no-baseline-no-suppressions)
   - [Adopting the set into a repository that already has code](#adopting-the-set-into-a-repository-that-already-has-code)

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
| No concatenation in a log call, classic and fluent | `observability-and-logging` | Two Checkstyle regexes |
| Identifier naming form | `project-naming-conventions` | Checkstyle naming modules |
| Integration tests run in their own phase | `spring-boot-testing` | Surefire/Failsafe or Gradle suites |
| Banned and duplicated dependencies, profile presence | `build-and-dependencies` | `maven-enforcer-plugin`, or a resolution rule plus a verification task on Gradle |
| Java release the build actually uses | `build-and-dependencies` | `requireJavaVersion` on Maven; the toolchain on Gradle, which removes the mismatch rather than detecting it |
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

## Gated but not proven

A green build proves that every gate ran and found nothing. It does not prove the rule holds,
because three of these gates are deliberately heuristics — each one is documented where it is
configured, and this is the consolidated list, so a reviewer has one place to look instead of
reconstructing it from the notes.

| Rule | What the gate catches | What it cannot see |
| --- | --- | --- |
| No `var` | A declaration at the start of a statement, in an enhanced-for, or in a try-with-resources | A `var` written mid-line or inside a single-line block. The patterns are anchored deliberately: an unanchored one fires on a string literal containing `var x = 1`, and a gate that fails correct code gets weakened |
| No concatenation in a log call | Both call styles, classic and fluent, where a string literal is followed by `+` on the same line | A concatenation split across lines, or one built into a variable first. It is a gate, not a proof |
| Import group order | Group placement and ordering, and the blank line between **type** groups | The blank line between the static block and the first type group. Checkstyle's `ImportOrder` scopes `separated` to type groups; Spotless formats that boundary and the committed IDE configuration preserves it |

Two more rules are review-only by design and appear in no gate at all: a shared numeric bound
repeated as a literal rather than referenced from its constant, since `MagicNumber` produces more
noise than value in a Spring project; and whether a `@Nullable` annotation is *correct*, which a
checker cannot judge because the code and the annotations then agree with each other and disagree
with reality.

**This list is what `spring-boot-code-review` reads before trusting a green build.** Anything on it
stays a review responsibility. Adding a rule to this table is how a gate's known blind spot becomes
visible instead of folklore; removing one requires the gate to actually close it.

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
- **The logger-name gate exists to keep the classic concatenation gate honest.** That pattern matches the literal identifier `LOGGER`, so a logger named `log` would be silently exempt from it; enforcing the name is what makes it mean anything. The fluent pattern does not share that dependency — it anchors on the terminal `.log(` or `.setMessage(` call and never sees the logger's name — which is why the two are worth having separately rather than merged. The name rule also matches the single logger declaration form `observability-and-logging` requires.
- **`IllegalCatch` deliberately allows `RuntimeException`.** Catching it to tag an observation, increment a failure counter, or record an outcome and then rethrow is a required pattern in `observability-and-logging`. `Error` and `Throwable` stay banned.
- **`MagicNumber` is not enabled.** In a Spring project it fires mostly on validation annotations and produces more noise than value; the real rule — shared bounds declared once — is covered by review and by the constants the skills already require.
- **Log concatenation takes two patterns, because the two call styles put the message in different places.** The classic form has the message inside `LOGGER.info(...)`, so the pattern anchors on the logger. The SLF4J fluent form that `observability-and-logging` requires for structured fields spreads the call across lines, and the message sits on a `.log(...)` or `.setMessage(...)` line carrying no `LOGGER.` prefix at all — invisible to the first pattern. The second anchors on the terminal call instead. Both are still heuristics: a concatenation split across two lines, or assembled into a variable first, passes either one. They are gates, not proofs.
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

## Layer 3: dependency and build guards

Four rules live at this layer: the Java release the build actually uses, the presence of
`docs/project-profile.md`, version convergence across the dependency graph, and the banned-artifact
list that encodes the duplicated-capability rule. **Both build tools enforce all four**, but they do
not enforce them the same way, and two of them Gradle handles structurally rather than with a check.
Read the subsection for the tool the profile records; the other one does not apply.

| Rule | Maven | Gradle |
| --- | --- | --- |
| Java release | `requireJavaVersion`, checking the JDK that runs Maven | The toolchain, which *selects* the JDK rather than checking it — see below |
| `docs/project-profile.md` exists | `requireFilesExist` | A verification task `check` depends on |
| Version convergence | `dependencyConvergence` and `requireUpperBoundDeps` | `failOnVersionConflict()`, which is stricter and costs more — see below |
| Banned artifacts | `bannedDependencies` | A resolution rule over every resolvable configuration |

### Maven: enforcer rules

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

### Gradle: the same four rules

Gradle has no enforcer plugin, and reaching for a third-party one is not the answer — three of these
four are a few lines of the build script, and the fourth is already covered by a mechanism the
project has for other reasons. Declare them together in one block so they are found and reviewed as
a set, the way the `<rules>` block is on Maven.

```kotlin
// ---- The banned-artifact list, mirroring the Maven <bannedDependencies> excludes. ----
val bannedGroups: Map<String, String> = mapOf(
    "com.github.dozermapper" to "MapStruct is the project's mapper",
    "org.powermock" to "Mockito is the project's mocking framework",
)
val bannedModules: Map<String, String> = mapOf(
    "com.google.code.gson:gson" to "Jackson is the project's JSON library",
    "org.modelmapper:modelmapper" to "MapStruct is the project's mapper",
    "junit:junit" to "JUnit Jupiter is the project's test framework",
    "commons-logging:commons-logging" to "SLF4J is the project's logging facade",
    "log4j:log4j" to "Logback is the project's logging backend",
)

configurations.configureEach {
    resolutionStrategy.eachDependency {
        val reason = bannedGroups[requested.group]
            ?: bannedModules["${requested.group}:${requested.name}"]
        if (reason != null) {
            throw GradleException(
                "Banned dependency ${requested.group}:${requested.name}: $reason. " +
                    "It arrived directly or transitively; run the dependency audit before adding an exclusion."
            )
        }
    }
}

// ---- The project profile must exist before anything is built. ----
val projectProfile = File(rootDir, "docs/project-profile.md")

val verifyProjectProfile by tasks.registering {
    group = "verification"
    description = "Fails when docs/project-profile.md is missing."
    outputs.upToDateWhen { false }
    doLast {
        if (!projectProfile.exists()) {
            throw GradleException(
                "docs/project-profile.md is missing. Create it from the template " +
                    "before building; see the project README."
            )
        }
    }
}

tasks.named("check") {
    dependsOn(verifyProjectProfile)
}
```

Five things in that block decide whether it is equivalent to the Maven one:

- **`configurations.configureEach`, not one named configuration.** A banned artifact reaching only `testRuntimeClasspath` is still in the build. Covering every configuration is what makes this the equivalent of `bannedDependencies`, which is graph-wide on Maven.
- **`eachDependency` sees transitives**, which is the point: almost nothing on that list is ever declared deliberately. `commons-logging` and `log4j` arrive through somebody else's dependency, which is exactly the case a declaration-only check would miss.
- **The message names the artifact and the reason.** Maven's rule prints the coordinate; the reason is what stops the next person from adding an exclusion instead of asking why two libraries do one job.
- **`File(rootDir, …)` and not a per-project path.** The profile lives once at the repository root, so in a multi-module build every subproject must look at the same file — the same decision `${maven.multiModuleProjectDirectory}` makes on Maven. Capturing it into a `val` outside `doLast` keeps the task configuration-cache compatible.
- **`outputs.upToDateWhen { false }`.** A task with no declared inputs is up-to-date after its first run, so without this line the check passes forever once it has passed once — including after someone deletes the file.

**The Java release needs no rule here, and that is a real difference rather than a gap.** Maven's
`requireJavaVersion` exists because `maven.compiler.release` and the JDK running Maven are two
different things, and a mismatch produces different bytecode with no visible failure. A Gradle
toolchain removes the mismatch instead of detecting it: it *selects* the JDK that compiles and runs
tests, so the JDK running Gradle cannot affect the output. Declare the toolchain, per
[Gradle configuration](gradle-configuration.md#java-toolchain-and-compiler), and add no check. A
project that has not declared one has the Maven problem and no rule against it — that is the finding,
not a missing enforcer.

**Version convergence is the one place Gradle is genuinely harder, so decide it rather than copying
it.** Maven's `requireUpperBoundDeps` demands that the resolved version be at least the highest
requested one; Gradle already resolves that way by default, so that half is free. `dependencyConvergence`
is stricter — it fails when two paths request *different* versions at all — and its Gradle equivalent,
`resolutionStrategy.failOnVersionConflict()`, fails on conflicts the Spring Boot BOM is there to
resolve, so a project that enables it maintains a `force` list from then on.

| Option | What it costs | When it fits |
| --- | --- | --- |
| Nothing beyond the BOM | Free. Highest-version resolution is the default, matching `requireUpperBoundDeps` | The default for a project on the Spring Boot BOM |
| `dependencyLocking` with committed lockfiles | One file per configuration, updated deliberately | Reproducibility matters more than convergence — a resolution change becomes a reviewable diff |
| `failOnVersionConflict()` plus a `force` list | Ongoing maintenance, and a build that fails on somebody else's upgrade | A project that has been bitten by a silent version bump and accepts the cost |

Record the choice in the profile beside the other build decisions. Do not enable
`failOnVersionConflict()` because the Maven column has two rules and the Gradle column has one; the
two tools do not divide this problem the same way, and matching rule counts is not the goal.

### Metric tag cardinality

Metric tag cardinality is the one rule in the gate table that no build tool enforces, because
cardinality is a runtime property. Its gate is a `MeterFilter` bean, and **that bean is application
code, not build configuration** — `observability-and-logging` owns it and declares it once, in
[metrics and tracing](../../observability-and-logging/references/metrics-and-tracing.md#capping-tag-cardinality-at-runtime).
It appears in the table above so that a reader auditing the gates finds every rule accounted for,
and nowhere else in this skill.

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

**The rule is that no gate runs after the test suite.** Tests are the slowest part of the cycle, and
a violation found after them is a violation found at the cost of everything before it. The ideal
order is format, static analysis, compile, tests — and one build tool can deliver exactly that while
the other cannot, so state the rule by its purpose rather than by that sequence.

**Maven.** Bind `spotless:check` to `validate` and `checkstyle:check` to `validate` as well, with
`failOnViolation` true and `violationSeverity` set to `error`. Point it at
`config/checkstyle/checkstyle.xml` and include test sources. Do not bind either to `verify`. That
gives the ideal order literally: both gates run in the first phase, before anything is compiled.

**Gradle.** Apply the `checkstyle` plugin with `toolVersion` pinned, `maxWarnings = 0`,
`ignoreFailures = false`, and `configDirectory` set to `config/checkstyle`. Spotless and Checkstyle
tasks are wired into `check` already — but `check` also carries `test`, and being in the same task
graph orders nothing. Two lines are required, and neither is a default:

```kotlin
// The formatter needs no compiled classes, so it can run before compilation as on Maven.
tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.named("spotlessCheck"))
}

// Checkstyle cannot: checkstyleMain consumes the compiled classes, so the achievable
// guarantee is that it runs before the tests rather than before the compiler.
tasks.withType<Test>().configureEach {
    mustRunAfter(tasks.withType<Checkstyle>())
}
```

**The middle two steps swap on Gradle, and it is a property of the tool rather than a choice.**
`checkstyleMain` takes `sourceSets.main.output` as an input, so it cannot precede `compileJava`;
Maven's `checkstyle:check` reads sources and runs in `validate` before the compiler. The order Gradle
can guarantee is therefore **format → compile → static analysis → tests**, which satisfies the rule
as stated — nothing runs after the tests — while differing from Maven's sequence in the middle.
Do not try to close the difference by moving Checkstyle to a `doFirst` block or a separate
source-reading task; that produces a second, weaker Checkstyle run rather than an earlier one.

Use `mustRunAfter` and not `shouldRunAfter` for the test ordering: the second is a hint Gradle drops
whenever it is scheduling-inconvenient, which makes the gate hold on some runs and not others.

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

### Adopting the set into a repository that already has code

Everything above assumes a greenfield project, which is what this set is written for. Turning the
same gates on over an existing codebase produces thousands of violations on the first run, and the
two obvious responses are both wrong: lowering the gates leaves a standard nobody enforces, and
fixing everything at once produces a diff nobody can review beside the feature work.

Neither is what a baseline is for, so this is the one case where a time-boxed one is the right tool
— and it is a different artefact from the suppression file this section rejects:

- **Turn the gates on at `error` for changed files only**, through the build's own path filtering or a pre-merge check scoped to the diff. New and touched code meets the standard from day one; untouched code does not block it.
- **Record the remaining violations once**, as a count per rule with an owner and a date, in the project's own tracking — not as entries a build silently consumes forever.
- **Give the adoption an end date and a removal condition**, in `docs/project-profile.md` under the deferred decisions. A scoped gate with no end date is a permanent two-tier standard, which is the failure this section exists to prevent, reached by a longer route.
- **Never let the scoped phase justify a call-site suppression.** The rule above holds throughout: a rule that does not fit is changed in the configuration, once, for everyone.

`RequireThis` and the import order are the two that produce the largest first run, and both are
mechanical — Spotless applies the import order, and the `this.` qualification is a single automated
pass. Doing those two before the first scoped build removes most of the count and leaves the
violations that need a human.
