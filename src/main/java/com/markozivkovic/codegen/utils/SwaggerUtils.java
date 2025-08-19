package com.markozivkovic.codegen.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Nullable;

public class SwaggerUtils {
    
    private SwaggerUtils() {
        
    }

    /**
     * Resolve the given yamlType to a Swagger type definition.
     * 
     * This method takes into account the following mappings:
     * <ul>
     * <li>String, CharSequence, char, Character -> string</li>
     * <li>UUID -> string with format uuid</li>
     * <li>boolean -> boolean</li>
     * <li>byte, short, int, Integer -> integer with format int32</li>
     * <li>long -> integer with format int64</li>
     * <li>float -> number with format float</li>
     * <li>double -> number with format double</li>
     * <li>big decimal -> number</li>
     * <li>LocalDate -> string with format date</li>
     * <li>LocalDateTime, Instant, Date -> string with format date-time</li>
     * <li>enum -> string with enum values</li>
     * <li>all others -> string</li>
     * </ul>
     * 
     * @param yamlType the yaml type to resolve
     * @param enumValues the values of the enum
     * @return the resolved Swagger type definition
     */
    public static Map<String, Object> resolve(final String yamlType, @Nullable final List<String> enumValues) {
        
        final String t = yamlType == null ? "" : yamlType.trim();
        final String key = t.replaceAll("\\s+", "").toUpperCase();
        final Map<String, Object> m = new LinkedHashMap<>();

        switch (key) {
            case "STRING":
            case "CHARSEQUENCE":
            case "CHAR":
            case "CHARACTER":
                m.put("type", "string");
                return m;

            case "UUID":
                m.put("type", "string");
                m.put("format", "uuid");
                return m;

            case "BOOLEAN":
                m.put("type", "boolean");
                return m;
        
            case "BYTE":
            case "SHORT":
            case "INT":
            case "INTEGER":
                m.put("type", "integer");
                m.put("format", "int32");
                return m;

            case "LONG":
                m.put("type", "integer");
                m.put("format", "int64");
                return m;

            case "FLOAT":
                m.put("type", "number");
                m.put("format", "float");
                return m;

            case "DOUBLE":
                m.put("type", "number");
                m.put("format", "double");
                return m;

            case "BIGDECIMAL":
                m.put("type", "number");
                return m;

            case "LOCALDATE":
                m.put("type", "string");
                m.put("format", "date");
                return m;

            case "LOCALDATETIME":
            case "INSTANT":
            case "DATE":
                m.put("type", "string");
                m.put("format", "date-time");
                return m;

            case "ENUM":
                m.put("type", "string");
                if (enumValues != null && !enumValues.isEmpty()) {
                    m.put("enum", new ArrayList<>(enumValues));
                }
                return m;
            default:
                m.put("type", "string");
                return m;
        }
    }

}