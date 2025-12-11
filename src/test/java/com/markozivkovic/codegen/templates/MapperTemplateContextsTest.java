package com.markozivkovic.codegen.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.imports.MapperImports;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;

class MapperTemplateContextsTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void computeMapperContext_shouldBuildRestMapperContextWithoutRelationsOrJsonOrSwagger() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);
        final ModelDefinition model = newModel("UserEntity", fields);

        final String packagePath = "com.example.app";
        final String expectedImports = "IMPORTS_REST";

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of());

            mapperImports.when(() -> MapperImports.computeMapperImports(
                    packagePath, model, pkgCfg, false, false
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, false, false, pkgCfg
            );

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("UserRestMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals("UserTO", ctx.get(TemplateContextConstants.TRANSFER_OBJECT_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.SWAGGER));
            assertFalse(ctx.containsKey(TemplateContextConstants.SWAGGER_MODEL));
            assertFalse(ctx.containsKey(TemplateContextConstants.PARAMETERS));
            assertEquals(expectedImports, ctx.get("projectImports"));
        }
    }

    @Test
    void computeMapperContext_shouldSetSwaggerModelWhenSwaggerIsTrue() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);
        final ModelDefinition model = newModel("OrderEntity", fields);

        final String packagePath = "com.example.app";
        final String expectedImports = "IMPORTS_SWAGGER";

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("OrderEntity"))
                     .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of());

            mapperImports.when(() -> MapperImports.computeMapperImports(
                    packagePath, model, pkgCfg, true, false
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, true, false, pkgCfg
            );

            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("OrderRestMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals("OrderTO", ctx.get(TemplateContextConstants.TRANSFER_OBJECT_NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.SWAGGER));
            assertEquals("Order", ctx.get(TemplateContextConstants.SWAGGER_MODEL));
            assertEquals(expectedImports, ctx.get("projectImports"));
        }
    }

    @Test
    void computeMapperContext_shouldComputeGraphQlMapperParametersFromRelationsAndJsonFields() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getType()).thenReturn("AddressEntity");

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        when(jsonField.getType()).thenReturn("String");

        final List<FieldDefinition> fields = List.of(relationField, jsonField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final String packagePath = "com.example.app";
        final String expectedImports = "IMPORTS_GRAPHQL";

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField))
                      .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                      .thenReturn("Address");

            fieldUtils.when(() -> FieldUtils.isJsonField(relationField))
                      .thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");

            mapperImports.when(() -> MapperImports.computeMapperImports(
                    packagePath, model, pkgCfg, false, true
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, false, true, pkgCfg
            );

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("UserGraphQLMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals("UserTO", ctx.get(TemplateContextConstants.TRANSFER_OBJECT_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.SWAGGER));
            assertEquals(expectedImports, ctx.get("projectImports"));

            final Object params = ctx.get(TemplateContextConstants.PARAMETERS);
            assertNotNull(params);
            assertEquals("AddressGraphQLMapper.class", params);
        }
    }

    @Test
    void computeMapperContext_shouldComputeRestMapperParametersWhenRestAndRelations() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition relation1 = mock(FieldDefinition.class);
        when(relation1.getType()).thenReturn("AddressEntity");
        final FieldDefinition relation2 = mock(FieldDefinition.class);
        when(relation2.getType()).thenReturn("OrderEntity");

        final List<FieldDefinition> fields = List.of(relation1, relation2);
        final ModelDefinition model = newModel("UserEntity", fields);

        final String packagePath = "com.example.app";
        final String expectedImports = "IMPORTS_REST_REL";

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields))
                      .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields))
                      .thenReturn(List.of(relation1, relation2));

            fieldUtils.when(() -> FieldUtils.isJsonField(relation1)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(relation2)).thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                     .thenReturn("Order");

            mapperImports.when(() -> MapperImports.computeMapperImports(
                    packagePath, model, pkgCfg, false, false
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, false, false, pkgCfg
            );

            assertEquals("UserRestMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals(expectedImports, ctx.get("projectImports"));

            final Object paramsObj = ctx.get(TemplateContextConstants.PARAMETERS);
            assertNotNull(paramsObj);
            final String params = paramsObj.toString();

            assertTrue(params.contains("AddressRestMapper.class"));
            assertTrue(params.contains("OrderRestMapper.class"));
            assertEquals("AddressRestMapper.class, OrderRestMapper.class", params);
        }
    }

    @Test
    void computeHelperMapperContext_shouldBuildRestHelperMapperContextWithoutSwagger() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final ModelDefinition parent = newModel("UserEntity", List.of());
        final ModelDefinition jsonModel = newModel("AddressEntity", List.of());

        final String packagePath = "com.example.app";
        final String expectedImports = "HELPER_IMPORTS_REST";

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("AddressEntity"))
                     .thenReturn("Address");

            mapperImports.when(() -> MapperImports.computeHelperMapperImports(
                    packagePath, jsonModel, parent, pkgCfg, false, false
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeHelperMapperContext(
                    parent, jsonModel, packagePath, false, false, pkgCfg
            );

            assertEquals("AddressEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("AddressRestMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals("AddressTO", ctx.get(TemplateContextConstants.TRANSFER_OBJECT_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.SWAGGER));
            assertEquals("Address", ctx.get(TemplateContextConstants.SWAGGER_MODEL));
            assertEquals(false, ctx.get(TemplateContextConstants.GENERATE_ALL_HELPER_METHODS));
            assertEquals(expectedImports, ctx.get("projectImports"));
        }
    }

    @Test
    void computeHelperMapperContext_shouldBuildGraphQlHelperMapperContextWithSwagger() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final ModelDefinition parent = newModel("UserEntity", List.of());
        final ModelDefinition jsonModel = newModel("ProfileEntity", List.of());

        final String packagePath = "com.example.app";
        final String expectedImports = "HELPER_IMPORTS_GRAPHQL";

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProfileEntity"))
                     .thenReturn("Profile");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("ProfileEntity"))
                     .thenReturn("Profile");

            mapperImports.when(() -> MapperImports.computeHelperMapperImports(
                    packagePath, jsonModel, parent, pkgCfg, true, true
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeHelperMapperContext(
                    parent, jsonModel, packagePath, true, true, pkgCfg
            );

            assertEquals("ProfileEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("ProfileGraphQLMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals("ProfileTO", ctx.get(TemplateContextConstants.TRANSFER_OBJECT_NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.SWAGGER));
            assertEquals("Profile", ctx.get(TemplateContextConstants.SWAGGER_MODEL));
            assertEquals(true, ctx.get(TemplateContextConstants.GENERATE_ALL_HELPER_METHODS));
            assertEquals(expectedImports, ctx.get("projectImports"));
        }
    }
}
