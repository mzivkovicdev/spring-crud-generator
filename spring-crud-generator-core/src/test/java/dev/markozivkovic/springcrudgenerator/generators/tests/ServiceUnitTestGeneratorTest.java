package dev.markozivkovic.springcrudgenerator.generators.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.ServiceImports;
import dev.markozivkovic.springcrudgenerator.imports.ServiceImports.ServiceImportScope;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.springcrudgenerator.templates.ServiceTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

class ServiceUnitTestGeneratorTest {

    private CrudConfiguration cfgWithTestsEnabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        return cfg;
    }

    @Test
    void generate_shouldDoNothing_whenUnitTestsDisabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");

        final ServiceUnitTestGenerator sut = new ServiceUnitTestGenerator(cfg, List.of(), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(false);

            sut.generate(model, "/project/src/main/java/com/acme");

            fileWriter.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldDoNothing_whenModelHasNoIdField() {

        final CrudConfiguration cfg = cfgWithTestsEnabled();
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = mock(ModelDefinition.class);

        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of());

        final ServiceUnitTestGenerator sut = new ServiceUnitTestGenerator(cfg, List.of(), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(false);

            sut.generate(model, "/project/src/main/java/com/acme");

            fileWriter.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldWriteServiceTestFile_whenUnitTestsEnabled_andIdExists() {

        final CrudConfiguration cfg = cfgWithTestsEnabled();
        when(cfg.getSpringBootVersion()).thenReturn("3.1.0");

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition idField = mock(FieldDefinition.class);

        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of(idField));

        final ServiceUnitTestGenerator sut = new ServiceUnitTestGenerator(cfg, List.of(), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<ServiceImports> imports = mockStatic(ServiceImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dataCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> ftl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(false);
            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.1.0")).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields())).thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractFieldNamesWithoutRelations(model.getFields())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(model)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(model.getFields())).thenReturn(List.of("name"));

            fieldUtils.when(() -> FieldUtils.hasRelation(model, List.of())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasCollectionRelation(model, List.of())).thenReturn(false);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("CampaignModel")).thenReturn("Campaign");
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(anyString())).thenReturn("com/acme");
            pkgUtils.when(() -> PackageUtils.computeServicePackage("com/acme", pkgCfg)).thenReturn("com.acme.service");
            pkgUtils.when(() -> PackageUtils.computeServiceSubPackage(pkgCfg)).thenReturn("service");

            imports.when(() -> ServiceImports.getTestBaseImport(model)).thenReturn("import base;");
            imports.when(() -> ServiceImports.computeModelsEnumsAndRepositoryImports(
                    eq(model), eq("/project/src/main/java/com/acme"), eq(ServiceImportScope.SERVICE_TEST), eq(pkgCfg)
            )).thenReturn("import project;");
            imports.when(() -> ServiceImports.computeTestServiceImports(eq(model), eq(List.of()), eq(false), eq(true)))
                    .thenReturn("import test;");

            dataCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any()))
                    .thenReturn(java.util.Map.of());

            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenReturn("//generated");

            sut.generate(model, "/project/src/main/java/com/acme");

            final String testOut = "/project/src/test/java/com/acme";

            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("service"), eq("CampaignServiceTest"), anyString()));

            ftl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/service-test-class-template.ftl"), anyMap()));
            ftl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("test/unit/service/service-test-class-template.ftl"),
                    argThat(ctx -> {
                        Map<String, Object> map = (Map<String, Object>) ctx;
                        return Boolean.TRUE.equals(map.get(TemplateContextConstants.IS_SPRING_BOOT_3));
                    })
            ));
        }
    }

    @Test
    void generate_shouldSetMongoSoftDeleteTrueInMethodContexts_whenMongoAndSoftDeleteEnabled() {

        final CrudConfiguration cfg = cfgWithTestsEnabled();
        when(cfg.getDatabase()).thenReturn(DatabaseType.MONGODB);
        when(cfg.getSpringBootVersion()).thenReturn("3.1.0");

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("String");
        when(idField.getName()).thenReturn("id");

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of(idField));
        when(model.getSoftDelete()).thenReturn(Boolean.TRUE);

        final List<ModelDefinition> entities = List.of(model);
        final ServiceUnitTestGenerator sut = new ServiceUnitTestGenerator(cfg, entities, pkgCfg);

        final AtomicReference<Map<String, Object>> getByIdCtxRef = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> getAllCtxRef = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> deleteCtxRef = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> updateCtxRef = new AtomicReference<>();

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<ServiceImports> imports = mockStatic(ServiceImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dataCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<ServiceTemplateContext> serviceCtx = mockStatic(ServiceTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> ftl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(false);
            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.1.0")).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractFieldNamesWithoutRelations(model.getFields())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(model)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(model.getFields())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.hasCollectionRelation(model, entities)).thenReturn(false);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("CampaignModel")).thenReturn("Campaign");
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(anyString())).thenReturn("com/acme");
            pkgUtils.when(() -> PackageUtils.computeServicePackage("com/acme", pkgCfg)).thenReturn("com.acme.service");
            pkgUtils.when(() -> PackageUtils.computeServiceSubPackage(pkgCfg)).thenReturn("service");

            imports.when(() -> ServiceImports.getTestBaseImport(model)).thenReturn("import base;");
            imports.when(() -> ServiceImports.computeModelsEnumsAndRepositoryImports(
                    eq(model), eq("/project/src/main/java/com/acme"), eq(ServiceImportScope.SERVICE_TEST), eq(pkgCfg)
            )).thenReturn("import project;");
            imports.when(() -> ServiceImports.computeTestServiceImports(eq(model), eq(entities), eq(false), eq(true)))
                    .thenReturn("import test;");

            dataCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());

            serviceCtx.when(() -> ServiceTemplateContext.computeGetByIdContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeGetAllContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeDeleteByIdContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeCreateContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeUpdateByIdContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.createAddRelationMethodContext(model)).thenReturn(Map.of());
            serviceCtx.when(() -> ServiceTemplateContext.createRemoveRelationMethodContext(model, entities)).thenReturn(Map.of());

            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/get-by-id.ftl"), anyMap()))
                    .thenAnswer(inv -> { getByIdCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/get-all.ftl"), anyMap()))
                    .thenAnswer(inv -> { getAllCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/delete-by-id.ftl"), anyMap()))
                    .thenAnswer(inv -> { deleteCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/create.ftl"), anyMap())).thenReturn("");
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/update-by-id.ftl"), anyMap()))
                    .thenAnswer(inv -> { updateCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/service-test-class-template.ftl"), anyMap())).thenReturn("");
            fileWriter.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            sut.generate(model, "/project/src/main/java/com/acme");
        }

        assertEquals(true, getByIdCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
        assertEquals(true, getAllCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
        assertEquals(true, deleteCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
        assertEquals(true, updateCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
    }

    @Test
    void generate_shouldSetMongoSoftDeleteFalseInMethodContexts_whenSqlDatabase() {

        final CrudConfiguration cfg = cfgWithTestsEnabled();
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getSpringBootVersion()).thenReturn("3.1.0");

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of(idField));
        when(model.getSoftDelete()).thenReturn(Boolean.TRUE);

        final List<ModelDefinition> entities = List.of(model);
        final ServiceUnitTestGenerator sut = new ServiceUnitTestGenerator(cfg, entities, pkgCfg);

        final AtomicReference<Map<String, Object>> getByIdCtxRef = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> getAllCtxRef = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> deleteCtxRef = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> updateCtxRef = new AtomicReference<>();

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<ServiceImports> imports = mockStatic(ServiceImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dataCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<ServiceTemplateContext> serviceCtx = mockStatic(ServiceTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> ftl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(false);
            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.1.0")).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractFieldNamesWithoutRelations(model.getFields())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(model)).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNames(model.getFields())).thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.hasCollectionRelation(model, entities)).thenReturn(false);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("CampaignModel")).thenReturn("Campaign");
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(anyString())).thenReturn("com/acme");
            pkgUtils.when(() -> PackageUtils.computeServicePackage("com/acme", pkgCfg)).thenReturn("com.acme.service");
            pkgUtils.when(() -> PackageUtils.computeServiceSubPackage(pkgCfg)).thenReturn("service");

            imports.when(() -> ServiceImports.getTestBaseImport(model)).thenReturn("import base;");
            imports.when(() -> ServiceImports.computeModelsEnumsAndRepositoryImports(
                    eq(model), eq("/project/src/main/java/com/acme"), eq(ServiceImportScope.SERVICE_TEST), eq(pkgCfg)
            )).thenReturn("import project;");
            imports.when(() -> ServiceImports.computeTestServiceImports(eq(model), eq(entities), eq(false), eq(true)))
                    .thenReturn("import test;");

            dataCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());

            serviceCtx.when(() -> ServiceTemplateContext.computeGetByIdContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeGetAllContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeDeleteByIdContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeCreateContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.computeUpdateByIdContext(model)).thenReturn(new HashMap<>());
            serviceCtx.when(() -> ServiceTemplateContext.createAddRelationMethodContext(model)).thenReturn(Map.of());
            serviceCtx.when(() -> ServiceTemplateContext.createRemoveRelationMethodContext(model, entities)).thenReturn(Map.of());

            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/get-by-id.ftl"), anyMap()))
                    .thenAnswer(inv -> { getByIdCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/get-all.ftl"), anyMap()))
                    .thenAnswer(inv -> { getAllCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/delete-by-id.ftl"), anyMap()))
                    .thenAnswer(inv -> { deleteCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/create.ftl"), anyMap())).thenReturn("");
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/method/update-by-id.ftl"), anyMap()))
                    .thenAnswer(inv -> { updateCtxRef.set(inv.getArgument(1, Map.class)); return ""; });
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/service-test-class-template.ftl"), anyMap())).thenReturn("");
            fileWriter.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            sut.generate(model, "/project/src/main/java/com/acme");
        }

        assertEquals(false, getByIdCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
        assertEquals(false, getAllCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
        assertEquals(false, deleteCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
        assertEquals(false, updateCtxRef.get().get(TemplateContextConstants.SOFT_DELETE_ENABLED));
    }
}
