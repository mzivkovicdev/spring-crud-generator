package com.markozivkovic.codegen.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants.GeneratorContextKeys;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.imports.common.ImportCommon;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.models.RelationDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

class ServiceImportsTest {

    @Test
    @DisplayName("getBaseImport: should return empty string when no special types and no lists and importList=false")
    void getBaseImport_noTypesNoLists_noImportListFlag_returnsEmpty() {

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);
            importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                .thenAnswer(inv -> null);

            final String result = ServiceImports.getBaseImport(model, false);

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("getBaseImport: should import BigDecimal, UUID and List when detected by FieldUtils")
    void getBaseImport_importsBasedOnFieldUtils() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getFields()).thenReturn(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = ServiceImports.getBaseImport(model, false);

            assertTrue(result.contains("import " + ImportConstants.Java.BIG_DECIMAL), "Expected BigDecimal import");
            assertTrue(result.contains("import " + ImportConstants.Java.UUID), "Expected UUID import");
            assertTrue(result.contains("import " + ImportConstants.Java.LIST), "Expected List import");
            assertTrue(result.endsWith("\n"));

            final String needle = "import " + ImportConstants.Java.LIST + ";";
            assertEquals(result.indexOf(needle), result.lastIndexOf(needle), "List import should not be duplicated");
        }
    }

    @Test
    @DisplayName("getBaseImport: should import only List when importList=true and no relations")
    void getBaseImport_importListFlagForcesListImport() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = ServiceImports.getBaseImport(model, true);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST), "Expected List import");
            assertFalse(result.contains("import " + ImportConstants.Java.SET + ";"), "Did not expect Set import");
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport: should import List+Set when model has simple element collections (via ImportCommon helper)")
    void getBaseImport_shouldImportListAndSet_forSimpleCollections() {
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getFields()).thenReturn(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {

                fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

                importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                        .thenAnswer(inv -> {
                                @SuppressWarnings("unchecked")
                                final Set<String> imports = (Set<String>) inv.getArgument(1);
                                imports.add(ImportConstants.Java.LIST);
                                imports.add(ImportConstants.Java.SET);
                                return null;
                        });

                final String result = ServiceImports.getBaseImport(model, false);

                assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import");
                assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import");
                assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport: should not duplicate List if ImportCommon adds LIST and hasLists/importList also add LIST")
    void getBaseImport_shouldNotDuplicateList_whenHelperAddsListAndHasListsTrue() {

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getFields()).thenReturn(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {

                fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);

                fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(true);
                fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

                importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                        .thenAnswer(inv -> {
                                @SuppressWarnings("unchecked")
                                final Set<String> imports = (Set<String>) inv.getArgument(1);
                                imports.add(ImportConstants.Java.LIST);
                                return null;
                        });

                final String result = ServiceImports.getBaseImport(model, false);

                final String needle = "import " + ImportConstants.Java.LIST + ";";
                assertTrue(result.contains(needle), "Expected List import");
                assertEquals(result.indexOf(needle), result.lastIndexOf(needle), "List import should not be duplicated");
        }
    }

    @Test
    @DisplayName("computeJpaServiceBaseImport: cache=false and retryable annotation NOT generated → includes @Transactional, no cache imports")
    void computeJpaServiceBaseImport_noCache_retryNotGenerated_includesTransactional() {
        
        try (final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(false);

            final String result = ServiceImports.computeJpaServiceBaseImport(false);

            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER), "LOGGER import missing");
            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER_FACTORY), "LOGGER_FACTORY import missing");
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE), "PAGE import missing");
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_REQUEST), "PAGE_REQUEST import missing");
            assertTrue(result.contains("import " + ImportConstants.SpringStereotype.SERVICE), "SERVICE import missing");
            assertTrue(result.contains("import " + ImportConstants.SpringTransaction.TRANSACTIONAL),
                    "@Transactional import should be present");

            assertFalse(result.contains(ImportConstants.SpringCache.CACHEABLE));
            assertFalse(result.contains(ImportConstants.SpringCache.CACHE_EVICT));
            assertFalse(result.contains(ImportConstants.SpringCache.CACHE_PUT));
        }
    }

    @Test
    @DisplayName("computeJpaServiceBaseImport: cache=true and retryable annotation NOT generated → includes @Transactional and cache imports")
    void computeJpaServiceBaseImport_cacheEnabled_retryNotGenerated_includesTransactionalAndCacheImports() {
        
        try (final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(false);

            final String result = ServiceImports.computeJpaServiceBaseImport(true);

            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER));
            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER_FACTORY));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_REQUEST));
            assertTrue(result.contains("import " + ImportConstants.SpringStereotype.SERVICE));
            assertTrue(result.contains("import " + ImportConstants.SpringTransaction.TRANSACTIONAL));
            assertTrue(result.contains("import " + ImportConstants.SpringCache.CACHEABLE));
            assertTrue(result.contains("import " + ImportConstants.SpringCache.CACHE_EVICT));
            assertTrue(result.contains("import " + ImportConstants.SpringCache.CACHE_PUT));
        }
    }

    @Test
    @DisplayName("computeJpaServiceBaseImport: cache=true and retryable annotation IS generated → no @Transactional, but has cache imports")
    void computeJpaServiceBaseImport_cacheEnabled_retryGenerated_noTransactionalButCacheImports() {
        
        try (final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(true);

            final String result = ServiceImports.computeJpaServiceBaseImport(true);

            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER));
            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER_FACTORY));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_REQUEST));
            assertTrue(result.contains("import " + ImportConstants.SpringStereotype.SERVICE));

            assertFalse(result.contains("import " + ImportConstants.SpringTransaction.TRANSACTIONAL),
                    "@Transactional import should NOT be present when retryable annotation is generated");

            assertTrue(result.contains("import " + ImportConstants.SpringCache.CACHEABLE));
            assertTrue(result.contains("import " + ImportConstants.SpringCache.CACHE_EVICT));
            assertTrue(result.contains("import " + ImportConstants.SpringCache.CACHE_PUT));
        }
    }

    @Test
    @DisplayName("computeJpaServiceBaseImport: cache=false and retryable annotation IS generated → no @Transactional and no cache imports")
    void computeJpaServiceBaseImport_noCache_retryGenerated_noTransactionalNoCacheImports() {
        
        try (final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(true);

            final String result = ServiceImports.computeJpaServiceBaseImport(false);

            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER));
            assertTrue(result.contains("import " + ImportConstants.Logger.LOGGER_FACTORY));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_REQUEST));
            assertTrue(result.contains("import " + ImportConstants.SpringStereotype.SERVICE));
            assertFalse(result.contains("import " + ImportConstants.SpringTransaction.TRANSACTIONAL));
            assertFalse(result.contains(ImportConstants.SpringCache.CACHEABLE));
            assertFalse(result.contains(ImportConstants.SpringCache.CACHE_EVICT));
            assertFalse(result.contains(ImportConstants.SpringCache.CACHE_PUT));
        }
    }

    @Test
    @DisplayName("computeModelsEnumsAndRepositoryImports: SERVICE scope, with relation and retryable → enums + entity + repo + exceptions + annotation + relation entity")
    void computeModelsEnumsAndRepositoryImports_serviceScope_withRelationAndRetryable() {
        
        final String outputDir = "/some/output/dir";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getName()).thenReturn("Order");

        final FieldDefinition relationField = Mockito.mock(FieldDefinition.class);
        Mockito.when(relationField.getRelation()).thenReturn(new RelationDefinition());
        Mockito.when(relationField.getType()).thenReturn("Customer");

        Mockito.when(model.getFields()).thenReturn(List.of(relationField));

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<ModelImports> modelImports = Mockito.mockStatic(ModelImports.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class);
             final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.computeRepositoryPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.repository");
            pkg.when(() -> PackageUtils.computeExceptionPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.exception");
            pkg.when(() -> PackageUtils.computeAnnotationPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.annotation");

            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");
            pkg.when(() -> PackageUtils.join("com.shop.repository", "OrderRepository"))
                    .thenReturn("com.shop.repository.OrderRepository");
            pkg.when(() -> PackageUtils.join("com.shop.exception", "ResourceNotFoundException"))
                    .thenReturn("com.shop.exception.ResourceNotFoundException");
            pkg.when(() -> PackageUtils.join("com.shop.exception", "InvalidResourceStateException"))
                    .thenReturn("com.shop.exception.InvalidResourceStateException");
            pkg.when(() -> PackageUtils.join("com.shop.entity", "Customer"))
                    .thenReturn("com.shop.entity.Customer");
            pkg.when(() -> PackageUtils.join("com.shop.annotation", GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY))
                    .thenReturn("com.shop.annotation." + GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY);

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");

            modelImports.when(() ->
                            ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, packageConfiguration))
                    .thenReturn("import com.shop.common.OrderEnums;\n");

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(true);

            enumImports.when(() ->
                            EnumImports.computeEnumImports(model, "com.shop", packageConfiguration))
                    .thenReturn(Set.of());

            final String result = ServiceImports.computeModelsEnumsAndRepositoryImports(
                    model,
                    outputDir,
                    ServiceImports.ServiceImportScope.SERVICE,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.shop.common.OrderEnums;"), "Enums import");
            assertTrue(result.contains("import com.shop.entity.Order;"), "Order entity import");
            assertTrue(result.contains("import com.shop.repository.OrderRepository;"), "OrderRepository import");
            assertTrue(result.contains("import com.shop.exception.ResourceNotFoundException;"), "ResourceNotFoundException import");
            assertTrue(result.contains("import com.shop.exception.InvalidResourceStateException;"), "InvalidResourceStateException import");
            assertTrue(result.contains("import com.shop.entity.Customer;"), "Relation entity import");
            assertTrue(result.contains("import com.shop.annotation." + GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY + ";"),
                    "Retryable annotation import");
        }
    }

    @Test
    @DisplayName("computeModelsEnumsAndRepositoryImports: SERVICE_TEST scope, no relations, retryable ignored")
    void computeModelsEnumsAndRepositoryImports_serviceTestScope_noRelations() {
        
        final String outputDir = "/test/output";
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getName()).thenReturn("User");
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<ModelImports> modelImports = Mockito.mockStatic(ModelImports.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class);
             final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.entity");
            pkg.when(() -> PackageUtils.computeRepositoryPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.repository");
            pkg.when(() -> PackageUtils.computeExceptionPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception");
            pkg.when(() -> PackageUtils.computeAnnotationPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.annotation");

            pkg.when(() -> PackageUtils.join("com.example.entity", "User"))
                    .thenReturn("com.example.entity.User");
            pkg.when(() -> PackageUtils.join("com.example.repository", "UserRepository"))
                    .thenReturn("com.example.repository.UserRepository");
            pkg.when(() -> PackageUtils.join("com.example.exception", "ResourceNotFoundException"))
                    .thenReturn("com.example.exception.ResourceNotFoundException");
            pkg.when(() -> PackageUtils.join("com.example.exception", "InvalidResourceStateException"))
                    .thenReturn("com.example.exception.InvalidResourceStateException");
            pkg.when(() -> PackageUtils.join("com.example.annotation", GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY))
                    .thenReturn("com.example.annotation." + GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY);

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            enumImports.when(() ->
                            EnumImports.computeEnumImports(model, "com.example", packageConfiguration))
                    .thenReturn(Set.of("import com.example.enums.UserStatusEnum;"));

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(true);

            modelImports.when(() ->
                            ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, packageConfiguration))
                    .thenReturn("SHOULD_NOT_BE_USED");

            final String result = ServiceImports.computeModelsEnumsAndRepositoryImports(
                    model,
                    outputDir,
                    ServiceImports.ServiceImportScope.SERVICE_TEST,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.example.entity.User;"));
            assertTrue(result.contains("import com.example.repository.UserRepository;"));
            assertTrue(result.contains("import com.example.exception.ResourceNotFoundException;"));
            assertFalse(result.contains("InvalidResourceStateException"),
                    "No relations => InvalidResourceStateException should NOT be imported");
            assertFalse(result.contains(GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY),
                    "SERVICE_TEST scope => retry annotation should NOT be imported");
        }
    }

    @Test
    @DisplayName("computeTestServiceImports: Instancio disabled → only base imports")
    void computeTestServiceImports_noInstancio() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(anyList()))
                    .thenReturn(false);

            final String result = ServiceImports.computeTestServiceImports(
                    model,
                    Collections.emptyList(),
                    false
            );

            assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH));
            assertTrue(result.contains("import " + ImportConstants.JUnit.BEFORE_EACH));
            assertTrue(result.contains("import " + ImportConstants.JUnit.TEST));
            assertTrue(result.contains("import " + ImportConstants.JUnit.EXTEND_WITH));
            assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_IMPL));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_REQUEST));
            assertTrue(result.contains("import " + ImportConstants.SpringTest.SPRING_EXTENSION));

            assertFalse(result.contains(ImportConstants.INSTANCIO.INSTANCIO));
        }
    }

    @Test
    @DisplayName("computeTestServiceImports: Instancio enabled")
    void computeTestServiceImports_instancioEnabled() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        Mockito.when(model.getFields()).thenReturn(List.of(Mockito.mock(FieldDefinition.class)));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(anyList()))
                    .thenReturn(true);

            final String result = ServiceImports.computeTestServiceImports(
                    model,
                    Collections.emptyList(),
                    true
            );

            assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH));
            assertTrue(result.contains("import " + ImportConstants.JUnit.BEFORE_EACH));
            assertTrue(result.contains("import " + ImportConstants.JUnit.TEST));
            assertTrue(result.contains("import " + ImportConstants.JUnit.EXTEND_WITH));
            assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_IMPL));
            assertTrue(result.contains("import " + ImportConstants.SpringData.PAGE_REQUEST));
            assertTrue(result.contains("import " + ImportConstants.SpringTest.SPRING_EXTENSION));

            assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: non-UUID id field → Optional + List, no UUID")
    void getTestBaseImport_nonUuidIdField() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(Mockito.mock(FieldDefinition.class));
        Mockito.when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = Mockito.mock(FieldDefinition.class);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            final String result = ServiceImports.getTestBaseImport(model);

            assertTrue(result.contains("import " + ImportConstants.Java.OPTIONAL), "Optional import missing");
            assertTrue(result.contains("import " + ImportConstants.Java.LIST), "List import missing");
            assertFalse(result.contains(ImportConstants.Java.UUID), "UUID import should NOT be present");
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: UUID id field → Optional + List + UUID")
    void getTestBaseImport_uuidIdField() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(Mockito.mock(FieldDefinition.class));
        Mockito.when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = Mockito.mock(FieldDefinition.class);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(true);

            final String result = ServiceImports.getTestBaseImport(model);

            assertTrue(result.contains("import " + ImportConstants.Java.OPTIONAL));
            assertTrue(result.contains("import " + ImportConstants.Java.LIST));
            assertTrue(result.contains("import " + ImportConstants.Java.UUID));
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: should include Set when ImportCommon adds it for simple collections")
    void getTestBaseImport_shouldIncludeSet_whenSimpleCollectionsExist() {

        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(Mockito.mock(FieldDefinition.class));
        Mockito.when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = Mockito.mock(FieldDefinition.class);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {

                fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
                fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);

                importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                        .thenAnswer(inv -> {
                                final boolean cond = inv.getArgument(0);

                                final Set<String> set = inv.getArgument(1);
                                final String value = inv.getArgument(2);
                                if (cond) set.add(value);
                                return null;
                        });

                importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                        .thenAnswer(inv -> {
                                final Set<String> set = inv.getArgument(1);
                                set.add(ImportConstants.Java.SET);
                                return null;
                        });

                final String result = ServiceImports.getTestBaseImport(model);

                assertTrue(result.contains("import " + ImportConstants.Java.OPTIONAL + ";"));
                assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"));
                assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"));
                assertFalse(result.contains("import " + ImportConstants.Java.UUID + ";"));
                assertTrue(result.endsWith("\n"));
        }
    }

}
