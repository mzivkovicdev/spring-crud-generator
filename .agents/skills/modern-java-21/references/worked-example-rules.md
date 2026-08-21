# Worked example rules

Use this reference when writing or adapting any code, configuration, or build snippet that appears in
a skill in this set, and when deciding how much of an example to reproduce in generated output. Every
reference file in this skill set is written against these rules and says so.

Every code, configuration, and build snippet in this skill set is a **pattern to adapt, not a file to
copy**. An agent asked for a product service writes `ProductService` from scratch; it does not rename
`UserService` and keep the rest.

**Self-containment.** A snippet declares every identifier it uses, or names where it comes from:

- Every constant referenced in a snippet is declared in that same snippet, unless the snippet states which example or type declares it.
- Every build property referenced as `${...}` is declared in the same file, or the file says where it is declared.
- Every type referenced across skills is named with the reference that defines it, so the reader can find it.
- Omit imports, and omit members that are irrelevant to the decision being shown — but never omit something the snippet itself refers to.

**Excerpts.** An excerpt shows one decision, not a complete type. Generate the members it omits
rather than copying it verbatim. When an omitted member is required for the code to work at all — an
accessible constructor for a mapper, a bean registration for a filter — the example says so.

**Verification.** Check a snippet against the versions the project profile records. When part of it
cannot be verified, say which part rather than presenting it with equal confidence.

## Where a rule is stated

These apply to the skill files themselves, not to the code they describe.

**One statement per rule, inside a skill.** A reference is reached only through its own `SKILL.md`,
so whoever reads the reference has already read the skill. A reference therefore never restates a
rule from its own `SKILL.md`; it carries the detail, the criteria, and the examples behind it.

**A summary in `SKILL.md` is allowed only when it is the whole rule.** A one-line rule that stands
on its own belongs there. A rule that needs a criterion, a condition, or an exception is either
written completely in `SKILL.md` or left entirely to the reference — never split, because the
shorter half is the one that gets applied and the two halves drift.

**Parallel references are the exception.** Where a project uses one of two tools, each tool's
reference states its own rules in full. Only one is ever read, so the repetition between them costs
nothing and removing it would leave a gap.

**No rule from another skill, in any form.** Attribution is allowed and encouraged: name the owner
and what it decides. Restating what it decided is not.
