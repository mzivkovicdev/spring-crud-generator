# Quality gates

Use this reference when setting up or changing the automated enforcement of project rules. Apply
every rule from `../SKILL.md`. The owner of each *rule* is the skill that defines it; this reference
owns only the configuration that makes the rule fail a build.

Snippets here follow the worked-example rules in `modern-java-21`: every identifier or build property a snippet uses is declared in that snippet or attributed to the file that declares it, and an excerpt names any omitted element that the configuration depends on.

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

### `.editorconfig`

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_style = space
indent_size = 4
max_line_length = 120

[*.{xml,yml,yaml,json}]
indent_style = space
indent_size = 2
```

### `.idea/codeStyles/codeStyleConfig.xml`

```xml
<component name="ProjectCodeStyleConfiguration">
  <state>
    <option name="USE_PER_PROJECT_SETTINGS" value="true" />
  </state>
</component>
```

### `.idea/codeStyles/Project.xml`

```xml
<component name="ProjectCodeStyleConfiguration">
  <code_scheme name="Project" version="173">
    <JavaCodeStyleSettings>
      <option name="CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" value="999" />
      <option name="NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND" value="999" />
      <option name="IMPORT_LAYOUT_TABLE">
        <value>
          <package name="" withSubpackages="true" static="true" />
          <emptyLine />
          <package name="java" withSubpackages="true" static="false" />
          <emptyLine />
          <package name="jakarta" withSubpackages="true" static="false" />
          <emptyLine />
          <package name="javax" withSubpackages="true" static="false" />
          <emptyLine />
          <package name="com" withSubpackages="true" static="false" />
          <emptyLine />
          <package name="org" withSubpackages="true" static="false" />
          <emptyLine />
          <package name="" withSubpackages="true" static="false" />
        </value>
      </option>
    </JavaCodeStyleSettings>
  </code_scheme>
