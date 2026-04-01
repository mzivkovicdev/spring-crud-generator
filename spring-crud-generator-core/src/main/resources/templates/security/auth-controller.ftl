${imports}

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody final AuthRequest request) {
        final Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final org.springframework.security.core.userdetails.UserDetails userDetails =
            userDetailsService.loadUserByUsername(request.getUsername());
        final java.util.List<String> roles = userDetails.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .collect(java.util.stream.Collectors.toList());
        final String token = jwtTokenProvider.generateToken(request.getUsername(), roles);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
