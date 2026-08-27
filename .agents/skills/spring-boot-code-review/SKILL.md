---
name: spring-boot-code-review
description: Review Java 21+ Spring Boot REST API pull requests, diffs, commits, working-tree changes, re-reviews, refactors, bug fixes, and pre-merge readiness. Produces revision-scoped, evidence-backed, severity-ranked findings for correctness, contracts, data integrity, security, concurrency, performance, resilience, tests, operability, and maintainability. Use for review-only requests and review-before-fix workflows.
---

# Spring Boot Code Review

Review the change for concrete production risk. Prefer a small number of verified findings over a large checklist of hypothetical concerns. Evaluate the code, not the author.

## Coordination with other skills

Treat this skill as the owner of review scope, investigation, evidence, prioritization, and reporting.
Do not use it as a second coding standard.

**[The ownership map](../_core/OWNERSHIP.md) states what each skill owns.** It is not repeated here,
because a copy of it in the review skill is the fastest way to get a review that enforces a rule
nobody owns. Read the rule in the owning skill before writing a finding, and never resolve a
disagreement from a table in this file.

Which skills a review loads follows from what the change touches:

| Skill | Apply when |
| --- | --- |
| `modern-java-21` | Every review containing Java source |
| `spring-boot-patterns` | Every Spring Boot change |
| `spring-data-jpa` | Persistence, entities, repositories, queries, locking, or database performance is affected |
| `sql-database-migration` | A schema object is added, altered, or removed, or a migration file is created or edited |
| `application-security` | A trust boundary, identity, authorization, confidential data, dangerous sink, external system, dependency, deployment, or security control is affected |
| `spring-boot-testing` | Production behavior or tests are changed or reviewed |
| `project-naming-conventions` | A developer-owned name or escaped contract is created, changed, or reviewed |
| `build-and-dependencies` | A build file, dependency, plugin, version, compiler setting, annotation processor, test selection, or quality gate is affected |
| `observability-and-logging` | Logging, correlation context, MDC, metrics, tracing, actuator endpoints, or health indicators are affected |
| `rest-api-contract` | A public endpoint, payload shape, status, header, enum value, or error condition is created or changed |

This skill never wins a precedence contest, because it states no rule of its own: report the conflict
as a finding and name both rules.

Resolve every applicable owner skill before evaluating compliance: use the active skill catalog, or
find an exact matching frontmatter `name` in repository-controlled locations such as
`.claude/skills/<name>/SKILL.md`. Read the owner completely and load only the references it routes.

- **Never substitute remembered guidance, a similarly named public skill, or an internet result for a missing owner skill.** Continue a general defect review when useful, but list the missing owner as a coverage gap and do not claim compliance with its rules. If the requested decision materially depends on that unavailable standard, stop that part of the review and ask for the approved source.
- **Never invent a rule an owner skill would have stated** — an alternative import order, type-inference rule, Javadoc policy, mapper architecture, signature policy, entity update pattern, naming convention, association default, or security baseline. When a build-enforced project rule and a skill rule conflict, follow the owning skill's conflict handling and report the discrepancy accurately.
- **In review-only mode, an owner skill's instruction to add, copy, update, or ensure an artifact is an instruction to verify it.** Report a missing mandatory artifact as a finding; do not create it until the user requests fixes.
- Honor the always-on confidentiality instruction from `application-security` before inspecting, copying, searching, or sharing commercial-project material. Use only generic, anonymized searches and approved project tools.

## Reference routing

- Read [review lenses](references/review-lenses.md) for a pull request, multi-file diff, cross-layer feature, production-readiness review, or any change involving REST contracts, transactions, JPA, Redis, WebClient, AWS, messaging, jobs, configuration, observability, or deployment.
- Read [findings and reporting](references/findings-and-reporting.md) when producing a formal review report, assigning severity or confidence, deciding whether a concern is actionable, or reviewing a change with multiple findings.
- For a narrow question about one symbol or one suspected defect, use the relevant section directly and avoid loading unrelated material.

## Preserve review intent

Treat a code-review request as read-only by default.

