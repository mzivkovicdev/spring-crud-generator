package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.enums.SwaggerSchemaModeEnum;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.RelationDefinition;

class SwaggerUtilsTest {

    private Map<String, Object> mapOf(Object... kv) {
        final Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(final Object obj) {
        return (Map<String, Object>) obj;
    }


    @Test
    @DisplayName("resolve: STRING -> type=string")
    void resolve_string_returnsStringType() {
        final Map<String, Object> result = SwaggerUtils.resolve("String", null);

        assertEquals(mapOf("type", "string"), result);
    }

    @Test
    @DisplayName("resolve: CHARSEQUENCE -> type=string")
    void resolve_charSequence_returnsStringType() {
        final Map<String, Object> result = SwaggerUtils.resolve("CharSequence", null);

        assertEquals(mapOf("type", "string"), result);
    }

    @Test
    @DisplayName("resolve: CHAR -> type=string")
    void resolve_char_returnsStringType() {
        final Map<String, Object> result = SwaggerUtils.resolve("char", null);

        assertEquals(mapOf("type", "string"), result);
    }

    @Test
    @DisplayName("resolve: CHARACTER -> type=string")
    void resolve_character_returnsStringType() {
        final Map<String, Object> result = SwaggerUtils.resolve("Character", null);

        assertEquals(mapOf("type", "string"), result);
    }

    @Test
    @DisplayName("resolve: UUID -> type=string, format=uuid")
    void resolve_uuid_returnsStringUuid() {
        final Map<String, Object> result = SwaggerUtils.resolve("UUID", null);

        assertEquals(mapOf("type", "string", "format", "uuid"), result);
    }

    @Test
    @DisplayName("resolve: BOOLEAN -> type=boolean")
    void resolve_boolean_returnsBoolean() {
        final Map<String, Object> result = SwaggerUtils.resolve("boolean", null);

        assertEquals(mapOf("type", "boolean"), result);
    }

    @Test
    @DisplayName("resolve: BYTE -> integer/int32")
    void resolve_byte_returnsInt32() {
        final Map<String, Object> result = SwaggerUtils.resolve("byte", null);

        assertEquals(mapOf("type", "integer", "format", "int32"), result);
    }

    @Test
    @DisplayName("resolve: SHORT -> integer/int32")
    void resolve_short_returnsInt32() {
        final Map<String, Object> result = SwaggerUtils.resolve("short", null);

        assertEquals(mapOf("type", "integer", "format", "int32"), result);
    }

    @Test
    @DisplayName("resolve: INT -> integer/int32")
    void resolve_int_returnsInt32() {
        final Map<String, Object> result = SwaggerUtils.resolve("int", null);

        assertEquals(mapOf("type", "integer", "format", "int32"), result);
    }

    @Test
    @DisplayName("resolve: INTEGER -> integer/int32")
    void resolve_integer_returnsInt32() {
        final Map<String, Object> result = SwaggerUtils.resolve("Integer", null);

        assertEquals(mapOf("type", "integer", "format", "int32"), result);
    }

    @Test
    @DisplayName("resolve: LONG -> integer/int64")
    void resolve_long_returnsInt64() {
        final Map<String, Object> result = SwaggerUtils.resolve("long", null);

        assertEquals(mapOf("type", "integer", "format", "int64"), result);
    }

    @Test
    @DisplayName("resolve: FLOAT -> number/float")
    void resolve_float_returnsNumberFloat() {
        final Map<String, Object> result = SwaggerUtils.resolve("float", null);

        assertEquals(mapOf("type", "number", "format", "float"), result);
    }

    @Test
    @DisplayName("resolve: DOUBLE -> number/double")
    void resolve_double_returnsNumberDouble() {
        final Map<String, Object> result = SwaggerUtils.resolve("double", null);

        assertEquals(mapOf("type", "number", "format", "double"), result);
    }

    @Test
    @DisplayName("resolve: BIGDECIMAL -> number")
    void resolve_bigDecimal_returnsNumber() {
        final Map<String, Object> result = SwaggerUtils.resolve("big decimal", null);

        assertEquals(mapOf("type", "number"), result);
    }

    @Test
    @DisplayName("resolve: LOCALDATE -> string/date")
    void resolve_localDate_returnsStringDate() {
        final Map<String, Object> result = SwaggerUtils.resolve("LocalDate", null);

        assertEquals(mapOf("type", "string", "format", "date"), result);
    }

    @Test
    @DisplayName("resolve: LOCALDATETIME -> string/date-time")
    void resolve_localDateTime_returnsStringDateTime() {
        final Map<String, Object> result = SwaggerUtils.resolve("LocalDateTime", null);

        assertEquals(mapOf("type", "string", "format", "date-time"), result);
    }

    @Test
    @DisplayName("resolve: INSTANT -> string/date-time")
    void resolve_instant_returnsStringDateTime() {
        final Map<String, Object> result = SwaggerUtils.resolve("Instant", null);

        assertEquals(mapOf("type", "string", "format", "date-time"), result);
    }

    @Test
    @DisplayName("resolve: DATE -> string/date-time")
    void resolve_date_returnsStringDateTime() {
        final Map<String, Object> result = SwaggerUtils.resolve("Date", null);

        assertEquals(mapOf("type", "string", "format", "date-time"), result);
    }

    @Test
    @DisplayName("resolve: ENUM with values -> type=string, enum=[values]")
    void resolve_enumWithValues_returnsStringWithEnum() {
        final List<String> values = Arrays.asList("A", "B", "C");

        final Map<String, Object> result = SwaggerUtils.resolve("enum", values);

        assertEquals("string", result.get("type"));
        @SuppressWarnings("unchecked")
        final List<String> enumList = (List<String>) result.get("enum");
        assertEquals(values, enumList);
    }

    @Test
    @DisplayName("resolve: ENUM with null enumValues -> type=string, no enum key")
    void resolve_enumWithNullValues_returnsStringWithoutEnumKey() {
        final Map<String, Object> result = SwaggerUtils.resolve("ENUM", null);

        assertEquals("string", result.get("type"));
        assertNull(result.get("enum"));
    }

    @Test
    @DisplayName("resolve: ENUM with empty enumValues -> type=string, no enum key")
    void resolve_enumWithEmptyValues_returnsStringWithoutEnumKey() {
        final Map<String, Object> result = SwaggerUtils.resolve("ENUM", new ArrayList<>());

        assertEquals("string", result.get("type"));
        assertNull(result.get("enum"));
    }

    @Test
    @DisplayName("resolve: unknown type -> defaults to type=string")
    void resolve_unknownType_defaultsToString() {
        final Map<String, Object> result = SwaggerUtils.resolve("SomeCustomType", null);

        assertEquals(mapOf("type", "string"), result);
    }

    @Test
    @DisplayName("resolve: null type -> treated as empty and defaults to string")
    void resolve_nullType_defaultsToString() {
        final Map<String, Object> result = SwaggerUtils.resolve(null, null);

        assertEquals(mapOf("type", "string"), result);
    }

    @Test
    @DisplayName("resolve: type with spaces and mixed case should still match (e.g. ' local date ')")
    void resolve_typeWithSpacesAndMixedCase_isHandled() {
        final Map<String, Object> result = SwaggerUtils.resolve("  local   date  ", null);

        assertEquals(mapOf("type", "string", "format", "date"), result);
    }

    @Test
    @DisplayName("resolve: trimming and case-insensitivity - '  sTrInG  ' -> string")
    void resolve_trimmingAndCaseInsensitivity() {
        final Map<String, Object> result = SwaggerUtils.resolve("  sTrInG  ", null);

        assertEquals(mapOf("type", "string"), result);
    }

    @Test
    @DisplayName("toSwaggerProperty: should set name and description when description is not blank")
    void toSwaggerProperty_setsNameAndDescription() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("id");
        field.setDescription("Primary identifier");
        field.setType("String");

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("id", property.get("name"));
        assertEquals("Primary identifier", property.get("description"));
        assertEquals("string", property.get("type"));
    }

