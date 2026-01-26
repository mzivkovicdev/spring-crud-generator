package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.GraphQLDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.OpenApiDefinition;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.MapperTemplateContexts;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class MapperGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenModelIsUsedAsJsonField() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = newModel("UserEntity", List.of());
        final List<ModelDefinition> allEntities = List.of(model);

        final MapperGenerator generator = new MapperGenerator(cfg, allEntities, pkgCfg);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<MapperTemplateContexts> mapperCtx = mockStatic(MapperTemplateContexts.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(model, allEntities))
                    .thenReturn(true);

            generator.generate(model, "out");

            fieldUtils.verify(() -> FieldUtils.isModelUsedAsJsonField(model, allEntities));
            fieldUtils.verifyNoMoreInteractions();

            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            mapperCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateRestAndGraphQlMappersAndHelperMappers() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(mock(OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);

        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition jsonField = mock(FieldDefinition.class);

        final ModelDefinition userModel = newModel("UserEntity", List.of(jsonField));
        final ModelDefinition addressModel = newModel("AddressEntity", List.of());
        final List<ModelDefinition> allEntities = List.of(userModel, addressModel);
        final MapperGenerator generator = new MapperGenerator(cfg, allEntities, pkgCfg);

        final String outputDir = "out";
        final String packagePath = "com.example.app";
        final List<String> writtenClassNames = new ArrayList<>();
        final List<String> writtenSubPackages = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<MapperTemplateContexts> mapperCtx = mockStatic(MapperTemplateContexts.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(userModel, allEntities))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("AddressEntity");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn(packagePath);

            pkg.when(() -> PackageUtils.computeRestMapperPackage(packagePath, pkgCfg))
                    .thenReturn("com.example.app.mapper.rest");
            pkg.when(() -> PackageUtils.computeGraphQlMapperPackage(packagePath, pkgCfg))
                    .thenReturn("com.example.app.mapper.graphql");
            pkg.when(() -> PackageUtils.computeRestMappersSubPackage(pkgCfg))
                    .thenReturn("mapper/rest");
            pkg.when(() -> PackageUtils.computeGraphQlMappersSubPackage(pkgCfg))
                    .thenReturn("mapper/graphql");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage(packagePath, pkgCfg))
                    .thenReturn("com.example.app.mapper.rest.helper");
            pkg.when(() -> PackageUtils.computeHelperGraphQlMapperPackage(packagePath, pkgCfg))
                    .thenReturn("com.example.app.mapper.graphql.helper");
            pkg.when(() -> PackageUtils.computeHelperRestMappersSubPackage(pkgCfg))
                    .thenReturn("mapper/rest/helper");
            pkg.when(() -> PackageUtils.computeHelperGraphQlMappersSubPackage(pkgCfg))
                    .thenReturn("mapper/graphql/helper");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                    .thenReturn("Address");

            mapperCtx.when(() -> MapperTemplateContexts.computeMapperContext(eq(userModel), eq(packagePath), eq(true), eq(false), eq(pkgCfg)))
                    .thenReturn(new HashMap<>());
            mapperCtx.when(() -> MapperTemplateContexts.computeMapperContext(eq(userModel), eq(packagePath), eq(false), eq(true), eq(pkgCfg)))
                    .thenReturn(new HashMap<>());

            mapperCtx.when(() -> MapperTemplateContexts.computeHelperMapperContext(eq(userModel), eq(addressModel), eq(packagePath), eq(true), eq(false), eq(pkgCfg)))
                    .thenReturn(new HashMap<>());
            mapperCtx.when(() -> MapperTemplateContexts.computeHelperMapperContext(eq(userModel), eq(addressModel), eq(packagePath), eq(false), eq(true), eq(pkgCfg)))
                    .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("mapper/mapper-template.ftl"), anyMap()))
                    .thenReturn("// MAPPER TEMPLATE");

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), anyString(), anyString(), anyString())).thenAnswer(inv -> {
                    final String sub = inv.getArgument(1, String.class);
                    final String cls = inv.getArgument(2, String.class);
                    writtenSubPackages.add(sub);
                    writtenClassNames.add(cls);
                    return null;
            });

            generator.generate(userModel, outputDir);
        }

        assertTrue(writtenClassNames.contains("AddressRestMapper"));
        assertTrue(writtenClassNames.contains("AddressGraphQLMapper"));
        assertTrue(writtenClassNames.contains("UserRestMapper"));
        assertTrue(writtenClassNames.contains("UserGraphQLMapper"));
        assertTrue(writtenSubPackages.contains("mapper/rest/helper"));
        assertTrue(writtenSubPackages.contains("mapper/graphql/helper"));
        assertTrue(writtenSubPackages.contains("mapper/rest"));
        assertTrue(writtenSubPackages.contains("mapper/graphql"));
    }

    @Test
    void generate_shouldThrowWhenJsonModelNotFound() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);
        
        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(false);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final ModelDefinition userModel = newModel("UserEntity", List.of(jsonField));
        final List<ModelDefinition> allEntities = List.of(userModel);

        final MapperGenerator generator = new MapperGenerator(cfg, allEntities, pkgCfg);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<MapperTemplateContexts> mapperCtx = mockStatic(MapperTemplateContexts.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(userModel, allEntities))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("AddressEntity");

            assertThrows(IllegalArgumentException.class,
                    () -> generator.generate(userModel, "out"));

            writer.verifyNoInteractions();
        }
    }
}
