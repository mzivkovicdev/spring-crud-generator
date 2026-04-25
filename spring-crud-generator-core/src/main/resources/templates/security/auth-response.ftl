public class AuthResponse {

    private String token;
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

    public String getToken() {
        return this.token;
    }

    public AuthResponse setToken(final String token) {
        this.token = token;
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
