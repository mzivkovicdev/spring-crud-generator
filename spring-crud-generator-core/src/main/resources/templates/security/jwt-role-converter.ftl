${converterImports}

/**
 * Converts JWT claims to Spring Security GrantedAuthority objects.
 * Supports nested claims (e.g. "realm_access.roles" from Keycloak).
 */
@org.springframework.stereotype.Component
public class JwtRoleConverter implements Converter<Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> {

    @Value("${r"${"}security.oauth2.roles-claim:${rolesClaim}${r"}"}")
    private String rolesClaim;

    @Override
    public org.springframework.security.authentication.AbstractAuthenticationToken convert(final Jwt jwt) {
        final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        final Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);

        final Collection<GrantedAuthority> roleAuthorities = extractRoles(jwt).stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .collect(java.util.stream.Collectors.toList());

        final List<GrantedAuthority> allAuthorities = new java.util.ArrayList<>();
        if (defaultAuthorities != null) {
            allAuthorities.addAll(defaultAuthorities);
        }
        allAuthorities.addAll(roleAuthorities);

        return new JwtAuthenticationConverter().convert(jwt);
    }

    private List<String> extractRoles(final Jwt jwt) {
        final String[] parts = rolesClaim.split("\\.");
        Object current = jwt.getClaims();
        for (final String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return java.util.Collections.emptyList();
            }
        }
        if (current instanceof List) {
            return ((List<?>) current).stream().map(Object::toString).collect(java.util.stream.Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }
}
