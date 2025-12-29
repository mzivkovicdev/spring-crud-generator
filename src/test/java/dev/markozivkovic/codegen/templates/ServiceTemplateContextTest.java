package dev.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.AnnotationConstants;
import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;

class ServiceTemplateContextTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void createServiceClassContext_shouldSetClassNameAndModelNameFromStrippedName() {
        final ModelDefinition model = newModel("UserEntity", List.of());

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            final Map<String, Object> ctx = ServiceTemplateContext.createServiceClassContext(model);

            assertEquals("UserService", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("User", ctx.get(TemplateContextConstants.MODEL_NAME));
        }
    }

    @Test
    void createGetAllByIdsMethodContext_shouldSetFieldsAndGenerateJavaDocWhenDescriptionPresent() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("java.util.UUID");
        when(idField.getName()).thenReturn("id");
        when(idField.getDescription()).thenReturn("Primary id");

        final ModelDefinition model = newModel("OrderEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            final Map<String, Object> ctx = ServiceTemplateContext.createGetAllByIdsMethodContext(model);

            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("java.util.UUID", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("Primary id", ctx.get(TemplateContextConstants.ID_DESCRIPTION));
            assertEquals(true, ctx.get(TemplateContextConstants.GENERATE_JAVA_DOC));
        }
    }

    @Test
    void createGetAllByIdsMethodContext_shouldNotGenerateJavaDocWhenDescriptionBlank() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");
        when(idField.getDescription()).thenReturn("   ");

        final ModelDefinition model = newModel("OrderEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            final Map<String, Object> ctx = ServiceTemplateContext.createGetAllByIdsMethodContext(model);

            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("   ", ctx.get(TemplateContextConstants.ID_DESCRIPTION));
            assertEquals(false, ctx.get(TemplateContextConstants.GENERATE_JAVA_DOC));
        }
    }

    @Test
    void createGetReferenceByIdMethodContext_shouldMirrorGetAllByIdsContextLogic() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");
        when(idField.getDescription()).thenReturn("Ref id");

        final ModelDefinition model = newModel("ProductEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity"))
                     .thenReturn("Product");

            final Map<String, Object> ctx = ServiceTemplateContext.createGetReferenceByIdMethodContext(model);

            assertEquals("ProductEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("product", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("Ref id", ctx.get(TemplateContextConstants.ID_DESCRIPTION));
            assertEquals(true, ctx.get(TemplateContextConstants.GENERATE_JAVA_DOC));
        }
    }

    @Test
    void computeGetAllContext_shouldSetModelStrippedNameAndId() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");

        final ModelDefinition model = newModel("InvoiceEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("InvoiceEntity"))
                     .thenReturn("Invoice");

            final Map<String, Object> ctx = ServiceTemplateContext.computeGetAllContext(model);

            assertEquals("InvoiceEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("invoice", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
        }
    }

    @Test
    void computeCreateContext_shouldUseTransactionalWhenRetryableNotGenerated() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField, f1, f2);

        final ModelDefinition model = newModel("CustomerEntity", fields);

        final List<String> inputArgs = List.of("String name", "int age");
        final List<String> fieldNames = List.of("name", "age");
        final List<String> javadocFields = List.of("name - customer name", "age - customer age");

        when(idField.getName()).thenReturn("id");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingId(fields))
                      .thenReturn(inputArgs);
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(fields))
                      .thenReturn(fieldNames);
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldForJavadoc(fields))
                      .thenReturn(javadocFields);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("CustomerEntity"))
                     .thenReturn("Customer");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(false);

            final Map<String, Object> ctx = ServiceTemplateContext.computeCreateContext(model);

            assertEquals("CustomerEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals(AnnotationConstants.TRANSACTIONAL_ANNOTATION, ctx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));
            assertEquals("String name, int age", ctx.get(TemplateContextConstants.INPUT_ARGS));
            assertEquals("name, age", ctx.get(TemplateContextConstants.FIELD_NAMES));
            assertEquals(javadocFields, ctx.get(TemplateContextConstants.JAVADOC_FIELDS));
            assertEquals("customer", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
        }
    }

    @Test
    void computeCreateContext_shouldUseOptimisticLockingAnnotationWhenRetryableGenerated() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);

        final ModelDefinition model = newModel("CustomerEntity", fields);
        when(idField.getName()).thenReturn("id");

        final List<String> inputArgs = List.of("String name");
        final List<String> fieldNames = List.of("name");
        final List<String> javadocFields = List.of("name - customer name");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingId(fields))
                      .thenReturn(inputArgs);
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(fields))
                      .thenReturn(fieldNames);
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldForJavadoc(fields))
                      .thenReturn(javadocFields);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("CustomerEntity"))
                     .thenReturn("Customer");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(true);

            final Map<String, Object> ctx = ServiceTemplateContext.computeCreateContext(model);

            assertEquals(GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION,
                    ctx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));
        }
    }

    @Test
    void computeUpdateByIdContext_shouldFillAllFieldsAndUseTransactionalOrOptimisticLocking() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField, f1, f2);

        final ModelDefinition model = newModel("OrderEntity", fields);

        final List<String> inputFields = List.of("Long id", "String name");
        final List<String> fieldNamesWithoutId = List.of("name");
        final List<String> javadocFields = List.of("name - order name");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.generateInputArgsWithoutRelations(fields))
                      .thenReturn(inputFields);
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNames(fields))
                      .thenReturn(fieldNamesWithoutId);
            fieldUtils.when(() -> FieldUtils.extractFieldForJavadocWithoutRelations(fields))
                      .thenReturn(javadocFields);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(false);

            Map<String, Object> ctx = ServiceTemplateContext.computeUpdateByIdContext(model);

            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals(inputFields, ctx.get(TemplateContextConstants.INPUT_FIELDS));
            assertEquals(fieldNamesWithoutId, ctx.get(TemplateContextConstants.FIELD_NAMES_WITHOUT_ID));
            assertEquals(javadocFields, ctx.get(TemplateContextConstants.JAVADOC_FIELDS));
            assertEquals("order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(AnnotationConstants.TRANSACTIONAL_ANNOTATION,
                    ctx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(true);

            ctx = ServiceTemplateContext.computeUpdateByIdContext(model);
            assertEquals(GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION,
                    ctx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));
        }
    }

    @Test
    void computeGetByIdContext_shouldSetModelIdFieldsDescriptionAndJavaDocFlag() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getDescription()).thenReturn("Id of the entity");

        final ModelDefinition model = newModel("UserEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            final Map<String, Object> ctx = ServiceTemplateContext.computeGetByIdContext(model);

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("Id of the entity", ctx.get(TemplateContextConstants.ID_DESCRIPTION));
            assertEquals(true, ctx.get(TemplateContextConstants.GENERATE_JAVA_DOC));
            assertEquals("user", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
        }
    }

    @Test
    void computeDeleteByIdContext_shouldReuseGetByIdAndAddTransactionalAndStrippedName() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getDescription()).thenReturn("Id field");

        final ModelDefinition model = newModel("UserEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(false);

            final Map<String, Object> ctx = ServiceTemplateContext.computeDeleteByIdContext(model);

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("Id field", ctx.get(TemplateContextConstants.ID_DESCRIPTION));
            assertEquals(true, ctx.get(TemplateContextConstants.GENERATE_JAVA_DOC));
            assertEquals("user", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(AnnotationConstants.TRANSACTIONAL_ANNOTATION,
                    ctx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));
        }
    }

    @Test
    void createAddRelationMethodContext_shouldReturnEmptyMapWhenNoRelations() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctxAdd = ServiceTemplateContext.createAddRelationMethodContext(model);
            final Map<String, Object> ctxRemove = ServiceTemplateContext.createRemoveRelationMethodContext(model);

            assertTrue(ctxAdd.isEmpty(), "Add-relation context should be empty when there are no relations");
            assertTrue(ctxRemove.isEmpty(), "Remove-relation context should be empty when there are no relations");
        }
    }

    @Test
    void createAddRelationMethodContext_shouldBuildModelAndRelationsWithCollectionRelation() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("addresses");
        when(relationField.getType()).thenReturn("AddressEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final List<String> javadocFields = List.of("id - user id", "addresses - related addresses");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of("ManyToMany"));

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                      .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                      .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.computeJavadocForFields(idField, relationField))
                      .thenReturn(javadocFields);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("addresses"))
                     .thenReturn("addresses");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(false);

            final Map<String, Object> ctx = ServiceTemplateContext.createAddRelationMethodContext(model);

            assertTrue(ctx.containsKey(TemplateContextConstants.MODEL));
            assertTrue(ctx.containsKey(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final Map<String, Object> modelCtx =
                    (Map<String, Object>) ctx.get(TemplateContextConstants.MODEL);

            assertEquals("UserEntity", modelCtx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Long", modelCtx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", modelCtx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("UserService", modelCtx.get(TemplateContextConstants.MODEL_SERVICE));
            assertEquals("user", modelCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(AnnotationConstants.TRANSACTIONAL_ANNOTATION,
                    modelCtx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations =
                    (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> rel = relations.get(0);

            assertEquals("AddressEntity", rel.get(TemplateContextConstants.RELATION_CLASS_NAME));
            assertEquals("Address", rel.get(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME));
            assertEquals("addresses", rel.get(TemplateContextConstants.ELEMENT_PARAM));
            assertEquals("Addresses", rel.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals(true, rel.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals(javadocFields, rel.get(TemplateContextConstants.JAVADOC_FIELDS));
            assertEquals("addAddresses", rel.get(TemplateContextConstants.METHOD_NAME));
        }
    }

    @Test
    void createRemoveRelationMethodContext_shouldBuildContextAndUseOptimisticLockingAnnotation() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("java.util.UUID");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("role");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final List<String> javadocFields = List.of("id - user id", "role - user role");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of("ManyToOne"));

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.computeJavadocForFields(idField, relationField))
                      .thenReturn(javadocFields);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("role"))
                     .thenReturn("role");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity"))
                     .thenReturn("Role");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(true);

            final Map<String, Object> ctx = ServiceTemplateContext.createRemoveRelationMethodContext(model);

            @SuppressWarnings("unchecked")
            Map<String, Object> modelCtx =
                    (Map<String, Object>) ctx.get(TemplateContextConstants.MODEL);

            assertEquals("UserEntity", modelCtx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("java.util.UUID", modelCtx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", modelCtx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("UserService", modelCtx.get(TemplateContextConstants.MODEL_SERVICE));
            assertEquals("user", modelCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION,
                    modelCtx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations =
                    (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> rel = relations.get(0);

            assertEquals("RoleEntity", rel.get(TemplateContextConstants.RELATION_CLASS_NAME));
            assertEquals("Role", rel.get(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME));
            assertEquals("role", rel.get(TemplateContextConstants.ELEMENT_PARAM));
            assertEquals("Role", rel.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals(false, rel.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals(javadocFields, rel.get(TemplateContextConstants.JAVADOC_FIELDS));
            assertEquals("removeRole", rel.get(TemplateContextConstants.METHOD_NAME));
        }
    }

}
