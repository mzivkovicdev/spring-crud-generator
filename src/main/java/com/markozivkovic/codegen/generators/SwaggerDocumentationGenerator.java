package com.markozivkovic.codegen.generators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.CrudConfiguration;
import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.model.ProjectMetadata;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.SwaggerUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class SwaggerDocumentationGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerDocumentationGenerator.class);
    private static final String SWAGGER = "swagger";
    private static final String SRC_MAIN_RESOURCES = "/src/main/resources";

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;

    public SwaggerDocumentationGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            LOGGER.warn("Model {} has no ID field defined. Skipping Swagger documentation generation.", modelDefinition.getName());
            return;
        }
        
        LOGGER.info("Generating API documentation for {}", modelDefinition.getName());
        
        if (Objects.isNull(configuration) || Objects.isNull(configuration.getSwagger()) || !configuration.getSwagger()) {
            return;
        }

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String pathToSwaggerDocs = String.format("%s/%s", projectMetadata.getProjectBaseDir(), SRC_MAIN_RESOURCES);
        
        final Map<String, Object> context = TemplateContextUtils.computeSwaggerTemplateContext(modelDefinition);
        context.put("create", createEndpoint(modelDefinition));
        context.put("getAll", getAllEndpoint(modelDefinition));
        context.put("getById", getByIdEndpoint(modelDefinition));
        context.put("deleteById", deleteByIdEndpoint(modelDefinition));
        context.put("updateById", updateByIdEndpoint(modelDefinition));
        context.put("objects", computeObjects(modelDefinition));

        final String swaggerDocumentation = FreeMarkerTemplateProcessorUtils.processTemplate(
                "swagger/swagger-template.ftl", context
        );

        FileWriterUtils.writeToFile(
            pathToSwaggerDocs, SWAGGER, String.format("%s.yaml", StringUtils.uncapitalize(strippedModelName)), swaggerDocumentation
        );

        LOGGER.info("Finished generating API documentation for {}", modelDefinition.getName());
    }

    /**
     * Computes the YAML representation of the objects for the given model definition.
     * 
     * @param modelDefinition The model definition for which the objects are computed.
     * @return The YAML representation of the objects for the given model definition.
     */
    private String computeObjects(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = computeBaseContext(modelDefinition);
        if (StringUtils.isNotBlank(modelDefinition.getDescription())) {
            context.put("description", modelDefinition.getDescription());
        }
        context.put("properties", this.computeProperties(modelDefinition));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate(
            "swagger/schema/object-template.ftl", context
        );
    }
    
    /**
     * Computes the properties for the given model definition.
     * 
     * @param modelDefinition The model definition for which the properties are computed.
     * @return A list of maps, each containing the name, type, and description of a field in the model definition.
     */
    private List<Map<String, Object>> computeProperties(final ModelDefinition modelDefinition) {
        
        return modelDefinition.getFields().stream()
                .map(field -> {
                    final Map<String, Object> property = new HashMap<>();
                    property.put("name", field.getName());
                    property.putAll(SwaggerUtils.resolve(field.getType(), field.getValues()));
                    if (StringUtils.isNotBlank(field.getDescription())) {
                        property.put("description", field.getDescription());
                    }
                    return property;
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates the update by ID endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the update by ID endpoint is generated.
     * @return The update by ID endpoint as a string.
     */
    private String updateByIdEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = this.computeContextWithId(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/update-by-id-endpoint.ftl", context);
    }

    /**
     * Generates the delete by ID endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the delete by ID endpoint is generated.
     * @return The delete by ID endpoint as a string.
     */
    private String deleteByIdEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = this.computeContextWithId(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/delete-by-id-endpoint.ftl", context);
    }

    /**
     * Generates the get by ID endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the get by ID endpoint is generated.
     * @return The get by ID endpoint as a string.
     */
    private String getByIdEndpoint(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = this.computeContextWithId(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/get-by-id-endpoint.ftl", context);
    }

    /**
     * Generates the create endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the create endpoint is generated.
     * @return The create endpoint as a string.
     */
    private String createEndpoint(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = this.computeBaseContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/create-endpoint.ftl", context);
    }

    /**
     * Generates the get all endpoint for the given model definition and returns it as a string.
     *
     * @param modelDefinition The model definition for which the get all endpoint is generated.
     * @return The get all endpoint as a string.
     */
    private String getAllEndpoint(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = this.computeBaseContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/get-all-endpoint.ftl", context);
    }

    /**
     * Computes the base context for the given model definition.
     *
     * @param modelDefinition The model definition for which the base context is computed.
     * @return A map containing the stripped model name as the value for the key "strippedModelName".
     */
    private Map<String, Object> computeBaseContext(final ModelDefinition modelDefinition) {
        
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final Map<String, Object> context = new HashMap<>(
                Map.of("strippedModelName", strippedModelName)
        );

        return context;
    }

    /**
     * Computes the context for the given model definition, including the ID field.
     *
     * @param modelDefinition The model definition for which the context is computed.
     * @return A map containing the stripped model name as the value for the key "strippedModelName",
     *         and the name of the ID field as the value for the key "idField".
     */
    private Map<String, Object> computeContextWithId(final ModelDefinition modelDefinition) {
        
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final Map<String, Object> context = computeBaseContext(modelDefinition);
        context.put("idField", idField.getName());

        return context;
    }

}
