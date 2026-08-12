# Spring Security for REST APIs

Snippets here follow the worked-example rules in `modern-java-21`: every identifier a snippet uses
is declared in that snippet or attributed to the example that declares it, and an excerpt names any
omitted member that the code depends on.

## Contents

1. [Security model](#security-model)
2. [Authorization](#authorization)
3. [Authentication and password storage](#authentication-and-password-storage)
4. [Tokens](#tokens)
5. [API keys and delegated login](#api-keys-and-delegated-login)
6. [Sessions, cookies, and CSRF](#sessions-cookies-and-csrf)
7. [CORS and headers](#cors-and-headers)
8. [Management endpoints and proxies](#management-endpoints-and-proxies)
9. [Failure handling](#failure-handling)
10. [Verification](#verification)

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

## Authorization

The excerpt below is the resource-server half of the model, which is identical in Profile A and
Profile B. Following the layered layout owned by `spring-boot-patterns`, keep `SecurityConfig` in
the configuration package. Route constants come from wherever the project's authoring direction puts them, so a route cannot be
protected under one spelling and served under another. **The matchers below are written for
code-first**, where the controller owns the constant. Under contract-first replace every
`UserController.USERS_PATH` with the corresponding `ApiPaths` constant; nothing else in the chain
changes. `spring-boot-patterns` owns that split, and the rule is the same either way: a matcher never
contains a repeated path literal.

The authentication endpoints are permitted explicitly. In Profile A they are this service's own
issuance and registration endpoints; in Profile B that block is absent because no such endpoints
exist here. Everything else stays the same.

Actuator endpoints do not appear in this chain at all. They are served on a separate management port
and secured by their own chain, described below.

```java
package com.acme.myapp.config;

import static org.springframework.security.oauth2.core.authorization.OAuth2AuthorizationManagers.hasScope;

import jakarta.servlet.DispatcherType;

import com.acme.myapp.controller.AuthController;
import com.acme.myapp.controller.UserController;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final int API_CHAIN_ORDER = 100;
    private static final int MANAGEMENT_CHAIN_ORDER = 0;
    private static final String MANAGEMENT_AUTHORITY = "SCOPE_management:read";
    private static final String USERS_READ_SCOPE = "users:read";
    private static final String USERS_WRITE_SCOPE = "users:write";

    @Bean
    @Order(API_CHAIN_ORDER)
    SecurityFilterChain apiSecurity(final HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Profile A only: this service's own token issuance and registration.
                        .requestMatchers(HttpMethod.POST, AuthController.TOKEN_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, AuthController.REGISTRATIONS_PATH)
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                UserController.USERS_PATH,
                                UserController.USERS_PATH + "/*")
                        .access(hasScope(USERS_READ_SCOPE))
                        .requestMatchers(HttpMethod.POST, UserController.USERS_PATH)
                        .access(hasScope(USERS_WRITE_SCOPE))
                        .requestMatchers(HttpMethod.PUT, UserController.USERS_PATH + "/*")
                        .access(hasScope(USERS_WRITE_SCOPE))
                        .requestMatchers(HttpMethod.DELETE, UserController.USERS_PATH + "/*")
                        .access(hasScope(USERS_WRITE_SCOPE))
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()));

        return http.build();
    }
}
```

This focused excerpt omits project-specific claim conversion and security error serialization. Use
the defaults only when they satisfy the selected authority model and public error contract.

The collection and one-segment item patterns are intentional; they do not silently authorize nested
resources. For a large API, group routes only when every route in the group has the same policy.
Extract cohesive policy registration or a focused `AuthorizationManager` when the matcher list
becomes difficult to review. Never replace explicit policy with a broad wildcard merely to shorten
the configuration. Keep `anyRequest().denyAll()` as the fallback.

Permit the `ERROR` dispatcher so an already-authorized REST request can complete Spring Boot error
handling. A direct request to an error path is still a normal `REQUEST` dispatch and remains subject
to the route rules and deny-by-default fallback.

CSRF is disabled because this filter chain authenticates exclusively through an explicitly supplied
`Authorization: Bearer` header and no browser-managed credential. Reassess this decision if the
credential model changes.

For an OAuth scope-based policy, define the public scope vocabulary once and use `hasScope(...)` in
authorization rules. With Spring's default JWT conversion, the token scope `users:write` becomes the
internal authority `SCOPE_users:write`; do not create a parallel constant for that derived value.
Roles are a separate model: use `hasRole(...)` only when the selected claim mapping deliberately
produces `ROLE_...` authorities. Configure and test any custom claim mapping explicitly.

Let Spring Security own authentication and access-denied responses. The bearer resource-server
defaults return `401` or `403` as appropriate, and a `401` includes the required bearer
`WWW-Authenticate` challenge. Keep those defaults when they satisfy the public API contract.
Configure focused `AuthenticationEntryPoint` and `AccessDeniedHandler` implementations only when
the contract additionally requires a custom body or stable code. Register them through the filter
chain, preserve protocol-required headers, and do not duplicate this handling in MVC advice.

### Management endpoints

`observability-and-logging` owns what is exposed and in what shape: the endpoint exposure list,
which endpoints are never published at all, and the health detail level. It requires those endpoints
on a separate management port that the public ingress does not route. Network segmentation is not
authorization, so the endpoints still get their own filter chain, ordered ahead of the API chain and
matched by `EndpointRequest` rather than by path strings, so a change to
`management.endpoints.web.base-path` cannot silently unprotect them.

```java
// Same SecurityConfig class as the API chain above; MANAGEMENT_CHAIN_ORDER and
// MANAGEMENT_AUTHORITY are declared there.
@Bean
@Order(MANAGEMENT_CHAIN_ORDER)
SecurityFilterChain managementSecurityFilterChain(final HttpSecurity http) throws Exception {
    return http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(EndpointRequest.to(
                            HealthEndpoint.class, InfoEndpoint.class)).permitAll()
                    .anyRequest().hasAuthority(MANAGEMENT_AUTHORITY))
            .csrf(CsrfConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults())
            .build();
}
```

Rules for this chain:

- Only the platform's probe and build-information endpoints are open. Everything else the project chooses to expose requires an authenticated operator identity.
- This chain leaves the probe endpoints reachable without a credential, so it only holds if the exposure and health-detail settings `observability-and-logging` defines are actually in place. Verify them rather than assuming them.
- Both chains carry an explicit `@Order`. A chain without one falls back to the lowest precedence, so the ordering that makes this work would depend on a default nobody can see in the code. State it.
- The management chain is ordered ahead of the API chain, and the API chain never matches an actuator path. Two chains matching the same request is a misconfiguration, not a defence in depth.
- `MANAGEMENT_AUTHORITY` is written as a scope-derived authority because the resource server maps scopes to `SCOPE_` authorities by default. If the project installs a custom authority converter, change this constant to match it rather than assuming the prefix.
- The credential for this chain is an operator credential managed by the platform, never a customer identity and never a shared static secret in configuration.
- If the deployment cannot provide a separate port, keep the same chain and matcher and rely on ingress rules to block the actuator base path externally. Record that as a compensating control.

### Token validation

Configure issuer and audience explicitly in both profiles. In Profile B the issuer is the external
provider. In Profile A the issuer is this service's own configured issuer identifier, and the
service validates the tokens it signs through the same resource-server path every client uses, so
there is exactly one validation implementation:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OIDC_ISSUER_URI}
          audiences:
            - ${API_AUDIENCE}
          jws-algorithms:
            - ${JWT_JWS_ALGORITHM}
```

If the supported version or identity-provider model requires custom JWT handling, configure an explicit
`JwtDecoder` with equivalent issuer, audience, timestamp validation, and approved signature
algorithms. Do not consider the resource server complete until the full token-validation policy is
configured and tested.

In Profile A, additionally define and configure the issuance side before it is used: the signing key
source and its rotation, the access-token lifetime, refresh-token rotation and reuse detection if
refresh tokens exist, the claim set that carries subject, tenant, and scopes, and the revocation and
logout behavior. Never sign with a hardcoded or committed key, and never publish a verification key
the service does not own. Prefer an established Spring Security authorization-server capability over
a hand-assembled signing path.

- Use the narrowest maintainable matchers and verify matcher ordering.
- Do not rely only on URL rules. Enforce operation, object, field, and tenant authorization in the service and persistence path.
- Derive subject and tenant from the authenticated principal, not from request TO values.
- Prefer scoped lookup. The following declaration is intentionally a repository-method excerpt; the containing repository and imports are omitted:

```java
Optional<DocumentEntity> findByIdAndTenantId(
        final Long documentId,
        final Long tenantId);
```

- Test horizontal access, vertical access, guessed IDs, bulk endpoints, exports, nested resources, and administrative actions.
- Avoid role-name scattering. Use stable application authorities or authorization policies and map identity-provider claims deliberately.
- Treat method security as an additional layer, not a replacement for coherent service and repository boundaries.

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
