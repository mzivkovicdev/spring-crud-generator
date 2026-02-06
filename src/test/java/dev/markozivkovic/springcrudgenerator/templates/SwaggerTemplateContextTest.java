package dev.markozivkovic.springcrudgenerator.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerObjectModeEnum;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerSchemaModeEnum;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;
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

    @Test
    void computeObjectContext_defaultMode_shouldIncludeAllFields_andSetTitleRequired_andDescription_andSchemaName() {

        final ModelDefinition e = mock(ModelDefinition.class);
        when(e.getName()).thenReturn("OrderModel");
        when(e.getDescription()).thenReturn("Some description");

        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField, f1, f2);
        when(e.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("OrderModel")).thenReturn("Order");
            modelNameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Order")).thenReturn("Order");
            stringUtils.when(() -> StringUtils.isNotBlank("Some description")).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractRequiredFields(fields)).thenReturn(List.of("id", "name"));

            final Map<String, Object> pId = Map.of("name", "id");
            final Map<String, Object> p1 = Map.of("name", "f1");
            final Map<String, Object> p2 = Map.of("name", "f2");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(idField)).thenReturn(pId);
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(f1)).thenReturn(p1);
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(f2)).thenReturn(p2);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeObjectContext(e, SwaggerObjectModeEnum.DEFAULT);

            assertEquals("OrderModel", ctx.get(TemplateContextConstants.SCHEMA_NAME));
            assertEquals("Some description", ctx.get(TemplateContextConstants.DESCRIPTION));
            assertEquals("Order", ctx.get(TemplateContextConstants.TITLE));
            assertEquals(List.of("id", "name"), ctx.get(TemplateContextConstants.REQUIRED));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> props = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.PROPERTIES);
            assertEquals(List.of(pId, p1, p2), props);

            modelNameUtils.verify(() -> ModelNameUtils.stripSuffix("OrderModel"));
            modelNameUtils.verify(() -> ModelNameUtils.computeOpenApiModelName("Order"));
            fieldUtils.verify(() -> FieldUtils.extractIdField(fields));
            fieldUtils.verify(() -> FieldUtils.extractRequiredFields(fields));
            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(idField));
            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(f1));
            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(f2));
        }
    }

    @Test
    void computeObjectContext_createMode_shouldExcludeIdField_andUseInputSchemaMode() {
    
        final ModelDefinition e = mock(ModelDefinition.class);
        when(e.getName()).thenReturn("OrderModel");
        when(e.getDescription()).thenReturn("");

        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(idField, f1);
        when(e.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("OrderModel")).thenReturn("Order");
            modelNameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("Order")).thenReturn("OrderCreate");
            stringUtils.when(() -> StringUtils.isNotBlank("")).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractRequiredFieldsForCreate(fields)).thenReturn(List.of("name"));

            final Map<String, Object> p1 = Map.of("name", "f1", "mode", "INPUT");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(f1, SwaggerSchemaModeEnum.INPUT)).thenReturn(p1);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeObjectContext(e, SwaggerObjectModeEnum.CREATE_MODEL);

            assertEquals("OrderModel", ctx.get(TemplateContextConstants.SCHEMA_NAME));
            assertNull(ctx.get(TemplateContextConstants.DESCRIPTION));
            assertEquals("OrderCreate", ctx.get(TemplateContextConstants.TITLE));
            assertEquals(List.of("name"), ctx.get(TemplateContextConstants.REQUIRED));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> props = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.PROPERTIES);
            assertEquals(List.of(p1), props);

            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(f1, SwaggerSchemaModeEnum.INPUT));
            swaggerUtils.verifyNoMoreInteractions();
        }
    }

    @Test
    void computeObjectContext_updateMode_shouldExcludeIdField_andExcludeRelationFields() {
    
        final ModelDefinition e = mock(ModelDefinition.class);
        when(e.getName()).thenReturn("OrderModel");
        when(e.getDescription()).thenReturn(null);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition relField = mock(FieldDefinition.class);
        when(relField.getRelation()).thenReturn(new RelationDefinition());

        final FieldDefinition normalField = mock(FieldDefinition.class);
        when(normalField.getRelation()).thenReturn(null);

        final List<FieldDefinition> fields = List.of(idField, relField, normalField);
        when(e.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("OrderModel")).thenReturn("Order");
            modelNameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("Order")).thenReturn("OrderUpdate");
            stringUtils.when(() -> StringUtils.isNotBlank(null)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractRequiredFieldsForUpdate(fields)).thenReturn(List.of());
            final Map<String, Object> pNormal = Map.of("name", "normal");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(normalField)).thenReturn(pNormal);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeObjectContext(e, SwaggerObjectModeEnum.UPDATE_MODEL);

            assertEquals("OrderUpdate", ctx.get(TemplateContextConstants.TITLE));
            assertEquals(List.of(), ctx.get(TemplateContextConstants.REQUIRED));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> props = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.PROPERTIES);
            assertEquals(List.of(pNormal), props);

            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(normalField));
            swaggerUtils.verifyNoMoreInteractions();
        }
    }

    @Test
    void computeObjectContext_jsonMode_shouldNotExtractIdField_andShouldIncludeAllFields_withNormalPropertyMapping() {

        final ModelDefinition e = mock(ModelDefinition.class);
        when(e.getName()).thenReturn("OrderModel");
        when(e.getDescription()).thenReturn("desc");

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1, f2);
        when(e.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("OrderModel")).thenReturn("Order");
            modelNameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Order")).thenReturn("Order");
            stringUtils.when(() -> StringUtils.isNotBlank("desc")).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRequiredFields(fields)).thenReturn(List.of("f1"));

            final Map<String, Object> p1 = Map.of("name", "f1");
            final Map<String, Object> p2 = Map.of("name", "f2");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(f1)).thenReturn(p1);
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(f2)).thenReturn(p2);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeObjectContext(e, SwaggerObjectModeEnum.JSON_MODEL);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> props = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.PROPERTIES);
            assertEquals(List.of(p1, p2), props);
            fieldUtils.verify(() -> FieldUtils.extractIdField(any()), never());
        }
    }

    @Test
    void computeObjectContext_defaultMode_withAuditEnabled_shouldSetAuditFields() {
    
        final ModelDefinition e = mock(ModelDefinition.class);
        when(e.getName()).thenReturn("OrderModel");
        when(e.getDescription()).thenReturn(null);

        final AuditDefinition auditDef = mock(AuditDefinition.class);
        when(auditDef.getEnabled()).thenReturn(true);
        when(auditDef.getType()).thenReturn(AuditTypeEnum.INSTANT);
        when(e.getAudit()).thenReturn(auditDef);

        final List<FieldDefinition> fields = List.of(mock(FieldDefinition.class));
        when(e.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("OrderModel")).thenReturn("Order");
            modelNameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Order")).thenReturn("Order");
            stringUtils.when(() -> StringUtils.isNotBlank(null)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(fields.get(0));
            fieldUtils.when(() -> FieldUtils.extractRequiredFields(fields)).thenReturn(List.of());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(Map.of("name", "x"));
            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT)).thenReturn(AuditTypeEnum.INSTANT.name());

            final Object resolvedAudit = Map.of("audit", "ok");
            swaggerUtils.when(() -> SwaggerUtils.resolve(eq(AuditTypeEnum.INSTANT.name()), eq(List.of()))).thenReturn(resolvedAudit);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeObjectContext(e, SwaggerObjectModeEnum.DEFAULT);

            assertEquals(true, ctx.get(TemplateContextConstants.AUDIT_ENABLED));
            assertEquals(resolvedAudit, ctx.get(TemplateContextConstants.AUDIT_TYPE));
        }
    }

    @Test
    void computeRelationInputModelContext_whenDescriptionNotBlank_shouldSetDescription_andBuildIdOnlyProperties() {

        final ModelDefinition relationModel = mock(ModelDefinition.class);
        when(relationModel.getName()).thenReturn("OrderModel");
        when(relationModel.getDescription()).thenReturn("rel desc");

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        final List<FieldDefinition> fields = List.of(idField);
        when(relationModel.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("OrderModel")).thenReturn("Order");
            stringUtils.when(() -> StringUtils.isNotBlank("rel desc")).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);

            final Map<String, Object> idProperty = new java.util.HashMap<>();
            idProperty.put("type", "integer");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(idField)).thenReturn(idProperty);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeRelationInputModelContext(relationModel);

            assertEquals("OrderModel", ctx.get(TemplateContextConstants.SCHEMA_NAME));
            assertEquals("OrderInput", ctx.get(TemplateContextConstants.TITLE));
            assertEquals("rel desc", ctx.get(TemplateContextConstants.DESCRIPTION));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> props = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.PROPERTIES);
            assertNotNull(props);
            assertEquals(1, props.size());

            final Map<String, Object> onlyProp = props.get(0);
            assertSame(idProperty, onlyProp);
            assertEquals("id", onlyProp.get(TemplateContextConstants.NAME));
            assertEquals("integer", onlyProp.get("type"));

            modelNameUtils.verify(() -> ModelNameUtils.stripSuffix("OrderModel"));
            stringUtils.verify(() -> StringUtils.isNotBlank("rel desc"));
            fieldUtils.verify(() -> FieldUtils.extractIdField(fields));
            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(idField));
        }
    }

    @Test
    void computeRelationInputModelContext_whenDescriptionBlank_shouldNotPutDescription() {

        final ModelDefinition relationModel = mock(ModelDefinition.class);
        when(relationModel.getName()).thenReturn("ProductModel");
        when(relationModel.getDescription()).thenReturn("   ");

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("productId");

        final List<FieldDefinition> fields = List.of(idField);
        when(relationModel.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("ProductModel")).thenReturn("Product");
            stringUtils.when(() -> StringUtils.isNotBlank("   ")).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);

            final Map<String, Object> idProperty = new java.util.HashMap<>();
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(idField)).thenReturn(idProperty);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeRelationInputModelContext(relationModel);

            assertEquals("ProductModel", ctx.get(TemplateContextConstants.SCHEMA_NAME));
            assertEquals("ProductInput", ctx.get(TemplateContextConstants.TITLE));
            assertFalse(ctx.containsKey(TemplateContextConstants.DESCRIPTION));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> props = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.PROPERTIES);
            assertEquals(1, props.size());
            assertEquals("productId", props.get(0).get(TemplateContextConstants.NAME));

            stringUtils.verify(() -> StringUtils.isNotBlank("   "));
            fieldUtils.verify(() -> FieldUtils.extractIdField(fields));
            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(idField));
        }
    }

    @Test
    void computeRelationInputModelContext_shouldAlwaysAddNameToSwaggerPropertyMap() {

        final ModelDefinition relationModel = mock(ModelDefinition.class);
        when(relationModel.getName()).thenReturn("XModel");
        when(relationModel.getDescription()).thenReturn(null);

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final List<FieldDefinition> fields = List.of(idField);
        when(relationModel.getFields()).thenReturn(fields);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = Mockito.mockStatic(SwaggerUtils.class);
             final MockedStatic<StringUtils> stringUtils = Mockito.mockStatic(StringUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("XModel")).thenReturn("X");
            stringUtils.when(() -> StringUtils.isNotBlank(null)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);

            final Map<String, Object> idProperty = new java.util.HashMap<>();
            idProperty.put(TemplateContextConstants.NAME, "WRONG");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(idField)).thenReturn(idProperty);

            final Map<String, Object> ctx = SwaggerTemplateContext.computeRelationInputModelContext(relationModel);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> props = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.PROPERTIES);
            assertEquals("id", props.get(0).get(TemplateContextConstants.NAME));
        }
    }

}