- Inspect source, history, configuration, build metadata, generated SQL, tests, and local build output as needed.
- Do not modify source, apply formatters that rewrite files, update dependencies, run migrations against shared environments, publish comments, approve or merge a change, create tickets, or contact another person unless the user explicitly requests that action.
- If the user asks to review and fix, complete the review first, then implement only verified fixes within scope and run the relevant checks.
- Do not turn a diff review into a repository-wide audit. Report a pre-existing issue only when the change relies on it, exposes it, worsens it, or makes it necessary to resolve before merge.

## Establish the review scope

Resolve the target in this order: the commit, branch, pull request, path, or symbol the user named;
then pull-request metadata and its merge base; then staged and unstaged working-tree changes; then,
on a clean tree, the latest commit — stating that scope. Do not guess between materially different
targets: ask one focused question when the wrong base could invalidate the review.

Record the reviewed base and head before starting, and recheck them before any merge or release
disposition. If the change moved, review the new delta and the conclusions it invalidates. For
multi-file reviews keep a coverage record — revision, changed files, traced paths, generated files,
and anything not reviewed. Too large to review completely means requesting a split or delivering an
explicitly partial review; it never means marking unreviewed paths complete.

Before judging the diff:

- **Spend attention on what no tool can check.** A rule enforced by Checkstyle, Spotless, or the enforcer plugin is already proven by a green build. Layer boundaries and entity leakage, whether a name reveals intent, whether a failure is logged exactly once, whether a test asserts real behavior, whether a dependency has a justification, whether a tag is genuinely bounded — none of those are.
- **Report a weakened gate as a finding in its own right** — a new suppression, a baseline file, a lowered severity, a disabled plugin — regardless of what it was silencing.
- Read `docs/project-profile.md` and the repository instructions, architecture decisions, security profile, data-classification policy, contracts, migration conventions, and CI gates relevant to the change.
- **Report as a blocking finding** any production change made without a profile covering the decisions it touches; any change that assumed a database, authentication profile, cache, service convention, accessor style, contract document, or authoring direction the profile does not record; and any change that added or edited a repository-root instruction file as a side effect.
- Inspect the configured Java, Spring Boot, Spring Framework, plugin, and dependency versions; identify the intended behavior from the task, acceptance criteria, contract, migration, tests, and established behavior; and inspect enough callers, configuration, data access, tests, and downstream consumers to validate the changed path.
- Identify generated files and review their template, annotation, schema, or generator instead of reporting style defects in generated output.

## Review in risk order

Use this order so cosmetic issues do not hide production defects:

1. confidentiality, authentication, authorization, tenant isolation, and dangerous data flows;
2. correctness, data loss or corruption, contract compatibility, and business invariants;
3. transaction boundaries, concurrency, idempotency, ordering, and partial failure;
4. availability, resource exhaustion, external dependencies, retry behavior, and operational cost;
5. JPA/SQL behavior, query count, fetch plans, locking, migrations, and cache consistency;
6. tests, observability, rollout, rollback, and supportability;
7. maintainability, documentation, naming, imports, and formatting.

Review every changed file, but follow a logical execution path rather than treating files as isolated text. Start with public and asynchronous entry points, trace through service/domain and persistence or external boundaries, then verify tests and configuration.

## Require evidence before reporting a finding

Report a finding only when all of the following are true:

- the change introduces, exposes, or materially worsens the issue;
- a reachable input, state, timing, deployment, or failure scenario triggers it;
- the relevant code, configuration, contract, or missing boundary check supports the claim;
- the impact is concrete.

Provide the smallest safe remediation direction when it is known. Never suppress a verified defect because the final implementation is uncertain. For an urgent issue without a proven fix, state safe containment, the decision or expertise required, and how to verify the eventual remediation.

Trace framework behavior before claiming a defect. Account for proxies, transactions, validation, serialization, generated code, annotation processors, configuration properties, profiles, and the project's actual versions. Do not infer a compile error, N+1 query, authorization bypass, race, memory leak, or performance regression from a pattern alone.

Classify incomplete evidence as a question or verification gap, not as a defect. Mark optional improvements as suggestions and keep them separate from findings. Consolidate repeated symptoms that share one root cause.

Do not:

