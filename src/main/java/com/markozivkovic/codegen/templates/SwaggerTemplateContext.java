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

package com.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.SwaggerUtils;

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
    
}
