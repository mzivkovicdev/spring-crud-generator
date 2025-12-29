package dev.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

class TransferObjectTemplateContextTest {

    @Test
    void computeUpdateTransferObjectContext_shouldBuildContextWithClassNameAndInputArgs() {
        final ModelDefinition model = mock(ModelDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1, f2);

        when(model.getName()).thenReturn("UserEntity");
        when(model.getFields()).thenReturn(fields);

        final List<String> expectedArgs = List.of("arg1", "arg2");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutFinalUpdateInputTO(fields))
                      .thenReturn(expectedArgs);

            final Map<String, Object> ctx = TransferObjectTemplateContext.computeUpdateTransferObjectContext(model);

            assertEquals("UserUpdate", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(expectedArgs, ctx.get(TemplateContextConstants.INPUT_ARGS));
        }
    }

    @Test
    void computeCreateTransferObjectContext_shouldBuildContextWithClassNameAndInputArgs() {
        final ModelDefinition model = mock(ModelDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);
        final List<ModelDefinition> allModels = List.of(model);

        when(model.getName()).thenReturn("OrderEntity");
        when(model.getFields()).thenReturn(fields);

        final List<String> expectedArgs = List.of("orderArg");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutFinalCreateInputTO(fields, allModels))
                      .thenReturn(expectedArgs);

            final Map<String, Object> ctx = TransferObjectTemplateContext.computeCreateTransferObjectContext(model, allModels);

            assertEquals("OrderCreate", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(expectedArgs, ctx.get(TemplateContextConstants.INPUT_ARGS));
        }
    }

    @Test
    void computeInputTransferObjectContext_shouldUseIdFieldTypeAndStrippedName() {
        final ModelDefinition model = mock(ModelDefinition.class);
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);

        when(model.getName()).thenReturn("ProductEntity");
        when(model.getFields()).thenReturn(fields);
        when(idField.getType()).thenReturn("java.util.UUID");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity"))
                     .thenReturn("Product");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            final Map<String, Object> ctx = TransferObjectTemplateContext.computeInputTransferObjectContext(model);

            assertEquals("Product", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("java.util.UUID", ctx.get(TemplateContextConstants.ID_TYPE));
        }
    }

    @Test
    void computeTransferObjectContext_shouldSetBaseFieldsAndAuditDisabledWhenNoAudit() {
        final ModelDefinition model = mock(ModelDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);

        when(model.getName()).thenReturn("CustomerEntity");
        when(model.getFields()).thenReturn(fields);
        when(model.getAudit()).thenReturn(null);

        final List<String> args = List.of("a", "b");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("CustomerEntity"))
                     .thenReturn("Customer");

            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutFinal(fields))
                      .thenReturn(args);

            final Map<String, Object> ctx = TransferObjectTemplateContext.computeTransferObjectContext(model);

            assertEquals("Customer", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(args, ctx.get(TemplateContextConstants.INPUT_ARGS));
            assertFalse((Boolean) ctx.get(TemplateContextConstants.AUDIT_ENABLED));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_TYPE));

            auditUtils.verifyNoInteractions();
        }
    }

    @Test
    void computeTransferObjectContext_shouldSetAuditFlagsWhenAuditEnabled() {
        final ModelDefinition model = mock(ModelDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);

        final AuditDefinition audit = mock(AuditDefinition.class);

        when(model.getName()).thenReturn("InvoiceEntity");
        when(model.getFields()).thenReturn(fields);
        when(model.getAudit()).thenReturn(audit);
        when(audit.isEnabled()).thenReturn(true);
        when(audit.getType()).thenReturn(AuditTypeEnum.INSTANT);

        final List<String> args = List.of("i1", "i2");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("InvoiceEntity"))
                     .thenReturn("Invoice");

            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutFinal(fields))
                      .thenReturn(args);

            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT))
                      .thenReturn("JPA_AUDIT");

            final Map<String, Object> ctx = TransferObjectTemplateContext.computeTransferObjectContext(model);

            assertEquals("Invoice", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(args, ctx.get(TemplateContextConstants.INPUT_ARGS));
            assertTrue((Boolean) ctx.get(TemplateContextConstants.AUDIT_ENABLED));
            assertEquals("JPA_AUDIT", ctx.get(TemplateContextConstants.AUDIT_TYPE));
        }
    }
}
