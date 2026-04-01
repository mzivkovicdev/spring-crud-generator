public class AuthRequest {

    private String username;
    private String password;

    public AuthRequest() {}

    public AuthRequest(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public AuthRequest setUsername(final String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return this.password;
    }

    public AuthRequest setPassword(final String password) {
        this.password = password;
        return this;
    }
}
