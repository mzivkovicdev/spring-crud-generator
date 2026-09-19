# Routing

Which skills a change loads. Every skill links here, so the answer does not depend on which skill a
tool happened to load first.

**Load every row that matches, not only the first.** A typical feature matches five or more rows. When
your tool did not load a matching skill by itself, open `../<name>/SKILL.md` from any skill folder, or
`.agents/skills/<name>/SKILL.md` from the repository root, and follow that skill's reference routing.

A matching skill that cannot be loaded is a coverage gap: say so in the handoff and do not claim
compliance with its rules. Never substitute remembered guidance, a similarly named public skill, or an
internet result for it.

This table decides **which skills to read**. [The ownership map](OWNERSHIP.md) decides **who owns a
rule** and how a genuine conflict between two skills is resolved.

| The change | Load |
| --- | --- |
| Creates or changes production code, before the first line is written | `project-decision-profile` |
| Creates, edits, refactors, fixes, or reviews any `.java` file, production or test | `modern-java-21` |
| Depends on a recorded decision, or `docs/project-profile.md` is missing, stale, or still holds a bare token | `project-decision-profile` |
| Is a Spring Boot change | `spring-boot-patterns` |
| Affects persistence, entities, repositories, queries, locking, or database performance | `spring-data-jpa` |
| Adds, alters, or removes a schema object, or creates or edits a migration file | `sql-database-migration` |
| Affects a trust boundary, identity, authorization, confidential data, a dangerous sink, an external system, a dependency, deployment, or a security control | `application-security` |
| Changes or reviews production behavior or tests | `spring-boot-testing` |
| Creates, changes, or reviews a developer-owned name or an escaped contract | `project-naming-conventions` |
| Affects a build file, dependency, plugin, version, compiler setting, annotation processor, test selection, or quality gate | `build-and-dependencies` |
| Affects logging, correlation context, MDC, metrics, tracing, actuator endpoints, or health indicators | `observability-and-logging` |
| Creates or changes a public endpoint, payload shape, status, header, enum value, or error condition | `rest-api-contract` |
| Adds, configures, reads from, invalidates, or removes a cache — the Hibernate second-level and query caches included — or writes data another cache holds | `application-caching` |
| Is a review of a pull request, diff, commit, or working-tree change | `spring-boot-code-review` |