</component>
```

The two on-demand thresholds set to 999 are what prevent the IDE from collapsing imports into
`java.util.*`. Commit both files. Both are project-scoped and contain no personal settings.

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

Save as `config/checkstyle/checkstyle.xml`. Severity is `error` everywhere: a warning nobody has to
fix is not a gate.

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
        "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <property name="severity" value="error"/>
    <property name="charset" value="UTF-8"/>
    <property name="fileExtensions" value="java"/>

    <module name="NewlineAtEndOfFile"/>
    <module name="FileTabCharacter">
        <property name="eachLine" value="true"/>
    </module>

    <!-- modern-java-21: class size -->
    <module name="FileLength">
        <property name="max" value="1000"/>
    </module>

    <!-- Line length is not defined by the skill set. Record the chosen value in the profile. -->
    <module name="LineLength">
        <property name="max" value="120"/>
        <property name="ignorePattern" value="^package .*|^import .*|https?://"/>
    </module>

    <module name="TreeWalker">

        <!-- ===== modern-java-21: imports ===== -->
        <module name="AvoidStarImport"/>
        <module name="RedundantImport"/>
        <module name="UnusedImports">
            <property name="processJavadoc" value="true"/>
        </module>
        <!-- VERIFY ON FIRST RUN: see "Modules to verify before relying on them". -->
        <module name="ImportOrder">
            <property name="groups" value="java,jakarta,javax,com,org,*"/>
            <property name="option" value="top"/>
            <property name="ordered" value="true"/>
            <property name="separated" value="true"/>
            <property name="sortStaticImportsAlphabetically" value="true"/>
        </module>

        <!-- ===== modern-java-21: explicitness ===== -->
        <module name="RequireThis">
            <property name="checkFields" value="true"/>
            <property name="checkMethods" value="true"/>
            <property name="validateOnlyOverlapping" value="false"/>
        </module>
        <module name="FinalParameters">
            <property name="tokens" value="METHOD_DEF,CTOR_DEF"/>
        </module>
        <module name="FinalLocalVariable">
            <property name="validateEnhancedForLoopVariable" value="true"/>
        </module>
        <module name="ExplicitInitialization"/>

        <!-- No var. Two patterns: declaration with assignment, and enhanced for. -->
        <module name="RegexpSinglelineJava">
            <property name="format" value="(^|[^\w.])var\s+\w+\s*="/>
            <property name="ignoreComments" value="true"/>
            <property name="message" value="Do not use var; declare the explicit type."/>
        </module>
        <module name="RegexpSinglelineJava">
            <property name="format" value="for\s*\(\s*(final\s+)?var\s"/>
            <property name="ignoreComments" value="true"/>
            <property name="message" value="Do not use var; declare the explicit type."/>
        </module>

        <!-- ===== modern-java-21: size and shape ===== -->
        <module name="MethodLength">
            <property name="max" value="100"/>
            <property name="countEmpty" value="false"/>
        </module>
        <module name="ParameterNumber">
            <property name="max" value="7"/>
            <property name="tokens" value="METHOD_DEF,CTOR_DEF"/>
            <property name="ignoreOverriddenMethods" value="true"/>
        </module>
        <module name="OuterTypeFilename"/>
        <module name="OneTopLevelClass"/>
        <!-- HideUtilityClassConstructor and VisibilityModifier are deliberately absent.
             See "Modules to verify before relying on them". -->

        <!-- ===== modern-java-21: exceptions ===== -->
        <module name="IllegalCatch">
            <property name="illegalClassNames" value="java.lang.Error,java.lang.Throwable"/>
        </module>
        <module name="IllegalThrows"/>
        <module name="EmptyCatchBlock">
            <property name="exceptionVariableName" value="ignored|expected"/>
        </module>
        <module name="EqualsHashCode"/>
        <module name="MissingOverride"/>
        <module name="StringLiteralEquality"/>
        <module name="SimplifyBooleanExpression"/>
        <module name="SimplifyBooleanReturn"/>
        <module name="NeedBraces"/>

        <!-- ===== project-naming-conventions: identifier form ===== -->
        <module name="PackageName">
            <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
        </module>
        <module name="TypeName"/>
        <module name="MethodName"/>
        <module name="MemberName"/>
        <module name="ParameterName"/>
        <module name="LocalVariableName"/>
        <module name="LocalFinalVariableName"/>
        <module name="ConstantName"/>
        <module name="AbbreviationAsWordInName">
            <property name="allowedAbbreviationLength" value="1"/>
            <property name="ignoreStaticFinal" value="true"/>
        </module>

        <!-- ===== observability-and-logging ===== -->
        <module name="RegexpSinglelineJava">
            <property name="format" value="System\.(out|err)\."/>
            <property name="ignoreComments" value="true"/>
            <property name="message" value="Use the SLF4J logger, not System.out or System.err."/>
        </module>
        <module name="RegexpSinglelineJava">
            <property name="format" value="\.printStackTrace\s*\("/>
            <property name="ignoreComments" value="true"/>
            <property name="message" value="Log the exception through SLF4J instead."/>
        </module>
        <module name="RegexpSinglelineJava">
            <property name="format" value="LOGGER\.(trace|debug|info|warn|error)\s*\([^;]*&quot;\s*\+"/>
            <property name="ignoreComments" value="true"/>
            <property name="message"
                      value="Do not concatenate in a log call; use fields or {} placeholders."/>
        </module>

        <!-- ===== Javadoc correctness, not presence ===== -->
        <module name="JavadocMethod">
            <property name="accessModifiers" value="public"/>
        </module>
        <module name="JavadocStyle">
            <property name="checkFirstSentence" value="true"/>
        </module>
        <module name="NonEmptyAtclauseDescription"/>

    </module>
</module>
```

Configuration decisions worth knowing before someone "fixes" them:

- **`AbbreviationAsWordInName` is set to `1`** so that two consecutive capitals are permitted. That is what allows the project's `TO` suffix, `UserTO` and `UserCreateTO`, while still rejecting `HTTPClient`. `ignoreStaticFinal` keeps `UPPER_SNAKE_CASE` constants out of scope.
- **`IllegalCatch` deliberately allows `RuntimeException`.** Catching it to tag an observation, increment a failure counter, or record an outcome and then rethrow is a required pattern in `observability-and-logging`. `Error` and `Throwable` stay banned.
- **`MagicNumber` is not enabled.** In a Spring project it fires mostly on validation annotations and produces more noise than value; the real rule — shared bounds declared once — is covered by review and by the constants the skills already require.
- **The log-concatenation regex is a heuristic.** It catches the common case and will not catch every one. It is a gate, not a proof.
- **`RequireThis` with `validateOnlyOverlapping=false`** is what makes the `this.` rule real. It is the single noisiest module on an existing codebase and the single most valuable one on a new project, which is why it goes in before the first feature.

## Modules to verify before relying on them

The configuration above is the high-confidence core. The following are useful but behave in ways
that depend on the installed Checkstyle version and on constructs this project uses heavily, so they
are either flagged or left out. Verify each on the first real run, then adopt or discard it
deliberately.

| Module | What to verify | Why it is uncertain |
| --- | --- | --- |
| `ImportOrder` (included, flagged) | That `option=top`, `separated=true`, and the `*` catch-all group interact as intended, and that a file matching the project order passes | Static-group separation is governed by `separated` in some versions and by `separatedStaticGroups` in others. Spotless already applies the order, so a false positive here is noise rather than a missing gate. If it misbehaves, remove the module and keep Spotless as the enforcement point. |
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
