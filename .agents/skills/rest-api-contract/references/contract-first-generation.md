# Contract-first generation

Read this only when `docs/project-profile.md` records contract-first. Apply every rule from
`../SKILL.md`, `build-and-dependencies` for the plugin declaration, and `project-naming-conventions`
for every generated name.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Settle the naming collision first](#settle-the-naming-collision-first)
2. [What to generate and what not to](#what-to-generate-and-what-not-to)
3. [Generator configuration](#generator-configuration)
   - [The options, independent of the build tool](#the-options-independent-of-the-build-tool)
   - [Maven](#maven)
   - [Gradle](#gradle)
   - [The Jackson decision on Spring Boot 4](#the-jackson-decision-on-spring-boot-4)
4. [Working with generated interfaces](#working-with-generated-interfaces)
5. [Rules for generated code](#rules-for-generated-code)

## Settle the naming collision first

This project names transport types with a `TO` suffix in a `transferobject` package, and forbids a
parallel `UserDto`. Generators default to `UserDto` or `User`. Resolve this before generating
anything; discovering it afterwards means either a large rename or two vocabularies in one codebase.

Three workable resolutions. Choose one, record it in the project profile, and do not mix them.

| Resolution | What happens | When it fits |
| --- | --- | --- |
| **Configure the suffix** | The generator emits `UserTO` into the project's package | Default choice; keeps one vocabulary |
| **Generate interfaces only** | The generator emits API interfaces; TOs stay hand-written | The contract is stable but the TOs need project-specific annotations or shapes |
| **Change the project convention** | Adopt the generator's naming everywhere | Only worth it if the project is new and the convention is not yet established |

Do not "resolve" it by hand-editing generated files, and do not let a generated `UserDto` and a
hand-written `UserTO` coexist. Two types for one concept is the anti-pattern the naming skill exists
to prevent.

## What to generate and what not to

Generate:

- API interfaces that the controllers implement;
- model types, if the configured-suffix resolution was chosen.

Do not generate:

- controller implementations, which would put logic in generated code;
- server stubs with example bodies;
- documentation providers, which duplicate what the committed document already is;
- test scaffolding.

Generated sources go to the build output directory, not to `src`. Never commit them and never edit
them. They are not *style* review material — nobody reviews a generator's formatting — which is not
the same as being ungated: `build-and-dependencies` owns that split in
[generated code and the gates](../../build-and-dependencies/references/quality-gates.md#generated-code-and-the-gates).

## Generator configuration

**The generation is not optional configuration here.** The generator has a separate switch per
Spring Boot major, and the wrong one produces code for the other generation's dependencies and
imports. Read the generation from `docs/project-profile.md` and set exactly one of them:

| Profile records | Option to set | Also set |
| --- | --- | --- |
| Spring Boot 3 | `useSpringBoot3` | — |
| Spring Boot 4 | `useSpringBoot4` | decide `useJackson3` deliberately, below |

Both options are false-by-default except `useSpringBoot3`, which the generator defaults to `true` —
so a Spring Boot 4 project that simply omits the setting silently generates Spring Boot 3 code.
Setting the correct one explicitly is what makes that visible in review.

### The options, independent of the build tool

The generator takes the same options whichever build tool invokes it. Decide them here, then declare
them in the project's own build file; `build-and-dependencies` owns that declaration and the version.

| Option | Value | What it decides |
| --- | --- | --- |
| `useSpringBoot3` / `useSpringBoot4` | one of them `true` | The generation, per the table above |
| `useJackson3` | a recorded decision | Spring Boot 4 only; see below |
| `interfaceOnly` | `true` | API interfaces without controller implementations, so the project's own thin controllers implement them and no logic lands in generated code |
| `useTags` | `true` | One interface per resource rather than one per path. This is why every operation must carry exactly one tag |
| `modelNameSuffix` | `TO` | Produces `UserTO` instead of `UserDto`. Omit it only under the interfaces-only resolution, where no models are generated at all |
| `documentationProvider` | `none` | Stops the generator from adding a springdoc or Swagger dependency. The committed document is the documentation; a generated one would be a second source |
| `openApiNullable` | `false` | Avoids the `JsonNullable` wrapper types, which leak an extra library into every signature. Turn it on only if the project deliberately adopts that library |

`useJakartaEe` is deliberately absent: both generation switches enable it, and both supported
generations are on the `jakarta` namespace. Setting it a second time is harmless but misleading — it
suggests the namespace is an independent choice when it follows from the generation.

Two wiring rules hold whichever build tool declares them, and both fail silently when missed: the
generated directory is on the compile source root, and generation runs **before** compilation, so
the interfaces exist when the controllers compile.

### Maven

The declaration below shows the Spring Boot 3 branch. Replace the one switch for a Spring Boot 4
project; nothing else in the block changes.

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>${openapi-generator.version}</version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi/openapi.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <apiPackage>com.example.myapp.controller.api</apiPackage>
                <modelPackage>com.example.myapp.transferobject</modelPackage>
                <configOptions>
                    <!-- Spring Boot 3. On Spring Boot 4 this line becomes
                         <useSpringBoot4>true</useSpringBoot4> plus the useJackson3 decision. -->
                    <useSpringBoot3>true</useSpringBoot3>
                    <interfaceOnly>true</interfaceOnly>
                    <useTags>true</useTags>
                    <modelNameSuffix>TO</modelNameSuffix>
                    <documentationProvider>none</documentationProvider>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

`openapi-generator.version` is declared in the properties block described in
[Maven configuration](../../build-and-dependencies/references/maven-configuration.md). Resolve it at
setup time rather than copying a number from documentation.

The Maven plugin adds the generated directory to the compile source root itself, and binding the
execution to a phase before `compile` satisfies the second wiring rule.

### Gradle

The same options, declared through the generator's own Gradle plugin. The plugin id is
`org.openapi.generator`, and it contributes the `openApiGenerate` task.

```kotlin
plugins {
    id("org.openapi.generator") version "RESOLVE"
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/resources/openapi/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.path)
    apiPackage.set("com.example.myapp.controller.api")
    modelPackage.set("com.example.myapp.transferobject")
    configOptions.set(
        mapOf(
            // Spring Boot 3. On Spring Boot 4 this entry becomes "useSpringBoot4" to "true",
            // plus the useJackson3 decision.
            "useSpringBoot3" to "true",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "modelNameSuffix" to "TO",
            "documentationProvider" to "none",
            "openApiNullable" to "false",
        )
    )
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
}

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}
```

Three things in that block are the Gradle-specific part of the contract, and all three are silent
when omitted:

- **The generated directory is added to the main source set.** Maven's plugin does this itself; Gradle's does not, so without the `srcDir` line the sources are generated and never compiled.
- **`compileJava` depends on the generate task.** Gradle infers no ordering from the source-set entry alone, so a clean build can compile before generating and fail on missing interfaces — or worse, succeed against a stale previous output.
- **The plugin version is resolved, not remembered**, and recorded in the profile's resolved-versions table like every other tool version. `build-and-dependencies` owns that choice.

Verify both by deleting the build directory and running a full build: the interfaces must be
regenerated and compiled in one command, with no manual step in between.

### The Jackson decision on Spring Boot 4

`useJackson3` exists only on the Spring Boot 4 branch — the generator rejects it otherwise — and it
is **a decision, not a default**. Record it in `docs/project-profile.md` and verify it rather than
assuming either answer, because the surrounding facts do not point one way:
`build-and-dependencies` records that Spring Boot 4 carries Jackson 3, but also that
`jackson-annotations` deliberately kept its old group and package, so generated models can look
correct under either setting while the databind types behind them differ.

Verify it the way this skill set verifies every generated artefact: generate once, read the imports
in the generated model, and confirm they match the Jackson the application actually configures.
A mismatch here does not fail the build — it fails at the first request that serializes one of those
types, which is the failure mode contract-first exists to eliminate.

Bind generation to the phase that runs before compilation so the interfaces exist when the
controllers compile, and confirm the generated sources are on the compile source root.

## Working with generated interfaces

The controller implements the generated interface and stays a thin transport boundary, exactly as
`spring-boot-patterns` requires. This is the same `UserController` that
[`spring-boot-patterns` → REST API examples](../../spring-boot-patterns/references/rest-api-examples.md#rest-controller)
declares, with the two differences contract-first makes: it implements a generated interface, and it
carries no mapping annotation and no route constant of its own.

```java
@RestController
public class UserController implements UsersApi {

    private final UserManagementApplicationService userManagement;
    private final UserService userService;

    public UserController(
            final UserManagementApplicationService userManagement,
            final UserService userService) {

        this.userManagement = userManagement;
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserProfileTO> usersUserIdGet(final Long userId) {
        final UserProfileDomain profile = this.userManagement.getProfile(userId);

        return ResponseEntity.ok(
                UserRestMapper.INSTANCE.mapUserProfileDomainToUserProfileTO(profile));
    }
}
```

Notes:

- The route and the HTTP method annotations come from the generated interface. Do not repeat them on the implementation; two sources for one route is how a path ends up served under one spelling and documented under another.
- The method name is the `operationId`. Under contract-first the document names the method, which is the direction the naming carve-out already assumes.
- Everything below the controller is unchanged: mapper, service, domain. Generated types stop at the transport boundary and never reach a service signature.
- Validation annotations generated from the schema are the contract's constraints. Do not weaken them in the implementation, and do not add stricter ones there; change the document instead.

## Rules for generated code

- The document is the source of truth. To change an endpoint, change the document, regenerate, then make the code compile. Never the reverse.
- A compilation failure after regeneration is the contract telling you what a change costs. Fix the code, not the generator settings.
- **A defect in the generated output is fixed in the document or the generator configuration, never in the output.** The project owns no template here — the generator is a third-party tool — so the general "fix it in the template" instruction does not apply to this path, and following it literally means hand-editing generated code.
- Do not commit generated sources, and exclude the generated directory from the human-style gates only — the project does not control the generator's formatter, so a failure there names no action anyone can take. Which gates those are, and which still apply, is stated once in [generated code and the gates](../../build-and-dependencies/references/quality-gates.md#generated-code-and-the-gates). It is not a blanket exemption; do not widen or narrow it here.
- Keep the document in the repository, reviewed like source. It is the artifact consumers depend on.
- Validate the document in the build before generating from it, so a malformed contract fails early with a clear message rather than as a generator stack trace.
- When the generator's output disagrees with a project convention, change the generator configuration or the convention deliberately. Do not paper over it with a hand-written wrapper type.
