package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    @DisplayName("capitalize: should return null when input is null")
    void capitalize_null_returnsNull() {
        
        final String result = StringUtils.capitalize(null);

        assertEquals(null, result);
    }

    @Test
    @DisplayName("capitalize: should return empty string when input is empty")
    void capitalize_empty_returnsEmpty() {
        
        final String result = StringUtils.capitalize("");

        assertEquals("", result);
    }

    @Test
    @DisplayName("capitalize: should return same string when input is whitespace only")
    void capitalize_whitespaceOnly_returnsSame() {
        final String input = "   ";
        final String result = StringUtils.capitalize(input);

        assertEquals(input, result);
    }

    @Test
    @DisplayName("capitalize: should capitalize first character of lowercase word")
    void capitalize_lowercaseWord_capitalizesFirstChar() {
        final String result = StringUtils.capitalize("hello");

        assertEquals("Hello", result);
    }

    @Test
    @DisplayName("capitalize: should only affect first character and keep the rest as is")
    void capitalize_mixedCase_changesOnlyFirstChar() {
        final String result = StringUtils.capitalize("hELLo");

        assertEquals("HELLo", result);
    }

    @Test
    @DisplayName("capitalize: should leave first character unchanged if it is not a letter")
    void capitalize_nonLetterFirstChar_unchangedFirstChar() {
        final String result = StringUtils.capitalize("1abc");

        assertEquals("1abc", result);
    }

    @Test
    @DisplayName("uncapitalize: should return null when input is null")
    void uncapitalize_null_returnsNull() {
        final String result = StringUtils.uncapitalize(null);

        assertEquals(null, result);
    }

    @Test
    @DisplayName("uncapitalize: should return empty string when input is empty")
    void uncapitalize_empty_returnsEmpty() {
        final String result = StringUtils.uncapitalize("");

        assertEquals("", result);
    }

    @Test
    @DisplayName("uncapitalize: should return same string when input is whitespace only")
    void uncapitalize_whitespaceOnly_returnsSame() {
        final String input = "   ";
        final String result = StringUtils.uncapitalize(input);

        assertEquals(input, result);
    }

    @Test
    @DisplayName("uncapitalize: should uncapitalize first character of capitalized word")
    void uncapitalize_capitalizedWord_uncapitalizesFirstChar() {
        final String result = StringUtils.uncapitalize("Hello");

        assertEquals("hello", result);
    }

    @Test
    @DisplayName("uncapitalize: should only affect first character and keep the rest as is")
    void uncapitalize_mixedCase_changesOnlyFirstChar() {
        final String result = StringUtils.uncapitalize("HELLo");

        assertEquals("hELLo", result);
    }

    @Test
    @DisplayName("uncapitalize: should leave first character unchanged if it is not a letter")
    void uncapitalize_nonLetterFirstChar_unchangedFirstChar() {
        final String result = StringUtils.uncapitalize("1ABC");

        assertEquals("1ABC", result);
    }

    @Test
    @DisplayName("isNotBlank: should return false for null")
    void isNotBlank_null_returnsFalse() {
        assertFalse(StringUtils.isNotBlank(null));
    }

    @Test
    @DisplayName("isNotBlank: should return false for empty string")
    void isNotBlank_empty_returnsFalse() {
        assertFalse(StringUtils.isNotBlank(""));
    }

    @Test
    @DisplayName("isNotBlank: should return false for whitespace-only string")
    void isNotBlank_whitespaceOnly_returnsFalse() {
        assertFalse(StringUtils.isNotBlank("   "));
    }

    @Test
    @DisplayName("isNotBlank: should return true for non-empty non-whitespace string")
    void isNotBlank_normalString_returnsTrue() {
        assertTrue(StringUtils.isNotBlank("abc"));
    }

    @Test
    @DisplayName("isNotBlank: should return true for string with leading and trailing spaces around text")
    void isNotBlank_spacesAroundText_returnsTrue() {
        assertTrue(StringUtils.isNotBlank("  abc  "));
    }

    @Test
    @DisplayName("isBlank: should return true for null")
    void isBlank_null_returnsTrue() {
        assertTrue(StringUtils.isBlank(null));
    }

    @Test
    @DisplayName("isBlank: should return true for empty string")
    void isBlank_empty_returnsTrue() {
        assertTrue(StringUtils.isBlank(""));
    }

    @Test
    @DisplayName("isBlank: should return true for whitespace-only string")
    void isBlank_whitespaceOnly_returnsTrue() {
        assertTrue(StringUtils.isBlank("   "));
    }

    @Test
    @DisplayName("isBlank: should return false for non-empty non-whitespace string")
    void isBlank_normalString_returnsFalse() {
        assertFalse(StringUtils.isBlank("abc"));
    }

    @Test
    @DisplayName("isBlank: should return false for string with spaces around text")
    void isBlank_spacesAroundText_returnsFalse() {
        assertFalse(StringUtils.isBlank("  abc  "));
    }

}
