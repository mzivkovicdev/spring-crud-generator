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

package dev.markozivkovic.springcrudgenerator.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerObjectModeEnum;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerSchemaModeEnum;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;
import dev.markozivkovic.springcrudgenerator.utils.SwaggerUtils;

public class SwaggerTemplateContext {

    private SwaggerTemplateContext() {}

    /**
     * Computes a Swagger template context for a model definition.
     * 
     * @param modelDefinition the model definition
     * @return a Swagger template context with stripped model name, ID field name, and ID description
     */
    public static Map<String, Object> computeSwaggerTemplateContext(final ModelDefinition modelDefinition) {
        
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put(TemplateContextConstants.ID_DESCRIPTION, Objects.nonNull(idField.getDescription()) ? idField.getDescription() : "");
        
        return context;
    }

    /**
     * Computes the base context for the given model definition.
     *
     * @param modelDefinition The model definition for which the base context is computed.
     * @return A map containing the stripped model name as the value for the key "strippedModelName".
     */
    public static Map<String, Object> computeBaseContext(final ModelDefinition modelDefinition) {
        
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
    public static Map<String, Object> computeContextWithId(final ModelDefinition modelDefinition) {
        
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final Map<String, Object> context = computeBaseContext(modelDefinition);
        context.put("idField", idField.getName());

        return context;
    }

    /**
     * Computes a Swagger template context for a relation endpoint of a model definition.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a Swagger template context with stripped model name, ID field name, ID description, and relation endpoints
     */
    public static Map<String, Object> computeRelationEndpointContext(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        final Map<String, Object> context = computeSwaggerTemplateContext(modelDefinition);
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final Map<String, Object> idProperty = SwaggerUtils.toSwaggerProperty(idField);
        context.put(TemplateContextConstants.ID, idProperty);

        final List<Map<String, Object>> relationEndpoints = modelDefinition.getFields().stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .map(field -> {
                    final Map<String, Object> endpointContext = new HashMap<>();
                    endpointContext.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(field.getType()));
                    endpointContext.put(TemplateContextConstants.RELATION_TYPE, field.getRelation().getType().toUpperCase());

                    final ModelDefinition relationModel = entities.stream()
                            .filter(e -> e.getName().equals(field.getType()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Relation model not found: " + field.getType()));
                    final FieldDefinition relationIdField = FieldUtils.extractIdField(relationModel.getFields());
                    endpointContext.put(TemplateContextConstants.RELATED_ID_PARAM, relationIdField.getName());
                    final Map<String, Object> relatedIdProperty = SwaggerUtils.toSwaggerProperty(relationIdField);
                    endpointContext.put(TemplateContextConstants.RELATED_ID, relatedIdProperty);
                    return endpointContext;
                })
                .collect(Collectors.toList());

        context.put(TemplateContextConstants.RELATIONS, relationEndpoints);
        return context;
    }

    /**
     * Computes the context for a given model definition, given a Swagger object mode.
     * 
     * @param e the model definition
     * @param mode the Swagger object mode
     * @return a map containing the context for the given model definition
     */
    public static Map<String, Object> computeObjectContext(final ModelDefinition e, final SwaggerObjectModeEnum mode) {

        final Map<String, Object> modelContext = new HashMap<>();
        final String strippedModelName = ModelNameUtils.stripSuffix(e.getName());

        modelContext.put(TemplateContextConstants.SCHEMA_NAME, e.getName());
        if (StringUtils.isNotBlank(e.getDescription())) {
            modelContext.put(TemplateContextConstants.DESCRIPTION, e.getDescription());
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

        modelContext.put(TemplateContextConstants.PROPERTIES, properties);

        final String title = switch (mode) {
                case CREATE_MODEL -> ModelNameUtils.computeOpenApiCreateModelName(strippedModelName);
                case UPDATE_MODEL -> ModelNameUtils.computeOpenApiUpdateModelName(strippedModelName);
                case DEFAULT -> ModelNameUtils.computeOpenApiModelName(strippedModelName);
                case JSON_MODEL -> ModelNameUtils.computeOpenApiModelName(strippedModelName);
        };
        modelContext.put(TemplateContextConstants.TITLE, title);

        final List<String> requiredFields = switch(mode) {
                case DEFAULT -> FieldUtils.extractRequiredFields(e.getFields());
                case CREATE_MODEL -> FieldUtils.extractRequiredFieldsForCreate(e.getFields());
                case UPDATE_MODEL -> FieldUtils.extractRequiredFieldsForUpdate(e.getFields());
                case JSON_MODEL -> FieldUtils.extractRequiredFields(e.getFields());
        };
        modelContext.put(TemplateContextConstants.REQUIRED, requiredFields);

        if (SwaggerObjectModeEnum.DEFAULT.equals(mode) && Objects.nonNull(e.getAudit()) && Boolean.TRUE.equals(e.getAudit().getEnabled())) {

            final String auditType = AuditUtils.resolveAuditType(e.getAudit().getType());
            modelContext.put(TemplateContextConstants.AUDIT_ENABLED, true);
            modelContext.put(TemplateContextConstants.AUDIT_TYPE, SwaggerUtils.resolve(auditType, List.of()));
        }

        return modelContext;
    }

    /**
     * Computes a Swagger template context for a relation input model of a model definition.
     * The generated context contains the model name, title, description, and properties.
     * 
     * @param relationModel the model definition of the relation
     * @return a Swagger template context for the relation input model
     */
    public static Map<String, Object> computeRelationInputModelContext(final ModelDefinition relationModel) {

        final String strippedModelName = ModelNameUtils.stripSuffix(relationModel.getName());
        final Map<String, Object> model = new HashMap<>();
        model.put(TemplateContextConstants.SCHEMA_NAME, relationModel.getName());
        model.put(TemplateContextConstants.TITLE, String.format("%sInput", strippedModelName));
        if (StringUtils.isNotBlank(relationModel.getDescription())) {
            model.put(TemplateContextConstants.DESCRIPTION, relationModel.getDescription());
        }

        final FieldDefinition idField = FieldUtils.extractIdField(relationModel.getFields());
        final Map<String, Object> idProperty = SwaggerUtils.toSwaggerProperty(idField);
        idProperty.put(TemplateContextConstants.NAME, idField.getName());

        model.put(TemplateContextConstants.PROPERTIES, List.of(idProperty));
        
        return model;
    }
    
}
