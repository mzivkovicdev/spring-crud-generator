# Maven configuration

Use this reference when the project profile records Maven. Apply every rule from `../SKILL.md`.
The snippets are excerpts of a `pom.xml`, not a complete file.

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
    <version>3.5.0</version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
    <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
    <mapstruct.version>1.6.3</mapstruct.version>
</properties>
```

The versions above are placeholders. Use the versions recorded in `docs/project-profile.md`.

Rules:

- Declare a version property only for an artifact the Spring Boot BOM does not manage. MapStruct and the Lombok–MapStruct binding are two such artifacts; Lombok itself is managed.
- When the project cannot inherit the parent, import the BOM in `dependencyManagement` with `<scope>import</scope>` and `<type>pom</type>`, and then configure `-parameters` and the Java release explicitly, because the parent is no longer supplying them.
- To change a managed version, override the BOM property, for example `<hibernate.version>`, rather than pinning the dependency. Add a comment stating the reason and the condition for removing the override.

## Compiler and annotation processors

`annotationProcessorPaths` replaces classpath discovery. Once it is declared, a processor that is
not listed does not run, even if it is a normal dependency. Order inside the list is the processing
order.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>${java.version}</release>
        <parameters>true</parameters>
        <annotationProcessorPaths>
            <!-- Order matters: Lombok, then the binding, then MapStruct. -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
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
                <version>${spring-boot.version}</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-Amapstruct.unmappedTargetPolicy=ERROR</arg>
            <arg>-Amapstruct.suppressGeneratorTimestamp=true</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

Notes:

- `lombok-mapstruct-binding` exists only to make MapStruct wait for Lombok's generated accessors. Without it, generation order is undefined: the build either fails with missing properties or produces a mapper that quietly skips fields. Include it whenever both are present, and omit it when the project does not use Lombok.
- `spring-boot-configuration-processor` produces configuration metadata for `@ConfigurationProperties`. It stops working the moment the processor path is declared without it, and its absence is invisible until someone notices IDE completion is gone.
- `-Amapstruct.unmappedTargetPolicy=ERROR` enforces the policy from `spring-boot-patterns` for every mapper, so it cannot be forgotten on one annotation.
- `-Amapstruct.suppressGeneratorTimestamp=true` keeps generated sources reproducible across builds.
- Lombok is `provided` scope as a dependency and additionally listed here as a processor. Both declarations are required.

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

<!-- Provided: annotations only, absent from the artifact. -->
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
- Commit `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/`, including the distribution URL and checksum. Review any change to them as executable code.
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
