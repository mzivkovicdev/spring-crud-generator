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

package dev.markozivkovic.codegen.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.StringUtils;

public class RestControllerTemplateContext {

    private RestControllerTemplateContext() {}

    /**
     * Creates a template context for a controller class of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a template context for the controller class
     */
    public static Map<String, Object> computeControllerClassContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Controller");
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.RELATIONS, !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put(TemplateContextConstants.JSON_FIELDS, jsonFields);

        return context;
    }

    /**
     * Computes a template context for a create endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a template context for the create endpoint
     */
    public static Map<String, Object> computeCreateEndpointContext(final ModelDefinition modelDefinition,
            final List<ModelDefinition> entities) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final Map<String, Object> context = new HashMap<>();
        
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<Map<String, Object>> inputFields = new ArrayList<>();
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        modelDefinition.getFields().stream()
            .filter(field -> !field.equals(idField))
            .forEach(field -> {

                final boolean isRelation = Objects.nonNull(field.getRelation());
                final boolean isCollection = isRelation && (manyToManyFields.contains(field) || oneToManyFields.contains(field));
                
                final Map<String, Object> fieldContext = new HashMap<>(Map.of(
                    TemplateContextConstants.FIELD, field.getName(),
                    TemplateContextConstants.FIELD_TYPE, field.getResolvedType(),
                    TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(field.getType()),
                    TemplateContextConstants.IS_COLLECTION, isCollection,
                    TemplateContextConstants.IS_RELATION, isRelation,
                    TemplateContextConstants.IS_JSON_FIELD, FieldUtils.isJsonField(field),
                    TemplateContextConstants.IS_ENUM, FieldUtils.isFieldEnum(field)
                ));

                if (isRelation) {
                    final FieldDefinition relationId = entities.stream()
                            .filter(entity -> entity.getName().equals(field.getType()))
                            .findFirst()
                            .map(entity -> FieldUtils.extractIdField(entity.getFields()))
                            .orElseThrow();
                            
                    fieldContext.put(TemplateContextConstants.RELATION_ID_TYPE, relationId.getType());
                    fieldContext.put(TemplateContextConstants.RELATION_ID_FIELD, relationId.getName());
                }
                
                inputFields.add(fieldContext);
        });

        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.INPUT_FIELDS, inputFields);
        context.put(TemplateContextConstants.RELATIONS, !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());

        return context;
    }

    /**
     * Computes a template context for a get by ID endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the get by ID endpoint
     */
    public static Map<String, Object> computeGetByIdEndpointContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }

    /**
     * Computes a template context for a get all endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the get all endpoint
     */
    public static Map<String, Object> computeGetAllEndpointContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);

        return context;
    }

    /**
     * Computes a template context for an update endpoint of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @param swagger whether Swagger is enabled
     * @return a template context for the update endpoint
     */
    public static Map<String, Object> computeUpdateEndpointContext(final ModelDefinition modelDefinition, final boolean swagger) {
        
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();

        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.INPUT_FIELDS, FieldUtils.extractNonIdNonRelationFieldNamesForController(modelDefinition.getFields(), swagger));
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }

    /**
     * Computes a template context for a delete endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the delete endpoint
     */
    public static Map<String, Object> computeDeleteEndpointContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();

        context.put(TemplateContextConstants.MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }

    /**
     * Computes a template context for an add resource relation endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the add resource relation endpoint
     */
    public static Map<String, Object> computeAddResourceRelationEndpointContext(final ModelDefinition modelDefinition) {
        
        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final Map<String, Object> modelContext = new HashMap<>();
        modelContext.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        modelContext.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        modelContext.put(TemplateContextConstants.ID_TYPE, idField.getType());

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<Map<String, Object>> relations = new ArrayList<>();
        
        relationFields.forEach(relation -> {

            final Map<String, Object> relationContext = new HashMap<>();
            final String strippedRelationClassName = ModelNameUtils.stripSuffix(relation.getType());

            relationContext.put(TemplateContextConstants.RELATION_FIELD_MODEL, relation.getName());
            relationContext.put(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME, strippedRelationClassName);
            relationContext.put(TemplateContextConstants.METHOD_NAME, String.format("%ssId%ssPost", StringUtils.uncapitalize(strippedModelName), strippedRelationClassName));
            relations.add(relationContext);
        });
        
        return new HashMap<>(Map.of(
            TemplateContextConstants.MODEL, modelContext,
            TemplateContextConstants.RELATIONS, relations
        ));
    }

    /**
     * Computes a template context for a remove resource relation endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a template context for the remove resource relation endpoint
     */
    public static Map<String, Object> computeRemoveResourceRelationEndpointContext(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {
        
        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final Map<String, Object> modelContext = new HashMap<>();
        modelContext.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        modelContext.put(TemplateContextConstants.ID_TYPE, idField.getType());

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());
        final List<Map<String, Object>> relations = new ArrayList<>();
        
        relationFields.forEach(relation -> {

            final ModelDefinition relationEntity = entities.stream()
                    .filter(entity -> entity.getName().equals(relation.getType()))
                    .findFirst()
                    .orElseThrow();

            final FieldDefinition entityIdField = FieldUtils.extractIdField(relationEntity.getFields());

            final Map<String, Object> relationContext = new HashMap<>();
            final String strippedRelationClassName = ModelNameUtils.stripSuffix(relation.getType());
            relationContext.put(TemplateContextConstants.RELATION_FIELD_MODEL, relation.getName());
            relationContext.put(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME, strippedRelationClassName);
            relationContext.put(TemplateContextConstants.METHOD_NAME, String.format("%ssId%ssDelete", StringUtils.uncapitalize(strippedModelName), strippedRelationClassName));
            relationContext.put(TemplateContextConstants.RELATION_ID_TYPE, entityIdField.getType());
            relationContext.put(TemplateContextConstants.RELATION_FIELD, String.format("%sId", StringUtils.uncapitalize(strippedRelationClassName)));
            relationContext.put(TemplateContextConstants.IS_COLLECTION, manyToManyFields.contains(relation) || oneToManyFields.contains(relation));

            relations.add(relationContext);
        });

        return new HashMap<>(Map.of(
                TemplateContextConstants.MODEL, modelContext,
                TemplateContextConstants.RELATIONS, relations
        ));
    }
    
}
