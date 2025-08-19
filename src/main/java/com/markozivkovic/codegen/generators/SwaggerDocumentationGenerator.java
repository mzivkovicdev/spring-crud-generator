package com.markozivkovic.codegen.generators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.context.GeneratorContext;
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
    private final List<ModelDefinition> entities;

    public SwaggerDocumentationGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata,
            final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (Objects.isNull(configuration) || Objects.isNull(configuration.getSwagger()) || !configuration.getSwagger()) {
            return;
        }

        if (GeneratorContext.isGenerated(SWAGGER)) { return; }

        final String pathToSwaggerDocs = String.format("%s/%s", projectMetadata.getProjectBaseDir(), SRC_MAIN_RESOURCES);

        entities.forEach(e -> {
            if (!FieldUtils.isAnyFieldId(e.getFields())) {
                LOGGER.warn("Model {} has no ID field defined. Skipping Swagger documentation generation.", e.getName());
                return;
            }

            final String strippedModelName = ModelNameUtils.stripSuffix(e.getName());
            final Map<String, Object> model = new HashMap<>();
            model.put("schemaName", e.getName());
            if (StringUtils.isNotBlank(e.getDescription())) {
                model.put("description", e.getDescription());
            }

            final List<Map<String, Object>> properties = e.getFields().stream()
                    .map(field -> SwaggerUtils.toSwaggerProperty(field))
                    .collect(Collectors.toList());

            model.put("properties", properties);

            final String swaggerObject = FreeMarkerTemplateProcessorUtils.processTemplate(
                "swagger/schema/object-template.ftl", model
            );
            final String subDir = String.format("%s/%s", SWAGGER, "components/schemas");

            FileWriterUtils.writeToFile(
                pathToSwaggerDocs, subDir, String.format("%s.yaml", StringUtils.uncapitalize(strippedModelName)), swaggerObject
            );
        });

        entities.forEach(e -> {

            if (!FieldUtils.isAnyFieldId(e.getFields())) {
                LOGGER.warn("Model {} has no ID field defined. Skipping Swagger documentation generation.", e.getName());
                return;
            }

            final List<FieldDefinition> relations = FieldUtils.extractRelationFields(e.getFields());
            final List<String> schemaNames = relations.stream()
                    .map(FieldDefinition::getType)
                    .filter(StringUtils::isNotBlank)
                    .map(ModelNameUtils::stripSuffix)
                    .map(StringUtils::uncapitalize)
                    .distinct()
                    .collect(Collectors.toList());
            schemaNames.add(StringUtils.uncapitalize(ModelNameUtils.stripSuffix(e.getName())));

            final Map<String, Object> context = TemplateContextUtils.computeSwaggerTemplateContext(e);
            context.put("create", createEndpoint(e));
            context.put("getAll", getAllEndpoint(e));
            context.put("getById", getByIdEndpoint(e));
            context.put("deleteById", deleteByIdEndpoint(e));
            context.put("updateById", updateByIdEndpoint(e));
            context.put("schemaNames", schemaNames);

            final String strippedModelName = ModelNameUtils.stripSuffix(e.getName());
            final String swaggerDocumentation = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "swagger/swagger-template.ftl", context
            );

            FileWriterUtils.writeToFile(
                    pathToSwaggerDocs, SWAGGER, String.format("%s.yaml", StringUtils.uncapitalize(strippedModelName)), swaggerDocumentation
            );
        });

        GeneratorContext.markGenerated(SWAGGER);
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
