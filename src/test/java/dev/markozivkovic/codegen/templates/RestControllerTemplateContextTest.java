package dev.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.RelationDefinition;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;

class RestControllerTemplateContextTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void computeControllerClassContext_shouldSetClassNameStrippedNameRelationsAndJsonFields() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final FieldDefinition relationField = mock(FieldDefinition.class);

        final List<FieldDefinition> fields = List.of(idField, jsonField, relationField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                      .thenReturn("AddressEntity");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relationField));

            final Map<String, Object> ctx = RestControllerTemplateContext.computeControllerClassContext(model);

            assertEquals("UserController", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<String> jsonFields = (List<String>) ctx.get(TemplateContextConstants.JSON_FIELDS);

            assertEquals(1, jsonFields.size());
            assertTrue(jsonFields.contains("AddressEntity"));
        }
    }

    @Test
    void computeControllerClassContext_shouldHaveNoRelationsAndEmptyJsonFieldsWhenNonePresent() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("OrderEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx = RestControllerTemplateContext.computeControllerClassContext(model);

            assertEquals("OrderController", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("Order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<String> jsonFields =
                    (List<String>) ctx.get(TemplateContextConstants.JSON_FIELDS);

            assertTrue(jsonFields.isEmpty());
        }
    }

    @Test
    void computeCreateEndpointContext_shouldBuildInputFieldsWithoutRelationsOrJsonOrEnum() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final FieldDefinition nameField = mock(FieldDefinition.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getType()).thenReturn("String");
        when(nameField.getResolvedType()).thenReturn("java.lang.String");
        when(nameField.getRelation()).thenReturn(null);

        final List<FieldDefinition> fields = List.of(idField, nameField);
        final ModelDefinition model = newModel("ProductEntity", fields);

        final List<ModelDefinition> allEntities = List.of(model);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity"))
                    .thenReturn("Product");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                      .thenReturn(List.of());
            nameUtils.when(() -> ModelNameUtils.stripSuffix("String"))
                    .thenReturn("");

            fieldUtils.when(() -> FieldUtils.isJsonField(nameField))
                      .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFieldEnum(nameField))
                      .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeCreateEndpointContext(model, allEntities);

            assertEquals("ProductEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Product", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> inputFields =
                    (List<Map<String, Object>>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertEquals(1, inputFields.size());
            final Map<String, Object> fCtx = inputFields.get(0);

            assertEquals("name", fCtx.get(TemplateContextConstants.FIELD));
            assertEquals("java.lang.String", fCtx.get(TemplateContextConstants.FIELD_TYPE));
            assertEquals("", fCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(false, fCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals(false, fCtx.get(TemplateContextConstants.IS_RELATION));
            assertEquals(false, fCtx.get(TemplateContextConstants.IS_JSON_FIELD));
            assertEquals(false, fCtx.get(TemplateContextConstants.IS_ENUM));
            assertFalse(fCtx.containsKey(TemplateContextConstants.RELATION_ID_TYPE));
            assertFalse(fCtx.containsKey(TemplateContextConstants.RELATION_ID_FIELD));
        }
    }

    @Test
    void computeCreateEndpointContext_shouldHandleRelationFieldWithCollectionJsonEnumAndRelationId() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("addresses");
        when(relationField.getType()).thenReturn("AddressEntity");
        when(relationField.getResolvedType()).thenReturn("java.util.List<com.example.AddressEntity>");

        final RelationDefinition relation = mock(RelationDefinition.class);
        when(relationField.getRelation()).thenReturn(relation);

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final FieldDefinition addressIdField = mock(FieldDefinition.class);
        when(addressIdField.getName()).thenReturn("addressId");
        when(addressIdField.getType()).thenReturn("java.util.UUID");

        final ModelDefinition addressModel = newModel("AddressEntity", List.of(addressIdField));

        final List<ModelDefinition> allEntities = List.of(mainModel, addressModel);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                      .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                      .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.isJsonField(relationField))
                      .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isFieldEnum(relationField))
                      .thenReturn(true);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.extractIdField(addressModel.getFields()))
                      .thenReturn(addressIdField);

            final Map<String, Object> ctx = RestControllerTemplateContext.computeCreateEndpointContext(mainModel, allEntities);

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> inputFields =
                    (List<Map<String, Object>>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertEquals(1, inputFields.size());
            final Map<String, Object> fCtx = inputFields.get(0);

            assertEquals("addresses", fCtx.get(TemplateContextConstants.FIELD));
            assertEquals("java.util.List<com.example.AddressEntity>", fCtx.get(TemplateContextConstants.FIELD_TYPE));
            assertEquals("Address", fCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(true, fCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals(true, fCtx.get(TemplateContextConstants.IS_RELATION));
            assertEquals(true, fCtx.get(TemplateContextConstants.IS_JSON_FIELD));
            assertEquals(true, fCtx.get(TemplateContextConstants.IS_ENUM));
            assertEquals("java.util.UUID", fCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
            assertEquals("addressId", fCtx.get(TemplateContextConstants.RELATION_ID_FIELD));
        }
    }

    @Test
    void computeGetByIdEndpointContext_shouldSetModelStrippedNameAndIdType() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("java.util.UUID");

        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("OrderEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeGetByIdEndpointContext(model);

            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("java.util.UUID", ctx.get(TemplateContextConstants.ID_TYPE));
        }
    }

    @Test
    void computeGetAllEndpointContext_shouldSetModelAndStrippedName() {
        final ModelDefinition model = newModel("CustomerEntity", new ArrayList<>());

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("CustomerEntity"))
                     .thenReturn("Customer");

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeGetAllEndpointContext(model);

            assertEquals("CustomerEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Customer", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
        }
    }

    @Test
    void computeUpdateEndpointContext_shouldSetContextWithSwaggerTrue() {
        
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("java.util.UUID");

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);

        final List<FieldDefinition> fields = List.of(idField, f1, f2);
        final ModelDefinition model = newModel("UserEntity", fields);

        final List<String> inputNamesSwaggerTrue = List.of("name", "email");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            fieldUtils.when(
                    () -> FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, true)
            ).thenReturn(inputNamesSwaggerTrue);

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeUpdateEndpointContext(model, true);

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("java.util.UUID", ctx.get(TemplateContextConstants.ID_TYPE));

            @SuppressWarnings("unchecked")
            final List<String> inputFields =
                    (List<String>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertEquals(inputNamesSwaggerTrue, inputFields);
        }
    }

    @Test
    void computeUpdateEndpointContext_shouldSetContextWithSwaggerFalse() {
        
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField, f1);
        final ModelDefinition model = newModel("OrderEntity", fields);

        final List<String> inputNamesSwaggerFalse = List.of("amount");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                    .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, false))
                    .thenReturn(inputNamesSwaggerFalse);

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeUpdateEndpointContext(model, false);

            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));

            @SuppressWarnings("unchecked")
            final List<String> inputFields = (List<String>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertEquals(inputNamesSwaggerFalse, inputFields);
        }
    }

    @Test
    void computeDeleteEndpointContext_shouldUseStrippedModelNameAndIdType() {
        
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("java.util.UUID");

        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("InvoiceEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("InvoiceEntity"))
                    .thenReturn("Invoice");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeDeleteEndpointContext(model);

            assertEquals("Invoice", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("java.util.UUID", ctx.get(TemplateContextConstants.ID_TYPE));
        }
    }

    @Test
    void computeAddResourceRelationEndpointContext_shouldReturnEmptyMapWhenNoRelations() {
        
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx = RestControllerTemplateContext.computeAddResourceRelationEndpointContext(model);

            assertTrue(ctx.isEmpty(), "Expected empty context when there are no relation types");
        }
    }

    @Test
    void computeAddResourceRelationEndpointContext_shouldBuildModelAndRelations() {
        
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                    .thenReturn(List.of("ManyToMany"));

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                    .thenReturn(List.of(relationField));

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity"))
                    .thenReturn("Role");

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeAddResourceRelationEndpointContext(model);

            assertTrue(ctx.containsKey(TemplateContextConstants.MODEL));
            assertTrue(ctx.containsKey(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final Map<String, Object> modelCtx = (Map<String, Object>) ctx.get(TemplateContextConstants.MODEL);

            assertEquals("UserEntity", modelCtx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("User", modelCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("Long", modelCtx.get(TemplateContextConstants.ID_TYPE));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("roles", relCtx.get(TemplateContextConstants.RELATION_FIELD_MODEL));
            assertEquals("Role", relCtx.get(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME));

            assertEquals("usersIdRolesPost", relCtx.get(TemplateContextConstants.METHOD_NAME));
        }
    }

    @Test
    void computeRemoveResourceRelationEndpointContext_shouldReturnEmptyMapWhenNoRelations() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final List<ModelDefinition> entities = List.of(model);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeRemoveResourceRelationEndpointContext(model, entities);

            assertTrue(ctx.isEmpty(), "Expected empty context when model has no relations");
        }
    }

    @Test
    void computeRemoveResourceRelationEndpointContext_shouldBuildModelAndRelationsWithCollectionRelation() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final FieldDefinition roleIdField = mock(FieldDefinition.class);
        when(roleIdField.getName()).thenReturn("id");
        when(roleIdField.getType()).thenReturn("java.util.UUID");

        final ModelDefinition roleModel = newModel("RoleEntity", List.of(roleIdField));

        final List<ModelDefinition> entities = List.of(mainModel, roleModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of("ManyToMany"));

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                    .thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                    .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                    .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractIdField(roleModel.getFields()))
                      .thenReturn(roleIdField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity"))
                    .thenReturn("Role");

            final Map<String, Object> ctx =
                    RestControllerTemplateContext.computeRemoveResourceRelationEndpointContext(mainModel, entities);

            assertTrue(ctx.containsKey(TemplateContextConstants.MODEL));
            assertTrue(ctx.containsKey(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final Map<String, Object> modelCtx = (Map<String, Object>) ctx.get(TemplateContextConstants.MODEL);

            assertEquals("User", modelCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("Long", modelCtx.get(TemplateContextConstants.ID_TYPE));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("roles", relCtx.get(TemplateContextConstants.RELATION_FIELD_MODEL));
            assertEquals("Role", relCtx.get(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME));
            assertEquals("usersIdRolesDelete", relCtx.get(TemplateContextConstants.METHOD_NAME));
            assertEquals("java.util.UUID", relCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
            assertEquals("roleId", relCtx.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals(true, relCtx.get(TemplateContextConstants.IS_COLLECTION));
        }
    }

    @Test
    void computeRemoveResourceRelationEndpointContext_shouldThrowWhenRelationEntityNotFound() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final List<ModelDefinition> entities = List.of(mainModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of("ManyToMany"));

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                      .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                      .thenReturn(List.of());

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            assertThrows(NoSuchElementException.class,
                    () -> RestControllerTemplateContext.computeRemoveResourceRelationEndpointContext(mainModel, entities),
                    "Expected NoSuchElementException when relation entity is missing from entities list"
            );
        }
    }

}
