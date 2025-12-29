package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.codegen.models.ColumnDefinition;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.IdDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.RelationDefinition;

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

    private FieldDefinition fieldWithNameAndRelation(final String name, final String type, final String cascade, final String fetch) {
        
        final RelationDefinition relation = new RelationDefinition();
        relation.setType(type);
        relation.setCascade(cascade);
        relation.setFetch(fetch);

        final FieldDefinition field = new FieldDefinition();
        field.setName(name);
        field.setRelation(relation);
        
        return field;
    }

    private FieldDefinition fieldWithNameTypeAndRelation(final String name, final String fieldType, final String relationType,
                final String cascade, final String fetch) {
        
        final RelationDefinition relation = new RelationDefinition();
        relation.setType(relationType);
        relation.setCascade(cascade);
        relation.setFetch(fetch);

        final FieldDefinition field = new FieldDefinition();
        field.setName(name);
        field.setRelation(relation);
        field.setType(fieldType);
        
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
                fieldWithType("JSON<String>")
        );

        final boolean result = FieldUtils.isAnyFieldJson(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldJson returns true when at least one field is of JSONB type (JSONB[...])")
    void isAnyFieldJson_shouldReturnTrue_whenJsonbTypePresent() {
        
        final List<FieldDefinition> fields = List.of(
                fieldWithType("JSONB<MyType>"),
                fieldWithType("Integer")
        );

        final boolean result = FieldUtils.isAnyFieldJson(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldSimpleCollection: empty list -> false")
    void isAnyFieldSimpleCollection_emptyList_false() {
        assertFalse(FieldUtils.isAnyFieldSimpleCollection(Collections.emptyList()));
    }

    @Test
    @DisplayName("isAnyFieldSimpleCollection: no simple collection fields -> false")
    void isAnyFieldSimpleCollection_noSimpleCollections_false() {
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("name", "String"),
                fieldWithNameAndType("age", "Integer")
        );

        assertFalse(FieldUtils.isAnyFieldSimpleCollection(fields));
    }

    @Test
    @DisplayName("isAnyFieldSimpleCollection: contains List<T> field without relation -> true")
    void isAnyFieldSimpleCollection_listField_true() {
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("name", "String"),
                fieldWithNameAndType("phoneNumbers", "List<String>")
        );

        assertTrue(FieldUtils.isAnyFieldSimpleCollection(fields));
    }

    @Test
    @DisplayName("isAnyFieldSimpleCollection: contains Set<T> field without relation -> true")
    void isAnyFieldSimpleCollection_setField_true() {
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("roles", "Set<String>")
        );

        assertTrue(FieldUtils.isAnyFieldSimpleCollection(fields));
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
        
        final FieldDefinition f1 = fieldWithNameAndType("metadata", "JSON<String>");
        final FieldDefinition f2 = fieldWithNameAndType("name", "String");
        final FieldDefinition f3 = fieldWithNameAndType("details", "JSONB<MyType>");
        final List<FieldDefinition> fields = List.of(f1, f2, f3);

        final List<FieldDefinition> result = FieldUtils.extractJsonFields(fields);

        assertEquals(2, result.size());
        assertSame(f1, result.get(0));
        assertSame(f3, result.get(1));
    }

    @Test
    @DisplayName("extractSimpleCollectionFields: empty input -> empty output")
    void extractSimpleCollectionFields_empty_returnsEmpty() {
        final List<FieldDefinition> result = FieldUtils.extractSimpleCollectionFields(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractSimpleCollectionFields: no simple collection fields -> empty output")
    void extractSimpleCollectionFields_noSimpleCollections_returnsEmpty() {
        final List<FieldDefinition> fields = List.of(
                fieldWithNameAndType("name", "String"),
                fieldWithNameAndType("age", "Integer")
        );

        final List<FieldDefinition> result = FieldUtils.extractSimpleCollectionFields(fields);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractSimpleCollectionFields: returns only List<T>/Set<T> simple collection fields, preserving order")
    void extractSimpleCollectionFields_filtersAndPreservesOrder() {
        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        final FieldDefinition f2 = fieldWithNameAndType("phoneNumbers", "List<String>");
        final FieldDefinition f3 = fieldWithNameAndType("roles", "Set<String>");
        final FieldDefinition f4 = fieldWithNameAndType("createdAt", "LocalDateTime");

        final List<FieldDefinition> fields = List.of(f1, f2, f3, f4);

        final List<FieldDefinition> result = FieldUtils.extractSimpleCollectionFields(fields);

        assertEquals(2, result.size());
        assertSame(f2, result.get(0), "Expected List field first");
        assertSame(f3, result.get(1), "Expected Set field second");
    }

    @Test
    @DisplayName("extractSimpleCollectionFields: should return new list instance (not same as input)")
    void extractSimpleCollectionFields_returnsNewListInstance() {
        final List<FieldDefinition> input = List.of(
                fieldWithNameAndType("phoneNumbers", "List<String>")
        );

        final List<FieldDefinition> result = FieldUtils.extractSimpleCollectionFields(input);

        assertNotSame(input, result);
        assertEquals(1, result.size());
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
        final FieldDefinition userAddressField = fieldWithNameAndType("address", "JSON<Address>");
        final ModelDefinition userModel = model("User", List.of(userAddressField));
        final List<ModelDefinition> entities = List.of(userModel);

        final boolean result = FieldUtils.isModelUsedAsJsonField(addressModel, entities);

        assertTrue(result);
    }

    @Test
    @DisplayName("isModelUsedAsJsonField returns true when model is referenced as JSONB[...] type in another model")
    void isModelUsedAsJsonField_shouldReturnTrue_whenReferencedAsJsonb() {
        
        final ModelDefinition addressModel = model("Address", List.of());
        final FieldDefinition userAddressField = fieldWithNameAndType("address", "JSONB<Address>");
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

        final FieldDefinition relatedField = fieldWithRelation("OneToOne", "ALL", "EAGER");
        final FieldDefinition enumField = fieldWithNameAndType("status", "enum");
        enumField.setRelation(null);

        final FieldDefinition jsonField = fieldWithNameAndType("metadata", "JSON<Metadata>");
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

        final FieldDefinition addressField = fieldWithNameAndType("address", "JSON<Address>");
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

        final FieldDefinition addressField = fieldWithNameAndType("address", "JSON<Address>");
        addressField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, addressField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, true);

        assertEquals(1, result.size());
        final String mapping = result.get(0);

        assertTrue(mapping.startsWith("addressMapper.map"));
        assertTrue(mapping.endsWith("(body.getAddress())"));
        assertTrue(mapping.contains("To"));
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

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForResolver returns input.fieldName() for plain fields without relations or JSON")
    void extractNonIdNonRelationFieldNamesForResolver_shouldReturnSimpleInputAccess_forPlainFields() {
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition relatedField = fieldWithNameAndRelation("owner" ,"OneToOne", "ALL", "ManyToOne");

        final List<FieldDefinition> fields = List.of(idField, nameField, relatedField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForResolver(fields);

        assertEquals(1, result.size());
        assertEquals("input.name()", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForResolver uses mapper for JSON fields")
    void extractNonIdNonRelationFieldNamesForResolver_shouldUseMapper_forJsonField() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition addressField = fieldWithNameAndType("address", "JSON<Address>");
        addressField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, addressField);

        final List<String> result = FieldUtils.extractNonIdNonRelationFieldNamesForResolver(fields);

        assertEquals(1, result.size());
        assertEquals("addressMapper.mapAddressTOToAddress(input.address())", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdNonRelationFieldNamesForResolver throws IllegalArgumentException when no ID field exists")
    void extractNonIdNonRelationFieldNamesForResolver_shouldThrow_whenNoIdField() {
        
        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition f2 = fieldWithNameTypeAndId("age", "Integer", false);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("extractNonIdFieldNamesForResolver returns input.fieldName() for plain fields")
    void extractNonIdFieldNamesForResolver_shouldReturnSimpleInputAccess_forPlainFields() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField);

        final List<String> result = FieldUtils.extractNonIdFieldNamesForResolver(fields);

        assertEquals(1, result.size());
        assertEquals("input.name()", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdFieldNamesForResolver uses mapper for JSON fields")
    void extractNonIdFieldNamesForResolver_shouldUseMapper_forJsonField() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition addressField = fieldWithNameAndType("address", "JSON<Address>");
        addressField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, addressField);

        final List<String> result = FieldUtils.extractNonIdFieldNamesForResolver(fields);

        assertEquals(1, result.size());
        assertEquals("addressMapper.mapAddressTOToAddress(input.address())", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdFieldNamesForResolver uses input.fieldNameId() for ManyToOne relations")
    void extractNonIdFieldNamesForResolver_shouldUseSingleId_forManyToOneRelation() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition ownerField = fieldWithNameTypeAndRelation(
                "owner", "OwnerModel", "ManyToOne", "ALL", "Eager"
        );

        final List<FieldDefinition> fields = List.of(idField, ownerField);

        final List<String> result = FieldUtils.extractNonIdFieldNamesForResolver(fields);

        assertEquals(1, result.size());
        assertEquals("input.ownerId()", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdFieldNamesForResolver uses input.fieldNameIds() for ToMany relations (OneToMany/ManyToMany)")
    void extractNonIdFieldNamesForResolver_shouldUseIds_forToManyRelations() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition tagsField = fieldWithNameTypeAndRelation("tags", "Tag", "OneToMany", "ALL", "Eager");
        final FieldDefinition groupsField = fieldWithNameTypeAndRelation("groups", "Group", "ManyToMany", "ALL", "Eager");

        final List<FieldDefinition> fields = List.of(idField, tagsField, groupsField);

        final List<String> result = FieldUtils.extractNonIdFieldNamesForResolver(fields);

        assertEquals(2, result.size());
        assertEquals("input.tagsIds()", result.get(0));
        assertEquals("input.groupsIds()", result.get(1));
    }

    @Test
    @DisplayName("extractNonIdFieldNamesForResolver throws IllegalArgumentException when no ID field exists")
    void extractNonIdFieldNamesForResolver_shouldThrow_whenNoIdField() {
        
        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition f2 = fieldWithNameTypeAndId("age", "Integer", false);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.extractNonIdFieldNamesForResolver(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("extractNonIdFieldForJavadoc generates @param tags for all non-ID fields with descriptions")
    void extractNonIdFieldForJavadoc_shouldReturnParamTags_forFieldsWithDescription() {
        
        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setDescription("Primary key");

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setDescription("The name of the entity");

        final FieldDefinition noDescField = fieldWithNameTypeAndId("age", "Integer", false);
        noDescField.setDescription("   ");

        final List<FieldDefinition> fields = List.of(idField, nameField, noDescField);

        final List<String> result = FieldUtils.extractNonIdFieldForJavadoc(fields);

        assertEquals(1, result.size());
        assertEquals("@param name The name of the entity", result.get(0));
    }

    @Test
    @DisplayName("extractNonIdFieldForJavadoc throws IllegalArgumentException when no ID field exists")
    void extractNonIdFieldForJavadoc_shouldThrow_whenNoIdField() {
        
        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setDescription("Name");

        final List<FieldDefinition> fields = List.of(nameField);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.extractNonIdFieldForJavadoc(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("extractFieldForJavadocWithoutRelations generates @param tags for fields without relations and with descriptions")
    void extractFieldForJavadocWithoutRelations_shouldReturnParamTags_forFieldsWithoutRelationsAndWithDescription() {
        
        final FieldDefinition nameField = fieldWithNameAndType("name", "String");
        nameField.setDescription("The name of the entity");
        nameField.setRelation(null);
        
        final FieldDefinition ageField = fieldWithNameAndType("age", "Integer");
        ageField.setDescription("   ");
        ageField.setRelation(null);

        final FieldDefinition relatedField = fieldWithRelation("owner", "Owner", "ManyToOne");
        relatedField.setDescription("The owner");

        final List<FieldDefinition> fields = List.of(nameField, ageField, relatedField);

        final List<String> result = FieldUtils.extractFieldForJavadocWithoutRelations(fields);

        assertEquals(1, result.size());
        assertEquals("@param name The name of the entity", result.get(0));
    }

    @Test
    @DisplayName("computeJavadocForFields returns empty list when all descriptions are blank")
    void computeJavadocForFields_shouldReturnEmpty_whenAllDescriptionsBlank() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setDescription("   ");

        final FieldDefinition f2 = fieldWithNameAndType("age", "Integer");
        f2.setDescription(null);

        final List<String> result = FieldUtils.computeJavadocForFields(f1, f2);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("computeJavadocForFields returns @param tags only for fields with non-blank description")
    void computeJavadocForFields_shouldReturnParamTags_forFieldsWithDescription() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setDescription("The name of the entity");

        final FieldDefinition f2 = fieldWithNameAndType("age", "Integer");
        f2.setDescription("   ");

        final FieldDefinition f3 = fieldWithNameAndType("status", "String");
        f3.setDescription("The status");

        final List<String> result = FieldUtils.computeJavadocForFields(f1, f2, f3);

        assertEquals(2, result.size());
        assertEquals("@param name The name of the entity", result.get(0));
        assertEquals("@param status The status", result.get(1));
    }

    @Test
    @DisplayName("extractFieldNames returns all field names in original order")
    void extractFieldNames_shouldReturnAllNames_inOrder() {

        final FieldDefinition f1 = fieldWithNameAndType("id", "Long");
        final FieldDefinition f2 = fieldWithNameAndType("name", "String");
        final FieldDefinition f3 = fieldWithNameAndType("age", "Integer");

        final List<FieldDefinition> fields = List.of(f1, f2, f3);

        final List<String> result = FieldUtils.extractFieldNames(fields);

        assertEquals(3, result.size());
        assertEquals("id", result.get(0));
        assertEquals("name", result.get(1));
        assertEquals("age", result.get(2));
    }

    @Test
    @DisplayName("extractFieldNamesWithoutRelations excludes OneToMany and ManyToMany relation fields")
    void extractFieldNamesWithoutRelations_shouldExcludeCollectionRelations() {

        final FieldDefinition idField = fieldWithoutRelation();
        idField.setName("id");

        final FieldDefinition nameField = fieldWithoutRelation();
        nameField.setName("name");

        final FieldDefinition ownerField = fieldWithNameAndRelation("owner", "ManyToOne", "ALL", "EAGER");
        final FieldDefinition tagsField = fieldWithNameAndRelation("tags", "OneToMany", "ALL", "EAGER");
        final FieldDefinition groupsField = fieldWithNameAndRelation("groups", "ManyToMany", "ALL", "EAGER");

        final List<FieldDefinition> fields = List.of(idField, nameField, ownerField, tagsField, groupsField);

        final List<String> result = FieldUtils.extractFieldNamesWithoutRelations(fields);

        assertEquals(3, result.size());
        assertEquals("id", result.get(0));
        assertEquals("name", result.get(1));
        assertEquals("owner", result.get(2));
    }

    @Test
    @DisplayName("extractFieldNamesWithoutRelations returns empty list when all fields are OneToMany or ManyToMany")
    void extractFieldNamesWithoutRelations_shouldReturnEmpty_whenAllCollectionRelations() {

        final FieldDefinition tagsField = fieldWithNameAndRelation("tags", "OneToMany", "ALL", "EAGER");
        final FieldDefinition groupsField = fieldWithNameAndRelation("groups", "ManyToMany", "ALL", "EAGER");

        final List<FieldDefinition> fields = List.of(tagsField, groupsField);

        final List<String> result = FieldUtils.extractFieldNamesWithoutRelations(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractCollectionRelationNames returns names of OneToMany and ManyToMany relation fields")
    void extractCollectionRelationNames_shouldReturnCollectionRelationNames() {

        final FieldDefinition idField = fieldWithoutRelation();
        idField.setName("id");

        final FieldDefinition tagsField = fieldWithNameAndRelation("tags", "OneToMany", "ALL", "EAGER");
        final FieldDefinition groupsField = fieldWithNameAndRelation("groups", "ManyToMany", "ALL", "EAGER");
        final FieldDefinition ownerField = fieldWithNameAndRelation("owner", "ManyToOne", "ALL", "EAGER");
        final FieldDefinition nameField = fieldWithoutRelation();
        nameField.setName("name");

        final ModelDefinition model = model("Example", List.of(idField, tagsField, groupsField, ownerField, nameField));

        final List<String> result = FieldUtils.extractCollectionRelationNames(model);

        assertEquals(2, result.size());
        assertEquals("tags", result.get(0));
        assertEquals("groups", result.get(1));
    }

    @Test
    @DisplayName("extractCollectionRelationNames returns empty list when model has no collection relations")
    void extractCollectionRelationNames_shouldReturnEmpty_whenNoCollectionRelations() {

        final FieldDefinition idField = fieldWithoutRelation();
        idField.setName("id");

        final FieldDefinition ownerField = fieldWithNameAndRelation("owner", "ManyToOne", "ALL", "EAGER");
        final FieldDefinition nameField = fieldWithoutRelation();
        nameField.setName("name");

        final ModelDefinition model = model("Example", List.of(idField, ownerField, nameField));

        final List<String> result = FieldUtils.extractCollectionRelationNames(model);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("hasCollectionRelation returns false when there are no relations")
    void hasCollectionRelation_shouldReturnFalse_whenNoRelations() {

        final ModelDefinition target = model("Tag", List.of());

        final FieldDefinition f1 = fieldWithoutRelation();
        f1.setName("id");
        final FieldDefinition f2 = fieldWithoutRelation();
        f2.setName("name");

        final ModelDefinition other = model("Post", List.of(f1, f2));

        final boolean result = FieldUtils.hasCollectionRelation(target, List.of(other));

        assertFalse(result);
    }

    @Test
    @DisplayName("hasCollectionRelation returns false when there are only non-collection relations")
    void hasCollectionRelation_shouldReturnFalse_whenOnlyNonCollectionRelations() {

        final ModelDefinition target = model("Tag", List.of());

        final FieldDefinition relationField = fieldWithNameTypeAndRelation(
                "tag", "Tag", "ManyToOne", "ALL", "EAGER"
        );

        final ModelDefinition other = model("Post", List.of(relationField));

        final boolean result = FieldUtils.hasCollectionRelation(target, List.of(other));

        assertFalse(result);
    }

    @Test
    @DisplayName("hasCollectionRelation returns true when there is a OneToMany relation to the target model")
    void hasCollectionRelation_shouldReturnTrue_whenOneToManyRelationExists() {

        final ModelDefinition target = model("Tag", List.of());

        final FieldDefinition relationField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );

        final ModelDefinition other = model("Post", List.of(relationField));

        final boolean result = FieldUtils.hasCollectionRelation(target, List.of(other));

        assertTrue(result);
    }

    @Test
    @DisplayName("hasCollectionRelation returns true when there is a ManyToMany relation to the target model")
    void hasCollectionRelation_shouldReturnTrue_whenManyToManyRelationExists() {

        final ModelDefinition target = model("Tag", List.of());

        final FieldDefinition relationField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "ManyToMany", "ALL", "EAGER"
        );

        final ModelDefinition other = model("Post", List.of(relationField));

        final boolean result = FieldUtils.hasCollectionRelation(target, List.of(other));

        assertTrue(result);
    }

    @Test
    @DisplayName("hasRelation returns false when there are no relations")
    void hasRelation_shouldReturnFalse_whenNoRelations() {

        final ModelDefinition target = model("Category", List.of());

        final FieldDefinition f1 = fieldWithoutRelation();
        f1.setName("id");
        final FieldDefinition f2 = fieldWithoutRelation();
        f2.setName("name");

        final ModelDefinition other = model("Product", List.of(f1, f2));

        final boolean result = FieldUtils.hasRelation(target, List.of(other));

        assertFalse(result);
    }

    @Test
    @DisplayName("hasRelation returns false when relations are to a different model")
    void hasRelation_shouldReturnFalse_whenRelationsPointToAnotherModel() {

        final ModelDefinition target = model("Category", List.of());

        final FieldDefinition relationField = fieldWithNameTypeAndRelation(
                "supplier", "Supplier", "ManyToOne", "ALL", "EAGER"
        );

        final ModelDefinition other = model("Product", List.of(relationField));

        final boolean result = FieldUtils.hasRelation(target, List.of(other));

        assertFalse(result);
    }

    @Test
    @DisplayName("hasRelation returns true when any relation points to the target model")
    void hasRelation_shouldReturnTrue_whenRelationToTargetExists() {

        final ModelDefinition target = model("Category", List.of());

        final FieldDefinition relationField = fieldWithNameTypeAndRelation(
                "category", "Category", "ManyToOne", "ALL", "EAGER"
        );

        final ModelDefinition other = model("Product", List.of(relationField));

        final boolean result = FieldUtils.hasRelation(target, List.of(other));

        assertTrue(result);
    }

    @Test
    @DisplayName("generateInputArgsBusinessService returns field names for non-relation fields excluding ID")
    void generateInputArgsBusinessService_shouldReturnNames_forPlainFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);
        ageField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField, ageField);

        final List<String> result = FieldUtils.generateInputArgsBusinessService(fields);

        assertEquals(2, result.size());
        assertEquals("name", result.get(0));
        assertEquals("age", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsBusinessService uses uncapitalized type name for ManyToOne/OneToOne relations")
    void generateInputArgsBusinessService_shouldUseSingularName_forToOneRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);

        final FieldDefinition categoryField = fieldWithNameTypeAndRelation(
                "category", "Category", "ManyToOne", "ALL", "EAGER"
        );

        final FieldDefinition ownerField = fieldWithNameTypeAndRelation(
                "owner", "UserModel", "OneToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, categoryField, ownerField);

        final List<String> result = FieldUtils.generateInputArgsBusinessService(fields);

        assertEquals(2, result.size());
        assertEquals("category", result.get(0));
        assertEquals("userModel", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsBusinessService appends 's' for OneToMany and ManyToMany relations")
    void generateInputArgsBusinessService_shouldUsePluralName_forToManyRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);

        final FieldDefinition tagsField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );

        final FieldDefinition groupsField = fieldWithNameTypeAndRelation(
                "groups", "Group", "ManyToMany", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, tagsField, groupsField);

        final List<String> result = FieldUtils.generateInputArgsBusinessService(fields);

        assertEquals(2, result.size());
        assertEquals("tags", result.get(0));
        assertEquals("groups", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsBusinessService throws IllegalArgumentException when no ID field exists")
    void generateInputArgsBusinessService_shouldThrow_whenNoIdField() {

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);

        final List<FieldDefinition> fields = List.of(nameField, ageField);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.generateInputArgsBusinessService(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId returns 'final <type> <name>' for plain fields excluding ID")
    void generateInputArgsExcludingId_shouldReturnFinalTypeAndName_forPlainFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);
        final List<FieldDefinition> fields = List.of(idField, nameField, ageField);

        final List<ModelDefinition> entities = List.of();

        final List<String> result = FieldUtils.generateInputArgsExcludingId(fields, entities);

        assertEquals(2, result.size());
        assertEquals("final String name", result.get(0));
        assertEquals("final Integer age", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId generates single ID argument for ManyToOne/OneToOne relations")
    void generateInputArgsExcludingId_shouldGenerateSingleId_forToOneRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        final FieldDefinition categoryField = fieldWithNameTypeAndRelation(
                "category", "Category", "ManyToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> mainFields = List.of(idField, categoryField);
        final FieldDefinition categoryIdField = fieldWithNameTypeAndId("id", "UUID", true);
        final ModelDefinition categoryModel = model("Category", List.of(categoryIdField));
        final List<ModelDefinition> entities = List.of(categoryModel);

        final List<String> result = FieldUtils.generateInputArgsExcludingId(mainFields, entities);

        assertEquals(1, result.size());
        assertEquals("final UUID categoryId", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId generates list of IDs for OneToMany/ManyToMany relations")
    void generateInputArgsExcludingId_shouldGenerateListOfIds_forToManyRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        final FieldDefinition tagsField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );
        final FieldDefinition groupsField = fieldWithNameTypeAndRelation(
                "groups", "Group", "ManyToMany", "ALL", "EAGER"
        );

        final List<FieldDefinition> mainFields = List.of(idField, tagsField, groupsField);

        final FieldDefinition tagIdField = fieldWithNameTypeAndId("id", "Long", true);
        final ModelDefinition tagModel = model("Tag", List.of(tagIdField));
        final FieldDefinition groupIdField = fieldWithNameTypeAndId("id", "UUID", true);
        final ModelDefinition groupModel = model("Group", List.of(groupIdField));

        final List<ModelDefinition> entities = List.of(tagModel, groupModel);

        final List<String> result = FieldUtils.generateInputArgsExcludingId(mainFields, entities);

        assertEquals(2, result.size());
        assertEquals("final List<Long> tagIds", result.get(0));
        assertEquals("final List<UUID> groupIds", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId throws IllegalArgumentException when no ID field exists")
    void generateInputArgsExcludingId_shouldThrow_whenNoIdField() {

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);

        final List<FieldDefinition> fields = List.of(nameField);
        final List<ModelDefinition> entities = List.of();

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.generateInputArgsExcludingId(fields, entities)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("generateInputArgsExcludingIdForTest returns plain field names for non-relation fields excluding ID")
    void generateInputArgsExcludingIdForTest_shouldReturnPlainNames_forNonRelationFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);
        ageField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField, ageField);
        final List<ModelDefinition> entities = List.of();

        final List<String> result = FieldUtils.generateInputArgsExcludingIdForTest(fields, entities);

        assertEquals(2, result.size());
        assertEquals("name", result.get(0));
        assertEquals("age", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsExcludingIdForTest generates '<modelName>Id' for ManyToOne/OneToOne relations")
    void generateInputArgsExcludingIdForTest_shouldGenerateSingleId_forToOneRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition categoryField = fieldWithNameTypeAndRelation(
                "category", "Category", "ManyToOne", "ALL", "EAGER"
        );

        final FieldDefinition ownerField = fieldWithNameTypeAndRelation(
                "owner", "User", "OneToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, categoryField, ownerField);

        final ModelDefinition categoryModel = model("Category", List.of());
        final ModelDefinition userModel = model("User", List.of());

        final List<ModelDefinition> entities = List.of(categoryModel, userModel);

        final List<String> result = FieldUtils.generateInputArgsExcludingIdForTest(fields, entities);

        assertEquals(2, result.size());
        assertEquals("categoryId", result.get(0));
        assertEquals("userId", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsExcludingIdForTest generates '<modelName>Ids' for OneToMany and ManyToMany relations")
    void generateInputArgsExcludingIdForTest_shouldGenerateIds_forToManyRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition tagsField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );

        final FieldDefinition groupsField = fieldWithNameTypeAndRelation(
                "groups", "Group", "ManyToMany", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, tagsField, groupsField);

        final ModelDefinition tagModel = model("Tag", List.of());
        final ModelDefinition groupModel = model("Group", List.of());

        final List<ModelDefinition> entities = List.of(tagModel, groupModel);

        final List<String> result = FieldUtils.generateInputArgsExcludingIdForTest(fields, entities);

        assertEquals(2, result.size());
        assertEquals("tagIds", result.get(0));
        assertEquals("groupIds", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsExcludingIdForTest throws IllegalArgumentException when no ID field exists")
    void generateInputArgsExcludingIdForTest_shouldThrow_whenNoIdField() {

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);

        final List<FieldDefinition> fields = List.of(nameField, ageField);
        final List<ModelDefinition> entities = List.of();

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.generateInputArgsExcludingIdForTest(fields, entities)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId(List) returns 'final <type> <name>' for plain fields excluding ID")
    void generateInputArgsExcludingIdSingleParam_shouldReturnFinalTypeAndName_forPlainFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);
        ageField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField, ageField);

        final List<String> result = FieldUtils.generateInputArgsExcludingId(fields);

        assertEquals(2, result.size());
        assertEquals("final String name", result.get(0));
        assertEquals("final Integer age", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId(List) uses 'final <type> <name>' for ManyToOne/OneToOne relations")
    void generateInputArgsExcludingIdSingleParam_shouldUseFinalType_forToOneRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition ownerField = fieldWithNameTypeAndRelation(
                "owner", "OwnerModel", "ManyToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, ownerField);

        final List<String> result = FieldUtils.generateInputArgsExcludingId(fields);

        assertEquals(1, result.size());
        assertEquals("final OwnerModel owner", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId(List) uses 'final List<type> name' for OneToMany and ManyToMany relations")
    void generateInputArgsExcludingIdSingleParam_shouldUseListType_forToManyRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition tagsField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );
        final FieldDefinition groupsField = fieldWithNameTypeAndRelation(
                "groups", "Group", "ManyToMany", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, tagsField, groupsField);

        final List<String> result = FieldUtils.generateInputArgsExcludingId(fields);

        assertEquals(2, result.size());
        assertEquals("final List<Tag> tags", result.get(0));
        assertEquals("final List<Group> groups", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsExcludingId(List) throws IllegalArgumentException when no ID field exists")
    void generateInputArgsExcludingIdSingleParam_shouldThrow_whenNoIdField() {

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);

        final List<FieldDefinition> fields = List.of(nameField);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.generateInputArgsExcludingId(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("generateInputArgsWithoutRelations returns 'final <type> <name>' only for fields without relations")
    void generateInputArgsWithoutRelations_shouldReturnOnlyFieldsWithoutRelations() {

        final FieldDefinition idField = fieldWithNameAndType("id", "Long");
        final FieldDefinition nameField = fieldWithNameAndType("name", "String");

        final FieldDefinition relatedField = fieldWithNameTypeAndRelation(
                "owner", "OwnerModel", "ManyToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, nameField, relatedField);

        final List<String> result = FieldUtils.generateInputArgsWithoutRelations(fields);

        assertEquals(2, result.size());
        assertEquals("final Long id", result.get(0));
        assertEquals("final String name", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsWithoutRelations returns empty list when all fields have relations")
    void generateInputArgsWithoutRelations_shouldReturnEmpty_whenAllFieldsHaveRelations() {

        final FieldDefinition ownerField = fieldWithNameTypeAndRelation(
                "owner", "OwnerModel", "ManyToOne", "ALL", "EAGER"
        );

        final FieldDefinition tagsField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(ownerField, tagsField);

        final List<String> result = FieldUtils.generateInputArgsWithoutRelations(fields);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalCreateInputTO returns '<type> <name>' for plain fields excluding ID")
    void generateInputArgsWithoutFinalCreateInputTO_shouldReturnTypeAndName_forPlainFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);
        ageField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField, ageField);
        final List<ModelDefinition> entities = List.of();

        final List<String> result = FieldUtils.generateInputArgsWithoutFinalCreateInputTO(fields, entities);

        assertEquals(2, result.size());
        assertTrue(result.get(0).endsWith("String name"));
        assertTrue(result.get(1).endsWith("Integer age"));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalCreateInputTO generates '<type> <name>Id' for ManyToOne/OneToOne relations")
    void generateInputArgsWithoutFinalCreateInputTO_shouldGenerateSingleId_forToOneRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition categoryField = fieldWithNameTypeAndRelation(
                "category", "Category", "ManyToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> mainFields = List.of(idField, categoryField);

        final FieldDefinition categoryIdField = fieldWithNameTypeAndId("id", "UUID", true);
        final ModelDefinition categoryModel = model("Category", List.of(categoryIdField));

        final List<ModelDefinition> entities = List.of(categoryModel);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinalCreateInputTO(mainFields, entities);

        assertEquals(1, result.size());
        assertEquals("UUID categoryId", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalCreateInputTO generates 'List<type> nameIds' for OneToMany/ManyToMany relations")
    void generateInputArgsWithoutFinalCreateInputTO_shouldGenerateListOfIds_forToManyRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition tagsField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );

        final FieldDefinition groupsField = fieldWithNameTypeAndRelation(
                "groups", "Group", "ManyToMany", "ALL", "EAGER"
        );

        final List<FieldDefinition> mainFields = List.of(idField, tagsField, groupsField);

        final FieldDefinition tagIdField = fieldWithNameTypeAndId("id", "Long", true);
        final ModelDefinition tagModel = model("Tag", List.of(tagIdField));

        final FieldDefinition groupIdField = fieldWithNameTypeAndId("id", "UUID", true);
        final ModelDefinition groupModel = model("Group", List.of(groupIdField));

        final List<ModelDefinition> entities = List.of(tagModel, groupModel);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinalCreateInputTO(mainFields, entities);

        assertEquals(2, result.size());
        assertEquals("List<Long> tagsIds", result.get(0));
        assertEquals("List<UUID> groupsIds", result.get(1));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalCreateInputTO uses '<resolvedType>TO name' for JSON fields")
    void generateInputArgsWithoutFinalCreateInputTO_shouldUseTOType_forJsonFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition addressField = fieldWithNameAndType("address", "JSON<Address>");
        addressField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, addressField);
        final List<ModelDefinition> entities = List.of();

        final List<String> result = FieldUtils.generateInputArgsWithoutFinalCreateInputTO(fields, entities);

        assertEquals(1, result.size());
        assertTrue(result.get(0).endsWith("AddressTO address"));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalCreateInputTO throws IllegalArgumentException when no ID field exists")
    void generateInputArgsWithoutFinalCreateInputTO_shouldThrow_whenNoIdField() {

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final List<FieldDefinition> fields = List.of(nameField);
        final List<ModelDefinition> entities = List.of();

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.generateInputArgsWithoutFinalCreateInputTO(fields, entities)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalUpdateInputTO returns '<type> <name>' for non-relation fields excluding ID")
    void generateInputArgsWithoutFinalUpdateInputTO_shouldReturnTypeAndName_forPlainFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition ownerField = fieldWithNameTypeAndRelation(
                "owner", "OwnerModel", "ManyToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(idField, nameField, ownerField);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinalUpdateInputTO(fields);

        assertEquals(1, result.size());
        assertEquals("String name", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalUpdateInputTO uses '<resolvedType>TO name' for JSON fields")
    void generateInputArgsWithoutFinalUpdateInputTO_shouldUseTOType_forJsonFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition jsonField = fieldWithNameAndType("metadata", "JSON<Metadata>");
        jsonField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, jsonField);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinalUpdateInputTO(fields);

        assertEquals(1, result.size());
        assertEquals("MetadataTO metadata", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalUpdateInputTO adds validation annotations when column constraints are present")
    void generateInputArgsWithoutFinalUpdateInputTO_shouldIncludeValidationAnnotations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(false);
        column.setLength(100);
        nameField.setColumn(column);

        final List<FieldDefinition> fields = List.of(idField, nameField);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinalUpdateInputTO(fields);

        assertEquals(1, result.size());
        assertEquals("@NotNull @Size(max = 100) String name", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinalUpdateInputTO throws IllegalArgumentException when no ID field exists")
    void generateInputArgsWithoutFinalUpdateInputTO_shouldThrow_whenNoIdField() {

        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        f1.setRelation(null);

        final FieldDefinition f2 = fieldWithNameTypeAndId("age", "Integer", false);
        f2.setRelation(null);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FieldUtils.generateInputArgsWithoutFinalUpdateInputTO(fields)
        );

        assertTrue(ex.getMessage().contains("No ID field found in the provided fields."));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinal generates '<type> <name>' for plain fields")
    void generateInputArgsWithoutFinal_shouldGeneratePlainArgs_forNonRelationNonJsonFields() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithNameTypeAndId("name", "String", false);
        nameField.setRelation(null);

        final FieldDefinition ageField = fieldWithNameTypeAndId("age", "Integer", false);
        ageField.setRelation(null);

        final List<FieldDefinition> fields = List.of(idField, nameField, ageField);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinal(fields);

        assertEquals(3, result.size());
        assertEquals("Long id", result.get(0));
        assertEquals("String name", result.get(1));
        assertEquals("Integer age", result.get(2));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinal uses '<resolvedType>TO name' for JSON fields")
    void generateInputArgsWithoutFinal_shouldUseTOType_forJsonFields() {

        final FieldDefinition jsonField = fieldWithNameAndType("address", "JSON<Address>");
        jsonField.setRelation(null);

        final List<FieldDefinition> fields = List.of(jsonField);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinal(fields);

        assertEquals(1, result.size());
        assertEquals("AddressTO address", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinal uses '<type>TO name' for ManyToOne/OneToOne relations")
    void generateInputArgsWithoutFinal_shouldUseTOType_forToOneRelations() {

        final FieldDefinition categoryField = fieldWithNameTypeAndRelation(
                "category", "Category", "ManyToOne", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(categoryField);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinal(fields);

        assertEquals(1, result.size());
        assertEquals("CategoryTO category", result.get(0));
    }

    @Test
    @DisplayName("generateInputArgsWithoutFinal uses 'List<typeTO> name' for OneToMany and ManyToMany relations")
    void generateInputArgsWithoutFinal_shouldUseListType_forToManyRelations() {

        final FieldDefinition tagsField = fieldWithNameTypeAndRelation(
                "tags", "Tag", "OneToMany", "ALL", "EAGER"
        );

        final FieldDefinition groupsField = fieldWithNameTypeAndRelation(
                "groups", "Group", "ManyToMany", "ALL", "EAGER"
        );

        final List<FieldDefinition> fields = List.of(tagsField, groupsField);

        final List<String> result = FieldUtils.generateInputArgsWithoutFinal(fields);

        assertEquals(2, result.size());
        assertEquals("List<TagTO> tags", result.get(0));
        assertEquals("List<GroupTO> groups", result.get(1));
    }

    @Test
    @DisplayName("isIdFieldUUID returns true when field type is UUID")
    void isIdFieldUUID_shouldReturnTrue_whenTypeIsUUID() {

        final FieldDefinition field = fieldWithNameAndType("id", "UUID");

        assertTrue(FieldUtils.isIdFieldUUID(field));
    }

    @Test
    @DisplayName("isIdFieldUUID returns true when field type is uuid (case-insensitive)")
    void isIdFieldUUID_shouldReturnTrue_whenTypeIsUUIDIgnoreCase() {

        final FieldDefinition field = fieldWithNameAndType("id", "uuid");

        assertTrue(FieldUtils.isIdFieldUUID(field));
    }

    @Test
    @DisplayName("isIdFieldUUID returns false when field type is not UUID")
    void isIdFieldUUID_shouldReturnFalse_whenTypeIsNotUUID() {

        final FieldDefinition field = fieldWithNameAndType("id", "Long");

        assertFalse(FieldUtils.isIdFieldUUID(field));
    }

    @Test
    @DisplayName("isAnyIdFieldUUID returns false when model has no relations")
    void isAnyIdFieldUUID_shouldReturnFalse_whenNoRelations() {

        final FieldDefinition idField = fieldWithNameTypeAndId("id", "Long", true);
        idField.setRelation(null);

        final FieldDefinition nameField = fieldWithoutRelation();
        nameField.setName("name");

        final ModelDefinition order = model("Order", List.of(idField, nameField));

        final List<ModelDefinition> entities = List.of(
                model("Customer", List.of()),
                model("Product", List.of())
        );

        final boolean result = FieldUtils.isAnyIdFieldUUID(order, entities);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyIdFieldUUID returns false when related models have non-UUID ID fields")
    void isAnyIdFieldUUID_shouldReturnFalse_whenRelatedIdsAreNotUUID() {

        final FieldDefinition orderId = fieldWithNameTypeAndId("id", "Long", true);
        orderId.setRelation(null);

        final FieldDefinition customerRelation = fieldWithNameTypeAndRelation(
                "customer", "Customer", "ManyToOne", "ALL", "EAGER"
        );

        final ModelDefinition order = model("Order", List.of(orderId, customerRelation));

        final FieldDefinition customerId = fieldWithNameTypeAndId("id", "Long", true);
        final ModelDefinition customer = model("Customer", List.of(customerId));

        final List<ModelDefinition> entities = List.of(customer);

        final boolean result = FieldUtils.isAnyIdFieldUUID(order, entities);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyIdFieldUUID returns true when any related model has an ID field of type UUID")
    void isAnyIdFieldUUID_shouldReturnTrue_whenAnyRelatedIdIsUUID() {

        final FieldDefinition orderId = fieldWithNameTypeAndId("id", "Long", true);
        orderId.setRelation(null);

        final FieldDefinition customerRelation = fieldWithNameTypeAndRelation(
                "customer", "Customer", "ManyToOne", "ALL", "EAGER"
        );
        final FieldDefinition productRelation = fieldWithNameTypeAndRelation(
                "product", "Product", "ManyToOne", "ALL", "EAGER"
        );

        final ModelDefinition order = model("Order", List.of(orderId, customerRelation, productRelation));

        final FieldDefinition customerId = fieldWithNameTypeAndId("id", "Long", true);
        final ModelDefinition customer = model("Customer", List.of(customerId));

        final FieldDefinition productId = fieldWithNameTypeAndId("id", "UUID", true);
        final ModelDefinition product = model("Product", List.of(productId));

        final List<ModelDefinition> entities = List.of(customer, product);

        final boolean result = FieldUtils.isAnyIdFieldUUID(order, entities);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldId returns false when list is empty")
    void isAnyFieldId_shouldReturnFalse_whenListIsEmpty() {

        final List<FieldDefinition> fields = List.of();

        final boolean result = FieldUtils.isAnyFieldId(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldId returns false when no field has an ID")
    void isAnyFieldId_shouldReturnFalse_whenNoFieldHasId() {

        final FieldDefinition f1 = fieldWithNameTypeAndId("name", "String", false);
        final FieldDefinition f2 = fieldWithNameTypeAndId("age", "Integer", false);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final boolean result = FieldUtils.isAnyFieldId(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldId returns true when at least one field has an ID")
    void isAnyFieldId_shouldReturnTrue_whenAnyFieldHasId() {

        final FieldDefinition f1 = fieldWithNameTypeAndId("id", "Long", true);
        final FieldDefinition f2 = fieldWithNameTypeAndId("name", "String", false);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final boolean result = FieldUtils.isAnyFieldId(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isJsonField returns true for JSON[...] type")
    void isJsonField_shouldReturnTrue_forJsonType() {

        final FieldDefinition field = fieldWithNameAndType("metadata", "JSON<Metadata>");

        final boolean result = FieldUtils.isJsonField(field);

        assertTrue(result);
    }

    @Test
    @DisplayName("isJsonField returns true for JSONB[...] type")
    void isJsonField_shouldReturnTrue_forJsonbType() {

        final FieldDefinition field = fieldWithNameAndType("metadata", "JSONB<Metadata>");

        final boolean result = FieldUtils.isJsonField(field);

        assertTrue(result);
    }

    @Test
    @DisplayName("isJsonField returns false for non-JSON types")
    void isJsonField_shouldReturnFalse_forNonJsonType() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        final FieldDefinition f2 = fieldWithNameAndType("value", "Integer");
        final FieldDefinition f3 = fieldWithNameAndType("status", "Enum");

        assertFalse(FieldUtils.isJsonField(f1));
        assertFalse(FieldUtils.isJsonField(f2));
        assertFalse(FieldUtils.isJsonField(f3));
    }

    @Test
    @DisplayName("extractJsonFieldName returns inner type for JSON[...] type")
    void extractJsonFieldName_shouldReturnInnerType_forJsonType() {

        final FieldDefinition field = fieldWithNameAndType("metadata", "JSON<Metadata>");

        final String result = FieldUtils.extractJsonFieldName(field);

        assertEquals("Metadata", result);
    }

    @Test
    @DisplayName("extractJsonFieldName returns inner type for JSONB[...] type")
    void extractJsonFieldName_shouldReturnInnerType_forJsonbType() {

        final FieldDefinition field = fieldWithNameAndType("metadata", "JSONB<MyCustom_Type>");

        final String result = FieldUtils.extractJsonFieldName(field);

        assertEquals("MyCustom_Type", result);
    }

    @Test
    @DisplayName("isSimpleCollectionField: List<String> -> true")
    void isSimpleCollectionField_list_true() {
        assertTrue(FieldUtils.isSimpleCollectionField(fieldWithType("List<String>")));
    }

    @Test
    @DisplayName("isSimpleCollectionField: Set<String> -> true")
    void isSimpleCollectionField_set_true() {
        assertTrue(FieldUtils.isSimpleCollectionField(fieldWithType("Set<String>")));
    }

    @Test
    @DisplayName("isSimpleCollectionField: allows whitespace inside <>")
    void isSimpleCollectionField_whitespace_true() {
        assertTrue(FieldUtils.isSimpleCollectionField(fieldWithType("List< String >")));
        assertTrue(FieldUtils.isSimpleCollectionField(fieldWithType("Set<   UUID   >")));
        assertTrue(FieldUtils.isSimpleCollectionField(fieldWithType("List<   com.acme.User   >")));
    }

    @Test
    @DisplayName("isSimpleCollectionField: non-collection types -> false")
    void isSimpleCollectionField_nonCollection_false() {
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("String")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("Integer")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("Map<String,String>")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("Optional<String>")));
    }

    @Test
    @DisplayName("isSimpleCollectionField: invalid/partial generic syntax -> false")
    void isSimpleCollectionField_invalidSyntax_false() {
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("List")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("Set")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("List<>")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("List<String")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("ListString>")));
    }

    @Test
    @DisplayName("isSimpleCollectionField: case-sensitive (list<String> -> false)")
    void isSimpleCollectionField_caseSensitive_false() {
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("list<String>")));
        assertFalse(FieldUtils.isSimpleCollectionField(fieldWithType("set<String>")));
    }

    @Test
    @DisplayName("extractSimpleCollectionType: List<String> -> String")
    void extractSimpleCollectionType_list_innerType() {
        assertEquals("String", FieldUtils.extractSimpleCollectionType(fieldWithType("List<String>")));
    }

    @Test
    @DisplayName("extractSimpleCollectionType: Set<UUID> -> UUID")
    void extractSimpleCollectionType_set_innerType() {
        assertEquals("UUID", FieldUtils.extractSimpleCollectionType(fieldWithType("Set<UUID>")));
    }

    @Test
    @DisplayName("extractSimpleCollectionType: trims whitespace around inner type")
    void extractSimpleCollectionType_trimsWhitespace() {
        assertEquals("String", FieldUtils.extractSimpleCollectionType(fieldWithType("List<  String  >")));
        assertEquals("com.acme.User", FieldUtils.extractSimpleCollectionType(fieldWithType("Set< com.acme.User >")));
    }

    @Test
    @DisplayName("extractSimpleCollectionType: supports nested generics as inner type (keeps them as-is)")
    void extractSimpleCollectionType_nestedGeneric_innerType() {
        assertEquals("List<String>", FieldUtils.extractSimpleCollectionType(fieldWithType("List<List<String>>")));
        assertEquals("Set<UUID>", FieldUtils.extractSimpleCollectionType(fieldWithType("Set<Set<UUID>>")));
    }

    @Test
    @DisplayName("extractSimpleCollectionType: when type does not match pattern -> throws IllegalStateException")
    void extractSimpleCollectionType_nonMatching_throws() {

        assertThrows(IllegalStateException.class,
                () -> FieldUtils.extractSimpleCollectionType(fieldWithType("String")));
        assertThrows(IllegalStateException.class,
                () -> FieldUtils.extractSimpleCollectionType(fieldWithType("List<>")));
        assertThrows(IllegalStateException.class,
                () -> FieldUtils.extractSimpleCollectionType(fieldWithType("Map<String,String>")));
    }

    @Test
    @DisplayName("computeResolvedType returns JSON inner type for JSON field")
    void computeResolvedType_shouldReturnInnerType_forJsonField() {

        final FieldDefinition field = fieldWithNameAndType("metadata", "JSON<Metadata>");

        final String result = FieldUtils.computeResolvedType(field);

        assertEquals("Metadata", result);
    }

    @Test
    @DisplayName("computeResolvedType returns <CapitalizedName>Enum for Enum type with name")
    void computeResolvedType_shouldReturnCapitalizedNameEnum_forEnumFieldWithName() {

        final FieldDefinition field = fieldWithNameAndType("status", "Enum");

        final String result = FieldUtils.computeResolvedType(field);

        assertEquals("StatusEnum", result);
    }

    @Test
    @DisplayName("computeResolvedType returns 'Enum' when type is Enum but name is null")
    void computeResolvedType_shouldReturnEnum_whenEnumTypeAndNullName() {

        final FieldDefinition field = new FieldDefinition();
        field.setName(null);
        field.setType("Enum");

        final String result = FieldUtils.computeResolvedType(field);

        assertEquals("Enum", result);
    }

    @Test
    @DisplayName("computeResolvedType returns original type for non-JSON non-Enum fields")
    void computeResolvedType_shouldReturnOriginalType_forOtherFields() {

        final FieldDefinition field = fieldWithNameAndType("name", "String");

        final String result = FieldUtils.computeResolvedType(field);

        assertEquals("String", result);
    }

    @Test
    @DisplayName("cloneFieldDefinition returns a deep copy of the field definition")
    void cloneFieldDefinition_shouldReturnDeepCopy() {

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(false);
        column.setLength(255);

        final FieldDefinition original = new FieldDefinition();
        original.setName("name");
        original.setType("String");
        original.setDescription("The name");
        original.setColumn(column);

        final FieldDefinition cloned = FieldUtils.cloneFieldDefinition(original);

        assertNotSame(original, cloned);

        assertEquals("name", cloned.getName());
        assertEquals("String", cloned.getType());
        assertEquals("The name", cloned.getDescription());

        assertNotNull(cloned.getColumn());
        assertNotSame(original.getColumn(), cloned.getColumn());
        assertEquals(original.getColumn().getNullable(), cloned.getColumn().getNullable());
        assertEquals(original.getColumn().getLength(), cloned.getColumn().getLength());

        original.setName("changed");
        original.getColumn().setLength(100);

        assertEquals("name", cloned.getName());
        assertEquals(255, cloned.getColumn().getLength());
    }

    @Test
    @DisplayName("hasAnyColumnValidation returns false when no field has column validations")
    void hasAnyColumnValidation_shouldReturnFalse_whenNoValidations() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(null);

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(true);
        column.setLength(null);
        final FieldDefinition f2 = fieldWithNameAndType("age", "Integer");
        f2.setColumn(column);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final boolean result = FieldUtils.hasAnyColumnValidation(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("hasAnyColumnValidation returns true when any field has nullable=false")
    void hasAnyColumnValidation_shouldReturnTrue_whenAnyFieldNonNullable() {

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(false);
        column.setLength(null);

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(column);

        final List<FieldDefinition> fields = List.of(f1);

        final boolean result = FieldUtils.hasAnyColumnValidation(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("hasAnyColumnValidation returns true when any field has length specified")
    void hasAnyColumnValidation_shouldReturnTrue_whenAnyFieldHasLength() {

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(true);
        column.setLength(100);

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(column);

        final List<FieldDefinition> fields = List.of(f1);

        final boolean result = FieldUtils.hasAnyColumnValidation(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAnyFieldNonNullable returns false when all fields are nullable or have no column")
    void isAnyFieldNonNullable_shouldReturnFalse_whenAllNullableOrNoColumn() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(null);

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(true);
        column.setLength(50);
        final FieldDefinition f2 = fieldWithNameAndType("description", "String");
        f2.setColumn(column);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final boolean result = FieldUtils.isAnyFieldNonNullable(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAnyFieldNonNullable returns true when at least one field has nullable=false")
    void isAnyFieldNonNullable_shouldReturnTrue_whenAnyFieldNonNullable() {

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(false);
        column.setLength(null);

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(column);

        final List<FieldDefinition> fields = List.of(f1);

        final boolean result = FieldUtils.isAnyFieldNonNullable(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("hasAnyFieldLengthValidation returns false when no field has length defined")
    void hasAnyFieldLengthValidation_shouldReturnFalse_whenNoLengthDefined() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(null);

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(false);
        column.setLength(null);
        final FieldDefinition f2 = fieldWithNameAndType("description", "String");
        f2.setColumn(column);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final boolean result = FieldUtils.hasAnyFieldLengthValidation(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("hasAnyFieldLengthValidation returns true when at least one field has length defined")
    void hasAnyFieldLengthValidation_shouldReturnTrue_whenAnyFieldHasLength() {

        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(true);
        column.setLength(255);

        final FieldDefinition f1 = fieldWithNameAndType("description", "String");
        f1.setColumn(column);

        final List<FieldDefinition> fields = List.of(f1);

        final boolean result = FieldUtils.hasAnyFieldLengthValidation(fields);

        assertTrue(result);
    }

}