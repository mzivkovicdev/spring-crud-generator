package com.markozivkovic.codegen.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.markozivkovic.codegen.constants.TransactionConstants;
import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;

public class TemplateContextUtils {

    private static final String ENUM_NAME = "enumName";
    private static final String VALUES = "values";
    private static final String FIELDS = "fields";
    private static final String FIELD_NAMES = "fieldNames";
    private static final String JAVADOC_FIELDS = "javadocFields";
    private static final String FIELD_NAMES_WITHOUT_ID = "fieldNamesWithoutId";
    private static final String INPUT_FIELDS = "inputFields";
    private static final String MODEL_NAME = "modelName";
    private static final String ID_TYPE = "idType";
    private static final String ID_DESCRIPTION = "idDescription";
    private static final String GENERATE_JAVA_DOC = "generateJavaDoc";
    private static final String TRANSACTIONAL_ANNOTATION = "transactionalAnnotation";
    private static final String INPUT_ARGS = "inputArgs";
    private static final String CLASS_NAME = "className";
    private static final String NON_ID_FIELD_NAMES = "nonIdFieldNames";

    private TemplateContextUtils() {
        
    }

    /**
     * Creates a template context for the enum class of a model.
     * 
     * @param enumName the name of the enum
     * @param enumValues the values of the enum
     * @return a template context for the enum class
     */
    public static Map<String, Object> createEnumContext(final String enumName, final List<String> enumValues) {

        final Map<String, Object> context = new HashMap<>();
        context.put(ENUM_NAME, enumName);
        context.put(VALUES, enumValues);
        return context;
    }

    /**
     * Creates a template context for a service class of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a template context for the service class
     */
    public static Map<String, Object> createServiceClassContext(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = new HashMap<>();
        context.put(CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Service");
        context.put(MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));

        return context;
    }

    public static Map<String, Object> createAddRelationMethodContext(final ModelDefinition modelDefinition) {

        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());

        final Map<String, Object> model = new HashMap<>();
        final List<Map<String, Object>> relations = new ArrayList<>();

        model.put(MODEL_NAME, modelDefinition.getName());
        model.put(TRANSACTIONAL_ANNOTATION, TransactionConstants.TRANSACTIONAL_ANNOTATION);
        model.put(ID_TYPE, idField.getType());
        
        relationFields.forEach(field -> {
            final Map<String, Object> relation = new HashMap<>();
            
            final String strippedFieldName = StringUtils.capitalize(ModelNameUtils.stripSuffix(field.getName()));
            relation.put("relationClassName", field.getType());
            relation.put("elementParam", field.getName());
            relation.put("relationField", strippedFieldName);
            relation.put("isCollection", manyToManyFields.contains(field) || oneToManyFields.contains(field));
            relation.put("methodName", String.format("add%s", strippedFieldName));
            
            relations.add(relation);
        });

        return Map.of(
            "model", model,
            "relations", relations
        );
    }

    public static Map<String, Object> createRemoveRelationMethodContext(final ModelDefinition modelDefinition) {

        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());

        final Map<String, Object> model = new HashMap<>();
        final List<Map<String, Object>> relations = new ArrayList<>();

        model.put(MODEL_NAME, modelDefinition.getName());
        model.put(TRANSACTIONAL_ANNOTATION, TransactionConstants.TRANSACTIONAL_ANNOTATION);
        model.put(ID_TYPE, idField.getType());
        
        relationFields.forEach(field -> {
            final Map<String, Object> relation = new HashMap<>();
            
            final String strippedFieldName = StringUtils.capitalize(ModelNameUtils.stripSuffix(field.getName()));
            relation.put("relationClassName", field.getType());
            relation.put("elementParam", field.getName());
            relation.put("relationField", strippedFieldName);
            relation.put("isCollection", manyToManyFields.contains(field) || oneToManyFields.contains(field));
            relation.put("methodName", String.format("remove%s", strippedFieldName));
            
            relations.add(relation);
        });

        return Map.of(
            "model", model,
            "relations", relations
        );
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
        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(ID_TYPE, idField.getType());
        context.put(ID_DESCRIPTION, idField.getDescription());
        context.put(GENERATE_JAVA_DOC, StringUtils.isNotBlank(idField.getDescription()));
        
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

        final Map<String, Object> context = new HashMap<>();
        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(TRANSACTIONAL_ANNOTATION, TransactionConstants.TRANSACTIONAL_ANNOTATION);
        context.put(INPUT_ARGS, String.join(", ", inputFields));
        context.put(FIELD_NAMES, String.join(", ", fieldNames));
        context.put(JAVADOC_FIELDS, javadocFields);

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
        
        final Map<String, Object> context = computeGetByIdContext(modelDefinition);
        context.put(TRANSACTIONAL_ANNOTATION, TransactionConstants.TRANSACTIONAL_ANNOTATION);
        
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

        final Map<String, Object> context = new HashMap<>();
        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(INPUT_FIELDS, FieldUtils.generateInputArgsWithoutRelations(modelDefinition.getFields()));
        context.put(FIELD_NAMES_WITHOUT_ID, FieldUtils.extractNonIdNonRelationFieldNames(modelDefinition.getFields()));
        context.put(JAVADOC_FIELDS, FieldUtils.extractFieldForJavadocWithoutRelations(modelDefinition.getFields()));
        context.put(TRANSACTIONAL_ANNOTATION, TransactionConstants.TRANSACTIONAL_ANNOTATION);

        return context;
    }

    /**
     * Creates a template context for the getAll method of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the getAll method
     */
    public static Map<String, Object> computeGetAllContext(final ModelDefinition modelDefinition) {
    
        final Map<String, Object> context = new HashMap<>();
        context.put(MODEL_NAME, modelDefinition.getName());
        
        return context;
    }

    /**
     * Creates a template context for the JPA interface of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the JPA interface
     */
    public static Map<String, Object> computeJpaInterfaceContext(final ModelDefinition modelDefinition) {
    
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Repository");
        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(ID_TYPE, idField.getType());
        
        return context;
    }

    /**
     * Creates a template context for the JPA model of a given model definition.
     * 
     * @param modelDefinition the model definition containing class and field details
     * @return a map representing the context for the JPA model
     */
    public static Map<String, Object> computeJpaModelContext(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(FIELDS, modelDefinition.getFields());
        context.put(FIELD_NAMES, FieldUtils.extractFieldNames(modelDefinition.getFields()));
        context.put(CLASS_NAME, modelDefinition.getName());
        context.put(INPUT_ARGS, FieldUtils.generateInputArgsExcludingId(modelDefinition.getFields()));
        context.put(NON_ID_FIELD_NAMES, FieldUtils.extractNonIdFieldNames(modelDefinition.getFields()));

        return context;  
    }

    /**
     * Creates a template context for a transfer object of a model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @return a template context for the transfer object
     */
    public static Map<String, Object> computeTransferObjectContext(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(INPUT_ARGS, FieldUtils.generateInputArgsWithoutFinal(modelDefinition.getFields()));
    
        return context;
    }
    
}
