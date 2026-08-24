# Gradle configuration

Use this reference when the project profile records Gradle. Apply every rule from `../SKILL.md`.
The snippets are Kotlin DSL excerpts of `build.gradle.kts`, not a complete file. Translate to Groovy
only if the repository already uses it; do not migrate an existing project between the two DSLs.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Plugins and version management](#plugins-and-version-management)
2. [Java toolchain and compiler](#java-toolchain-and-compiler)
3. [Annotation processors](#annotation-processors)
4. [Dependency declarations by configuration](#dependency-declarations-by-configuration)
5. [Test selection](#test-selection)
6. [Packaging and wrapper](#packaging-and-wrapper)
7. [Useful commands](#useful-commands)

## Plugins and version management

```kotlin
plugins {
    java
    checkstyle
    id("org.springframework.boot") version "RESOLVE"
    id("io.spring.dependency-management") version "RESOLVE"
    id("com.diffplug.spotless") version "RESOLVE"
}

checkstyle {
    // The Checkstyle tool, which is a different version from any plugin.
    toolVersion = "RESOLVE"
    configDirectory = layout.projectDirectory.dir("config/checkstyle")
    maxWarnings = 0
    isIgnoreFailures = false
}

extra["mapstructVersion"] = "RESOLVE"
// Only when docs/project-profile.md records that the project uses Lombok.
extra["lombokMapstructBindingVersion"] = "RESOLVE"
```

`checkstyle` and `java` are Gradle-distributed plugins and take no version — the Gradle version in
the committed wrapper pins them, which is one of the reasons the wrapper is reviewed as executable
code. Everything applied by identifier carries a version. `toolVersion` is separate and required:
without it the plugin picks its own default Checkstyle, which is usually behind the version the
committed configuration was written against.

`RESOLVE` is the decision token defined in `spring-boot-patterns`: look the current release up at
setup time, write it into the build file, and record it with its resolution date in the
resolved-versions table of `docs/project-profile.md`. This reference deliberately carries no pinned
number, because a number in documentation goes stale and then propagates. If the lookup is
impossible, record `UNDECIDED` with the reason rather than a remembered number. The minimum versions
and their reasons are listed in
[Maven configuration](maven-configuration.md#project-skeleton-and-version-management); they apply to
Gradle identically.

Rules:

- The `io.spring.dependency-management` plugin, or the Spring Boot plugin's own BOM application, is what lets dependencies be declared without versions. Keep it.
- Plugin versions belong in the `plugins` block, resolved as `../SKILL.md` requires.
- Never use dynamic versions such as `1.+` or `latest.release` for any dependency.
- To change a managed version, set the BOM property, for example `extra["hibernate.version"]`, rather than pinning the dependency. Record the reason and a removal condition beside it.

## Java toolchain and compiler

Use a toolchain so the build does not depend on the developer's or the CI agent's default JDK.

```kotlin
java {
    toolchain {
        // The release recorded in docs/project-profile.md; 21 is the floor.
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "-parameters",
            "-Amapstruct.unmappedTargetPolicy=ERROR",
            "-Amapstruct.suppressGeneratorTimestamp=true",
        )
    )
}
```

The Spring Boot Gradle plugin adds `-parameters` to the main compile task. Adding it explicitly
covers every compile task, including the integration-test source set, and survives a future change
in plugin behavior. Spring needs it for constructor binding, `@ConfigurationProperties`, and query
derivation.

## Annotation processors

Gradle builds the processor path from the `annotationProcessor` configuration in declaration order.

### Default: no Lombok

```kotlin
dependencies {
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
}
```

### When the project uses Lombok

Lombok is optional and the decision belongs in `docs/project-profile.md`. Nothing in this skill set
requires it. Add these declarations **only** when the profile records that the project uses it, and
keep Lombok, then the binding, then MapStruct in this order.

```kotlin
dependencies {
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${property("lombokMapstructBindingVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}
```

`lombok-mapstruct-binding` exists only to make MapStruct wait for Lombok's generated accessors.
Without it, generation order is undefined: the build either fails with missing properties or
produces a mapper that quietly skips fields. It is meaningless without Lombok, so it appears only in
this variant. Lombok also needs a separate `testAnnotationProcessor` declaration, or it silently
stops working in test sources.

### When the project uses the JPA static metamodel

`spring-data-jpa` prefers the generated static metamodel over raw attribute-name strings. The
processor artifact differs by Spring Boot generation — [generation differences](generation-differences.md)
carries both coordinates — and needs no version, because the Spring Boot BOM manages Hibernate:

```kotlin
annotationProcessor("org.hibernate.orm:ARTIFACT-FROM-GENERATION-DIFFERENCES")
```

Add it only when the project actually uses the metamodel, and confirm that `<EntityName>_` classes
appear under `build/generated/sources/annotationProcessor` afterwards.

### Rules that apply either way

- `mapstruct` is a compile dependency; `mapstruct-processor` is a processor. Both are required, and they are not interchangeable.
- `spring-boot-configuration-processor` produces metadata for `@ConfigurationProperties`. Its absence is invisible until IDE completion disappears.
- Verify the order in the resolved configuration rather than assuming it. After changing any processor, its version, an entity, or a mapper, rebuild and read the generated sources under `build/generated/sources/annotationProcessor`. A green compile does not prove the mapper mapped what you expected.

## Dependency declarations by configuration

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

- No version appears for a BOM-managed artifact. The Spring Boot BOM manages the Testcontainers modules, so importing `testcontainers-bom` on top of it is a second source of truth for the same versions. Import it only to deliberately move Testcontainers off the managed version, and record the reason and a removal condition beside it.
- Use `implementation` by default. Use `api` only in a library module that deliberately exposes a type in its own public API; in an application module it is almost always wrong and it slows down compilation for everything downstream.
- The JDBC driver is `runtimeOnly`. It is loaded by name and never imported, so every analyzer calls it unused.
- `spring-boot-starter-test` already provides JUnit Jupiter, AssertJ, Hamcrest, Mockito, JSONassert, JsonPath, and the Spring test module. Declaring any of those separately duplicates a managed capability.

## Test selection

`spring-boot-testing` names full application and persistence tests `*IntegrationTest` and requires
that they run separately from unit and slice tests. Use the JVM test suite plugin, which creates the
source set, its configurations, and its task together.

```kotlin
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation("org.springframework.boot:spring-boot-starter-test")
            }
            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
```

With a separate source set, integration tests live in `src/integrationTest/java` and the `test` task
never picks them up, so no exclusion pattern is needed.

If the project keeps all tests in `src/test/java` instead, filter by name and register the extra
task explicitly:

```kotlin
tasks.test {
    useJUnitPlatform()
    filter { excludeTestsMatching("*IntegrationTest") }
}

val integrationTest by tasks.registering(Test::class) {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*IntegrationTest") }
    shouldRunAfter(tasks.test)
}

tasks.named("check") { dependsOn(integrationTest) }
```

Either way, `check` must depend on the integration task. A task nobody runs is worse than no
separation. Do not add `ignoreFailures`, `enabled = false`, or a property that skips tests in
committed configuration.

Because integration tests start containers, apply `maxParallelForks` deliberately and keep
parallelism off for infrastructure that `spring-boot-testing` has not proven parallel-safe.

## Packaging and wrapper

- Keep the Spring Boot plugin's `bootJar` task for packaging, and enable layers when the artifact is containerized.
- Commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/`, including `distributionUrl` and `distributionSha256Sum`. Review any change to them as executable code.
- Declare repositories only when an artifact genuinely is not on `mavenCentral()`, and only over authenticated TLS.
- Keep credentials in Gradle properties or environment variables supplied by the platform, never in the build script.
- Do not enable the build cache or configuration cache against an untrusted change without reading what the change does to the build logic first.

## Useful commands

```text
./gradlew compileJava                  # compile and run annotation processing
./gradlew test                         # unit and slice tests
./gradlew check                        # everything, including integration tests
./gradlew dependencies --configuration runtimeClasspath
./gradlew dependencyInsight --dependency <artifact> --configuration runtimeClasspath
./gradlew buildEnvironment             # plugin and build-script dependencies
```

Gradle has no built-in equivalent of Maven's dependency analysis. When the project needs one, the
community dependency-analysis plugin can produce declared-unused and used-undeclared leads. Treat
its output as leads only, and see [dependency audit and removal](dependency-audit.md) before acting
on it.