    @Test
    @DisplayName("toSwaggerProperty: should not set description when description is blank")
    void toSwaggerProperty_ignoresBlankDescription() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("id");
        field.setDescription("   ");
        field.setType("String");

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("id", property.get("name"));
        assertFalse(property.containsKey("description"));
        assertEquals("string", property.get("type"));
    }

    @Test
    @DisplayName("toSwaggerProperty: relation ONE_TO_ONE should use $ref schema")
    void toSwaggerProperty_oneToOneRelation_usesRef() {
        final RelationDefinition rel = new RelationDefinition();
        rel.setType("OneToOne");

        final FieldDefinition field = new FieldDefinition();
        field.setName("user");
        field.setType("UserDto");
        field.setRelation(rel);

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("user", property.get("name"));
        assertTrue(property.containsKey("$ref"));
        assertFalse(property.containsKey("type"));
    }

    @Test
    @DisplayName("toSwaggerProperty: relation MANY_TO_ONE should use $ref schema")
    void toSwaggerProperty_manyToOneRelation_usesRef() {
        final RelationDefinition rel = new RelationDefinition();
        rel.setType("ManyToOne");

        final FieldDefinition field = new FieldDefinition();
        field.setName("user");
        field.setType("UserDto");
        field.setRelation(rel);

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertTrue(property.containsKey("$ref"));
    }

    @Test
    @DisplayName("toSwaggerProperty: relation ONE_TO_MANY should use array of $ref schema")
    void toSwaggerProperty_oneToManyRelation_usesArrayOfRef() {
        final RelationDefinition rel = new RelationDefinition();
        rel.setType("OneToMany");

        final FieldDefinition field = new FieldDefinition();
        field.setName("users");
        field.setType("UserDto");
        field.setRelation(rel);

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("array", property.get("type"));
        final Object items = property.get("items");
        assertNotNull(items);
        final Map<String, Object> itemsMap = asMap(items);
        assertTrue(itemsMap.containsKey("$ref"));
    }

    @Test
    @DisplayName("toSwaggerProperty: relation MANY_TO_MANY should use array of $ref schema")
    void toSwaggerProperty_manyToManyRelation_usesArrayOfRef() {
        final RelationDefinition rel = new RelationDefinition();
        rel.setType("ManyToMany");

        final FieldDefinition field = new FieldDefinition();
        field.setName("users");
        field.setType("UserDto");
        field.setRelation(rel);

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("array", property.get("type"));
        final Map<String, Object> itemsMap = asMap(property.get("items"));
        assertTrue(itemsMap.containsKey("$ref"));
    }

    @Test
    @DisplayName("toSwaggerProperty: no relation and non-JSON field should use resolve(type, values)")
    void toSwaggerProperty_simpleNonRelation_usesResolve() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("amount");
        field.setDescription("Amount");
        field.setType("BigDecimal");
        field.setValues(null);

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("amount", property.get("name"));
        assertEquals("number", property.get("type"));
    }

    @Test
    @DisplayName("toSwaggerProperty: ENUM type should add enum values to schema")
    void toSwaggerProperty_enumType_addsEnumValues() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("status");
        field.setType("ENUM");
        field.setValues(Arrays.asList("ACTIVE", "INACTIVE"));

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("status", property.get("name"));
        assertEquals("string", property.get("type"));
        @SuppressWarnings("unchecked")
        final List<String> enumValues = (List<String>) property.get("enum");
        assertEquals(Arrays.asList("ACTIVE", "INACTIVE"), enumValues);
    }

    @Test
    @DisplayName("toSwaggerProperty: simple collection List<String> -> array with string items")
    void toSwaggerProperty_simpleCollection_listOfString() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("phoneNumbers");
        field.setType("List<String>");

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("phoneNumbers", property.get("name"));
        assertEquals("array", property.get("type"));

        final Map<String, Object> items = asMap(property.get("items"));
        assertEquals("string", items.get("type"));
        assertFalse(items.containsKey("$ref"));
    }

    @Test
    @DisplayName("toSwaggerProperty: simple collection Set<Enum> with values -> array items include enum list")
    void toSwaggerProperty_simpleCollection_setOfEnum_addsEnumValuesOnItems() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("statuses");
        field.setType("Set<Enum>");
        field.setValues(List.of("ACTIVE", "INACTIVE"));

        final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field);

        assertEquals("statuses", property.get("name"));
        assertEquals("array", property.get("type"));

        final Map<String, Object> items = asMap(property.get("items"));
        assertEquals("string", items.get("type"));

        @SuppressWarnings("unchecked")
        final List<String> enumValues = (List<String>) items.get("enum");
        assertEquals(List.of("ACTIVE", "INACTIVE"), enumValues);
    }

    @Test
    @DisplayName("toSwaggerProperty(INPUT): ONE_TO_ONE relation should use Input $ref")
    void toSwaggerProperty_inputMode_oneToOne_usesInputRef() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("user");
        field.setType("UserDto");
        field.setRelation(new RelationDefinition().setType("OneToOne"));

        try (MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserDto")).thenReturn("User");

            final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field, SwaggerSchemaModeEnum.INPUT);

            assertEquals("user", property.get("name"));
            assertEquals("./userInput.yaml", property.get("$ref"));
            assertFalse(property.containsKey("type"));
        }
    }

    @Test
    @DisplayName("toSwaggerProperty(INPUT): ONE_TO_MANY relation should use array with Input $ref items")
    void toSwaggerProperty_inputMode_oneToMany_usesArrayOfInputRef() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("users");
        field.setType("UserDto");
        field.setRelation(new RelationDefinition().setType("OneToMany"));

        try (MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserDto")).thenReturn("User");

            final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field, SwaggerSchemaModeEnum.INPUT);

            assertEquals("users", property.get("name"));
            assertEquals("array", property.get("type"));
            final Map<String, Object> itemsMap = asMap(property.get("items"));
            assertEquals("./userInput.yaml", itemsMap.get("$ref"));
        }
    }

    @Test
    @DisplayName("toSwaggerProperty(INPUT): JSON field still uses DEFAULT ref (implementation uses ref(...) without mode)")
    void toSwaggerProperty_inputMode_jsonField_usesDefaultRef() {
        final FieldDefinition field = new FieldDefinition();
        field.setName("payload");
        field.setType("JsonNode");

        try (MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isJsonField(field)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isSimpleCollectionField(field)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(field)).thenReturn("PayloadDto");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("PayloadDto")).thenReturn("Payload");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Payload")).thenReturn("Payload");

            final Map<String, Object> property = SwaggerUtils.toSwaggerProperty(field, SwaggerSchemaModeEnum.INPUT);

            assertEquals("payload", property.get("name"));
            assertEquals("./payload.yaml", property.get("$ref"));
        }
    }

    @Test
    @DisplayName("ref: should create $ref entry with .yaml path")
    void ref_createsRefWithYamlPath() {
        final Map<String, Object> result = SwaggerUtils.ref("UserDto");

        assertTrue(result.containsKey("$ref"));
        final Object refValue = result.get("$ref");
        assertNotNull(refValue);
        assertTrue(refValue instanceof String);

        final String ref = (String) refValue;
        assertTrue(ref.startsWith("./"));
        assertTrue(ref.endsWith(".yaml"));
    }

    @Test
    @DisplayName("ref: should not add any other keys except $ref")
    void ref_containsOnlyRefKey() {
        final Map<String, Object> result = SwaggerUtils.ref("UserDto");

        assertEquals(1, result.size());
        assertTrue(result.containsKey("$ref"));
    }

    @Test
    @DisplayName("arrayOfRef: should create array type with items as $ref")
    void arrayOfRef_createsArrayWithRefItems() {
        final Map<String, Object> result = SwaggerUtils.arrayOfRef("UserDto");

        assertEquals("array", result.get("type"));
        final Object items = result.get("items");
        assertNotNull(items);

        final Map<String, Object> itemsMap = asMap(items);
        assertTrue(itemsMap.containsKey("$ref"));
    }

    @Test
    @DisplayName("arrayOfRef: items should be equal to ref(targetSchemaName)")
    void arrayOfRef_itemsEqualsRef() {
        final Map<String, Object> ref = SwaggerUtils.ref("UserDto");
        final Map<String, Object> array = SwaggerUtils.arrayOfRef("UserDto");

        final Map<String, Object> itemsMap = asMap(array.get("items"));
        assertEquals(ref, itemsMap);
    }

    @Test
    @DisplayName("ref(DEFAULT): should create $ref entry with ./<model>.yaml using computeOpenApiModelName")
    void ref_default_createsRefWithYamlPath_usingComputeModelName() {
        try (MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserDto")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");

            final Map<String, Object> result = SwaggerUtils.ref("UserDto", SwaggerSchemaModeEnum.DEFAULT);

            assertEquals(1, result.size());
            assertEquals("./user.yaml", result.get("$ref"));
        }
    }

    @Test
    @DisplayName("ref(INPUT): should create $ref entry with ./<model>Input.yaml")
    void ref_input_createsRefWithInputYamlPath() {
        try (MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserDto")).thenReturn("User");

            final Map<String, Object> result = SwaggerUtils.ref("UserDto", SwaggerSchemaModeEnum.INPUT);

            assertEquals(1, result.size());
            assertEquals("./userInput.yaml", result.get("$ref"));
        }
    }

    @Test
    @DisplayName("ref: overload without mode delegates to DEFAULT")
    void ref_overload_delegatesToDefault() {
        try (MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserDto")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");

            final Map<String, Object> result = SwaggerUtils.ref("UserDto");

            assertEquals("./user.yaml", result.get("$ref"));
        }
    }

    @Test
    @DisplayName("arrayOfRef(INPUT): should create array with items $ref pointing to Input schema")
    void arrayOfRef_input_createsArrayWithInputRefItems() {
        try (MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserDto")).thenReturn("User");

            final Map<String, Object> result = SwaggerUtils.arrayOfRef("UserDto", SwaggerSchemaModeEnum.INPUT);

            assertEquals("array", result.get("type"));
            final Map<String, Object> itemsMap = asMap(result.get("items"));
            assertEquals("./userInput.yaml", itemsMap.get("$ref"));
        }
    }

    @Test
    @DisplayName("arrayOfRef: overload without mode delegates to DEFAULT")
    void arrayOfRef_overload_delegatesToDefault() {
        try (MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserDto")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");

            final Map<String, Object> array = SwaggerUtils.arrayOfRef("UserDto");

            assertEquals("array", array.get("type"));
            final Map<String, Object> itemsMap = asMap(array.get("items"));
            assertEquals("./user.yaml", itemsMap.get("$ref"));
        }
    }
    
}
