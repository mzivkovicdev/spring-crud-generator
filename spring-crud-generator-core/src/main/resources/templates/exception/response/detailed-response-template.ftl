import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;

public class HttpResponse {
    
    private final String responseId;
    private final String message;
    private final LocalDateTime timestamp;
    private final HttpStatus status;

    public HttpResponse(final String message, final HttpStatus status) {
        this.responseId = UUID.randomUUID().toString();
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.status = status;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HttpResponse)) {
            return false;
        }
        final HttpResponse httpResponse = (HttpResponse) o;
        return Objects.equals(responseId, httpResponse.responseId) &&
                Objects.equals(message, httpResponse.message) &&
                Objects.equals(timestamp, httpResponse.timestamp) &&
                Objects.equals(status, httpResponse.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseId, message, timestamp, status);
    }

    @Override
    public String toString() {
        return "{" +
            " responseId='" + getResponseId() + "'" +
            ", message='" + getMessage() + "'" +
            ", timestamp='" + getTimestamp() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }

}