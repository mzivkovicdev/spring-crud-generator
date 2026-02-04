package dev.markozivkovic.springcrudgenerator.templates.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.ColumnDefinition;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;

class ValidationContextBuilderTest {

    @Test
    void contribute_string_required_generatesInvalidNull() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(true);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(null);
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("name");
        when(f.getResolvedType()).thenReturn("String");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        assertTrue(ctx.containsKey(TemplateContextConstants.VALIDATION_OVERRIDES));

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        assertEquals(1, overrides.size());
        final Map<String, Object> o = overrides.get(0);

        assertEquals("name", o.get(TemplateContextConstants.FIELD));
        assertEquals("generateString(1)", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("null", o.get(TemplateContextConstants.INVALID_VALUE));
        assertEquals(true, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_string_notBlank_generatesWhitespaceInvalid() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(null);
        when(v.getNotBlank()).thenReturn(true);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(null);
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("code");
        when(f.getResolvedType()).thenReturn("String");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        final Map<String, Object> o = overrides.get(0);

        assertEquals("code", o.get(TemplateContextConstants.FIELD));
        assertEquals("generateString(1)", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("\"   \"", o.get(TemplateContextConstants.INVALID_VALUE));
        assertEquals(true, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_string_notEmpty_generatesEmptyInvalid() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(null);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(true);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(null);
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("desc");
        when(f.getResolvedType()).thenReturn("String");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        final Map<String, Object> o = overrides.get(0);

        assertEquals("desc", o.get(TemplateContextConstants.FIELD));
        assertEquals("generateString(1)", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("\"\"", o.get(TemplateContextConstants.INVALID_VALUE));

        assertEquals(true, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_string_email_generatesValidEmailAndInvalidNotEmail() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(null);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(null);
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(true);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("email");
        when(f.getResolvedType()).thenReturn("String");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        final Map<String, Object> o = overrides.get(0);

        assertEquals("email", o.get(TemplateContextConstants.FIELD));
        assertEquals("\"a@b.co\"", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("\"not-an-email\"", o.get(TemplateContextConstants.INVALID_VALUE));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_string_onlyColumnLength_generatesOverride() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ColumnDefinition col = mock(ColumnDefinition.class);
        when(col.getLength()).thenReturn(3);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("title");
        when(f.getResolvedType()).thenReturn("String");
        when(f.getValidation()).thenReturn(null);
        when(f.getColumn()).thenReturn(col);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        final Map<String, Object> o = overrides.get(0);

        assertEquals("title", o.get(TemplateContextConstants.FIELD));
        assertEquals("generateString(1)", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("generateString(4)", o.get(TemplateContextConstants.INVALID_VALUE));
        assertEquals(true, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_integer_minMax_generatesValidMin_invalidMaxPlusOne() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(null);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(new BigDecimal("2"));
        when(v.getMax()).thenReturn(new BigDecimal("5"));
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("age");
        when(f.getResolvedType()).thenReturn("Integer");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);
        final Map<String, Object> o = overrides.get(0);

        assertEquals("age", o.get(TemplateContextConstants.FIELD));
        assertEquals("2", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("6", o.get(TemplateContextConstants.INVALID_VALUE));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_bigDecimal_maxOnly_generatesNewBigDecimalExpressions() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(null);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(new BigDecimal("2.5"));
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("price");
        when(f.getResolvedType()).thenReturn("BigDecimal");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);
        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        assertEquals(1, overrides.size());
        final Map<String, Object> o = overrides.get(0);

        assertEquals("price", o.get(TemplateContextConstants.FIELD));
        assertEquals("new " + ImportConstants.Java.BIG_DECIMAL + "(\"2.5\")", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("new " + ImportConstants.Java.BIG_DECIMAL + "(\"3.5\")", o.get(TemplateContextConstants.INVALID_VALUE));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_listOfString_required_generatesGenerateList_valid_andEmptyList_invalid() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(true);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(null);
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("tags");
        when(f.getResolvedType()).thenReturn("List<String>");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        final Map<String, Object> o = overrides.get(0);

        assertEquals("tags", o.get(TemplateContextConstants.FIELD));
        assertEquals("generateList(1, () -> \"a\")", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals(ImportConstants.Java.LIST + ".of()", o.get(TemplateContextConstants.INVALID_VALUE));

        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(true, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_setOfInteger_minMaxItems_wrapsHashSet() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(null);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(null);
        when(v.getMinItems()).thenReturn(2);
        when(v.getMaxItems()).thenReturn(3);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("ids");
        when(f.getResolvedType()).thenReturn("Set<Integer>");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

        final Map<String, Object> o = overrides.get(0);

        assertEquals("ids", o.get(TemplateContextConstants.FIELD));
        assertEquals("new java.util.HashSet<>(generateList(2, () -> 1))", o.get(TemplateContextConstants.VALID_VALUE));
        assertEquals("new java.util.HashSet<>(generateList(4, () -> 1))", o.get(TemplateContextConstants.INVALID_VALUE));

        assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
        assertEquals(true, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
    }

    @Test
    void contribute_customType_evenIfValidationExists_doesNotGenerateOverride() {
        final ModelDefinition model = mock(ModelDefinition.class);

        final ValidationDefinition v = mock(ValidationDefinition.class);
        when(v.getRequired()).thenReturn(true);
        when(v.getNotBlank()).thenReturn(null);
        when(v.getNotEmpty()).thenReturn(null);
        when(v.getMinLength()).thenReturn(null);
        when(v.getMaxLength()).thenReturn(null);
        when(v.getMin()).thenReturn(null);
        when(v.getMax()).thenReturn(null);
        when(v.getMinItems()).thenReturn(null);
        when(v.getMaxItems()).thenReturn(null);
        when(v.getPattern()).thenReturn(null);
        when(v.getEmail()).thenReturn(null);

        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn("obj");
        when(f.getResolvedType()).thenReturn("com.acme.CustomType");
        when(f.getValidation()).thenReturn(v);
        when(f.getColumn()).thenReturn(null);

        when(model.getFields()).thenReturn(List.of(f));

        final Map<String, Object> ctx = new HashMap<>();
        ValidationContextBuilder.contribute(model, ctx, "gen", "one");

        assertFalse(ctx.containsKey(TemplateContextConstants.VALIDATION_OVERRIDES));
        assertFalse(ctx.containsKey(TemplateContextConstants.HAS_GENERATE_STRING));
        assertFalse(ctx.containsKey(TemplateContextConstants.HAS_GENERATE_LIST));
    }
}
