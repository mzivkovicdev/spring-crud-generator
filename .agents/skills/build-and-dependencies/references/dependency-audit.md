# Dependency audit and removal

Use this reference when adding or replacing a dependency, and whenever the user asks which
dependencies do not belong in the project. Apply every rule from `../SKILL.md`. Apply
`application-security` for vulnerability triage and provenance, and `spring-boot-code-review` when
the task is review-only.

## Contents

1. [When to run an audit](#when-to-run-an-audit)
2. [Gather evidence](#gather-evidence)
3. [Classify every declared dependency](#classify-every-declared-dependency)
4. [Check the false-positive list first](#check-the-false-positive-list-first)
5. [Recognize dependencies that do not belong](#recognize-dependencies-that-do-not-belong)
6. [Report before removing](#report-before-removing)
7. [Remove safely](#remove-safely)
8. [Replacing rather than removing](#replacing-rather-than-removing)

## When to run an audit

Run the full audit only when the user asks for it, in words such as "check which dependencies we do
not need", "clean up the build", or "why is this here". Removing dependencies is a behavioral change
with runtime consequences, so it is never a side effect of another task.

Outside an explicit request:

- When adding a dependency, run the justification gate from `../SKILL.md` and check only for an existing library that already provides the capability.
- When reviewing a change, report a newly introduced unnecessary or duplicated dependency as a finding under `spring-boot-code-review`, with evidence, and do not remove it.
- When a dependency is obviously dead — its last usage was deleted in the same change — say so and offer to remove it.

## Gather evidence

Never audit from the dependency declarations alone. Produce the resolved graph, because a direct
declaration and an effective dependency are different things.

Maven:

```text
./mvnw dependency:tree
./mvnw dependency:tree -Dincludes=<groupId>:<artifactId>
./mvnw dependency:analyze
./mvnw -N dependency:analyze-duplicate
```

Gradle:

```text
./gradlew dependencies --configuration runtimeClasspath
./gradlew dependencies --configuration testRuntimeClasspath
./gradlew dependencyInsight --dependency <artifact> --configuration runtimeClasspath
./gradlew buildEnvironment
```

Then search the source directly. A static analyzer sees imports; it does not see reflection,
service loading, auto-configuration, SpEL, annotations processed at build time, or a class named in
a properties file.

- Search main and test sources for the library's packages.
- Search `application.yml`, `application.properties`, and every profile for class names, driver names, dialect names, and property prefixes the library owns.
- Search for the library's annotations, including ones only a processor reads.
- Check `META-INF` entries, `spring.factories`, and auto-configuration imports contributed by the dependency.
- Check the build files themselves: a library used only by a plugin or a processor never appears in source.

## Classify every declared dependency

Put each declared dependency in exactly one bucket:

| Bucket | Meaning | Action |
| --- | --- | --- |
| Used directly | Main or test source imports it | Keep. Verify the scope is right. |
| Used at runtime only | Loaded by name, auto-configured, or a backend implementation | Keep. Expect every analyzer to call it unused. |
| Used by the build only | Annotation processor, plugin dependency, test infrastructure | Keep, in the right configuration. |
| Duplicated capability | A second library doing what an existing one already does | Candidate for removal. |
| Obsolete | Its usage was removed, or Spring Boot or the JDK absorbed it | Candidate for removal. |
| Misscoped | Correct library, wrong scope | Fix the scope, do not remove. |
| No evidence | Nothing in source, configuration, or build references it | Candidate, only after the checks below. |

Transitive dependencies are not audited for removal. You do not declare them, so you do not remove
them; you remove or replace the direct dependency that pulls them in.

## Check the false-positive list first

Every one of these is genuinely required and reported as unused by static analysis. Removing them
produces a build that compiles and an application that fails at startup or under load. Verify
against this list before proposing any removal.

| Dependency kind | Why it looks unused |
| --- | --- |
| JDBC driver | Loaded by name from the datasource URL |
| Flyway or Liquibase | Invoked by auto-configuration, never imported |
| Logging backend and bridges | Bound through SLF4J at runtime |
| `spring-boot-configuration-processor` | Runs at build time only, produces metadata |
| `lombok-mapstruct-binding` | Exists only to order two other processors |
| `mapstruct` runtime artifact | Imported only by generated sources |
| Starters that only contribute auto-configuration, such as actuator or validation | No import in project source |
| Micrometer registry implementations | Discovered and wired by auto-configuration |
| Testcontainers database modules | Referenced by class name or by service connection |
| Jackson modules such as JSR-310 or the parameter-names module | Registered automatically |
| A dependency used only in a non-default profile or a non-default source set | Outside the analyzed scope |
| Servlet container or WebSocket implementations | Provided, not imported |

If a candidate is on this list, it is not a finding. Say why it is required and move on.

## Recognize dependencies that do not belong

These are the patterns that actually justify removal, ordered by how often they appear in a Spring
Boot project.

**Duplicated capability.** One project needs one of each. Look for:

- two JSON libraries, typically Jackson plus Gson;
- two assertion libraries, typically AssertJ plus Hamcrest used deliberately;
- two mocking frameworks, typically Mockito plus EasyMock or PowerMock;
- two HTTP clients, typically `RestClient` or `WebClient` plus OkHttp or Apache HttpClient used directly;
- two test-data generators;
- two mapping libraries, typically MapStruct plus ModelMapper or Dozer;
- two logging facades, or a backend bound twice.

**Absorbed by the platform.** The JDK or Spring Boot now provides it:

- a collections or utility library used for one or two methods the JDK has had since Java 9, such as `List.of`, `Objects.requireNonNullElse`, or `String.join`;
- a string or IO helper library where `java.nio.file.Files` and text blocks suffice;
- a connection pool that duplicates HikariCP;
- a JSON path or date library that overlaps what the starters already bring;
- a validation helper that duplicates Jakarta Bean Validation.

**Added for one method.** A whole library, its transitive graph, and its future CVEs in exchange for
one utility call. Write the method instead, in a focused, named class.

**Never used at all.** A dependency added during an experiment, with no reference anywhere in source,
configuration, or build. Common examples are a reactive starter in a servlet application, a security
starter in a service with no security configuration, a messaging client for a broker the service
does not use, and a caching library when no cache was ever selected.

**Wrong scope rather than wrong dependency.** A test library on the compile classpath, a driver on
the compile classpath, or an annotation-only library packaged into the artifact. Fix the scope; do
not delete it.

**Unmaintained or unnecessarily risky.** No release in years, an unresolved critical advisory with
no upgrade path, or a single-maintainer package doing something the platform already does. Escalate
through `application-security` rather than removing it unilaterally.

**Version pinned over the BOM.** Not a removal, but a defect in the same family: a managed artifact
carrying an explicit version desynchronizes the tested set. Remove the version, not the dependency,
and verify.

## Report before removing

Produce the report first. The user decides. For each candidate:

```text
[Candidate] com.example:some-library — duplicated capability

Declaration: pom.xml, compile scope
Evidence:    No import in src/main or src/test. Jackson already provides JSON
             serialization and is used in 14 files.
Pulls in:    3 transitive artifacts, including one with an open advisory.
Risk:        Low. No reflection, configuration, or auto-configuration reference found.
Verification: Full build including integration tests and application startup.
```

Rules for the report:

- Group by bucket, and put duplicated-capability and never-used candidates first.
- State the risk honestly. "No evidence found" is not the same as "proven unused", and a dependency reached only by reflection or a rare profile is medium risk however clean the search looked.
- List what each removal also removes transitively, because that is often the real benefit.
- Keep dependencies you verified as required in a short "keep, and why" section, so the same false positives are not re-raised in the next audit.
- Never present the analyzer output as the finding. The analyzer produced the lead; the evidence is what you found in the source, configuration, and build.

## Remove safely

Once the user approves:

1. Remove **one** dependency at a time. A batch removal that breaks the build tells you nothing about which one was responsible.
2. After each removal, run the full verification, not just compilation:
   - compile main and test sources;
   - inspect generated sources if a processor or a mapper could be affected;
   - run unit, slice, persistence, and integration suites per `spring-boot-testing`;
   - start the application against the real configuration and confirm it reaches a healthy state.
3. Application startup is mandatory. Most genuine false positives — drivers, migration tools, auto-configuration — fail at startup and nowhere else.
4. Check that no transitive artifact you still need disappeared with the removal. If it did, declare it directly rather than restoring the removed dependency.
5. Remove everything the dependency owned along with it: configuration properties, profile entries, excluded classes, suppressions, documentation, and now-dead code.
6. Keep the removals in their own change, separate from feature work, so a revert is cheap.

If verification cannot run — no container runtime, no representative configuration — do not remove.
Report the candidate and the verification gap instead.

## Replacing rather than removing

When a dependency is genuinely needed but the wrong choice, replacement is a design change, not
cleanup:

- Name the capability, then confirm that the platform or an existing dependency cannot provide it.
- Replace in one focused change with the tests that prove equivalent behavior at the boundary the dependency serves.
- Do not leave both libraries declared "during migration" without a recorded removal condition and owner. That is how a project ends up with two of everything.
