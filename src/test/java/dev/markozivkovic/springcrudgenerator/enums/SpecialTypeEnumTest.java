package dev.markozivkovic.springcrudgenerator.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpecialTypeEnumTest {

    @Test
    void getKey_shouldReturnCorrectKeys() {
        assertEquals("Enum", SpecialTypeEnum.ENUM.getKey());
        assertEquals("JSON", SpecialTypeEnum.JSON.getKey());
    }

    @Test
    void isEnumType_shouldReturnTrueForEnumIgnoringCase() {
        assertTrue(SpecialTypeEnum.isEnumType("Enum"));
        assertTrue(SpecialTypeEnum.isEnumType("ENUM"));
        assertTrue(SpecialTypeEnum.isEnumType("eNuM"));
    }

    @Test
    void isEnumType_shouldReturnFalseForNonEnumValues() {
        assertFalse(SpecialTypeEnum.isEnumType("JSON"));
        assertFalse(SpecialTypeEnum.isEnumType("ENUM[Something]"));
        assertFalse(SpecialTypeEnum.isEnumType("SomeOther"));
    }

    @Test
    void isEnumType_shouldReturnFalseForNull() {
        assertFalse(SpecialTypeEnum.isEnumType(null));
    }

    @Test
    void isJsonType_shouldReturnTrueForValidJsonTypeIgnoringCase() {
        assertTrue(SpecialTypeEnum.isJsonType("JSON<User>"));
        assertTrue(SpecialTypeEnum.isJsonType("json<Something>"));
        assertTrue(SpecialTypeEnum.isJsonType("JsOn<Another>"));
    }

    @Test
    void isJsonType_shouldReturnFalseForMissingBrackets() {
        assertFalse(SpecialTypeEnum.isJsonType("JSONUser"));
        assertFalse(SpecialTypeEnum.isJsonType("JSON<User"));
        assertFalse(SpecialTypeEnum.isJsonType("JSONUser>"));
    }

    @Test
    void isJsonType_shouldReturnFalseForPlainJsonKeyword() {
        assertFalse(SpecialTypeEnum.isJsonType("JSON"));
        assertFalse(SpecialTypeEnum.isJsonType("json"));
    }

    @Test
    void isJsonType_shouldReturnFalseForNull() {
        assertFalse(SpecialTypeEnum.isJsonType(null));
    }

    @Test
    void isCollectionType_shouldReturnFalse_forNullAndNonCollection() {
        assertFalse(SpecialTypeEnum.isCollectionType(null));
        assertFalse(SpecialTypeEnum.isCollectionType(""));
        assertFalse(SpecialTypeEnum.isCollectionType("List"));
        assertFalse(SpecialTypeEnum.isCollectionType("Set"));
        assertFalse(SpecialTypeEnum.isCollectionType("List<String"));
        assertFalse(SpecialTypeEnum.isCollectionType("Set<String"));
        assertFalse(SpecialTypeEnum.isCollectionType("ArrayList<String>"));
        assertFalse(SpecialTypeEnum.isCollectionType("HashSet<String>"));
        assertFalse(SpecialTypeEnum.isCollectionType("Map<String,String>"));
        assertFalse(SpecialTypeEnum.isCollectionType("LIST"));
        assertFalse(SpecialTypeEnum.isCollectionType("SET"));
    }

    @Test
    void isCollectionType_shouldReturnTrue_forListAndSet_caseInsensitive() {
        assertTrue(SpecialTypeEnum.isCollectionType("List<String>"));
        assertTrue(SpecialTypeEnum.isCollectionType("Set<String>"));

        assertTrue(SpecialTypeEnum.isCollectionType("LIST<String>"));
        assertTrue(SpecialTypeEnum.isCollectionType("SET<String>"));

        assertTrue(SpecialTypeEnum.isCollectionType("lIsT<UserEntity>"));
        assertTrue(SpecialTypeEnum.isCollectionType("sEt<UUID>"));
    }

    @Test
    @DisplayName("isCollectionType: respects 'endsWith(>)' strictly")
    void isCollectionType_shouldBeStrictAboutClosingBracket() {
        assertFalse(SpecialTypeEnum.isCollectionType("List<String>   "));
        assertFalse(SpecialTypeEnum.isCollectionType("Set<Integer>%n"));
        assertTrue(SpecialTypeEnum.isCollectionType("List<String>"));
    }

    @Test
    void isListType_shouldReturnFalse_forNullSetAndInvalidFormats() {
        assertFalse(SpecialTypeEnum.isListType(null));
        assertFalse(SpecialTypeEnum.isListType(""));
        assertFalse(SpecialTypeEnum.isListType("Set<String>"));
        assertFalse(SpecialTypeEnum.isListType("LIST"));
        assertFalse(SpecialTypeEnum.isListType("List<String"));
        assertFalse(SpecialTypeEnum.isListType("ArrayList<String>"));
    }

    @Test
    void isListType_shouldReturnTrue_forList_caseInsensitive() {
        assertTrue(SpecialTypeEnum.isListType("List<String>"));
        assertTrue(SpecialTypeEnum.isListType("LIST<String>"));
        assertTrue(SpecialTypeEnum.isListType("lIsT<UserEntity>"));
    }

    @Test
    void isListType_shouldBeStrictAboutClosingBracket() {
        assertFalse(SpecialTypeEnum.isListType("List<String> "));
        assertTrue(SpecialTypeEnum.isListType("List<String>"));
    }

    @Test
    void isSetType_shouldReturnFalse_forNullListAndInvalidFormats() {
        assertFalse(SpecialTypeEnum.isSetType(null));
        assertFalse(SpecialTypeEnum.isSetType(""));
        assertFalse(SpecialTypeEnum.isSetType("List<String>"));
        assertFalse(SpecialTypeEnum.isSetType("SET"));
        assertFalse(SpecialTypeEnum.isSetType("Set<String"));
        assertFalse(SpecialTypeEnum.isSetType("HashSet<String>"));
    }

    @Test
    void isSetType_shouldReturnTrue_forSet_caseInsensitive() {
        assertTrue(SpecialTypeEnum.isSetType("Set<String>"));
        assertTrue(SpecialTypeEnum.isSetType("SET<String>"));
        assertTrue(SpecialTypeEnum.isSetType("sEt<UUID>"));
    }

    @Test
    void isSetType_shouldBeStrictAboutClosingBracket() {
        assertFalse(SpecialTypeEnum.isSetType("Set<String>\t"));
        assertTrue(SpecialTypeEnum.isSetType("Set<String>"));
    }

    @Test
    @DisplayName("getSupportedCollectionValues should return 'List, Set'")
    void getSupportedCollectionValues_returnsExpectedString() {

        assertEquals("List, Set", SpecialTypeEnum.getSupportedCollectionValues());
    }

    @Test
    void getSupportedValues_shouldReturnCommaSeparatedKeys() {
        final String expected = Stream.of(SpecialTypeEnum.values())
                .map(SpecialTypeEnum::getKey)
                .collect(Collectors.joining(", "));

        assertEquals(expected, SpecialTypeEnum.getSupportedValues());
    }
    
}
