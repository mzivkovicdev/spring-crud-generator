package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.IdDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.RelationDefinition;

class FieldUtilsTest {

    private FieldDefinition fieldWithoutRelation() {
        
        final FieldDefinition field = new FieldDefinition();
        field.setRelation(null);
        
        return field;
    }

    private FieldDefinition fieldWithType(final String type) {
        
        final FieldDefinition field = new FieldDefinition();
        field.setType(type);
        
        return field;
    }

    private FieldDefinition fieldWithRelation(final String type, final String cascade, final String fetch) {
        
        final RelationDefinition relation = new RelationDefinition();
        relation.setType(type);
        relation.setCascade(cascade);
        relation.setFetch(fetch);

        final FieldDefinition field = new FieldDefinition();
        field.setRelation(relation);
        
        return field;
    }

    private FieldDefinition fieldWithNameAndType(final String name, final String type) {
        
        final FieldDefinition field = new FieldDefinition()
                .setName(name)
                .setType(type);
        return field;
    }

    private ModelDefinition model(final String name, final List<FieldDefinition> fields) {
        
        final ModelDefinition model = new ModelDefinition()
                .setName(name)
                .setFields(fields);
        return model;
    }

    private FieldDefinition fieldWithNameTypeAndId(String name, String type, boolean isId) {
        final FieldDefinition field = new FieldDefinition()
                .setName(name)
                .setType(type);
        if (isId) {
            field.setId(new IdDefinition());
        }
        return field;
    }

