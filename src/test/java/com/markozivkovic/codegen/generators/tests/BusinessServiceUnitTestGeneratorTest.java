package com.markozivkovic.codegen.generators.tests;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.imports.BusinessServiceImports;
import com.markozivkovic.codegen.imports.BusinessServiceImports.BusinessServiceImportScope;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.models.RelationDefinition;
import com.markozivkovic.codegen.templates.BusinessServiceTemplateContext;
import com.markozivkovic.codegen.templates.DataGeneratorTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

class BusinessServiceUnitTestGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenUnitTestsDisabled() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final List<ModelDefinition> allEntities = List.of();
        final BusinessServiceUnitTestGenerator gen =
                new BusinessServiceUnitTestGenerator(cfg, allEntities, pkgCfg);

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("UserEntity", List.of(f1));

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<BusinessServiceImports> bsImports = mockStatic(BusinessServiceImports.class);
             final MockedStatic<BusinessServiceTemplateContext> bsCtx = mockStatic(BusinessServiceTemplateContext.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                     .thenReturn(false);

            gen.generate(model, "src/main/java");

            unitUtils.verify(() -> UnitTestUtils.isUnitTestsEnabled(cfg));
            fieldUtils.verifyNoInteractions();
            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            bsImports.verifyNoInteractions();
            bsCtx.verifyNoInteractions();
            dgCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenModelHasNoIdField() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final List<ModelDefinition> allEntities = List.of();
        final BusinessServiceUnitTestGenerator gen =
                new BusinessServiceUnitTestGenerator(cfg, allEntities, pkgCfg);

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("UserEntity", List.of(f1));

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                     .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                      .thenReturn(false);

            gen.generate(model, "src/main/java");

            unitUtils.verify(() -> UnitTestUtils.isUnitTestsEnabled(cfg));
            fieldUtils.verify(() -> FieldUtils.isAnyFieldId(model.getFields()));
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenModelHasNoRelations() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final List<ModelDefinition> allEntities = List.of();
        final BusinessServiceUnitTestGenerator gen =
                new BusinessServiceUnitTestGenerator(cfg, allEntities, pkgCfg);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("UserEntity", List.of(idField));

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                     .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                      .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(model.getFields()))
                      .thenReturn(List.of());

            gen.generate(model, "src/main/java");

            fieldUtils.verify(() -> FieldUtils.isAnyFieldId(model.getFields()));
            fieldUtils.verify(() -> FieldUtils.extractRelationTypes(model.getFields()));
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateBusinessServiceUnitTestAndWriteFile() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final List<ModelDefinition> allEntities = List.of();
        final BusinessServiceUnitTestGenerator gen =
                new BusinessServiceUnitTestGenerator(cfg, allEntities, pkgCfg);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition nonRelField = mock(FieldDefinition.class);
        final FieldDefinition relField = mock(FieldDefinition.class);

        when(nonRelField.getRelation()).thenReturn(null);
        when(relField.getRelation()).thenReturn(mock(RelationDefinition.class));

        final List<FieldDefinition> fields = List.of(idField, nonRelField, relField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        final TestDataGeneratorConfig dgConfig = mock(TestDataGeneratorConfig.class);

        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        final String outputDir = "src/main/java";
        final String expectedTestOutputDir = "src/test/java";

        final String packagePath = "com.example.app";
        final String bsPackage = "com.example.app.service";
        final String bsSubPackage = "service";
        final String strippedModel = "User";

        final String baseImport = "// base import\n";
        final String projectImports = "// project imports\n";
        final String testImports = "// test imports\n";

        final Map<String, Object> bsCtxMap = Map.of("bsKey", "bsVal");
        final Map<String, Object> createServiceCtx = Map.of("createKey", "createVal");
        final Map<String, Object> addRelCtx = new HashMap<>(Map.of("addKey", "addVal"));
        final Map<String, Object> removeRelCtx = new HashMap<>(Map.of("removeKey", "removeVal"));
        final Map<String, Object> dgCtxMap = Map.of("dgKey", "dgVal");

        final String createMethodTpl = "// create method";
        final String addMethodTpl = "// add relation";
        final String removeMethodTpl = "// remove relation";
        final String testClassTpl = "// CLASS BODY";

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<BusinessServiceImports> bsImports = mockStatic(BusinessServiceImports.class);
             final MockedStatic<BusinessServiceTemplateContext> bsTemplateCtx = mockStatic(BusinessServiceTemplateContext.class);
             final MockedStatic<DataGeneratorTemplateContext> dgTemplateCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                     .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(fields))
                      .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                      .thenReturn(List.of("ManyToOne"));
            fieldUtils.when(() -> FieldUtils.extractIdField(fields))
                      .thenReturn(idField);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn(strippedModel);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
               .thenReturn(packagePath);
            pkg.when(() -> PackageUtils.computeBusinessServicePackage(packagePath, pkgCfg))
               .thenReturn(bsPackage);
            pkg.when(() -> PackageUtils.computeBusinessServiceSubPackage(pkgCfg))
               .thenReturn(bsSubPackage);

            bsImports.when(() -> BusinessServiceImports.getTestBaseImport(model))
                     .thenReturn(baseImport);
            bsImports.when(() -> BusinessServiceImports.computeModelsEnumsAndServiceImports(
                    eq(model), eq(outputDir), eq(BusinessServiceImportScope.BUSINESS_SERVICE_TEST), eq(pkgCfg)))
                     .thenReturn(projectImports);
            bsImports.when(() -> BusinessServiceImports.computeTestBusinessServiceImports(
                    UnitTestUtils.isInstancioEnabled(cfg)))
                     .thenReturn(testImports);

            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.PODAM))
                     .thenReturn(dgConfig);
            unitUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg))
                     .thenReturn(true);

            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeBusinessServiceContext(model))
                         .thenReturn(bsCtxMap);

            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeCreateResourceMethodServiceContext(
                    model, allEntities))
                         .thenReturn(createServiceCtx);
            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeAddRelationMethodServiceContext(
                    model, allEntities))
                         .thenReturn(addRelCtx);
            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeRemoveRelationMethodServiceContext(
                    model, allEntities))
                         .thenReturn(removeRelCtx);

            dgTemplateCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(dgConfig))
                         .thenReturn(dgCtxMap);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("test/unit/businessservice/method/create-resource.ftl"), anyMap()))
               .thenReturn(createMethodTpl);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("test/unit/businessservice/method/add-relation.ftl"), anyMap()))
               .thenReturn(addMethodTpl);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("test/unit/businessservice/method/remove-relation.ftl"), anyMap()))
               .thenReturn(removeMethodTpl);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("test/unit/businessservice/businessservice-test-class-template.ftl"), anyMap()))
               .thenReturn(testClassTpl);

            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> null);

            gen.generate(model, outputDir);

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedTestOutputDir),
                    eq(bsSubPackage),
                    eq("UserBusinessServiceTest"),
                    anyString()
            ));
        }
    }
}