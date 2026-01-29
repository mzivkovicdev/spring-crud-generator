package dev.markozivkovic.springcrudgenerator.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BasicTypeTest {

    private static Stream<BasicType> allBasicTypes() {
        return Stream.of(BasicType.values());
    }

    @Test
    void getKey_shouldReturnCorrectKey() {
        assertEquals("String", BasicType.STRING.getKey());
        assertEquals("Integer", BasicType.INTEGER.getKey());
    }

    @Test
    void isBasicType_shouldReturnFalseForBasicTypesIgnoringCase() {
        assertFalse(BasicType.isBasicType("string"));
        assertFalse(BasicType.isBasicType("StRiNg"));
        assertFalse(BasicType.isBasicType("INTEGER"));
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

    @ParameterizedTest
    @MethodSource("allBasicTypes")
    void fromString_shouldReturnMatchingEnum_forExactKey(BasicType expected) {
        final BasicType actual = BasicType.fromString(expected.getKey());
        assertSame(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("allBasicTypes")
    void fromString_shouldTrimInput_andReturnMatchingEnum(BasicType expected) {
        
        final String input = "   " + expected.getKey() + "   ";
        final BasicType actual = BasicType.fromString(input);
        assertSame(expected, actual);
    }

    @Test
    void fromString_shouldThrowIllegalArgumentException_forUnsupportedType_andContainHelpfulMessage() {
        
        final String input = "unknown type";
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> BasicType.fromString(input)
        );

        assertTrue(ex.getMessage().contains("Unsupported basic type: " + input), "Exception message should contain the unsupported type.");
        assertTrue(ex.getMessage().contains("Supported basic types are: "), "Exception message should mention supported basic types.");
        assertTrue(ex.getMessage().contains(BasicType.getSupportedValues()), "Exception message should include supported values list.");
    }

    @Test
    void fromString_shouldThrowIllegalArgumentException_forBlankString() {
        
        final String input = "   ";

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> BasicType.fromString(input)
        );

        assertTrue(ex.getMessage().contains("Unsupported basic type: " + input));
    }

    @Test
    void getSupportedValues_shouldReturnCommaSeparatedKeys() {
        final String expected = Stream.of(BasicType.values())
                .map(BasicType::getKey)
                .collect(Collectors.joining(", "));

        assertEquals(expected, BasicType.getSupportedValues());
    }

}
