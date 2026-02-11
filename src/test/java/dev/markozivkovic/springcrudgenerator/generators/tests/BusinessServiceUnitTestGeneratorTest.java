package dev.markozivkovic.springcrudgenerator.generators.tests;

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

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.templates.BusinessServiceTemplateContext;
import dev.markozivkovic.springcrudgenerator.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

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
             final MockedStatic<BusinessServiceTemplateContext> bsCtx = mockStatic(BusinessServiceTemplateContext.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(false);

            gen.generate(model, "src/main/java");

            unitUtils.verify(() -> UnitTestUtils.isUnitTestsEnabled(cfg));
            fieldUtils.verifyNoInteractions();
            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
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

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(false);

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

        final BusinessServiceUnitTestGenerator gen = new BusinessServiceUnitTestGenerator(cfg, allEntities, pkgCfg);
        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("UserEntity", List.of(idField));

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(model.getFields())).thenReturn(List.of());

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

        final BusinessServiceUnitTestGenerator gen = new BusinessServiceUnitTestGenerator(cfg, allEntities, pkgCfg);
        final FieldDefinition idField = mock(FieldDefinition.class);

        final FieldDefinition nonRelField = mock(FieldDefinition.class);
        when(nonRelField.getRelation()).thenReturn(null);

        final FieldDefinition relField = mock(FieldDefinition.class);
        when(relField.getRelation()).thenReturn(mock(RelationDefinition.class));

        final List<FieldDefinition> fields = List.of(idField, nonRelField, relField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        final TestDataGeneratorConfig dgConfig = mock(TestDataGeneratorConfig.class);

        final String outputDir = "src/main/java";
        final String expectedTestOutputDir = "src/test/java";

        final String packagePath = "com.example.app";
        final String bsPackage = "com.example.app.service";
        final String bsSubPackage = "service";
        final String strippedModel = "User";

        final Map<String, Object> testContext = new HashMap<>(Map.of("tKey", "tVal")); // MUST be mutable
        final Map<String, Object> createServiceCtx = Map.of("createKey", "createVal");
        final Map<String, Object> addRelCtx = new HashMap<>(Map.of("addKey", "addVal"));       // mutable ok
        final Map<String, Object> removeRelCtx = new HashMap<>(Map.of("removeKey", "removeVal")); // mutable ok
        final Map<String, Object> dgCtxMap = Map.of("dgKey", "dgVal");

        final String createMethodTpl = "// create method";
        final String addMethodTpl = "// add relation";
        final String removeMethodTpl = "// remove relation";
        final String testClassTpl = "// CLASS BODY";

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<BusinessServiceTemplateContext> bsTemplateCtx = mockStatic(BusinessServiceTemplateContext.class);
             final MockedStatic<DataGeneratorTemplateContext> dgTemplateCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields)).thenReturn(List.of("ManyToOne"));
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn(strippedModel);
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn(packagePath);
            pkg.when(() -> PackageUtils.computeBusinessServicePackage(packagePath, pkgCfg)).thenReturn(bsPackage);
            pkg.when(() -> PackageUtils.computeBusinessServiceSubPackage(pkgCfg)).thenReturn(bsSubPackage);

            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeBusinessServiceTestContext(model, cfg, pkgCfg, outputDir))
                    .thenReturn(testContext);
            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeCreateResourceMethodServiceContext(model, allEntities))
                    .thenReturn(createServiceCtx);
            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeAddRelationMethodServiceContext(model, allEntities))
                    .thenReturn(addRelCtx);
            bsTemplateCtx.when(() -> BusinessServiceTemplateContext.computeRemoveRelationMethodServiceContext(model, allEntities))
                    .thenReturn(removeRelCtx);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.PODAM)).thenReturn(dgConfig);
            dgTemplateCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(dgConfig)).thenReturn(dgCtxMap);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/businessservice/method/create-resource.ftl"), anyMap()))
                    .thenReturn(createMethodTpl);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/businessservice/method/add-relation.ftl"), anyMap()))
                    .thenReturn(addMethodTpl);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/businessservice/method/remove-relation.ftl"), anyMap()))
                    .thenReturn(removeMethodTpl);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/businessservice/businessservice-test-class-template.ftl"), anyMap()))
                    .thenReturn(testClassTpl);
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            gen.generate(model, outputDir);

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedTestOutputDir), eq(bsSubPackage), eq("UserBusinessServiceTest"), anyString()
            ));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("test/unit/businessservice/method/create-resource.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        @SuppressWarnings("unchecked")
                        final List<FieldDefinition> passedFields = (List<FieldDefinition>) map.get(TemplateContextConstants.FIELDS);
                        return passedFields != null
                                && passedFields.size() == 1
                                && passedFields.get(0) == nonRelField
                                && "createVal".equals(map.get("createKey"))
                                && "dgVal".equals(map.get("dgKey"));
                    })
            ));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("test/unit/businessservice/businessservice-test-class-template.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return createMethodTpl.equals(map.get(TemplateContextConstants.CREATE_RESOURCE))
                                && addMethodTpl.equals(map.get(TemplateContextConstants.ADD_RELATION_METHOD))
                                && removeMethodTpl.equals(map.get(TemplateContextConstants.REMOVE_RELATION_METHOD));
                    })
            ));
        }
    }
}
