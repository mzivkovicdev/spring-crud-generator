package com.markozivkovic.codegen.utils;

import java.util.List;

public class ModelNameUtils {

    private static final List<String> SQL_MODEL_SUFFIXES = List.of(
        "Entity", "Model", "Table", "JpaEntity", "Domain", "DAO", "Data"
    );

    private ModelNameUtils() {

    }

    /**
     * Removes a suffix from a model name, if the model name ends with it.
     * 
     * @param modelName the model name to strip the suffix from
     * @return the model name with the suffix removed, or the original model name if it does not match any suffix
     */
    public static String stripSuffix(final String modelName) {

        return SQL_MODEL_SUFFIXES.stream()
                .filter(suffix -> modelName.toLowerCase().endsWith(suffix.toLowerCase()))
                .findFirst()
                .map(suffix -> modelName.substring(0, modelName.length() - suffix.length()))
                .orElse(modelName);
    }

    /**
     * Converts a camel-case string to a snake-case string.
     * @param s the string to convert
     * @return the converted string
     */
    public static String toSnakeCase(final String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
}
