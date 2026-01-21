package dev.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.models.ProjectMetadata;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class OpenApiCodeGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenConfigurationIsNull() {

        final CrudConfiguration cfg = null;
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final OpenApiCodeGenerator generator = new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr = mockConstruction(OpenAPIV3Parser.class);
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class);
             final MockedConstruction<CodegenConfigurator> cfgConstr = mockConstruction(CodegenConfigurator.class)) {

            generator.generate("out");

            ctx.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenOpenApiOrSwaggerDisabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final CrudConfiguration.OpenApiDefinition openApiDef = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApiDef);
        when(openApiDef.getApiSpec()).thenReturn(false);
        when(openApiDef.getGenerateResources()).thenReturn(false);

        final OpenApiCodeGenerator generator = new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr = mockConstruction(OpenAPIV3Parser.class);
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class);
             final MockedConstruction<CodegenConfigurator> cfgConstr = mockConstruction(CodegenConfigurator.class)) {

            generator.generate("out");

            ctx.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenAlreadyGeneratedInContext() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final CrudConfiguration.OpenApiDefinition openApiDef = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApiDef);
        when(openApiDef.getApiSpec()).thenReturn(true);
        when(openApiDef.getGenerateResources()).thenReturn(true);

        final OpenApiCodeGenerator generator = new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr = mockConstruction(OpenAPIV3Parser.class);
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class);
             final MockedConstruction<CodegenConfigurator> cfgConstr = mockConstruction(CodegenConfigurator.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN))
               .thenReturn(true);

            generator.generate("out");

            writer.verifyNoInteractions();
            assertEquals(0, parserConstr.constructed().size());
            assertEquals(0, genConstr.constructed().size());
            assertEquals(0, cfgConstr.constructed().size());
        }
    }

    @Test
    void generate_shouldGenerateOpenApiCodeForEntitiesWithIdField_andPackageConfiguration() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);

        final ModelDefinition userEntity = newModel("UserEntity", List.of(idField));
        final ModelDefinition otherEntityNoId = newModel("OtherEntity", List.of());

        final List<ModelDefinition> entities = List.of(userEntity, otherEntityNoId);

        final CrudConfiguration.OpenApiDefinition openApiDef = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApiDef);
        when(openApiDef.getApiSpec()).thenReturn(true);
        when(openApiDef.getGenerateResources()).thenReturn(true);

        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");
        when(pkgCfg.getGenerated()).thenReturn("generated-src");

        final OpenApiCodeGenerator generator = new OpenApiCodeGenerator(cfg, projectMetadata, entities, pkgCfg);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr =
                     mockConstruction(OpenAPIV3Parser.class, (mockParser, constructionCtx) -> {
                         final SwaggerParseResult pr = mock(SwaggerParseResult.class);
                         final OpenAPI openApi = mock(OpenAPI.class);
                         when(pr.getOpenAPI()).thenReturn(openApi);

                         when(mockParser.readLocation(anyString(), eq(null), any(ParseOptions.class)))
                                 .thenReturn(pr);
                     });
             final MockedConstruction<CodegenConfigurator> cfgConstr = mockConstruction(CodegenConfigurator.class, (mockCfgObj, constructionCtx) -> {
                         when(mockCfgObj.setInputSpec(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setGeneratorName(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setLibrary(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setOutputDir(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setApiPackage(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setModelPackage(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.addAdditionalProperty(anyString(), any())).thenReturn(mockCfgObj);
                         when(mockCfgObj.addTypeMapping(anyString(), anyString())).thenReturn(mockCfgObj);

                         final ClientOptInput clientOptInput = mock(ClientOptInput.class);
                         when(mockCfgObj.toClientOptInput()).thenReturn(clientOptInput);
                     });
             final MockedConstruction<DefaultGenerator> genConstr =
                     mockConstruction(DefaultGenerator.class, (mockGen, constructionCtx) -> {
                         when(mockGen.opts(any(ClientOptInput.class))).thenReturn(mockGen);
                         when(mockGen.generate()).thenReturn(List.of());
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(otherEntityNoId.getFields())).thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("OtherEntity")).thenReturn("Other");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out/generated-src/user")).thenReturn("com.example.generated.user");

            generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    "/tmp/project", GeneratorConstants.OPEN_API_GENERATOR_IGNORE, "pom.xml"
            ));

            assertEquals(1, parserConstr.constructed().size());
            assertEquals(1, cfgConstr.constructed().size());
            assertEquals(1, genConstr.constructed().size());

            final CodegenConfigurator usedCfg = cfgConstr.constructed().get(0);

            verify(usedCfg).addAdditionalProperty("useSpringBoot3", true);
            verify(usedCfg).addAdditionalProperty("interfaceOnly", true);
            verify(usedCfg).addAdditionalProperty("hideGenerationTimestamp", true);
            verify(usedCfg).addTypeMapping("UserInput0", "UserInput");
            verify(usedCfg).addTypeMapping("UserInput1", "UserInput");
            verify(usedCfg).addTypeMapping("OtherInput0", "OtherInput");
            verify(usedCfg).addTypeMapping("OtherInput1", "OtherInput");

            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN));
        }
    }

    @Test
    void generate_shouldGenerateOpenApiCodeForEntitiesWithIdField_whenPackageConfigurationIsNull() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition userEntity = newModel("UserEntity", List.of(idField));
        final List<ModelDefinition> entities = List.of(userEntity);

        final CrudConfiguration.OpenApiDefinition openApiDef = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApiDef);
        when(openApiDef.getApiSpec()).thenReturn(true);
        when(openApiDef.getGenerateResources()).thenReturn(true);

        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final OpenApiCodeGenerator generator = new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr =
                     mockConstruction(OpenAPIV3Parser.class, (mockParser, constructionCtx) -> {
                         final SwaggerParseResult pr = mock(SwaggerParseResult.class);
                         final OpenAPI openApi = mock(OpenAPI.class);
                         when(pr.getOpenAPI()).thenReturn(openApi);

                         when(mockParser.readLocation(anyString(), eq(null), any(ParseOptions.class)))
                                 .thenReturn(pr);
                     });
             final MockedConstruction<CodegenConfigurator> cfgConstr =
                     mockConstruction(CodegenConfigurator.class, (mockCfgObj, constructionCtx) -> {
                         when(mockCfgObj.setInputSpec(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setGeneratorName(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setLibrary(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setOutputDir(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setApiPackage(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.setModelPackage(anyString())).thenReturn(mockCfgObj);
                         when(mockCfgObj.addAdditionalProperty(anyString(), any())).thenReturn(mockCfgObj);
                         when(mockCfgObj.addTypeMapping(anyString(), anyString())).thenReturn(mockCfgObj);

                         final ClientOptInput clientOptInput = mock(ClientOptInput.class);
                         when(mockCfgObj.toClientOptInput()).thenReturn(clientOptInput);
                     });
             final MockedConstruction<DefaultGenerator> genConstr =
                     mockConstruction(DefaultGenerator.class, (mockGen, constructionCtx) -> {
                         when(mockGen.opts(any(ClientOptInput.class))).thenReturn(mockGen);
                         when(mockGen.generate()).thenReturn(List.of());
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields())).thenReturn(true);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out/generated/user")).thenReturn("com.example.generated.user");

            generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    "/tmp/project", GeneratorConstants.OPEN_API_GENERATOR_IGNORE, "pom.xml"
            ));

            assertEquals(1, genConstr.constructed().size());
            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN));
        }
    }

    @Test
    void generate_shouldThrowWhenOpenApiParseFails_andIncludeMessages() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition userEntity = newModel("UserEntity", List.of(idField));
        final List<ModelDefinition> entities = List.of(userEntity);

        final CrudConfiguration.OpenApiDefinition openApiDef = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApiDef);
        when(openApiDef.getApiSpec()).thenReturn(true);
        when(openApiDef.getGenerateResources()).thenReturn(true);

        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final OpenApiCodeGenerator generator = new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr =
                     mockConstruction(OpenAPIV3Parser.class, (mockParser, constructionCtx) -> {
                         final SwaggerParseResult pr = mock(SwaggerParseResult.class);
                         when(pr.getOpenAPI()).thenReturn(null);
                         when(pr.getMessages()).thenReturn(List.of("Spec invalid", "Missing paths"));
                         when(mockParser.readLocation(anyString(), eq(null), any(ParseOptions.class))).thenReturn(pr);
                     });
             final MockedConstruction<CodegenConfigurator> cfgConstr = mockConstruction(CodegenConfigurator.class);
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields())).thenReturn(true);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");

            final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> generator.generate("out"));

            assertTrue(ex.getMessage().contains("OpenAPI parse failed for:"), ex.getMessage());
            assertTrue(ex.getMessage().contains("Spec invalid"), ex.getMessage());
            assertTrue(ex.getMessage().contains("Missing paths"), ex.getMessage());

            writer.verify(() -> FileWriterUtils.writeToFile(
                    "/tmp/project", GeneratorConstants.OPEN_API_GENERATOR_IGNORE, "pom.xml"
            ));

            assertEquals(0, genConstr.constructed().size());
        }
    }
}
