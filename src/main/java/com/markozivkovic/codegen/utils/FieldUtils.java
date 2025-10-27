package com.markozivkovic.codegen.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.RelationDefinition;

public class FieldUtils {

    private static final String BIG_INTEGER = "BigInteger";
    private static final String BIG_DECIMAL = "BigDecimal";
    private static final String UUID = "UUID";
    private static final String LOCAL_DATE = "LocalDate";
    private static final String LOCAL_DATE_TIME = "LocalDateTime";
    private static final String ENUM = "Enum";
    private static final String ONE_TO_ONE = "OneToOne";
    private static final String ONE_TO_MANY = "OneToMany";
    private static final String MANY_TO_ONE = "ManyToOne";
    private static final String MANY_TO_MANY = "ManyToMany";

    private static final Pattern pattern = Pattern.compile("^JSONB?\\[(.+)]$");
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);;

    private FieldUtils() {
        
    }

    /**
     * Determines if any of the given fields have a relation with a non-null cascade type defined.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a relation with a non-null cascade type, false otherwise.
     */
    public static boolean isCascadeTypeDefined(final List<FieldDefinition> fields) {
        
        return fields.stream()
                    .filter(field -> Objects.nonNull(field.getRelation()))
                    .anyMatch(field -> Objects.nonNull(field.getRelation().getCascade()));
    }

    /**
     * Determines if any of the given fields have a relation with a non-null fetch type defined.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a relation with a non-null fetch type, false otherwise.
     */
    public static boolean isFetchTypeDefined(final List<FieldDefinition> fields) {

        return fields.stream()
                    .filter(field -> Objects.nonNull(field.getRelation()))
                    .anyMatch(field -> Objects.nonNull(field.getRelation().getFetch()));
    }

    /**
     * Determines if any of the given fields have a relation of type {@link RelationDefinition#ONE_TO_ONE}.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a one-to-one relation, false otherwise.
     */
    public static boolean isAnyRelationOneToOne(final List<FieldDefinition> fields) {

        final List<String> relations = extractRelationTypes(fields);

        return relations.stream().anyMatch(relation -> ONE_TO_ONE.equalsIgnoreCase(relation));
    }

    /**
     * Determines if any of the given fields have a relation of type {@link RelationDefinition#ONE_TO_MANY}.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a one-to-many relation, false otherwise.
     */
    public static boolean isAnyRelationOneToMany(final List<FieldDefinition> fields) {

        final List<String> relations = extractRelationTypes(fields);

        return relations.stream().anyMatch(relation -> ONE_TO_MANY.equalsIgnoreCase(relation));
    }

    /**
     * Determines if any of the given fields have a relation of type {@link RelationDefinition#MANY_TO_ONE}.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a many-to-one relation, false otherwise.
     */
    public static boolean isAnyRelationManyToOne(final List<FieldDefinition> fields) {

        final List<String> relations = extractRelationTypes(fields);

        return relations.stream().anyMatch(relation -> MANY_TO_ONE.equalsIgnoreCase(relation));
    }

    /**
     * Determines if any of the given fields have a relation of type {@link RelationDefinition#MANY_TO_MANY}.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a many-to-many relation, false otherwise.
     */
    public static boolean isAnyRelationManyToMany(final List<FieldDefinition> fields) {

        final List<String> relations = extractRelationTypes(fields);

        return relations.stream().anyMatch(relation -> MANY_TO_MANY.equalsIgnoreCase(relation));
    }

    /**
     * Extracts the types of all relations from the given list of fields.
     *
     * @param fields The list of fields to extract relations from.
     * @return A list of types of relations in the given list of fields.
     */
    public static List<String> extractRelationTypes(final List<FieldDefinition> fields) {

        return fields.stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .map(FieldDefinition::getRelation)
                .map(RelationDefinition::getType)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that have a many-to-many relation.
     *
     * @param fields The list of fields to extract many-to-many relation names from.
     * @return A list of names of fields that have a many-to-many relation.
     */
    public static List<FieldDefinition> extractManyToManyRelations(final List<FieldDefinition> fields) {
        
        return extractRelationsByType(fields, MANY_TO_MANY);
    }

    /**
     * Extracts the names of all fields from the given list that have a one-to-many relation.
     *
     * @param fields The list of fields to extract one-to-many relation names from.
     * @return A list of names of fields that have a one-to-many relation.
     */
    public static List<FieldDefinition> extractOneToManyRelations(final List<FieldDefinition> fields) {
        
        return extractRelationsByType(fields, ONE_TO_MANY);
    }

    /**
     * Extracts all fields from the given list that have a relation of type {@code type}.
     *
     * @param fields The list of fields to extract relations from.
     * @param type The type of relation to extract.
     * @return A list of fields that have a relation of type {@code type}.
     */
    private static List<FieldDefinition> extractRelationsByType(final List<FieldDefinition> fields, final String type) {
        
        return fields.stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .filter(field -> type.equalsIgnoreCase(field.getRelation().getType()))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that have a many-to-many relation.
     *
     * @param fields The list of fields to extract many-to-many relation names from.
     * @return A list of names of fields that have a many-to-many relation.
     */
    public static List<FieldDefinition> extractRelationFields(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .collect(Collectors.toList());
    }

    /**
     * Returns true if any field in the given list of fields is of type Enum,
     * false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is of type Enum, false otherwise.
     */
    public static boolean isAnyFieldEnum(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(field -> ENUM.equalsIgnoreCase(field.getType()));
    }

    /**
     * Returns true if the given field is of type Enum, false otherwise.
     * 
     * @param field The field to check.
     * @return True if the field is of type Enum, false otherwise.
     */
    public static boolean isFieldEnum(final FieldDefinition field) {

        return ENUM.equalsIgnoreCase(field.getType());
    }

    /**
     * Returns true if any field in the given list of fields is of type JSON,
     * false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is of type JSON, false otherwise.
     */
    public static boolean isAnyFieldJson(final List<FieldDefinition> fields) {
        
        return fields.stream().anyMatch(field -> isJsonField(field));
    }

    /**
     * Extracts all fields from the given list of fields that are of type Enum.
     * 
     * @param fields The list of fields to extract Enum fields from.
     * @return A list of fields that are of type Enum.
     */
    public static List<FieldDefinition> extractEnumFields(final List<FieldDefinition> fields) {

        return fields.stream()
                .filter(field -> ENUM.equalsIgnoreCase(field.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of names of all fields from the given list of fields that are of type Enum.
     * 
     * @param fields The list of fields to extract Enum field names from.
     * @return A list of names of fields that are of type Enum.
     */
    public static List<String> extractNamesOfEnumFields(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .filter(field -> ENUM.equalsIgnoreCase(field.getType()))
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extracts all fields from the given list of fields that are of type JSON.
     * 
     * @param fields The list of fields to extract JSON fields from.
     * @return A list of fields that are of type JSON.
     */
    public static List<FieldDefinition> extractJsonFields(final List<FieldDefinition> fields) {

        return fields.stream()                
                .filter(field -> isJsonField(field))
                .collect(Collectors.toList());
    }

    /**
     * Checks if the given model definition is used as a JSON field in any of the other model definitions.
     * 
     * @param modelDefinition The model definition to check.
     * @param entities The list of all model definitions.
     * @return True if the given model definition is used as a JSON field in any of the other model definitions, false otherwise.
     */
    public static boolean isModelUsedAsJsonField(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        return entities.stream()
                .flatMap(entity -> entity.getFields().stream())
                .filter(field -> isJsonField(field))
                .map(field -> extractJsonFieldName(field))
                .anyMatch(fieldName -> fieldName.equals(modelDefinition.getName()));
    }

    /**
     * Returns true if any field in the given list of fields is of type UUID,
     * false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is of type UUID, false otherwise.
     */
    public static boolean isAnyFieldUUID(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(field -> UUID.equalsIgnoreCase(field.getType()));
    }

    /**
     * Returns true if any field in the given list of fields is of type LocalDate,
     * false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is of type LocalDate, false otherwise.
     */
    public static boolean isAnyFieldLocalDate(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(field -> LOCAL_DATE.equalsIgnoreCase(field.getType()));
    }

    /**
     * Returns true if any field in the given list of fields is of type LocalDateTime,
     * false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is of type LocalDateTime, false otherwise.
     */
    public static boolean isAnyFieldLocalDateTime(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(field -> LOCAL_DATE_TIME.equalsIgnoreCase(field.getType()));
    }

    /**
     * Returns true if any field in the given list of fields is of type BigDecimal,
     * false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is of type BigDecimal, false otherwise.
     */
    public static boolean isAnyFieldBigDecimal(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(field -> BIG_DECIMAL.equalsIgnoreCase(field.getType()));
    }

    /**
     * Returns true if any field in the given list of fields is of type BigInteger,
     * false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is of type BigInteger, false otherwise.
     */
    public static boolean isAnyFieldBigInteger(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(field -> BIG_INTEGER.equalsIgnoreCase(field.getType()));
    }

    /**
     * Extracts the ID field from the given list of fields. If no ID field is found,
     * an exception is thrown. The ID field is the first field in the list that is
     * marked as an ID field.
     * 
     * @param fields The list of fields to extract the ID field from.
     * @return The ID field.
     * @throws IllegalArgumentException If no ID field is found in the provided fields.
     */
    public static FieldDefinition extractIdField(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .filter(field -> Objects.nonNull(field.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No ID field found in the provided fields."));
    }

    /**
     * Extracts the names of all fields from the given list that are not marked as ID fields.
     * 
     * @param fields The list of fields to extract non-ID field names from.
     * @return A list of names of fields that are not marked as ID fields.
     * @throws IllegalArgumentException If no ID field is found in the provided fields.
     */
    public static List<String> extractNonIdFieldNames(final List<FieldDefinition> fields) {

        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that are not marked as ID fields
     * and do not have a relation.
     * 
     * @param fields The list of fields to extract non-ID non-relation field names from.
     * @return A list of names of fields that are not marked as ID fields and do not have a
     *         relation.
     */
    public static List<String> extractNonIdNonRelationFieldNames(final List<FieldDefinition> fields) {
        
        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .filter(field -> Objects.isNull(field.getRelation()))
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that do not have a relation, do not have an enum type
     * and do not have a JSON field.
     * 
     * @param fields The list of fields to extract non-ID non-relation field names from.
     * @return A list of names of fields that are not marked as ID fields and do not have a
     *         relation, and do not have an enum type.
     */
    public static List<String> extractNonRelationNonEnumAndNonJsonFieldNames(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .filter(field -> Objects.isNull(field.getRelation()))
                .filter(field -> !isFieldEnum(field))
                .filter(field -> !isJsonField(field))
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that are not marked as ID fields
     * and do not have a relation, and formats them as strings in the format expected by the
     * controller layer.
     * 
     * @param fields The list of fields to extract non-ID non-relation field names from.
     * @param swagger True if the generated code is for Swagger, false otherwise.
     * @return A list of strings representing the non-ID non-relation fields in the format
     *         expected by the controller layer.
     */
    public static List<String> extractNonIdNonRelationFieldNamesForController(final List<FieldDefinition> fields, final boolean swagger) {
        
        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .filter(field -> Objects.isNull(field.getRelation()))
                .map(field -> {
                    if (isJsonField(field)) {
                        return String.format(
                            !swagger ? "%sMapper.map%sTOTo%s(body.%s())" : "%sMapper.map%sTo%s(body.get%s())",
                            StringUtils.uncapitalize(field.getResolvedType()),
                            StringUtils.capitalize(field.getResolvedType()),
                            StringUtils.capitalize(field.getResolvedType()),
                            !swagger ? StringUtils.uncapitalize(field.getResolvedType()) : StringUtils.capitalize(field.getResolvedType())
                        );
                    }

                    if (isFieldEnum(field) && swagger) {
                        return  String.format(
                            "body.get%s() != null ? %s.valueOf(body.get%s().name()) : null",
                            StringUtils.capitalize(field.getName()),
                            StringUtils.capitalize(field.getResolvedType()),
                            StringUtils.capitalize(field.getName())
                        );
                    }

                    return !swagger ? String.format("body.%s()", field.getName()) :
                        String.format("body.get%s()", StringUtils.capitalize(field.getName()));
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that are not marked as ID fields
     * and do not have a relation, and formats them as strings in the format expected by the
     * resolver layer.
     * 
     * @param fields The list of fields to extract non-ID non-relation field names from.
     * @return A list of strings representing the non-ID non-relation fields in the format
     *         expected by the resolver layer.
     */
    public static List<String> extractNonIdNonRelationFieldNamesForResolver(final List<FieldDefinition> fields) {

        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .filter(field -> Objects.isNull(field.getRelation()))
                .map(field -> {
                    if (isJsonField(field)) {
                        return String.format(
                            "%sMapper.map%sTOTo%s(input.%s())",
                            StringUtils.uncapitalize(field.getResolvedType()),
                            StringUtils.capitalize(field.getResolvedType()),
                            StringUtils.capitalize(field.getResolvedType()),
                            StringUtils.uncapitalize(field.getResolvedType())
                        );
                    }

                    return String.format("input.%s()", field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that are not marked as ID fields
     * and formats them as strings in the format expected by the resolver layer.
     * 
     * @param fields The list of fields to extract non-ID field names from.
     * @return A list of strings representing the non-ID fields in the format expected by the
     *         resolver layer.
     */
    public static List<String> extractNonIdFieldNamesForResolver(final List<FieldDefinition> fields) {

        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .map(field -> {
                    if (isJsonField(field)) {
                        return String.format(
                            "%sMapper.map%sTOTo%s(input.%s())",
                            StringUtils.uncapitalize(field.getResolvedType()),
                            StringUtils.capitalize(field.getResolvedType()),
                            StringUtils.capitalize(field.getResolvedType()),
                            StringUtils.uncapitalize(field.getResolvedType())
                        );
                    }

                    if (Objects.nonNull(field.getRelation())) {
                        final String inputArg;
                        if (Objects.equals(field.getRelation().getType(), ONE_TO_MANY) || Objects.equals(field.getRelation().getType(), MANY_TO_MANY)) {
                            inputArg = String.format("input.%sIds()", field.getName());
                        } else {
                            inputArg = String.format("input.%sId()", field.getName());
                        }

                        return inputArg;
                    }

                    return String.format("input.%s()", field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list that are not marked as ID fields, and
     * formats them as Javadoc @param tags.
     * 
     * @param fields The list of fields to extract non-ID field names from.
     * @return A list of Javadoc @param tags for fields that are not marked as ID fields.
     * @throws IllegalArgumentException If no ID field is found in the provided fields.
     */
    public static List<String> extractNonIdFieldForJavadoc(final List<FieldDefinition> fields) {

        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .filter(field -> StringUtils.isNotBlank(field.getDescription()))
                .map(field -> String.format("@param %s %s", field.getName(), field.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names and descriptions of all fields from the given list that have a description, and
     * formats them as Javadoc @param tags.
     * 
     * @param fields The list of fields to extract names and descriptions from.
     * @return A list of Javadoc @param tags for fields that have a description.
     */
    public static List<String> extractFieldForJavadocWithoutRelations(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .filter(field -> StringUtils.isNotBlank(field.getDescription()))
                .filter(field -> Objects.isNull(field.getRelation()))
                .map(field -> String.format("@param %s %s", field.getName(), field.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Computes a list of Javadoc @param tags from the given array of FieldDefinition objects.
     * 
     * @param field The array of FieldDefinition objects to compute the Javadoc @param tags from.
     * @return A list of Javadoc @param tags from the given array of FieldDefinition objects.
     */
    public static List<String> computeJavadocForFields(final FieldDefinition... field) {

        return Arrays.stream(field)
                .filter(fieldDefinition -> StringUtils.isNotBlank(fieldDefinition.getDescription()))
                .map(fieldDefinition -> String.format("@param %s %s", fieldDefinition.getName(), fieldDefinition.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list.
     * 
     * @param fields The list of fields to extract names from.
     * @return A list of names of the fields.
     */
    public static List<String> extractFieldNames(final List<FieldDefinition> fields) {

        return fields.stream()
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given list.
     * 
     * @param fields The list of fields to extract names from.
     * @return A list of names of the fields.
     */
    public static List<String> extractFieldNamesWithoutRelations(final List<FieldDefinition> fields) {

        return fields.stream()
                .map(field -> {
                    if (field.getRelation() != null) {
                        if (field.getRelation().getType().equalsIgnoreCase(ONE_TO_MANY) ||
                                field.getRelation().getType().equalsIgnoreCase(MANY_TO_MANY)) {
                            return null;
                        }
                    }

                    return field.getName();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the names of all fields from the given model definition that have a one-to-many or
     * many-to-many relation.
     *
     * @param modelDefinition The model definition to extract the names of fields with a one-to-many or
     *                  many-to-many relation from.
     * @return A list of names of fields from the given model definition that have a one-to-many or
     *          many-to-many relation.
     */
    public static List<String> extractCollectionRelationNames(final ModelDefinition modelDefinition) {
        
        return modelDefinition.getFields().stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .filter(field -> field.getRelation().getType().equalsIgnoreCase(ONE_TO_MANY) || field.getRelation().getType().equalsIgnoreCase(MANY_TO_MANY))
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * Checks if any field within the provided list of model definitions has a one-to-many or
     * many-to-many relation to the specified model definition.
     *
     * @param modelDefinition The model definition to check for a relation.
     * @param entities The list of model definitions to search for a relation.
     * @return true if a relation to the given model definition is found, false otherwise.
     */
    public static boolean hasCollectionRelation(final ModelDefinition modelDefinition, final List<ModelDefinition> entites) {

        return entites.stream()
                .flatMap(entity -> entity.getFields().stream())
                .filter(field -> Objects.nonNull(field.getRelation()))
                .filter(field -> field.getRelation().getType().equalsIgnoreCase(ONE_TO_MANY) || field.getRelation().getType().equalsIgnoreCase(MANY_TO_MANY))
                .anyMatch(field -> field.getType().equals(modelDefinition.getName()));
    }

    /**
     * Checks if any field within the provided list of model definitions has a relation
     * to the specified model definition.
     * 
     * @param modelDefinition The model definition to check for a relation.
     * @param entities The list of model definitions to search for a relation.
     * @return true if a relation to the given model definition is found, false otherwise.
     */
    public static boolean hasRelation(final ModelDefinition modelDefinition, final List<ModelDefinition> entites) {

        return entites.stream()
                .flatMap(entity -> entity.getFields().stream())
                .filter(field -> Objects.nonNull(field.getRelation()))
                .anyMatch(field -> field.getType().equals(modelDefinition.getName()));
    }

    /**
     * Generates a list of strings representing the input arguments for a business service method.
     * The generated list of strings is in the format "<name>" where <name> is the name of the field.
     * If the field has a relation, the name is the camel-cased version of the type of the related
     * entity, and if the relation is many-to-many or one-to-many, the name is in the plural form.
     * The generated list does not include the ID field.
     * 
     * @param fields The list of fields to generate the input arguments from.
     * @return A list of strings representing the input arguments for a business service method.
     */
    public static List<String> generateInputArgsBusinessService(final List<FieldDefinition> fields) {

        final FieldDefinition id = extractIdField(fields);

        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .map(field -> {
                    if (Objects.nonNull(field.getRelation())) {
                        final String name = StringUtils.uncapitalize(
                                field.getType()
                        );
                        final String arg;
                        if (field.getRelation().getType().equals(MANY_TO_MANY) || field.getRelation().getType().equals(ONE_TO_MANY)) {
                            arg = String.format("%ss", name);
                        } else {
                            arg = String.format("%s", name);
                        }
                        return arg;
                    }
                    return String.format("%s", field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates a list of strings representing the input arguments for a constructor or method,
     * excluding the ID field. The generated list of strings is in the format "final <type> <name>"
     * where <type> is the type of the field and <name> is the name of the field. If the field
     * has a relation, the name is formatted as "<modelName>Id" for a one-to-one or many-to-one
     * relation, and as "<modelName>Ids" for a many-to-many or one-to-many relation. The ID field
     * is excluded from the list.
     *
     * @param fields The list of fields to generate the input arguments from.
     * @param entities The list of model definitions to resolve related model types.
     * @return A list of strings representing the input arguments for a constructor or method,
     * excluding the ID field.
     * @throws IllegalArgumentException If no ID field is found in the provided fields.
     */
    public static List<String> generateInputArgsExcludingId(final List<FieldDefinition> fields, final List<ModelDefinition> entities) {

        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .map(field -> {
                    if (Objects.nonNull(field.getRelation())) {
                        final ModelDefinition modelDefinition = entities.stream()
                                .filter(model -> model.getName().equals(field.getType()))
                                .findFirst()
                                .orElseThrow();

                        final FieldDefinition relationId = extractIdField(modelDefinition.getFields());
                        final String modelName = StringUtils.uncapitalize(
                                ModelNameUtils.stripSuffix(modelDefinition.getName())
                        );
                        if (field.getRelation().getType().equals(MANY_TO_MANY) || field.getRelation().getType().equals(ONE_TO_MANY)) {
                            return String.format("final List<%s> %s", relationId.getType(), String.format("%sIds", modelName));
                        } else {
                            return String.format("final %s %s", relationId.getType(), String.format("%sId", modelName));
                        }
                    }
                    return String.format("final %s %s", field.getResolvedType(), field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates a list of strings representing the input arguments for a test method, excluding the ID field.
     * The generated list of strings is in the format expected by the test layer.
     * If the field has a relation, the name is formatted as "<modelName>Id" for a one-to-one or many-to-one
     * relation, and as "<modelName>Ids" for a many-to-many or one-to-many relation.
     * 
     * @param fields The list of fields to generate the input arguments from.
     * @param entities The list of model definitions to resolve related model types.
     * @return A list of strings representing the input arguments for a test method, excluding the ID field.
     * @throws IllegalArgumentException If no ID field is found in the provided fields.
     */
    public static List<String> generateInputArgsExcludingIdForTest(final List<FieldDefinition> fields, final List<ModelDefinition> entities) {

        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .map(field -> {
                    if (Objects.nonNull(field.getRelation())) {
                        final ModelDefinition modelDefinition = entities.stream()
                                .filter(model -> model.getName().equals(field.getType()))
                                .findFirst()
                                .orElseThrow();

                        final String modelName = StringUtils.uncapitalize(
                                ModelNameUtils.stripSuffix(modelDefinition.getName())
                        );
                        if (field.getRelation().getType().equals(MANY_TO_MANY) || field.getRelation().getType().equals(ONE_TO_MANY)) {
                            return String.format("%s", String.format("%sIds", modelName));
                        } else {
                            return String.format("%s", String.format("%sId", modelName));
                        }
                    }
                    return String.format("%s", field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates a list of strings representing the input arguments for a constructor or method, excluding the ID field.
     * The generated list of strings is in the format "final <type> <name>" where <type> is the type of the field and
     * <name> is the name of the field. The ID field is excluded from the list.
     *
     * @param fields The list of fields to generate the input arguments from.
     * @return A list of strings representing the input arguments for a constructor or method, excluding the ID field.
     */
    public static List<String> generateInputArgsExcludingId(final List<FieldDefinition> fields) {

        final FieldDefinition id = extractIdField(fields);
        
        return fields.stream()
                .filter(field -> !field.getName().equals(id.getName()))
                .map(field -> {
                    if (Objects.nonNull(field.getRelation()) &&
                        (Objects.equals(field.getRelation().getType(), ONE_TO_MANY) || 
                        Objects.equals(field.getRelation().getType(), MANY_TO_MANY)
                    )) {
                        return String.format("final List<%s> %s", field.getResolvedType(), field.getName());
                    }
                    return String.format("final %s %s", field.getResolvedType(), field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates a list of strings representing the input arguments for a constructor or method.
     * The generated list of strings is in the format "final <type> <name>" where <type> is the type of the field and
     * <name> is the name of the field.
     *
     * @param fields The list of fields to generate the input arguments from.
     * @return A list of strings representing the input arguments for a constructor or method.
     */
    public static List<String> generateInputArgsWithoutRelations(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .filter(field -> Objects.isNull(field.getRelation()))
                .map(field -> String.format("final %s %s", field.getResolvedType(), field.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Generates a list of strings representing the input arguments for a constructor or method without the final keyword.
     * The generated list of strings is in the format "<type> <name>" where <type> is the type of the field and
     * <name> is the name of the field.
     *
     * @param fields The list of fields to generate the input arguments from.
     * @return A list of strings representing the input arguments for a constructor or method without the final keyword.
     */
    public static List<String> generateInputArgsWithoutFinalCreateInputTO(final List<FieldDefinition> fields, final List<ModelDefinition> entities) {
        
        return fields.stream()
                .map(field -> {

                    if (Objects.nonNull(field.getRelation())) {
                        final String inputArg;
                        final ModelDefinition modelDefinition = entities.stream()
                                .filter(model -> model.getName().equals(field.getType()))
                                .findFirst()
                                .orElseThrow();
                        final FieldDefinition relationId = extractIdField(modelDefinition.getFields());
                        if (Objects.equals(field.getRelation().getType(), ONE_TO_MANY) || Objects.equals(field.getRelation().getType(), MANY_TO_MANY)) {
                            inputArg = String.format("List<%s> %sIds", relationId.getType(), field.getName());
                        } else {
                            inputArg = String.format("%s %sId", relationId.getType(), field.getName());
                        }
                        return inputArg;
                    }

                    if (isJsonField(field)) {
                        return String.format("%sTO %s", field.getResolvedType(), field.getName());
                    }
                    return String.format("%s %s", field.getResolvedType(), field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates a list of strings representing the input arguments for a constructor or method without the final keyword
     * for the updateInputTO of a model.
     * The generated list of strings is in the format "<type> <name>" where <type> is the type of the field and
     * <name> is the name of the field, for all fields that are not relations.
     *
     * @param fields The list of fields to generate the input arguments from.
     * @return A list of strings representing the input arguments for a constructor or method without the final keyword
     *         for the updateInputTO of a model.
     */
    public static List<String> generateInputArgsWithoutFinalUpdateInputTO(final List<FieldDefinition> fields) {

        return fields.stream()
                .filter(field -> Objects.isNull(field.getRelation()))
                .map(field -> {
                    if (isJsonField(field)) {
                        return String.format("%sTO %s", field.getResolvedType(), field.getName());
                    }
                    return String.format("%s %s", field.getResolvedType(), field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates a list of strings representing the input arguments for a constructor or method without the final keyword.
     * The generated list of strings is in the format "<type> <name>" where <type> is the type of the field and
     * <name> is the name of the field.
     *
     * @param fields The list of fields to generate the input arguments from.
     * @return A list of strings representing the input arguments for a constructor or method without the final keyword.
     */
    public static List<String> generateInputArgsWithoutFinal(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .map(field -> {

                    if (Objects.nonNull(field.getRelation())) {
                        final String inputArg;
                        if (Objects.equals(field.getRelation().getType(), ONE_TO_MANY) || Objects.equals(field.getRelation().getType(), MANY_TO_MANY)) {
                            inputArg = String.format("List<%sTO> %s", ModelNameUtils.stripSuffix(field.getType()), field.getName());
                        } else {
                            inputArg = String.format("%sTO %s", ModelNameUtils.stripSuffix(field.getType()), field.getName());
                        }
                        return inputArg;
                    }

                    if (isJsonField(field)) {
                        return String.format("%sTO %s", field.getResolvedType(), field.getName());
                    }
                    return String.format("%s %s", field.getResolvedType(), field.getName());
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the given field is marked as an ID field and is of type UUID, false otherwise.
     * 
     * @param field The field to check.
     * @return True if the field is marked as an ID field and is of type UUID, false otherwise.
     */
    public static boolean isIdFieldUUID(final FieldDefinition field) {
        
        return UUID.equalsIgnoreCase(field.getType());
    }

    /**
     * Determines whether any of the ID fields of the given model definition or any of its related models is of type UUID.
     * 
     * @param modelDefinition the model definition for which to check the ID field type
     * @param entities the list of model definitions for which to check the ID field type
     * @return true if any of the ID fields of the given model definition or any of its related models is of type UUID, false otherwise
     */
    public static boolean isAnyIdFieldUUID(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {
        
        final List<FieldDefinition> relations = extractRelationFields(modelDefinition.getFields());
        final List<String> relationTypes = extractRelationFields(relations).stream()
                .map(FieldDefinition::getType)
                .collect(Collectors.toList());
        
        return entities.stream()
                .filter(entity -> relationTypes.contains(entity.getName()))
                .map(entity -> entity.getFields())
                .map(entity -> extractIdField(entity))
                .anyMatch(field -> isIdFieldUUID(field));
    }

    /**
     * Returns true if any field in the given list of fields is marked as an ID field, false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is marked as an ID field, false otherwise.
     */
    public static boolean isAnyFieldId(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(field -> Objects.nonNull(field.getId()));
    }

    /**
     * Returns true if the given field is marked as a JSON field, false otherwise.
     * A JSON field is a field whose type is of the form "Json&lt;name&gt;", where
     * &lt;name&gt; is the name of the JSON field.
     * 
     * @param field The field to check.
     * @return True if the field is marked as a JSON field, false otherwise.
     */
    public static boolean isJsonField(final FieldDefinition field) {

        return pattern.matcher(field.getType()).matches();
    }

    /**
     * Extracts the name of a JSON field from the given field definition if it is marked as a JSON field.
     * A JSON field is a field whose type is of the form "Json&lt;name&gt;", where &lt;name&gt; is the name of the JSON field.
     * 
     * @param field The field definition to extract the JSON field name from.
     * @return The name of the JSON field if the field is marked as a JSON field, null otherwise.
     */
    public static String extractJsonFieldName(final FieldDefinition field) {

        final Matcher matcher = pattern.matcher(field.getType());
        matcher.matches();
        return matcher.group(1);
    }
    
    /**
     * Computes the resolved type of the given field definition.
     * If the field is a JSON field, it extracts and returns the JSON field name.
     * If the field is of type Enum and has a name, it returns the capitalized name
     * appended with "Enum". Otherwise, it returns the field's original type.
     *
     * @param fieldDefinition The field definition to compute the resolved type for.
     * @return The resolved type of the field definition.
     */
    public static String computeResolvedType(final FieldDefinition fieldDefinition) {

        if (isJsonField(fieldDefinition)) {
            return extractJsonFieldName(fieldDefinition);
        }
        
        if (ENUM.equalsIgnoreCase(fieldDefinition.getType()) && Objects.nonNull(fieldDefinition.getName())) {
            return StringUtils.capitalize(fieldDefinition.getName()) + ENUM;
        }
        
        return fieldDefinition.getType();
    }

    /**
     * Creates a deep copy of the given field definition.
     * 
     * @param fieldDefinition The field definition to clone.
     * @return A deep copy of the given field definition.
     */
    public static FieldDefinition cloneFieldDefinition(final FieldDefinition fieldDefinition) {
        
        try {
            final String jsonValue = mapper.writeValueAsString(fieldDefinition);
            return mapper.readValue(
                jsonValue, FieldDefinition.class
            );
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to clone FieldDefinition", e);
        }
    }
    
}
