# Spring Security for REST APIs

## Contents

1. [Security model](#security-model)
2. [Authorization](#authorization)
3. [Authentication and password storage](#authentication-and-password-storage)
4. [Tokens](#tokens)
5. [API keys, delegated login, and webhooks](#api-keys-delegated-login-and-webhooks)
6. [Sessions, cookies, and CSRF](#sessions-cookies-and-csrf)
7. [CORS and headers](#cors-and-headers)
8. [Management endpoints and proxies](#management-endpoints-and-proxies)
9. [Failure handling](#failure-handling)
10. [Verification](#verification)

## Security model

Inspect the actual Spring Boot and Spring Security versions and the complete authentication flow before editing configuration. Do not paste a universal `SecurityFilterChain` from an example.

Document:

- which endpoints are public;
- credential type and where the client stores and transmits it;
- identity provider, issuers, audiences, and trust anchors;
- roles, permissions, tenant rules, resource ownership, and administrative operations;
- session or token expiry, revocation, logout, rotation, and replay handling;
- browser, mobile, service-to-service, webhook, and scheduled-job clients;
- reverse proxy, gateway, service mesh, and management-port behavior.

Keep framework defaults unless a verified requirement justifies a change. A comment saying “REST” or “stateless” is not evidence.

## Authorization

Configure HTTP rules with public endpoints listed explicitly and a protected fallback:

```java
@Bean
SecurityFilterChain apiSecurity(final HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
            .requestMatchers("/api/admin/**").hasAuthority("user:admin")
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults()));

    return http.build();
}
```

This example intentionally makes no CSRF or session decision. Add those controls only after evaluating the credential model.

- Use exact matchers and verify matcher ordering.
- Do not rely only on URL rules. Enforce operation, object, field, and tenant authorization in the service and persistence path.
- Derive subject and tenant from the authenticated principal, not from request TO values.
- Prefer scoped lookup:

```java
Optional<DocumentEntity> findByIdAndTenantId(
        final Long documentId,
        final Long tenantId);
```

- Test horizontal access, vertical access, guessed IDs, bulk endpoints, exports, nested resources, and administrative actions.
- Avoid role-name scattering. Use stable application authorities or authorization policies and map identity-provider claims deliberately.
- Treat method security as an additional layer, not a replacement for coherent service and repository boundaries.

## Authentication and password storage

Prefer OIDC/OAuth2 or another approved identity provider. Do not invent authentication, password recovery, MFA, or token protocols.

When the application stores passwords:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

- Use an adaptive one-way function supported by Spring Security.
- Benchmark the work factor on representative production hardware and review it periodically.
- Store the encoded representation only.
- Support algorithm migration rather than assuming one fixed algorithm and cost forever.
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

Do not log complete tokens or expose them in URLs, query parameters, error bodies, metrics, traces, or browser storage without an explicit threat-model decision.

## API keys, delegated login, and webhooks

- Put API keys in an authorization header, never in a URL. Store only a protected verifier when the original key does not need to be recovered.
- Give every key an owner, purpose, narrow scope, environment, creation time, expiry or review date, rotation path, revocation path, and last-used audit signal.
- Do not use one shared key across users, tenants, environments, or unrelated integrations.
- For OAuth2/OIDC authorization flows, use exact registered redirect URIs, authorization code flow with PKCE where applicable, and validated `state` and `nonce` values.
- Never accept an identity or authorization decision merely because a callback contains an email address or another user attribute.
- Verify webhook signatures over the exact raw bytes required by the provider protocol before parsing or mutating state.
- Validate signature algorithm, key identity, timestamp and allowed skew; use constant-time comparison where the protocol requires a shared-secret digest.
- Prevent replay with a provider event ID or nonce, bounded retention, and idempotent processing.
- Authenticate webhook configuration and rotation separately from webhook delivery. Return minimal errors that do not provide a signature oracle.

## Sessions, cookies, and CSRF

Determine CSRF protection from how credentials are attached:

| Client and credential behavior | CSRF posture |
|---|---|
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

## Management endpoints and proxies

- Expose only required Actuator endpoints.
- Keep health details minimal for unauthenticated callers.
- Protect management access with a separate network path, port, authorization policy, or equivalent control when appropriate.
- Never expose environment, heap dump, thread dump, log file, configuration properties, mappings, or metrics containing sensitive labels to untrusted callers.
- Restrict shutdown and other state-changing management operations.
- Configure which proxy addresses are trusted to supply forwarded headers.
- Derive client scheme, host, and address only from trusted proxy processing; otherwise attacker-supplied forwarding headers can affect redirects, audit data, rate limits, and secure-cookie behavior.

## Failure handling

- Use stable `ProblemDetail` responses consistent with `spring-boot-patterns`.
- Return `401` for missing or invalid authentication and `403` for authenticated callers lacking permission, unless the API intentionally conceals resource existence.
- Do not expose provider messages, claim-validation details, stack traces, internal authorities, or account existence.
- Log the security event once with a correlation ID and minimized subject/resource identifiers.
- Fail closed when authorization data, key material, identity providers, or policy dependencies are unavailable unless an explicitly reviewed availability design says otherwise.

## Verification

Add tests for:

- public endpoint access and protected fallback;
- missing, malformed, expired, wrong-issuer, wrong-audience, and insufficient-scope tokens;
- revoked, expired, wrongly scoped, and cross-environment API keys;
- invalid, stale, replayed, and duplicate webhooks;
- OAuth2/OIDC state, nonce, PKCE, and redirect-URI failures where delegated login exists;
- permitted and forbidden roles or authorities;
- cross-user and cross-tenant access;
- CSRF presence or absence according to the credential model;
- allowed and rejected CORS origins and preflight requests;
- secure cookie properties and session rotation where applicable;
- management endpoint isolation;
- safe `401`, `403`, and hidden-resource behavior;
- rate limits and lockout/recovery behavior at the appropriate integration boundary.

Use Spring Security test support for filter behavior and integration tests for the deployed authentication path. Do not mock away the control being tested.

## References

- [Spring Security reference](https://docs.spring.io/spring-security/reference/)
- [Spring Security CSRF reference](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [OWASP REST Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)
- [OWASP HTTP Headers Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html)
