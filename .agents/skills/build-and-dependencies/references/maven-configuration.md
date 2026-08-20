# Maven configuration

Use this reference when the project profile records Maven. Apply every rule from `../SKILL.md`.
The snippets are excerpts of a `pom.xml`, not a complete file.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Project skeleton and version management](#project-skeleton-and-version-management)
2. [Compiler and annotation processors](#compiler-and-annotation-processors)
3. [Test selection](#test-selection)
4. [Dependency declarations by scope](#dependency-declarations-by-scope)
5. [Packaging and wrapper](#packaging-and-wrapper)
6. [Useful commands](#useful-commands)

## Project skeleton and version management

Inherit from the Spring Boot parent so the BOM, the compiler defaults, `-parameters`, and the
resource filtering are configured consistently.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>RESOLVE</version>
    <relativePath/>
</parent>

<properties>
    <java.version>RESOLVE</java.version>
    <checkstyle.version>RESOLVE</checkstyle.version>
    <mapstruct.version>RESOLVE</mapstruct.version>
    <spotless.version>RESOLVE</spotless.version>
    <!-- Only when docs/project-profile.md records contract-first. -->
    <openapi-generator.version>RESOLVE</openapi-generator.version>
    <!-- Only when docs/project-profile.md records that the project uses Lombok. -->
    <lombok-mapstruct-binding.version>RESOLVE</lombok-mapstruct-binding.version>
</properties>
```

Every version property referenced anywhere in this skill's references is declared here. The Spring
Boot BOM manages none of these tools, so each is pinned explicitly.

`RESOLVE` is the decision token defined in `spring-boot-patterns`: look the current release up at
setup time, write it into the build file, and record it with its resolution date in the
resolved-versions table of `docs/project-profile.md`. This reference deliberately carries no pinned
number, because a number written into documentation is stale the month after it is written and is
then copied into projects for years. If the lookup is impossible, record `UNDECIDED` with the reason
rather than a remembered number, and say so in the handoff.

Respect these minimums when resolving:

| Property | Minimum | Reason |
| --- | --- | --- |
| `java.version` | 21 | The floor this skill set is written against; `../SKILL.md` governs choosing the release |
| `maven-compiler-plugin` (from the parent) | 3.12.0 | Below it, `annotationProcessorPaths` ignores `dependencyManagement`, so every processor entry needs an explicit version |
| `checkstyle.version` | 10.12.x | Earlier versions handle `record` constructs inconsistently |
| `spotless.version` | 2.30.x | Earlier versions do not support the catch-all group in `importOrder` |
| `mapstruct.version` | 1.5.x | Constructor-based mapping and `unmappedTargetPolicy` behave as this skill set assumes |
| `lombok-mapstruct-binding.version` | 0.2.0 | Required alongside Lombok on the toolchains this skill set supports |

Verify the choice by running the build once, not by trusting the table.

Rules:

- Declare a version property only for an artifact the Spring Boot BOM does not manage. MapStruct and, if the project uses Lombok, the Lombok–MapStruct binding are two such artifacts; Lombok itself is managed.
- When the project cannot inherit the parent, import the BOM in `dependencyManagement` with `<scope>import</scope>` and `<type>pom</type>`, and then configure `-parameters` and the Java release explicitly, because the parent is no longer supplying them.
- To change a managed version, override the BOM property, for example `<hibernate.version>`, rather than pinning the dependency. Add a comment stating the reason and the condition for removing the override.

## Compiler and annotation processors

`annotationProcessorPaths` replaces classpath discovery. Once it is declared, a processor that is
not listed does not run, even if it is a normal dependency. Order inside the list is the processing
order.

Since `maven-compiler-plugin` 3.12.0, entries in `annotationProcessorPaths` resolve their versions
from `dependencyManagement`, so a BOM-managed processor needs no `<version>`. Every supported Spring
Boot generation ships a newer plugin than that through the parent, so the default form below omits
versions for managed artifacts and declares a property only for artifacts the BOM does not manage. Verify the effective
plugin version with `./mvnw help:effective-pom`; on an older plugin, every entry needs an explicit
version, and `${project.parent.version}` is the correct value for Spring Boot's own artifacts when
the project inherits the Spring Boot parent.

### Default: no Lombok

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>${java.version}</release>
        <parameters>true</parameters>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
            <path>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-configuration-processor</artifactId>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-Amapstruct.unmappedTargetPolicy=ERROR</arg>
            <arg>-Amapstruct.suppressGeneratorTimestamp=true</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

`mapstruct-processor` carries a version because the Spring Boot BOM does not manage MapStruct.
`spring-boot-configuration-processor` does not, because the BOM manages it.

### When the project uses Lombok

Lombok is optional and the decision belongs in `docs/project-profile.md`. Nothing in this skill set
requires it. Add the two entries below **only** when the profile records that the project uses it,
and place them exactly in this order.

```xml
<annotationProcessorPaths>
    <!-- Order matters: Lombok, then the binding, then MapStruct. -->
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </path>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
        <version>${lombok-mapstruct-binding.version}</version>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
    </path>
    <path>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
    </path>
</annotationProcessorPaths>
```

`lombok-mapstruct-binding` exists only to make MapStruct wait for Lombok's generated accessors.
Without it, generation order is undefined: the build either fails with missing properties or
produces a mapper that quietly skips fields. It is meaningless without Lombok, so it appears only in
this variant. Lombok itself is BOM-managed and needs no version; the binding is not.

With Lombok, the library is also declared as a `provided`-scope dependency. Both declarations are
required: the dependency makes the annotations visible to the compiler, the processor path makes the
generator run.

### Rules that apply either way

- `spring-boot-configuration-processor` produces configuration metadata for `@ConfigurationProperties`. It stops working the moment the processor path is declared without it, and its absence is invisible until someone notices IDE completion is gone.
- `-Amapstruct.unmappedTargetPolicy=ERROR` enforces the policy from `spring-boot-patterns` for every mapper, so it cannot be forgotten on an individual mapper.
- `-Amapstruct.suppressGeneratorTimestamp=true` keeps generated sources reproducible across builds.

After changing any processor, its version, an entity, or a mapper, rebuild and read the generated
sources under `target/generated-sources/annotations`. A green compile does not prove the mapper
mapped what you expected.

## Test selection

`spring-boot-testing` names full application and persistence tests `*IntegrationTest`. That pattern
also matches Surefire's default `**/*Test.java`, so it must be excluded there, or every
container-backed test runs in the `test` phase.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
        </excludes>
    </configuration>
</plugin>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*IntegrationTest.java</include>
        </includes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The `verify` goal is what turns a failed integration test into a failed build. Binding only
`integration-test` reports failures and then succeeds.

Do not add `<skipTests>`, `<testFailureIgnore>`, or a profile that disables either plugin. If a
suite is too slow for a given pipeline stage, run a narrower command in that stage rather than
weakening the build.

## Dependency declarations by scope

```xml
<!-- Compile: the source imports it. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Runtime: needed at run time, never imported. -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Provided: annotations only, absent from the artifact. Present only if the
     project profile records that the project uses Lombok. -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>

<!-- Test: test sources only. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- No `<version>` appears for a BOM-managed artifact.
- The JDBC driver is `runtime`. It is loaded by name and never imported, so every analyzer calls it unused.
- `spring-boot-starter-test` already provides JUnit Jupiter, AssertJ, Hamcrest, Mockito, JSONassert, JsonPath, and the Spring test module. Declaring any of those separately duplicates a managed capability.
- Testcontainers is declared through its own BOM plus the module for the project's database, in `test` scope.

## Packaging and wrapper

- Keep `spring-boot-maven-plugin` for repackaging, and configure layers when the artifact is containerized.
- The wrapper is `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/`, including the distribution URL and checksum.
- Declare repositories only when an artifact genuinely is not on the default one, and only over authenticated TLS.
- Keep credentials in `settings.xml` supplied by the platform, never in `pom.xml`.

## Useful commands

```text
./mvnw -q -DskipTests compile          # compile and run annotation processing
./mvnw test                            # unit and slice tests
./mvnw verify                          # everything, including integration tests
./mvnw dependency:tree                 # full graph with scopes
./mvnw dependency:tree -Dincludes=<groupId>:<artifactId>
./mvnw dependency:analyze              # declared-unused and used-undeclared leads
./mvnw -N dependency:analyze-duplicate
```

Treat `dependency:analyze` output as leads only. See
[dependency audit and removal](dependency-audit.md) before acting on it.
