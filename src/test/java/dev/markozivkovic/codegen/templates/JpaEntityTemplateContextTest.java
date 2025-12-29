package dev.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.models.AuditDefinition;
import dev.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.AuditUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;

class JpaEntityTemplateContextTest {

    private ModelDefinition newModel(final String name,
                                     final String storageName,
                                     final List<FieldDefinition> fields,
                                     final AuditDefinition audit) {

        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getStorageName()).thenReturn(storageName);
        when(m.getFields()).thenReturn(fields);
        when(m.getAudit()).thenReturn(audit);
        return m;
    }

    @Test
    void computeJpaModelContext_shouldUseIdAwareArgsAndNonIdFieldNames_whenNoException() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField, f1, f2);
        final ModelDefinition model = newModel("UserEntity", "user_table", fields, null);
        final List<String> allFieldNames = List.of("id", "firstName", "lastName");
        final List<String> inputArgsExcludingId = List.of("String firstName", "String lastName");
        final List<String> nonIdFieldNames = List.of("firstName", "lastName");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractFieldNames(fields)).thenReturn(allFieldNames);
            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingId(fields)).thenReturn(inputArgsExcludingId);
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(fields)).thenReturn(nonIdFieldNames);
            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");

            final Map<String, Object> ctx = JpaEntityTemplateContext.computeJpaModelContext(model);

            assertEquals(fields, ctx.get(TemplateContextConstants.FIELDS));
            assertEquals(allFieldNames, ctx.get(TemplateContextConstants.FIELD_NAMES));
            assertEquals("UserEntity", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));

            assertEquals(inputArgsExcludingId, ctx.get(TemplateContextConstants.INPUT_ARGS));
            assertEquals(nonIdFieldNames, ctx.get(TemplateContextConstants.NON_ID_FIELD_NAMES));

            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_ENABLED));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_TYPE));

            assertEquals(true, ctx.get(TemplateContextConstants.IS_BASE_ENTITY));
            assertEquals("user_table", ctx.get(TemplateContextConstants.STORAGE_NAME));

            auditUtils.verifyNoInteractions();
        }
    }

    @Test
    void computeJpaModelContext_shouldFallbackWhenGenerateInputArgsExcludingIdThrows_andIncludeAuditInfo() {
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1, f2);

        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.isEnabled()).thenReturn(true);
        when(audit.getType()).thenReturn(AuditTypeEnum.INSTANT);

        final ModelDefinition model = newModel("OrderEntity", null, fields, audit);

        final List<String> allFieldNames = List.of("amount", "status");
        final List<String> fallbackInputArgs = List.of("BigDecimal amount", "String status");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractFieldNames(fields)).thenReturn(allFieldNames);
            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingId(fields)).thenThrow(new IllegalArgumentException("No id field"));
            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutRelations(fields)).thenReturn(fallbackInputArgs);
            fieldUtils.when(() -> FieldUtils.extractFieldNames(fields)).thenReturn(allFieldNames);
            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT)).thenReturn("JPA_AUDIT_TYPE");
            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity")).thenReturn("Order");

            final Map<String, Object> ctx = JpaEntityTemplateContext.computeJpaModelContext(model);

            assertEquals(fields, ctx.get(TemplateContextConstants.FIELDS));
            assertEquals(allFieldNames, ctx.get(TemplateContextConstants.FIELD_NAMES));
            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("Order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));

            assertEquals(fallbackInputArgs, ctx.get(TemplateContextConstants.INPUT_ARGS));
            assertEquals(allFieldNames, ctx.get(TemplateContextConstants.NON_ID_FIELD_NAMES));

            assertEquals(true, ctx.get(TemplateContextConstants.AUDIT_ENABLED));
            assertEquals("JPA_AUDIT_TYPE", ctx.get(TemplateContextConstants.AUDIT_TYPE));

            assertEquals(false, ctx.get(TemplateContextConstants.IS_BASE_ENTITY));
            assertNull(ctx.get(TemplateContextConstants.STORAGE_NAME));
        }
    }
}
