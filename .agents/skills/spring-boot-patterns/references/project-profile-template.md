# Filling the project profile

Use this reference when creating `docs/project-profile.md`, when a required decision is missing from
it, or when a change makes one of its entries obsolete.

The template itself is a file, not a snippet: copy
[`../assets/project-profile-template.md`](../assets/project-profile-template.md) to
`docs/project-profile.md` and fill it in. Do not retype it, and do not drop rows that look
irrelevant today — an empty `ASK` row is a visible question, while a missing row is a decision
nobody knows was skipped.

`spring-boot-patterns` owns the file and the decision tokens defined in `../SKILL.md`; each skill
named in the template owns the meaning of its own entries.

## How to fill it

1. **Take what the repository proves.** A declared dependency, an applied migration, an existing package layout, or a configured datasource is an answer. Record it and move on.
2. **Complete every `RESOLVE` yourself.** Look the answer up, record it with the date, and state it in the handoff. Do not ask the user for something that has a correct answer.
3. **Apply the `Fallback` where the template records one.** Those rows have a sanctioned safe answer, so an unanswered decision does not block: write the fallback into the `Value` column and say in the handoff that a fallback was applied. Never invent a fallback for a row whose `Fallback` cell is empty, and never carry a fallback from one row to another.
4. **Ask once for every `ASK` with no fallback.** One message listing every unresolved decision the task depends on, not one question per skill. Present the options the template lists so the user can answer in a word.
5. **Leave `UNDECIDED` for anything genuinely deferred**, and add a sentence saying what will force the decision. `UNDECIDED` is a legitimate value; a guessed value is not.
6. **Never infer a decision from a test dependency or an example.** An H2 dependency does not make H2 the database, and an example showing Lombok does not make Lombok a project choice.
7. **Update the entry in the same change that changes the decision.** A profile that disagrees with the code is worse than no profile.

Check the filled profile against the template before relying on it: no row may still hold a bare
token, no row may hold a value outside the ones the template lists, and no row from the template may
be missing. A missing row is the dangerous case, because an absent decision looks like a settled one
while an empty `ASK` row is a visible question.

## Notes on specific entries

- **Spring Boot generation** is recorded separately from the version because rules branch on the generation, not on the patch level. Every skill in this set reads this one row to decide which of its two documented cases applies. `build-and-dependencies` owns the choice; a project on an existing build has it already, and a new project is asked.
- **Token issuance profile** determines how integration tests obtain a credential. Until it is decided, `spring-boot-testing` permits a documented temporary test-only issuer; record that here as a deferred decision with its removal condition.
- **Entity accessor style** and **service interface convention** both change generated code shape, so a project that leaves them unrecorded will produce a different shape per feature.
- **Aggregate roots** decide which service owns which table. Recording them once prevents two features from splitting the same aggregate differently; add a root the first time a feature introduces one.
- **Stale-write protection** decides what a caller must send to prove which version it edited. The fallback, `server retry only`, is correct whenever every write recomputes from state the server re-reads; it is wrong the moment a caller submits a full representation a human edited from a stale read. `spring-data-jpa` explains the distinction and owns the retry annotation; `rest-api-contract` documents whichever shape the project chose.
- **Caching has no owner skill yet.** No skill in this set decides whether the project uses a cache or which technology it uses; a caching skill will own that. Record the answer here when it is made, and leave both rows `UNDECIDED` until then. Do not introduce a cache to fill the row.
  - `application-security` owns what may be cached and under what conditions: classification of cached values, TTL, tenant scope, serialization, and eviction of sensitive data. It does not own the decision itself.
  - `project-naming-conventions` owns cache and cache-key names.
- **Management authority** depends on whether a custom authority converter is installed; record the literal value the configuration uses, not the scope name.
- **Quality gate commands** exist so that the first response to a failed gate is to run the fixer rather than to disable the gate.
- **Interactive UI exposed** follows the same split as the actuator row above: the skill that owns the artifact records whether it is exposed, and `application-security` owns how it is protected wherever it is. Recording `never` is a valid and common answer.
- **Contract document** is decided before the authoring direction, and `none` is a legitimate answer. Without a document there is no drift gate and no generated client, so breaking-change judgement rests entirely on review. Do not record `OpenAPI` because springdoc is on the classpath.
- **Authoring direction** applies only when the contract document is OpenAPI, and must be decided before the first endpoint. It cannot be switched later without a dedicated project, and under contract-first it also forces the generated-type naming resolution. `rest-api-contract` presents the trade-off; the user chooses.
- **Known consumers** is the list a breaking change must be confirmed against. It carries the most weight when there is no document, because nothing else surfaces a contract change to the people it affects.
- **Resolved tool versions** is the audit trail for every `RESOLVE`. A row without a date is a version nobody can justify later; a row holding a number with no resolution date is the exact failure the token mechanism exists to prevent.
