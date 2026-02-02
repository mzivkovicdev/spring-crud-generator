package dev.markozivkovic.springcrudgenerator.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.MapperImports;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;

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

        final List<String> lazyFields = List.of();
        final List<String> eagerFields = List.of();
        final List<String> baseCollectionFields = List.of();

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);
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
            assertEquals(lazyFields, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eagerFields, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollectionFields, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));
            assertFalse(ctx.containsKey(TemplateContextConstants.SWAGGER_MODEL));
            assertFalse(ctx.containsKey(TemplateContextConstants.PARAMETERS));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_ENABLED));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_TYPE));
            assertEquals(expectedImports, ctx.get(TemplateContextConstants.PROJECT_IMPORTS));
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

        final List<String> lazyFields = List.of("users");
        final List<String> eagerFields = List.of("profile");
        final List<String> baseCollectionFields = List.of("tags");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity")).thenReturn("Order");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("OrderEntity")).thenReturn("OrderPayload");
            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);
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
            assertEquals("OrderPayload", ctx.get(TemplateContextConstants.SWAGGER_MODEL));
            assertEquals(lazyFields, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eagerFields, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollectionFields, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));
            assertEquals(expectedImports, ctx.get(TemplateContextConstants.PROJECT_IMPORTS));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_ENABLED));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_TYPE));
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

        final List<String> lazyFields = List.of();
        final List<String> eagerFields = List.of();
        final List<String> baseCollectionFields = List.of();

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("Address");
            fieldUtils.when(() -> FieldUtils.isJsonField(relationField)).thenReturn(false);

            mapperImports.when(() -> MapperImports.computeMapperImports(
                    packagePath, model, pkgCfg, false, true
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, false, true, pkgCfg
            );

            assertEquals("UserGraphQLMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals("UserTO", ctx.get(TemplateContextConstants.TRANSFER_OBJECT_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.SWAGGER));
            assertEquals(lazyFields, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eagerFields, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollectionFields, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));
            assertEquals(expectedImports, ctx.get(TemplateContextConstants.PROJECT_IMPORTS));
            assertEquals("AddressGraphQLMapper.class", ctx.get(TemplateContextConstants.PARAMETERS));
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

        final List<String> lazyFields = List.of();
        final List<String> eagerFields = List.of();
        final List<String> baseCollectionFields = List.of();

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity")).thenReturn("Order");
            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of(relation1, relation2));
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);
            fieldUtils.when(() -> FieldUtils.isJsonField(relation1)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(relation2)).thenReturn(false);
            mapperImports.when(() -> MapperImports.computeMapperImports(
                    packagePath, model, pkgCfg, false, false
            )).thenReturn(expectedImports);

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, false, false, pkgCfg
            );

            assertEquals("UserRestMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertEquals(expectedImports, ctx.get(TemplateContextConstants.PROJECT_IMPORTS));

            assertEquals(lazyFields, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eagerFields, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollectionFields, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));

            assertEquals("AddressRestMapper.class, OrderRestMapper.class", String.valueOf(ctx.get(TemplateContextConstants.PARAMETERS)));
        }
    }

    @Test
    void computeMapperContext_shouldSetAuditFieldsOnlyForSwaggerRestWhenAuditEnabled() {

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);
        final ModelDefinition model = newModel("ProductEntity", fields);

        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.getEnabled()).thenReturn(true);
        when(audit.getType()).thenReturn(AuditTypeEnum.INSTANT);
        when(model.getAudit()).thenReturn(audit);

        final String packagePath = "com.example.app";

        final List<String> lazyFields = List.of();
        final List<String> eagerFields = List.of();
        final List<String> baseCollectionFields = List.of();

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity")).thenReturn("Product");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("ProductEntity")).thenReturn("ProductPayload");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);

            mapperImports.when(() -> MapperImports.computeMapperImports(packagePath, model, pkgCfg, true, false))
                    .thenReturn("IMPORTS");

            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT)).thenReturn("Instant");

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, true, false, pkgCfg
            );

            assertEquals(true, ctx.get(TemplateContextConstants.SWAGGER));
            assertEquals("ProductPayload", ctx.get(TemplateContextConstants.SWAGGER_MODEL));
            assertEquals(true, ctx.get(TemplateContextConstants.AUDIT_ENABLED));
            assertEquals("Instant", ctx.get(TemplateContextConstants.AUDIT_TYPE));

            assertEquals(lazyFields, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eagerFields, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollectionFields, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));
        }
    }

    @Test
    void computeMapperContext_shouldNotSetAuditFieldsForGraphQlEvenWhenSwaggerTrue() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);
        final ModelDefinition model = newModel("ProductEntity", fields);

        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.getEnabled()).thenReturn(true);
        when(model.getAudit()).thenReturn(audit);

        final String packagePath = "com.example.app";

        final List<String> lazyFields = List.of();
        final List<String> eagerFields = List.of();
        final List<String> baseCollectionFields = List.of();

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity")).thenReturn("Product");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("ProductEntity")).thenReturn("ProductPayload");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);

            mapperImports.when(() -> MapperImports.computeMapperImports(packagePath, model, pkgCfg, true, true))
                    .thenReturn("IMPORTS");

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, true, true, pkgCfg
            );

            assertEquals("ProductGraphQLMapper", ctx.get(TemplateContextConstants.MAPPER_NAME));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_ENABLED));
            assertFalse(ctx.containsKey(TemplateContextConstants.AUDIT_TYPE));
            auditUtils.verifyNoInteractions();
            assertEquals(lazyFields, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eagerFields, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollectionFields, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));
        }
    }

    @Test
    void computeMapperContext_shouldDistinctMapperParametersAcrossRelationsAndJson() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getType()).thenReturn("AddressEntity");

        final FieldDefinition jsonField = mock(FieldDefinition.class);

        final List<FieldDefinition> fields = List.of(relationField, jsonField);
        final ModelDefinition model = newModel("UserEntity", fields);
        final String packagePath = "com.example.app";

        final List<String> lazyFields = List.of();
        final List<String> eagerFields = List.of();
        final List<String> baseCollectionFields = List.of();

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);
            fieldUtils.when(() -> FieldUtils.isJsonField(relationField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("Address");

            mapperImports.when(() -> MapperImports.computeMapperImports(packagePath, model, pkgCfg, false, false))
                    .thenReturn("IMPORTS");

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, false, false, pkgCfg
            );

            assertEquals("AddressRestMapper.class", ctx.get(TemplateContextConstants.PARAMETERS));
        }
    }

    @Test
    void computeMapperContext_shouldAlwaysSetLazyEagerAndBaseCollectionFields() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(f1);
        final ModelDefinition model = newModel("UserEntity", fields);

        final String packagePath = "com.example.app";
        final List<String> lazy = List.of("tags", "orders");
        final List<String> eager = List.of("profile");
        final List<String> baseCollections = List.of("tags");

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazy);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eager);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollections);

            mapperImports.when(() -> MapperImports.computeMapperImports(packagePath, model, pkgCfg, false, false))
                    .thenReturn("IMPORTS");

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, false, false, pkgCfg
            );

            assertEquals(lazy, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eager, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollections, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));
        }
    }

    @Test
    void computeMapperContext_swaggerTrue_withRelations_shouldSetSwaggerModelAndParameters() {
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition relation = mock(FieldDefinition.class);
        when(relation.getType()).thenReturn("AddressEntity");

        final List<FieldDefinition> fields = List.of(relation);
        final ModelDefinition model = newModel("UserEntity", fields);

        final String packagePath = "com.example.app";

        final List<String> lazyFields = List.of();
        final List<String> eagerFields = List.of();
        final List<String> baseCollectionFields = List.of();

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("UserEntity")).thenReturn("UserPayload");
            fieldUtils.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of(relation));
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyFields);
            fieldUtils.when(() -> FieldUtils.extractEagerFetchFieldNames(fields)).thenReturn(eagerFields);
            fieldUtils.when(() -> FieldUtils.extractBaseCollectionFieldNames(fields)).thenReturn(baseCollectionFields);
            fieldUtils.when(() -> FieldUtils.isJsonField(relation)).thenReturn(false);
            mapperImports.when(() -> MapperImports.computeMapperImports(packagePath, model, pkgCfg, true, false))
                    .thenReturn("IMPORTS");

            final Map<String, Object> ctx = MapperTemplateContexts.computeMapperContext(
                    model, packagePath, true, false, pkgCfg
            );

            assertEquals(true, ctx.get(TemplateContextConstants.SWAGGER));
            assertEquals("UserPayload", ctx.get(TemplateContextConstants.SWAGGER_MODEL));
            assertEquals("AddressRestMapper.class", ctx.get(TemplateContextConstants.PARAMETERS));
            assertEquals(lazyFields, ctx.get(TemplateContextConstants.LAZY_FIELDS));
            assertEquals(eagerFields, ctx.get(TemplateContextConstants.EAGER_FIELDS));
            assertEquals(baseCollectionFields, ctx.get(TemplateContextConstants.BASE_COLLECTION_FIELDS));
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
