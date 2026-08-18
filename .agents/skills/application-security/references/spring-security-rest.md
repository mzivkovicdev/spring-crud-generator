# Spring Security for REST APIs

Snippets here follow the worked-example rules in `modern-java-21`: every identifier a snippet uses
is declared in that snippet or attributed to the example that declares it, and an excerpt names any
omitted member that the code depends on.

## Contents

1. [Security model](#security-model)
2. [Authentication and password storage](#authentication-and-password-storage)
3. [Tokens](#tokens)
4. [API keys and delegated login](#api-keys-and-delegated-login)
5. [Sessions, cookies, and CSRF](#sessions-cookies-and-csrf)
6. [CORS and headers](#cors-and-headers)
7. [Trusted proxies](#trusted-proxies)
8. [Failure handling](#failure-handling)
9. [Verification](#verification)

## Security model

Inspect the actual Spring Boot and Spring Security versions and the complete authentication flow
before editing configuration. Do not paste a universal `SecurityFilterChain` from an example.

### One generalized model, two issuance profiles

Every protected API in this project is a **stateless bearer resource server**. Clients send
`Authorization: Bearer <token>`, the filter chain validates the token against a configured issuer,
and authorities are derived from validated claims. That half of the model is fixed, so route rules,
authorization code, error contract, and tests are written the same way regardless of who issued the
token.

Only token issuance varies. Exactly one profile is selected per deployable service and recorded in
`docs/project-profile.md`:

| Profile | Who issues the token | Typical use |
| --- | --- | --- |
| **A — application-issued** | This service owns identity: registration, login, credential storage, recovery, and token issuance through its own authentication endpoints | The service is the identity provider for its own clients |
| **B — externally issued** | A separate identity provider issues the token; this service only validates it | A shared IdP, an internal authorization server, or a partner OIDC provider |

An identity provider exists in both profiles. In Profile A this service *is* it. Profile B is not
an alternative security model, only a different issuer and trust anchor.

Consequences that hold in both profiles:

- The resource-server configuration, route matchers, `denyAll` fallback, and `401`/`403` contract are identical. Write them once.
- Integration tests obtain a real token through the selected issuance path and send it in the `Authorization` header. There is no profile in which tokens are mocked. `spring-boot-testing` owns how that token is acquired per profile.
- Authorities come from validated claims, never from a request field.

When the profile is not recorded and cannot be inferred from the repository, **ask the user which
profile applies and record the answer before implementing authentication**. Do not add a local
identity store or token endpoint merely to make an example work, and do not create an ad hoc
authentication protocol in either profile.

### Separate self-registration from administrative user creation

These are different operations with different policies, and conflating them produces a contract
that cannot be satisfied:

- **Self-registration**, when the product has it, is a public endpoint in the authentication boundary, such as `POST /api/v1/auth/registrations`. It creates an identity for the caller, applies anti-abuse controls, and never accepts privileged fields such as roles, scopes, tenant, or status.
- **Administrative user creation**, `POST /api/v1/users`, is a protected operation that requires an existing privileged token. It may set fields self-registration must not.

If the product has no self-registration, the first identity is provisioned out of band — a seeded
administrator, a migration, or an operator tool — not by relaxing the protected endpoint. Tests seed
the identity the same way and then obtain a token through the real issuance path, so the protected
endpoint stays protected.

Document:

- which endpoints are public;
- credential type and where the client stores and transmits it;
- credential and token owner, identity provider when applicable, issuers, audiences, and trust
  anchors;
- roles, permissions, tenant rules, resource ownership, and administrative operations;
- session or token expiry, revocation, logout, rotation, and replay handling;
- browser, mobile, service-to-service, webhook, and scheduled-job clients;
- reverse proxy, gateway, service mesh, and management-port behavior.

Keep framework defaults unless a verified requirement justifies a change. A comment saying “REST” or “stateless” is not evidence.

## Authentication and password storage

This section applies to Profile A, where this service owns the credential lifecycle. Under Profile B
the provider owns it and this service stores no passwords at all; do not add a local credential
store "just in case". In either profile, use reviewed Spring Security and protocol capabilities
rather than custom cryptography or an ad hoc authentication protocol.

For an identity-owning service, adopt an approved identity and authorization-server design before
implementation. Define supported clients and grants, credential lifecycle, issuer and audience,
signing-key storage and rotation, access- and refresh-token lifecycle, recovery and MFA, abuse
controls, and audit. Do not infer token issuance from the resource-server excerpt or hand-roll the
protocol.

- Use an adaptive one-way function supported by Spring Security.
- Benchmark the work factor on representative production hardware and review it periodically.
- Store the encoded representation only.
- Configure a `DelegatingPasswordEncoder` or equivalent migration-capable strategy with a project-approved default encoder and benchmarked parameters. Retain only the legacy encoders needed to verify and migrate existing hashes; do not assume a factory default satisfies every deployment.
- Protect registration, login, password reset, verification, recovery, and MFA endpoints against enumeration, brute force, replay, and abuse.
- Use single-use, high-entropy, short-lived recovery values and invalidate them after use or credential changes.
- Require current credentials or stronger verification for sensitive account changes when appropriate.

## Tokens

For JWT access tokens, verify:

- a trusted signature and explicit allowed algorithms;
- issuer and audience;
- expiration and not-before time with a small justified clock skew;
- token type or intended use;
- required subject, tenant, scope, and authorization claims;
- key rotation and failure behavior.

Reject algorithm confusion, unsigned tokens, unexpected key sources, tokens for another API, and attacker-controlled key identifiers or URLs.

JWT payloads are encoded and usually signed, not encrypted. Do not include secrets, excessive personal data, or internal state. Keep access tokens short-lived. Define refresh-token rotation, reuse detection, revocation, storage, and logout where refresh tokens exist.

Choose JWT or opaque access tokens from the actual requirements:

- use locally verified JWTs when offline validation, latency, and authorization-server availability justify the trade-off;
- consider opaque-token introspection when central revocation and current authorization state are more important;
- define introspection authentication, timeouts, caching, outage behavior, and data minimization;
- for multiple issuers, allowlist issuers and bind issuer resolution to the expected tenant and audience before trusting tenant or authorization claims;
- never resolve a decoder, key source, or introspection endpoint from an arbitrary unverified token claim.

Do not log complete tokens or expose them in URLs, query parameters, error bodies, metrics, traces, or browser storage without an explicit threat-model decision.

## API keys and delegated login

- Generate API keys with a cryptographically secure random generator and sufficient entropy. Show the secret only when issued; store a non-reversible verifier plus a non-secret lookup identifier or prefix when recovery is unnecessary.
- Put API keys in an authorization header, never in a URL. Compare verifiers in constant time where the chosen construction requires it.
- Give every key an owner, purpose, narrow scope, environment, creation time, expiry or review date, rotation path, revocation path, and last-used audit signal.
- Do not use one shared key across users, tenants, environments, or unrelated integrations.
- For OAuth2/OIDC authorization flows, use exact registered redirect URIs, authorization code flow with PKCE where applicable, and validated `state` and `nonce` values.
- Never accept an identity or authorization decision merely because a callback contains an email address or another user attribute.

## Sessions, cookies, and CSRF

Determine CSRF protection from how credentials are attached:

| Client and credential behavior | CSRF posture |
| --- | --- |
| Browser automatically sends session cookie, Basic credentials, client certificate, or authentication cookie | Keep CSRF protection and integrate the client with the token mechanism |
| Browser stores a bearer value in a cookie | Treat it as cookie authentication; keep CSRF protection |
| Non-browser client explicitly sets an `Authorization` bearer header and no ambient browser credential authenticates the request | CSRF may be disabled after documenting and testing this assumption |
| Mixed browser and service clients | Separate filter chains or preserve protection for affected endpoints |

Do not configure CSRF twice in the same chain or disable it and then configure a token repository.

For cookie-backed authentication:

- set `Secure` and `HttpOnly`;
- choose `SameSite` from the actual cross-site flow;
- narrow domain and path;
- rotate the session identifier after authentication and privilege changes;
- implement idle and absolute timeout, logout, invalidation, and concurrent-session policy as required.

Never use CORS as a CSRF defense.

## CORS and headers

- Allow only required origins, methods, and headers.
- Never combine credentialed requests with a wildcard origin.
- Keep development origins out of production.
- Verify preflight behavior and rejection of unapproved origins.
- Remember that non-browser clients are not constrained by CORS.

Use current Spring Security defaults and current OWASP guidance for response headers. Configure:

- HSTS only when HTTPS is consistently enforced for the host;
- `X-Content-Type-Options: nosniff`;
- framing controls through CSP `frame-ancestors` or `X-Frame-Options` where relevant;
- a CSP based on actual browser-rendered content, not a copied policy;
- `Referrer-Policy` and `Permissions-Policy` where browser behavior requires them.

Do not add the obsolete `X-XSS-Protection: 1; mode=block` header. Do not assume security headers fix unsafe output handling.

## Trusted proxies

`observability-and-logging` owns actuator exposure and health detail level; the management filter
chain earlier in this reference owns access to it. Neither is restated here.

- Configure which proxy addresses are trusted to supply forwarded headers.
- Derive client scheme, host, and address only from trusted proxy processing; otherwise attacker-supplied forwarding headers can affect redirects, audit data, rate limits, and secure-cookie behavior.

## Failure handling

- Use stable `ProblemDetail` responses consistent with `spring-boot-patterns`.
- Return `401` for missing or invalid authentication and `403` for authenticated callers lacking permission, unless the API intentionally conceals resource existence.
- Include a `WWW-Authenticate` challenge for the selected authentication scheme in every `401` response.
- Do not expose provider messages, claim-validation details, stack traces, internal authorities, or account existence.
- Log the security event once with a correlation ID and minimized subject/resource identifiers.
- Fail closed when authorization data, key material, identity providers, or policy dependencies are unavailable unless an explicitly reviewed availability design says otherwise.

## Verification

Add tests for:

- public endpoint access and deny-by-default fallback;
- missing, malformed, expired, wrong-issuer, wrong-audience, and insufficient-authority or
  insufficient-scope tokens, as applicable;
- wrong-tenant, unapproved-issuer, unavailable-introspection, and key-rotation behavior where applicable;
- revoked, expired, wrongly scoped, and cross-environment API keys;
- OAuth2/OIDC state, nonce, PKCE, and redirect-URI failures where delegated login exists;
- permitted and forbidden roles or authorities;
- cross-user and cross-tenant access;
- CSRF presence or absence according to the credential model;
- allowed and rejected CORS origins and preflight requests;
- secure cookie properties and session rotation where applicable;
- management endpoint isolation;
- safe `401`, `403`, and hidden-resource behavior;
- application failures that must not be replaced by `401` or `403` during an error dispatch;
- rate limits and lockout/recovery behavior at the appropriate integration boundary.

Use `spring-boot-testing` as the owner of test levels, credential acquisition, invalid-token
fixtures, and prohibited substitutes. Runtime security evidence must traverse the real configured
filter chain and selected authentication flow.

## References

- [Spring Security reference](https://docs.spring.io/spring-security/reference/)
- [Spring Security CSRF reference](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [OWASP REST Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)
- [OWASP HTTP Headers Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html)
