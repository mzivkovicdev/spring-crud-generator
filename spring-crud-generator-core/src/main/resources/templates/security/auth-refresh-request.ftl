public class AuthRefreshRequest {

    private String refreshToken;

    public AuthRefreshRequest() {}

    public AuthRefreshRequest(final String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public AuthRefreshRequest setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }
}
