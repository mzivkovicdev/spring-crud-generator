package com.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.RelationDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;

class GraphQlTemplateContextTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void computeGraphQlResolver_shouldSetStrippedNameClassNameJsonFieldsAndRelationsTrue() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final FieldDefinition relField = mock(FieldDefinition.class);

        final List<FieldDefinition> fields = List.of(idField, jsonField, relField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                      .thenReturn("Address");

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of("ManyToOne"));

            final Map<String, Object> ctx =
                    GraphQlTemplateContext.computeGraphQlResolver(model);

            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("UserResolver", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<String> jsonFields =
                    (List<String>) ctx.get(TemplateContextConstants.JSON_FIELDS);

            assertEquals(1, jsonFields.size());
            assertTrue(jsonFields.contains("Address"));
        }
    }

    @Test
    void computeGraphQlResolver_shouldHaveEmptyJsonFieldsAndRelationsFalseWhenNonePresent() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("OrderEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx =
                    GraphQlTemplateContext.computeGraphQlResolver(model);

            assertEquals("Order", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("OrderResolver", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<String> jsonFields =
                    (List<String>) ctx.get(TemplateContextConstants.JSON_FIELDS);

            assertTrue(jsonFields.isEmpty());
        }
    }

    @Test
    void computeGraphQlSchemaContext_shouldCloneRelationFieldsAndCollectJsonModels() {
     
        final RelationDefinition relDef = mock(RelationDefinition.class);

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("addresses");
        when(relationField.getType()).thenReturn("AddressEntity");
        when(relationField.getRelation()).thenReturn(relDef);

     
        final FieldDefinition normalField = mock(FieldDefinition.class);
        when(normalField.getName()).thenReturn("name");
        when(normalField.getType()).thenReturn("String");
        when(normalField.getRelation()).thenReturn(null);

        final List<FieldDefinition> fields = List.of(relationField, normalField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final ModelDefinition addressModel = newModel("AddressEntity", List.of());
        final ModelDefinition otherModel = newModel("OtherEntity", List.of());
        final List<ModelDefinition> allEntities = List.of(addressModel, otherModel);

        final FieldDefinition clonedRelationField = mock(FieldDefinition.class);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");

            fieldUtils.when(() -> FieldUtils.cloneFieldDefinition(relationField))
                      .thenReturn(clonedRelationField);
            when(clonedRelationField.setType("Address")).thenReturn(clonedRelationField);

            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList()))
                      .thenReturn(List.of(clonedRelationField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(clonedRelationField))
                      .thenReturn("AddressEntity");

            final Map<String, Object> ctx =
                    GraphQlTemplateContext.computeGraphQlSchemaContext(mainModel, allEntities);

            assertEquals("User", ctx.get(TemplateContextConstants.NAME));

            @SuppressWarnings("unchecked")
            final List<FieldDefinition> ctxFields =
                    (List<FieldDefinition>) ctx.get(TemplateContextConstants.FIELDS);

            assertEquals(2, ctxFields.size());
            assertSame(clonedRelationField, ctxFields.get(0));
            assertSame(normalField, ctxFields.get(1));

            @SuppressWarnings("unchecked")
            final List<ModelDefinition> jsonModels = (List<ModelDefinition>) ctx.get(TemplateContextConstants.JSON_MODELS);

            assertEquals(1, jsonModels.size());
            assertSame(addressModel, jsonModels.get(0));
        }
    }

    @Test
    void computeMutationMappingGraphQL_shouldBuildContextWithRelationsAndCollections() {
        
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("orders");
        when(relationField.getType()).thenReturn("OrderEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final FieldDefinition orderIdField = mock(FieldDefinition.class);
        when(orderIdField.getType()).thenReturn("java.util.UUID");
        final ModelDefinition orderModel = newModel("OrderEntity", List.of(orderIdField));

        final List<ModelDefinition> allEntities = List.of(mainModel, orderModel);

        final List<String> noRelArgs = List.of("String name");
        final List<String> withRelArgs = List.of("String name", "Set<OrderEntity> orders");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("orders"))
                    .thenReturn("orders");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractIdField(orderModel.getFields()))
                    .thenReturn(orderIdField);

            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(fields))
                    .thenReturn(noRelArgs);
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(fields))
                    .thenReturn(withRelArgs);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                    .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                    .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                    .thenReturn(List.of());

            final Map<String, Object> ctx =
                    GraphQlTemplateContext.computeMutationMappingGraphQL(mainModel, allEntities);

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals(noRelArgs, ctx.get(TemplateContextConstants.INPUT_FIELDS_WITHOUT_RELATIONS));
            assertEquals(withRelArgs, ctx.get(TemplateContextConstants.INPUT_FIELDS_WITH_RELATIONS));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("Orders", relCtx.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals(true, relCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals("java.util.UUID", relCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
        }
    }

    @Test
    void computeMutationMappingGraphQL_shouldSetIsCollectionFalseWhenNotManyOrOneToMany() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("profile");
        when(relationField.getType()).thenReturn("ProfileEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final FieldDefinition profileIdField = mock(FieldDefinition.class);
        when(profileIdField.getType()).thenReturn("java.util.UUID");
        final ModelDefinition profileModel = newModel("ProfileEntity", List.of(profileIdField));

        final List<ModelDefinition> allEntities = List.of(mainModel, profileModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("profile"))
                    .thenReturn("profile");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractIdField(profileModel.getFields()))
                    .thenReturn(profileIdField);

            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(fields))
                    .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(fields))
                    .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                    .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                    .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                    .thenReturn(List.of());

            final Map<String, Object> ctx =
                    GraphQlTemplateContext.computeMutationMappingGraphQL(mainModel, allEntities);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("Profile", relCtx.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals(false, relCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals("java.util.UUID", relCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
        }
    }

    @Test
    void computeMutationMappingGraphQL_shouldThrowWhenRelationEntityNotFound() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("orders");
        when(relationField.getType()).thenReturn("OrderEntity");

        final List<FieldDefinition> fields = List.of(idField, relationField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final List<ModelDefinition> allEntities = List.of(mainModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("orders"))
                    .thenReturn("orders");

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(fields))
                    .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(fields))
                    .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                    .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields))
                    .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields))
                    .thenReturn(List.of());

            assertThrows(
                    NoSuchElementException.class,
                    () -> GraphQlTemplateContext.computeMutationMappingGraphQL(mainModel, allEntities)
            );
        }
    }

    // ---------------------------------------------------
    // computeQueryMappingGraphQL
    // ---------------------------------------------------

    @Test
    void computeQueryMappingGraphQL_shouldBuildSimpleContext() {
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("java.util.UUID");

        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("InvoiceEntity", fields);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("InvoiceEntity"))
                     .thenReturn("Invoice");

            final Map<String, Object> ctx =
                    GraphQlTemplateContext.computeQueryMappingGraphQL(model);

            assertEquals("InvoiceEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Invoice", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("java.util.UUID", ctx.get(TemplateContextConstants.ID_TYPE));
        }
    }
}
