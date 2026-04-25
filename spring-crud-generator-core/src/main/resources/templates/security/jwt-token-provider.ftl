${imports}

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    @Value("${r"${"}jwt.secret:defaultSecretKeyForDevelopmentPurposesOnly12345${r"}"}")
    private String jwtSecret;

    @Value("${r"${"}jwt.expiration-ms:86400000${r"}"}")
    private long jwtExpirationMs;

    @Value("${r"${"}jwt.refresh-expiration-ms:604800000${r"}"}")
    private long jwtRefreshExpirationMs;

    @Value("${r"${"}jwt.issuer:spring-crud-app${r"}"}")
    private String jwtIssuer;

    public String generateToken(final String username, final List<String> roles) {
        return generateToken(username, roles, ACCESS_TOKEN_TYPE, jwtExpirationMs);
    }

    public String generateRefreshToken(final String username, final List<String> roles) {
        return generateToken(username, roles, REFRESH_TOKEN_TYPE, jwtRefreshExpirationMs);
    }

    private String generateToken(final String username, final List<String> roles,
            final String tokenType, final long expirationInMs) {
        final List<String> safeRoles = roles == null ? Collections.emptyList() : roles;
        final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
            .subject(username)
            .claim("roles", safeRoles)
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .issuer(jwtIssuer)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationInMs))
            .signWith(key)
            .compact();
    }

    public String extractUsername(final String token) {
        return extractClaims(token).getSubject();
    }

    public List<String> extractRoles(final String token) {
        final Claims claims = extractClaims(token);
        final Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
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

    public boolean validateAccessToken(final String token) {
        return validateToken(token) && ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean validateRefreshToken(final String token) {
        return validateToken(token) && REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public List<GrantedAuthority> extractAuthorities(final String token) {
        return extractRoles(token).stream()
            .map(role -> (GrantedAuthority) () -> "ROLE_" + role)
            .collect(Collectors.toList());
    }

    private String extractTokenType(final String token) {
        try {
            final String tokenType = extractClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
            return tokenType == null ? ACCESS_TOKEN_TYPE : tokenType;
        } catch (final Exception e) {
            return "";
        }
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
