---
name: spring-boot-code-review
description: Review Java 21+ Spring Boot REST API pull requests, diffs, commits, working-tree changes, refactors, bug fixes, and pre-merge readiness. Coordinate modern-java-21, spring-boot-patterns, spring-data-jpa, and application-security without redefining their rules. Produce evidence-backed, severity-ranked findings for correctness, contracts, data integrity, security, concurrency, performance, resilience, tests, operability, and maintainability. Use for review-only requests and for review-before-fix workflows.
---

# Spring Boot Code Review

Review the change for concrete production risk. Prefer a small number of verified findings over a large checklist of hypothetical concerns. Evaluate the code, not the author.

## Coordinate the project skills

Treat this skill as the owner of review scope, investigation, evidence, prioritization, and reporting. Do not use it as a second coding standard.

Apply the normative skills as follows:

| Skill | Apply when | Treat as owner of |
|---|---|---|
| `modern-java-21` | Every review containing Java source | Java 21 usage, explicit types, imports, Javadoc, nullability, exceptions, source structure, and general test rules |
| `spring-boot-patterns` | Every Spring Boot change | REST-only boundaries, TO–Domain–Entity architecture, mappers, services, validation, errors, configuration, transactions, and feature structure |
| `spring-data-jpa` | Persistence, entities, repositories, queries, migrations, locking, or database performance is affected | JPA mappings, association ownership, fetch plans, SQL/query behavior, transactions, locking, migrations, and persistence tests |
| `application-security` | A trust boundary, identity, authorization, confidential data, dangerous sink, external system, dependency, deployment, or security control is affected | Confidentiality, threat analysis, authentication, authorization, abuse prevention, secrets, cloud and messaging security, and security verification |

Honor the always-on confidentiality instruction from `application-security` before inspecting, copying, searching, or sharing commercial-project material. Use only generic, anonymized internet searches and approved project tools.

Do not restate an owner skill's exact rule in this skill. In particular, do not invent an alternative import order, Javadoc policy, mapper architecture, service signature policy, entity update pattern, association default, or security baseline. If a build-enforced project rule and a skill rule conflict, follow the conflict handling defined by the owning skill and report the discrepancy accurately.

In review-only mode, interpret an owner skill's instruction to add, copy, update, or ensure a repository artifact as an instruction to verify it. Report a missing mandatory artifact as a finding; do not create it until the user requests fixes.

## Route the references

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

Resolve the target in this order:

1. Use the commit, branch, pull request, base branch, path, or symbol explicitly named by the user.
2. Use available pull-request metadata and its merge base when the request refers to the current pull request.
3. Review staged and unstaged working-tree changes when the request refers to current local changes.
4. If the tree is clean and the user asks to review the latest change, review the latest commit and state that scope.

Do not guess between materially different targets. Ask one focused question when choosing the wrong base or range could invalidate the review.

Before judging the diff:

- inspect the configured Java, Spring Boot, Spring Framework, build-plugin, and dependency versions relevant to the change;
- identify the intended behavior from the task, acceptance criteria, API or event contract, migration, tests, and established behavior;
- inspect enough callers, implementations, configuration, data access, tests, and downstream consumers to validate the changed path;
- identify generated files and review their source template, annotation, schema, or generator instead of reporting style defects in generated output.

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
- the impact is concrete;
- a practical remediation direction exists.

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
- focused unit or slice tests for local behavior and REST boundaries;
- integration and supported-database tests for persistence, transaction, serialization, and configuration behavior;
- contract tests for HTTP, events, jobs, and external adapters;
- generated SQL, query counts, and representative execution plans for performance-sensitive persistence claims;
- security tests and project-approved scanners for changed trust boundaries and dependencies.

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

If no actionable finding remains after verification, say:

> No actionable findings found in the reviewed scope.

Then state the exact scope and any checks not run. Do not translate “no finding” into a guarantee that the change is safe.

## Complete the review

- [ ] The exact diff or target and intended behavior are identified.
- [ ] Applicable owner skills were used without redefining their rules.
- [ ] Every changed file and every affected execution path was examined.
- [ ] Security, correctness, failure, data, performance, rollout, and test risks were considered proportionately.
- [ ] Every finding has evidence, a trigger, impact, remediation direction, and verification.
- [ ] Questions, suggestions, pre-existing issues, and verification gaps are not presented as defects.
- [ ] Findings are deduplicated, severity-ranked, concise, and limited to the requested scope.
- [ ] No commercial-project information left an approved boundary.

## Primary guidance

- [Google Engineering Practices: The Standard of Code Review](https://google.github.io/eng-practices/review/reviewer/standard.html)
- [Google Engineering Practices: What to Look For](https://google.github.io/eng-practices/review/reviewer/looking-for.html)
- [Google Engineering Practices: Writing Review Comments](https://google.github.io/eng-practices/review/reviewer/comments.html)
- [Spring Framework Testing Reference](https://docs.spring.io/spring-framework/reference/testing.html)
