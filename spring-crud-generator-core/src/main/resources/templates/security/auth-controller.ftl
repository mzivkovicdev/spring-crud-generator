${imports}

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthController(final AuthenticationManager authenticationManager,
            final JwtTokenProvider jwtTokenProvider,
            final UserDetailsServiceImpl userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody final AuthRequest request) {
        final Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        return ResponseEntity.ok(createAuthResponse(request.getUsername(), userDetails));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody final AuthRefreshRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final String username = jwtTokenProvider.extractUsername(refreshToken);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return ResponseEntity.ok(createAuthResponse(username, userDetails));
    }

    private AuthResponse createAuthResponse(final String username, final UserDetails userDetails) {

        final List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        final String accessToken = jwtTokenProvider.generateToken(username, roles);
        final String refreshToken = jwtTokenProvider.generateRefreshToken(username, roles);
        
        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }
}
