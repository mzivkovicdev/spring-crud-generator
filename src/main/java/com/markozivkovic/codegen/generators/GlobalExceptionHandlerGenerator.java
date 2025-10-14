package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.CrudConfiguration.ErrorResponse;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class GlobalExceptionHandlerGenerator implements CodeGenerator {

    private static final String EXCEPTIONS = "exceptions";
    private static final String EXCEPTIONS_RESPONSES = EXCEPTIONS + "/responses";
    private static final String EXCEPTIONS_HANDLERS = EXCEPTIONS + "/handlers";

    private static final String EXCEPTIONS_PACKAGE = "." + EXCEPTIONS;
    private static final String EXCEPTIONS_HANDLERS_PACKAGE = EXCEPTIONS_PACKAGE + ".handlers";
    private static final String EXCEPTIONS_RESPONSES_PACKAGE = EXCEPTIONS_PACKAGE + ".responses";
    
    private static final String HAS_RELATIONS = "hasRelations";
    private static final String PROJECT_IMPORTS = "projectImports";
    private static final String IS_DETAILED = "isDetailed";
    
    private static final String HTTP_RESPONSE = "HttpResponse";
    private static final String GLOBAL_EXCEPTION_HANDLER = "GlobalExceptionHandler";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandlerGenerator.class);

    private final CrudConfiguration crudConfiguration;
    private final List<ModelDefinition> entities;

    public GlobalExceptionHandlerGenerator(final CrudConfiguration crudConfiguration, final List<ModelDefinition> entities) {
        this.crudConfiguration = crudConfiguration;
        this.entities = entities;
    }
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (Objects.isNull(crudConfiguration) || Objects.isNull(crudConfiguration.getErrorResponse()) ||
                crudConfiguration.getErrorResponse().equals(ErrorResponse.NONE)) {

            LOGGER.info("Skipping GlobalExceptionHandlerGenerator, as error response is set to NONE or not configured.");
            return;
        }

        this.generateHttpResponse(outputDir);
        this.generateExceptionHandler(outputDir);
    }

    /**
     * Generates the HttpResponse class based on the configured error response.
     * 
     * @param outputDir the base directory where the generated file should be saved
     */
    private void generateHttpResponse(final String outputDir) {

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String httpResponse;

        switch (this.crudConfiguration.getErrorResponse()) {
            case DETAILED:
                httpResponse = "detailed-response-template.ftl";
                break;
            case MINIMAL:
                httpResponse = "minimal-response-template.ftl";
                break;
            case SIMPLE:
            default:
                httpResponse = "simple-response-template.ftl";
                break;
        }

        final String httpResponseTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                String.format("exception/response/%s", httpResponse), Map.of()
        );

        final StringBuilder sb = new StringBuilder();
        
        sb.append(String.format(PACKAGE, packagePath + EXCEPTIONS_RESPONSES_PACKAGE))
                .append(httpResponseTemplate);

        FileWriterUtils.writeToFile(outputDir, EXCEPTIONS_RESPONSES, HTTP_RESPONSE, sb.toString());
    }

    /**
     * Generates a global exception handler class which handles different exceptions.
     *
     * @param outputDir the directory where the generated code will be written
     */
    private void generateExceptionHandler(final String outputDir) {

        final List<FieldDefinition> fields = this.entities.stream()
                .flatMap(models -> models.getFields().stream())
                .collect(Collectors.toList());

        final List<String> relationTypes = FieldUtils.extractRelationTypes(fields);
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final boolean hasRelations = !relationTypes.isEmpty();

        final String exceptionTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "exception/exception-handler-template.ftl", Map.of(
                    HAS_RELATIONS, hasRelations,
                    PROJECT_IMPORTS, ImportUtils.computeGlobalExceptionHandlerProjectImports(hasRelations, outputDir),
                    IS_DETAILED, this.crudConfiguration.getErrorResponse().equals(ErrorResponse.DETAILED)
                )
        );

        final StringBuilder sb = new StringBuilder();
        
        sb.append(String.format(PACKAGE, packagePath + EXCEPTIONS_HANDLERS_PACKAGE))
                .append(exceptionTemplate);

        FileWriterUtils.writeToFile(outputDir, EXCEPTIONS_HANDLERS, GLOBAL_EXCEPTION_HANDLER, sb.toString());
    }

}
