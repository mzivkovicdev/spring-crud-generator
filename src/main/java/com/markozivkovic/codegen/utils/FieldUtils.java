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
    
}
