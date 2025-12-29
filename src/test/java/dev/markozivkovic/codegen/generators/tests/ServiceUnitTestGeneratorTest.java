package dev.markozivkovic.codegen.generators.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.imports.ServiceImports;
import dev.markozivkovic.codegen.imports.ServiceImports.ServiceImportScope;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

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
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(false);
            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));

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
            imports.when(() -> ServiceImports.computeTestServiceImports(eq(model), eq(List.of()), eq(false)))
                    .thenReturn("import test;");

            dataCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any()))
                    .thenReturn(java.util.Map.of());

            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenReturn("//generated");

            sut.generate(model, "/project/src/main/java/com/acme");

            final String testOut = "/project/src/test/java/com/acme";

            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("service"), eq("CampaignServiceTest"), anyString()));

            ftl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/service/service-test-class-template.ftl"), anyMap()));
        }
    }
}
