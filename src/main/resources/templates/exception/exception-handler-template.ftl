import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

${projectImports}
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String EXTENDED_MESSAGE_FORMAT = "%s %s";
    private static final String METHOD_NOT_ALLOWED_MESSAGE = "This method is not supported. Please send a '%s' request";
    private static final String INVALID_FORMAT_MESSAGE = "Invalid format.";
    private static final String REQUEST_NOT_READABLE_MESSAGE = "Not readable HTTP message";
    private static final String GENERAL_EXCEPTION_MESSAGE = "Server is unavailable.";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
    private static final String INVALID_RESOURCE_STATE_MESSAGE = "Invalid resource state";

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<HttpResponse> methodNotSupportedError(final HttpRequestMethodNotSupportedException e) {

        final HttpMethod supportedHttpMethods = Objects.requireNonNull(e.getSupportedHttpMethods()).stream()
                .findAny()
                .orElseThrow();

        return new ResponseEntity<>(
                new HttpResponse(
                        String.format(METHOD_NOT_ALLOWED_MESSAGE, supportedHttpMethods)<#if isDetailed>,
                        HttpStatus.METHOD_NOT_ALLOWED</#if>
                ),
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<HttpResponse> methodArgumentTypeMissmatchError(final MethodArgumentTypeMismatchException e) {

        return new ResponseEntity<>(
                new HttpResponse(
                        String.format(EXTENDED_MESSAGE_FORMAT, INVALID_FORMAT_MESSAGE, e.getMessage())<#if isDetailed>,
                        HttpStatus.BAD_REQUEST</#if>
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HttpResponse> invalidArgumentError(final IllegalArgumentException e) {

        return new ResponseEntity<>(
                new HttpResponse(
                        String.format(EXTENDED_MESSAGE_FORMAT, INVALID_FORMAT_MESSAGE, e.getMessage())<#if isDetailed>,
                        HttpStatus.BAD_REQUEST</#if>
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HttpResponse> messageNotReadableError(final HttpMessageNotReadableException e) {

        return new ResponseEntity<>(
                new HttpResponse(
                        String.format(EXTENDED_MESSAGE_FORMAT, REQUEST_NOT_READABLE_MESSAGE,e.getMessage())<#if isDetailed>,
                        HttpStatus.BAD_REQUEST</#if>
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<HttpResponse> mediaTypeNotSupportedError() {

        return new ResponseEntity<>(
                new HttpResponse(REQUEST_NOT_READABLE_MESSAGE<#if isDetailed>, HttpStatus.UNSUPPORTED_MEDIA_TYPE</#if>),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<HttpResponse> noResourceFoundError(final NoResourceFoundException e) {
        
        return new ResponseEntity<>(
                new HttpResponse(e.getMessage()<#if isDetailed>, HttpStatus.NOT_FOUND</#if>),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<HttpResponse> missingServletRequestParameterError(final MissingServletRequestParameterException e) {
        
        return new ResponseEntity<>(
                new HttpResponse(e.getMessage()<#if isDetailed>, HttpStatus.BAD_REQUEST</#if>),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<HttpResponse> resourceNotFoundError(final ResourceNotFoundException e) {

        return new ResponseEntity<>(
                new HttpResponse(
                    String.format(EXTENDED_MESSAGE_FORMAT, RESOURCE_NOT_FOUND_MESSAGE, e.getMessage())<#if isDetailed>,
                    HttpStatus.NOT_FOUND</#if>
                ),
                HttpStatus.NOT_FOUND
        );
    }
<#if hasRelations>
    @ExceptionHandler(InvalidResourceStateException.class)
    public ResponseEntity<HttpResponse> invalidResourceStateError(final InvalidResourceStateException e) {

        return new ResponseEntity<>(
                new HttpResponse(
                    String.format(EXTENDED_MESSAGE_FORMAT, INVALID_RESOURCE_STATE_MESSAGE, e.getMessage())<#if isDetailed>,
                    HttpStatus.CONFLICT</#if>
                ),
                HttpStatus.CONFLICT
        );
    }
</#if>
    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpResponse> internalServerError(final Exception e) {

        LOGGER.error("An unexpected error occurred", e);

        return new ResponseEntity<>(
                new HttpResponse(GENERAL_EXCEPTION_MESSAGE<#if isDetailed>, HttpStatus.INTERNAL_SERVER_ERROR</#if>),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}