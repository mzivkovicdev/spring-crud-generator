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

class BasicTypeEnumTest {

    private static Stream<BasicTypeEnum> allBasicTypes() {
        return Stream.of(BasicTypeEnum.values());
    }

    @Test
    void getKey_shouldReturnCorrectKey() {
        assertEquals("String", BasicTypeEnum.STRING.getKey());
        assertEquals("Integer", BasicTypeEnum.INTEGER.getKey());
    }

    @Test
    void isBasicType_shouldReturnFalseForBasicTypesIgnoringCase() {
        assertFalse(BasicTypeEnum.isBasicType("string"));
        assertFalse(BasicTypeEnum.isBasicType("StRiNg"));
        assertFalse(BasicTypeEnum.isBasicType("INTEGER"));
    }

    @Test
    void isBasicType_shouldReturnFalseForInvalidTypes() {
        assertFalse(BasicTypeEnum.isBasicType("unknown"));
        assertFalse(BasicTypeEnum.isBasicType(""));
        assertFalse(BasicTypeEnum.isBasicType("Object"));
    }

    @Test
    void isBasicType_shouldReturnFalseForNull() {
        assertFalse(BasicTypeEnum.isBasicType(null));
    }

    @ParameterizedTest
    @MethodSource("allBasicTypes")
    void fromString_shouldReturnMatchingEnum_forExactKey(BasicTypeEnum expected) {
        final BasicTypeEnum actual = BasicTypeEnum.fromString(expected.getKey());
        assertSame(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("allBasicTypes")
    void fromString_shouldTrimInput_andReturnMatchingEnum(BasicTypeEnum expected) {
        
        final String input = "   " + expected.getKey() + "   ";
        final BasicTypeEnum actual = BasicTypeEnum.fromString(input);
        assertSame(expected, actual);
    }

    @Test
    void fromString_shouldThrowIllegalArgumentException_forUnsupportedType_andContainHelpfulMessage() {
        
        final String input = "unknown type";
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> BasicTypeEnum.fromString(input)
        );

        assertTrue(ex.getMessage().contains("Unsupported basic type: " + input), "Exception message should contain the unsupported type.");
        assertTrue(ex.getMessage().contains("Supported basic types are: "), "Exception message should mention supported basic types.");
        assertTrue(ex.getMessage().contains(BasicTypeEnum.getSupportedValues()), "Exception message should include supported values list.");
    }

    @Test
    void fromString_shouldThrowIllegalArgumentException_forBlankString() {
        
        final String input = "   ";

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> BasicTypeEnum.fromString(input)
        );

        assertTrue(ex.getMessage().contains("Unsupported basic type: " + input));
    }

    @Test
    void getSupportedValues_shouldReturnCommaSeparatedKeys() {
        final String expected = Stream.of(BasicTypeEnum.values())
                .map(BasicTypeEnum::getKey)
                .collect(Collectors.joining(", "));

        assertEquals(expected, BasicTypeEnum.getSupportedValues());
    }

}
