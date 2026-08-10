# Contract-first generation

Read this only when `docs/project-profile.md` records contract-first. Apply every rule from
`../SKILL.md`, `build-and-dependencies` for the plugin declaration, and `project-naming-conventions`
for every generated name.

Snippets here follow the worked-example rules in `modern-java-21`: every identifier or build property
a snippet uses is declared in that snippet or attributed to the file that declares it.

## Contents

1. [Settle the naming collision first](#settle-the-naming-collision-first)
2. [What to generate and what not to](#what-to-generate-and-what-not-to)
3. [Generator configuration](#generator-configuration)
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

Generated sources go to the build output directory, not to `src`. Never commit them, never edit
them, and never treat them as review material.

## Generator configuration

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
                <apiPackage>com.acme.myapp.controller.api</apiPackage>
                <modelPackage>com.acme.myapp.transferobject</modelPackage>
                <configOptions>
                    <useSpringBoot3>true</useSpringBoot3>
                    <interfaceOnly>true</interfaceOnly>
                    <useTags>true</useTags>
                    <modelNameSuffix>TO</modelNameSuffix>
                    <useJakartaEe>true</useJakartaEe>
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

What each option is doing, because several are load-bearing:

- `interfaceOnly` generates API interfaces without controller implementations, so the project's own thin controllers implement them and no logic lands in generated code.
- `modelNameSuffix` is the setting that produces `UserTO` instead of `UserDto`. Omit it only under the interfaces-only resolution, where no models are generated at all.
- `useTags` groups operations by tag, so one interface per resource rather than one per path. This is why every operation must carry exactly one tag.
- `documentationProvider=none` stops the generator from adding a springdoc or Swagger dependency. The committed document is the documentation; a generated one would be a second source.
- `openApiNullable=false` avoids the `JsonNullable` wrapper types, which leak an extra library into every signature. Turn it on only if the project deliberately adopts that library.
- `useJakartaEe` is required on Spring Boot 3.

Bind generation to the phase that runs before compilation so the interfaces exist when the
controllers compile, and confirm the generated sources are on the compile source root.

## Working with generated interfaces

The controller implements the generated interface and stays a thin transport boundary, exactly as
`spring-boot-patterns` requires.

```java
@RestController
public class UserController implements UsersApi {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserTO> usersUserIdGet(final Long userId) {
        final UserDomain user = this.userService.getById(userId);

        return ResponseEntity.ok(UserRestMapper.INSTANCE.mapUserDomainToUserTO(user));
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
- Do not commit generated sources, and exclude the generated directory from formatting and quality gates. Gating generated code produces failures nobody can act on.
- Keep the document in the repository, reviewed like source. It is the artifact consumers depend on.
- Validate the document in the build before generating from it, so a malformed contract fails early with a clear message rather than as a generator stack trace.
- When the generator's output disagrees with a project convention, change the generator configuration or the convention deliberately. Do not paper over it with a hand-written wrapper type.
