package com.markozivkovic.codegen.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    void getSupportedValues_shouldReturnCommaSeparatedKeys() {
        final String expected = Stream.of(SpecialType.values())
                .map(SpecialType::getKey)
                .collect(Collectors.joining(", "));

        assertEquals(expected, SpecialType.getSupportedValues());
    }
    
}
