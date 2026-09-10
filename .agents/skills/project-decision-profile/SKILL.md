---
name: project-decision-profile
description: Owns docs/project-profile.md and the ASK / RESOLVE / UNDECIDED decision tokens that every other skill in this set reads instead of guessing. Use before writing production code on any task; when creating the profile on a new or empty repository; when a change makes a recorded decision obsolete; and whenever a build, version, database, migration, contract, security, observability, or test decision must be recorded or is missing from the profile. Covers which decisions block and which are looked up, how fallbacks work, and how the profile is filled and kept correct. Not Spring profiles, and not the token-issuance or security profiles those skills name; naming belongs to project-naming-conventions.
---

# Project Decision Profile Skill

One file records what a project decided, so no task has to guess and no two tasks guess
differently. This skill owns that file and the vocabulary it is written in — nothing else.

## Coordination with other skills

This skill owns **the mechanism**: the profile file, the three decision tokens, the fallback rule,
and the order of work for filling a row. It does **not** own the meaning of any individual entry.
Each row names its owner skill, and that owner decides what the value means, what it changes, and
what a wrong answer costs.

[The ownership map](../_core/OWNERSHIP.md) is the canonical statement of who owns what, and it
carries the precedence order for a genuine conflict.

| Seam | This skill owns | The other owner owns |
| --- | --- | --- |
| Any profile row | that the decision is recorded, with which token, and whether it blocks | the named owner decides what the value means |
| Build and version rows | the token vocabulary they are written in | `build-and-dependencies` owns every version choice and the classification of build decisions |
| Architecture rows | that the row exists and is answered | `spring-boot-patterns` owns service shape, aggregates, the error contract, and the API base path |

Do not restate an owner's rule here, and do not decide an entry's meaning from this skill.

## Reference routing

| Read | When |
| --- | --- |
| [Filling the project profile](references/filling-the-project-profile.md) *(rules)* | Creating the profile, filling a missing decision, or judging whether an entry has gone stale. It carries the order of work, the completeness check, the test for which token a non-build decision carries, and the notes on individual entries |

[The template](assets/project-profile-template.md) is an **asset, not a snippet**: copy the file to
`docs/project-profile.md` and fill it in. Do not retype it, and do not drop rows that look irrelevant
today — an empty `ASK` row is a visible question, while a missing row is a decision nobody knows was
skipped.

The template repeats the token definitions in its own header on purpose. It is copied into a project
repository, where this skill is not present, so the copy has to stand alone. That repetition is the
one sanctioned duplication in this skill set; do not "fix" it by trimming the template.

## The profile is a precondition

**Do not write production code until `docs/project-profile.md` exists and records every decision the
task depends on.** This is a gate, not a preference. Without it each feature silently picks its own
database, service shape, accessor style, or contract direction, and the codebase disagrees with
itself in ways no review catches until much later.

Exceptions to the gate are narrow: documentation, comment, or formatting changes need no profile, and
a task may proceed on a partial profile as long as every decision *that task* touches is recorded.

## Decision tokens

Every skill and template in this set uses exactly these three tokens and no synonym.

| Token | Who settles it | Does it block? |
| --- | --- | --- |
| `ASK` | The user, and only the user | **Yes**, unless the template row records a fallback |
| `RESOLVE` | The agent, by looking it up and recording it with the date | **No.** Resolve, record, and state the choice in the handoff |
| `UNDECIDED` | Deferred on purpose | **No**, unless the current task touches it. Record what will force the decision |

Three rules hold the vocabulary together, and none of them has an exception:

- **Fallbacks live in exactly one place: the `Fallback` column of the template.** No skill may introduce one in its own prose. An `ASK` row with an empty fallback blocks; a row with one is applied, recorded, and reported in the handoff. A fallback belongs to its own row only: never carry one across rows, and never read a neighbouring row's fallback as a precedent for an empty cell. If a rule elsewhere in this set reads like a default for a profile decision, the template is authoritative and that prose is the defect to fix.
- **Never write a version, a coordinate, or any other value from memory into the profile.** When a `RESOLVE` cannot be completed, record `UNDECIDED` with the reason. A remembered version is a guess wearing a specific-looking number, and it is the failure mode this whole mechanism exists to prevent. `build-and-dependencies` carries the same prohibition for build files, where it also owns the choice.
- **Never assume a value or infer one from a test dependency or an example.** An H2 dependency does not make H2 the database. `UNDECIDED` with a note is a legitimate entry; a fabricated value is not.

`build-and-dependencies` owns which build and version decisions carry which token. Do not reclassify
one here, and do not classify a new row from this skill alone — the row's owner decides.

## Rows with no owner skill

Two rows appear in the template with `none yet` in the owner column, and both belong to the
messaging *mechanism*. They exist so the decision stays visible, not so a skill can act on them.
While such a row reads `none` or `UNDECIDED`, do not introduce the technology it names to fill it.
[`_core/README.md`](../_core/README.md) states exactly which parts of that topic are owned and by
whom; read it before concluding a rule is missing.

The caching rows were in this category and are not any more: `application-caching` owns them, and
the guard on them is that skill's rather than this one's — unchanged in substance, and now with a
skill behind it that says what to do once the answer is `yes`.

## Completion checklist

Before finishing any task that touched or needed the profile:

- [ ] Every decision the change depends on is recorded, with no bare token left in a row the task used.
- [ ] Every `RESOLVE` the task needed was looked up and recorded with its date, or recorded `UNDECIDED` with the reason it could not be.
- [ ] Every applied fallback is written into the `Value` column and named in the handoff, so the user overrides once instead of being asked again.
- [ ] No value was inferred from a test dependency, an example, or memory.
- [ ] A change that made a recorded decision obsolete updated that row in the same change.
