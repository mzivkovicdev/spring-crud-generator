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

package dev.markozivkovic.springcrudgenerator.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.api.annotations.Nullable;

import dev.markozivkovic.springcrudgenerator.enums.BasicType;
import dev.markozivkovic.springcrudgenerator.enums.SpecialType;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerSchemaModeEnum;
import dev.markozivkovic.springcrudgenerator.models.ColumnDefinition;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;

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
     * Creates a Swagger property from a given {@link FieldDefinition} using the default Swagger schema mode.
     *
     * @param fieldDefinition the field definition to create a Swagger property from
     * @return a Swagger property
     */
    public static Map<String, Object> toSwaggerProperty(final FieldDefinition fieldDefinition) {
        
        return toSwaggerProperty(fieldDefinition, SwaggerSchemaModeEnum.DEFAULT);
    }

    /**
     * Creates a Swagger property from a given {@link FieldDefinition}.
     *
     * @param fieldDefinition   the field definition to create a Swagger property from
     * @param swaggerSchemaMode the Swagger schema mode
     * @return a Swagger property
     */
    public static Map<String, Object> toSwaggerProperty(final FieldDefinition fieldDefinition, final SwaggerSchemaModeEnum swaggerSchemaMode) {

        final Map<String, Object> property = new LinkedHashMap<>();
        
        property.put("name", fieldDefinition.getName());
        if (StringUtils.isNotBlank(fieldDefinition.getDescription())) {
            property.put("description", fieldDefinition.getDescription());
        }

        if (Objects.nonNull(fieldDefinition.getColumn()) || Objects.nonNull(fieldDefinition.getValidation())) {
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
                    schema = ref(type, swaggerSchemaMode);
                    break;
                case ONE_TO_MANY:
                case MANY_TO_MANY:
                    schema = arrayOfRef(type, swaggerSchemaMode);
                    break;
                default:
                    schema = ref(type, swaggerSchemaMode);
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

        final boolean basicType = BasicType.isBasicType(fieldDefinition.getType());
        final boolean isCollection = SpecialType.isCollectionType(fieldDefinition.getType());

        if (Objects.nonNull(fieldDefinition.getColumn()) && Objects.nonNull(fieldDefinition.getColumn().getNullable())) {
            validationSchema.put("nullable", fieldDefinition.getColumn().getNullable());
        }
        
        if ((Objects.nonNull(fieldDefinition.getValidation()) && Boolean.TRUE.equals(fieldDefinition.getValidation().getRequired()))) {
            validationSchema.put("nullable", !fieldDefinition.getValidation().getRequired());
        }

        if (basicType) {
            validationSchema.putAll(applyBasicTypeSwaggerValidation(fieldDefinition));
        }

        if (isCollection) {
            validationSchema.putAll(applyCollectionTypeSwaggerValidation(fieldDefinition));
        }

        return validationSchema;
    }

    /**
     * Applies Swagger validation annotations for a given field definition that is a collection.
     * 
     * If the minimum items constraint is present on the field definition, it will be added to the validation schema.
     * If the maximum items constraint is present on the field definition, it will be added to the validation schema.
     * If the not empty constraint is present on the field definition, it will set the minimum items constraint to 1.
     * 
     * @param fieldDefinition the field definition to apply the Swagger validation annotations to
     * @return a map containing the Swagger validation annotations
     */
    private static Map<String, Object> applyCollectionTypeSwaggerValidation(final FieldDefinition fieldDefinition) {

        final Map<String, Object> validationSchema = new LinkedHashMap<>();
        final ValidationDefinition validationDefinition = fieldDefinition.getValidation();

        if (Objects.isNull(validationDefinition)) {
            return Map.of();
        }

        if (Objects.nonNull(validationDefinition.getMinItems())) { validationSchema.put("minItems", validationDefinition.getMinItems()); }
        if (Objects.nonNull(validationDefinition.getMaxItems())) { validationSchema.put("maxItems", validationDefinition.getMaxItems()); }

        if (Boolean.TRUE.equals(validationDefinition.isNotEmpty())) {
            final Object existingMinItems = validationSchema.get("minItems");
            if (!(existingMinItems instanceof Integer) || ((Integer) existingMinItems) < 1) {
                validationSchema.put("minItems", 1);
            }
        }

        return validationSchema;
    }

    /**
     * Applies Swagger validation annotations to a given field definition that is a basic type (String, Integer, Long, BigInteger, Double, Float, BigDecimal).
     * 
     * If the minimum length constraint is present on the field definition, it will be added to the validation schema.
     * If the maximum length constraint is present on the field definition, it will be added to the validation schema.
     * If the not empty constraint is present on the field definition, it will set the minimum length constraint to 1.
     * If the not blank constraint is present on the field definition, it will set the minimum length constraint to 1.
     * If the email constraint is present on the field definition, it will set the format constraint to "email".
     * 
     * @param fieldDefinition the field definition to apply the Swagger validation annotations to
     * @return a map containing the Swagger validation annotations
     */
    private static Map<String, Object> applyBasicTypeSwaggerValidation(final FieldDefinition fieldDefinition) {

        final BasicType basicFieldType = BasicType.fromString(fieldDefinition.getType().trim());
        final ValidationDefinition validationDefinition = fieldDefinition.getValidation();
        final ColumnDefinition columnDefinition = fieldDefinition.getColumn();
        final Map<String, Object> validationSchema = new LinkedHashMap<>();

        switch (basicFieldType) {
            case STRING:
                Integer minLength = Objects.nonNull(validationDefinition) ? validationDefinition.getMinLength() : null;
                Integer maxLength = Objects.nonNull(validationDefinition) ? validationDefinition.getMaxLength() : null;

                if (Objects.isNull(maxLength) && Objects.nonNull(columnDefinition) && Objects.nonNull(columnDefinition.getLength())) {
                    maxLength = columnDefinition.getLength();
                }
                if (Objects.nonNull(minLength)) { validationSchema.put("minLength", minLength); }
                if (Objects.nonNull(maxLength)) { validationSchema.put("maxLength", maxLength); }

                if (Objects.nonNull(validationDefinition)) {

                    if (Boolean.TRUE.equals(validationDefinition.getNotEmpty()) || Boolean.TRUE.equals(validationDefinition.getNotBlank())) {
                        final Object existingMinLength = validationSchema.get("minLength");
                        if (!(existingMinLength instanceof Integer) || ((Integer) existingMinLength) < 1) { validationSchema.put("minLength", 1); }
                    }
                    if (Boolean.TRUE.equals(validationDefinition.getEmail())) { validationSchema.put("format", "email"); }
                }
                break;
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
                if (Objects.nonNull(validationDefinition)) {
                    if (Objects.nonNull(validationDefinition.getMin())) { validationSchema.put("minimum", validationDefinition.getMin().longValue()); }
                    if (Objects.nonNull(validationDefinition.getMax())) { validationSchema.put("maximum", validationDefinition.getMax().longValue()); }
                }
                break;
            case DOUBLE:
            case FLOAT:
            case BIG_DECIMAL:
                if (Objects.nonNull(validationDefinition)) {
                    if (Objects.nonNull(validationDefinition.getMin())) { validationSchema.put("minimum", validationDefinition.getMin()); }
                    if (Objects.nonNull(validationDefinition.getMax())) { validationSchema.put("maximum", validationDefinition.getMax()); }
                }
                break;
            default:
                break;
        }

        return validationSchema;
    }

    /**
     * Return a Swagger $ref definition to the given targetSchemaName.
     *
     * @param targetSchemaName  the name of the target schema
     * @return a Swagger $ref definition
     */
    public static Map<String, Object> ref(final String targetSchemaName) {
    
        return ref(targetSchemaName, SwaggerSchemaModeEnum.DEFAULT);
    }

    /**
     * Return a Swagger $ref definition to the given targetSchemaName.
     * 
     * @param targetSchemaName the name of the target schema
     * @param swaggerSchemaMode the Swagger schema mode
     * @return a Swagger $ref definition
     */
    public static Map<String, Object> ref(final String targetSchemaName, final SwaggerSchemaModeEnum swaggerSchemaMode) {
        
        final Map<String, Object> m = new LinkedHashMap<>();
        final String schemaName = ModelNameUtils.stripSuffix(targetSchemaName);
        final String ref;
        if (SwaggerSchemaModeEnum.INPUT.equals(swaggerSchemaMode)) {
            ref = String.format("./%sInput.yaml", StringUtils.uncapitalize(schemaName));
        } else {
            ref = String.format("./%s.yaml", StringUtils.uncapitalize(ModelNameUtils.computeOpenApiModelName(schemaName)));
        }
        m.put("$ref", ref);
        return m;
    }

    /**
     * Return a Swagger array type definition which references the given targetSchemaName.
     * 
     * @param targetSchemaName the name of the target schema
     * @return a Swagger array type definition
     */
    public static Map<String, Object> arrayOfRef(final String targetSchemaName) {
    
        return arrayOfRef(targetSchemaName, SwaggerSchemaModeEnum.DEFAULT);
    }

    /**
     * Return a Swagger array type definition which references the given targetSchemaName.
     * 
     * @param targetSchemaName the name of the target schema
     * @param swaggerSchemaMode the Swagger schema mode
     * @return a Swagger array type definition
     */
    public static Map<String, Object> arrayOfRef(final String targetSchemaName, final SwaggerSchemaModeEnum swaggerSchemaMode) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "array");
        m.put("items", ref(targetSchemaName, swaggerSchemaMode));
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