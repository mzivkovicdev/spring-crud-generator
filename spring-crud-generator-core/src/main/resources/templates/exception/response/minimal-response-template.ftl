import java.util.Objects;

public class HttpResponse {
    
    private final String message;
    
    public HttpResponse(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HttpResponse)) {
            return false;
        }
        final HttpResponse httpResponse = (HttpResponse) o;
        return Objects.equals(message, httpResponse.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }

    @Override
    public String toString() {
        return "{" +
                "message='" + message + '\'' +
                '}';
    }

}