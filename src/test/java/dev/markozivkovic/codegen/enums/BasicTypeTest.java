package dev.markozivkovic.codegen.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class BasicTypeTest {

    @Test
    void getKey_shouldReturnCorrectKey() {
        assertEquals("String", BasicType.STRING.getKey());
        assertEquals("Integer", BasicType.INTEGER.getKey());
    }

    @Test
    void isBasicType_shouldReturnTrueForValidTypesIgnoringCase() {
        assertTrue(BasicType.isBasicType("string"));
        assertTrue(BasicType.isBasicType("StRiNg"));
        assertTrue(BasicType.isBasicType("INTEGER"));
    }

    @Test
    void isBasicType_shouldReturnFalseForInvalidTypes() {
        assertFalse(BasicType.isBasicType("unknown"));
        assertFalse(BasicType.isBasicType(""));
        assertFalse(BasicType.isBasicType("Object"));
    }

    @Test
    void isBasicType_shouldReturnFalseForNull() {
        assertFalse(BasicType.isBasicType(null));
    }

    @Test
    void getSupportedValues_shouldReturnCommaSeparatedKeys() {
        final String expected = Stream.of(BasicType.values())
                .map(BasicType::getKey)
                .collect(Collectors.joining(", "));

        assertEquals(expected, BasicType.getSupportedValues());
    }

}
