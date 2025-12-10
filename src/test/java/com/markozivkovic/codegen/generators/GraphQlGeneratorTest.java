package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.imports.ResolverImports;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.templates.GraphQlTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

class GraphQlGeneratorTest {

    private ModelDefinition newModel(final String name, final  List<FieldDefinition> fields) {
        
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenGraphQlDisabledOrNull() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getGraphQl()).thenReturn(null);

        final ProjectMetadata metadata = mock(ProjectMetadata.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final List<ModelDefinition> entities = List.of();

        final GraphQlGenerator generator = new GraphQlGenerator(cfg, metadata, entities, pkgCfg);
        final ModelDefinition model = newModel("UserEntity", List.of());

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GraphQlTemplateContext> gqlCtx = mockStatic(GraphQlTemplateContext.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate(model, "out");

            genCtx.verifyNoInteractions();
            pkg.verifyNoInteractions();
            fieldUtils.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            gqlCtx.verifyNoInteractions();
            resolverImports.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateSchemasScalarsAndResolverOnFirstCall() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getGraphQl()).thenReturn(true);

        final ProjectMetadata metadata = mock(ProjectMetadata.class);
        when(metadata.getProjectBaseDir()).thenReturn("/base");

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition f = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("UserEntity", List.of(f));
        final List<ModelDefinition> allEntities = List.of(model);

        final GraphQlGenerator generator =
                new GraphQlGenerator(cfg, metadata, allEntities, pkgCfg);

        final String outputDir = "out";
        final String pathToGraphQlSchema = "/base/" + GeneratorConstants.SRC_MAIN_RESOURCES_GRAPHQL;

        final AtomicReference<Map<String, Object>> resolverCtxRef = new AtomicReference<>();
        final List<InvocationOnMock> writerInvocations = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<GraphQlTemplateContext> gqlCtx = mockStatic(GraphQlTemplateContext.class);
             final MockedStatic<ResolverImports> resolverImports = mockStatic(ResolverImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class, invocation -> {
                         writerInvocations.add(invocation);
                         return null;
                     })) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList()))
                    .thenReturn(true);
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeResolversPackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.graphql");
            pkg.when(() -> PackageUtils.computeResolversSubPackage(pkgCfg))
                    .thenReturn("graphql");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            final Map<String, Object> schemaCtx = new HashMap<>();
            gqlCtx.when(() -> GraphQlTemplateContext.computeGraphQlSchemaContext(model, allEntities))
                    .thenReturn(schemaCtx);

            final Map<String, Object> baseResolverCtx = new HashMap<>();
            gqlCtx.when(() -> GraphQlTemplateContext.computeGraphQlResolver(model))
                    .thenReturn(baseResolverCtx);

            final Map<String, Object> mutationsCtx = new HashMap<>();
            final Map<String, Object> queriesCtx = new HashMap<>();
            gqlCtx.when(() -> GraphQlTemplateContext.computeMutationMappingGraphQL(model, allEntities))
                    .thenReturn(mutationsCtx);
            gqlCtx.when(() -> GraphQlTemplateContext.computeQueryMappingGraphQL(model))
                    .thenReturn(queriesCtx);

            resolverImports.when(() -> ResolverImports.computeResolverBaseImports(model))
                    .thenReturn("BASE_IMPORTS;");
            resolverImports.when(() -> ResolverImports.computeGraphQlResolverImports(model, outputDir, pkgCfg))
                    .thenReturn("RESOLVER_IMPORTS;");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("graphql/entity.graphql.ftl"),
                    eq(schemaCtx)
            )).thenReturn("ENTITY_GRAPHQL");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("graphql/scalars.graphql.ftl"),
                    eq(Map.of())
            )).thenReturn("SCALARS");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("graphql/mapping/mutations.ftl"),
                    eq(mutationsCtx)
            )).thenReturn("MUTATIONS_TEMPLATE");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("graphql/mapping/queries.ftl"),
                    eq(queriesCtx)
            )).thenReturn("QUERIES_TEMPLATE");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("graphql/resolver-template.ftl"),
                    anyMap()
            )).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                resolverCtxRef.set(ctx);
                return "RESOLVER_TEMPLATE";
            });

            generator.generate(model, outputDir);
        }

        final boolean wroteSchema = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && pathToGraphQlSchema.equals(inv.getArgument(0))
                        && "user.graphqls".equals(inv.getArgument(1))
                        && "ENTITY_GRAPHQL".equals(inv.getArgument(2)));

        assertTrue(wroteSchema, "Entity GraphQL schema should be written");

        final boolean wroteScalars = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && pathToGraphQlSchema.equals(inv.getArgument(0))
                        && "scalars.graphqls".equals(inv.getArgument(1))
                        && "SCALARS".equals(inv.getArgument(2)));

        assertTrue(wroteScalars, "scalars.graphqls should be written");

        final boolean wroteResolver = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && "out".equals(inv.getArgument(0))
                        && "graphql".equals(inv.getArgument(1))
                        && "UserResolver.java".equals(inv.getArgument(2)));

        assertTrue(wroteResolver, "UserResolver.java should be written");

        final Map<String, Object> resolverCtx = resolverCtxRef.get();
        assertNotNull(resolverCtx);
        assertEquals("MUTATIONS_TEMPLATE", resolverCtx.get("mutations"));
        assertEquals("QUERIES_TEMPLATE", resolverCtx.get("queries"));
        assertEquals("RESOLVER_IMPORTS;", resolverCtx.get("projectImports"));
    }

    @Test
    void generate_shouldNotGenerateResolverWhenModelHasNoIdField() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getGraphQl()).thenReturn(true);

        final ProjectMetadata metadata = mock(ProjectMetadata.class);
        when(metadata.getProjectBaseDir()).thenReturn("/base");

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final List<ModelDefinition> allEntities = List.of();

        final GraphQlGenerator generator = new GraphQlGenerator(cfg, metadata, allEntities, pkgCfg);
        final FieldDefinition f = mock(FieldDefinition.class);
        final ModelDefinition modelWithoutId = newModel("OrderEntity", List.of(f));

        final String pathToGraphQlSchema = "/base/" + GeneratorConstants.SRC_MAIN_RESOURCES_GRAPHQL;
        final List<InvocationOnMock> writerInvocations = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class, invocation -> {
                         writerInvocations.add(invocation);
                         return null;
                     })) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(modelWithoutId.getFields()))
                    .thenReturn(false);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("graphql/scalars.graphql.ftl"),
                    eq(Map.of())
            )).thenReturn("SCALARS_ONLY");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");

            generator.generate(modelWithoutId, "out");
        }

        final boolean wroteScalars = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && pathToGraphQlSchema.equals(inv.getArgument(0))
                        && "scalars.graphqls".equals(inv.getArgument(1))
                        && "SCALARS_ONLY".equals(inv.getArgument(2)));

        assertTrue(wroteScalars, "scalars.graphqls should be written when GraphQL is enabled");

        final boolean wroteAnyResolver = writerInvocations.stream()
                .filter(inv -> "writeToFile".equals(inv.getMethod().getName()))
                .filter(inv -> inv.getArguments().length == 4)
                .anyMatch(inv -> {
                    Object fileName = inv.getArgument(2);
                    return fileName != null && fileName.toString().endsWith("Resolver.java");
                });

        assertFalse(wroteAnyResolver, "No resolver should be generated when model has no ID field");
    }
    
}
