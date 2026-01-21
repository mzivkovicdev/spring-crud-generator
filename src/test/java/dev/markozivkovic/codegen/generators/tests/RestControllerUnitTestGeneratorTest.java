package dev.markozivkovic.codegen.generators.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.imports.RestControllerImports;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.ErrorResponse;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.codegen.templates.RestControllerTemplateContext;
import dev.markozivkovic.codegen.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

class RestControllerUnitTestGeneratorTest {

    private CrudConfiguration cfgWithTestsEnabled(boolean swaggerEnabled) {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        final String springBootVersion = "3.0.0";
        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(swaggerEnabled);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(swaggerEnabled);
        when(cfg.getSpringBootVersion()).thenReturn(springBootVersion);
        when(cfg.getErrorResponse()).thenReturn(ErrorResponse.NONE);

        return cfg;
    }

    @Test
    void generate_shouldDoNothing_whenUnitTestsDisabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");

        final RestControllerUnitTestGenerator sut = new RestControllerUnitTestGenerator(cfg, List.of(), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(false);

            sut.generate(model, "/project/src/main/java/com/acme");

            fileWriter.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldDoNothing_whenModelHasNoIdField() {

        final CrudConfiguration cfg = cfgWithTestsEnabled(true);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = mock(ModelDefinition.class);

        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of());

        final RestControllerUnitTestGenerator sut = new RestControllerUnitTestGenerator(cfg, List.of(), pkgCfg);

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
    void generate_shouldWriteCrudEndpointTests_whenIdExists_andNoRelations() {

        final CrudConfiguration cfg = cfgWithTestsEnabled(true);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of(idField));

        final RestControllerUnitTestGenerator sut = new RestControllerUnitTestGenerator(cfg, List.of(), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<RestControllerTemplateContext> templateCtx = mockStatic(RestControllerTemplateContext.class);
             final MockedStatic<RestControllerImports> imports = mockStatic(RestControllerImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dataCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> ftl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(false);
            unitTestUtils.when(() -> UnitTestUtils.computeInvalidIdType(any())).thenReturn("invalid");

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractJsonFields(model.getFields())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(model.getFields())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(model.getFields())).thenReturn(false);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("CampaignModel")).thenReturn("Campaign");

            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(anyString())).thenReturn("com/acme");
            pkgUtils.when(() -> PackageUtils.computeControllerPackage(anyString(), any())).thenReturn("com.acme.controller");
            pkgUtils.when(() -> PackageUtils.computeControllerSubPackage(any())).thenReturn("controller");
            addProps.when(() -> AdditionalPropertiesUtils.resolveBasePath(cfg)).thenReturn("/api");

            templateCtx.when(() -> RestControllerTemplateContext.computeCreateTestEndpointContext(eq(model), eq(List.of())))
                    .thenReturn(new HashMap<>());
            templateCtx.when(() -> RestControllerTemplateContext.computeUpdateByIdTestEndpointContext(
                    eq(model), eq(cfg), eq(pkgCfg), anyBoolean(), anyBoolean(), anyString(), anyString(), anyString()
            )).thenReturn(new HashMap<>());

            dataCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());

            imports.when(() -> RestControllerImports.computeGetEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// GET IMPORTS");
            imports.when(() -> RestControllerImports.computeUpdateEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// UPDATE IMPORTS");
            imports.when(() -> RestControllerImports.computeDeleteEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// DELETE IMPORTS");
            imports.when(() -> RestControllerImports.computeControllerTestProjectImports(any(), anyString(), anyBoolean(), any(), any(), anyBoolean()))
                    .thenReturn("// CONTROLLER TEST IMPORTS");
            imports.when(() -> RestControllerImports.computeCreateEndpointTestProjectImports(any(), anyString(), anyBoolean(), any(), anyBoolean()))
                    .thenReturn("// CREATE IMPORTS");
            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenReturn("//generated");

            sut.generate(model, "/project/src/main/java/com/acme");

            final String testOut = "/project/src/test/java/com/acme";

            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignGetMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignDeleteByIdMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignUpdateByIdMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignCreateMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), startsWith("CampaignAdd"), anyString()), never());
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), startsWith("CampaignRemove"), anyString()), never());
        }
    }

    @Test
    void generate_shouldAlsoWriteRelationEndpointTests_whenRelationsExist() {

        final CrudConfiguration cfg = cfgWithTestsEnabled(true);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");

        final FieldDefinition rel = mock(FieldDefinition.class);
        when(rel.getType()).thenReturn("UserModel");
        when(rel.getName()).thenReturn("owners");

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of(idField, rel));

        final ModelDefinition related = mock(ModelDefinition.class);
        when(related.getName()).thenReturn("UserModel");

        final FieldDefinition relatedId = mock(FieldDefinition.class);
        when(relatedId.getType()).thenReturn("UUID");
        when(relatedId.getName()).thenReturn("userId");
        when(related.getFields()).thenReturn(List.of(relatedId));

        final RestControllerUnitTestGenerator sut = new RestControllerUnitTestGenerator(cfg, List.of(related), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<RestControllerTemplateContext> templateCtx = mockStatic(RestControllerTemplateContext.class);
             final MockedStatic<RestControllerImports> imports = mockStatic(RestControllerImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dataCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> ftl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(false);
            unitTestUtils.when(() -> UnitTestUtils.computeInvalidIdType(any())).thenReturn("invalid");

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of(rel));
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(model)).thenReturn(List.of("owners"));
            fieldUtils.when(() -> FieldUtils.extractIdField(related.getFields())).thenReturn(relatedId);

            fieldUtils.when(() -> FieldUtils.extractJsonFields(model.getFields())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(model.getFields())).thenReturn(false);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("CampaignModel")).thenReturn("Campaign");
            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("UserModel")).thenReturn("User");

            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(anyString())).thenReturn("com/acme");
            pkgUtils.when(() -> PackageUtils.computeControllerPackage(anyString(), any())).thenReturn("com.acme.controller");
            pkgUtils.when(() -> PackageUtils.computeControllerSubPackage(any())).thenReturn("controller");

            addProps.when(() -> AdditionalPropertiesUtils.resolveBasePath(cfg)).thenReturn("/api");

            templateCtx.when(() -> RestControllerTemplateContext.computeCreateTestEndpointContext(eq(model), eq(List.of(related))))
                    .thenReturn(new HashMap<>());
            templateCtx.when(() -> RestControllerTemplateContext.computeUpdateByIdTestEndpointContext(
                    eq(model), eq(cfg), eq(pkgCfg), anyBoolean(), anyBoolean(), anyString(), anyString(), anyString()
            )).thenReturn(new HashMap<>());

            dataCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());

            imports.when(() -> RestControllerImports.computeGetEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// GET IMPORTS");
            imports.when(() -> RestControllerImports.computeUpdateEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// UPDATE IMPORTS");
            imports.when(() -> RestControllerImports.computeDeleteEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// DELETE IMPORTS");
            imports.when(() -> RestControllerImports.computeAddRelationEndpointBaseImports(model)).thenReturn("// ADD BASE IMPORTS");
            imports.when(() -> RestControllerImports.computeRemoveRelationEndpointBaseImports(model, List.of(related))).thenReturn("// REMOVE BASE IMPORTS");
            imports.when(() -> RestControllerImports.computeControllerTestProjectImports(any(), anyString(), anyBoolean(), any(), any(), anyBoolean()))
                    .thenReturn("// CONTROLLER IMPORTS");
            imports.when(() -> RestControllerImports.computeControllerTestProjectImports(any(), anyString(), anyBoolean(), any(), any(), any(), anyBoolean()))
                    .thenReturn("// CONTROLLER IMPORTS (REL)");
            imports.when(() -> RestControllerImports.computeCreateEndpointTestProjectImports(any(), anyString(), anyBoolean(), any(), anyBoolean()))
                    .thenReturn("// CREATE IMPORTS");
            imports.when(() -> RestControllerImports.computeAddRelationEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// ADD TEST IMPORTS");
            imports.when(() -> RestControllerImports.computeDeleteEndpointTestImports(false, cfg.getSpringBootVersion()))
                    .thenReturn("// DELETE TEST IMPORTS");

            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenReturn("//generated");

            sut.generate(model, "/project/src/main/java/com/acme");

            final String testOut = "/project/src/test/java/com/acme";

            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignGetMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignDeleteByIdMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignUpdateByIdMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignCreateMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignAddUserMockMvcTest"), anyString()));
            fileWriter.verify(() -> FileWriterUtils.writeToFile(eq(testOut), eq("controller"), eq("CampaignRemoveUserMockMvcTest"), anyString()));
        }
    }

    @Test
    void generate_shouldCallNewTemplateContextEntryPoints_forCreateAndUpdate() {
        final CrudConfiguration cfg = cfgWithTestsEnabled(true);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("CampaignModel");
        when(model.getFields()).thenReturn(List.of(idField));

        final RestControllerUnitTestGenerator sut = new RestControllerUnitTestGenerator(cfg, List.of(), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitTestUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<RestControllerTemplateContext> templateCtx = mockStatic(RestControllerTemplateContext.class);
             final MockedStatic<RestControllerImports> imports = mockStatic(RestControllerImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dataCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> ftl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> fileWriter = mockStatic(FileWriterUtils.class)) {

            unitTestUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitTestUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            unitTestUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(model.getFields())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractJsonFields(model.getFields())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(model.getFields())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(model.getFields())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("CampaignModel")).thenReturn("Campaign");

            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(anyString())).thenReturn("com/acme");
            pkgUtils.when(() -> PackageUtils.computeControllerPackage(anyString(), any())).thenReturn("com.acme.controller");
            pkgUtils.when(() -> PackageUtils.computeControllerSubPackage(any())).thenReturn("controller");

            addProps.when(() -> AdditionalPropertiesUtils.resolveBasePath(cfg)).thenReturn("/api");

            templateCtx.when(() -> RestControllerTemplateContext.computeCreateTestEndpointContext(eq(model), eq(List.of())))
                    .thenReturn(new HashMap<>());
            templateCtx.when(() -> RestControllerTemplateContext.computeUpdateByIdTestEndpointContext(
                    eq(model), eq(cfg), eq(pkgCfg), anyBoolean(), anyBoolean(), anyString(), anyString(), anyString()
            )).thenReturn(new HashMap<>());

            dataCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());

            imports.when(() -> RestControllerImports.computeGetEndpointTestImports(false, cfg.getSpringBootVersion())).thenReturn("//");
            imports.when(() -> RestControllerImports.computeUpdateEndpointTestImports(false, cfg.getSpringBootVersion())).thenReturn("//");
            imports.when(() -> RestControllerImports.computeDeleteEndpointTestImports(false, cfg.getSpringBootVersion())).thenReturn("//");
            imports.when(() -> RestControllerImports.computeControllerTestProjectImports(any(), anyString(), anyBoolean(), any(), any(), anyBoolean()))
                    .thenReturn("//");
            imports.when(() -> RestControllerImports.computeCreateEndpointTestProjectImports(any(), anyString(), anyBoolean(), any(), anyBoolean()))
                    .thenReturn("//");

            ftl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("//generated");

            sut.generate(model, "/project/src/main/java/com/acme");

            templateCtx.verify(() -> RestControllerTemplateContext.computeCreateTestEndpointContext(eq(model), eq(List.of())), times(1));
            templateCtx.verify(() -> RestControllerTemplateContext.computeUpdateByIdTestEndpointContext(
                    eq(model), eq(cfg), eq(pkgCfg), anyBoolean(), anyBoolean(), anyString(), anyString(), anyString()
            ), times(1));
        }
    }
}
