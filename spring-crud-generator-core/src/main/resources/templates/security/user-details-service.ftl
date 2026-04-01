${imports}

/**
 * UserDetailsService implementation.
 * Replace the stub logic below with your actual user repository lookup.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        // TODO: Replace with actual user lookup from your repository
        if ("admin".equals(username)) {
            final List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            return new User(username, "{noop}admin", authorities);
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
