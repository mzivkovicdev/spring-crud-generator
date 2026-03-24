import java.time.LocalDateTime;
import java.util.Objects;

public class HttpResponse {
    
    private final String message;
    private final LocalDateTime timestamp;

    public HttpResponse(final String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HttpResponse)) {
            return false;
        }
        final HttpResponse httpResponse = (HttpResponse) o;
        return Objects.equals(message, httpResponse.message) &&
                Objects.equals(timestamp, httpResponse.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, timestamp);
    }

    @Override
    public String toString() {
        return "{" +
            " message='" + message + "'" +
            ", timestamp='" + timestamp + "'" +
            "}";
    }

}