    @Test
    @DisplayName("isCascadeTypeDefined returns false when list has no relations")
    void isCascadeTypeDefined_shouldReturnFalse_whenNoRelations() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        assertFalse(FieldUtils.isCascadeTypeDefined(fields));
    }

    @Test
    @DisplayName("isCascadeTypeDefined returns false when all relations have null cascade")
    void isCascadeTypeDefined_shouldReturnFalse_whenAllCascadeNull() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("OneToMany", null, null)
        );

        assertFalse(FieldUtils.isCascadeTypeDefined(fields));
    }

    @Test
    @DisplayName("isCascadeTypeDefined returns true when at least one relation has non-null cascade")
    void isCascadeTypeDefined_shouldReturnTrue_whenAnyCascadeNonNull() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("OneToMany", "ALL", null)
        );

        assertTrue(FieldUtils.isCascadeTypeDefined(fields));
    }

    @Test
    @DisplayName("isFetchTypeDefined returns false when list has no relations")
    void isFetchTypeDefined_shouldReturnFalse_whenNoRelations() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        assertFalse(FieldUtils.isFetchTypeDefined(fields));
    }

    @Test
    @DisplayName("isFetchTypeDefined returns false when all relations have null fetch")
    void isFetchTypeDefined_shouldReturnFalse_whenAllFetchNull() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("OneToMany", null, null)
        );

        assertFalse(FieldUtils.isFetchTypeDefined(fields));
    }

    @Test
    @DisplayName("isFetchTypeDefined returns true when at least one relation has non-null fetch")
    void isFetchTypeDefined_shouldReturnTrue_whenAnyFetchNonNull() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("OneToMany", null, "EAGER")
        );

        assertTrue(FieldUtils.isFetchTypeDefined(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToOne returns false when there are no relations")
    void isAnyRelationOneToOne_shouldReturnFalse_whenNoRelations() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        assertFalse(FieldUtils.isAnyRelationOneToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToOne returns false when no relation is OneToOne")
    void isAnyRelationOneToOne_shouldReturnFalse_whenNoOneToOne() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToMany", null, null),
                fieldWithRelation("ManyToOne", null, null)
        );

        assertFalse(FieldUtils.isAnyRelationOneToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToOne returns true when at least one relation is OneToOne")
    void isAnyRelationOneToOne_shouldReturnTrue_whenAnyOneToOne() {

        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("ManyToOne", null, null),
                fieldWithRelation("OneToOne", null, null)
        );

        assertTrue(FieldUtils.isAnyRelationOneToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToOne ignores case in relation type")
    void isAnyRelationOneToOne_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("onetoone", null, null)
        );

        assertTrue(FieldUtils.isAnyRelationOneToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToMany returns false when there are no relations")
    void isAnyRelationOneToMany_shouldReturnFalse_whenNoRelations() {
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        assertFalse(FieldUtils.isAnyRelationOneToMany(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToMany returns false when no relation is OneToMany")
    void isAnyRelationOneToMany_shouldReturnFalse_whenNoOneToMany() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("ManyToOne", null, null)
        );

        assertFalse(FieldUtils.isAnyRelationOneToMany(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToMany returns true when at least one relation is OneToMany")
    void isAnyRelationOneToMany_shouldReturnTrue_whenAnyOneToMany() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("ManyToOne", null, null),
                fieldWithRelation("OneToMany", null, null)
        );

        assertTrue(FieldUtils.isAnyRelationOneToMany(fields));
    }

    @Test
    @DisplayName("isAnyRelationOneToMany ignores case in relation type")
    void isAnyRelationOneToMany_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("onetomany", null, null)
        );

        assertTrue(FieldUtils.isAnyRelationOneToMany(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToOne returns false when there are no relations")
    void isAnyRelationManyToOne_shouldReturnFalse_whenNoRelations() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        assertFalse(FieldUtils.isAnyRelationManyToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToOne returns false when no relation is ManyToOne")
    void isAnyRelationManyToOne_shouldReturnFalse_whenNoManyToOne() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("OneToMany", null, null)
        );

        assertFalse(FieldUtils.isAnyRelationManyToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToOne returns true when at least one relation is ManyToOne")
    void isAnyRelationManyToOne_shouldReturnTrue_whenAnyManyToOne() {
        
        final FieldDefinition f1 = fieldWithRelation("OneToOne", null, null);
        final FieldDefinition f2 = fieldWithRelation("ManyToOne", null, null);

        final List<FieldDefinition> fields = List.of(f1, f2);

        assertTrue(FieldUtils.isAnyRelationManyToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToOne ignores case in relation type")
    void isAnyRelationManyToOne_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("manytoone", null, null)
        );

        assertTrue(FieldUtils.isAnyRelationManyToOne(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToMany returns false when there are no relations")
    void isAnyRelationManyToMany_shouldReturnFalse_whenNoRelations() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        assertFalse(FieldUtils.isAnyRelationManyToMany(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToMany returns false when no relation is ManyToMany")
    void isAnyRelationManyToMany_shouldReturnFalse_whenNoManyToMany() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("ManyToOne", null, null)
        );

        assertFalse(FieldUtils.isAnyRelationManyToMany(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToMany returns true when at least one relation is ManyToMany")
    void isAnyRelationManyToMany_shouldReturnTrue_whenAnyManyToMany() {
        
        final FieldDefinition f1 = fieldWithRelation("OneToMany", null, null);
        final FieldDefinition f2 = fieldWithRelation("ManyToMany", null, null);
        final List<FieldDefinition> fields = List.of(f1, f2);

        assertTrue(FieldUtils.isAnyRelationManyToMany(fields));
    }

    @Test
    @DisplayName("isAnyRelationManyToMany ignores case in relation type")
    void isAnyRelationManyToMany_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithRelation("manytomany", null, null)
        );

        assertTrue(FieldUtils.isAnyRelationManyToMany(fields));
    }

    @Test
    @DisplayName("extractRelationTypes returns empty list when there are no relations")
    void extractRelationTypes_shouldReturnEmpty_whenNoRelations() {
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        final List<String> result = FieldUtils.extractRelationTypes(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractRelationTypes returns only non-null relation types in order")
    void extractRelationTypes_shouldReturnRelationTypes_inOrder() {
        
        final FieldDefinition f1 = fieldWithRelation("OneToOne", null, null);
        final FieldDefinition f2 = fieldWithoutRelation();
        final FieldDefinition f3 = fieldWithRelation("ManyToMany", null, null);

        final List<FieldDefinition> fields = List.of(f1, f2, f3);

        final List<String> result = FieldUtils.extractRelationTypes(fields);

        assertEquals(2, result.size());
        assertEquals("OneToOne", result.get(0));
        assertEquals("ManyToMany", result.get(1));
    }

    @Test
    @DisplayName("extractManyToManyRelations returns empty list when there are no ManyToMany relations")
    void extractManyToManyRelations_shouldReturnEmpty_whenNoManyToMany() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithRelation("OneToMany", null, null),
                fieldWithRelation("ManyToOne", null, null)
        );

        final List<FieldDefinition> result = FieldUtils.extractManyToManyRelations(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractManyToManyRelations returns only fields with ManyToMany relation")
    void extractManyToManyRelations_shouldReturnOnlyManyToMany() {
        
        final FieldDefinition f1 = fieldWithRelation("ManyToMany", null, null);
        final FieldDefinition f2 = fieldWithRelation("OneToMany", null, null);
        final FieldDefinition f3 = fieldWithRelation("ManyToMany", null, null);

        final List<FieldDefinition> fields = List.of(f1, f2, f3);

        final List<FieldDefinition> result = FieldUtils.extractManyToManyRelations(fields);

        assertEquals(2, result.size());
        assertTrue(result.contains(f1));
        assertTrue(result.contains(f3));
    }

    @Test
    @DisplayName("extractManyToManyRelations ignores case in relation type")
    void extractManyToManyRelations_shouldIgnoreCase() {
        
        final FieldDefinition f = fieldWithRelation("manytomany", null, null);

        final List<FieldDefinition> result = FieldUtils.extractManyToManyRelations(List.of(f));

        assertEquals(1, result.size());
        assertSame(f, result.get(0));
    }

    @Test
    @DisplayName("extractOneToManyRelations returns empty list when there are no OneToMany relations")
    void extractOneToManyRelations_shouldReturnEmpty_whenNoOneToMany() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithRelation("OneToOne", null, null),
                fieldWithRelation("ManyToOne", null, null)
        );

        final List<FieldDefinition> result = FieldUtils.extractOneToManyRelations(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractOneToManyRelations returns only fields with OneToMany relation")
    void extractOneToManyRelations_shouldReturnOnlyOneToMany() {
        
        final FieldDefinition f1 = fieldWithRelation("OneToMany", null, null);
        final FieldDefinition f2 = fieldWithRelation("ManyToOne", null, null);
        final FieldDefinition f3 = fieldWithRelation("OneToMany", null, null);

        final List<FieldDefinition> fields = List.of(f1, f2, f3);

        final List<FieldDefinition> result = FieldUtils.extractOneToManyRelations(fields);

        assertEquals(2, result.size());
        assertTrue(result.contains(f1));
        assertTrue(result.contains(f3));
    }

    @Test
    @DisplayName("extractOneToManyRelations ignores case in relation type")
    void extractOneToManyRelations_shouldIgnoreCase() {
        
        final FieldDefinition f = fieldWithRelation("onetomany", null, null);

        final List<FieldDefinition> result = FieldUtils.extractOneToManyRelations(List.of(f));

        assertEquals(1, result.size());
        assertSame(f, result.get(0));
    }

    @Test
    @DisplayName("extractRelationFields returns empty list when all fields have null relation")
    void extractRelationFields_shouldReturnEmpty_whenNoRelations() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithoutRelation(),
                fieldWithoutRelation()
        );

        final List<FieldDefinition> result = FieldUtils.extractRelationFields(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractRelationFields returns only fields with non-null relation in original order")
    void extractRelationFields_shouldReturnOnlyFieldsWithRelation_inOrder() {
        
        final FieldDefinition f1 = fieldWithoutRelation();
        final FieldDefinition f2 = fieldWithRelation("OneToOne", null, null);
        final FieldDefinition f3 = fieldWithoutRelation();
        final FieldDefinition f4 = fieldWithRelation("ManyToMany", null, null);

        final List<FieldDefinition> fields = List.of(f1, f2, f3, f4);

        final List<FieldDefinition> result = FieldUtils.extractRelationFields(fields);

        assertEquals(2, result.size());
        assertSame(f2, result.get(0));
        assertSame(f4, result.get(1));
    }

    @Test
    @DisplayName("isAnyFieldEnum returns false when list is empty")
    void isAnyFieldEnum_shouldReturnFalse_whenEmptyList() {
        final List<FieldDefinition> fields = List.of();

        final boolean result = FieldUtils.isAnyFieldEnum(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldEnum returns false when no field type is Enum")
    void isAnyFieldEnum_shouldReturnFalse_whenNoEnumType() {
        final List<FieldDefinition> fields = List.of(
                fieldWithType("String"),
                fieldWithType("Integer")
        );

        final boolean result = FieldUtils.isAnyFieldEnum(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldEnum returns true when at least one field type is Enum")
    void isAnyFieldEnum_shouldReturnTrue_whenAnyEnumType() {
        final List<FieldDefinition> fields = List.of(
                fieldWithType("String"),
                fieldWithType("Enum"),
                fieldWithType("Integer")
        );

        final boolean result = FieldUtils.isAnyFieldEnum(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldEnum ignores case for Enum type")
    void isAnyFieldEnum_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithType("enum")
        );

        final boolean result = FieldUtils.isAnyFieldEnum(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isFieldEnum returns true when field type is exactly Enum")
    void isFieldEnum_shouldReturnTrue_whenTypeEnum() {
        final FieldDefinition field = fieldWithType("Enum");

        final boolean result = FieldUtils.isFieldEnum(field);

        assertTrue(result);
    }

    @Test
    @DisplayName("isFieldEnum returns true when field type is enum (case-insensitive)")
    void isFieldEnum_shouldReturnTrue_whenTypeEnumIgnoreCase() {
        
        final FieldDefinition field = fieldWithType("enum");

        final boolean result = FieldUtils.isFieldEnum(field);

        assertTrue(result);
    }

    @Test
    @DisplayName("isFieldEnum returns false when field type is not Enum")
    void isFieldEnum_shouldReturnFalse_whenTypeNotEnum() {
        
        final FieldDefinition field = fieldWithType("String");

        final boolean result = FieldUtils.isFieldEnum(field);

        assertFalse(result);
    }

    @Test
    @DisplayName("isFieldEnum returns false when field type is null")
    void isFieldEnum_shouldReturnFalse_whenTypeNull() {
        
        final FieldDefinition field = fieldWithType(null);

        final boolean result = FieldUtils.isFieldEnum(field);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldJson returns false when no field is of JSON type")
    void isAnyFieldJson_shouldReturnFalse_whenNoJsonField() {
        final List<FieldDefinition> fields = List.of(
                fieldWithType("String"),
                fieldWithType("Integer")
        );

        final boolean result = FieldUtils.isAnyFieldJson(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldJson returns true when at least one field is of JSON type (JSON[...])")
    void isAnyFieldJson_shouldReturnTrue_whenJsonTypePresent() {
        final List<FieldDefinition> fields = List.of(
                fieldWithType("String"),
                fieldWithType("JSON[String]")
        );

        final boolean result = FieldUtils.isAnyFieldJson(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldJson returns true when at least one field is of JSONB type (JSONB[...])")
    void isAnyFieldJson_shouldReturnTrue_whenJsonbTypePresent() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithType("JSONB[MyType]"),
                fieldWithType("Integer")
        );

        final boolean result = FieldUtils.isAnyFieldJson(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("extractEnumFields returns empty list when there are no Enum fields")
    void extractEnumFields_shouldReturnEmpty_whenNoEnumFields() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithType("String"),
                fieldWithType("Integer")
        );

        final List<FieldDefinition> result = FieldUtils.extractEnumFields(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractEnumFields returns only fields with type Enum, in original order")
    void extractEnumFields_shouldReturnOnlyEnumFields_inOrder() {
        
        final FieldDefinition f1 = fieldWithType("Enum");
        final FieldDefinition f2 = fieldWithType("String");
        final FieldDefinition f3 = fieldWithType("enum");
        final FieldDefinition f4 = fieldWithType("Integer");

        final List<FieldDefinition> fields = List.of(f1, f2, f3, f4);

        final List<FieldDefinition> result = FieldUtils.extractEnumFields(fields);

        assertEquals(2, result.size());
        assertSame(f1, result.get(0));
        assertSame(f3, result.get(1));
    }

    @Test
    @DisplayName("extractNamesOfEnumFields returns empty list when there are no Enum fields")
    void extractNamesOfEnumFields_shouldReturnEmpty_whenNoEnumFields() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("field1", "String"),
                fieldWithNameAndType("field2", "Integer")
        );

        final List<String> result = FieldUtils.extractNamesOfEnumFields(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractNamesOfEnumFields returns names of fields with type Enum in original order")
    void extractNamesOfEnumFields_shouldReturnEnumFieldNames_inOrder() {
            
        final FieldDefinition f1 = fieldWithNameAndType("status", "Enum");
            final FieldDefinition f2 = fieldWithNameAndType("description", "String");
            final FieldDefinition f3 = fieldWithNameAndType("type", "enum");
            final FieldDefinition f4 = fieldWithNameAndType("age", "Integer");

        final List<FieldDefinition> fields = List.of(f1, f2, f3, f4);

        final List<String> result = FieldUtils.extractNamesOfEnumFields(fields);

        assertEquals(2, result.size());
        assertEquals("status", result.get(0));
        assertEquals("type", result.get(1));
    }

    @Test
    @DisplayName("extractJsonFields returns empty list when there are no JSON fields")
    void extractJsonFields_shouldReturnEmpty_whenNoJsonFields() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("name", "String"),
                fieldWithNameAndType("age", "Integer")
        );

        final List<FieldDefinition> result = FieldUtils.extractJsonFields(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractJsonFields returns fields with JSON[...] type")
    void extractJsonFields_shouldReturnJsonFields() {
        
        final FieldDefinition f1 = fieldWithNameAndType("metadata", "JSON[String]");
        final FieldDefinition f2 = fieldWithNameAndType("name", "String");
        final FieldDefinition f3 = fieldWithNameAndType("details", "JSONB[MyType]");
        final List<FieldDefinition> fields = List.of(f1, f2, f3);

        final List<FieldDefinition> result = FieldUtils.extractJsonFields(fields);

        assertEquals(2, result.size());
        assertSame(f1, result.get(0));
        assertSame(f3, result.get(1));
    }

    @Test
    @DisplayName("isModelUsedAsJsonField returns false when model is not referenced as JSON field")
    void isModelUsedAsJsonField_shouldReturnFalse_whenNotReferenced() {
        
        final ModelDefinition addressModel = model("Address", List.of());
        final ModelDefinition userModel = model("User", List.of(
                fieldWithNameAndType("name", "String"),
                fieldWithNameAndType("age", "Integer")
        ));
        final ModelDefinition orderModel = model("Order", List.of(
                fieldWithNameAndType("total", "BigDecimal")
        ));

        final List<ModelDefinition> entities = List.of(userModel, orderModel);

        final boolean result = FieldUtils.isModelUsedAsJsonField(addressModel, entities);

        assertFalse(result);
    }

    @Test
    @DisplayName("isModelUsedAsJsonField returns true when model is referenced as JSON[...] type in another model")
    void isModelUsedAsJsonField_shouldReturnTrue_whenReferencedAsJson() {
        
        final ModelDefinition addressModel = model("Address", List.of());
        final FieldDefinition userAddressField = fieldWithNameAndType("address", "JSON[Address]");
        final ModelDefinition userModel = model("User", List.of(userAddressField));
        final List<ModelDefinition> entities = List.of(userModel);

        final boolean result = FieldUtils.isModelUsedAsJsonField(addressModel, entities);

        assertTrue(result);
    }

    @Test
    @DisplayName("isModelUsedAsJsonField returns true when model is referenced as JSONB[...] type in another model")
    void isModelUsedAsJsonField_shouldReturnTrue_whenReferencedAsJsonb() {
        
        final ModelDefinition addressModel = model("Address", List.of());
        final FieldDefinition userAddressField = fieldWithNameAndType("address", "JSONB[Address]");
        final ModelDefinition userModel = model("User", List.of(userAddressField));

        final List<ModelDefinition> entities = List.of(userModel);

        final boolean result = FieldUtils.isModelUsedAsJsonField(addressModel, entities);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldUUID returns false when there are no UUID fields")
    void isAnyFieldUUID_shouldReturnFalse_whenNoUuidFields() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("id", "Long"),
                fieldWithNameAndType("name", "String")
        );

        final boolean result = FieldUtils.isAnyFieldUUID(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldUUID returns true when at least one field is of type UUID")
    void isAnyFieldUUID_shouldReturnTrue_whenUuidFieldPresent() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("id", "UUID"),
                fieldWithNameAndType("name", "String")
        );

        final boolean result = FieldUtils.isAnyFieldUUID(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldUUID ignores case in type comparison")
    void isAnyFieldUUID_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("id", "uuid")
        );

        final boolean result = FieldUtils.isAnyFieldUUID(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldLocalDate returns false when there are no LocalDate fields")
    void isAnyFieldLocalDate_shouldReturnFalse_whenNoLocalDateFields() {
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("createdAt", "LocalDateTime"),
                fieldWithNameAndType("name", "String")
        );

        final boolean result = FieldUtils.isAnyFieldLocalDate(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldLocalDate returns true when at least one field is of type LocalDate")
    void isAnyFieldLocalDate_shouldReturnTrue_whenLocalDateFieldPresent() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("birthDate", "LocalDate"),
                fieldWithNameAndType("name", "String")
        );

        final boolean result = FieldUtils.isAnyFieldLocalDate(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldLocalDate ignores case in type comparison")
    void isAnyFieldLocalDate_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("birthDate", "localdate")
        );

        final boolean result = FieldUtils.isAnyFieldLocalDate(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldLocalDateTime returns false when there are no LocalDateTime fields")
    void isAnyFieldLocalDateTime_shouldReturnFalse_whenNoLocalDateTimeFields() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("birthDate", "LocalDate"),
                fieldWithNameAndType("name", "String")
        );

        final boolean result = FieldUtils.isAnyFieldLocalDateTime(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldLocalDateTime returns true when at least one field is of type LocalDateTime")
    void isAnyFieldLocalDateTime_shouldReturnTrue_whenLocalDateTimeFieldPresent() {
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("createdAt", "LocalDateTime"),
                fieldWithNameAndType("name", "String")
        );

        final boolean result = FieldUtils.isAnyFieldLocalDateTime(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldLocalDateTime ignores case in type comparison")
    void isAnyFieldLocalDateTime_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("createdAt", "localdatetime")
        );

        final boolean result = FieldUtils.isAnyFieldLocalDateTime(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldBigDecimal returns false when there are no BigDecimal fields")
    void isAnyFieldBigDecimal_shouldReturnFalse_whenNoBigDecimalFields() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("price", "Double"),
                fieldWithNameAndType("quantity", "Integer")
        );

        final boolean result = FieldUtils.isAnyFieldBigDecimal(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldBigDecimal returns true when at least one field is of type BigDecimal")
    void isAnyFieldBigDecimal_shouldReturnTrue_whenBigDecimalFieldPresent() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("price", "BigDecimal"),
                fieldWithNameAndType("quantity", "Integer")
        );

        final boolean result = FieldUtils.isAnyFieldBigDecimal(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldBigDecimal ignores case in type comparison")
    void isAnyFieldBigDecimal_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("price", "bigdecimal")
        );

        final boolean result = FieldUtils.isAnyFieldBigDecimal(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldBigInteger returns false when there are no BigInteger fields")
    void isAnyFieldBigInteger_shouldReturnFalse_whenNoBigIntegerFields() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("id", "Long"),
                fieldWithNameAndType("value", "Integer")
        );

        final boolean result = FieldUtils.isAnyFieldBigInteger(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldBigInteger returns true when at least one field is of type BigInteger")
    void isAnyFieldBigInteger_shouldReturnTrue_whenBigIntegerFieldPresent() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("id", "BigInteger"),
                fieldWithNameAndType("value", "Integer")
        );

        final boolean result = FieldUtils.isAnyFieldBigInteger(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldBigInteger ignores case in type comparison")
    void isAnyFieldBigInteger_shouldIgnoreCase() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("id", "biginteger")
        );

        final boolean result = FieldUtils.isAnyFieldBigInteger(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("extractIdField returns first field with non-null id")
    void extractIdField_shouldReturnFirstFieldWithId() {
        
        final FieldDefinition f1 = fieldWithNameTypeAndId("id1", "Long", true);
        final FieldDefinition f2 = fieldWithNameTypeAndId("id2", "Long", true);
        final List<FieldDefinition> fields = List.of(f1, f2);

        final FieldDefinition result = FieldUtils.extractIdField(fields);

        assertSame(f1, result);
    }

    @Test
    @DisplayName("extractIdField skips fields without id and returns the first one with id")
    void extractIdField_shouldSkipNonIdFields() {
        
        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition f2 = fieldWithNameTypeAndId("id", "Long", true);
        final FieldDefinition f3 = fieldWithNameTypeAndId("code", "String", false);
        final List<FieldDefinition> fields = List.of(f1, f2, f3);

        final FieldDefinition result = FieldUtils.extractIdField(fields);

        assertSame(f2, result);
    }

    @Test
    @DisplayName("extractIdField throws IllegalArgumentException when no ID field is found")
    void extractIdField_shouldThrow_whenNoIdFieldFound() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameTypeAndId("name", "String", false),
                fieldWithNameTypeAndId("code", "String", false)
        );

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.extractIdField(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("extractNonIdFieldNames returns names of all fields except the ID field")
    void extractNonIdFieldNames_shouldReturnAllNamesExceptId() {
     
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition f2 = fieldWithNameTypeAndId("age", "Integer", false);
        final List<FieldDefinition> fields = List.of(idField, f1, f2);

        final List<String> result = FieldUtils.extractNonIdFieldNames(fields);

        assertEquals(2, result.size());
        assertEquals("name", result.get(0));
        assertEquals("age", result.get(1));
    }

    @Test
    @DisplayName("extractNonIdFieldNames works when ID field is not the first in the list")
    void extractNonIdFieldNames_shouldWork_whenIdNotFirst() {
        
        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        final FieldDefinition f2 = fieldWithNameTypeAndId("age", "Integer", false);
        final List<FieldDefinition> fields = List.of(f1, idField, f2);

        final List<String> result = FieldUtils.extractNonIdFieldNames(fields);

        assertEquals(2, result.size());
        assertEquals("name", result.get(0));
        assertEquals("age", result.get(1));
    }

    @Test
    @DisplayName("extractNonIdFieldNames throws IllegalArgumentException when no ID field is found")
    void extractNonIdFieldNames_shouldThrow_whenNoIdFieldFound() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithNameTypeAndId("name", "String", false),
                fieldWithNameTypeAndId("age", "Integer", false)
        );

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.extractNonIdFieldNames(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNames returns names of fields that are not ID and have no relation")
    void extractNonIdNonRelationFieldNames_shouldReturnNonIdNonRelationNames() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition relatedField = fieldWithNameTypeAndId("owner", "String", false);
        relatedField.setRelation(new RelationDefinition());

        final List<FieldDefinition> fields = List.of(idField, nameField, relatedField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNames(fields);

        assertEquals(1, result.size());
        assertEquals("name", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNames throws IllegalArgumentException when no ID field is found")
    void extractNonIdNonRelationFieldNames_shouldThrow_whenNoIdFieldFound() {
        
        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition f2 = fieldWithNameTypeAndId("age", "Integer", false);
        final List<FieldDefinition> fields = List.of(f1, f2);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.extractNonIdNonRelationFieldNames(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("extractNonRelationNonEnumAndNonJsonFieldNames returns only fields without relation, enum and json")
    void extractNonRelationNonEnumAndNonJsonFieldNames_shouldReturnOnlyPlainFields() {
        
        final FieldDefinition plainField = fieldWithNameAndType("name", "String");
        plainField.setRelation(null);

        final FieldDefinition relatedField = fieldWithRelation("owner", "String", "ManyToOne");
        final FieldDefinition enumField = fieldWithNameAndType("status", "enum");
        enumField.setRelation(null);

        final FieldDefinition jsonField = fieldWithNameAndType("metadata", "JSON[Metadata]");
        jsonField.setRelation(null);

        final List<FieldDefinition> fields = List.of(
                plainField, relatedField, enumField, jsonField
        );

        final List<String> result = FieldUtils.extractNonRelationNonEnumAndNonJsonFieldNames(fields);

        assertEquals(1, result.size());
        assertEquals("name", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForController returns body.fieldName() for simple field when swagger=false")
    void extractNonIdNonRelationFieldNamesForController_shouldUseDirectBodyAccess_whenNonSwagger() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, false);

        assertEquals(1, result.size());
        assertEquals("body.name()", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForController returns body.getFieldName() for simple field when swagger=true")
    void extractNonIdNonRelationFieldNamesForController_shouldUseGetter_whenSwagger() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, true);

        assertEquals(1, result.size());
        assertEquals("body.getName()", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForController maps JSON field using mapper when swagger=false")
    void extractNonIdNonRelationFieldNamesForController_shouldMapJsonField_whenNonSwagger() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition addressField = fieldWithNameAndType("address", "JSON[Address]");
        addressField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, addressField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, false);

        assertEquals(1, result.size());
        assertEquals("addressMapper.mapAddressTOToAddress(body.address())", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForController maps JSON field using mapper when swagger=true")
    void extractNonIdNonRelationFieldNamesForController_shouldMapJsonField_whenSwagger() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition addressField = fieldWithNameAndType("address", "JSON[Address]");
        addressField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, addressField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, true);

        assertEquals(1, result.size());
        final String mapping = result.get(0);

        assertTrue(mapping.startsWith("addressMapper.map"));
        assertTrue(mapping.endsWith("(body.getAddress())"));
        assertTrue(mapping.contains("To")); // negde u sredini
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForController maps Enum field correctly when swagger=true")
    void extractNonIdNonRelationFieldNamesForController_shouldMapEnumField_whenSwagger() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition statusField = fieldWithNameAndType("status", "enum");
        statusField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, statusField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, true);

        assertEquals(1, result.size());
        assertEquals(
                "body.getStatus() != null ? StatusEnum.valueOf(body.getStatus().name()) : null",
                result.get(0)
        );
    }

}
