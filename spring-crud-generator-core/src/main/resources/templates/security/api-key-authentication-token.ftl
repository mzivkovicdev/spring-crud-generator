${imports}

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final List<String> roles;

    public ApiKeyAuthenticationToken(final String apiKey, final List<String> roles) {
        super(buildAuthorities(roles));
        this.apiKey = apiKey;
        this.roles = roles;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.apiKey;
    }

    @Override
    public Object getPrincipal() {
        return this.apiKey;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    private static Collection<GrantedAuthority> buildAuthorities(final List<String> roles) {
        if (roles == null) {
            return java.util.Collections.emptyList();
        }
        return roles.stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }
}
