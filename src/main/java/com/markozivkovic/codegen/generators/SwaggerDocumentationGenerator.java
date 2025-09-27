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

        LOGGER.info("Generating Swagger documentation");

        final String pathToSwaggerDocs = String.format("%s/%s", projectMetadata.getProjectBaseDir(), SRC_MAIN_RESOURCES);

        entities.stream()
            .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
            .forEach(e -> this.generateObjects(e, pathToSwaggerDocs));

        final List<ModelDefinition> relationModels = entities.stream()
                .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
                .flatMap(e -> e.getFields().stream())
                .filter(e -> Objects.nonNull(e.getRelation()))
                .distinct()
                .map(relationField -> {
                    return this.entities.stream()
                        .filter(e -> e.getName().equals(relationField.getType()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            String.format(
                                "Relation model not found: %s", relationField.getType()
                            ))
                        );
                }).collect(Collectors.toList());

        relationModels.forEach(relationModel -> 
            this.generateRelationInputModels(relationModel, pathToSwaggerDocs)
        );

        this.generateJsonObjects(pathToSwaggerDocs);

        entities.stream()
            .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
            .forEach(e -> this.generateSwaggerDocumentation(relationModels, e, pathToSwaggerDocs));

        GeneratorContext.markGenerated(SWAGGER);

        LOGGER.info("Swagger documentation generated successfully at: {}", pathToSwaggerDocs);
    }

    /**
     * Generates swagger schema for the given model. The generated schema
     * will have one property for each field in the given model.
     *
     * @param e the model for which to generate the swagger schema
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     */
    private void generateObjects(final ModelDefinition e, final String pathToSwaggerDocs) {

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
    }

    /**
     * Generates JSON objects for all entities that have fields of type JSON or JSONB.
     * The generated JSON objects will be created in the specified path to the Swagger
     * documentation directory.
     *
     * @param pathToSwaggerDocs the path to the Swagger documentation directory
     */
    private void generateJsonObjects(final String pathToSwaggerDocs) {
        
        final List<FieldDefinition> fields = this.entities.stream()
                .flatMap(entity -> entity.getFields().stream())
                .distinct()
                .collect(Collectors.toList());

        final List<String> jsonFields = FieldUtils.extractJsonFields(fields).stream()
                .map(FieldUtils::extractJsonFieldName)
                .distinct()
                .collect(Collectors.toList());

        this.entities.stream()
            .filter(entity -> jsonFields.contains(entity.getName()))
            .forEach(entity -> {
                this.generateObjects(entity, pathToSwaggerDocs);
            });
    }

    /**
     * Generates swagger documentation for the given model. The generated documentation
     * will include endpoints for creating, retrieving all, retrieving by ID, deleting by ID,
     * and updating by ID. If the model has any relation fields, the generated documentation
     * will also include endpoints for creating, retrieving, deleting, and updating
     * relation models.
     *
     * @param relationModels the list of models that are relation models
     * @param e the model to generate swagger documentation for
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     */
    private void generateSwaggerDocumentation(final List<ModelDefinition> relationModels, final ModelDefinition e,
            final String pathToSwaggerDocs) {

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(e.getFields());
        final List<String> schemaNames = relations.stream()
                .map(FieldDefinition::getType)
                .filter(StringUtils::isNotBlank)
                .map(ModelNameUtils::stripSuffix)
                .map(StringUtils::uncapitalize)
                .distinct()
                .collect(Collectors.toList());
        schemaNames.add(StringUtils.uncapitalize(ModelNameUtils.stripSuffix(e.getName())));

        final List<String> relationInputSchemaNames = relationModels.stream()
                .map(ModelDefinition::getName)
                .map(ModelNameUtils::stripSuffix)
                .map(StringUtils::uncapitalize)
                .map(name -> String.format("%sInput", name))
                .distinct()
                .collect(Collectors.toList());
        schemaNames.addAll(relationInputSchemaNames);
        
        final FieldDefinition idField = FieldUtils.extractIdField(e.getFields());
        final Map<String, Object> idProperty = SwaggerUtils.toSwaggerProperty(idField);

        final Map<String, Object> context = TemplateContextUtils.computeSwaggerTemplateContext(e);
        context.put("id", idProperty);
        context.put("create", createEndpoint(e));
        context.put("getAll", getAllEndpoint(e));
        context.put("getById", getByIdEndpoint(e));
        context.put("deleteById", deleteByIdEndpoint(e));
        context.put("updateById", updateByIdEndpoint(e));
        context.put("relationEndpoints", relationEndpoints(e));
        context.put("schemaNames", schemaNames);

        final String strippedModelName = ModelNameUtils.stripSuffix(e.getName());
        final String swaggerDocumentation = FreeMarkerTemplateProcessorUtils.processTemplate(
                "swagger/swagger-template.ftl", context
        );

        FileWriterUtils.writeToFile(
                pathToSwaggerDocs, SWAGGER, String.format("%s-api.yaml", StringUtils.uncapitalize(strippedModelName)), swaggerDocumentation
        );
    }

    /**
     * Generates swagger schema for the input transfer object of the given relation model. The schema will have one
     * property, "id", which is the ID of the relation model.
     *
     * @param relationModel the relation model
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     */
    private void generateRelationInputModels(final ModelDefinition relationModel, final String pathToSwaggerDocs) {

        final String strippedModelName = ModelNameUtils.stripSuffix(relationModel.getName());
        final Map<String, Object> model = new HashMap<>();
        model.put("schemaName", relationModel.getName());
        if (StringUtils.isNotBlank(relationModel.getDescription())) {
            model.put("description", relationModel.getDescription());
        }

        final FieldDefinition idField = FieldUtils.extractIdField(relationModel.getFields());
        final Map<String, Object> idProperty = SwaggerUtils.toSwaggerProperty(idField);
        idProperty.put("name", "id");

        model.put("properties", List.of(idProperty));

        final String swaggerObject = FreeMarkerTemplateProcessorUtils.processTemplate(
            "swagger/schema/object-template.ftl", model
        );
        final String subDir = String.format("%s/%s", SWAGGER, "components/schemas");

        FileWriterUtils.writeToFile(
            pathToSwaggerDocs, subDir, String.format("%sInput.yaml", StringUtils.uncapitalize(strippedModelName)), swaggerObject
        );
    }

    /**
     * Generates the swagger documentation for the add relation endpoints of the given model definition.
     * 
     * @param modelDefinition the model definition
     * @return a string containing the swagger documentation for the add relation endpoints
     */
    private String relationEndpoints(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeSwaggerTemplateContext(modelDefinition);
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final Map<String, Object> idProperty = SwaggerUtils.toSwaggerProperty(idField);
        context.put("id", idProperty);

        final List<Map<String, Object>> relationEndpoints = modelDefinition.getFields().stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .map(field -> {
                    final Map<String, Object> endpointContext = new HashMap<>();
                    endpointContext.put("strippedModelName", ModelNameUtils.stripSuffix(field.getType()));
                    endpointContext.put("relationType", field.getRelation().getType().toUpperCase());

                    final ModelDefinition relationModel = this.entities.stream()
                            .filter(e -> e.getName().equals(field.getType()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Relation model not found: " + field.getType()));
                    final FieldDefinition relationIdField = FieldUtils.extractIdField(relationModel.getFields());
                    endpointContext.put("relatedIdParam", relationIdField.getName());
                    final Map<String, Object> relatedIdProperty = SwaggerUtils.toSwaggerProperty(relationIdField);
                    endpointContext.put("relatedId", relatedIdProperty);
                    return endpointContext;
                })
                .collect(Collectors.toList());

        context.put("relations", relationEndpoints);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/relation-endpoint.ftl", context);
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
