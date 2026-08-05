# Supply Chain and Security Testing

## Contents

1. [Security requirements and threat analysis](#security-requirements-and-threat-analysis)
2. [Dependencies and build integrity](#dependencies-and-build-integrity)
3. [CI/CD and deployment](#cicd-and-deployment)
4. [Security test strategy](#security-test-strategy)
5. [Vulnerability handling](#vulnerability-handling)
6. [Release readiness](#release-readiness)

## Security requirements and threat analysis

Use the project-pinned OWASP ASVS version and applicable requirement set as the verification baseline. Record versioned identifiers such as `v5.0.0-1.2.5`; do not silently change the baseline during a feature. Use the current OWASP Top 10 and API Security Top 10 to prompt risk discussion, not as proof that the application is secure.

For a new or materially changed feature, record:

- protected assets and data classification;
- actors, tenants, roles, and service identities;
- trust boundaries, entry points, data flows, and privileged operations;
- external providers and failure modes;
- abuse cases and expected security behavior;
- selected controls, test evidence, assumptions, and residual risk.
- applicable ASVS identifiers and approved exceptions with owner and expiry.

Revisit the analysis when authentication, authorization, data sensitivity, tenancy, deployment topology, external integrations, or attacker capability changes.

Security requirements must be testable. Replace “secure endpoint” with concrete outcomes such as “a caller from tenant A cannot learn whether tenant B owns resource X.”

## Dependencies and build integrity

- Prefer the supported Spring Boot dependency-management baseline or an approved BOM.
- Use only approved artifact and plugin repositories over authenticated TLS.
- Pin build plugins and direct dependencies according to the project's reproducibility policy.
- Use a trusted committed Maven or Gradle wrapper where the project standard requires it; review wrapper, distribution URL, checksum, and build-image changes.
- Do not run a blanket “upgrade everything to latest.” Review release notes, compatibility, migrations, transitive changes, provenance, known vulnerabilities, and tests.
- Remove unused dependencies and plugins to reduce attack surface.
- Review dependencies that execute code during build, annotation processing, tests, deserialization, parsing, templating, file conversion, or startup.
- Generate and retain an SBOM for production releases when supported by project policy and delivery tooling; document any approved alternative.
- Verify artifact provenance, signatures, checksums, attestations, or repository controls supported by the delivery platform.
- Keep dependency resolution reproducible. Detect unexpected repository, lockfile, checksum, and dependency-tree changes.
- Triage vulnerabilities by reachable behavior, exposure, exploit prerequisites, compensating controls, affected versions, and vendor guidance. A CVSS number alone is not the decision.

Do not hardcode a scanner or plugin version into this skill. Use a project-supported maintained version and review it periodically.

## CI/CD and deployment

Use checks relevant to the artifact:

- secret scanning before merge and on history where authorized;
- static analysis for source and configuration;
- software composition analysis;
- full application integration tests for runtime security controls;
- container image and base-image scanning;
- infrastructure-as-code and cloud-policy checks;
- API contract and dynamic security tests in an authorized environment;
- artifact signing, provenance, and deployment-policy verification.

All tools must satisfy the confidentiality rules. Do not upload source, artifacts, SBOMs, dependency graphs, endpoint inventories, or findings to an unapproved hosted service.

Protect the pipeline:

- grant jobs minimal repository, environment, network, cloud, and secret access;
- separate untrusted pull-request execution from protected credentials;
- pin or verify third-party CI actions and build images;
- prevent untrusted build output from becoming a trusted command;
- require review for workflow, deployment, policy, dependency-source, and permission changes;
- keep secrets out of command lines and logs;
- promote the same verified artifact between environments rather than rebuilding from mutable inputs.

Protect runtime deployment:

- use a maintained minimal base image and non-root process where feasible;
- keep filesystem, Linux capabilities, service accounts, IAM roles, network access, and database privileges minimal;
- make containers and hosts immutable where practical;
- expose only required ports and management endpoints;
- validate production configuration and fail startup when mandatory security properties are absent;
- preserve rollback, audit, monitoring, and credential-revocation capability.

## Security test strategy

Apply `spring-boot-testing` for test structure, fixtures, isolation, and execution. This reference
defines the security controls and scenarios that those tests must prove.

Match the test to the control:

| Control | Minimum useful evidence |
| --- | --- |
| HTTP authentication and route rules | Full application integration test through the real filter chain and selected authentication flow |
| Object and tenant authorization | Service plus database-backed integration test with two users or tenants |
| Input constraints | Boundary tests for valid, invalid, oversized, and malformed values |
| Query injection resistance | Repository integration test plus review of construction and generated SQL |
| CSRF and CORS | Full application integration test using the real credential model |
| Token validation | Tests for signature, algorithm, issuer, audience, time, type, and required claims |
| Error confidentiality | HTTP test asserting the public body and protected telemetry behavior |
| Logging confidentiality | Captured-log test proving sensitive values are absent |
| WebClient and SSRF | Mock remote service, destination-policy tests, redirects, timeouts, and response limits |
| File processing | Type, traversal, authorization, archive-bomb, cleanup, and size-limit tests |
| Redis isolation | Key, tenant, TTL, serialization, invalidation, and sensitive-value tests |
| API inventory and lifecycle | Contract/deployment comparison plus tests proving obsolete or debug routes are unavailable |
| Business-flow abuse | Actor-, tenant-, operation-, batch-, quota-, and cost-limit tests |
| Messaging and jobs | Duplicate, replay, stale, poison, DLQ, redrive, concurrency, and authorization tests |
| Cloud permissions | Deployment-policy tests and evidence for workload identity, resource policy, network, key, and public-access controls |
| Vulnerability remediation | A failing-before, passing-after regression test at the exploitable boundary |

Include happy paths, negative cases, boundary values, and dependency failures. Do not test only annotations or mocks when the real security behavior depends on filters, proxies, serializers, database constraints, network configuration, or cloud policies.

Use production-like versions and configuration in integration tests. Never use production credentials or unapproved production data.

Scans produce leads. Review results for reachability and evidence, and do not suppress findings without a reason, owner, scope, and expiry.

## Vulnerability handling

Describe a finding with:

1. title and affected component;
2. severity and rationale;
3. confirmed evidence or clearly labeled hypothesis;
4. preconditions and realistic exploit path;
5. confidentiality, integrity, availability, tenant, and operational impact;
6. affected versions and environments;
7. immediate containment and long-term remediation;
8. regression test and verification method;
9. owner, target date, and residual risk.

Do not include live credentials, complete customer records, or more exploit detail than the authorized audience needs.

For dependency findings:

- confirm the resolved version and dependency path;
- determine whether the vulnerable code is present and reachable;
- check authoritative vendor or project advisories;
- test the upgrade or mitigation;
- document temporary compensating controls and expiry;
- do not silently dismiss a transitive vulnerability because it is not called directly.

For a suspected secret:

- do not print or copy its value;
- identify type and location;
- rotate or revoke first according to incident policy;
- remove it from active files and history through the approved process;
- inspect access and add a prevention test or scanner rule.

## Release readiness

Before a production release:

- [ ] Security requirements and abuse cases for changed behavior are covered.
- [ ] Applicable versioned ASVS requirements have verification evidence or approved, expiring exceptions.
- [ ] Confidential data and tools remained within approved boundaries.
- [ ] Authentication, authorization, tenant isolation, validation, errors, logging, and resource limits are verified.
- [ ] Dependencies, plugins, repositories, artifact provenance, and SBOM meet project policy.
- [ ] Required secret, source, dependency, container, and infrastructure checks pass or have approved, time-bound exceptions.
- [ ] Production configuration, TLS, management endpoints, cloud roles, database access, Redis access, and network policy are reviewed.
- [ ] No sample credentials, debug features, test endpoints, verbose errors, or unsafe fallbacks remain.
- [ ] Monitoring, alerting, audit retention, incident ownership, rollback, and credential revocation are ready.
- [ ] Every accepted risk has an owner, rationale, expiry, and remediation plan.

## References

- [OWASP Application Security Verification Standard](https://owasp.org/www-project-application-security-verification-standard/)
- [OWASP Top 10: 2025](https://owasp.org/Top10/2025/)
- [NIST Secure Software Development Framework](https://csrc.nist.gov/pubs/sp/800/218/final)
- [OWASP Software Component Verification Standard](https://owasp.org/www-project-software-component-verification-standard/)
