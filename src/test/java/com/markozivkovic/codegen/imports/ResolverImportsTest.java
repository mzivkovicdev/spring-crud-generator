package com.markozivkovic.codegen.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

class ResolverImportsTest {

    @Test
    @DisplayName("computeResolverBaseImports: id field is UUID → UUID import present")
    void computeResolverBaseImports_uuidId() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition idField = new FieldDefinition();
        model.setFields(List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(true);

            final String result = ResolverImports.computeResolverBaseImports(model);

            assertTrue(result.contains("import " + ImportConstants.Java.UUID + ";"),
                    "UUID import should be present for UUID id field");
        }
    }

    @Test
    @DisplayName("computeResolverBaseImports: id field is NOT UUID → no UUID import")
    void computeResolverBaseImports_nonUuidId() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition idField = new FieldDefinition();
        model.setFields(List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            final String result = ResolverImports.computeResolverBaseImports(model);

            assertFalse(result.contains(ImportConstants.Java.UUID),
                    "UUID import should NOT be present for non-UUID id field");
        }
    }

    @Test
    @DisplayName("computeGraphQlResolverImports: no JSON fields, no relations → basic GraphQL imports only (no helper mappers, no BusinessService)")
    void computeGraphQlResolverImports_noJson_noRelations() {
        
        final String outputDir = "/some/output/dir";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example");

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields()))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(model.getFields()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.computeEntityPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.entity");
            pkg.when(() -> PackageUtils.join("com.example.entity", "User"))
                    .thenReturn("com.example.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.example", packageConfiguration))
                    .thenReturn("com.example.service");
            pkg.when(() -> PackageUtils.join("com.example.service", "UserService"))
                    .thenReturn("com.example.service.UserService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.graphql.to");
            pkg.when(() -> PackageUtils.join("com.example.graphql.to", "UserTO"))
                    .thenReturn("com.example.graphql.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.example.graphql.to", "UserCreateTO"))
                    .thenReturn("com.example.graphql.to.UserCreateTO");
            pkg.when(() -> PackageUtils.join("com.example.graphql.to", "UserUpdateTO"))
                    .thenReturn("com.example.graphql.to.UserUpdateTO");

            pkg.when(() -> PackageUtils.computeGraphQlMapperPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.graphql.mapper");
            pkg.when(() -> PackageUtils.join("com.example.graphql.mapper", "UserGraphQLMapper"))
                    .thenReturn("com.example.graphql.mapper.UserGraphQLMapper");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.to");
            pkg.when(() -> PackageUtils.join("com.example.to", "PageTO"))
                    .thenReturn("com.example.to.PageTO");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.example", packageConfiguration))
                    .thenReturn("com.example.business");

            final String result = ResolverImports.computeGraphQlResolverImports(
                    model,
                    outputDir,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.example.entity.User;"));
            assertTrue(result.contains("import com.example.service.UserService;"));
            assertTrue(result.contains("import com.example.graphql.to.UserTO;"));
            assertTrue(result.contains("import com.example.graphql.to.UserCreateTO;"));
            assertTrue(result.contains("import com.example.graphql.to.UserUpdateTO;"));
            assertTrue(result.contains("import com.example.graphql.mapper.UserGraphQLMapper;"));
            assertTrue(result.contains("import com.example.to.PageTO;"));

            assertFalse(result.contains("GraphQLMapper;") && result.contains("Helper"),
                    "No helper GraphQL mappers expected without JSON fields");
            assertFalse(result.contains("BusinessService"),
                    "BusinessService should not be imported when there are no relations");
        }
    }

    @Test
    @DisplayName("computeGraphQlResolverImports: JSON fields present → helper GraphQL mappers are imported")
    void computeGraphQlResolverImports_withJsonHelpers() {
        
        final String outputDir = "/output/json";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition jsonField1 = new FieldDefinition();
        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(List.of(jsonField1));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields()))
                    .thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField1)).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField1))
                    .thenReturn("Shipping");
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(model.getFields()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.computeHelperGraphQlMapperPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.graphql.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.helper.mapper", "ShippingGraphQLMapper"))
                    .thenReturn("com.shop.graphql.helper.mapper.ShippingGraphQLMapper");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");

            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService"))
                    .thenReturn("com.shop.service.OrderService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.graphql.to");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderTO"))
                    .thenReturn("com.shop.graphql.to.OrderTO");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderCreateTO"))
                    .thenReturn("com.shop.graphql.to.OrderCreateTO");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderUpdateTO"))
                    .thenReturn("com.shop.graphql.to.OrderUpdateTO");

            pkg.when(() -> PackageUtils.computeGraphQlMapperPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.graphql.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.mapper", "OrderGraphQLMapper"))
                    .thenReturn("com.shop.graphql.mapper.OrderGraphQLMapper");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.to");
            pkg.when(() -> PackageUtils.join("com.shop.to", "PageTO"))
                    .thenReturn("com.shop.to.PageTO");

            final String result = ResolverImports.computeGraphQlResolverImports(
                    model,
                    outputDir,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.shop.graphql.helper.mapper.ShippingGraphQLMapper;"),
                    "Helper GraphQL mapper for JSON field missing");
            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.graphql.to.OrderTO;"));
            assertTrue(result.contains("import com.shop.graphql.mapper.OrderGraphQLMapper;"));
            assertTrue(result.contains("import com.shop.to.PageTO;"));

            assertFalse(result.contains("GraphQLMapper;") && result.contains("jsonField2"),
                    "No unexpected helper mappers for non-JSON fields");
        }
    }

    @Test
    @DisplayName("computeGraphQlResolverImports: relations present → BusinessService import added")
    void computeGraphQlResolverImports_withRelations_businessServiceImport() {
        
        final String outputDir = "/output/relations";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("Invoice");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.billing");

            names.when(() -> ModelNameUtils.stripSuffix("Invoice"))
                    .thenReturn("Invoice");

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields()))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(model.getFields()))
                    .thenReturn(List.of("MANY_TO_ONE"));

            pkg.when(() -> PackageUtils.computeEntityPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.entity");
            pkg.when(() -> PackageUtils.join("com.billing.entity", "Invoice"))
                    .thenReturn("com.billing.entity.Invoice");

            pkg.when(() -> PackageUtils.computeServicePackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.service");
            pkg.when(() -> PackageUtils.join("com.billing.service", "InvoiceService"))
                    .thenReturn("com.billing.service.InvoiceService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.graphql.to");
            pkg.when(() -> PackageUtils.join("com.billing.graphql.to", "InvoiceTO"))
                    .thenReturn("com.billing.graphql.to.InvoiceTO");
            pkg.when(() -> PackageUtils.join("com.billing.graphql.to", "InvoiceCreateTO"))
                    .thenReturn("com.billing.graphql.to.InvoiceCreateTO");
            pkg.when(() -> PackageUtils.join("com.billing.graphql.to", "InvoiceUpdateTO"))
                    .thenReturn("com.billing.graphql.to.InvoiceUpdateTO");

            pkg.when(() -> PackageUtils.computeGraphQlMapperPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.graphql.mapper");
            pkg.when(() -> PackageUtils.join("com.billing.graphql.mapper", "InvoiceGraphQLMapper"))
                    .thenReturn("com.billing.graphql.mapper.InvoiceGraphQLMapper");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.to");
            pkg.when(() -> PackageUtils.join("com.billing.to", "PageTO"))
                    .thenReturn("com.billing.to.PageTO");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.business");
            pkg.when(() -> PackageUtils.join("com.billing.business", "InvoiceBusinessService"))
                    .thenReturn("com.billing.business.InvoiceBusinessService");

            final String result = ResolverImports.computeGraphQlResolverImports(
                    model,
                    outputDir,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.billing.business.InvoiceBusinessService;"),
                    "BusinessService import should be present when relations exist");
        }
    }

    @Test
    @DisplayName("computeQueryResolverTestImports: Instancio disabled → should NOT include Instancio import")
    void computeQueryResolverTestImports_instancioDisabled() {
        
        final String result = ResolverImports.computeQueryResolverTestImports(false);

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));
        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringContext.IMPORT + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_IMPL + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringCore.PARAMETERIZED_TYPE_REFERENCE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.TEST_PROPERTY_SORUCE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.AUTO_CONFIGURE_GRAPH_QL_TESTER + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.GRAPH_QL_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.GraphQLTest.GRAPH_QL_TESTER + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
    }

    @Test
    @DisplayName("computeQueryResolverTestImports: Instancio enabled → should include Instancio import")
    void computeQueryResolverTestImports_instancioEnabled() {
        
        final String result = ResolverImports.computeQueryResolverTestImports(true);

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO + ";"));
    }

    @Test
    @DisplayName("computeMutationResolverTestImports: No Instancio, No JSON → minimal import set")
    void computeMutationResolverTestImports_noInstancio_noJson() {
        
        final String result = ResolverImports.computeMutationResolverTestImports(false, false);

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));
        assertFalse(result.contains(ImportConstants.MapStruct.FACTORY_MAPPERS));

        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringContext.IMPORT + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.TEST_PROPERTY_SORUCE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.AUTO_CONFIGURE_GRAPH_QL_TESTER + ";"));
    }

    @Test
    @DisplayName("computeMutationResolverTestImports: Instancio true, JSON false → only Instancio added")
    void computeMutationResolverTestImports_instancioOnly() {
        
        final String result = ResolverImports.computeMutationResolverTestImports(true, false);

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO + ";"));
        assertFalse(result.contains(ImportConstants.MapStruct.FACTORY_MAPPERS));
    }

    @Test
    @DisplayName("computeMutationResolverTestImports: JSON true, Instancio false → only MapStruct factory added")
    void computeMutationResolverTestImports_jsonOnly() {
        
        final String result = ResolverImports.computeMutationResolverTestImports(false, true);

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));
        assertTrue(result.contains("import " + ImportConstants.MapStruct.FACTORY_MAPPERS + ";"));
    }

    @Test
    @DisplayName("computeProjectImportsForQueryUnitTests: no relations → should not include BusinessService import")
    void computeProjectImportsForQueryUnitTests_noRelations() {
        
        final String outputDir = "/out";
        final PackageConfiguration config = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fields = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.app");

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fields.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", config))
                    .thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User"))
                    .thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", config))
                    .thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService"))
                    .thenReturn("com.app.service.UserService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.app", config))
                    .thenReturn("com.app.graphql.to");
            pkg.when(() -> PackageUtils.join("com.app.graphql.to", "UserTO"))
                    .thenReturn("com.app.graphql.to.UserTO");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.app", config))
                    .thenReturn("com.app.to");
            pkg.when(() -> PackageUtils.join("com.app.to", GeneratorConstants.PAGE_TO))
                    .thenReturn("com.app.to.PageTO");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", config))
                    .thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalGraphqlExceptionHandler");

            final String result = ResolverImports.computeProjectImportsForQueryUnitTests(outputDir, model, config);

            assertTrue(result.contains("import com.app.entity.User;"));
            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.graphql.to.UserTO;"));
            assertTrue(result.contains("import com.app.to.PageTO;"));
            assertTrue(result.contains("import com.app.exception.GlobalGraphqlExceptionHandler;"));

            assertFalse(result.contains("BusinessService"),
                    "BusinessService import must NOT appear when no relations exist");
        }
    }

    @Test
    @DisplayName("computeProjectImportsForQueryUnitTests: relations exist → BusinessService import included")
    void computeProjectImportsForQueryUnitTests_withRelations() {
        
        final String outputDir = "/out";
        final PackageConfiguration config = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(List.of(new FieldDefinition()));

        final FieldDefinition relField = new FieldDefinition();

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fields = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");

            fields.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(List.of(relField));

            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", config))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");

            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", config))
                    .thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService"))
                    .thenReturn("com.shop.service.OrderService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.shop", config))
                    .thenReturn("com.shop.graphql.to");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderTO"))
                    .thenReturn("com.shop.graphql.to.OrderTO");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.shop", config))
                    .thenReturn("com.shop.to");
            pkg.when(() -> PackageUtils.join("com.shop.to", GeneratorConstants.PAGE_TO))
                    .thenReturn("com.shop.to.PageTO");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.shop", config))
                    .thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.join("com.shop.exception", GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER))
                    .thenReturn("com.shop.exception.GlobalGraphqlExceptionHandler");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.shop", config))
                    .thenReturn("com.shop.business");
            pkg.when(() -> PackageUtils.join("com.shop.business", "OrderBusinessService"))
                    .thenReturn("com.shop.business.OrderBusinessService");

            final String result = ResolverImports.computeProjectImportsForQueryUnitTests(outputDir, model, config);

            assertTrue(result.contains("import com.shop.business.OrderBusinessService;"),
                    "BusinessService import should be present when relations exist");
        }
    }

    @Test
    @DisplayName("computeProjectImportsForMutationUnitTests: no JSON fields, no relations → only base imports")
    void computeProjectImportsForMutationUnitTests_noJson_noRelations() {
        
        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.app");

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields()))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User"))
                    .thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration))
                    .thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService"))
                    .thenReturn("com.app.service.UserService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.graphql.to");
            pkg.when(() -> PackageUtils.join("com.app.graphql.to", "UserTO"))
                    .thenReturn("com.app.graphql.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.app.graphql.to", "UserCreateTO"))
                    .thenReturn("com.app.graphql.to.UserCreateTO");
            pkg.when(() -> PackageUtils.join("com.app.graphql.to", "UserUpdateTO"))
                    .thenReturn("com.app.graphql.to.UserUpdateTO");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalGraphqlExceptionHandler");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.app", packageConfiguration))
                    .thenReturn("com.app.business");

            final String result = ResolverImports.computeProjectImportsForMutationUnitTests(
                    outputDir,
                    model,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.app.entity.User;"));
            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.graphql.to.UserTO;"));
            assertTrue(result.contains("import com.app.graphql.to.UserCreateTO;"));
            assertTrue(result.contains("import com.app.graphql.to.UserUpdateTO;"));
            assertTrue(result.contains("import com.app.exception.GlobalGraphqlExceptionHandler;"));
            assertTrue(result.contains("import " + ImportConstants.Jackson.OBJECT_MAPPER + ";"));
            assertTrue(result.contains("import " + ImportConstants.Jackson.TYPE_REFERENCE + ";"));

            assertFalse(result.contains("GraphQLMapper"),
                    "No helper GraphQL mappers expected when there are no JSON fields");
            assertFalse(result.contains("BusinessService"),
                    "BusinessService import should not be present without relations");
        }
    }

    @Test
    @DisplayName("computeProjectImportsForMutationUnitTests: JSON fields present → helper GraphQL mappers imported")
    void computeProjectImportsForMutationUnitTests_withJsonFields() {
        
        final String outputDir = "/out/json";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition jsonField = new FieldDefinition().setName("Shipping");
        final FieldDefinition nonJsonField = new FieldDefinition().setName("Test");

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(List.of(jsonField, nonJsonField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(nonJsonField)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("Shipping");
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.computeHelperGraphQlMapperPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.graphql.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.helper.mapper", "ShippingGraphQLMapper"))
                    .thenReturn("com.shop.graphql.helper.mapper.ShippingGraphQLMapper");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");
            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService"))
                    .thenReturn("com.shop.service.OrderService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.graphql.to");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderTO"))
                    .thenReturn("com.shop.graphql.to.OrderTO");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderCreateTO"))
                    .thenReturn("com.shop.graphql.to.OrderCreateTO");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderUpdateTO"))
                    .thenReturn("com.shop.graphql.to.OrderUpdateTO");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.join("com.shop.exception", GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER))
                    .thenReturn("com.shop.exception.GlobalGraphqlExceptionHandler");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.business");

            final String result = ResolverImports.computeProjectImportsForMutationUnitTests(
                    outputDir,
                    model,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.shop.graphql.helper.mapper.ShippingGraphQLMapper;"),
                    "Helper GraphQL mapper for JSON field should be imported");
            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.graphql.to.OrderTO;"));
            assertTrue(result.contains("import com.shop.graphql.to.OrderCreateTO;"));
            assertTrue(result.contains("import com.shop.graphql.to.OrderUpdateTO;"));
            assertTrue(result.contains("import com.shop.exception.GlobalGraphqlExceptionHandler;"));
            assertTrue(result.contains("import " + ImportConstants.Jackson.OBJECT_MAPPER + ";"));
            assertTrue(result.contains("import " + ImportConstants.Jackson.TYPE_REFERENCE + ";"));
            assertFalse(result.contains("BusinessService"));
        }
    }

    @Test
    @DisplayName("computeProjectImportsForMutationUnitTests: relations present → BusinessService import added")
    void computeProjectImportsForMutationUnitTests_withRelations() {
        
        final String outputDir = "/out/relations";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("Invoice");
        model.setFields(List.of(new FieldDefinition()));

        final FieldDefinition relationField = new FieldDefinition();

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.billing");

            names.when(() -> ModelNameUtils.stripSuffix("Invoice"))
                    .thenReturn("Invoice");

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields()))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(List.of(relationField));

            pkg.when(() -> PackageUtils.computeHelperGraphQlMapperPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.graphql.helper.mapper");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.entity");
            pkg.when(() -> PackageUtils.join("com.billing.entity", "Invoice"))
                    .thenReturn("com.billing.entity.Invoice");

            pkg.when(() -> PackageUtils.computeServicePackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.service");
            pkg.when(() -> PackageUtils.join("com.billing.service", "InvoiceService"))
                    .thenReturn("com.billing.service.InvoiceService");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.graphql.to");
            pkg.when(() -> PackageUtils.join("com.billing.graphql.to", "InvoiceTO"))
                    .thenReturn("com.billing.graphql.to.InvoiceTO");
            pkg.when(() -> PackageUtils.join("com.billing.graphql.to", "InvoiceCreateTO"))
                    .thenReturn("com.billing.graphql.to.InvoiceCreateTO");
            pkg.when(() -> PackageUtils.join("com.billing.graphql.to", "InvoiceUpdateTO"))
                    .thenReturn("com.billing.graphql.to.InvoiceUpdateTO");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.exception");
            pkg.when(() -> PackageUtils.join("com.billing.exception", GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER))
                    .thenReturn("com.billing.exception.GlobalGraphqlExceptionHandler");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.billing", packageConfiguration))
                    .thenReturn("com.billing.business");
            pkg.when(() -> PackageUtils.join("com.billing.business", "InvoiceBusinessService"))
                    .thenReturn("com.billing.business.InvoiceBusinessService");

            final String result = ResolverImports.computeProjectImportsForMutationUnitTests(
                    outputDir,
                    model,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.billing.business.InvoiceBusinessService;"),
                    "BusinessService import should be present when relations exist");
            assertTrue(result.contains("import " + ImportConstants.Jackson.OBJECT_MAPPER + ";"));
            assertTrue(result.contains("import " + ImportConstants.Jackson.TYPE_REFERENCE + ";"));
        }
    }
    
}
