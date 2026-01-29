/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerObjectModeEnum;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerSchemaModeEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.templates.SwaggerTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;
import dev.markozivkovic.springcrudgenerator.utils.SwaggerUtils;

public class SwaggerDocumentationGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerDocumentationGenerator.class);

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
    public void generate(final String outputDir) {

        if (Objects.isNull(this.configuration) || Objects.isNull(configuration.getOpenApi())
                    || !Boolean.TRUE.equals(configuration.getOpenApi().getApiSpec())) {
            return;
        }

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)) { return; }

        LOGGER.info("Generating Swagger documentation");

        final String pathToSwaggerDocs = String.format("%s/%s", projectMetadata.getProjectBaseDir(), GeneratorConstants.SRC_MAIN_RESOURCES);

        entities.stream()
            .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
            .forEach(e -> {
                this.generateObjects(e, pathToSwaggerDocs);
                this.generateCreateObjects(e, pathToSwaggerDocs);
                this.generateUpdateObjects(e, pathToSwaggerDocs);
            });

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

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER);

        LOGGER.info("Swagger documentation generated successfully at: {}", pathToSwaggerDocs);
    }

    /**
     * Generates swagger schema for the given model.
     * Depending on the mode:
     * - DEFAULT: includes all fields
     * - CREATE_MODEL: excludes ID field, uses INPUT mode for properties, title ends with "Create"
     * - UPDATE_MODEL: excludes ID field and relation fields, title ends with "Update"
     *
     * @param e the model for which to generate the swagger schema
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     * @param mode generation mode
     */
    private void generateObjects(final ModelDefinition e, final String pathToSwaggerDocs,
                final SwaggerObjectModeEnum mode) {

        final String strippedModelName = ModelNameUtils.stripSuffix(e.getName());

        final Map<String, Object> modelContext = new HashMap<>();
        modelContext.put("schemaName", e.getName());

        if (StringUtils.isNotBlank(e.getDescription())) {
            modelContext.put("description", e.getDescription());
        }

        final List<Map<String, Object>> properties;
        if (!SwaggerObjectModeEnum.JSON_MODEL.equals(mode)) {
            final FieldDefinition idField = FieldUtils.extractIdField(e.getFields());
            properties = e.getFields().stream()
                    .filter(field -> SwaggerObjectModeEnum.DEFAULT.equals(mode) || !field.equals(idField))
                    .filter(field -> !SwaggerObjectModeEnum.UPDATE_MODEL.equals(mode) || Objects.isNull(field.getRelation()))
                    .map(field -> {
                        if (SwaggerObjectModeEnum.CREATE_MODEL.equals(mode)) {
                            return SwaggerUtils.toSwaggerProperty(field, SwaggerSchemaModeEnum.INPUT);
                        }
                        return SwaggerUtils.toSwaggerProperty(field);
                    })
                    .collect(Collectors.toList());
        } else {
            properties = e.getFields().stream()
                    .map(field -> SwaggerUtils.toSwaggerProperty(field))
                    .collect(Collectors.toList());
        }

        modelContext.put("properties", properties);

        final String title = switch (mode) {
            case CREATE_MODEL -> ModelNameUtils.computeOpenApiCreateModelName(strippedModelName);
            case UPDATE_MODEL -> ModelNameUtils.computeOpenApiUpdateModelName(strippedModelName);
            case DEFAULT -> ModelNameUtils.computeOpenApiModelName(strippedModelName);
            case JSON_MODEL -> ModelNameUtils.computeOpenApiModelName(strippedModelName);
        };
        modelContext.put("title", title);

        final List<String> requiredFields = switch(mode) {
            case DEFAULT -> FieldUtils.extractRequiredFields(e.getFields());
            case CREATE_MODEL -> FieldUtils.extractRequiredFieldsForCreate(e.getFields());
            case UPDATE_MODEL -> FieldUtils.extractRequiredFieldsForUpdate(e.getFields());
            case JSON_MODEL -> FieldUtils.extractRequiredFields(e.getFields());
        };
        modelContext.put("required", requiredFields);

        if (SwaggerObjectModeEnum.DEFAULT.equals(mode) && Objects.nonNull(e.getAudit()) && Boolean.TRUE.equals(e.getAudit().getEnabled())) {

            final String auditType = AuditUtils.resolveAuditType(e.getAudit().getType());
            modelContext.put(TemplateContextConstants.AUDIT_ENABLED, true);
            modelContext.put(TemplateContextConstants.AUDIT_TYPE, SwaggerUtils.resolve(auditType, List.of()));
        }

        final String swaggerObject = FreeMarkerTemplateProcessorUtils.processTemplate(
            "swagger/schema/object-template.ftl", modelContext
        );

        final String subDir = String.format("%s/%s", GeneratorConstants.DefaultPackageLayout.SWAGGER, "components/schemas");

        FileWriterUtils.writeToFile(pathToSwaggerDocs, subDir, String.format("%s.yaml", StringUtils.uncapitalize(title)), swaggerObject);
    }

    /**
     * Generates create model swagger schema for the given model.
     * 
     * @param e                 the model definition
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     */
    private void generateCreateObjects(final ModelDefinition e, final String pathToSwaggerDocs) {
        generateObjects(e, pathToSwaggerDocs, SwaggerObjectModeEnum.CREATE_MODEL);
    }

    /**
     * Generates update model swagger schema for the given model.
     * 
     * @param e                 the model definition
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     */
    private void generateUpdateObjects(final ModelDefinition e, final String pathToSwaggerDocs) {
        generateObjects(e, pathToSwaggerDocs, SwaggerObjectModeEnum.UPDATE_MODEL);
    }

    /**
     * Generates default model swagger schema for the given model.
     * 
     * @param e                 the model definition
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     */
    private void generateObjects(final ModelDefinition e, final String pathToSwaggerDocs) {
        generateObjects(e, pathToSwaggerDocs, SwaggerObjectModeEnum.DEFAULT);
    }

    /**
     * Generates JSON object swagger schema for the given model.
     * 
     * @param e                 the model definition
     * @param pathToSwaggerDocs the path to the swagger documentation directory
     */
    private void generateJsonObjects(final ModelDefinition e, final String pathToSwaggerDocs) {
        generateObjects(e, pathToSwaggerDocs, SwaggerObjectModeEnum.JSON_MODEL);
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
                this.generateJsonObjects(entity, pathToSwaggerDocs);
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

        final List<String> schemaNames = new ArrayList<>();
        schemaNames.add(StringUtils.uncapitalize(ModelNameUtils.computeOpenApiModelName(e.getName())));
        schemaNames.add(StringUtils.uncapitalize(ModelNameUtils.computeOpenApiCreateModelName(e.getName())));
        schemaNames.add(StringUtils.uncapitalize(ModelNameUtils.computeOpenApiUpdateModelName(e.getName())));

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

        final Map<String, Object> context = SwaggerTemplateContext.computeSwaggerTemplateContext(e);
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
                pathToSwaggerDocs, GeneratorConstants.DefaultPackageLayout.SWAGGER,
                String.format("%s-api.yaml", StringUtils.uncapitalize(strippedModelName)), swaggerDocumentation
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
        model.put("title", String.format("%sInput", strippedModelName));
        if (StringUtils.isNotBlank(relationModel.getDescription())) {
            model.put("description", relationModel.getDescription());
        }

        final FieldDefinition idField = FieldUtils.extractIdField(relationModel.getFields());
        final Map<String, Object> idProperty = SwaggerUtils.toSwaggerProperty(idField);
        idProperty.put("name", idField.getName());

        model.put("properties", List.of(idProperty));

        final String swaggerObject = FreeMarkerTemplateProcessorUtils.processTemplate(
            "swagger/schema/object-template.ftl", model
        );
        final String subDir = String.format("%s/%s", GeneratorConstants.DefaultPackageLayout.SWAGGER, "components/schemas");

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

        final Map<String, Object> context = SwaggerTemplateContext.computeRelationEndpointContext(modelDefinition, this.entities);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/relation-endpoint.ftl", context);
    }

    /**
     * Generates the update by ID endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the update by ID endpoint is generated.
     * @return The update by ID endpoint as a string.
     */
    private String updateByIdEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = SwaggerTemplateContext.computeContextWithId(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/update-by-id-endpoint.ftl", context);
    }

    /**
     * Generates the delete by ID endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the delete by ID endpoint is generated.
     * @return The delete by ID endpoint as a string.
     */
    private String deleteByIdEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = SwaggerTemplateContext.computeContextWithId(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/delete-by-id-endpoint.ftl", context);
    }

    /**
     * Generates the get by ID endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the get by ID endpoint is generated.
     * @return The get by ID endpoint as a string.
     */
    private String getByIdEndpoint(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = SwaggerTemplateContext.computeContextWithId(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/get-by-id-endpoint.ftl", context);
    }

    /**
     * Generates the create endpoint for the given model definition and returns it as a string.
     * 
     * @param modelDefinition The model definition for which the create endpoint is generated.
     * @return The create endpoint as a string.
     */
    private String createEndpoint(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = SwaggerTemplateContext.computeBaseContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/create-endpoint.ftl", context);
    }

    /**
     * Generates the get all endpoint for the given model definition and returns it as a string.
     *
     * @param modelDefinition The model definition for which the get all endpoint is generated.
     * @return The get all endpoint as a string.
     */
    private String getAllEndpoint(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = SwaggerTemplateContext.computeBaseContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("swagger/endpoint/get-all-endpoint.ftl", context);
    }

}
