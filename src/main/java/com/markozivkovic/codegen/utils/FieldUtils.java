package com.markozivkovic.codegen.utils;

import java.util.List;

import com.markozivkovic.codegen.model.FieldDefinition;

public class FieldUtils {

    private static final String UUID = "UUID";
    private static final String LOCAL_DATE = "LocalDate";
    private static final String LOCAL_DATE_TIME = "LocalDateTime";

    private FieldUtils() {
        
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
    
}
