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
import java.util.stream.Collectors;

import dev.markozivkovic.codegen.constants.AnnotationConstants;
import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.StringUtils;

public class BusinessServiceTemplateContext {

    private BusinessServiceTemplateContext() {}

    /**
     * Computes a template context for a business service class of a model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @return a template context for the business service class
     */
    public static Map<String, Object> computeBusinessServiceContext(final ModelDefinition modelDefinition) {
        
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
       
        final List<String> serviceClasses = relationFields.stream()
                .map(FieldDefinition::getType)
                .map(ModelNameUtils::stripSuffix)
                .map(modelName -> String.format("%sService", modelName))
                .collect(Collectors.toList());
                
        serviceClasses.add(String.format("%sService", strippedModelName));
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, String.format("%sBusinessService", strippedModelName));
        context.put(TemplateContextConstants.SERVICE_CLASSES, serviceClasses);

        return context;
    }

    /**
     * Computes a template context for the addRelation method of a model's service.
     *
     * @param modelDefinition the model definition containing the class and field details
     * @param entities a list of model definitions representing entities related to the model
     * @return a map representing the context for the addRelation method, or an empty map if no relation types are present
     */
    public static Map<String, Object> computeAddRelationMethodServiceContext(final ModelDefinition modelDefinition,
            final List<ModelDefinition> entities) {
        
        return computeRelationMethodContext(modelDefinition, true, entities);
    }

    /**
     * Computes a template context for the removeRelation method of a model's service.
     *
     * @param modelDefinition the model definition containing the class and field details
     * @param entities a list of model definitions representing entities related to the model
     * @return a map representing the context for the removeRelation method, or an empty map if no relation types are present
     */
    public static Map<String, Object> computeRemoveRelationMethodServiceContext(final ModelDefinition modelDefinition,
            final List<ModelDefinition> entities) {
        
        return computeRelationMethodContext(modelDefinition, false, entities);
    }

    /**
     * Computes a template context for the create resource method of a model's service.
     *
     * @param modelDefinition the model definition containing the class and field details
     * @param entities a list of model definitions representing entities related to the model
     * @return a map representing the context for the create resource method, or an empty map if no relation types are present
     */
    public static Map<String, Object> computeCreateResourceMethodServiceContext(final ModelDefinition modelDefinition,
            final List<ModelDefinition> entities) {

        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final String inputArgs = FieldUtils.generateInputArgsExcludingId(modelDefinition.getFields(), entities).stream()
                .collect(Collectors.joining(", "));
                
        final String testInputArgs = FieldUtils.generateInputArgsExcludingIdForTest(modelDefinition.getFields(), entities).stream()
                .collect(Collectors.joining(", "));

        final String fieldNames = FieldUtils.generateInputArgsBusinessService(modelDefinition.getFields()).stream()
                .collect(Collectors.joining(", "));

        final Map<String, Object> model = new HashMap<>();
        final List<Map<String, Object>> relations = new ArrayList<>();

        model.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        model.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        model.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, AnnotationConstants.TRANSACTIONAL_ANNOTATION);
        if (GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION)) {
            model.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION);
        } else {
            model.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, AnnotationConstants.TRANSACTIONAL_ANNOTATION);
        }
        model.put(TemplateContextConstants.MODEL_SERVICE, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Service");
        model.put(TemplateContextConstants.INPUT_ARGS, inputArgs);
        model.put(TemplateContextConstants.FIELD_NAMES, fieldNames);
        model.put(TemplateContextConstants.TEST_INPUT_ARGS, testInputArgs);

        relationFields.forEach(field -> {
            relations.add(computeRelationContext(field, idField, manyToManyFields, oneToManyFields, true, entities));
        });

        return Map.of(
            TemplateContextConstants.MODEL, model,
            TemplateContextConstants.RELATIONS, relations
        );
    }

    /**
     * Computes a template context for a relation field, given the original field
     * definition, the ID field definition, the many-to-many fields, the one-to-many
     * fields and whether the relation is an add or a remove relation.
     * 
     * @param field the field definition
     * @param idField the ID field definition
     * @param manyToManyFields the many-to-many fields
     * @param oneToManyFields the one-to-many fields
     * @param isAddRelation whether the relation is an add or a remove relation
     * @return a template context for the relation field
     */
    private static Map<String, Object> computeRelationContext(final FieldDefinition field, final FieldDefinition idField,
            final List<FieldDefinition> manyToManyFields, final List<FieldDefinition> oneToManyFields,
            final Boolean isAddRelation, final List<ModelDefinition> entities) {

        final Map<String, Object> relation = new HashMap<>();
        final String strippedFieldName = StringUtils.capitalize(ModelNameUtils.stripSuffix(field.getName()));
        final String methodName = isAddRelation ? String.format("add%s", strippedFieldName) :
                String.format("remove%s", strippedFieldName);

        entities.stream()
                .filter(entity -> entity.getName().equals(field.getType()))
                .findFirst()
                .ifPresent(entity -> {
                    final FieldDefinition entityIdField = FieldUtils.extractIdField(entity.getFields());
                    relation.put(TemplateContextConstants.RELATION_ID_TYPE, entityIdField.getType());
                    relation.put(TemplateContextConstants.RELATION_ID_FIELD, entityIdField.getName());
                });
        
        relation.put(TemplateContextConstants.RELATION_CLASS_NAME, field.getType());
        relation.put(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME, ModelNameUtils.stripSuffix(field.getType()));
        relation.put(TemplateContextConstants.ELEMENT_PARAM, field.getName());
        relation.put(TemplateContextConstants.RELATION_FIELD, strippedFieldName);
        relation.put(TemplateContextConstants.IS_COLLECTION, manyToManyFields.contains(field) || oneToManyFields.contains(field));
        relation.put(TemplateContextConstants.JAVADOC_FIELDS, FieldUtils.computeJavadocForFields(idField, field));
        relation.put(TemplateContextConstants.METHOD_NAME, methodName);

        return relation;
    }

    /**
     * Computes a template context for a relation method of a model.
     * 
     * @param modelDefinition the model definition
     * @param isAddRelation whether the relation is an add or a remove relation
     * @return a template context for the relation method
     */
    private static Map<String, Object> computeRelationMethodContext(final ModelDefinition modelDefinition, final Boolean isAddRelation,
            final List<ModelDefinition> entities) {

        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());

        final Map<String, Object> model = new HashMap<>();
        final List<Map<String, Object>> relations = new ArrayList<>();

        model.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        if (GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION)) {
            model.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION);
        } else {
            model.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, AnnotationConstants.TRANSACTIONAL_ANNOTATION);
        }
        model.put(TemplateContextConstants.ID_TYPE, idField.getType());
        model.put(TemplateContextConstants.ID_FIELD, idField.getName());
        model.put(TemplateContextConstants.MODEL_SERVICE, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Service");
        model.put(TemplateContextConstants.STRIPPED_MODEL_NAME, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(modelDefinition.getName())));
        
        relationFields.forEach(field -> 
            relations.add(computeRelationContext(field, idField, manyToManyFields, oneToManyFields, isAddRelation, entities))
        );

        return new HashMap<>(Map.of(
                TemplateContextConstants.MODEL, model,
                TemplateContextConstants.RELATIONS, relations
        ));
    }
    
}
