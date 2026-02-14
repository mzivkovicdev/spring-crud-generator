package dev.markozivkovic.springcrudgenerator.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.RestControllerImports;
import dev.markozivkovic.springcrudgenerator.models.ColumnDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.templates.common.ValidationContextBuilder;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.ContainerUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

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
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField))
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

            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity")).thenReturn("Product");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("String")).thenReturn("");
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.isJsonField(nameField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFieldEnum(nameField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());

            final Map<String, Object> ctx = RestControllerTemplateContext.computeCreateEndpointContext(model, allEntities);

            assertEquals("ProductEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Product", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> inputFields = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

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
    void computeCreateEndpointContext_shouldHandleRelationField_withCollectionFlag_andRelationId() {

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

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields)).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.isJsonField(relationField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerType(relationField)).thenReturn("List<AddressEntity>");
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(relationField)).thenReturn("AddressEntity");
            fieldUtils.when(() -> FieldUtils.isFieldEnum(relationField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractIdField(addressModel.getFields())).thenReturn(addressIdField);

            final Map<String, Object> ctx = RestControllerTemplateContext.computeCreateEndpointContext(mainModel, allEntities);

            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("User", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> inputFields = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertEquals(1, inputFields.size());
            final Map<String, Object> fCtx = inputFields.get(0);

            assertEquals("addresses", fCtx.get(TemplateContextConstants.FIELD));
            assertEquals("AddressEntity", fCtx.get(TemplateContextConstants.FIELD_TYPE));
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
    void computeCreateEndpointContext_shouldHandleJsonNonCollectionFieldType_fromInnerType_new() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        when(jsonField.getName()).thenReturn("metadata");
        when(jsonField.getType()).thenReturn("JSON<Metadata>");
        when(jsonField.getResolvedType()).thenReturn("Metadata");
        when(jsonField.getRelation()).thenReturn(null);

        final List<FieldDefinition> fields = List.of(idField, jsonField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn(""); // not used here
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn(""); // safe
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("JSON<Metadata>");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("JSON<Metadata>");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<Metadata>")).thenReturn("JSON<Metadata>");
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerType(jsonField)).thenReturn("Metadata");
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Metadata");
            fieldUtils.when(() -> FieldUtils.isFieldEnum(jsonField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());

            final Map<String, Object> ctx = RestControllerTemplateContext.computeCreateEndpointContext(model, List.of(model));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> inputFields = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertEquals(1, inputFields.size());
            final Map<String, Object> fCtx = inputFields.get(0);

            assertEquals("metadata", fCtx.get(TemplateContextConstants.FIELD));
            assertEquals("Metadata", fCtx.get(TemplateContextConstants.FIELD_TYPE));
            assertEquals(false, fCtx.get(TemplateContextConstants.IS_RELATION));
            assertEquals(false, fCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals(true, fCtx.get(TemplateContextConstants.IS_JSON_FIELD));
        }
    }

    @Test
    void computeCreateEndpointContext_shouldHandleJsonCollectionFieldType_asElementType_new() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        when(jsonField.getName()).thenReturn("metadata");
        when(jsonField.getType()).thenReturn("JSON<List<Metadata>>");
        when(jsonField.getResolvedType()).thenReturn("java.util.List<Metadata>");
        when(jsonField.getRelation()).thenReturn(null);

        final List<FieldDefinition> fields = List.of(idField, jsonField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JSON<List<Metadata>>")).thenReturn("JSON<List<Metadata>>");
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(fields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerType(jsonField)).thenReturn("List<Metadata>");
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Metadata");
            fieldUtils.when(() -> FieldUtils.isFieldEnum(jsonField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());

            final Map<String, Object> ctx = RestControllerTemplateContext.computeCreateEndpointContext(model, List.of(model));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> inputFields = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertEquals(1, inputFields.size());
            final Map<String, Object> fCtx = inputFields.get(0);

            assertEquals("Metadata", fCtx.get(TemplateContextConstants.FIELD_TYPE));
            assertEquals(true, fCtx.get(TemplateContextConstants.IS_JSON_FIELD));
            assertEquals(false, fCtx.get(TemplateContextConstants.IS_COLLECTION));
        }
    }

    @Test
    void computeCreateTestEndpointContext_addsInputFieldsAndFieldsWithLength_andRelationIdInfo() {
        
        final ModelDefinition category = mock(ModelDefinition.class);
        when(category.getName()).thenReturn("Category");

        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition nameField = mock(FieldDefinition.class);
        final FieldDefinition productsField = mock(FieldDefinition.class);

        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getResolvedType()).thenReturn("Long");
        when(idField.getRelation()).thenReturn(null);

        when(nameField.getName()).thenReturn("name");
        when(nameField.getType()).thenReturn("String");
        when(nameField.getResolvedType()).thenReturn("String");

        final ColumnDefinition nameCol = mock(ColumnDefinition.class);
        when(nameCol.getLength()).thenReturn(3);
        when(nameField.getColumn()).thenReturn(nameCol);
        when(productsField.getName()).thenReturn("products");
        when(productsField.getType()).thenReturn("Product");
        when(productsField.getResolvedType()).thenReturn("List<Product>");

        final RelationDefinition rel = mock(RelationDefinition.class);
        when(productsField.getRelation()).thenReturn(rel);

        final List<FieldDefinition> categoryFields = List.of(idField, nameField, productsField);
        when(category.getFields()).thenReturn(categoryFields);

        final ModelDefinition productEntity = mock(ModelDefinition.class);
        when(productEntity.getName()).thenReturn("Product");

        final FieldDefinition productIdField = mock(FieldDefinition.class);
        when(productIdField.getName()).thenReturn("id");
        when(productIdField.getType()).thenReturn("Long");

        when(productEntity.getFields()).thenReturn(List.of(productIdField));

        final List<ModelDefinition> entities = List.of(productEntity);

        try (final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ContainerUtils> containerUtils = mockStatic(ContainerUtils.class)) {

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix(anyString())).thenAnswer(inv -> inv.getArgument(0));
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(categoryFields)).thenReturn(List.of(productsField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(categoryFields)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(categoryFields)).thenReturn(List.of(productsField));
            fieldUtils.when(() -> FieldUtils.isJsonField(any())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFieldEnum(any())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(productEntity.getFields())).thenReturn(productIdField);

            containerUtils.when(() -> ContainerUtils.isEmpty(any(Collection.class))).thenAnswer(inv -> {
                Object arg = inv.getArgument(0);
                if (arg instanceof Collection<?> c) return c.isEmpty();
                return arg == null;
            });

            final Map<String, Object> ctx = RestControllerTemplateContext.computeCreateTestEndpointContext(category, entities);

            assertEquals("Category", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Category", ctx.get(TemplateContextConstants.STRIPPED_MODEL_NAME));
            assertEquals(true, ctx.get(TemplateContextConstants.RELATIONS));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> inputFields = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.INPUT_FIELDS);

            assertNotNull(inputFields);
            assertEquals(2, inputFields.size(), "Id field ne treba da bude u inputFields");

            final Map<String, Object> nameCtx = inputFields.stream()
                .filter(m -> "name".equals(m.get(TemplateContextConstants.FIELD)))
                .findFirst()
                .orElseThrow();
            assertEquals(false, nameCtx.get(TemplateContextConstants.IS_RELATION));
            assertEquals(false, nameCtx.get(TemplateContextConstants.IS_COLLECTION));

            final Map<String, Object> productsCtx = inputFields.stream()
                .filter(m -> "products".equals(m.get(TemplateContextConstants.FIELD)))
                .findFirst()
                .orElseThrow();
            assertEquals(true, productsCtx.get(TemplateContextConstants.IS_RELATION));
            assertEquals(true, productsCtx.get(TemplateContextConstants.IS_COLLECTION));
            assertEquals("Long", productsCtx.get(TemplateContextConstants.RELATION_ID_TYPE));
            assertEquals("id", productsCtx.get(TemplateContextConstants.RELATION_ID_FIELD));
        }
    }

    @Test
    void computeUpdateByIdTestEndpointContext_populatesExpectedKeys_andValidationOverridesFromColumnLength() {

        final ModelDefinition product = mock(ModelDefinition.class);
        when(product.getName()).thenReturn("Product");

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getResolvedType()).thenReturn("Long");
        when(idField.getValidation()).thenReturn(null);
        when(idField.getColumn()).thenReturn(null);

        final FieldDefinition nameField = mock(FieldDefinition.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getType()).thenReturn("String");
        when(nameField.getResolvedType()).thenReturn("String");
        when(nameField.getValidation()).thenReturn(null);

        final ColumnDefinition nameCol = mock(ColumnDefinition.class);
        when(nameCol.getLength()).thenReturn(20);
        when(nameField.getColumn()).thenReturn(nameCol);

        final List<FieldDefinition> fields = List.of(idField, nameField);
        when(product.getFields()).thenReturn(fields);

        final CrudConfiguration crudCfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(crudCfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        when(crudCfg.getSpringBootVersion()).thenReturn("4.0.1.RELEASE");

        try (final MockedStatic<AdditionalPropertiesUtils> apu = mockStatic(AdditionalPropertiesUtils.class);
            final MockedStatic<UnitTestUtils> utu = mockStatic(UnitTestUtils.class);
            final MockedStatic<FieldUtils> fu = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> mnu = mockStatic(ModelNameUtils.class);
            final MockedStatic<RestControllerImports> rci = mockStatic(RestControllerImports.class);
            final MockedStatic<DataGeneratorTemplateContext> dgtc = mockStatic(DataGeneratorTemplateContext.class);
            final MockedStatic<ContainerUtils> cu = mockStatic(ContainerUtils.class);
            final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class);
            final MockedStatic<ValidationContextBuilder> vcb = mockStatic(ValidationContextBuilder.class)) {

            apu.when(() -> AdditionalPropertiesUtils.resolveBasePath(crudCfg)).thenReturn("/api");

            final TestDataGeneratorConfig generatorCfg = mock(TestDataGeneratorConfig.class);
            utu.when(() -> UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.PODAM)).thenReturn(generatorCfg);

            fu.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            mnu.when(() -> ModelNameUtils.stripSuffix("Product")).thenReturn("Product");

            fu.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);
            fu.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());

            final FieldDefinition jsonField = mock(FieldDefinition.class);
            fu.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of(jsonField));
            fu.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("payload");

            utu.when(() -> UnitTestUtils.computeInvalidIdType(idField)).thenReturn("invalid-id");
            utu.when(() -> UnitTestUtils.isInstancioEnabled(crudCfg)).thenReturn(false);

            fu.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, true)).thenReturn(List.of("name"));
            fu.when(() -> FieldUtils.extractNonIdNonRelationFieldNames(fields)).thenReturn(List.of("name"));

            rci.when(() -> RestControllerImports.computeUpdateEndpointTestImports(false, "4.0.1.RELEASE"))
                    .thenReturn("import x;");

            rci.when(() -> RestControllerImports.computeUpdateEndpointTestProjectImports(
                    eq(product), anyString(), eq(true), eq(pkgCfg), eq(true)
            )).thenReturn("import y;");

            final Map<String, Object> generatorCtx = new HashMap<>();
            generatorCtx.put("dataGenerator", "PODAM");
            generatorCtx.put(TemplateContextConstants.DATA_GENERATOR_FIELD_NAME, "gen");
            generatorCtx.put(TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ, "one");
            dgtc.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(generatorCfg)).thenReturn(generatorCtx);

            cu.when(() -> ContainerUtils.isEmpty(any(Collection.class))).thenAnswer(inv -> {
                final Object arg = inv.getArgument(0);
                if (arg instanceof Collection<?> c) return c.isEmpty();
                return arg == null;
            });

            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("4.0.1.RELEASE")).thenReturn(false);

            vcb.when(() -> ValidationContextBuilder.contribute(any(), anyMap(), anyString(), anyString()))
            .thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = inv.getArgument(1, Map.class);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> overrides = (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);

                if (overrides == null) {
                    overrides = new ArrayList<>();
                    ctx.put(TemplateContextConstants.VALIDATION_OVERRIDES, overrides);
                }

                final boolean exists = overrides.stream()
                        .anyMatch(m -> "name".equals(m.get(TemplateContextConstants.FIELD)));

                if (!exists) {
                    final Map<String, Object> nameOverride = new HashMap<>();
                    nameOverride.put(TemplateContextConstants.FIELD, "name");
                    nameOverride.put(TemplateContextConstants.VALID_VALUE, "generateString(1)");
                    nameOverride.put(TemplateContextConstants.INVALID_VALUE, "generateString(21)");
                    overrides.add(nameOverride);
                }
                return null;
            });

            final Map<String, Object> ctx = RestControllerTemplateContext.computeUpdateByIdTestEndpointContext(
                    product, crudCfg, pkgCfg, true, true, "/out", "/testOut", "com/demo"
            );

            assertEquals("/api", ctx.get("basePath"));
            assertEquals("ProductController", ctx.get("controllerClassName"));
            assertEquals("ProductUpdateByIdMockMvcTest", ctx.get("className"));
            assertEquals("Product", ctx.get("strippedModelName"));
            assertEquals("Product", ctx.get("modelName"));
            assertEquals(false, ctx.get("hasRelations"));
            assertEquals(false, ctx.get("isIdUuid"));
            assertEquals("Long", ctx.get("idType"));
            assertEquals("id", ctx.get("idField"));
            assertEquals("invalid-id", ctx.get("invalidIdType"));
            assertEquals(true, ctx.get("swagger"));
            assertEquals(true, ctx.get("isGlobalExceptionHandlerEnabled"));
            assertEquals(List.of("name"), ctx.get("inputFields"));
            assertEquals(List.of("name"), ctx.get("fieldNames"));
            assertEquals("import x;", ctx.get("testImports"));
            assertEquals("import y;", ctx.get("projectImports"));

            @SuppressWarnings("unchecked")
            final List<String> jsonFields = (List<String>) ctx.get("jsonFields");
            assertEquals(List.of("payload"), jsonFields);

            assertEquals("PODAM", ctx.get("dataGenerator"));
            assertTrue(ctx.containsKey(TemplateContextConstants.VALIDATION_OVERRIDES));

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> overrides =
                    (List<Map<String, Object>>) ctx.get(TemplateContextConstants.VALIDATION_OVERRIDES);
            assertNotNull(overrides);

            final Map<String, Object> nameOverride = overrides.stream()
                    .filter(m -> "name".equals(m.get(TemplateContextConstants.FIELD)))
                    .findFirst()
                    .orElseThrow();

            assertEquals("generateString(1)", nameOverride.get(TemplateContextConstants.VALID_VALUE));
            assertEquals("generateString(21)", nameOverride.get(TemplateContextConstants.INVALID_VALUE));
        }
    }

    @Test
    void computeUpdateByIdTestEndpointContext_setsIsSpringBoot3Flag_trueWhenBoot3() {

        final ModelDefinition product = mock(ModelDefinition.class);
        when(product.getName()).thenReturn("Product");

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");
        when(idField.getType()).thenReturn("Long");
        when(idField.getResolvedType()).thenReturn("Long");
        when(idField.getValidation()).thenReturn(null);
        when(idField.getColumn()).thenReturn(null);

        final FieldDefinition nameField = mock(FieldDefinition.class);
        when(nameField.getName()).thenReturn("name");
        when(nameField.getType()).thenReturn("String");
        when(nameField.getResolvedType()).thenReturn("String");
        when(nameField.getValidation()).thenReturn(null);

        final ColumnDefinition nameCol = mock(ColumnDefinition.class);
        when(nameCol.getLength()).thenReturn(20);
        when(nameField.getColumn()).thenReturn(nameCol);

        final List<FieldDefinition> fields = List.of(idField, nameField);
        when(product.getFields()).thenReturn(fields);

        final CrudConfiguration crudCfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(crudCfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);
        when(crudCfg.getSpringBootVersion()).thenReturn("3.1.0");

        try (final MockedStatic<AdditionalPropertiesUtils> apu = mockStatic(AdditionalPropertiesUtils.class);
            final MockedStatic<UnitTestUtils> utu = mockStatic(UnitTestUtils.class);
            final MockedStatic<FieldUtils> fu = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> mnu = mockStatic(ModelNameUtils.class);
            final MockedStatic<RestControllerImports> rci = mockStatic(RestControllerImports.class);
            final MockedStatic<DataGeneratorTemplateContext> dgtc = mockStatic(DataGeneratorTemplateContext.class);
            final MockedStatic<ContainerUtils> cu = mockStatic(ContainerUtils.class);
            final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class);
            final MockedStatic<ValidationContextBuilder> vcb = mockStatic(ValidationContextBuilder.class)) {

            apu.when(() -> AdditionalPropertiesUtils.resolveBasePath(crudCfg)).thenReturn("/api");

            final TestDataGeneratorConfig generatorCfg = mock(TestDataGeneratorConfig.class);
            utu.when(() -> UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.PODAM)).thenReturn(generatorCfg);
            fu.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            mnu.when(() -> ModelNameUtils.stripSuffix("Product")).thenReturn("Product");
            fu.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);
            fu.when(() -> FieldUtils.extractRelationFields(fields)).thenReturn(List.of());
            fu.when(() -> FieldUtils.extractJsonFields(fields)).thenReturn(List.of());
            utu.when(() -> UnitTestUtils.computeInvalidIdType(idField)).thenReturn("invalid-id");
            utu.when(() -> UnitTestUtils.isInstancioEnabled(crudCfg)).thenReturn(false);
            fu.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForController(fields, true)).thenReturn(List.of("name"));
            fu.when(() -> FieldUtils.extractNonIdNonRelationFieldNames(fields)).thenReturn(List.of("name"));
            rci.when(() -> RestControllerImports.computeUpdateEndpointTestImports(false, "3.1.0"))
                    .thenReturn("import x;");
            rci.when(() -> RestControllerImports.computeUpdateEndpointTestProjectImports(
                    eq(product), anyString(), eq(true), eq(pkgCfg), eq(true)
            )).thenReturn("import y;");

            dgtc.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(generatorCfg))
                    .thenReturn(Map.of(
                            "dataGenerator", "PODAM",
                            TemplateContextConstants.DATA_GENERATOR_FIELD_NAME, "gen",
                            TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ, "one"
                    ));

            cu.when(() -> ContainerUtils.isEmpty(any(Collection.class))).thenAnswer(inv -> {
                final Object arg = inv.getArgument(0);
                if (arg instanceof Collection<?> c) return c.isEmpty();
                return arg == null;
            });

            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.1.0")).thenReturn(true);
            vcb.when(() -> ValidationContextBuilder.contribute(any(), anyMap(), anyString(), anyString())).thenAnswer(inv -> null);

            final Map<String, Object> ctx = RestControllerTemplateContext.computeUpdateByIdTestEndpointContext(
                    product, crudCfg, pkgCfg, true, true, "/out", "/testOut", "com/demo"
            );

            assertEquals(true, ctx.get(TemplateContextConstants.IS_SPRING_BOOT_3));
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
        final List<ModelDefinition> entities = List.of(model);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of());

            final Map<String, Object> ctx = RestControllerTemplateContext.computeAddResourceRelationEndpointContext(model, entities);

            assertTrue(ctx.isEmpty(), "Expected empty context when there are no relation types");
        }
    }

    @Test
    void computeAddResourceRelationEndpointContext_shouldBuildModelAndRelations() {

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        when(userIdField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> userFields = List.of(userIdField, relationField);
        final ModelDefinition userModel = newModel("UserEntity", userFields);

        final FieldDefinition roleIdField = mock(FieldDefinition.class);
        when(roleIdField.getName()).thenReturn("roleId");

        final List<FieldDefinition> roleFields = List.of(roleIdField);
        final ModelDefinition roleModel = newModel("RoleEntity", roleFields);

        final List<ModelDefinition> entities = List.of(userModel, roleModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(userFields)).thenReturn(List.of("ManyToMany"));
            fieldUtils.when(() -> FieldUtils.extractIdField(userFields)).thenReturn(userIdField);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(userFields)).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractIdField(roleFields)).thenReturn(roleIdField);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity")).thenReturn("Role");

            final Map<String, Object> ctx = RestControllerTemplateContext.computeAddResourceRelationEndpointContext(userModel, entities);

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

            assertEquals("roleId", relCtx.get(TemplateContextConstants.RELATION_ID_FIELD));
            assertEquals("roles", relCtx.get(TemplateContextConstants.RELATION_FIELD_MODEL));
            assertEquals("Role", relCtx.get(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME));
            assertEquals("usersIdRolesPost", relCtx.get(TemplateContextConstants.METHOD_NAME));
        }
    }

    @Test
    void computeAddResourceRelationEndpointContext_shouldThrowWhenRelationEntityNotFound() {

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        when(userIdField.getType()).thenReturn("Long");

        final FieldDefinition relationField = mock(FieldDefinition.class);
        when(relationField.getName()).thenReturn("roles");
        when(relationField.getType()).thenReturn("RoleEntity");

        final List<FieldDefinition> userFields = List.of(userIdField, relationField);
        final ModelDefinition userModel = newModel("UserEntity", userFields);

        final List<ModelDefinition> entities = List.of(userModel);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(userFields)).thenReturn(List.of("ManyToMany"));
            fieldUtils.when(() -> FieldUtils.extractIdField(userFields)).thenReturn(userIdField);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(userFields)).thenReturn(List.of(relationField));
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("RoleEntity")).thenReturn("Role");

            assertThrows(NoSuchElementException.class, () ->
                    RestControllerTemplateContext.computeAddResourceRelationEndpointContext(userModel, entities)
            );
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
