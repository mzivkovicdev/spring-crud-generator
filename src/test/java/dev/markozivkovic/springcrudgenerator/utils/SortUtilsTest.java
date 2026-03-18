package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.SortDefinition;
import dev.markozivkovic.springcrudgenerator.models.SortDirection;

class SortUtilsTest {

    @Test
    @DisplayName("isSortEnabled should be false for null model and model without sort")
    void isSortEnabled_shouldBeFalse_whenModelOrSortIsNull() {
        assertFalse(SortUtils.isSortEnabled(null));
        assertFalse(SortUtils.isSortEnabled(new ModelDefinition()));
    }

    @Test
    @DisplayName("isSortEnabled should be true when sort config exists")
    void isSortEnabled_shouldBeTrue_whenSortExists() {
        final ModelDefinition model = new ModelDefinition()
                .setSort(new SortDefinition());

        assertTrue(SortUtils.isSortEnabled(model));
    }

    @Test
    @DisplayName("resolveAllowedFields should return empty list when sort is not configured")
    void resolveAllowedFields_shouldReturnEmpty_whenSortMissing() {
        final ModelDefinition model = new ModelDefinition();

        assertEquals(List.of(), SortUtils.resolveAllowedFields(model));
    }

    @Test
    @DisplayName("resolveAllowedFields should return immutable copy when configured")
    void resolveAllowedFields_shouldReturnImmutableCopy_whenConfigured() {
        final ModelDefinition model = new ModelDefinition()
                .setSort(new SortDefinition().setAllowedFields(List.of("name", "price")));

        final List<String> fields = SortUtils.resolveAllowedFields(model);

        assertEquals(List.of("name", "price"), fields);
        assertThrows(UnsupportedOperationException.class, () -> fields.add("createdAt"));
    }

    @Test
    @DisplayName("resolveDefaultDirection should return ASC when missing and configured value when present")
    void resolveDefaultDirection_shouldReturnExpectedValue() {
        assertEquals("ASC", SortUtils.resolveDefaultDirection(new ModelDefinition()));

        final ModelDefinition withNullDirection = new ModelDefinition()
                .setSort(new SortDefinition().setDefaultDirection(null));
        assertEquals("ASC", SortUtils.resolveDefaultDirection(withNullDirection));

        final ModelDefinition withDesc = new ModelDefinition()
                .setSort(new SortDefinition().setDefaultDirection(SortDirection.DESC));
        assertEquals("DESC", SortUtils.resolveDefaultDirection(withDesc));
    }

    @Test
    @DisplayName("contributeSortContext should populate sort context keys")
    void contributeSortContext_shouldPopulateKeys() {
        final ModelDefinition model = new ModelDefinition()
                .setSort(new SortDefinition()
                        .setAllowedFields(List.of("name", "price"))
                        .setDefaultDirection(SortDirection.DESC));

        final Map<String, Object> context = new HashMap<>();
        SortUtils.contributeSortContext(model, context);

        assertEquals(true, context.get(TemplateContextConstants.SORT_ENABLED));
        assertEquals(List.of("name", "price"), context.get(TemplateContextConstants.SORT_ALLOWED_FIELDS));
        assertEquals("name, price", context.get(TemplateContextConstants.SORT_ALLOWED_FIELDS_CSV));
        assertEquals("DESC", context.get(TemplateContextConstants.SORT_DEFAULT_DIRECTION));
    }
}
