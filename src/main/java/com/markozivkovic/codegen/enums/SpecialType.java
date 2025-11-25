package com.markozivkovic.codegen.enums;

import java.util.Objects;

public enum SpecialType {
    
    ENUM("Enum"),
    JSON("JSON");

    private final String key;

    SpecialType(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns true if the given type is equal to "ENUM", false otherwise.
     * 
     * @param type the type to check
     * @return true if the type is equal to "ENUM", false otherwise
     */
    public static boolean isEnumType(final String type) {

        return Objects.nonNull(type) && type.equalsIgnoreCase(ENUM.getKey());
    }

    /**
     * Returns true if the given type is a JSON type, false otherwise.
     * A JSON type is a type that starts with "JSON[" and ends with "]". For example, "JSON[SomeType]" is a JSON type.
     * 
     * @param type the type to check
     * @return true if the type is a JSON type, false otherwise
     */
    public static boolean isJsonType(final String type) {

        return Objects.nonNull(type) &&
                type.toUpperCase().startsWith(JSON.getKey() + "[") &&
                type.endsWith("]");
    }
}
