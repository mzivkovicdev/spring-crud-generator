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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.markozivkovic.springcrudgenerator.constants.AnnotationConstants;
import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.utils.ContainerUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

public class ServiceTemplateContext {

    private ServiceTemplateContext() {}

    /**
     * Creates a template context for a service class of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a template context for the service class
     */
    public static Map<String, Object> createServiceClassContext(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Service");
        context.put(TemplateContextConstants.MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));

        return context;
    }

    /**
     * Creates a template context for the getAllByIds method of a model.
     *
     * @param modelDefinition the model definition
     * @return a template context for the getAllByIds method
     */
    public static Map<String, Object> createGetAllByIdsMethodContext(final ModelDefinition modelDefinition) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(modelDefinition.getName())));
        context.put(TemplateContextConstants.ID_DESCRIPTION, idField.getDescription());
        context.put(TemplateContextConstants.GENERATE_JAVA_DOC, StringUtils.isNotBlank(idField.getDescription()));
        
        return context;
    }

    /**
     * Creates a template context for the getAll method of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the getAll method
     */
    public static Map<String, Object> computeGetAllContext(final ModelDefinition modelDefinition) {
    
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(modelDefinition.getName())));
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        
        return context;
    }

    /**
     * Creates a template context for the create method of a model.
     * 
     * The generated context contains the model name, transactional annotation, input fields as strings,
     * field names without the ID field, a list of fields to be documented in the JavaDoc comment.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the create method
     */
    public static Map<String, Object> computeCreateContext(final ModelDefinition modelDefinition) {

        final List<String> inputFields = FieldUtils.generateInputArgsExcludingId(modelDefinition.getFields());
        final List<String> fieldNames = FieldUtils.extractNonIdFieldNames(modelDefinition.getFields());
        final List<String> javadocFields = FieldUtils.extractNonIdFieldForJavadoc(modelDefinition.getFields());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        if (GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION)) {
            context.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION);
        } else {
            context.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, AnnotationConstants.TRANSACTIONAL_ANNOTATION);
        }
        context.put(TemplateContextConstants.INPUT_ARGS, String.join(", ", inputFields));
        context.put(TemplateContextConstants.FIELD_NAMES, String.join(", ", fieldNames));
        context.put(TemplateContextConstants.JAVADOC_FIELDS, javadocFields);
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(modelDefinition.getName())));
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());

        return context;
    }

    /**
     * Creates a template context for the updateById method of a model.
     * 
     * The generated context contains the model name, a list of input fields as strings, a list of field names
     * without the ID field, a list of fields to be documented in the JavaDoc comment, and the transactional
     * annotation.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the updateById method
     */
    public static Map<String, Object> computeUpdateByIdContext(final ModelDefinition modelDefinition) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.INPUT_FIELDS, FieldUtils.generateInputArgsWithoutRelations(modelDefinition.getFields()));
        context.put(TemplateContextConstants.FIELD_NAMES_WITHOUT_ID, FieldUtils.extractNonIdNonRelationFieldNames(modelDefinition.getFields()));
        context.put(TemplateContextConstants.JAVADOC_FIELDS, FieldUtils.extractFieldForJavadocWithoutRelations(modelDefinition.getFields()));
        if (GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION)) {
            context.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION);
        } else {
            context.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, AnnotationConstants.TRANSACTIONAL_ANNOTATION);
        }
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(modelDefinition.getName())));

        return context;
    }

    /**
     * Creates a template context for the deleteById method of a model.
     * 
     * The generated context contains the model name, the ID type, the ID description and a flag
     * indicating whether a JavaDoc comment should be generated. The transactional annotation is
     * also included.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the deleteById method
     */
    public static Map<String, Object> computeDeleteByIdContext(final ModelDefinition modelDefinition) {
        
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final Map<String, Object> context = computeGetByIdContext(modelDefinition);
        
        if (GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION)) {
            context.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION);
        } else {
            context.put(TemplateContextConstants.TRANSACTIONAL_ANNOTATION, AnnotationConstants.TRANSACTIONAL_ANNOTATION);
        }
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(modelDefinition.getName())));
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        
        return context;
    }

    /**
     * Creates a template context for the ID field of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the ID field
     */
    public static Map<String, Object> computeGetByIdContext(final ModelDefinition modelDefinition) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put(TemplateContextConstants.ID_DESCRIPTION, idField.getDescription());
        context.put(TemplateContextConstants.GENERATE_JAVA_DOC, StringUtils.isNotBlank(idField.getDescription()));
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(modelDefinition.getName())));
        
        return context;
    }

    /**
     * Creates a template context for the addRelation method of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the addRelation method
     */
    public static Map<String, Object> createAddRelationMethodContext(final ModelDefinition modelDefinition) {

        return computeRelationMethodContext(modelDefinition, true, List.of());
    }

    /**
     * Creates a template context for the removeRelation method of a model.
     * 
     * @param modelDefinition the model definition
     * @param entities        the list of entities
     * @return a template context for the removeRelation method
     */
    public static Map<String, Object> createRemoveRelationMethodContext(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        return computeRelationMethodContext(modelDefinition, false, entities);
    }

    /**
     * Computes a template context for a relation method of a model.
     * 
     * @param modelDefinition the model definition
     * @param isAddRelation   whether the relation is an add or a remove relation
     * @param entities        the list of entities
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

    /**
     * Computes a template context for a relation field, given the original field
     * definition, the ID field definition, the many-to-many fields, the one-to-many
     * fields and whether the relation is an add or a remove relation.
     * 
     * @param field            the field definition
     * @param idField          the ID field definition
     * @param manyToManyFields the many-to-many fields
     * @param oneToManyFields  the one-to-many fields
     * @param isAddRelation    whether the relation is an add or a remove relation
     * @param entities         the list of entities
     * @return a template context for the relation field
     */
    private static Map<String, Object> computeRelationContext(final FieldDefinition field, final FieldDefinition idField,
            final List<FieldDefinition> manyToManyFields, final List<FieldDefinition> oneToManyFields,
            final Boolean isAddRelation, final List<ModelDefinition> entities) {

        final Map<String, Object> relation = new HashMap<>();
        final String strippedFieldName = StringUtils.capitalize(ModelNameUtils.stripSuffix(field.getName()));
        final String methodName = isAddRelation ? String.format("add%s", strippedFieldName) :
                String.format("remove%s", strippedFieldName);

        if (!ContainerUtils.isEmpty(entities)) {
            entities.stream()
                    .filter(entity -> entity.getName().equals(field.getType()))
                    .findFirst()
                    .ifPresent(entity -> {
                        final FieldDefinition entityIdField = FieldUtils.extractIdField(entity.getFields());
                        relation.put(TemplateContextConstants.RELATION_ID_FIELD, entityIdField.getName());
                    });
        }
        
        relation.put(TemplateContextConstants.RELATION_CLASS_NAME, field.getType());
        relation.put(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME, ModelNameUtils.stripSuffix(field.getType()));
        relation.put(TemplateContextConstants.ELEMENT_PARAM, field.getName());
        relation.put(TemplateContextConstants.RELATION_FIELD, strippedFieldName);
        relation.put(TemplateContextConstants.IS_COLLECTION, manyToManyFields.contains(field) || oneToManyFields.contains(field));
        relation.put(TemplateContextConstants.JAVADOC_FIELDS, FieldUtils.computeJavadocForFields(idField, field));
        relation.put(TemplateContextConstants.METHOD_NAME, methodName);

        return relation;
    }
    
}
