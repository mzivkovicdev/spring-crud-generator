package com.markozivkovic.codegen.utils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.RelationDefinition;

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

        final List<String> relations = extractRelations(fields);

        return relations.stream().anyMatch(relation -> ONE_TO_ONE.equalsIgnoreCase(relation));
    }

    /**
     * Determines if any of the given fields have a relation of type {@link RelationDefinition#ONE_TO_MANY}.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a one-to-many relation, false otherwise.
     */
    public static boolean isAnyRelationOneToMany(final List<FieldDefinition> fields) {

        final List<String> relations = extractRelations(fields);

        return relations.stream().anyMatch(relation -> ONE_TO_MANY.equalsIgnoreCase(relation));
    }

    /**
     * Determines if any of the given fields have a relation of type {@link RelationDefinition#MANY_TO_ONE}.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a many-to-one relation, false otherwise.
     */
    public static boolean isAnyRelationManyToOne(final List<FieldDefinition> fields) {

        final List<String> relations = extractRelations(fields);

        return relations.stream().anyMatch(relation -> MANY_TO_ONE.equalsIgnoreCase(relation));
    }

    /**
     * Determines if any of the given fields have a relation of type {@link RelationDefinition#MANY_TO_MANY}.
     *
     * @param fields The list of fields to check.
     * @return true if any of the fields have a many-to-many relation, false otherwise.
     */
    public static boolean isAnyRelationManyToMany(final List<FieldDefinition> fields) {

        final List<String> relations = extractRelations(fields);

        return relations.stream().anyMatch(relation -> MANY_TO_MANY.equalsIgnoreCase(relation));
    }

    /**
     * Extracts the types of all relations from the given list of fields.
     *
     * @param fields The list of fields to extract relations from.
     * @return A list of types of relations in the given list of fields.
     */
    public static List<String> extractRelations(final List<FieldDefinition> fields) {

        return fields.stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .map(FieldDefinition::getRelation)
                .map(RelationDefinition::getType)
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

    public static List<FieldDefinition> extractEnumFields(final List<FieldDefinition> fields) {

        return fields.stream()
                .filter(field -> ENUM.equalsIgnoreCase(field.getType()))
                .collect(Collectors.toList());
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
                .filter(FieldDefinition::isId)
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
    public static List<String> extractFieldForJavadoc(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .filter(field -> StringUtils.isNotBlank(field.getDescription()))
                .map(field -> String.format("@param %s %s", field.getName(), field.getDescription()))
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
                .map(field -> String.format("final %s %s", field.getResolvedType(), field.getName()))
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
    public static List<String> generateInputArgs(final List<FieldDefinition> fields) {
        
        return fields.stream()
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
    public static List<String> generateInputArgsWithoutFinal(final List<FieldDefinition> fields) {
        
        return fields.stream()
                .map(field -> {

                    if (Objects.nonNull(field.getRelation())) {
                        return String.format("%sTO %s", ModelNameUtils.stripSuffix(field.getType()), field.getName());
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
     * Returns true if any field in the given list of fields is marked as an ID field, false otherwise.
     * 
     * @param fields The list of fields to check.
     * @return True if any field is marked as an ID field, false otherwise.
     */
    public static boolean isAnyFieldId(final List<FieldDefinition> fields) {

        return fields.stream().anyMatch(FieldDefinition::isId);
    }

    /**
     * Computes the resolved type given the type and name of a field.
     * 
     * @param type The type of the field.
     * @param name The name of the field.
     * @return The resolved type of the field.
     */
    public static String computeResolvedType(final String type, final String name) {
        
        if (ENUM.equalsIgnoreCase(type) && Objects.nonNull(name)) {
            return StringUtils.capitalize(name) + ENUM;
        }
        
        return type;
    }
    
}
