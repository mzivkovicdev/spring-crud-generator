# Findings and reporting

Use this reference to decide what belongs in a review report and to express each result with enough evidence for an engineer to act on it.

## Contents

1. [Classify review observations](#classify-review-observations)
2. [Apply the finding gate](#apply-the-finding-gate)
3. [Assign severity](#assign-severity)
4. [Assign confidence](#assign-confidence)
5. [Write an actionable finding](#write-an-actionable-finding)
6. [Deduplicate and order findings](#deduplicate-and-order-findings)
7. [Track revision and re-review](#track-revision-and-re-review)
8. [Report checks and gaps](#report-checks-and-gaps)
9. [Use the final report structure](#use-the-final-report-structure)

## Classify review observations

Put each observation in exactly one category:

| Category | Meaning | Placement |
| --- | --- | --- |
| Finding | A concrete defect or production risk introduced, exposed, or materially worsened by the change | Findings |
| Question | Missing information prevents a reliable conclusion | Questions and assumptions |
| Suggestion | An optional improvement that is not required for correctness, policy, or safe operation | Suggestions, only when useful |
| Verification gap | A relevant check could not be run or representative evidence is unavailable | Checks and gaps |
| Pre-existing issue | The problem exists independently of the change and is not worsened by it | Omit, unless it blocks evaluation or the user requested an audit |

Do not disguise a preference as a finding. Do not write a question as an accusation. Do not use “consider” or “maybe” for a verified defect; explain the defect directly.

## Apply the finding gate

Before reporting a finding, answer:

1. What changed?
2. Which reachable input, state, concurrency sequence, dependency failure, or deployment sequence triggers the problem?
3. What code, configuration, contract, query, migration, or missing enforcement proves the claim?
4. What user, data, security, performance, cost, or operational impact follows?
5. Which expected behavior, business invariant, owner-skill rule, or established project contract is violated?
6. What is the smallest safe direction for resolving it, when known?
7. Which test or check would fail before the fix and pass after it?

If answers 2, 3, or 4 are missing, classify the observation as a question or gap. A missing answer to 5 can indicate undocumented expected behavior; state the supporting assumption. If only answer 6 is missing, report the finding and state containment, required expertise, or the decision needed instead of inventing a fix.

Use static-analysis and scanner output as a lead, not proof. Confirm reachability, sanitization, framework behavior, configuration, dependency version, and compensating controls.

## Assign severity

Assess severity from impact and realistic likelihood or reachability. Do not use code style, diff size, or reviewer effort as severity.

### Critical

Use only for a likely or directly exploitable change that can cause:

- broad unauthorized access or disclosure of highly sensitive data;
- irreversible or broad data loss or corruption;
- remote code execution, credential compromise, or equivalent control loss;
- a likely widespread production outage without a practical containment path.

Treat Critical findings as merge blockers.

### High

Use for a realistic production path that can cause:

- authorization or tenant-isolation bypass with meaningful scope;
- incorrect business outcome, duplicate irreversible side effect, or material data-integrity failure;
- common request or job failure;
- severe query, resource, retry, or availability regression under expected load;
- incompatible API, event, cache, configuration, or migration behavior during normal rollout.

Treat High findings as merge blockers unless the project has an explicit, owned, time-bounded risk acceptance.

### Medium

Use for a bounded but realistic defect such as:

- an edge or failure-path correctness problem with recoverable impact;
- incomplete validation or resilience that affects a limited scope;
- a test gap on changed high-risk behavior when the code cannot otherwise be verified confidently;
- an operational, maintainability, or observability problem likely to cause incidents or materially slow recovery;
- a performance issue that needs a particular but credible data shape or load level.

State whether a Medium finding blocks this release based on the feature's acceptance criteria and operational context.

### Low

Use for a localized, concrete standards or maintainability defect such as:

- incorrect or stale documentation that can mislead a caller;
- import, formatting, naming, or source-hygiene violations covered by the owner skills;
- avoidable complexity with a clear future defect cost but no current behavioral failure.

Keep Low findings concise. Do not flood a report with every cosmetic occurrence; group mechanically related violations by root cause or file set.

Do not lower a security or correctness severity merely because the triggering path is intentional, internal, or protected by convention. Do lower it when verified controls materially reduce reachability or impact.

## Assign confidence

Keep confidence separate from severity:

- **High confidence:** The changed code and configuration prove the behavior, or a focused test/check reproduces it.
- **Medium confidence:** The code path is clear but one runtime, data-volume, deployment, or external-system fact is inferred.
- **Low confidence:** Essential context is missing. Usually convert this observation to a question or verification gap instead of a finding.

Include confidence explicitly for Medium-confidence findings. Explain the assumption that prevents High confidence.

Do not inflate confidence because a familiar anti-pattern appears. Verify the actual behavior in the configured Java, Spring, provider, and database versions.

## Write an actionable finding

Use this format:

```text
[High] Cross-tenant lookup is not scoped by tenant

Location: src/main/java/com/acme/user/UserService.java — getById
Evidence: The authenticated tenant reaches getById, but the service calls findById(id) and no
          ownership or tenant predicate is enforced before UserDomain is returned.
Scenario: A caller submits an identifier belonging to another tenant.
Impact: The endpoint can disclose another tenant's user data.
Fix direction: Enforce tenant ownership in the service and persistence lookup using the trusted
               tenant from the authenticated context.
Verification: Add an integration/security test proving that the same identifier is accessible to
              its owner and rejected for a different tenant.
Confidence: High
```

Use the example only as a format demonstration. Never infer multi-tenancy or an authorization requirement when the reviewed project does not have one.

Write the title as a consequence, not a label such as “Security issue” or “Bad code”. Put the most decision-relevant fact first.

For `Location`:

- name the narrowest affected file and line or symbol;
- point to the changed line when it creates the issue;
- point to the nearest relevant changed symbol when the failure manifests in unchanged code;
- list a second location only when it is required to show the broken interaction.

For `Evidence`:

- describe what is present or absent in the reviewed path;
- cite relevant annotations, calls, predicates, state transitions, configuration, SQL, or test result;
- avoid copying confidential values, large code blocks, or scanner output.

For `Scenario`:

- state the exact input, ordering, concurrent action, data volume, deployment sequence, or dependency failure;
- avoid vague phrases such as “could potentially fail”.

For `Impact`:

- name the affected user, tenant, data, invariant, latency, capacity, cost, rollout, or support process;
- avoid unsupported worst-case outcomes.

For `Fix direction`:

- describe the smallest safe outcome and boundary that should own it when the remediation is known;
- when it is not known, state safe containment and the decision or specialist input required;
- preserve the TO–Domain–Entity terminology and mapper/service ownership from `spring-boot-patterns`;
- defer persistence and security mechanics to their owner skills;
- avoid writing a full replacement implementation unless the user asked for fixes.

For `Verification`:

- identify a test or check capable of proving the behavior;
- state the important setup and assertion;
- do not merely say “add tests”.

Write comments respectfully and impersonally. Use “the change”, “the method”, or “this path”, not statements about the developer.

## Deduplicate and order findings

Group symptoms under one root cause when one fix resolves them. For example:

- group multiple endpoints leaking the same entity through one shared mapper when that mapper is the root cause;
- separate two endpoints when authorization ownership differs and each needs an independent fix;
- group repeated import-order violations by the affected file set;
- do not repeat a missing regression test as a second finding when the primary defect already includes the required verification.

Order findings by:

1. severity;
2. prerequisite or execution order;
3. source location for otherwise equivalent findings.

Do not count findings or add a score unless the user requests metrics. Counts can reward fragmentation and hide impact.

## Track revision and re-review

Record the reviewed base and head commit, pull-request revision, or working-tree scope. Include a coverage record for multi-file reviews: reviewed files and execution paths, generated or mechanical files, explicit exclusions, and unavailable owner skills.

Before giving a merge or release disposition:

1. Recheck that the reviewed head and working tree have not changed.
2. If they changed, inspect the delta from the last reviewed revision and revisit conclusions invalidated by it.
3. Close a previous finding only after the new code and its verification address the original scenario.
4. Review fixes for new defects, scope expansion, weakened tests, or unrelated changes.
5. State the newest reviewed revision; do not carry approval or “no findings” forward to unseen code.

For a partial review, name what remains unreviewed. Do not use a successful focused review as a decision for the entire change.

## Report checks and gaps

List checks with their actual result:

```text
Checks:
- `./mvnw -q -DskipTests compile`: passed
- `./mvnw -q -Dtest=UserServiceTest test`: passed
- Supported-database integration tests: not run; container runtime unavailable
- Representative query plan: not available; production-like statistics were not accessible
- Full build: not run; the untrusted change could not be executed in an approved isolated environment
```

Do not claim a command passed when it was not run to completion. Distinguish:

- test failure caused by the change;
- pre-existing failure;
- environment or dependency failure;
- check not run because it was unsafe or outside scope.

Keep a code-supported finding when the environment cannot execute a confirming test, but reduce confidence or explain the gap as appropriate. Do not convert every missing tool into a defect in the change.

## Use the final report structure

Use this order:

```text
Findings
1. [Severity] Title
   ...

Questions and assumptions
- ...

Suggestions
- ...  (omit when empty)

Reviewed revision and coverage
- Base/head or working-tree scope
- Reviewed paths and explicit exclusions
- Missing owner skills

Checks and verification gaps
- ...

Summary
- Reviewed scope
- Merge/release blockers, if any
- Disposition, only when requested
- Residual risk
```

Keep the summary short and subordinate to the findings. Do not begin with general praise.

If there are no findings, use:

```text
No actionable findings found in the reviewed scope.

Reviewed revision and coverage
- ...

Checks and verification gaps
- ...

Summary
- Reviewed ...
- Residual risk remains in ...
```

Do not say “LGTM”, “approved”, “safe”, “secure”, or “production-ready” unless the user explicitly asks for that decision, the reviewed revision is still current, and the available evidence supports its limited scope. Even then, state verification gaps and residual risk.

When a merge or release decision is requested, use `Blocker`, `Non-blocker`, or `Needs decision` independently of severity. For an explicitly accepted blocker, record the accountable owner, reason, expiry, compensating controls, and residual risk. Never infer acceptance from silence.
