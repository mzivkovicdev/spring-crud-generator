package dev.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.codegen.constants.TemplateContextConstants;

class EnumTemplateContextTest {

    @Test
    void createEnumContext_shouldReturnCorrectContext() {

        final String enumName = "Status";
        final List<String> values = List.of("ACTIVE", "INACTIVE");

        final Map<String, Object> ctx = EnumTemplateContext.createEnumContext(enumName, values);

        assertEquals("Status", ctx.get(TemplateContextConstants.ENUM_NAME));
        assertEquals(values, ctx.get(TemplateContextConstants.VALUES));
    }

    @Test
    void createEnumContext_shouldSupportEmptyValuesList() {

        final Map<String, Object> ctx = EnumTemplateContext.createEnumContext("EmptyEnum", List.of());

        assertEquals("EmptyEnum", ctx.get(TemplateContextConstants.ENUM_NAME));

        @SuppressWarnings("unchecked")
        final List<String> vals = (List<String>) ctx.get(TemplateContextConstants.VALUES);

        assertNotNull(vals);
        assertTrue(vals.isEmpty());
    }

    @Test
    void createEnumContext_shouldReturnImmutableValuesReference() {

        final List<String> originalList = new java.util.ArrayList<>(List.of("A", "B"));

        final Map<String, Object> ctx =
                EnumTemplateContext.createEnumContext("TestEnum", originalList);

        @SuppressWarnings("unchecked")
        final List<String> storedList = (List<String>) ctx.get(TemplateContextConstants.VALUES);

        assertSame(originalList, storedList);

        originalList.add("C");
        assertEquals(3, storedList.size());
        assertTrue(storedList.contains("C"));
    }
}
