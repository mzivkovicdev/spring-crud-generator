package dev.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

class DataGeneratorTemplateContextTest {

    @Test
    void computeDataGeneratorContext_shouldReturnCorrectContext() {
        
        final TestDataGeneratorConfig cfg = mock(TestDataGeneratorConfig.class);
        when(cfg.generator()).thenReturn("UserDataGenerator");
        when(cfg.randomFieldName()).thenReturn("random");
        when(cfg.singleObjectMethodName()).thenReturn("generateUser");
        when(cfg.multipleObjectsMethodName()).thenReturn("generateUsers");

        final Map<String, Object> ctx = DataGeneratorTemplateContext.computeDataGeneratorContext(cfg);

        assertEquals("UserDataGenerator", ctx.get(TemplateContextConstants.DATA_GENERATOR));
        assertEquals("random", ctx.get(TemplateContextConstants.DATA_GENERATOR_FIELD_NAME));
        assertEquals("generateUser", ctx.get(TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ));
        assertEquals("generateUsers", ctx.get(TemplateContextConstants.DATA_GENERATOR_LIST_METHOD));
    }

    @Test
    void computeDataGeneratorContext_shouldSupportEmptyStrings() {

        final TestDataGeneratorConfig cfg = mock(TestDataGeneratorConfig.class);
        when(cfg.generator()).thenReturn("");
        when(cfg.randomFieldName()).thenReturn("");
        when(cfg.singleObjectMethodName()).thenReturn("");
        when(cfg.multipleObjectsMethodName()).thenReturn("");

        final Map<String, Object> ctx = DataGeneratorTemplateContext.computeDataGeneratorContext(cfg);

        assertEquals("", ctx.get(TemplateContextConstants.DATA_GENERATOR));
        assertEquals("", ctx.get(TemplateContextConstants.DATA_GENERATOR_FIELD_NAME));
        assertEquals("", ctx.get(TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ));
        assertEquals("", ctx.get(TemplateContextConstants.DATA_GENERATOR_LIST_METHOD));
    }

    @Test
    void computeDataGeneratorContext_shouldSupportNullValues() {

        final TestDataGeneratorConfig cfg = mock(TestDataGeneratorConfig.class);
        when(cfg.generator()).thenReturn(null);
        when(cfg.randomFieldName()).thenReturn(null);
        when(cfg.singleObjectMethodName()).thenReturn(null);
        when(cfg.multipleObjectsMethodName()).thenReturn(null);

        final Map<String, Object> ctx = DataGeneratorTemplateContext.computeDataGeneratorContext(cfg);

        assertNull(ctx.get(TemplateContextConstants.DATA_GENERATOR));
        assertNull(ctx.get(TemplateContextConstants.DATA_GENERATOR_FIELD_NAME));
        assertNull(ctx.get(TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ));
        assertNull(ctx.get(TemplateContextConstants.DATA_GENERATOR_LIST_METHOD));
    }
}
