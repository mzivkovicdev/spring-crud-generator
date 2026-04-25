public class AuthResponse {

    private String token;
    private String refreshToken;
    private String tokenType;

    public AuthResponse() {}

    public AuthResponse(final String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }

    public AuthResponse(final String token, final String tokenType) {
        this.token = token;
        this.tokenType = tokenType;
    }

    public AuthResponse(final String token, final String refreshToken, final String tokenType) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
    }

    public String getToken() {
        return this.token;
    }

    public AuthResponse setToken(final String token) {
        this.token = token;
        return this;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public AuthResponse setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public AuthResponse setTokenType(final String tokenType) {
        this.tokenType = tokenType;
        return this;
    }
}
