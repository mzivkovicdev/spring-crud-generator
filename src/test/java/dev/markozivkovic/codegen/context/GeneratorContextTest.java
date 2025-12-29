package dev.markozivkovic.codegen.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeneratorContextTest {

    @BeforeEach
    void resetGeneratedParts() throws Exception {
        final Field field = GeneratorContext.class.getDeclaredField("GENERATED_PARTS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Set<String> set = (Set<String>) field.get(null);
        set.clear();
    }

    @Test
    @DisplayName("isGenerated: should return false when part was never marked")
    void isGenerated_partNotMarked_returnsFalse() {
        assertFalse(GeneratorContext.isGenerated("entity"));
    }

    @Test
    @DisplayName("markGenerated: should return true when marking a new part")
    void markGenerated_newPart_returnsTrue() {

        final boolean result = GeneratorContext.markGenerated("service");

        assertTrue(result);
        assertTrue(GeneratorContext.isGenerated("service"));
    }

    @Test
    @DisplayName("markGenerated: should return false when marking an already generated part")
    void markGenerated_samePartTwice_returnsFalse() {
        GeneratorContext.markGenerated("controller");
        
        final boolean secondAttempt = GeneratorContext.markGenerated("controller");

        assertFalse(secondAttempt);
        assertTrue(GeneratorContext.isGenerated("controller"));
    }

    @Test
    @DisplayName("isGenerated: should return true only for parts that were marked")
    void isGenerated_onlyMarkedPartsReturnTrue() {
        
        GeneratorContext.markGenerated("model");

        assertTrue(GeneratorContext.isGenerated("model"));
        assertFalse(GeneratorContext.isGenerated("repository"));
    }
}
