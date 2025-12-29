package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HashUtilsTest {

    @Test
    @DisplayName("sha256(String) produces correct SHA-256 for known input")
    void sha256String_shouldProduceCorrectHash() {

        final String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

        final String result = HashUtils.sha256("hello");

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("sha256(byte[]) matches sha256(String) for the same input")
    void sha256ByteArray_shouldMatchStringVersion() {

        final String text = "test-input";
        final String hash1 = HashUtils.sha256(text);
        final String hash2 = HashUtils.sha256(text.getBytes(StandardCharsets.UTF_8));

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("sha256(String) is deterministic for same input")
    void sha256_shouldBeDeterministic() {

        final String input = "abcdef123456";
        final String h1 = HashUtils.sha256(input);
        final String h2 = HashUtils.sha256(input);

        assertEquals(h1, h2);
    }

    @Test
    @DisplayName("sha256(String) generates different hashes for different inputs")
    void sha256_shouldProduceDifferentHashesForDifferentInput() {

        final String h1 = HashUtils.sha256("A");
        final String h2 = HashUtils.sha256("B");

        assertNotEquals(h1, h2);
    }

    @Test
    @DisplayName("sha256(\"\") returns well-known SHA-256 of empty string")
    void sha256_shouldHashEmptyStringCorrectly() {

        final String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        final String actual = HashUtils.sha256("");

        assertEquals(expected, actual);
    }
}
