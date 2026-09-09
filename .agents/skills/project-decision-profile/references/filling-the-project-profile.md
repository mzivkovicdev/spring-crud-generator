# Filling the project profile

Use this reference when creating `docs/project-profile.md`, when a required decision is missing from
it, or when a change makes one of its entries obsolete. Apply every rule from `../SKILL.md`, which
states the gate and the token vocabulary; this file carries the order of work behind them.

**This file carries rules, not only guidance.** The order of work, the completeness check, and the
test for which token a *non-build* decision carries are stated here in full and nowhere else.
`build-and-dependencies` classifies every build and version row and states why; do not re-derive one
here. Each skill named in the template owns the meaning of its own rows, so the notes below say what
the mechanism needs and point at the owner for the rest.

## Which token a decision carries

The test is whether a correct answer exists independently of this project.

A decision is **`ASK`** when it does not: the database engine, whether a contract document exists,
the authoring direction, the service interface convention, the entity accessor style, the message
broker. Nothing in the repository or the ecosystem points to one answer, and a wrong one is expensive
to reverse.

A decision is **`RESOLVE`** when it does, and the answer only needs looking up.

`UNDECIDED` is neither — it is a deliberate deferral that names what will force the decision, and it
is the correct entry for a `RESOLVE` that could not be completed.

Build and version rows are classified by `build-and-dependencies`, in the table its `SKILL.md`
carries. Read the token off the template row rather than deciding it from the test above.

## How to fill it

The profile file exists before this list starts; `../SKILL.md` says where it comes from. Identify
which entries the task actually needs, then work through these in order. The order matters: every
step above another is cheaper, and asking the user is deliberately last.

1. **Take what the repository proves.** A declared dependency, an applied migration, an existing package layout, or a configured datasource is an answer. Record it and move on. Proof is narrower than evidence: a *test* dependency and an example prove nothing, and `../SKILL.md` states why.
2. **Complete every `RESOLVE` yourself.** Look the answer up, record it with the date, and state it in the handoff. Do not ask the user for something that has a correct answer.
3. **Apply the `Fallback` where the template records one**, writing it into the `Value` column and naming it in the handoff. Read the fallback off the row being filled, not off the row beside it.
4. **Ask once for every `ASK` with no fallback.** One message listing every unresolved decision the task depends on, not one question per skill. Present the options the template lists so the user can answer in a word.
5. **Leave `UNDECIDED` for anything genuinely deferred**, and add a sentence saying what will force the decision. `UNDECIDED` is a legitimate value; a guessed value is not.
6. **Update the entry in the same change that changes the decision.** A profile that disagrees with the code is worse than no profile, because the disagreement is invisible: every later task reads the stale row and trusts it.

Check the filled profile against the template before relying on it: no row may still hold a bare
token, no row may hold a value outside the ones the template lists, and no row from the template may
be missing. A missing row is the dangerous case, because an absent decision looks like a settled one
while an empty `ASK` row is a visible question.

## Notes on specific entries

- **Spring Boot generation** is recorded separately from the version because rules branch on the generation, not on the patch level. Every skill in this set reads this one row to decide which of its two documented cases applies. `build-and-dependencies` owns the choice; a project on an existing build has it already, and a new project is asked.
- **Spring Boot version records the minor line too.** Rules branch on the generation, but coordinates and properties can differ between minor lines within Spring Boot 4, so `4` alone does not tell a later task what it is looking at. Both generations are fully supported by this skill set; the version rows decide which branch *this* project sits on, not which generations exist.
- **Support end date and support model** belong together. The date is a lookup and never a memory; the model is a procurement fact only the user knows. A branch past its open-source end date is a legitimate choice when a commercial subscription covers it and someone has confirmed that — and an unpatched production system when nobody has. Recording both is what makes the difference visible in review instead of a year later.
- **Token issuance profile** blocks integration tests that need a credential. Until it is decided, record it as a deferred decision *with its removal condition*, which is what lets `spring-boot-testing` allow a temporary stand-in without the stand-in becoming permanent.
- **Entity accessor style** and **service interface convention** both change generated code shape, so a project that leaves them unrecorded will produce a different shape per feature.
- **Aggregate roots** decide which service owns which table. Recording them once prevents two features from splitting the same aggregate differently; add a root the first time a feature introduces one.
- **Stale-write protection** has a fallback that is safe for some write shapes and wrong for others, so read `spring-data-jpa` before applying it — that skill states which shape the project has. `rest-api-contract` documents whichever shape is chosen.
- **Messaging splits across three rows, and only two are unowned.** `Reliable-delivery mechanism` is a `spring-boot-patterns` decision and blocks like any other row with an empty fallback; `Message broker` and `Message ordering guarantee required` are the `none yet` rows, under the guard `../SKILL.md` states. Read [`_core/README.md`](../../_core/README.md) before concluding a messaging rule is missing — most of the topic is owned.
- **Caching** is answered in order, and the order matters: `Cache used` gates everything below it, the technology and topology decide which rules apply, and the register is filled one row per cache as caches are added rather than up front. `application-caching` owns all of them; `application-security` still owns what may be cached and `project-naming-conventions` what the caches and keys are called. A project that leaves `Cache used` at `no` has a complete, valid profile — that is the point of the row.
- **Concurrency model** is answered before the performance rows below it mean anything, because it decides what the database pool and the per-caller limits are actually limiting. It is also the one row here whose *change* is a change to three other rows: `spring-boot-patterns` states which, and a project that switches models without re-deriving them has moved its bottleneck somewhere nobody sized.
- **Ingress request ceiling** has no fallback on purpose. Nothing inside the application bounds a synchronous request, so a project that leaves this empty has no request budget at all — only a number in a row that nothing enforces.
- **Connection-level settings channel** is a `RESOLVE` because the engine and driver decide it, but it is worth filling early: it is one string or one property map per pool, shared by the statement, lock, and idle-in-transaction settings, and a project that discovers that after configuring the second one has silently lost the first.
- **Nullability enforcement** records how hard the contract is checked, never whether the annotations are written — that is not a decision and has no row. `build-and-dependencies` owns the choice, what each level costs, and the resolved JSpecify version.
- **Management authority** depends on whether a custom authority converter is installed; record the literal value the configuration uses, not the scope name.
- **Quality gate commands** exist so that the first response to a failed gate is to run the fixer rather than to disable the gate.
- **Interactive UI exposed** and **Exposed actuator endpoints** follow the same split: the skill that owns the artifact records whether it is exposed, and `application-security` owns how it is protected wherever it is.
- **Contract document** is decided before the authoring direction, and `none` is a legitimate answer. Without a document there is no drift gate and no generated client, so breaking-change judgement rests entirely on review. Do not record `OpenAPI` because springdoc is on the classpath.
- **Authoring direction** applies only when the contract document is OpenAPI, and must be decided before the first endpoint. It cannot be switched later without a dedicated project, and under contract-first it also forces the generated-type naming resolution. `rest-api-contract` presents the trade-off; the user chooses.
- **Committed document path** records one file, and its extension is the format decision: JSON or YAML, never both. `rest-api-contract` explains the trade-off and requires the drift gate to read the endpoint matching the recorded extension.
- **Known consumers** is the list a breaking change must be confirmed against. It carries the most weight when there is no document, because nothing else surfaces a contract change to the people it affects.
- **Resolved tool versions** is the audit trail for every `RESOLVE`. A row without a date is a version nobody can justify later; a row holding a number with no resolution date is the exact failure the token mechanism exists to prevent.
