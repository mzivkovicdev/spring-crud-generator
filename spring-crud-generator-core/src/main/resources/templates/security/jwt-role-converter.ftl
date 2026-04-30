${imports}

/**
 * Converts JWT claims to Spring Security GrantedAuthority objects.
 * Supports nested claims (e.g. "realm_access.roles" from Keycloak).
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Value("${r"${"}security.oauth2.roles-claim:${rolesClaim}${r"}"}")
    private String rolesClaim;

    @Override
    public AbstractAuthenticationToken convert(final Jwt jwt) {
        final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        final Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();

        final Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);
        if (defaultAuthorities != null) {
            authorities.addAll(defaultAuthorities);
        }

        extractRoles(jwt).stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .forEach(authorities::add);

        return new JwtAuthenticationToken(jwt, authorities);
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
