package com.markozivkovic.codegen.generators.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.constants.GeneratorConstants.GeneratorContextKeys;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.imports.ResolverImports;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.templates.DataGeneratorTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

class GraphQlUnitTestGeneratorTest {

    private ModelDefinition model(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldReturn_whenUnitTestsDisabledOrGraphQlOff() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(), pkgCfg);
        final ModelDefinition model = model("UserEntity", List.of());

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                    .thenReturn(false);

            gen.generate(model, "src/main/java");

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkip_whenModelHasNoIdField() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getGraphQl()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(), pkgCfg);
        final ModelDefinition model = model("UserEntity", List.of());

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                    .thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(false);

            gen.generate(model, "src/main/java");

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateConfigQueryAndMutationTests() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getGraphQl()).thenReturn(true);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition id = mock(FieldDefinition.class);
        when(id.getName()).thenReturn("id");
        when(id.getType()).thenReturn("Long");

        final FieldDefinition relation = mock(FieldDefinition.class);
        when(relation.getName()).thenReturn("roles");
        when(relation.getType()).thenReturn("RoleEntity");

        final ModelDefinition user = model("UserEntity", List.of(id, relation));

        final FieldDefinition roleId = mock(FieldDefinition.class);
        when(roleId.getName()).thenReturn("id");
        when(roleId.getType()).thenReturn("UUID");

        final ModelDefinition role = model("RoleEntity", List.of(roleId));

        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(user, role), pkgCfg);
        final List<String> writtenFiles = new ArrayList<>();

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ResolverImports> imports = mockStatic(ResolverImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                    .thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg))
                    .thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any()))
                    .thenReturn(mock(TestDataGeneratorConfig.class));
            unitUtils.when(() -> UnitTestUtils.computeInvalidIdType(any()))
                    .thenReturn("String");

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(user.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(user.getFields()))
                    .thenReturn(id);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(user.getFields()))
                    .thenReturn(List.of(relation));
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(user))
                    .thenReturn(List.of("roles"));
            fieldUtils.when(() -> FieldUtils.extractJsonFields(user.getFields()))
                    .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractIdField(role.getFields()))
                    .thenReturn(roleId);

            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(any()))
                    .thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(any()))
                    .thenReturn(List.of("name"));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(any()))
                    .thenReturn("com.test");
            pkg.when(() -> PackageUtils.computeResolversPackage(any(), any()))
                    .thenReturn("com.test.resolver");
            pkg.when(() -> PackageUtils.computeResolversSubPackage(any()))
                    .thenReturn("resolver");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            ctx.when(() -> GeneratorContext.isGenerated(any()))
                    .thenReturn(false);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenReturn("// TEMPLATE");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any()))
                    .thenReturn(Map.of());

            writer.when(() -> FileWriterUtils.writeToFile(any(), any(), any(), any()))
                  .thenAnswer(inv -> {
                      writtenFiles.add(inv.getArgument(2));
                      return null;
                  });

            gen.generate(user, "src/main/java");

            assertTrue(writtenFiles.contains("ResolverTestConfiguration.java"));
            assertTrue(writtenFiles.contains("UserResolverQueryTest"));
            assertTrue(writtenFiles.contains("UserResolverMutationTest"));

            ctx.verify(() -> GeneratorContext.markGenerated(
                    GeneratorContextKeys.RESOLVER_TEST_CONFIG
            ));
        }
    }

    @Test
    void generate_shouldGenerateQueryAndMutationTests() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getGraphQl()).thenReturn(true);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition id = mock(FieldDefinition.class);
        when(id.getName()).thenReturn("id");
        when(id.getType()).thenReturn("Long");

        final FieldDefinition relation = mock(FieldDefinition.class);
        when(relation.getName()).thenReturn("roles");
        when(relation.getType()).thenReturn("RoleEntity");

        final ModelDefinition user = model("UserEntity", List.of(id, relation));

        final FieldDefinition roleId = mock(FieldDefinition.class);
        when(roleId.getName()).thenReturn("id");
        when(roleId.getType()).thenReturn("UUID");

        final ModelDefinition role = model("RoleEntity", List.of(roleId));

        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(user, role), pkgCfg);
        final List<String> writtenFiles = new ArrayList<>();

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ResolverImports> imports = mockStatic(ResolverImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg))
                    .thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg))
                    .thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any()))
                    .thenReturn(mock(TestDataGeneratorConfig.class));
            unitUtils.when(() -> UnitTestUtils.computeInvalidIdType(any()))
                    .thenReturn("String");

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(user.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(user.getFields()))
                    .thenReturn(id);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(user.getFields()))
                    .thenReturn(List.of(relation));
            fieldUtils.when(() -> FieldUtils.extractCollectionRelationNames(user))
                    .thenReturn(List.of("roles"));
            fieldUtils.when(() -> FieldUtils.extractJsonFields(user.getFields()))
                    .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractIdField(role.getFields()))
                    .thenReturn(roleId);

            fieldUtils.when(() -> FieldUtils.extractNonIdFieldNamesForResolver(any()))
                    .thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNonIdNonRelationFieldNamesForResolver(any()))
                    .thenReturn(List.of("name"));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(any()))
                    .thenReturn("com.test");
            pkg.when(() -> PackageUtils.computeResolversPackage(any(), any()))
                    .thenReturn("com.test.resolver");
            pkg.when(() -> PackageUtils.computeResolversSubPackage(any()))
                    .thenReturn("resolver");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            ctx.when(() -> GeneratorContext.isGenerated(any()))
                    .thenReturn(true);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenReturn("// TEMPLATE");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any()))
                    .thenReturn(Map.of());

            writer.when(() -> FileWriterUtils.writeToFile(any(), any(), any(), any()))
                  .thenAnswer(inv -> {
                      writtenFiles.add(inv.getArgument(2));
                      return null;
                  });

            gen.generate(user, "src/main/java");

            assertFalse(writtenFiles.contains("ResolverTestConfiguration.java"));
            assertTrue(writtenFiles.contains("UserResolverQueryTest"));
            assertTrue(writtenFiles.contains("UserResolverMutationTest"));
        }
    }

}
