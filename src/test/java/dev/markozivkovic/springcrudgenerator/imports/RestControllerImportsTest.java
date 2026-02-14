package dev.markozivkovic.springcrudgenerator.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class RestControllerImportsTest {

    @Test
    @DisplayName("No UUID, no collection relations, no relations → no imports")
    void computeControllerBaseImports_noUuid_noCollections_noRelations() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());

        final List<ModelDefinition> entities = Collections.emptyList();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());
            FieldDefinition idField = new FieldDefinition();
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());

            final String result = RestControllerImports.computeControllerBaseImports(model, entities);

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("Model id is UUID → UUID import is added")
    void computeControllerBaseImports_modelUuidId() {
        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());

        final List<ModelDefinition> entities = Collections.emptyList();

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());

            final String result = RestControllerImports.computeControllerBaseImports(model, entities);

            assertTrue(result.contains("import " + ImportConstants.Java.UUID + ";"),
                    "UUID import should be present when model id is UUID");
            assertFalse(result.contains(ImportConstants.Java.LIST));
            assertFalse(result.contains(ImportConstants.Java.COLLECTORS));
        }
    }

    @Test
    @DisplayName("Many-to-many or one-to-many relations → List and Collectors imports are added")
    void computeControllerBaseImports_collectionRelations_addListAndCollectors() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        List<ModelDefinition> entities = Collections.emptyList();

        final FieldDefinition idField = new FieldDefinition();
        final FieldDefinition m2mField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields()))
                    .thenReturn(List.of(m2mField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());

            final String result = RestControllerImports.computeControllerBaseImports(model, entities);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"),
                    "List import should be present when there is a collection relation");
            assertTrue(result.contains("import " + ImportConstants.Java.COLLECTORS + ";"),
                    "Collectors import should be present when there is a collection relation");
        }
    }

    @Test
    @DisplayName("Related entity with UUID id → UUID import added based on relation model")
    void computeControllerBaseImports_relatedModelUuidId_addsUuidImport() {
        
        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("Related");

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(relationField));

        final ModelDefinition relatedModel = new ModelDefinition();
        relatedModel.setName("Related");
        final FieldDefinition relatedIdField = new FieldDefinition();
        relatedModel.setFields(List.of(relatedIdField));

        final List<ModelDefinition> entities = List.of(relatedModel);

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractIdField(relatedModel.getFields()))
                    .thenReturn(relatedIdField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(relatedIdField))
                    .thenReturn(true);

            final String result = RestControllerImports.computeControllerBaseImports(model, entities);

            assertTrue(result.contains("import " + ImportConstants.Java.UUID + ";"),
                    "UUID import should be present when a related model id is UUID");
        }
    }

    @Test
    @DisplayName("Missing related entity in entities list → throws NoSuchElementException")
    void computeControllerBaseImports_missingRelatedEntity_throws() {
        
        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("Missing");

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(relationField));

        final List<ModelDefinition> entities = Collections.emptyList();

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields()))
                    .thenReturn(Collections.emptyList());

            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(List.of(relationField));

            assertThrows(
                    NoSuchElementException.class,
                    () -> RestControllerImports.computeControllerBaseImports(model, entities),
                    "Should throw when related entity is not found in entities list"
            );
        }
    }

    @Test
    @DisplayName("No swagger, no relations, no JSON → Entity, Service, ModelTO, ModelCreateTO, ModelUpdateTO, PageTO, RestMapper")
    void computeControllerProjectImports_noSwagger_noRelations_noJson() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            names.when(() -> ModelNameUtils.computeCreateTOModelName("User")).thenReturn("UserCreateTO");
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("User")).thenReturn("UserUpdateTO");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO")).thenReturn("com.app.rest.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserCreateTO")).thenReturn("com.app.rest.to.UserCreateTO");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserUpdateTO")).thenReturn("com.app.rest.to.UserUpdateTO");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.app", packageConfiguration)).thenReturn("com.app.to");
            pkg.when(() -> PackageUtils.join("com.app.to", GeneratorConstants.PAGE_TO)).thenReturn("com.app.to.PageTO");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeControllerProjectImports(
                    model, outputDir, false, packageConfiguration
            );

            assertTrue(result.contains("import com.app.entity.User;"));
            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.rest.to.UserTO;"));
            assertTrue(result.contains("import com.app.rest.to.UserCreateTO;"));
            assertTrue(result.contains("import com.app.rest.to.UserUpdateTO;"));
            assertTrue(result.contains("import com.app.to.PageTO;"));
            assertTrue(result.contains("import com.app.rest.mapper.UserRestMapper;"));

            assertFalse(result.contains("generated"), "No generated model/api imports expected when swagger=false");
            assertFalse(result.contains("BusinessService"), "No BusinessService import expected when no relations");

            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("No swagger, relations present → RelationInputTOs + BusinessService + base imports (no RelationTO)")
    void computeControllerProjectImports_noSwagger_withRelations() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("OrderItem");

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(List.of(relationField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order")).thenReturn("Order");
            names.when(() -> ModelNameUtils.stripSuffix("OrderItem")).thenReturn("OrderItem");
            names.when(() -> ModelNameUtils.computeInputTOModelName("OrderItem")).thenReturn("OrderItemInputTO");
            names.when(() -> ModelNameUtils.computeCreateTOModelName("Order")).thenReturn("OrderCreateTO");
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("Order")).thenReturn("OrderUpdateTO");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration)).thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order")).thenReturn("com.shop.entity.Order");

            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService")).thenReturn("com.shop.service.OrderService");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.to");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderTO")).thenReturn("com.shop.rest.to.OrderTO");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderCreateTO")).thenReturn("com.shop.rest.to.OrderCreateTO");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderUpdateTO")).thenReturn("com.shop.rest.to.OrderUpdateTO");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderItemInputTO")).thenReturn("com.shop.rest.to.OrderItemInputTO");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.shop", packageConfiguration)).thenReturn("com.shop.to");
            pkg.when(() -> PackageUtils.join("com.shop.to", GeneratorConstants.PAGE_TO)).thenReturn("com.shop.to.PageTO");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.business");
            pkg.when(() -> PackageUtils.join("com.shop.business", "OrderBusinessService")).thenReturn("com.shop.business.OrderBusinessService");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper", "OrderRestMapper")).thenReturn("com.shop.rest.mapper.OrderRestMapper");

            final String result = RestControllerImports.computeControllerProjectImports(
                    model, outputDir, false, packageConfiguration
            );

            assertTrue(result.contains("import com.shop.rest.to.OrderItemInputTO;"));
            assertFalse(result.contains("import com.shop.rest.to.OrderItemTO;"), "RelationTO should NOT be imported anymore");
            assertTrue(result.contains("import com.shop.business.OrderBusinessService;"));
            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.rest.to.OrderTO;"));
            assertTrue(result.contains("import com.shop.rest.to.OrderCreateTO;"));
            assertTrue(result.contains("import com.shop.rest.to.OrderUpdateTO;"));
            assertTrue(result.contains("import com.shop.to.PageTO;"));
            assertTrue(result.contains("import com.shop.rest.mapper.OrderRestMapper;"));

            assertFalse(result.contains("generated"));
            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("No swagger, JSON fields present → helper REST mappers imported + Create/Update TO")
    void computeControllerProjectImports_noSwagger_withJsonFields() {

        final String outputDir = "/out/json";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition jsonField = new FieldDefinition().setName("Settings");
        final FieldDefinition normalField = new FieldDefinition().setName("Test");

        final ModelDefinition model = new ModelDefinition();
        model.setName("Profile");
        model.setFields(List.of(jsonField, normalField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.api");
            names.when(() -> ModelNameUtils.stripSuffix("Profile")).thenReturn("Profile");
            names.when(() -> ModelNameUtils.computeCreateTOModelName("Profile")).thenReturn("ProfileCreateTO");
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("Profile")).thenReturn("ProfileUpdateTO");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(normalField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Settings");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.api.rest.helper.mapper", "SettingsRestMapper"))
            .thenReturn("com.api.rest.helper.mapper.SettingsRestMapper");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.api", packageConfiguration)).thenReturn("com.api.entity");
            pkg.when(() -> PackageUtils.join("com.api.entity", "Profile")).thenReturn("com.api.entity.Profile");

            pkg.when(() -> PackageUtils.computeServicePackage("com.api", packageConfiguration)).thenReturn("com.api.service");
            pkg.when(() -> PackageUtils.join("com.api.service", "ProfileService")).thenReturn("com.api.service.ProfileService");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.to");
            pkg.when(() -> PackageUtils.join("com.api.rest.to", "ProfileTO")).thenReturn("com.api.rest.to.ProfileTO");
            pkg.when(() -> PackageUtils.join("com.api.rest.to", "ProfileCreateTO")).thenReturn("com.api.rest.to.ProfileCreateTO");
            pkg.when(() -> PackageUtils.join("com.api.rest.to", "ProfileUpdateTO")).thenReturn("com.api.rest.to.ProfileUpdateTO");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.api", packageConfiguration)).thenReturn("com.api.to");
            pkg.when(() -> PackageUtils.join("com.api.to", GeneratorConstants.PAGE_TO)).thenReturn("com.api.to.PageTO");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.api.rest.mapper", "ProfileRestMapper")).thenReturn("com.api.rest.mapper.ProfileRestMapper");

            final String result = RestControllerImports.computeControllerProjectImports(
                    model, outputDir, false, packageConfiguration
            );

            assertTrue(result.contains("import com.api.rest.helper.mapper.SettingsRestMapper;"));
            assertTrue(result.contains("import com.api.entity.Profile;"));
            assertTrue(result.contains("import com.api.service.ProfileService;"));
            assertTrue(result.contains("import com.api.rest.to.ProfileTO;"));
            assertTrue(result.contains("import com.api.rest.to.ProfileCreateTO;"));
            assertTrue(result.contains("import com.api.rest.to.ProfileUpdateTO;"));
            assertTrue(result.contains("import com.api.to.PageTO;"));
            assertTrue(result.contains("import com.api.rest.mapper.ProfileRestMapper;"));

            assertFalse(result.contains("generated"));
            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Swagger enabled, relations present, no JSON → generated API + create/update + model + get200 + relation Input + BusinessService")
    void computeControllerProjectImports_swagger_withRelations_noJson() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("OrderItem");

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(List.of(relationField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order")).thenReturn("Order");
            names.when(() -> ModelNameUtils.stripSuffix("OrderItem")).thenReturn("OrderItem");

            names.when(() -> ModelNameUtils.computeOpenApiModelName("Order")).thenReturn("OrderDTO");
            names.when(() -> ModelNameUtils.computeOpenApiCreateModelName("Order")).thenReturn("OrderCreateDTO");
            names.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("Order")).thenReturn("OrderUpdateDTO");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.shop", packageConfiguration))
                    .thenReturn(Set.of("import com.shop.enums.StatusEnum;"));

            pkg.when(() -> PackageUtils.computeGeneratedApiPackage("com.shop", packageConfiguration, "order")).thenReturn("com.shop.generated.api.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.api.order", "OrdersApi")).thenReturn("com.shop.generated.api.order.OrdersApi");
            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.shop", packageConfiguration, "order")).thenReturn("com.shop.generated.model.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrderItemInput")).thenReturn("com.shop.generated.model.order.OrderItemInput");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrderCreateDTO")).thenReturn("com.shop.generated.model.order.OrderCreateDTO");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrderUpdateDTO")).thenReturn("com.shop.generated.model.order.OrderUpdateDTO");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrderDTO")).thenReturn("com.shop.generated.model.order.OrderDTO");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrdersGet200Response")).thenReturn("com.shop.generated.model.order.OrdersGet200Response");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration)).thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order")).thenReturn("com.shop.entity.Order");

            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService")).thenReturn("com.shop.service.OrderService");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.business");
            pkg.when(() -> PackageUtils.join("com.shop.business", "OrderBusinessService")).thenReturn("com.shop.business.OrderBusinessService");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper", "OrderRestMapper")).thenReturn("com.shop.rest.mapper.OrderRestMapper");

            final String result = RestControllerImports.computeControllerProjectImports(
                    model, outputDir, true, packageConfiguration
            );

            assertTrue(result.contains("import com.shop.enums.StatusEnum;"));
            assertTrue(result.contains("import com.shop.generated.api.order.OrdersApi;"));

            assertTrue(result.contains("import com.shop.generated.model.order.OrderCreateDTO;"));
            assertTrue(result.contains("import com.shop.generated.model.order.OrderUpdateDTO;"));
            assertTrue(result.contains("import com.shop.generated.model.order.OrderDTO;"));
            assertTrue(result.contains("import com.shop.generated.model.order.OrdersGet200Response;"));
            assertTrue(result.contains("import com.shop.generated.model.order.OrderItemInput;"));

            assertTrue(result.contains("import com.shop.business.OrderBusinessService;"));
            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.rest.mapper.OrderRestMapper;"));

            assertFalse(result.contains(".rest.to.OrderTO;"));
            assertFalse(result.contains("PageTO"));

            enumImports.verify(() -> EnumImports.computeEnumImports(model, "com.shop", packageConfiguration));
        }
    }

    @Test
    @DisplayName("Swagger enabled, no relations, no JSON → generated API + create/update + model + get200; no BusinessService, no relation inputs")
    void computeControllerProjectImports_swagger_noRelations_noJson() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration))
                    .thenReturn(Set.of());

            names.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("UserDTO");
            names.when(() -> ModelNameUtils.computeOpenApiCreateModelName("User")).thenReturn("UserCreateDTO");
            names.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("User")).thenReturn("UserUpdateDTO");

            pkg.when(() -> PackageUtils.computeGeneratedApiPackage("com.app", packageConfiguration, "user"))
                    .thenReturn("com.app.generated.api.user");
            pkg.when(() -> PackageUtils.join("com.app.generated.api.user", "UsersApi"))
                    .thenReturn("com.app.generated.api.user.UsersApi");

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.app", packageConfiguration, "user"))
                    .thenReturn("com.app.generated.model.user");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserCreateDTO"))
                    .thenReturn("com.app.generated.model.user.UserCreateDTO");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserUpdateDTO"))
                    .thenReturn("com.app.generated.model.user.UserUpdateDTO");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserDTO"))
                    .thenReturn("com.app.generated.model.user.UserDTO");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UsersGet200Response"))
                    .thenReturn("com.app.generated.model.user.UsersGet200Response");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeControllerProjectImports(
                    model, outputDir, true, packageConfiguration
            );

            assertTrue(result.contains("import com.app.generated.api.user.UsersApi;"));
            assertTrue(result.contains("import com.app.generated.model.user.UserCreateDTO;"));
            assertTrue(result.contains("import com.app.generated.model.user.UserUpdateDTO;"));
            assertTrue(result.contains("import com.app.generated.model.user.UserDTO;"));
            assertTrue(result.contains("import com.app.generated.model.user.UsersGet200Response;"));

            assertFalse(result.contains("BusinessService"));
            assertFalse(result.contains("Input;"), "No relation input imports expected when no relations");
            assertFalse(result.contains("PageTO"));
            assertFalse(result.contains(".rest.to.UserTO;"));

            enumImports.verify(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration));
        }
    }

    @Test
    @DisplayName("No swagger → always includes CreateTO and UpdateTO imports for the model")
    void computeControllerProjectImports_noSwagger_alwaysIncludesCreateAndUpdateTO() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("Account");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.t");

            names.when(() -> ModelNameUtils.stripSuffix("Account")).thenReturn("Account");
            names.when(() -> ModelNameUtils.computeCreateTOModelName("Account")).thenReturn("AccountCreateTO");
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("Account")).thenReturn("AccountUpdateTO");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.t", packageConfiguration)).thenReturn("com.t.rest.to");
            pkg.when(() -> PackageUtils.join("com.t.rest.to", "AccountCreateTO")).thenReturn("com.t.rest.to.AccountCreateTO");
            pkg.when(() -> PackageUtils.join("com.t.rest.to", "AccountUpdateTO")).thenReturn("com.t.rest.to.AccountUpdateTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.t", packageConfiguration)).thenReturn("com.t.entity");
            pkg.when(() -> PackageUtils.join("com.t.entity", "Account")).thenReturn("com.t.entity.Account");
            pkg.when(() -> PackageUtils.computeServicePackage("com.t", packageConfiguration)).thenReturn("com.t.service");
            pkg.when(() -> PackageUtils.join("com.t.service", "AccountService")).thenReturn("com.t.service.AccountService");
            pkg.when(() -> PackageUtils.join("com.t.rest.to", "AccountTO")).thenReturn("com.t.rest.to.AccountTO");
            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.t", packageConfiguration)).thenReturn("com.t.to");
            pkg.when(() -> PackageUtils.join("com.t.to", GeneratorConstants.PAGE_TO)).thenReturn("com.t.to.PageTO");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.t", packageConfiguration)).thenReturn("com.t.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.t.rest.mapper", "AccountRestMapper")).thenReturn("com.t.rest.mapper.AccountRestMapper");

            final String result = RestControllerImports.computeControllerProjectImports(
                    model, outputDir, false, packageConfiguration
            );

            assertTrue(result.contains("import com.t.rest.to.AccountCreateTO;"));
            assertTrue(result.contains("import com.t.rest.to.AccountUpdateTO;"));
        }
    }

    @Test
    @DisplayName("computeAddRelationEndpointBaseImports: UUID id field → UUID import added")
    void computeAddRelationEndpointBaseImports_uuidId() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition idField = new FieldDefinition();
        model.setFields(List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(true);

            final String result = RestControllerImports.computeAddRelationEndpointBaseImports(model);

            assertTrue(result.contains(ImportConstants.Java.UUID),
                    "UUID import value should be present when id field is UUID");
        }
    }

    @Test
    @DisplayName("computeAddRelationEndpointBaseImports: non-UUID id field → no imports")
    void computeAddRelationEndpointBaseImports_nonUuidId() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition idField = new FieldDefinition();
        model.setFields(List.of(idField));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            final String result = RestControllerImports.computeAddRelationEndpointBaseImports(model);

            assertEquals("", result, "No imports should be produced when id field is not UUID");
        }
    }

    @Test
    @DisplayName("computeAddRelationEndpointTestImports: Instancio disabled, Spring Boot 3 → no Instancio import, but all base imports present")
    void computeAddRelationEndpointTestImports_instancioDisabled_springBoot3() {

        final String result = RestControllerImports.computeAddRelationEndpointTestImports(false, "3");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));

        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.MapStruct.FACTORY_MAPPERS + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringHttp.MEDIA_TYPE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.RESULT_ACTIONS + ";"));
    }

    @Test
    @DisplayName("computeAddRelationEndpointTestImports: Instancio disabled, Spring Boot 4 → no Instancio import, but all base imports present")
    void computeAddRelationEndpointTestImports_instancioDisabled_springBoot4() {

        final String result = RestControllerImports.computeAddRelationEndpointTestImports(false, "4");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));

        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.MapStruct.FACTORY_MAPPERS + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringHttp.MEDIA_TYPE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.RESULT_ACTIONS + ";"));
    }

    @Test
    @DisplayName("computeAddRelationEndpointTestImports: Instancio enabled → Instancio import added")
    void computeAddRelationEndpointTestImports_instancioEnabled() {

        final String result = RestControllerImports.computeAddRelationEndpointTestImports(true, "4");

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO + ";"),
                "Instancio import should be present when enabled");
    }

    @Test
    @DisplayName("computeAddRelationEndpointTestImports: Boot 3 uses @MockBean, Boot 4 uses @MockitoBean")
    void computeAddRelationEndpointTestImports_mockAnnotationDependsOnBootVersion() {

        final String boot3 = RestControllerImports.computeAddRelationEndpointTestImports(false, "3");
        assertTrue(boot3.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertFalse(boot3.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));

        final String boot4 = RestControllerImports.computeAddRelationEndpointTestImports(false, "4");
        assertTrue(boot4.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertFalse(boot4.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
    }

    @Test
    @DisplayName("computeDeleteEndpointTestImports: Instancio disabled, Spring Boot 3 → no Instancio import, base imports present")
    void computeDeleteEndpointTestImports_instancioDisabled_springBoot3() {

        final String result = RestControllerImports.computeDeleteEndpointTestImports(false, "3");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));

        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
    }

    @Test
    @DisplayName("computeDeleteEndpointTestImports: Instancio disabled, Spring Boot 4 → no Instancio import, base imports present")
    void computeDeleteEndpointTestImports_instancioDisabled_springBoot4() {

        final String result = RestControllerImports.computeDeleteEndpointTestImports(false, "4");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));

        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
    }

    @Test
    @DisplayName("computeDeleteEndpointTestImports: Instancio enabled → Instancio import added")
    void computeDeleteEndpointTestImports_instancioEnabled() {

        final String result = RestControllerImports.computeDeleteEndpointTestImports(true, "4");

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO + ";"),
                "Instancio import should be present when enabled");
    }

    @Test
    @DisplayName("computeDeleteEndpointTestImports: Boot 3 uses @MockBean, Boot 4 uses @MockitoBean")
    void computeDeleteEndpointTestImports_mockAnnotationDependsOnBootVersion() {

        final String boot3 = RestControllerImports.computeDeleteEndpointTestImports(false, "3");
        assertTrue(boot3.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertFalse(boot3.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));

        final String boot4 = RestControllerImports.computeDeleteEndpointTestImports(false, "4");
        assertTrue(boot4.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertFalse(boot4.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
    }

    @Test
    @DisplayName("computeUpdateEndpointTestImports: Instancio disabled, Spring Boot 3 → no Instancio import, base imports present")
    void computeUpdateEndpointTestImports_instancioDisabled_springBoot3() {

        final String result = RestControllerImports.computeUpdateEndpointTestImports(false, "3");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));

        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.MapStruct.FACTORY_MAPPERS + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringHttp.MEDIA_TYPE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.RESULT_ACTIONS + ";"));
    }

    @Test
    @DisplayName("computeUpdateEndpointTestImports: Instancio disabled, Spring Boot 4 → no Instancio import, base imports present")
    void computeUpdateEndpointTestImports_instancioDisabled_springBoot4() {

        final String result = RestControllerImports.computeUpdateEndpointTestImports(false, "4");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));

        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.MapStruct.FACTORY_MAPPERS + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringHttp.MEDIA_TYPE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.RESULT_ACTIONS + ";"));
    }

    @Test
    @DisplayName("computeUpdateEndpointTestImports: Instancio enabled → Instancio import added")
    void computeUpdateEndpointTestImports_instancioEnabled() {

        final String result = RestControllerImports.computeUpdateEndpointTestImports(true, "4");

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO + ";"),
                "Instancio import should be present when enabled");
    }

    @Test
    @DisplayName("computeUpdateEndpointTestImports: Boot 3 uses @MockBean, Boot 4 uses @MockitoBean")
    void computeUpdateEndpointTestImports_mockAnnotationDependsOnBootVersion() {

        final String boot3 = RestControllerImports.computeUpdateEndpointTestImports(false, "3");
        assertTrue(boot3.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertFalse(boot3.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));

        final String boot4 = RestControllerImports.computeUpdateEndpointTestImports(false, "4");
        assertTrue(boot4.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertFalse(boot4.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
    }

    @Test
    @DisplayName("computeGetEndpointTestImports: Instancio disabled, Spring Boot 3 → no Instancio import, base imports present")
    void computeGetEndpointTestImports_instancioDisabled_springBoot3() {

        final String result = RestControllerImports.computeGetEndpointTestImports(false, "3");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));
        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.MapStruct.FACTORY_MAPPERS + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot3.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_IMPL + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.RESULT_ACTIONS + ";"));
    }

    @Test
    @DisplayName("computeGetEndpointTestImports: Instancio disabled, Spring Boot 4 → no Instancio import, base imports present")
    void computeGetEndpointTestImports_instancioDisabled_springBoot4() {

        final String result = RestControllerImports.computeGetEndpointTestImports(false, "4");

        assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));
        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH + ";"));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.MapStruct.FACTORY_MAPPERS + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBean.AUTOWIRED + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_CLIENT_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.AUTO_CONFIGURE_MOCK_MVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringBootTest.SpringBoot4.WEB_MVC_TEST + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_IMPL + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.CONTEXT_CONFIGURATION + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKMVC + ";"));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.RESULT_ACTIONS + ";"));
    }

    @Test
    @DisplayName("computeGetEndpointTestImports: Instancio enabled → Instancio import added")
    void computeGetEndpointTestImports_instancioEnabled() {

        final String result = RestControllerImports.computeGetEndpointTestImports(true, "4");

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO + ";"),
                "Instancio import should be present when enabled");
    }

    @Test
    @DisplayName("computeGetEndpointTestImports: Boot 3 uses @MockBean, Boot 4 uses @MockitoBean")
    void computeGetEndpointTestImports_mockAnnotationDependsOnBootVersion() {

        final String boot3 = RestControllerImports.computeGetEndpointTestImports(false, "3");
        assertTrue(boot3.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
        assertFalse(boot3.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));

        final String boot4 = RestControllerImports.computeGetEndpointTestImports(false, "4");
        assertTrue(boot4.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN + ";"));
        assertFalse(boot4.contains("import " + ImportConstants.SpringTest.MOCK_BEAN + ";"));
    }

    @Test
    @DisplayName("computeRemoveRelationEndpointBaseImports: model id is UUID → UUID import added")
    void computeRemoveRelationEndpointBaseImports_modelUuidId() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final FieldDefinition idField = new FieldDefinition();
        final List<FieldDefinition> fields = List.of(idField);

        Mockito.when(model.getFields()).thenReturn(fields);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {
            
            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(true);

            final String result = RestControllerImports.computeRemoveRelationEndpointBaseImports(
                    model,
                    Collections.emptyList()
            );

            assertTrue(result.contains(ImportConstants.Java.UUID),
                    "UUID import should be present when model id is UUID");
        }
    }

    @Test
    @DisplayName("computeRemoveRelationEndpointBaseImports: related entity has UUID id → UUID import added")
    void computeRemoveRelationEndpointBaseImports_relatedEntityUuidId() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final FieldDefinition idField = new FieldDefinition();
        final FieldDefinition relationField = Mockito.mock(FieldDefinition.class);

        final List<FieldDefinition> fields = List.of(idField, relationField);
        Mockito.when(model.getFields()).thenReturn(fields);

        Mockito.when(relationField.getRelation()).thenReturn(new RelationDefinition());
        Mockito.when(relationField.getType()).thenReturn("Related");

        final ModelDefinition relatedEntity = Mockito.mock(ModelDefinition.class);
        Mockito.when(relatedEntity.getName()).thenReturn("Related");
        final FieldDefinition relatedIdField = new FieldDefinition();
        Mockito.when(relatedEntity.getFields()).thenReturn(List.of(relatedIdField));

        final List<ModelDefinition> entities = List.of(relatedEntity);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractIdField(relatedEntity.getFields()))
                    .thenReturn(relatedIdField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(relatedIdField))
                    .thenReturn(true);

            final String result = RestControllerImports.computeRemoveRelationEndpointBaseImports(
                    model,
                    entities
            );

            assertTrue(result.contains(ImportConstants.Java.UUID),
                    "UUID import should be present when related entity id is UUID");
        }
    }

    @Test
    @DisplayName("computeRemoveRelationEndpointBaseImports: no UUID on model or related entities → no imports")
    void computeRemoveRelationEndpointBaseImports_noUuidAnywhere() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final FieldDefinition idField = new FieldDefinition();
        final FieldDefinition relationField = Mockito.mock(FieldDefinition.class);

        final List<FieldDefinition> fields = List.of(idField, relationField);
        Mockito.when(model.getFields()).thenReturn(fields);

        Mockito.when(relationField.getRelation()).thenReturn(new RelationDefinition());
        Mockito.when(relationField.getType()).thenReturn("Related");

        final ModelDefinition relatedEntity = Mockito.mock(ModelDefinition.class);
        Mockito.when(relatedEntity.getName()).thenReturn("Related");
        final FieldDefinition relatedIdField = new FieldDefinition();
        Mockito.when(relatedEntity.getFields()).thenReturn(List.of(relatedIdField));

        final List<ModelDefinition> entities = List.of(relatedEntity);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractIdField(relatedEntity.getFields()))
                    .thenReturn(relatedIdField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(relatedIdField))
                    .thenReturn(false);

            final String result = RestControllerImports.computeRemoveRelationEndpointBaseImports(
                    model,
                    entities
            );

            assertEquals("", result, "No imports should be produced when no UUID ids exist");
        }
    }

    @Test
    @DisplayName("computeRemoveRelationEndpointBaseImports: related entity not found → throws IllegalArgumentException")
    void computeRemoveRelationEndpointBaseImports_relatedEntityMissing_throws() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final FieldDefinition idField = new FieldDefinition();
        final FieldDefinition relationField = Mockito.mock(FieldDefinition.class);

        final List<FieldDefinition> fields = List.of(idField, relationField);
        Mockito.when(model.getFields()).thenReturn(fields);
        Mockito.when(relationField.getRelation()).thenReturn(new RelationDefinition());
        Mockito.when(relationField.getType()).thenReturn("MissingEntity");

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);) {
            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            final IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> RestControllerImports.computeRemoveRelationEndpointBaseImports(
                            model,
                            Collections.emptyList()
                    )
            );

            assertTrue(ex.getMessage().contains("Related entity not found: MissingEntity"));
        }
    }

    @Test
    @DisplayName("No swagger, no relations, no JSON, GlobalExceptionHandler=false → basic imports only")
    void computeUpdateEndpointTestProjectImports_noSwagger_noRelations_noJson_noGlobalExceptionHandler() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("User")).thenReturn("UserUpdateTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");
            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO")).thenReturn("com.app.rest.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserUpdateTO")).thenReturn("com.app.rest.to.UserUpdateTO");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeUpdateEndpointTestProjectImports(
                    model, outputDir, false, packageConfiguration, false
            );

            assertTrue(result.contains("import com.app.entity.User;"));
            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.rest.to.UserTO;"));
            assertTrue(result.contains("import com.app.rest.to.UserUpdateTO;"));
            assertTrue(result.contains("import com.app.rest.mapper.UserRestMapper;"));

            assertFalse(result.contains("GlobalRestExceptionHandler"));
            assertFalse(result.contains("BusinessService"));
            assertFalse(result.contains("helper"), "No helper rest mapper imports expected");
            assertFalse(result.contains("generated"));
            assertFalse(result.contains("StatusEnum"));

            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("No swagger, no relations, no JSON, GlobalExceptionHandler=true → adds GlobalRestExceptionHandler")
    void computeUpdateEndpointTestProjectImports_noSwagger_noRelations_noJson_withGlobalExceptionHandler() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("User")).thenReturn("UserUpdateTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");
            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO")).thenReturn("com.app.rest.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserUpdateTO")).thenReturn("com.app.rest.to.UserUpdateTO");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", packageConfiguration)).thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeUpdateEndpointTestProjectImports(
                    model, outputDir, false, packageConfiguration, true
            );

            assertTrue(result.contains("import com.app.exception.GlobalRestExceptionHandler;"));
            assertTrue(result.contains("import com.app.rest.to.UserUpdateTO;"));
            assertFalse(result.contains("generated"));
            assertFalse(result.contains("StatusEnum"));

            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("No swagger, relations and JSON fields → BusinessService + helper REST mappers + rest TO + update TO")
    void computeUpdateEndpointTestProjectImports_noSwagger_withRelations_andJson() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setRelation(new RelationDefinition());
        relationField.setType("ProfileJson");

        final FieldDefinition jsonField = new FieldDefinition();
        jsonField.setRelation(null);

        final ModelDefinition model = new ModelDefinition();
        model.setName("Account");
        model.setFields(List.of(relationField, jsonField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.api");
            names.when(() -> ModelNameUtils.stripSuffix("Account")).thenReturn("Account");
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("Account")).thenReturn("AccountUpdateTO");
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(relationField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Settings");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.api.rest.helper.mapper", "SettingsRestMapper"))
                    .thenReturn("com.api.rest.helper.mapper.SettingsRestMapper");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.api", packageConfiguration)).thenReturn("com.api.business");
            pkg.when(() -> PackageUtils.join("com.api.business", "AccountBusinessService"))
                    .thenReturn("com.api.business.AccountBusinessService");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.api", packageConfiguration)).thenReturn("com.api.entity");
            pkg.when(() -> PackageUtils.join("com.api.entity", "Account")).thenReturn("com.api.entity.Account");

            pkg.when(() -> PackageUtils.computeServicePackage("com.api", packageConfiguration)).thenReturn("com.api.service");
            pkg.when(() -> PackageUtils.join("com.api.service", "AccountService")).thenReturn("com.api.service.AccountService");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.to");
            pkg.when(() -> PackageUtils.join("com.api.rest.to", "AccountTO")).thenReturn("com.api.rest.to.AccountTO");
            pkg.when(() -> PackageUtils.join("com.api.rest.to", "AccountUpdateTO")).thenReturn("com.api.rest.to.AccountUpdateTO");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.api.rest.mapper", "AccountRestMapper")).thenReturn("com.api.rest.mapper.AccountRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.api", packageConfiguration)).thenReturn("com.api.exception");
            pkg.when(() -> PackageUtils.join("com.api.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER)).thenReturn("com.api.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeUpdateEndpointTestProjectImports(
                    model, outputDir, false, packageConfiguration, true
            );

            assertTrue(result.contains("import com.api.business.AccountBusinessService;"));
            assertTrue(result.contains("import com.api.rest.helper.mapper.SettingsRestMapper;"));
            assertTrue(result.contains("import com.api.entity.Account;"));
            assertTrue(result.contains("import com.api.service.AccountService;"));
            assertTrue(result.contains("import com.api.rest.to.AccountTO;"));
            assertTrue(result.contains("import com.api.rest.to.AccountUpdateTO;"));
            assertTrue(result.contains("import com.api.rest.mapper.AccountRestMapper;"));
            assertTrue(result.contains("import com.api.exception.GlobalRestExceptionHandler;"));

            assertFalse(result.contains("generated"));
            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Swagger enabled, relations and JSON fields → enum imports, BusinessService, helper mappers, generated model + update model")
    void computeUpdateEndpointTestProjectImports_swagger_withRelations_andJson() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setRelation(new RelationDefinition());
        relationField.setType("ChildJson");

        final FieldDefinition jsonField = new FieldDefinition();
        jsonField.setRelation(null);

        final ModelDefinition model = new ModelDefinition();
        model.setName("Parent");
        model.setFields(List.of(relationField, jsonField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Parent")).thenReturn("Parent");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("Parent")).thenReturn("ParentDTO");
            names.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("Parent")).thenReturn("ParentUpdateDTO");

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.shop", packageConfiguration))
                    .thenReturn(Set.of("import com.shop.enums.StatusEnum;"));

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(relationField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Config");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.helper.mapper", "ConfigRestMapper")).thenReturn("com.shop.rest.helper.mapper.ConfigRestMapper");
            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.business");
            pkg.when(() -> PackageUtils.join("com.shop.business", "ParentBusinessService")).thenReturn("com.shop.business.ParentBusinessService");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration)).thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Parent")).thenReturn("com.shop.entity.Parent");
            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "ParentService")).thenReturn("com.shop.service.ParentService");

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.shop", packageConfiguration, "parent"))
                    .thenReturn("com.shop.generated.model.parent");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.parent", "ParentDTO")).thenReturn("com.shop.generated.model.parent.ParentDTO");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.parent", "ParentUpdateDTO")).thenReturn("com.shop.generated.model.parent.ParentUpdateDTO");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper", "ParentRestMapper")).thenReturn("com.shop.rest.mapper.ParentRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.shop", packageConfiguration)).thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.join("com.shop.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER)).thenReturn("com.shop.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeUpdateEndpointTestProjectImports(
                    model, outputDir, true, packageConfiguration, true
            );

            assertTrue(result.contains("import com.shop.enums.StatusEnum;"));
            assertTrue(result.contains("import com.shop.business.ParentBusinessService;"));
            assertTrue(result.contains("import com.shop.rest.helper.mapper.ConfigRestMapper;"));
            assertTrue(result.contains("import com.shop.entity.Parent;"));
            assertTrue(result.contains("import com.shop.service.ParentService;"));
            assertTrue(result.contains("import com.shop.rest.mapper.ParentRestMapper;"));
            assertTrue(result.contains("import com.shop.exception.GlobalRestExceptionHandler;"));
            assertTrue(result.contains("import com.shop.generated.model.parent.ParentDTO;"));
            assertTrue(result.contains("import com.shop.generated.model.parent.ParentUpdateDTO;"));
            assertFalse(result.contains(".rest.to.ParentTO;"), "Plain REST TO should not be imported in swagger mode");

            enumImports.verify(() -> EnumImports.computeEnumImports(model, "com.shop", packageConfiguration));
        }
    }

    @Test
    @DisplayName("Swagger enabled, no relations, no JSON → generated model + update model, no BusinessService/helper mappers")
    void computeUpdateEndpointTestProjectImports_swagger_noRelations_noJson() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("UserDTO");
            names.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("User")).thenReturn("UserUpdateDTO");

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration)).thenReturn(Set.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.app", packageConfiguration, "user"))
                    .thenReturn("com.app.generated.model.user");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserDTO")).thenReturn("com.app.generated.model.user.UserDTO");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserUpdateDTO")).thenReturn("com.app.generated.model.user.UserUpdateDTO");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeUpdateEndpointTestProjectImports(
                    model, outputDir, true, packageConfiguration, false
            );

            assertTrue(result.contains("import com.app.generated.model.user.UserDTO;"));
            assertTrue(result.contains("import com.app.generated.model.user.UserUpdateDTO;"));
            assertFalse(result.contains("BusinessService"));
            assertFalse(result.contains("helper"), "No helper rest mapper imports expected");
            assertFalse(result.contains(".rest.to.UserTO;"));
            assertFalse(result.contains("GlobalRestExceptionHandler"));

            enumImports.verify(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration));
        }
    }

    @Test
    @DisplayName("GlobalExceptionHandler null → should not import it")
    void computeUpdateEndpointTestProjectImports_globalExceptionHandlerNull_doesNotImport() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);
            names.when(() -> ModelNameUtils.computeUpdateTOModelName("User")).thenReturn("UserUpdateTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO")).thenReturn("com.app.rest.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserUpdateTO")).thenReturn("com.app.rest.to.UserUpdateTO");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeUpdateEndpointTestProjectImports(
                    model, outputDir, false, packageConfiguration, null
            );

            assertFalse(result.contains("GlobalRestExceptionHandler"));
            assertTrue(result.contains("import com.app.rest.to.UserUpdateTO;"));

            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Swagger enabled + GlobalExceptionHandler null → does not import GlobalRestExceptionHandler")
    void computeUpdateEndpointTestProjectImports_swagger_globalExceptionHandlerNull_doesNotImport() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("UserDTO");
            names.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("User")).thenReturn("UserUpdateDTO");

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration)).thenReturn(Set.of());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.app", packageConfiguration, "user"))
                    .thenReturn("com.app.generated.model.user");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserUpdateDTO")).thenReturn("com.app.generated.model.user.UserUpdateDTO");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserDTO")).thenReturn("com.app.generated.model.user.UserDTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");
            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeUpdateEndpointTestProjectImports(
                    model, outputDir, true, packageConfiguration, null
            );

            assertFalse(result.contains("GlobalRestExceptionHandler"));
            assertTrue(result.contains("import com.app.generated.model.user.UserDTO;"));
            assertTrue(result.contains("import com.app.generated.model.user.UserUpdateDTO;"));

            enumImports.verify(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration));
        }
    }

    @Test
    @DisplayName("No swagger, no relations, no JSON, GlobalExceptionHandler=false → basic imports only (+ CreateTO)")
    void computeCreateEndpointTestProjectImports_noSwagger_noRelations_noJson_noGlobalExceptionHandler() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);
            names.when(() -> ModelNameUtils.computeCreateTOModelName("User")).thenReturn("UserCreateTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");
            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO")).thenReturn("com.app.rest.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserCreateTO")).thenReturn("com.app.rest.to.UserCreateTO");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeCreateEndpointTestProjectImports(
                    model, outputDir, false, packageConfiguration, false
            );

            assertTrue(result.contains("import com.app.entity.User;"));
            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.rest.to.UserTO;"));
            assertTrue(result.contains("import com.app.rest.to.UserCreateTO;"));
            assertTrue(result.contains("import com.app.rest.mapper.UserRestMapper;"));
            assertFalse(result.contains("GlobalRestExceptionHandler"));
            assertFalse(result.contains("BusinessService"));
            assertFalse(result.contains("generated"));
            assertFalse(result.contains("StatusEnum"));

            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("No swagger, no relations, no JSON, GlobalExceptionHandler=true → adds GlobalRestExceptionHandler (+ CreateTO)")
    void computeCreateEndpointTestProjectImports_noSwagger_noRelations_noJson_withGlobalExceptionHandler() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);
            names.when(() -> ModelNameUtils.computeCreateTOModelName("User")).thenReturn("UserCreateTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");
            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO")).thenReturn("com.app.rest.to.UserTO");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserCreateTO")).thenReturn("com.app.rest.to.UserCreateTO");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", packageConfiguration)).thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeCreateEndpointTestProjectImports(
                    model, outputDir, false, packageConfiguration, true
            );

            assertTrue(result.contains("import com.app.exception.GlobalRestExceptionHandler;"));
            assertTrue(result.contains("import com.app.rest.to.UserCreateTO;"));
            assertFalse(result.contains("BusinessService"));
            assertFalse(result.contains("generated"));
            assertFalse(result.contains("StatusEnum"));

            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("No swagger, many-to-many/one-to-many + JSON + relations → relation InputTOs, BusinessService, helper RestMapper, OrderTO + OrderCreateTO")
    void computeCreateEndpointTestProjectImports_noSwagger_withRelations_andJson() {

        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("OrderItem");
        relationField.setRelation(new RelationDefinition());

        final FieldDefinition jsonField = new FieldDefinition();
        jsonField.setRelation(null);

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(List.of(relationField, jsonField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order")).thenReturn("Order");
            names.when(() -> ModelNameUtils.stripSuffix("OrderItem")).thenReturn("OrderItem");
            names.when(() -> ModelNameUtils.computeInputTOModelName("OrderItem")).thenReturn("OrderItemInputTO");
            names.when(() -> ModelNameUtils.computeCreateTOModelName("Order")).thenReturn("OrderCreateTO");
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(relationField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Shipping");
            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.to");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderItemInputTO")).thenReturn("com.shop.rest.to.OrderItemInputTO");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderTO")).thenReturn("com.shop.rest.to.OrderTO");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderCreateTO")).thenReturn("com.shop.rest.to.OrderCreateTO");
            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.helper.mapper", "ShippingRestMapper")).thenReturn("com.shop.rest.helper.mapper.ShippingRestMapper");
            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.business");
            pkg.when(() -> PackageUtils.join("com.shop.business", "OrderBusinessService")).thenReturn("com.shop.business.OrderBusinessService");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration)).thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order")).thenReturn("com.shop.entity.Order");
            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration)).thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService")).thenReturn("com.shop.service.OrderService");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.shop", packageConfiguration)).thenReturn("com.shop.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper", "OrderRestMapper")).thenReturn("com.shop.rest.mapper.OrderRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.shop", packageConfiguration)).thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.join("com.shop.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.shop.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeCreateEndpointTestProjectImports(
                    model, outputDir, false, packageConfiguration, true
            );

            assertTrue(result.contains("import com.shop.rest.to.OrderItemInputTO;"));
            assertTrue(result.contains("import com.shop.business.OrderBusinessService;"));
            assertTrue(result.contains("import com.shop.rest.helper.mapper.ShippingRestMapper;"));
            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.rest.to.OrderTO;"));
            assertTrue(result.contains("import com.shop.rest.to.OrderCreateTO;"));
            assertTrue(result.contains("import com.shop.rest.mapper.OrderRestMapper;"));
            assertTrue(result.contains("import com.shop.exception.GlobalRestExceptionHandler;"));
            assertFalse(result.contains("generated"));
            enumImports.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Swagger enabled, relations + many-to-many + JSON → generated relation Input + enums + Create model + base model + BusinessService + helper + mapper")
    void computeCreateEndpointTestProjectImports_swagger_withRelations_andJson() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("Child");
        relationField.setRelation(new RelationDefinition());

        final FieldDefinition jsonField = new FieldDefinition();
        jsonField.setRelation(null);

        final ModelDefinition model = new ModelDefinition();
        model.setName("Parent");
        model.setFields(List.of(relationField, jsonField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.api");

            names.when(() -> ModelNameUtils.stripSuffix("Parent")).thenReturn("Parent");
            names.when(() -> ModelNameUtils.stripSuffix("Child")).thenReturn("Child");

            names.when(() -> ModelNameUtils.computeOpenApiModelName("Parent")).thenReturn("ParentDTO");
            names.when(() -> ModelNameUtils.computeOpenApiCreateModelName("Parent")).thenReturn("ParentCreateDTO");
            names.when(() -> ModelNameUtils.computeOpenApiInputModelName("Child")).thenReturn("ChildInputDTO");
            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.api", packageConfiguration))
                    .thenReturn(Set.of("import com.api.enums.StatusEnum;"));
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(relationField));

            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(relationField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Config");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.helper.mapper");
            pkg.when(() -> PackageUtils.join("com.api.rest.helper.mapper", "ConfigRestMapper")).thenReturn("com.api.rest.helper.mapper.ConfigRestMapper");
            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.api", packageConfiguration, "parent"))
                    .thenReturn("com.api.generated.model.parent");
            pkg.when(() -> PackageUtils.join("com.api.generated.model.parent", "ChildInputDTO"))
                    .thenReturn("com.api.generated.model.parent.ChildInputDTO");
            pkg.when(() -> PackageUtils.join("com.api.generated.model.parent", "ParentCreateDTO"))
                    .thenReturn("com.api.generated.model.parent.ParentCreateDTO");
            pkg.when(() -> PackageUtils.join("com.api.generated.model.parent", "ParentDTO")).thenReturn("com.api.generated.model.parent.ParentDTO");
            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.api", packageConfiguration)).thenReturn("com.api.business");
            pkg.when(() -> PackageUtils.join("com.api.business", "ParentBusinessService")).thenReturn("com.api.business.ParentBusinessService");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.api", packageConfiguration)).thenReturn("com.api.entity");
            pkg.when(() -> PackageUtils.join("com.api.entity", "Parent")).thenReturn("com.api.entity.Parent");

            pkg.when(() -> PackageUtils.computeServicePackage("com.api", packageConfiguration)).thenReturn("com.api.service");
            pkg.when(() -> PackageUtils.join("com.api.service", "ParentService")).thenReturn("com.api.service.ParentService");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.api", packageConfiguration)).thenReturn("com.api.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.api.rest.mapper", "ParentRestMapper")).thenReturn("com.api.rest.mapper.ParentRestMapper");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.api", packageConfiguration)).thenReturn("com.api.exception");
            pkg.when(() -> PackageUtils.join("com.api.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.api.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeCreateEndpointTestProjectImports(
                    model, outputDir, true, packageConfiguration, true
            );

            assertTrue(result.contains("import com.api.enums.StatusEnum;"));
            assertTrue(result.contains("import com.api.generated.model.parent.ChildInputDTO;"));
            assertTrue(result.contains("import com.api.generated.model.parent.ParentCreateDTO;"));
            assertTrue(result.contains("import com.api.generated.model.parent.ParentDTO;"));
            assertTrue(result.contains("import com.api.business.ParentBusinessService;"));
            assertTrue(result.contains("import com.api.rest.helper.mapper.ConfigRestMapper;"));
            assertTrue(result.contains("import com.api.entity.Parent;"));
            assertTrue(result.contains("import com.api.service.ParentService;"));
            assertTrue(result.contains("import com.api.rest.mapper.ParentRestMapper;"));
            assertTrue(result.contains("import com.api.exception.GlobalRestExceptionHandler;"));
            assertFalse(result.contains(".rest.to.ParentTO;"));
            assertFalse(result.contains("ParentCreateTO"));

            enumImports.verify(() -> EnumImports.computeEnumImports(model, "com.api", packageConfiguration));
        }
    }

    @Test
    @DisplayName("Swagger enabled, no relations, no JSON, GlobalExceptionHandler=null → no exception handler import; still has create+model+enums")
    void computeCreateEndpointTestProjectImports_swagger_noRelations_noJson_globalNull() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration)).thenReturn(Set.of());

            names.when(() -> ModelNameUtils.computeOpenApiCreateModelName("User")).thenReturn("UserCreateDTO");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("UserDTO");

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.app", packageConfiguration, "user"))
                    .thenReturn("com.app.generated.model.user");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserCreateDTO")).thenReturn("com.app.generated.model.user.UserCreateDTO");
            pkg.when(() -> PackageUtils.join("com.app.generated.model.user", "UserDTO")).thenReturn("com.app.generated.model.user.UserDTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration)).thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User")).thenReturn("com.app.entity.User");
            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration)).thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService")).thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration)).thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper")).thenReturn("com.app.rest.mapper.UserRestMapper");

            final String result = RestControllerImports.computeCreateEndpointTestProjectImports(
                    model, outputDir, true, packageConfiguration, null
            );

            assertTrue(result.contains("import com.app.generated.model.user.UserCreateDTO;"));
            assertTrue(result.contains("import com.app.generated.model.user.UserDTO;"));
            assertFalse(result.contains("GlobalRestExceptionHandler"));

            enumImports.verify(() -> EnumImports.computeEnumImports(model, "com.app", packageConfiguration));
        }
    }

    @Test
    @DisplayName("Swagger enabled, relation exists but NOT in many-to-many/one-to-many → no relation input import from stream; BusinessService still added")
    void computeCreateEndpointTestProjectImports_swagger_relationButNotCollection_noRelationInputFromStream() {

        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("Child");
        relationField.setRelation(new RelationDefinition());

        final ModelDefinition model = new ModelDefinition();
        model.setName("Parent");
        model.setFields(List.of(relationField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.x");
            names.when(() -> ModelNameUtils.stripSuffix("Parent")).thenReturn("Parent");
            fieldUtils.when(() -> FieldUtils.extractManyToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractOneToManyRelations(model.getFields())).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(relationField));
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);
            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.x", packageConfiguration)).thenReturn(Set.of());
            names.when(() -> ModelNameUtils.computeOpenApiCreateModelName("Parent")).thenReturn("ParentCreateDTO");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("Parent")).thenReturn("ParentDTO");
            names.when(() -> ModelNameUtils.computeOpenApiInputModelName("Child")).thenReturn("ChildInputDTO");

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.x", packageConfiguration, "parent"))
                    .thenReturn("com.x.generated.model.parent");
            pkg.when(() -> PackageUtils.join("com.x.generated.model.parent", "ParentCreateDTO"))
                    .thenReturn("com.x.generated.model.parent.ParentCreateDTO");
            pkg.when(() -> PackageUtils.join("com.x.generated.model.parent", "ParentDTO"))
                    .thenReturn("com.x.generated.model.parent.ParentDTO");
            pkg.when(() -> PackageUtils.join("com.x.generated.model.parent", "ChildInputDTO"))
                    .thenReturn("com.x.generated.model.parent.ChildInputDTO");
            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.x", packageConfiguration)).thenReturn("com.x.business");
            pkg.when(() -> PackageUtils.join("com.x.business", "ParentBusinessService"))
                    .thenReturn("com.x.business.ParentBusinessService");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.x", packageConfiguration)).thenReturn("com.x.entity");
            pkg.when(() -> PackageUtils.join("com.x.entity", "Parent")).thenReturn("com.x.entity.Parent");
            pkg.when(() -> PackageUtils.computeServicePackage("com.x", packageConfiguration)).thenReturn("com.x.service");
            pkg.when(() -> PackageUtils.join("com.x.service", "ParentService")).thenReturn("com.x.service.ParentService");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.x", packageConfiguration)).thenReturn("com.x.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.x.rest.mapper", "ParentRestMapper")).thenReturn("com.x.rest.mapper.ParentRestMapper");

            final String result = RestControllerImports.computeCreateEndpointTestProjectImports(
                    model, outputDir, true, packageConfiguration, false
            );

            assertTrue(result.contains("import com.x.business.ParentBusinessService;"));
            assertFalse(result.contains("ChildInputDTO"), "No relation input import expected from stream for non-collection relations");
        }
    }

    
    @Test
    @DisplayName("Short overload delegates to full overload")
    void shortOverloadDelegatesToFullOverload() {
        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

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

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO"))
                    .thenReturn("com.app.rest.to.UserTO");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper"))
                    .thenReturn("com.app.rest.mapper.UserRestMapper");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalRestExceptionHandler");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.to");
            pkg.when(() -> PackageUtils.join("com.app.to", GeneratorConstants.PAGE_TO))
                    .thenReturn("com.app.to.PageTO");

            final String viaShort = RestControllerImports.computeControllerTestProjectImports(
                    model, outputDir, false, RestControllerImports.RestEndpointOperation.GET, packageConfiguration, true
            );

            final String viaFull = RestControllerImports.computeControllerTestProjectImports(
                    model, outputDir, false, RestControllerImports.RestEndpointOperation.GET, null, packageConfiguration, true
            );

            assertEquals(viaFull, viaShort, "Short overload should delegate to full overload with null fieldToBeAdded");
        }
    }

    @Test
    @DisplayName("Non-swagger GET without relations and without global exception handler → entity, service, TO, mapper, ObjectMapper")
    void computeControllerTestProjectImports_nonSwaggerGet_noRelations_noGlobalExceptionHandler() {
        
        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(List.of());
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User"))
                    .thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration))
                    .thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService"))
                    .thenReturn("com.app.service.UserService");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO"))
                    .thenReturn("com.app.rest.to.UserTO");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.to");
            pkg.when(() -> PackageUtils.join("com.app.to", GeneratorConstants.PAGE_TO))
                    .thenReturn("com.app.to.PageTO");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.app", packageConfiguration))
                    .thenReturn("com.app.business");
            pkg.when(() -> PackageUtils.join("com.app.business", "UserBusinessService"))
                    .thenReturn("com.app.business.UserBusinessService");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper"))
                    .thenReturn("com.app.rest.mapper.UserRestMapper");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeControllerTestProjectImports(
                    model,
                    outputDir,
                    false,
                    RestControllerImports.RestEndpointOperation.GET,
                    null,
                    packageConfiguration,
                    false
            );

            assertTrue(result.contains("import com.app.entity.User;"));
            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.rest.to.UserTO;"));
            assertTrue(result.contains("import com.app.rest.mapper.UserRestMapper;"));
            assertTrue(result.contains("import com.app.to.PageTO;"));
            assertFalse(result.contains("generated"));
            assertFalse(result.contains("business"));
        }
    }

    @Test
    @DisplayName("Non-swagger GET with relations → entity, service, TO, PageTO, TypeReference, BusinessService, mapper, ObjectMapper, exception handler")
    void computeControllerTestProjectImports_nonSwaggerGet_withRelations() {
        
        final String outputDir = "/out";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        final FieldDefinition relationField = new FieldDefinition();
        model.setFields(List.of(relationField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.app");
            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(List.of(relationField));
            pkg.when(() -> PackageUtils.computeEntityPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.entity");
            pkg.when(() -> PackageUtils.join("com.app.entity", "User"))
                    .thenReturn("com.app.entity.User");

            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration))
                    .thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService"))
                    .thenReturn("com.app.service.UserService");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.rest.to");
            pkg.when(() -> PackageUtils.join("com.app.rest.to", "UserTO"))
                    .thenReturn("com.app.rest.to.UserTO");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.to");
            pkg.when(() -> PackageUtils.join("com.app.to", GeneratorConstants.PAGE_TO))
                    .thenReturn("com.app.to.PageTO");

            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.app", packageConfiguration))
                    .thenReturn("com.app.business");
            pkg.when(() -> PackageUtils.join("com.app.business", "UserBusinessService"))
                    .thenReturn("com.app.business.UserBusinessService");

            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.app.rest.mapper", "UserRestMapper"))
                    .thenReturn("com.app.rest.mapper.UserRestMapper");

            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeControllerTestProjectImports(
                    model,
                    outputDir,
                    false,
                    RestControllerImports.RestEndpointOperation.GET,
                    null,
                    packageConfiguration,
                    true
            );

            assertTrue(result.contains("import com.app.entity.User;"));
            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.rest.to.UserTO;"));
            assertTrue(result.contains("import com.app.rest.mapper.UserRestMapper;"));
            assertTrue(result.contains("import com.app.exception.GlobalRestExceptionHandler;"));
            assertTrue(result.contains("import com.app.to.PageTO;"));
            assertTrue(result.contains("import com.app.business.UserBusinessService;"));
            assertFalse(result.contains("generated"));
        }
    }

    @Test
    @DisplayName("Swagger GET with relations → entity, service, generated models, Get200Response, BusinessService, mapper, ObjectMapper, exception handler")
    void computeControllerTestProjectImports_swaggerGet_withRelations() {
        
        final String outputDir = "/out/swagger";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        final FieldDefinition relationField = new FieldDefinition();
        model.setFields(List.of(relationField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("Order"))
                    .thenReturn("OrderDTO");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(List.of(relationField));
            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");
            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService"))
                    .thenReturn("com.shop.service.OrderService");
            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.shop", packageConfiguration, "order"))
                    .thenReturn("com.shop.generated.model.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrderDTO"))
                    .thenReturn("com.shop.generated.model.order.OrderDTO");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrdersGet200Response"))
                    .thenReturn("com.shop.generated.model.order.OrdersGet200Response");
            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.business");
            pkg.when(() -> PackageUtils.join("com.shop.business", "OrderBusinessService"))
                    .thenReturn("com.shop.business.OrderBusinessService");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper", "OrderRestMapper"))
                    .thenReturn("com.shop.rest.mapper.OrderRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.join("com.shop.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.shop.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeControllerTestProjectImports(
                    model,
                    outputDir,
                    true,
                    RestControllerImports.RestEndpointOperation.GET,
                    null,
                    packageConfiguration,
                    true
            );

            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.rest.mapper.OrderRestMapper;"));
            assertTrue(result.contains("import com.shop.exception.GlobalRestExceptionHandler;"));
            assertTrue(result.contains("import com.shop.generated.model.order.OrderDTO;"));
            assertTrue(result.contains("import com.shop.generated.model.order.OrdersGet200Response;"));
            assertTrue(result.contains("import com.shop.business.OrderBusinessService;"));
            assertFalse(result.contains(".rest.to.OrderTO;"));
            assertFalse(result.contains("PageTO"));
        }
    }

    @Test
    @DisplayName("Non-swagger ADD_RELATION → InputTO for relation + entity, service, TO, mapper, ObjectMapper, exception handler")
    void computeControllerTestProjectImports_nonSwagger_addRelation() {
        
        final String outputDir = "/out/add";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(Collections.emptyList());

        final FieldDefinition fieldToAdd = new FieldDefinition();
        fieldToAdd.setType("OrderItem");

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.stripSuffix("OrderItem"))
                    .thenReturn("OrderItem");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.rest.to");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderItemInputTO"))
                    .thenReturn("com.shop.rest.to.OrderItemInputTO");
            pkg.when(() -> PackageUtils.join("com.shop.rest.to", "OrderTO"))
                    .thenReturn("com.shop.rest.to.OrderTO");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");
            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService"))
                    .thenReturn("com.shop.service.OrderService");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper", "OrderRestMapper"))
                    .thenReturn("com.shop.rest.mapper.OrderRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.join("com.shop.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.shop.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeControllerTestProjectImports(
                    model,
                    outputDir,
                    false,
                    RestControllerImports.RestEndpointOperation.ADD_RELATION,
                    fieldToAdd,
                    packageConfiguration,
                    true
            );

            assertTrue(result.contains("import com.shop.rest.to.OrderItemInputTO;"));
            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.rest.to.OrderTO;"));
            assertTrue(result.contains("import com.shop.rest.mapper.OrderRestMapper;"));
            assertTrue(result.contains("import com.shop.exception.GlobalRestExceptionHandler;"));
            assertFalse(result.contains("PageTO"));
        }
    }

    @Test
    @DisplayName("Swagger ADD_RELATION → generated Input + generated main model, entity, service, mapper, ObjectMapper, exception handler")
    void computeControllerTestProjectImports_swagger_addRelation() {

        final String outputDir = "/out/swagger-add";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");
        model.setFields(Collections.emptyList());

        final FieldDefinition fieldToAdd = new FieldDefinition();
        fieldToAdd.setType("OrderItem");

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");
            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.stripSuffix("OrderItem"))
                    .thenReturn("OrderItem");
            names.when(() -> ModelNameUtils.computeOpenApiModelName("Order"))
                    .thenReturn("OrderTO");
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.computeGeneratedModelPackage("com.shop", packageConfiguration, "order"))
                    .thenReturn("com.shop.generated.model.order");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrderItemInput"))
                    .thenReturn("com.shop.generated.model.order.OrderItemInput");
            pkg.when(() -> PackageUtils.join("com.shop.generated.model.order", "OrderTO"))
                    .thenReturn("com.shop.generated.model.order.OrderTO");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");
            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService"))
                    .thenReturn("com.shop.service.OrderService");
            pkg.when(() -> PackageUtils.computeRestMapperPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.rest.mapper");
            pkg.when(() -> PackageUtils.join("com.shop.rest.mapper", "OrderRestMapper"))
                    .thenReturn("com.shop.rest.mapper.OrderRestMapper");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.join("com.shop.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.shop.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeControllerTestProjectImports(
                    model,
                    outputDir,
                    true,
                    RestControllerImports.RestEndpointOperation.ADD_RELATION,
                    fieldToAdd,
                    packageConfiguration,
                    true
            );

            assertTrue(result.contains("import com.shop.generated.model.order.OrderItemInput;"));
            assertTrue(result.contains("import com.shop.generated.model.order.OrderTO;"));
            assertTrue(result.contains("import com.shop.entity.Order;"));
            assertTrue(result.contains("import com.shop.service.OrderService;"));
            assertTrue(result.contains("import com.shop.rest.mapper.OrderRestMapper;"));
            assertTrue(result.contains("import com.shop.exception.GlobalRestExceptionHandler;"));
            assertFalse(result.contains(".rest.to.OrderTO;"));
        }
    }

    @Test
    @DisplayName("Non-swagger DELETE → only service + exception handler (no entity, no TO, no mapper, no ObjectMapper)")
    void computeControllerTestProjectImports_nonSwagger_delete() {
        
        final String outputDir = "/out/delete";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.app");

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields()))
                    .thenReturn(Collections.emptyList());
            pkg.when(() -> PackageUtils.computeServicePackage("com.app", packageConfiguration))
                    .thenReturn("com.app.service");
            pkg.when(() -> PackageUtils.join("com.app.service", "UserService"))
                    .thenReturn("com.app.service.UserService");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.app", packageConfiguration))
                    .thenReturn("com.app.exception");
            pkg.when(() -> PackageUtils.join("com.app.exception", GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER))
                    .thenReturn("com.app.exception.GlobalRestExceptionHandler");

            final String result = RestControllerImports.computeControllerTestProjectImports(
                    model,
                    outputDir,
                    false,
                    RestControllerImports.RestEndpointOperation.DELETE,
                    null,
                    packageConfiguration,
                    true
            );

            assertTrue(result.contains("import com.app.service.UserService;"));
            assertTrue(result.contains("import com.app.exception.GlobalRestExceptionHandler;"));
            assertFalse(result.contains("entity.User"));
            assertFalse(result.contains("UserTO"));
            assertFalse(result.contains("RestMapper"));
            assertFalse(result.contains("PageTO"));
        }
    }

}
