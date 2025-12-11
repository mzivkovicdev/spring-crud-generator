package com.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.models.AuditDefinition;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;
import com.markozivkovic.codegen.utils.AuditUtils;
import com.markozivkovic.codegen.utils.FieldUtils;

class JpaEntityTemplateContextTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields, final AuditDefinition audit) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
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

        final ModelDefinition model = newModel("UserEntity", fields, null);

        final List<String> allFieldNames = List.of("id", "firstName", "lastName");
        final List<String> inputArgsExcludingId = List.of("String firstName", "String lastName");
        final List<String> nonIdFieldNames = List.of("firstName", "lastName");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractFieldNames(fields))
                      .thenReturn(allFieldNames);

            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingId(fields))
                      .thenReturn(inputArgsExcludingId);
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(fields))
                      .thenReturn(nonIdFieldNames);

            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutRelations(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractFieldNames(fields))
                      .thenReturn(allFieldNames);

            final Map<String, Object> ctx = JpaEntityTemplateContext.computeJpaModelContext(model);

            assertEquals(fields, ctx.get(TemplateContextConstants.FIELDS));
            assertEquals(allFieldNames, ctx.get(TemplateContextConstants.FIELD_NAMES));
            assertEquals("UserEntity", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(inputArgsExcludingId, ctx.get(TemplateContextConstants.INPUT_ARGS));
            assertEquals(nonIdFieldNames, ctx.get(TemplateContextConstants.NON_ID_FIELD_NAMES));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_ENABLED));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_TYPE));
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

        final ModelDefinition model = newModel("OrderEntity", fields, audit);

        final List<String> allFieldNames = List.of("amount", "status");
        final List<String> fallbackInputArgs = List.of("BigDecimal amount", "String status");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractFieldNames(fields))
                      .thenReturn(allFieldNames);

            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingId(fields))
                      .thenThrow(new IllegalArgumentException("No id field"));
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(fields))
                      .thenThrow(new IllegalArgumentException("No id field"));

            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutRelations(fields))
                      .thenReturn(fallbackInputArgs);
            fieldUtils.when(() -> FieldUtils.extractFieldNames(fields))
                      .thenReturn(allFieldNames);
            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT))
                      .thenReturn("JPA_AUDIT_TYPE");

            final Map<String, Object> ctx = JpaEntityTemplateContext.computeJpaModelContext(model);

            assertEquals(fields, ctx.get(TemplateContextConstants.FIELDS));
            assertEquals(allFieldNames, ctx.get(TemplateContextConstants.FIELD_NAMES));
            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(fallbackInputArgs, ctx.get(TemplateContextConstants.INPUT_ARGS));
            assertEquals(allFieldNames, ctx.get(TemplateContextConstants.NON_ID_FIELD_NAMES));
            assertEquals(true, ctx.get(TemplateContextConstants.AUDIT_ENABLED));
            assertEquals("JPA_AUDIT_TYPE", ctx.get(TemplateContextConstants.AUDIT_TYPE));
        }
    }
}
