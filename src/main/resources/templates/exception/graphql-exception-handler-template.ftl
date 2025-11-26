<#-- GlobalGraphQlExceptionHandler.ftl -->
<#setting number_format="computer">
<#if isDetailed>import java.util.Map;</#if>

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.validation.BindException;

${projectImports}
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

@ControllerAdvice(annotations = Controller.class)
public class GlobalGraphQlExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalGraphQlExceptionHandler.class);

    private static final String GENERAL_EXCEPTION_MESSAGE = "Server is unavailable.";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
    private static final String INVALID_FORMAT_MESSAGE = "Invalid format.";
    private static final String INVALID_RESOURCE_STATE_MESSAGE = "Invalid resource state";

    private GraphQLError buildError(final DataFetchingEnvironment env, final ErrorType type,
            final String message, final Throwable ex) {
                
        GraphqlErrorBuilder<?> builder = GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(message);

        <#if isDetailed>
        builder = builder.extensions(Map.of(
                "exception", ex.getClass().getSimpleName(),
                "path", env.getExecutionStepInfo().getPath().toString()
        ));
        </#if>

        return builder.build();
    }

    @GraphQlExceptionHandler(ResourceNotFoundException.class)
    public GraphQLError handleResourceNotFound(final ResourceNotFoundException ex, final DataFetchingEnvironment env) {
        return buildError(
            env,
            ErrorType.NOT_FOUND,
            String.format("%s %s", RESOURCE_NOT_FOUND_MESSAGE, ex.getMessage()),
            ex
        );
    }

    @GraphQlExceptionHandler(IllegalArgumentException.class)
    public GraphQLError handleIllegalArgument(final IllegalArgumentException ex, final DataFetchingEnvironment env) {
        return buildError(
            env,
            ErrorType.BAD_REQUEST,
            String.format("%s %s", INVALID_FORMAT_MESSAGE, ex.getMessage()),
            ex
        );
    }

    @GraphQlExceptionHandler(ConstraintViolationException.class)
    public GraphQLError handleConstraintViolation(final ConstraintViolationException ex, final DataFetchingEnvironment env) {
        return buildError(
            env,
            ErrorType.BAD_REQUEST,
            String.format("%s %s", INVALID_FORMAT_MESSAGE, ex.getMessage()),
            ex
        );
    }

    @GraphQlExceptionHandler(BindException.class)
    public GraphQLError handleBindException(final BindException ex, final DataFetchingEnvironment env) {
        return buildError(
            env,
            ErrorType.BAD_REQUEST,
            String.format("%s %s", INVALID_FORMAT_MESSAGE, ex.getMessage()),
            ex
        );
    }

    <#if hasRelations>
    @GraphQlExceptionHandler(InvalidResourceStateException.class)
    public GraphQLError handleInvalidResourceState(final InvalidResourceStateException ex, final DataFetchingEnvironment env) {
        return buildError(
            env,
            ErrorType.BAD_REQUEST,
            String.format("%s %s", INVALID_RESOURCE_STATE_MESSAGE, ex.getMessage()),
            ex
        );
    }
    </#if>

    @GraphQlExceptionHandler(Exception.class)
    public GraphQLError handleGeneric(final Exception ex, final DataFetchingEnvironment env) {
        LOGGER.error("Unexpected error during GraphQL data fetching", ex);
        return buildError(
            env,
            ErrorType.INTERNAL_ERROR,
            GENERAL_EXCEPTION_MESSAGE,
            ex
        );
    }
}
