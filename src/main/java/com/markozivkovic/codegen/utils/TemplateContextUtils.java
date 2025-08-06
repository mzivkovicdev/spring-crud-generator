package com.markozivkovic.codegen.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.markozivkovic.codegen.constants.TransactionConstants;
import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;

public class TemplateContextUtils {

    private static final String METHOD_NAME = "methodName";
    private static final String IS_COLLECTION = "isCollection";
    private static final String RELATION_FIELD = "relationField";
    private static final String ELEMENT_PARAM = "elementParam";
    private static final String RELATION_CLASS_NAME = "relationClassName";
    private static final String RELATIONS = "relations";
    private static final String MODEL = "model";
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
    private static final String STRIPPED_MODEL_NAME = "strippedModelName";

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
        
        relation.put(RELATION_CLASS_NAME, field.getType());
        relation.put(ELEMENT_PARAM, field.getName());
        relation.put(RELATION_FIELD, strippedFieldName);
        relation.put(IS_COLLECTION, manyToManyFields.contains(field) || oneToManyFields.contains(field));
        relation.put(JAVADOC_FIELDS, FieldUtils.computeJavadocForFields(idField, field));
        relation.put(METHOD_NAME, methodName);

        return relation;
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

        model.put(MODEL_NAME, modelDefinition.getName());
        model.put(TRANSACTIONAL_ANNOTATION, TransactionConstants.TRANSACTIONAL_ANNOTATION);
        model.put(ID_TYPE, idField.getType());
        
        relationFields.forEach(field -> 
            relations.add(computeRelationContext(field, idField, manyToManyFields, oneToManyFields, isAddRelation))
        );

        return Map.of(
                MODEL, model,
                RELATIONS, relations
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

    /**
     * Creates a template context for a controller class of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a template context for the controller class
     */
    public static Map<String, Object> computeControllerClassContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final Map<String, Object> context = new HashMap<>();
        context.put(CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Controller");
        context.put(STRIPPED_MODEL_NAME, strippedModelName);

        return context;
    }

    /**
     * Computes a template context for a create endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the create endpoint
     */
    public static Map<String, Object> computeCreateEndpointContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final Map<String, Object> context = new HashMap<>();

        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(STRIPPED_MODEL_NAME, strippedModelName);
        context.put(INPUT_FIELDS, FieldUtils.extractNonIdFieldNames(modelDefinition.getFields()));

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
        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(STRIPPED_MODEL_NAME, strippedModelName);
        context.put(ID_TYPE, idField.getType());

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
        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(STRIPPED_MODEL_NAME, strippedModelName);

        return context;
    }

    /**
     * Computes a template context for an update endpoint of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a template context for the update endpoint
     */
    public static Map<String, Object> computeUpdateEndpointContext(final ModelDefinition modelDefinition) {
        
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();

        context.put(MODEL_NAME, modelDefinition.getName());
        context.put(STRIPPED_MODEL_NAME, strippedModelName);
        context.put(INPUT_FIELDS, FieldUtils.extractNonIdFieldNames(modelDefinition.getFields()));
        context.put(ID_TYPE, idField.getType());

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

        context.put(MODEL_NAME, strippedModelName);
        context.put(ID_TYPE, idField.getType());

        return context;
    }
    
}
