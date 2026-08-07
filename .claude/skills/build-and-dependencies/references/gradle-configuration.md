# Gradle configuration

Use this reference when the project profile records Gradle. Apply every rule from `../SKILL.md`.
The snippets are Kotlin DSL excerpts of `build.gradle.kts`, not a complete file. Translate to Groovy
only if the repository already uses it; do not migrate an existing project between the two DSLs.

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
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

extra["mapstructVersion"] = "1.6.3"
extra["lombokMapstructBindingVersion"] = "0.2.0"
```

The versions above are placeholders. Use the versions recorded in `docs/project-profile.md`.

Rules:

- The `io.spring.dependency-management` plugin, or the Spring Boot plugin's own BOM application, is what lets dependencies be declared without versions. Keep it.
- Pin every plugin version in the `plugins` block. An unpinned or dynamic version makes the build non-reproducible.
- Never use dynamic versions such as `1.+` or `latest.release` for any dependency.
- To change a managed version, set the BOM property, for example `extra["hibernate.version"]`, rather than pinning the dependency. Record the reason and a removal condition beside it.

## Java toolchain and compiler

Use a toolchain so the build does not depend on the developer's or the CI agent's default JDK.

```kotlin
java {
    toolchain {
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

Gradle builds the processor path from the `annotationProcessor` configuration in declaration order,
so declare Lombok, then the binding, then MapStruct.

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

Notes:

- `lombok-mapstruct-binding` exists only to make MapStruct wait for Lombok's generated accessors. Without it, generation order is undefined: the build either fails with missing properties or produces a mapper that quietly skips fields. Include it whenever both are present, and omit it when the project does not use Lombok.
- `mapstruct` is a compile dependency; `mapstruct-processor` is a processor. Both are required, and they are not interchangeable.
- Lombok needs a separate `testAnnotationProcessor` declaration, or it silently stops working in test sources.
- `spring-boot-configuration-processor` produces metadata for `@ConfigurationProperties`. Its absence is invisible until IDE completion disappears.
- Verify the order in the resolved configuration rather than assuming it. After changing any processor, its version, an entity, or a mapper, rebuild and read the generated sources under `build/generated/sources/annotationProcessor`. A green compile does not prove the mapper mapped what you expected.

## Dependency declarations by configuration

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:junit-jupiter")
}
```

- No version appears for a BOM-managed artifact.
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
