# Spring Security authorization

Authorization rules and filter-chain construction for the security model in
[Spring Security for REST](spring-security-rest.md). Read that file first: it establishes the
stateless bearer resource-server model, the issuance profiles, and the token rules these chains
assume.

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
