package dev.markozivkovic.springcrudgenerator.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.ResolverImports;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.templates.common.ValidationContextBuilder;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.ColumnDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ErrorResponse;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

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

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");

            fieldUtils.when(() -> FieldUtils.cloneFieldDefinition(relationField)).thenReturn(clonedRelationField);
            when(clonedRelationField.setType("Address")).thenReturn(clonedRelationField);

            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of(clonedRelationField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(clonedRelationField)).thenReturn("AddressEntity");

            final Map<String, Object> ctx = GraphQlTemplateContext.computeGraphQlSchemaContext(mainModel, allEntities);

            assertEquals("User", ctx.get(TemplateContextConstants.NAME));

            @SuppressWarnings("unchecked")
            final List<FieldDefinition> ctxFields = (List<FieldDefinition>) ctx.get(TemplateContextConstants.FIELDS);

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
    void computeGraphQlSchemaContext_shouldNotCloneFieldsWhenNoRelations() {

        final FieldDefinition normalField1 = mock(FieldDefinition.class);
        when(normalField1.getName()).thenReturn("name");
        when(normalField1.getType()).thenReturn("String");
        when(normalField1.getRelation()).thenReturn(null);

        final FieldDefinition normalField2 = mock(FieldDefinition.class);
        when(normalField2.getName()).thenReturn("age");
        when(normalField2.getType()).thenReturn("Int");
        when(normalField2.getRelation()).thenReturn(null);

        final List<FieldDefinition> fields = List.of(normalField1, normalField2);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final List<ModelDefinition> entities = List.of(newModel("SomethingEntity", List.of()));

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
            final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());

            final Map<String, Object> ctx = GraphQlTemplateContext.computeGraphQlSchemaContext(mainModel, entities);

            assertEquals("User", ctx.get(TemplateContextConstants.NAME));

            @SuppressWarnings("unchecked")
            final List<FieldDefinition> ctxFields = (List<FieldDefinition>) ctx.get(TemplateContextConstants.FIELDS);

            assertEquals(2, ctxFields.size());
            assertSame(normalField1, ctxFields.get(0));
            assertSame(normalField2, ctxFields.get(1));

            @SuppressWarnings("unchecked")
            final List<ModelDefinition> jsonModels = (List<ModelDefinition>) ctx.get(TemplateContextConstants.JSON_MODELS);

            assertTrue(jsonModels.isEmpty());
            fieldUtils.verify(() -> FieldUtils.cloneFieldDefinition(any()), never());
            nameUtils.verify(() -> ModelNameUtils.stripSuffix("String"), never());
            nameUtils.verify(() -> ModelNameUtils.stripSuffix("Int"), never());
        }
    }

    @Test
    void computeGraphQlSchemaContext_shouldAddAuditFieldsWhenAuditEnabled() {

        final FieldDefinition normalField = mock(FieldDefinition.class);
        when(normalField.getName()).thenReturn("name");
        when(normalField.getType()).thenReturn("String");
        when(normalField.getRelation()).thenReturn(null);

        final List<FieldDefinition> fields = List.of(normalField);
        final ModelDefinition mainModel = newModel("UserEntity", fields);

        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.getEnabled()).thenReturn(true);
        when(audit.getType()).thenReturn(AuditTypeEnum.INSTANT);

        when(mainModel.getAudit()).thenReturn(audit);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
            final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT)).thenReturn("Instant");

            final Map<String, Object> ctx = GraphQlTemplateContext.computeGraphQlSchemaContext(mainModel, List.of());

            assertEquals("User", ctx.get(TemplateContextConstants.NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.AUDIT_ENABLED));
            assertEquals("Instant", ctx.get(TemplateContextConstants.AUDIT_TYPE));
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

    @Test
    void computeMutationUnitTestContext_shouldPutFieldsWithLength_whenLengthExists() {

        final ModelDefinition modelDefinition = mock(ModelDefinition.class);
        when(modelDefinition.getName()).thenReturn("ProductEntity");

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getResolvedType()).thenReturn("Long");

        final ColumnDefinition column = new ColumnDefinition();
        column.setLength(12);

        final FieldDefinition nameField = mock(FieldDefinition.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getColumn()).thenReturn(column);
        when(nameField.getValidation()).thenReturn(null);
        when(nameField.getResolvedType()).thenReturn("String");

        when(modelDefinition.getFields()).thenReturn(List.of(idField, nameField));

        final CrudConfiguration configuration = mock(CrudConfiguration.class);
        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(configuration.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.INSTANCIO);
        when(configuration.getErrorResponse()).thenReturn(ErrorResponse.NONE);
        when(configuration.getSpringBootVersion()).thenReturn("3.3.0");

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);

        final TestDataGeneratorConfig generatorConfig = mock(TestDataGeneratorConfig.class);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<SpringBootVersionUtils> sbvu = mockStatic(SpringBootVersionUtils.class);
             final MockedStatic<ValidationContextBuilder> vcb = mockStatic(ValidationContextBuilder.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity")).thenReturn("Product");

            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(any())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(anyList())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(anyList())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.generateFieldNamesForCreateInputTO(anyList())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNames(anyList())).thenReturn(List.of("name"));

            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.INSTANCIO)).thenReturn(generatorConfig);
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(configuration)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.computeInvalidIdType(idField)).thenReturn("999");

            resolverImports.when(() -> ResolverImports.computeMutationResolverTestImports(true, false, "3.3.0"))
                    .thenReturn("import a;");
            resolverImports.when(() -> ResolverImports.computeProjectImportsForMutationUnitTests(
                    anyString(), eq(modelDefinition), eq(packageConfiguration), anyBoolean()
            )).thenReturn("import x;");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig))
                    .thenReturn(Map.of(
                            TemplateContextConstants.DATA_GENERATOR_FIELD_NAME, "gen",
                            TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ, "one",
                            "dataGenerator", "INSTANCIO"
                    ));
            sbvu.when(() -> SpringBootVersionUtils.isSpringBoot3("3.3.0")).thenReturn(true);
            vcb.when(() -> ValidationContextBuilder.contribute(eq(modelDefinition), anyMap(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);

                        ctx.put(TemplateContextConstants.VALIDATION_OVERRIDES, List.of(
                                Map.of(
                                        TemplateContextConstants.FIELD, "name",
                                        TemplateContextConstants.VALID_VALUE, "generateString(1)",
                                        TemplateContextConstants.INVALID_VALUE, "generateString(13)"
                                )
                        ));
                        ctx.put(TemplateContextConstants.HAS_GENERATE_STRING, true);
                        ctx.put(TemplateContextConstants.HAS_GENERATE_LIST, false);
                        return null;
                    });

            final Map<String, Object> ctx = GraphQlTemplateContext.computeMutationUnitTestContext(
                    modelDefinition, configuration, packageConfiguration, List.of(), "out", "testOut"
            );

            assertEquals("Product", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals("ProductResolver", ctx.get("resolverClassName"));
            assertEquals("ProductResolverMutationTest", ctx.get(TemplateContextConstants.CLASS_NAME));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> validationOverrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

            assertEquals(1, validationOverrides.size());
            assertEquals("name", validationOverrides.get(0).get(TemplateContextConstants.FIELD));
            assertEquals("generateString(1)", validationOverrides.get(0).get(TemplateContextConstants.VALID_VALUE));
            assertEquals("generateString(13)", validationOverrides.get(0).get(TemplateContextConstants.INVALID_VALUE));
            assertTrue(ctx.containsKey(TemplateContextConstants.VALIDATION_OVERRIDES));
            assertEquals(true, ctx.get(TemplateContextConstants.HAS_GENERATE_STRING));
            assertEquals(false, ctx.get(TemplateContextConstants.HAS_GENERATE_LIST));
            assertEquals(true, ctx.get(TemplateContextConstants.IS_SPRING_BOOT_3));
        }
    }

    @Test
    void computeMutationUnitTestContext_shouldNotPutFieldsWithLength_whenNoLengthExists() {

        final ModelDefinition modelDefinition = mock(ModelDefinition.class);
        when(modelDefinition.getName()).thenReturn("ProductEntity");

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getResolvedType()).thenReturn("Long");

        final FieldDefinition nameField = mock(FieldDefinition.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getColumn()).thenReturn(null);
        when(nameField.getValidation()).thenReturn(null);
        when(nameField.getResolvedType()).thenReturn("String");
        when(modelDefinition.getFields()).thenReturn(List.of(idField, nameField));

        final CrudConfiguration configuration = mock(CrudConfiguration.class);
        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(configuration.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.INSTANCIO);
        when(configuration.getErrorResponse()).thenReturn(ErrorResponse.NONE);
        when(configuration.getSpringBootVersion()).thenReturn("3.3.0");

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final TestDataGeneratorConfig generatorConfig = mock(TestDataGeneratorConfig.class);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<SpringBootVersionUtils> sbvu = mockStatic(SpringBootVersionUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity")).thenReturn("Product");

            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(any())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(anyList())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(anyList())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.generateFieldNamesForCreateInputTO(anyList())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNames(anyList())).thenReturn(List.of("name"));

            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.INSTANCIO)).thenReturn(generatorConfig);
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(configuration)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.computeInvalidIdType(idField)).thenReturn("999");

            resolverImports.when(() -> ResolverImports.computeMutationResolverTestImports(true, false, "3.3.0"))
                    .thenReturn("import a;");
            resolverImports.when(() -> ResolverImports.computeProjectImportsForMutationUnitTests(
                    anyString(), eq(modelDefinition), eq(packageConfiguration), anyBoolean()
            )).thenReturn("import x;");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig))
                    .thenReturn(Map.of(
                            TemplateContextConstants.DATA_GENERATOR_FIELD_NAME, "gen",
                            TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ, "one",
                            "dataGenerator", "INSTANCIO"
                    ));
            sbvu.when(() -> SpringBootVersionUtils.isSpringBoot3("3.3.0")).thenReturn(true);

            final Map<String, Object> ctx = GraphQlTemplateContext.computeMutationUnitTestContext(
                    modelDefinition, configuration, packageConfiguration, List.of(), "out", "testOut"
            );

            assertFalse(ctx.containsKey(TemplateContextConstants.FIELDS_WITH_LENGTH));
            assertFalse(ctx.containsKey(TemplateContextConstants.VALIDATION_OVERRIDES));
            assertFalse(ctx.containsKey(TemplateContextConstants.HAS_GENERATE_STRING));
            assertFalse(ctx.containsKey(TemplateContextConstants.HAS_GENERATE_LIST));
        }
    }

    @Test
    void computeMutationUnitTestContext_shouldBuildRelationsContext_withCollectionFlagAndInvalidIdType() {

        final RelationDefinition relDef = mock(RelationDefinition.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getResolvedType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("products");
        when(relationField.getType()).thenReturn("ProductEntity");
        when(relationField.getRelation()).thenReturn(relDef);
        when(relationField.getColumn()).thenReturn(null);
        when(relationField.getValidation()).thenReturn(null);

        final ModelDefinition modelDefinition = mock(ModelDefinition.class);
        when(modelDefinition.getName()).thenReturn("CategoryEntity");
        when(modelDefinition.getFields()).thenReturn(List.of(idField, relationField));

        final FieldDefinition relIdField = mock(FieldDefinition.class);
        when(relIdField.getName()).thenReturn("id");
        when(relIdField.getType()).thenReturn("Long");
        when(relIdField.getResolvedType()).thenReturn("Long");

        final ModelDefinition relationModel = mock(ModelDefinition.class);
        when(relationModel.getName()).thenReturn("ProductEntity");
        when(relationModel.getFields()).thenReturn(List.of(relIdField));

        final CrudConfiguration configuration = mock(CrudConfiguration.class);
        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(configuration.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.INSTANCIO);
        when(configuration.getErrorResponse()).thenReturn(ErrorResponse.NONE);
        when(configuration.getSpringBootVersion()).thenReturn("3.3.0");

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final TestDataGeneratorConfig generatorConfig = mock(TestDataGeneratorConfig.class);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<SpringBootVersionUtils> sbvu = mockStatic(SpringBootVersionUtils.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("CategoryEntity")).thenReturn("Category");

            fieldUtils.when(() -> FieldUtils.extractIdField(modelDefinition.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(modelDefinition.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(modelDefinition)).thenReturn(List.of("products"));

            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(anyList())).thenReturn(List.of("products"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.generateFieldNamesForCreateInputTO(anyList())).thenReturn(List.of("productsIds"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNames(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractIdField(relationModel.getFields())).thenReturn(relIdField);

            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.INSTANCIO)).thenReturn(generatorConfig);
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(configuration)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.computeInvalidIdType(idField)).thenReturn("999");
            unitTestUtils.when(() -> UnitTestUtils.computeInvalidIdType(relIdField)).thenReturn("888");

            resolverImports.when(() -> ResolverImports.computeMutationResolverTestImports(true, false, "3.3.0"))
                    .thenReturn("import a;");
            resolverImports.when(() -> ResolverImports.computeProjectImportsForMutationUnitTests(
                    anyString(), eq(modelDefinition), eq(packageConfiguration), anyBoolean()
            )).thenReturn("import x;");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig))
                    .thenReturn(Map.of(
                            TemplateContextConstants.DATA_GENERATOR_FIELD_NAME, "gen",
                            TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ, "one",
                            "dataGenerator", "INSTANCIO"
                    ));
            sbvu.when(() -> SpringBootVersionUtils.isSpringBoot3("3.3.0")).thenReturn(true);

            final Map<String, Object> ctx = GraphQlTemplateContext.computeMutationUnitTestContext(
                    modelDefinition, configuration, packageConfiguration, List.of(relationModel), "out", "testOut"
            );

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relations = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.RELATIONS);

            assertNotNull(relations);
            assertEquals(1, relations.size());

            final Map<String, Object> relCtx = relations.get(0);
            assertEquals("products", relCtx.get(TemplateContextConstants.RELATION_FIELD));
            assertEquals("Long", relCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
            assertEquals(true, relCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals("888", relCtx.get("invalidRelationIdType"));
        }
    }

}
