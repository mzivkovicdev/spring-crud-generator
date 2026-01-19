/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.codegen.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.api.annotations.Nullable;

import dev.markozivkovic.codegen.enums.SpecialType;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.RelationDefinition;

public class SwaggerUtils {

    private static final String ONE_TO_ONE = "OneToOne";
    private static final String ONE_TO_MANY = "OneToMany";
    private static final String MANY_TO_ONE = "ManyToOne";
    private static final String MANY_TO_MANY = "ManyToMany";
    
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
        
        final String type = yamlType == null ? "" : yamlType.trim();
        final String key = type.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        final Map<String, Object> typeContext = new LinkedHashMap<>();

        switch (key) {
            case "STRING":
            case "CHARSEQUENCE":
            case "CHAR":
            case "CHARACTER":
                typeContext.put("type", "string");
                return typeContext;

            case "UUID":
                typeContext.put("type", "string");
                typeContext.put("format", "uuid");
                return typeContext;

            case "BOOLEAN":
                typeContext.put("type", "boolean");
                return typeContext;
        
            case "BYTE":
            case "SHORT":
            case "INT":
            case "INTEGER":
                typeContext.put("type", "integer");
                typeContext.put("format", "int32");
                return typeContext;

            case "LONG":
                typeContext.put("type", "integer");
                typeContext.put("format", "int64");
                return typeContext;

            case "FLOAT":
                typeContext.put("type", "number");
                typeContext.put("format", "float");
                return typeContext;

            case "DOUBLE":
                typeContext.put("type", "number");
                typeContext.put("format", "double");
                return typeContext;

            case "BIGDECIMAL":
                typeContext.put("type", "number");
                return typeContext;

            case "LOCALDATE":
                typeContext.put("type", "string");
                typeContext.put("format", "date");
                return typeContext;

            case "LOCALDATETIME":
            case "INSTANT":
            case "DATE":
                typeContext.put("type", "string");
                typeContext.put("format", "date-time");
                return typeContext;

            case "ENUM":
                typeContext.put("type", "string");
                if (enumValues != null && !enumValues.isEmpty()) {
                    typeContext.put("enum", new ArrayList<>(enumValues));
                }
                return typeContext;
            default:
                typeContext.put("type", "string");
                return typeContext;
        }
    }

    /**
     * Creates a Swagger property from a given {@link FieldDefinition}.
     *
     * @param fieldDefinition the field definition to create a Swagger property from
     * @return a Swagger property
     */
    public static Map<String, Object> toSwaggerProperty(final FieldDefinition fieldDefinition) {

        final Map<String, Object> property = new LinkedHashMap<>();
        
        property.put("name", fieldDefinition.getName());
        if (StringUtils.isNotBlank(fieldDefinition.getDescription())) {
            property.put("description", fieldDefinition.getDescription());
        }

        if (Objects.nonNull(fieldDefinition.getColumn())) {
            property.putAll(applySwaggerValidation(fieldDefinition));
        }

        Map<String, Object> schema;
        final RelationDefinition rel = fieldDefinition.getRelation();
        final String type = fieldDefinition.getType();
        final boolean isJsonField = FieldUtils.isJsonField(fieldDefinition);
        final boolean isSimpleCollectionType = FieldUtils.isSimpleCollectionField(fieldDefinition);

        if (Objects.nonNull(rel)) {

            switch (rel.getType()) {
                case ONE_TO_ONE:
                case MANY_TO_ONE:
                    schema = ref(type);
                    break;
                case ONE_TO_MANY:
                case MANY_TO_MANY:
                    schema = arrayOfRef(type);
                    break;
                default:
                    schema = ref(type);
                    break;
            }
        } else {
            if (isJsonField) {
                schema = ref(FieldUtils.extractJsonFieldName(fieldDefinition));
            } else if (isSimpleCollectionType) {
                schema = arrayOfSimpleType(
                        FieldUtils.extractSimpleCollectionType(fieldDefinition), fieldDefinition.getValues(), SpecialType.isSetType(fieldDefinition.getType())
                );
            } else {
                schema = resolve(type, fieldDefinition.getValues());
            }
        }

        property.putAll(schema);
        
        return property;
    }

    /**
     * Applies Swagger validation annotations to a given field definition.
     * 
     * If the nullable attribute is present on the field definition, it will be added to the validation schema.
     * If the length attribute is present on the field definition, and the type of the field definition is either String or CharSequence,
     * it will be added to the validation schema.
     * 
     * @param fieldDefinition the field definition to apply the Swagger validation annotations to
     * @return a map containing the Swagger validation annotations
     */
    private static Map<String, Object> applySwaggerValidation(final FieldDefinition fieldDefinition) {

        final Map<String, Object> validationSchema = new LinkedHashMap<>();

        if (Objects.nonNull(fieldDefinition.getColumn().getNullable())) {
            validationSchema.put("nullable", fieldDefinition.getColumn().getNullable());
        }

        if (Objects.nonNull(fieldDefinition.getColumn().getLength())) {
            final String type = fieldDefinition.getResolvedType();
            if (type.equalsIgnoreCase("String") || type.equalsIgnoreCase("CharSequence")) {
                validationSchema.put("maxLength", fieldDefinition.getColumn().getLength());
            }
        }

        return validationSchema;
    }

    /**
     * Return a Swagger $ref definition to the given targetSchemaName.
     *
     * @param targetSchemaName the name of the target schema
     * @return a Swagger $ref definition
     */
    public static Map<String, Object> ref(final String targetSchemaName) {
        
        final Map<String, Object> m = new LinkedHashMap<>();
        final String schemaName = ModelNameUtils.stripSuffix(targetSchemaName);
        m.put("$ref", String.format("./%s.yaml", StringUtils.uncapitalize(ModelNameUtils.computeOpenApiModelName(schemaName))));
        return m;
    }

    /**
     * Return a Swagger array type definition which references the given targetSchemaName.
     * 
     * @param targetSchemaName the name of the target schema
     * @return a Swagger array type definition
     */
    public static Map<String, Object> arrayOfRef(final String targetSchemaName) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "array");
        m.put("items", ref(targetSchemaName));
        return m;
    }

    /**
     * Return a Swagger array type definition which contains items of the given yamlType.
     * 
     * If the yamlType is an enum, the enum values will be added to the items definition.
     * 
     * @param yamlType   the yaml type of the items
     * @param enumValues the values of the enum if the yamlType is an enum
     * @param isUnique   whether the items should be unique
     * @return a Swagger array type definition
     */
    private static Map<String, Object> arrayOfSimpleType(final String yamlType, @Nullable final List<String> enumValues,
                @Nullable final Boolean isUnique) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "array");
        m.put("uniqueItems", isUnique);
        m.put("items", resolve(yamlType, enumValues));
        return m;
    }

}