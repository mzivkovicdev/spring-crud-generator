package com.markozivkovic.codegen.imports.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImportCommonTest {

    @Test
    @DisplayName("Should add value to set when condition is true")
    void addIf_conditionTrue_addsValue() {
        
        final Set<String> imports = new HashSet<>();

        ImportCommon.addIf(true, imports, "java.util.List");

        assertTrue(imports.contains("java.util.List"));
        assertEquals(1, imports.size());
    }

    @Test
    @DisplayName("Should NOT add value to set when condition is false")
    void addIf_conditionFalse_doesNotAddValue() {
        
        final Set<String> imports = new HashSet<>();

        ImportCommon.addIf(false, imports, "java.util.List");

        assertFalse(imports.contains("java.util.List"));
        assertTrue(imports.isEmpty());
    }

    @Test
    @DisplayName("Should ignore null value when condition is true")
    void addIf_nullValueStillAcceptedBySet() {
        
        final Set<String> imports = new HashSet<>();

        ImportCommon.addIf(true, imports, null);

        assertTrue(imports.contains(null));
        assertEquals(1, imports.size());
    }
}