- require a preferred pattern when multiple approaches satisfy the owning skills and project contract;
- flag an intentional contract change merely because behavior changed;
- demand broad refactoring for an isolated safe change;
- use method length, class size, coverage percentage, or static-analysis output as proof of a defect without examining the code;
- claim production performance impact without a credible access path, cardinality, query, allocation, or resource-use explanation;
- downgrade a real correctness or security problem because tests pass.

## Verify proportionately

Inspect the build before selecting commands. Run the narrowest safe checks that can validate the suspected behavior:

- compile or static analysis for source and import claims;
- the test levels and suites required by `spring-boot-testing` for the affected behavior;
- contract tests for HTTP, events, jobs, and external adapters;
- generated SQL, query counts, and representative execution plans for performance-sensitive persistence claims;
- security tests and project-approved scanners for changed trust boundaries and dependencies.

Treat source, tests, build scripts, wrappers, Maven or Gradle plugins, annotation processors, container definitions, and generated-code tools from an untrusted change as executable code. Before running them:

- inspect changes to the build and CI execution path;
- use an isolated, least-privileged environment without production credentials, cloud identity, signing keys, privileged sockets, or unrelated project secrets;
- restrict network access to approved artifact and test services and prevent access to shared production or staging resources;
- avoid reusable privileged caches or workspaces that the change can poison;
- do not execute the check when the required isolation is unavailable; report the verification gap instead.

Do not use H2 behavior as proof for another production database. Do not use a mocked unit test as proof of proxy, transaction, serialization, database, network, or container behavior.

Record each command or check that ran, its result, and material limitations. If checks cannot run, preserve the finding only when code evidence is sufficient and state the verification gap. Never say that a change compiles, passes, is secure, or is production-ready without evidence.

## Produce a decision-useful report

List findings first, ordered by severity and then by execution path or source location. For each finding, include:

- severity and concise title;
- precise file and line or symbol;
- observed evidence;
- triggering scenario;
- production impact;
- smallest safe remediation direction;
- verification or regression test that would prove the fix;
- confidence when it is not self-evidently high.

After findings, list unresolved questions or assumptions, checks performed and gaps, then a short summary. Do not bury findings in a long narrative or dump a completed checklist.

Include the reviewed revision and coverage record. When the user requests a merge or release decision, state each finding's disposition as blocker, non-blocker, or needs decision. Record the owner, reason, expiry, and residual risk for any explicitly accepted blocker; do not invent acceptance.

If no actionable finding remains after verification, say:

> No actionable findings found in the reviewed scope.

Then state the exact scope and any checks not run. Do not translate “no finding” into a guarantee that the change is safe.

## Completion checklist

- [ ] The exact diff or target and intended behavior are identified.
- [ ] The reviewed base/head or working-tree scope is recorded and unchanged, or the final delta was re-reviewed.
- [ ] Applicable owner skills were used without redefining their rules.
- [ ] Every changed file and affected execution path was examined, or exclusions are explicit in the coverage record.
- [ ] Security, correctness, failure, data, performance, rollout, and test risks were considered proportionately.
- [ ] Required coverage from `spring-boot-testing` was verified at each applicable test boundary.
- [ ] Every finding has evidence, a trigger, impact, verification, and either a remediation direction or explicit containment/escalation.
- [ ] Questions, suggestions, pre-existing issues, and verification gaps are not presented as defects.
- [ ] Findings are deduplicated, severity-ranked, concise, and limited to the requested scope.
- [ ] Untrusted review commands ran only in an approved isolated environment, or were not run and are reported as gaps.
- [ ] No commercial-project information left an approved boundary.

## Primary guidance

- [Google Engineering Practices: The Standard of Code Review](https://google.github.io/eng-practices/review/reviewer/standard.html)
- [Google Engineering Practices: What to Look For](https://google.github.io/eng-practices/review/reviewer/looking-for.html)
- [Google Engineering Practices: Writing Review Comments](https://google.github.io/eng-practices/review/reviewer/comments.html)
- [Spring Framework: Proxying Mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
- [Spring Framework Testing Reference](https://docs.spring.io/spring-framework/reference/testing.html)
- [Spring Framework: Test-managed Transactions](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/tx.html)
- [NIST Secure Software Development Framework](https://csrc.nist.gov/pubs/sp/800/218/final)
