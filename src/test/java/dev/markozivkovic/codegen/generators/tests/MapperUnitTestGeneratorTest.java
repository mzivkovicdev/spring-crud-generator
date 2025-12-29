package dev.markozivkovic.codegen.generators.tests;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.imports.MapperImports;
import dev.markozivkovic.codegen.models.CrudConfiguration;
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

class MapperUnitTestGeneratorTest {

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
        final List<ModelDefinition> entities = List.of();

        final MapperUnitTestGenerator gen = new MapperUnitTestGenerator(cfg, entities, pkgCfg);
        final ModelDefinition model = newModel("UserEntity", List.of());

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(false);

            gen.generate(model, "src/main/java/");

            unitUtils.verify(() -> UnitTestUtils.isUnitTestsEnabled(cfg));
            fieldUtils.verifyNoInteractions();
            pkg.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenModelIsUsedAsJsonField() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final ModelDefinition model = newModel("UserEntity", List.of());
        final List<ModelDefinition> entities = List.of(model);

        final MapperUnitTestGenerator gen = new MapperUnitTestGenerator(cfg, entities, pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(model, entities)).thenReturn(true);

            gen.generate(model, "src/main/java/");

            fieldUtils.verify(() -> FieldUtils.isModelUsedAsJsonField(model, entities));
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldThrowWhenJsonModelNotFound() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.TestConfiguration tests = mock(CrudConfiguration.TestConfiguration.class);
        when(cfg.getTests()).thenReturn(tests);
        when(tests.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);
        when(cfg.getGraphQl()).thenReturn(false);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final ModelDefinition user = newModel("UserEntity", List.of(jsonField));
        final List<ModelDefinition> entities = List.of(user);

