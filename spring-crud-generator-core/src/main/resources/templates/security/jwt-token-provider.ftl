${jwtImports}

@Component
public class JwtTokenProvider {

    @Value("${r"${"}jwt.secret:defaultSecretKeyForDevelopmentPurposesOnly12345${r"}"}")
    private String jwtSecret;

    @Value("${r"${"}jwt.expiration-ms:86400000${r"}"}")
    private long jwtExpirationMs;

    @Value("${r"${"}jwt.issuer:spring-crud-app${r"}"}")
    private String jwtIssuer;

    public String generateToken(final String username, final List<String> roles) {
        final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .issuer(jwtIssuer)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(key)
            .compact();
    }

    public String extractUsername(final String token) {
        return extractClaims(token).getSubject();
    }

    public List<String> extractRoles(final String token) {
        final Claims claims = extractClaims(token);
        final Object roles = claims.get("roles");
        if (roles instanceof java.util.List<?>) {
            return ((java.util.List<?>) roles).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }

    public boolean validateToken(final String token) {
        try {
            final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    public List<GrantedAuthority> extractAuthorities(final String token) {
        return extractRoles(token).stream()
            .map(role -> (GrantedAuthority) () -> "ROLE_" + role)
            .collect(Collectors.toList());
    }

    private Claims extractClaims(final String token) {
        final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
