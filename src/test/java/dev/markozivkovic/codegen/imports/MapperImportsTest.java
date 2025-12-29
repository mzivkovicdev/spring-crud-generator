package dev.markozivkovic.codegen.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

class MapperImportsTest {

    @Test
    @DisplayName("computeMapperImports: GraphQL mapper, no swagger, no JSON fields → imports entity + GraphQL TO only")
    void computeMapperImports_graphql_noSwagger_noJson() {
        
        final String packagePath = "com.example";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getName()).thenReturn("User");
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.computeEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.entity");
            pkg.when(() -> PackageUtils.join("com.example.entity", "User"))
                    .thenReturn("com.example.entity.User");
            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.graphql.to");
            pkg.when(() -> PackageUtils.join("com.example.graphql.to", "UserTO"))
                    .thenReturn("com.example.graphql.to.UserTO");

            final String result = MapperImports.computeMapperImports(
                    packagePath,
                    model,
                    packageConfiguration,
                    false,
                    true
            );

            assertTrue(result.contains("import com.example.entity.User;"),
                    "Entity import missing");
            assertTrue(result.contains("import com.example.graphql.to.UserTO;"),
                    "GraphQL TO import missing");

            assertFalse(result.contains("RestMapper"));
            assertFalse(result.contains("GraphQLMapper"));
            assertFalse(result.contains("generated"));
        }
    }

    @Test
    @DisplayName("computeMapperImports: REST mapper, swagger=true, JSON fields present → helper Rest mappers + entity + REST TO + generated model")
    void computeMapperImports_rest_swagger_withJson() {
        
        final String packagePath = "com.shop";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getName()).thenReturn("Order");
        final FieldDefinition jsonField = new FieldDefinition();
        Mockito.when(model.getFields()).thenReturn(List.of(jsonField));

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("Order"))
                    .thenReturn("OrderDTO");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList()))
                    .thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("Address");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.rest.mapper.helper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper.helper", "AddressRestMapper"))
                    .thenReturn("com.shop.rest.mapper.helper.AddressRestMapper");

            pkg.when(() -> PackageUtils.computeEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, "order"))
                    .thenReturn("com.shop.generated.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.order", "OrderDTO"))
                    .thenReturn("com.shop.generated.order.OrderDTO");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.rest.to");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderTO"))
                    .thenReturn("com.shop.rest.to.OrderTO");

            final String result = MapperImports.computeMapperImports(
                    packagePath,
                    model,
                    packageConfiguration,
                    true,
                    false
            );

            assertTrue(result.contains("import com.shop.rest.mapper.helper.AddressRestMapper;"),
                    "Helper REST mapper import missing");
            assertTrue(result.contains("import com.shop.entity.Order;"),
                    "Entity import missing");
            assertTrue(result.contains("import com.shop.rest.to.OrderTO;"),
                    "REST TO import missing");
            assertTrue(result.contains("import com.shop.generated.order.OrderDTO;"),
                    "Generated swagger model import missing");
            assertFalse(result.contains("GraphQLMapper"));
            assertFalse(result.contains(".graphql.to."));
        }
    }

    @Test
    @DisplayName("computeMapperImports: GraphQL mapper with JSON field → use GraphQL helper mapper and GraphQL TO")
    void computeMapperImports_graphql_withJsonHelpers() {
        
        final String packagePath = "com.api";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getName()).thenReturn("Profile");
        final FieldDefinition jsonField = new FieldDefinition();
        Mockito.when(model.getFields()).thenReturn(List.of(jsonField));

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("Profile"))
                    .thenReturn("Profile");

            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList()))
                    .thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("Settings");

            pkg.when(() -> PackageUtils.computeHelperGraphQlMapperPackage(packagePath, packageConfiguration))
                    .thenReturn("com.api.graphql.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.api.graphql.helper.mapper", "SettingsGraphQLMapper"))
                    .thenReturn("com.api.graphql.helper.mapper.SettingsGraphQLMapper");

            pkg.when(() -> PackageUtils.computeEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.api.entity");
            pkg.when(() -> PackageUtils.join("com.api.entity", "Profile"))
                    .thenReturn("com.api.entity.Profile");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.api.graphql.to");
            pkg.when(() -> PackageUtils.join("com.api.graphql.to", "ProfileTO"))
                    .thenReturn("com.api.graphql.to.ProfileTO");

            final String result = MapperImports.computeMapperImports(
                    packagePath,
                    model,
                    packageConfiguration,
                    false,
                    true
            );

            assertTrue(result.contains("import com.api.graphql.helper.mapper.SettingsGraphQLMapper;"));
            assertTrue(result.contains("import com.api.entity.Profile;"));
            assertTrue(result.contains("import com.api.graphql.to.ProfileTO;"));
            assertFalse(result.contains(".rest.to."));
        }
    }

    @Test
    @DisplayName("computeHelperMapperImports: GraphQL helper mapper, swagger=false → helper GraphQL TO + helper entity")
    void computeHelperMapperImports_graphql_noSwagger() {
        
        final String packagePath = "com.example";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition jsonModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(jsonModel.getName()).thenReturn("AddressJson");

        final ModelDefinition parentModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(parentModel.getName()).thenReturn("User");

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("AddressJson"))
                    .thenReturn("AddressJson");

            pkg.when(() -> PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.graphql.helper.to");
            pkg.when(() -> PackageUtils.join("com.example.graphql.helper.to", "AddressJsonTO"))
                    .thenReturn("com.example.graphql.helper.to.AddressJsonTO");
            pkg.when(() -> PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.helper.entity");
            pkg.when(() -> PackageUtils.join("com.example.helper.entity", "AddressJson"))
                    .thenReturn("com.example.helper.entity.AddressJson");

            final String result = MapperImports.computeHelperMapperImports(
                    packagePath,
                    jsonModel,
                    parentModel,
                    packageConfiguration,
                    false,
                    true
            );

            assertTrue(result.contains("import com.example.graphql.helper.to.AddressJsonTO;"),
                    "Helper GraphQL TO import missing");
            assertTrue(result.contains("import com.example.helper.entity.AddressJson;"),
                    "Helper entity import missing");
            assertFalse(result.contains("generated"),
                    "Swagger model import should not be present");
        }
    }

    @Test
    @DisplayName("computeHelperMapperImports: REST helper mapper, swagger=true → REST helper TO + helper entity + generated parent-based model")
    void computeHelperMapperImports_rest_swagger() {
        
        final String packagePath = "com.shop";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition jsonModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(jsonModel.getName()).thenReturn("AddressJson");

        final ModelDefinition parentModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(parentModel.getName()).thenReturn("Order");

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("AddressJson"))
                    .thenReturn("AddressJson");
            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("AddressJson"))
                    .thenReturn("AddressJsonDTO");

            pkg.when(() -> PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.rest.helper.to");
            pkg.when(() -> PackageUtils.join("com.shop.rest.helper.to", "AddressJsonTO"))
                    .thenReturn("com.shop.rest.helper.to.AddressJsonTO");

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, "order"))
                    .thenReturn("com.shop.generated.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.order", "AddressJsonDTO"))
                    .thenReturn("com.shop.generated.order.AddressJsonDTO");

            pkg.when(() -> PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.helper.entity");
            pkg.when(() -> PackageUtils.join("com.shop.helper.entity", "AddressJson"))
                    .thenReturn("com.shop.helper.entity.AddressJson");

            final String result = MapperImports.computeHelperMapperImports(
                    packagePath,
                    jsonModel,
                    parentModel,
                    packageConfiguration,
                    true,
                    false
            );

            assertTrue(result.contains("import com.shop.rest.helper.to.AddressJsonTO;"),
                    "REST helper TO import missing");
            assertTrue(result.contains("import com.shop.helper.entity.AddressJson;"),
                    "Helper entity import missing");
            assertTrue(result.contains("import com.shop.generated.order.AddressJsonDTO;"),
                    "Generated swagger model import missing");
            assertFalse(result.contains(".graphql."),
                    "Should not contain GraphQL packages");
        }
    }

    @Test
    @DisplayName("computeTestMapperImports: REST, swagger=false → REST TO + entity imports")
    void computeTestMapperImports_rest_noSwagger() {
        
        final String packagePath = "com.example";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getName()).thenReturn("User");

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.rest.to");
            pkg.when(() -> PackageUtils.join("com.example.rest.to", "UserTO"))
                    .thenReturn("com.example.rest.to.UserTO");

            pkg.when(() -> PackageUtils.computeEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.entity");
            pkg.when(() -> PackageUtils.join("com.example.entity", "User"))
                    .thenReturn("com.example.entity.User");

            final String result = MapperImports.computeTestMapperImports(
                    packagePath,
                    model,
                    packageConfiguration,
                    false,
                    false  
            );

            assertTrue(result.contains("import com.example.rest.to.UserTO;"),
                    "REST TO import missing");
            assertTrue(result.contains("import com.example.entity.User;"),
                    "Entity import missing");
            assertFalse(result.contains("generated"),
                    "Swagger generated model import should not be present");
            assertFalse(result.contains(".graphql.to."));
        }
    }

    @Test
    @DisplayName("computeTestMapperImports: GraphQL, swagger=true → GraphQL TO + entity + generated model imports")
    void computeTestMapperImports_graphql_swagger() {
        
        final String packagePath = "com.shop";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getName()).thenReturn("Order");

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("Order"))
                    .thenReturn("OrderDTO");
            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.graphql.to");
            pkg.when(() -> PackageUtils.join("com.shop.graphql.to", "OrderTO"))
                    .thenReturn("com.shop.graphql.to.OrderTO");
            pkg.when(() -> PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, "order"))
                    .thenReturn("com.shop.generated.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.order", "OrderDTO"))
                    .thenReturn("com.shop.generated.order.OrderDTO");
            pkg.when(() -> PackageUtils.computeEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");

            final String result = MapperImports.computeTestMapperImports(
                    packagePath,
                    model,
                    packageConfiguration,
                    true,
                    true
            );

            assertTrue(result.contains("import com.shop.graphql.to.OrderTO;"),
                    "GraphQL TO import missing");
            assertTrue(result.contains("import com.shop.entity.Order;"),
                    "Entity import missing");
            assertTrue(result.contains("import com.shop.generated.order.OrderDTO;"),
                    "Generated swagger model import missing");
            assertFalse(result.contains(".rest.to."));
        }
    }

    @Test
    @DisplayName("computeTestHelperMapperImports: GraphQL, swagger=false → helper GraphQL TO + helper entity")
    void computeTestHelperMapperImports_graphql_noSwagger() {
        
        final String packagePath = "com.example";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition jsonModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(jsonModel.getName()).thenReturn("AddressJson");

        final ModelDefinition parentModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(parentModel.getName()).thenReturn("User");

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("AddressJson"))
                    .thenReturn("AddressJson");

            pkg.when(() -> PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.graphql.helper.to");
            pkg.when(() -> PackageUtils.join("com.example.graphql.helper.to", "AddressJsonTO"))
                    .thenReturn("com.example.graphql.helper.to.AddressJsonTO");
            pkg.when(() -> PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.helper.entity");
            pkg.when(() -> PackageUtils.join("com.example.helper.entity", "AddressJson"))
                    .thenReturn("com.example.helper.entity.AddressJson");

            final String result = MapperImports.computeTestHelperMapperImports(
                    packagePath,
                    jsonModel,
                    parentModel,
                    packageConfiguration,
                    false,
                    true
            );

            assertTrue(result.contains("import com.example.graphql.helper.to.AddressJsonTO;"),
                    "GraphQL helper TO import missing");
            assertTrue(result.contains("import com.example.helper.entity.AddressJson;"),
                    "Helper entity import missing");
            assertFalse(result.contains("generated"),
                    "Swagger generated model import should not be present");
        }
    }

    @Test
    @DisplayName("computeTestHelperMapperImports: REST, swagger=true → REST helper TO + helper entity + generated model")
    void computeTestHelperMapperImports_rest_swagger() {
        
        final String packagePath = "com.shop";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition jsonModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(jsonModel.getName()).thenReturn("MetaJson");

        final ModelDefinition parentModel = Mockito.mock(ModelDefinition.class);
        Mockito.when(parentModel.getName()).thenReturn("Order");

        try (final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            names.when(() -> ModelNameUtils.stripSuffix("MetaJson"))
                    .thenReturn("MetaJson");
            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("MetaJson"))
                    .thenReturn("MetaJsonDTO");
            pkg.when(() -> PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.rest.helper.to");
            pkg.when(() -> PackageUtils.join("com.shop.rest.helper.to", "MetaJsonTO"))
                    .thenReturn("com.shop.rest.helper.to.MetaJsonTO");
            pkg.when(() -> PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, "order"))
                    .thenReturn("com.shop.generated.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.order", "MetaJsonDTO"))
                    .thenReturn("com.shop.generated.order.MetaJsonDTO");
            pkg.when(() -> PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration))
                    .thenReturn("com.shop.helper.entity");
            pkg.when(() -> PackageUtils.join("com.shop.helper.entity", "MetaJson"))
                    .thenReturn("com.shop.helper.entity.MetaJson");

            final String result = MapperImports.computeTestHelperMapperImports(
                    packagePath,
                    jsonModel,
                    parentModel,
                    packageConfiguration,
                    true, 
                    false
            );

            assertTrue(result.contains("import com.shop.rest.helper.to.MetaJsonTO;"),
                    "REST helper TO import missing");
            assertTrue(result.contains("import com.shop.helper.entity.MetaJson;"),
                    "Helper entity import missing");
            assertTrue(result.contains("import com.shop.generated.order.MetaJsonDTO;"),
                    "Generated swagger model import missing");
            assertFalse(result.contains(".graphql."),
                    "Should not contain GraphQL packages");
        }
    }

}
