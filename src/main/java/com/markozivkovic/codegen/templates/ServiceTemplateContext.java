package com.markozivkovic.codegen.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.markozivkovic.codegen.constants.AnnotationConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.StringUtils;

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
     * Creates a template context for the getReferenceById method of a model.
     * 
     * @param modelDefinition the model definition containing the field details
     * @return a template context for the getReferenceById method
     */
    public static Map<String, Object> createGetReferenceByIdMethodContext(final ModelDefinition modelDefinition) {

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
     * Creates a template context for the addRelation method of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the addRelation method
     */
    public static Map<String, Object> createAddRelationMethodContext(final ModelDefinition modelDefinition) {

        return computeRelationMethodContext(modelDefinition, true);
    }

    /**
     * Creates a template context for the removeRelation method of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the removeRelation method
     */
    public static Map<String, Object> createRemoveRelationMethodContext(final ModelDefinition modelDefinition) {

        return computeRelationMethodContext(modelDefinition, false);
    }

    /**
     * Computes a template context for a relation method of a model.
     * 
     * @param modelDefinition the model definition
     * @param isAddRelation whether the relation is an add or a remove relation
     * @return a template context for the relation method
     */
    private static Map<String, Object> computeRelationMethodContext(final ModelDefinition modelDefinition, final Boolean isAddRelation) {

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
            relations.add(computeRelationContext(field, idField, manyToManyFields, oneToManyFields, isAddRelation))
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
     * @param field the field definition
     * @param idField the ID field definition
     * @param manyToManyFields the many-to-many fields
     * @param oneToManyFields the one-to-many fields
     * @param isAddRelation whether the relation is an add or a remove relation
     * @return a template context for the relation field
     */
    private static Map<String, Object> computeRelationContext(final FieldDefinition field, final FieldDefinition idField,
            final List<FieldDefinition> manyToManyFields, final List<FieldDefinition> oneToManyFields,
            final Boolean isAddRelation) {

        final Map<String, Object> relation = new HashMap<>();
        final String strippedFieldName = StringUtils.capitalize(ModelNameUtils.stripSuffix(field.getName()));
        final String methodName = isAddRelation ? String.format("add%s", strippedFieldName) :
                String.format("remove%s", strippedFieldName);
        
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
