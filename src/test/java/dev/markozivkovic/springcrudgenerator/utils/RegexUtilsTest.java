package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RegexUtilsTest {

    @Test
    @DisplayName("null / blank should be invalid")
    void isValidRegex_nullOrBlankShouldBeInvalid() {
        assertFalse(RegexUtils.isValidRegex(null));
        assertFalse(RegexUtils.isValidRegex(""));
        assertFalse(RegexUtils.isValidRegex("   "));
        assertFalse(RegexUtils.isValidRegex("\n\t"));
    }

    @Test
    @DisplayName("simple valid regexes should be valid")
    void isValidRegex_simpleValidRegexes() {
        assertTrue(RegexUtils.isValidRegex("a"));
        assertTrue(RegexUtils.isValidRegex("^a$"));
        assertTrue(RegexUtils.isValidRegex("^[A-Za-z]+$"));
        assertTrue(RegexUtils.isValidRegex("\\d+"));
        assertTrue(RegexUtils.isValidRegex("a{2,5}"));
        assertTrue(RegexUtils.isValidRegex("(abc|def)"));
    }

    @Test
    @DisplayName("regex with escaping should be valid")
    void isValidRegex_escapingValidRegexes() {
        assertTrue(RegexUtils.isValidRegex("^\\p{L}+$"));
        assertTrue(RegexUtils.isValidRegex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"));
        assertTrue(RegexUtils.isValidRegex("abc\\\\def"));
    }

    @Test
    @DisplayName("invalid regexes should be invalid")
    void isValidRegex_invalidRegexes() {
        assertFalse(RegexUtils.isValidRegex("("));
        assertFalse(RegexUtils.isValidRegex("[abc"));
        assertFalse(RegexUtils.isValidRegex("*abc"));
        assertFalse(RegexUtils.isValidRegex("a{2,"));
        assertFalse(RegexUtils.isValidRegex("abc\\"));
    }

    @Test
    @DisplayName("edge cases: valid but unusual patterns should still be valid")
    void isValidRegex_validButUnusualPatterns() {
        assertTrue(RegexUtils.isValidRegex(".*"));
        assertTrue(RegexUtils.isValidRegex("^$"));
        assertTrue(RegexUtils.isValidRegex("|"));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void normalizeRegexPattern_shouldNormalizeCorrectly(final String input, final String expected) {
        
        assertEquals(expected, RegexUtils.normalizeRegexPattern(input));
    }

    private static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of(null, ""),
            Arguments.of("", ""),
            Arguments.of("   \t\n", ""),
            Arguments.of("abc", "abc"),
            Arguments.of("\\d", "\\\\d"),
            Arguments.of("a\\b\\c", "a\\\\b\\\\c"),
            Arguments.of("\"quoted\"", "\\\"quoted\\\""),
            Arguments.of("%", "%%'"),
            Arguments.of("a%b%c", "a%%'b%%'c"),
            Arguments.of("\\d\"%", "\\\\d\\\"%%'"),
            Arguments.of("\\\\d", "\\\\\\\\d")
        );
    }

}
