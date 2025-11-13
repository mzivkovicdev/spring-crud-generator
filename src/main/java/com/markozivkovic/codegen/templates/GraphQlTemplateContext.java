package com.markozivkovic.codegen.templates;

import java.util.ArrayList;
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
import com.markozivkovic.codegen.utils.StringUtils;

public class GraphQlTemplateContext {
    
    /**
     * Computes a template context for a GraphQL resolver class of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the GraphQL resolver class with stripped model name and class name
     */
    public static Map<String, Object> computeGraphQlResolver(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.CLASS_NAME, String.format("%sResolver", strippedModelName));
        context.put(TemplateContextConstants.JSON_FIELDS, jsonFields);
        context.put(TemplateContextConstants.RELATIONS, !FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty());
        
        return context;
    }

    /**
     * Computes a template context for a GraphQL schema class of a model.
     * 
     * The generated context contains the stripped model name, a list of fields with their relation types replaced
     * by their stripped relation types, and a list of model definitions representing entities related to the model.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a template context for the GraphQL schema class
     */
    public static Map<String, Object> computeGraphQlSchemaContext(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        final List<FieldDefinition> fields = modelDefinition.getFields().stream()
            .map(field -> {
                if (Objects.nonNull(field.getRelation())) {
                    return FieldUtils.cloneFieldDefinition(field)
                        .setType(ModelNameUtils.stripSuffix(field.getType()));
                }
                return field;
            }).collect(Collectors.toList());
            
        final List<String> jsonFieldNames = FieldUtils.extractJsonFields(fields).stream()
                .map(jsonField -> FieldUtils.extractJsonFieldName(jsonField))
                .collect(Collectors.toList());
        final List<ModelDefinition> jsonModels = entities.stream()
                .filter(model -> jsonFieldNames.contains(model.getName()))
                .collect(Collectors.toList());

        return Map.of(
            TemplateContextConstants.NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()),
            TemplateContextConstants.FIELDS, fields,
            TemplateContextConstants.JSON_MODELS, jsonModels
        );
    }

    /**
     * Computes a GraphQL mutation mapping template context for a model definition.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a GraphQL mutation mapping template context with model name, stripped model name, and ID type
     */
    public static Map<String, Object> computeMutationMappingGraphQL(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.INPUT_FIELDS_WITHOUT_RELATIONS, FieldUtils.extractNonIdNonRelationFieldNamesForResolver(modelDefinition.getFields()));
        context.put(TemplateContextConstants.INPUT_FIELDS_WITH_RELATIONS, FieldUtils.extractNonIdFieldNamesForResolver(modelDefinition.getFields()));
        final List<Map<String, Object>> relations = new ArrayList<>();

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        relationFields.forEach(relation -> {

            final ModelDefinition relationEntity = entities.stream()
                    .filter(entity -> entity.getName().equals(relation.getType()))
                    .findFirst()
                    .orElseThrow();
            
            final Map<String, Object> relationContext = new HashMap<>();
            final String strippedFieldName = StringUtils.capitalize(ModelNameUtils.stripSuffix(relation.getName()));
            relationContext.put(TemplateContextConstants.RELATION_FIELD, strippedFieldName);
            relationContext.put(TemplateContextConstants.IS_COLLECTION, manyToManyFields.contains(relation) || oneToManyFields.contains(relation));
            relationContext.put(TemplateContextConstants.RELATION_ID_TYPE, FieldUtils.extractIdField(relationEntity.getFields()).getType());

            relations.add(relationContext);
        });

        context.put(TemplateContextConstants.RELATIONS, relations);

        return context;
    }

    /**
     * Computes a GraphQL query mapping template context for a model definition.
     * 
     * @param modelDefinition the model definition
     * @return a GraphQL query mapping template context with model name, stripped model name, and ID type
     */
    public static Map<String, Object> computeQueryMappingGraphQL(final ModelDefinition modelDefinition) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }
}
