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

class BusinessServiceTemplateContextTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void computeBusinessServiceContext_shouldReturnOnlyOwnServiceWhenNoRelations() {
        final List<FieldDefinition> fields = List.of();
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx =
                    BusinessServiceTemplateContext.computeBusinessServiceContext(model);

            assertEquals("UserBusinessService", ctx.get(TemplateContextConstants.CLASS_NAME));

            @SuppressWarnings("unchecked")
            final List<String> services =
                    (List<String>) ctx.get(TemplateContextConstants.SERVICE_CLASSES);

            assertEquals(1, services.size());
            assertEquals("UserService", services.get(0));
        }
    }

    @Test
    void computeBusinessServiceContext_shouldIncludeRelationServiceClasses() {
        final FieldDefinition relCustomer = mock(FieldDefinition.class);
        when(relCustomer.getType()).thenReturn("CustomerEntity");

        final FieldDefinition relProduct = mock(FieldDefinition.class);
        when(relProduct.getType()).thenReturn("ProductEntity");

        final List<FieldDefinition> fields = List.of(relCustomer, relProduct);
        final ModelDefinition model = newModel("OrderEntity", fields);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("CustomerEntity"))
                     .thenReturn("Customer");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity"))
                     .thenReturn("Product");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relCustomer, relProduct));

            final Map<String, Object> ctx =
                    BusinessServiceTemplateContext.computeBusinessServiceContext(model);

            assertEquals("OrderBusinessService", ctx.get(TemplateContextConstants.CLASS_NAME));

            @SuppressWarnings("unchecked")
            final List<String> services = (List<String>) ctx.get(TemplateContextConstants.SERVICE_CLASSES);

            assertEquals(List.of("CustomerService", "ProductService", "OrderService"), services);
        }
    }

    @Test
    void computeAddRelationMethodServiceContext_shouldReturnEmptyMapWhenNoRelations() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final List<ModelDefinition> entities = List.of(model);

        try (MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx =
                    BusinessServiceTemplateContext.computeAddRelationMethodServiceContext(model, entities);

            assertTrue(ctx.isEmpty());
        }
    }

    @Test
    void computeAddRelationMethodServiceContext_shouldBuildModelAndRelationsWithTransactionalAnnotationDefault() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final FieldDefinition roleIdField = mock(FieldDefinition.class);
        when(roleIdField.getName()).thenReturn("roleId");
        when(roleIdField.getType()).thenReturn("java.util.UUID");
        final ModelDefinition roleModel = newModel("RoleEntity", List.of(roleIdField));

        final List<ModelDefinition> entities = List.of(mainModel, roleModel);

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
            fieldUtils.when(() -> FieldUtils.extractIdField(roleModel.getFields()))
                      .thenReturn(roleIdField);
            fieldUtils.when(() -> FieldUtils.computeJavadocForFields(idField, relationField))
                      .thenReturn(List.of("id", "roles"));

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity"))
                     .thenReturn("Role");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("roles"))
                     .thenReturn("roles");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(false);

            final Map<String, Object> ctx =
                    BusinessServiceTemplateContext.computeAddRelationMethodServiceContext(mainModel, entities);

            assertTrue(ctx.containsKey(TemplateContextConstants.MODEL));
            assertTrue(ctx.containsKey(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final Map<String, Object> modelCtx =
                    (Map<String, Object>) ctx.get(TemplateContextConstants.MODEL);

            assertEquals("UserEntity", modelCtx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals(AnnotationConstants.TRANSACTIONAL_ANNOTATION,
                    modelCtx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));
            assertEquals("Long", modelCtx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", modelCtx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("UserService", modelCtx.get(TemplateContextConstants.MODEL_SERVICE));
            assertEquals("user", modelCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations =
                    (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("RoleEntity", relCtx.get(TemplateContextConstants.RELATION_CLASS_NAME));
            assertEquals("Role", relCtx.get(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME));
            assertEquals("roles", relCtx.get(TemplateContextConstants.ELEMENT_PARAM));
            assertEquals("Roles", relCtx.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals(true, relCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals(List.of("id", "roles"), relCtx.get(TemplateContextConstants.JAVADOC_FIELDS));
            assertEquals("addRoles", relCtx.get(TemplateContextConstants.METHOD_NAME));
            assertEquals("java.util.UUID", relCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
            assertEquals("roleId", relCtx.get(TemplateContextConstants.RELATION_ID_FIELD));
        }
    }

    @Test
    void computeRemoveRelationMethodServiceContext_shouldUseOptimisticLockingAnnotationAndRemoveMethodName() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final FieldDefinition roleIdField = mock(FieldDefinition.class);
        when(roleIdField.getName()).thenReturn("roleId");
        when(roleIdField.getType()).thenReturn("java.util.UUID");
        final ModelDefinition roleModel = newModel("RoleEntity", List.of(roleIdField));

        final List<ModelDefinition> entities = List.of(mainModel, roleModel);

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
            fieldUtils.when(() -> FieldUtils.extractIdField(roleModel.getFields()))
                      .thenReturn(roleIdField);
            fieldUtils.when(() -> FieldUtils.computeJavadocForFields(idField, relationField))
                      .thenReturn(List.of("id", "roles"));

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity"))
                     .thenReturn("Role");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("roles"))
                     .thenReturn("roles");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(true);

            final Map<String, Object> ctx =
                    BusinessServiceTemplateContext.computeRemoveRelationMethodServiceContext(mainModel, entities);

            @SuppressWarnings("unchecked")
            final Map<String, Object> modelCtx =
                    (Map<String, Object>) ctx.get(TemplateContextConstants.MODEL);

            assertEquals(GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION,
                    modelCtx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("removeRoles", relCtx.get(TemplateContextConstants.METHOD_NAME));
        }
    }

    @Test
    void computeCreateResourceMethodServiceContext_shouldReturnEmptyMapWhenNoRelations() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);
        final List<ModelDefinition> entities = List.of(model);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx =
                    BusinessServiceTemplateContext.computeCreateResourceMethodServiceContext(model, entities);

            assertTrue(ctx.isEmpty());
        }
    }

    @Test
    void computeCreateResourceMethodServiceContext_shouldBuildModelAndRelationsWithOptimisticLocking() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final FieldDefinition roleIdField = mock(FieldDefinition.class);
        when(roleIdField.getName()).thenReturn("roleId");
        when(roleIdField.getType()).thenReturn("java.util.UUID");
        final ModelDefinition roleModel = newModel("RoleEntity", List.of(roleIdField));

        final List<ModelDefinition> entities = List.of(mainModel, roleModel);

        final List<String> inputArgsExcludingId = List.of("String name", "List<RoleEntity> roles");
        final List<String> testInputArgsList = List.of("name", "roles");
        final List<String> fieldNamesList = List.of("name", "roles");

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

            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingId(fields, entities))
                      .thenReturn(inputArgsExcludingId);
            fieldUtils.when(() -> FieldUtils.generateInputArgsExcludingIdForTest(fields, entities))
                      .thenReturn(testInputArgsList);
            fieldUtils.when(() -> FieldUtils.generateInputArgsBusinessService(fields))
                      .thenReturn(fieldNamesList);

            fieldUtils.when(() -> FieldUtils.extractIdField(roleModel.getFields()))
                      .thenReturn(roleIdField);
            fieldUtils.when(() -> FieldUtils.computeJavadocForFields(idField, relationField))
                      .thenReturn(List.of("id", "roles"));

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity"))
                     .thenReturn("Role");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("roles"))
                     .thenReturn("roles");

            genCtx.when(() -> GeneratorContext.isGenerated(TemplateContextConstants.RETRYABLE_ANNOTATION))
                  .thenReturn(true);

            final Map<String, Object> ctx =
                    BusinessServiceTemplateContext.computeCreateResourceMethodServiceContext(mainModel, entities);

            assertTrue(ctx.containsKey(TemplateContextConstants.MODEL));
            assertTrue(ctx.containsKey(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final Map<String, Object> modelCtx = (Map<String, Object>) ctx.get(TemplateContextConstants.MODEL);

            assertEquals("UserEntity", modelCtx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("User", modelCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY_ANNOTATION,
                    modelCtx.get(TemplateContextConstants.TRANSACTIONAL_ANNOTATION));
            assertEquals("UserService", modelCtx.get(TemplateContextConstants.MODEL_SERVICE));
            assertEquals("String name, List<RoleEntity> roles", modelCtx.get(TemplateContextConstants.INPUT_ARGS));
            assertEquals("name, roles", modelCtx.get(TemplateContextConstants.FIELD_NAMES));
            assertEquals("name, roles", modelCtx.get(TemplateContextConstants.TEST_INPUT_ARGS));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("RoleEntity", relCtx.get(TemplateContextConstants.RELATION_CLASS_NAME));
            assertEquals("Role", relCtx.get(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME));
            assertEquals("roles", relCtx.get(TemplateContextConstants.ELEMENT_PARAM));
            assertEquals("Roles", relCtx.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals(true, relCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals("java.util.UUID", relCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
            assertEquals("roleId", relCtx.get(TemplateContextConstants.RELATION_ID_FIELD));
            assertEquals("addRoles", relCtx.get(TemplateContextConstants.METHOD_NAME));
        }
    }
}