        final MapperUnitTestGenerator gen = new MapperUnitTestGenerator(cfg, entities, pkgCfg);

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(user, entities)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("AddressEntity");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("src/main/java/"))
                    .thenReturn("com.example.app");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            final IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> gen.generate(user, "src/main/java/")
            );

            assertTrue(ex.getMessage().contains("JSON model not found: AddressEntity"));

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateRestMapperTestsAndHelperWhenSwaggerEnabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.TestConfiguration tests = mock(CrudConfiguration.TestConfiguration.class);
        when(cfg.getTests()).thenReturn(tests);
        when(tests.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);
        when(cfg.getGraphQl()).thenReturn(false);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final FieldDefinition jsonField = mock(FieldDefinition.class);

        final ModelDefinition user = newModel("UserEntity", List.of(idField, jsonField));

        final FieldDefinition addrAnyField = mock(FieldDefinition.class);
        when(addrAnyField.getName()).thenReturn("addrId");

        final ModelDefinition address = newModel("AddressEntity", List.of(addrAnyField));

        final List<ModelDefinition> entities = List.of(user, address);

        final MapperUnitTestGenerator gen = new MapperUnitTestGenerator(cfg, entities, pkgCfg);

        final List<String> written = new ArrayList<>();
        final List<String> writtenSubDirs = new ArrayList<>();

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any())).thenReturn(mock(TestDataGeneratorConfig.class));
            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(user, entities)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("AddressEntity");
            fieldUtils.when(() -> FieldUtils.extractIdField(user.getFields())).thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractNonRelationNonEnumAndNonJsonFieldNames(anyList()))
                    .thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNamesOfEnumFields(anyList()))
                    .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractEnumFields(address.getFields()))
                    .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractFieldNames(address.getFields()))
                    .thenReturn(List.of("addrId"));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("src/main/java/"))
                    .thenReturn("com.example.app");

            pkg.when(() -> PackageUtils.computeRestMapperPackage(anyString(), eq(pkgCfg)))
                    .thenReturn("com.example.app.mapper.rest");
            pkg.when(() -> PackageUtils.computeRestMappersSubPackage(eq(pkgCfg)))
                    .thenReturn("mapper/rest");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage(anyString(), eq(pkgCfg)))
                    .thenReturn("com.example.app.mapper.rest.helper");
            pkg.when(() -> PackageUtils.computeHelperRestMappersSubPackage(eq(pkgCfg)))
                    .thenReturn("mapper/rest/helper");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");

            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Address")).thenReturn("Address");

            mapperImports.when(() -> MapperImports.computeTestMapperImports(anyString(), eq(user), eq(pkgCfg), eq(true), eq(false)))
                    .thenReturn("// imports");
            mapperImports.when(() -> MapperImports.computeTestHelperMapperImports(anyString(), eq(address), eq(user), eq(pkgCfg), eq(true), eq(false)))
                    .thenReturn("// helper imports");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any()))
                    .thenReturn(Map.of());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/mapper/mapper-test-template.ftl"), anyMap()))
                    .thenReturn("// TEST BODY");

            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        writtenSubDirs.add(inv.getArgument(1, String.class));
                        written.add(inv.getArgument(2, String.class));
                        return null;
                    });

            gen.generate(user, "src/main/java/");

            assertTrue(written.contains("UserRestMapperTest"));
            assertTrue(written.contains("AddressRestMapperTest"));
            assertTrue(writtenSubDirs.contains("mapper/rest"));
            assertTrue(writtenSubDirs.contains("mapper/rest/helper"));
        }
    }

    @Test
    void generate_shouldGenerateRestAndGraphQlMapperTests_whenGraphQlEnabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.TestConfiguration tests = mock(CrudConfiguration.TestConfiguration.class);
        when(cfg.getTests()).thenReturn(tests);
        when(tests.getDataGenerator()).thenReturn(DataGeneratorEnum.PODAM);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);
        when(cfg.getGraphQl()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getName()).thenReturn("id");

        final FieldDefinition jsonField = mock(FieldDefinition.class);

        final ModelDefinition user = newModel("UserEntity", List.of(idField, jsonField));

        final FieldDefinition addrAnyField = mock(FieldDefinition.class);
        when(addrAnyField.getName()).thenReturn("addrId");

        final ModelDefinition address = newModel("AddressEntity", List.of(addrAnyField));

        final List<ModelDefinition> entities = List.of(user, address);

        final MapperUnitTestGenerator gen = new MapperUnitTestGenerator(cfg, entities, pkgCfg);

        final List<String> written = new ArrayList<>();

        try (final MockedStatic<UnitTestUtils> unitUtils = mockStatic(UnitTestUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<MapperImports> mapperImports = mockStatic(MapperImports.class);
             final MockedStatic<DataGeneratorTemplateContext> dgCtx = mockStatic(DataGeneratorTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            unitUtils.when(() -> UnitTestUtils.isUnitTestsEnabled(cfg)).thenReturn(true);
            unitUtils.when(() -> UnitTestUtils.resolveGeneratorConfig(any()))
                    .thenReturn(mock(TestDataGeneratorConfig.class));

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(user, entities)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("AddressEntity");

            fieldUtils.when(() -> FieldUtils.extractIdField(user.getFields())).thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.extractNonRelationNonEnumAndNonJsonFieldNames(anyList()))
                    .thenReturn(List.of("name"));
            fieldUtils.when(() -> FieldUtils.extractNamesOfEnumFields(anyList()))
                    .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractEnumFields(address.getFields()))
                    .thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractFieldNames(address.getFields()))
                    .thenReturn(List.of("addrId"));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("src/main/java/"))
                    .thenReturn("com.example.app");

            pkg.when(() -> PackageUtils.computeRestMapperPackage(anyString(), eq(pkgCfg)))
                    .thenReturn("com.example.app.mapper.rest");
            pkg.when(() -> PackageUtils.computeRestMappersSubPackage(eq(pkgCfg)))
                    .thenReturn("mapper/rest");

            pkg.when(() -> PackageUtils.computeHelperRestMapperPackage(anyString(), eq(pkgCfg)))
                    .thenReturn("com.example.app.mapper.rest.helper");
            pkg.when(() -> PackageUtils.computeHelperRestMappersSubPackage(eq(pkgCfg)))
                    .thenReturn("mapper/rest/helper");

            pkg.when(() -> PackageUtils.computeGraphQlMapperPackage(anyString(), eq(pkgCfg)))
                   .thenReturn("com.example.app.mapper.graphql");
            pkg.when(() -> PackageUtils.computeGraphQlMappersSubPackage(eq(pkgCfg)))
                   .thenReturn("mapper/graphql");

            pkg.when(() -> PackageUtils.computeHelperGraphQlMapperPackage(anyString(), eq(pkgCfg)))
                   .thenReturn("com.example.app.mapper.graphql.helper");
            pkg.when(() -> PackageUtils.computeHelperGraphQlMappersSubPackage(eq(pkgCfg)))
                   .thenReturn("mapper/graphql/helper");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Address")).thenReturn("Address");

            mapperImports.when(() -> MapperImports.computeTestMapperImports(anyString(), eq(user), eq(pkgCfg), eq(true), eq(false)))
                    .thenReturn("// imports rest");
            mapperImports.when(() -> MapperImports.computeTestHelperMapperImports(anyString(), eq(address), eq(user), eq(pkgCfg), eq(true), eq(false)))
                    .thenReturn("// helper imports rest");

            mapperImports.when(() -> MapperImports.computeTestMapperImports(anyString(), eq(user), eq(pkgCfg), eq(false), eq(true)))
                    .thenReturn("// imports gql");
            mapperImports.when(() -> MapperImports.computeTestHelperMapperImports(anyString(), eq(address), eq(user), eq(pkgCfg), eq(false), eq(true)))
                    .thenReturn("// helper imports gql");

            dgCtx.when(() -> DataGeneratorTemplateContext.computeDataGeneratorContext(any()))
                    .thenReturn(Map.of());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("test/unit/mapper/mapper-test-template.ftl"), anyMap()))
                    .thenReturn("// TEST BODY");

            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        written.add(inv.getArgument(2, String.class));
                        return null;
                    });

            gen.generate(user, "src/main/java/");

            assertTrue(written.contains("UserRestMapperTest"));
            assertTrue(written.contains("AddressRestMapperTest"));
            assertTrue(written.contains("UserGraphQlMapperTest"));
            assertTrue(written.contains("AddressGraphQlMapperTest"));
        }
    }
}
