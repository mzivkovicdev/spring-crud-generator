package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.RelationDefinition;

class FieldUtilsTest {

    private FieldDefinition fieldWithoutRelation() {
        
        final FieldDefinition field = new FieldDefinition();
        field.setRelation(null);
        
        return field;
    }

    private FieldDefinition fieldWithType(String type) {
        
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


}
