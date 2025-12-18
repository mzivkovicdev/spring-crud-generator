package com.markozivkovic.codegen.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpecialTypeTest {

    @Test
    void getKey_shouldReturnCorrectKeys() {
        assertEquals("Enum", SpecialType.ENUM.getKey());
        assertEquals("JSON", SpecialType.JSON.getKey());
    }

    @Test
    void isEnumType_shouldReturnTrueForEnumIgnoringCase() {
        assertTrue(SpecialType.isEnumType("Enum"));
        assertTrue(SpecialType.isEnumType("ENUM"));
        assertTrue(SpecialType.isEnumType("eNuM"));
    }

    @Test
    void isEnumType_shouldReturnFalseForNonEnumValues() {
        assertFalse(SpecialType.isEnumType("JSON"));
        assertFalse(SpecialType.isEnumType("ENUM[Something]"));
        assertFalse(SpecialType.isEnumType("SomeOther"));
    }

    @Test
    void isEnumType_shouldReturnFalseForNull() {
        assertFalse(SpecialType.isEnumType(null));
    }

    @Test
    void isJsonType_shouldReturnTrueForValidJsonTypeIgnoringCase() {
        assertTrue(SpecialType.isJsonType("JSON<User>"));
        assertTrue(SpecialType.isJsonType("json<Something>"));
        assertTrue(SpecialType.isJsonType("JsOn<Another>"));
    }

    @Test
    void isJsonType_shouldReturnFalseForMissingBrackets() {
        assertFalse(SpecialType.isJsonType("JSONUser"));
        assertFalse(SpecialType.isJsonType("JSON<User"));
        assertFalse(SpecialType.isJsonType("JSONUser>"));
    }

    @Test
    void isJsonType_shouldReturnFalseForPlainJsonKeyword() {
        assertFalse(SpecialType.isJsonType("JSON"));
        assertFalse(SpecialType.isJsonType("json"));
    }

    @Test
    void isJsonType_shouldReturnFalseForNull() {
        assertFalse(SpecialType.isJsonType(null));
    }

    @Test
    void isCollectionType_shouldReturnFalse_forNullAndNonCollection() {
        assertFalse(SpecialType.isCollectionType(null));
        assertFalse(SpecialType.isCollectionType(""));
        assertFalse(SpecialType.isCollectionType("List"));
        assertFalse(SpecialType.isCollectionType("Set"));
        assertFalse(SpecialType.isCollectionType("List<String"));
        assertFalse(SpecialType.isCollectionType("Set<String"));
        assertFalse(SpecialType.isCollectionType("ArrayList<String>"));
        assertFalse(SpecialType.isCollectionType("HashSet<String>"));
        assertFalse(SpecialType.isCollectionType("Map<String,String>"));
        assertFalse(SpecialType.isCollectionType("LIST"));
        assertFalse(SpecialType.isCollectionType("SET"));
    }

    @Test
    void isCollectionType_shouldReturnTrue_forListAndSet_caseInsensitive() {
        assertTrue(SpecialType.isCollectionType("List<String>"));
        assertTrue(SpecialType.isCollectionType("Set<String>"));

        assertTrue(SpecialType.isCollectionType("LIST<String>"));
        assertTrue(SpecialType.isCollectionType("SET<String>"));

        assertTrue(SpecialType.isCollectionType("lIsT<UserEntity>"));
        assertTrue(SpecialType.isCollectionType("sEt<UUID>"));
    }

    @Test
    @DisplayName("isCollectionType: respects 'endsWith(>)' strictly")
    void isCollectionType_shouldBeStrictAboutClosingBracket() {
        assertFalse(SpecialType.isCollectionType("List<String>   "));
        assertFalse(SpecialType.isCollectionType("Set<Integer>\n"));
        assertTrue(SpecialType.isCollectionType("List<String>"));
    }

    @Test
    void isListType_shouldReturnFalse_forNullSetAndInvalidFormats() {
        assertFalse(SpecialType.isListType(null));
        assertFalse(SpecialType.isListType(""));
        assertFalse(SpecialType.isListType("Set<String>"));
        assertFalse(SpecialType.isListType("LIST"));
        assertFalse(SpecialType.isListType("List<String"));
        assertFalse(SpecialType.isListType("ArrayList<String>"));
    }

    @Test
    void isListType_shouldReturnTrue_forList_caseInsensitive() {
        assertTrue(SpecialType.isListType("List<String>"));
        assertTrue(SpecialType.isListType("LIST<String>"));
        assertTrue(SpecialType.isListType("lIsT<UserEntity>"));
    }

    @Test
    void isListType_shouldBeStrictAboutClosingBracket() {
        assertFalse(SpecialType.isListType("List<String> "));
        assertTrue(SpecialType.isListType("List<String>"));
    }

    @Test
    void isSetType_shouldReturnFalse_forNullListAndInvalidFormats() {
        assertFalse(SpecialType.isSetType(null));
        assertFalse(SpecialType.isSetType(""));
        assertFalse(SpecialType.isSetType("List<String>"));
        assertFalse(SpecialType.isSetType("SET"));
        assertFalse(SpecialType.isSetType("Set<String"));
        assertFalse(SpecialType.isSetType("HashSet<String>"));
    }

    @Test
    void isSetType_shouldReturnTrue_forSet_caseInsensitive() {
        assertTrue(SpecialType.isSetType("Set<String>"));
        assertTrue(SpecialType.isSetType("SET<String>"));
        assertTrue(SpecialType.isSetType("sEt<UUID>"));
    }

    @Test
    void isSetType_shouldBeStrictAboutClosingBracket() {
        assertFalse(SpecialType.isSetType("Set<String>\t"));
        assertTrue(SpecialType.isSetType("Set<String>"));
    }

    @Test
    void getSupportedValues_shouldReturnCommaSeparatedKeys() {
        final String expected = Stream.of(SpecialType.values())
                .map(SpecialType::getKey)
                .collect(Collectors.joining(", "));

        assertEquals(expected, SpecialType.getSupportedValues());
    }
    
}
