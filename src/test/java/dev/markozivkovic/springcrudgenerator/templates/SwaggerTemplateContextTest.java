package dev.markozivkovic.springcrudgenerator.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.SwaggerUtils;

class SwaggerTemplateContextTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void computeSwaggerTemplateContext_shouldSetStrippedNameIdFieldAndDescription() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getDescription()).thenReturn("Primary identifier");

        final ModelDefinition model = newModel("UserEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            final Map<String, Object> ctx = SwaggerTemplateContext.computeSwaggerTemplateContext(model);

            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("Primary identifier", ctx.get(TemplateContextConstants.ID_DESCRIPTION));
        }
    }

    @Test
    void computeSwaggerTemplateContext_shouldUseEmptyDescriptionWhenNull() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getDescription()).thenReturn(null);

        final ModelDefinition model = newModel("UserEntity", List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            final Map<String, Object> ctx = SwaggerTemplateContext.computeSwaggerTemplateContext(model);

            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals("", ctx.get(TemplateContextConstants.ID_DESCRIPTION));
        }
    }

    @Test
    void computeBaseContext_shouldReturnStrippedModelNameOnly() {

        final ModelDefinition model = newModel("OrderEntity", List.of());

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            final Map<String, Object> ctx = SwaggerTemplateContext.computeBaseContext(model);

            assertEquals("Order", ctx.get("strippedModelName"));
        }
    }

    @Test
    void computeContextWithId_shouldIncludeBaseContextAndIdFieldName() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final ModelDefinition model = newModel("ProductEntity", List.of(idField));

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity"))
                     .thenReturn("Product");

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                      .thenReturn(idField);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeContextWithId(model);

            assertEquals("Product", ctx.get("strippedModelName"));
            assertEquals("id", ctx.get("idField"));
        }
    }

    @Test
    void computeRelationEndpointContext_shouldBuildRelationEndpointsForEachRelationField() {

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        when(userIdField.getName()).thenReturn("id");
        when(userIdField.getDescription()).thenReturn("User id");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getType()).thenReturn("AddressEntity");

        final RelationDefinition relation = mock(RelationDefinition.class);
        when(relation.getType()).thenReturn("ManyToOne");
        when(relationField.getRelation()).thenReturn(relation);

        final ModelDefinition userModel = newModel("UserEntity", List.of(userIdField, relationField));

        final FieldDefinition addressIdField = mock(FieldDefinition.class);
        when(addressIdField.getName()).thenReturn("addressId");

        final ModelDefinition addressModel = newModel("AddressEntity", List.of(addressIdField));

        final List<ModelDefinition> allModels = List.of(userModel, addressModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(userModel.getFields()))
                      .thenReturn(userIdField);

            fieldUtils.when(() -> FieldUtils.extractIdField(addressModel.getFields()))
                      .thenReturn(addressIdField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");

            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(userIdField))
                        .thenReturn(Map.of("type", "integer"));
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(addressIdField))
                        .thenReturn(Map.of("type", "string"));

            final Map<String, Object> ctx = SwaggerTemplateContext.computeRelationEndpointContext(userModel, allModels);

            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            @SuppressWarnings("unchecked")
            final Map<String, Object> idProp = (Map<String, Object>) ctx.get(TemplateContextConstants.ID);
            assertEquals("integer", idProp.get("type"));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations =
                    (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertEquals(1, relations.size());
            final Map<String, Object> relCtx = relations.get(0);

            assertEquals("Address", relCtx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("MANYTOONE", relCtx.get(TemplateContextConstants.RELATION_TYPE));
            assertEquals("addressId", relCtx.get(TemplateContextConstants.RELATED_ID_PARAM));

            @SuppressWarnings("unchecked")
            final Map<String, Object> relatedIdProp = (Map<String, Object>) relCtx.get(TemplateContextConstants.RELATED_ID);
            assertEquals("string", relatedIdProp.get("type"));
        }
    }

    @Test
    void computeRelationEndpointContext_shouldThrowWhenRelationModelNotFound() {

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        when(userIdField.getName()).thenReturn("id");
        when(userIdField.getDescription()).thenReturn(null);

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getType()).thenReturn("AddressEntity");

        final RelationDefinition relation = mock(RelationDefinition.class);
        when(relation.getType()).thenReturn("OneToMany");
        when(relationField.getRelation()).thenReturn(relation);

        final ModelDefinition userModel = newModel("UserEntity", List.of(userIdField, relationField));
        final List<ModelDefinition> allModels = List.of(userModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(userModel.getFields()))
                      .thenReturn(userIdField);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(userIdField))
                        .thenReturn(Map.of("type", "integer"));

            assertThrows(IllegalArgumentException.class,
                    () -> SwaggerTemplateContext.computeRelationEndpointContext(userModel, allModels));
        }
    }
}
