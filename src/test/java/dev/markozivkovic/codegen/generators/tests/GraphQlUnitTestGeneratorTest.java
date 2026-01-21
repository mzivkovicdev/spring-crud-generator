package dev.markozivkovic.codegen.generators.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.GeneratorConstants.GeneratorContextKeys;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.imports.ResolverImports;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.ErrorResponse;
import dev.markozivkovic.codegen.models.CrudConfiguration.GraphQLDefinition;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.models.RelationDefinition;
import dev.markozivkovic.codegen.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.codegen.templates.GraphQlTemplateContext;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

class GraphQlUnitTestGeneratorTest {

    private ModelDefinition model(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    @DisplayName("generate: graphQl disabled -> no file writes")
    void generate_shouldReturn_whenGraphqlDisabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(false);

        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(), pkgCfg);
        final ModelDefinition model = model("UserEntity", List.of());

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);

            gen.generate(model, "src/main/java");

            writer.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("generate: graphQl disabled -> no file writes")
    void generate_shouldReturn_whenUnitTestsDisabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(), pkgCfg);
        final ModelDefinition model = model("UserEntity", List.of());

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(false);

            gen.generate(model, "src/main/java");

            writer.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("generate: model has no ID field -> skips generation")
    void generate_shouldSkip_whenModelHasNoIdField() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);

        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(), pkgCfg);
        final ModelDefinition model = model("UserEntity", List.of());

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(false);

            gen.generate(model, "src/main/java");

            writer.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("generate: first time (config not generated) -> writes config + query + mutation; global handler enabled")
    void generate_shouldGenerateConfigQueryAndMutationTests_globalHandlerEnabled() {

        final String outputDir = "src/main/java";
        final String testOutputDir = "src/test/java";

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        when(cfg.getSpringBootVersion()).thenReturn("4");
        when(cfg.getErrorResponse()).thenReturn(ErrorResponse.SIMPLE);

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

        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(user), pkgCfg);

        final List<String> writtenFiles = new ArrayList<>();
        final List<String> writtenDirs = new ArrayList<>();

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<GraphQlTemplateContext> gqlCtx = mockStatic(GraphQlTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(user.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(user.getFields())).thenReturn(id);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(user.getFields())).thenReturn(List.of(relation));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.test");
            pkg.when(() -> PackageUtils.computeResolversPackage("com.test", pkgCfg)).thenReturn("com.test.resolver");
            pkg.when(() -> PackageUtils.computeResolversSubPackage(pkgCfg)).thenReturn("resolver");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RESOLVER_TEST_CONFIG)).thenReturn(false);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            unitUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.computeInvalidIdType(any())).thenReturn("String");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());
            resolverImports.when(() -> ResolverImports.computeQueryResolverTestImports(anyBoolean(), anyString())).thenReturn("");
            resolverImports.when(() -> ResolverImports.computeProjectImportsForQueryUnitTests(anyString(), any(), any(), anyBoolean()))
                    .thenReturn("");
            gqlCtx.when(() -> GraphQlTemplateContext.computeMutationUnitTestContext(
                    eq(user), eq(cfg), eq(pkgCfg), anyList(), eq(outputDir), eq(testOutputDir)
            )).thenReturn(Map.of());
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// TEMPLATE");

            writer.when(() -> FileWriterUtils.writeToFile(any(), any(), any(), any())).thenAnswer(inv -> {
                writtenDirs.add(inv.getArgument(0));
                writtenFiles.add(inv.getArgument(2));
                return null;
            });

            gen.generate(user, outputDir);

            assertTrue(writtenFiles.contains("ResolverTestConfiguration.java"));
            assertTrue(writtenFiles.contains("UserResolverQueryTest"));
            assertTrue(writtenFiles.contains("UserResolverMutationTest"));
            assertTrue(writtenDirs.stream().allMatch(testOutputDir::equals));

            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorContextKeys.RESOLVER_TEST_CONFIG));

            resolverImports.verify(() ->
                    ResolverImports.computeProjectImportsForQueryUnitTests(outputDir, user, pkgCfg, true)
            );

            gqlCtx.verify(() -> GraphQlTemplateContext.computeMutationUnitTestContext(
                    eq(user), eq(cfg), eq(pkgCfg), anyList(), eq(outputDir), eq(testOutputDir)
            ));
        }
    }

    @Test
    @DisplayName("generate: config already generated -> only query + mutation; global handler disabled when errorResponse NONE")
    void generate_shouldGenerateQueryAndMutationTests_whenConfigAlreadyGenerated_globalHandlerDisabled() {

        final String outputDir = "src/main/java";

        final CrudConfiguration cfg = mock(CrudConfiguration.class);

        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        when(cfg.getSpringBootVersion()).thenReturn("3");
        when(cfg.getErrorResponse()).thenReturn(ErrorResponse.NONE);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition id = mock(FieldDefinition.class);
        when(id.getName()).thenReturn("id");
        when(id.getType()).thenReturn("Long");

        final ModelDefinition user = model("UserEntity", List.of(id));

        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(user), pkgCfg);
        final List<String> writtenFiles = new ArrayList<>();

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<GraphQlTemplateContext> gqlCtx = mockStatic(GraphQlTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(user.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(user.getFields())).thenReturn(id);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(user.getFields())).thenReturn(List.of());

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.test");
            pkg.when(() -> PackageUtils.computeResolversPackage("com.test", pkgCfg)).thenReturn("com.test.resolver");
            pkg.when(() -> PackageUtils.computeResolversSubPackage(pkgCfg)).thenReturn("resolver");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            ctx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RESOLVER_TEST_CONFIG)).thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            unitUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.computeInvalidIdType(any())).thenReturn("String");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());
            resolverImports.when(() -> ResolverImports.computeQueryResolverTestImports(anyBoolean(), anyString())).thenReturn("");
            resolverImports.when(() -> ResolverImports.computeProjectImportsForQueryUnitTests(anyString(), any(), any(), anyBoolean()))
                    .thenReturn("");
            gqlCtx.when(() -> GraphQlTemplateContext.computeMutationUnitTestContext(
                    eq(user), eq(cfg), eq(pkgCfg), anyList(), eq(outputDir), anyString()
            )).thenReturn(Map.of());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// TEMPLATE");

            writer.when(() -> FileWriterUtils.writeToFile(any(), any(), any(), any()))
                    .thenAnswer(inv -> {
                        writtenFiles.add(inv.getArgument(2));
                        return null;
                    });

            gen.generate(user, outputDir);

            assertFalse(writtenFiles.contains("ResolverTestConfiguration.java"));
            assertTrue(writtenFiles.contains("UserResolverQueryTest"));
            assertTrue(writtenFiles.contains("UserResolverMutationTest"));

            resolverImports.verify(() ->
                    ResolverImports.computeProjectImportsForQueryUnitTests(outputDir, user, pkgCfg, false)
            );
        }
    }

    @Test
    @DisplayName("generate: relation field points to missing entity -> throws NoSuchElementException")
    void generate_shouldThrow_whenRelatedEntityMissing() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);

        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        when(cfg.getSpringBootVersion()).thenReturn("4");
        when(cfg.getErrorResponse()).thenReturn(ErrorResponse.SIMPLE);

        final TestConfiguration testsCfg = mock(TestConfiguration.class);
        when(cfg.getTests()).thenReturn(testsCfg);
        when(testsCfg.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition id = mock(FieldDefinition.class);
        when(id.getName()).thenReturn("id");
        when(id.getType()).thenReturn("Long");

        final RelationDefinition relDef = mock(RelationDefinition.class);
        final FieldDefinition relation = mock(FieldDefinition.class);
        when(relation.getName()).thenReturn("roles");
        when(relation.getType()).thenReturn("RoleEntity");
        when(relation.getRelation()).thenReturn(relDef);

        final ModelDefinition user = model("UserEntity", List.of(id, relation));
        final GraphQlUnitTestGenerator gen = new GraphQlUnitTestGenerator(cfg, List.of(user), pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(user.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(user.getFields())).thenReturn(id);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(user.getFields())).thenReturn(List.of(relation));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(anyString())).thenReturn("com.test");
            pkg.when(() -> PackageUtils.computeResolversPackage(anyString(), any())).thenReturn("com.test.resolver");
            pkg.when(() -> PackageUtils.computeResolversSubPackage(any())).thenReturn("resolver");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");

            ctx.when(() -> GeneratorContext.isGenerated(anyString())).thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            unitUtils.when(() -> UnitTestUtils.isInstancioEnabled(cfg)).thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.computeInvalidIdType(any())).thenReturn("String");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any())).thenReturn(Map.of());
            resolverImports.when(() -> ResolverImports.computeQueryResolverTestImports(anyBoolean(), anyString())).thenReturn("");
            resolverImports.when(() -> ResolverImports.computeProjectImportsForQueryUnitTests(anyString(), any(), any(), anyBoolean()))
                    .thenReturn("");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// TEMPLATE");

            assertThrows(NoSuchElementException.class, () -> gen.generate(user, "src/main/java"));
        }
    }
}
