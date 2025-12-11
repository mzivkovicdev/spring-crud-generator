package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

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

        final OpenApiCodeGenerator generator =
                new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr = mockConstruction(OpenAPIV3Parser.class);
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class)) {

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

        when(cfg.getOpenApiCodegen()).thenReturn(false);
        when(cfg.getSwagger()).thenReturn(true);

        final OpenApiCodeGenerator generator =
                new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr = mockConstruction(OpenAPIV3Parser.class);
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class)) {

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

        when(cfg.getOpenApiCodegen()).thenReturn(true);
        when(cfg.getSwagger()).thenReturn(true);

        final OpenApiCodeGenerator generator =
                new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr = mockConstruction(OpenAPIV3Parser.class);
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN))
               .thenReturn(true);

            generator.generate("out");

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateOpenApiCodeForEntitiesWithIdField_andPackageConfiguration() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition userEntity = newModel("UserEntity", List.of(idField));
        final ModelDefinition noIdEntity = newModel("OtherEntity", List.of());
        final List<ModelDefinition> entities = List.of(userEntity, noIdEntity);

        when(cfg.getOpenApiCodegen()).thenReturn(true);
        when(cfg.getSwagger()).thenReturn(true);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");
        when(pkgCfg.getGenerated()).thenReturn("generated-src");

        final OpenApiCodeGenerator generator =
                new OpenApiCodeGenerator(cfg, projectMetadata, entities, pkgCfg);

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

                         final ClientOptInput clientOptInput = mock(ClientOptInput.class);
                         when(mockCfgObj.toClientOptInput()).thenReturn(clientOptInput);
                     });
             final MockedConstruction<DefaultGenerator> genConstr =
                     mockConstruction(DefaultGenerator.class, (mockGen, constructionCtx) -> {
                         when(mockGen.opts(any(ClientOptInput.class))).thenReturn(mockGen);
                         when(mockGen.generate()).thenReturn(List.of());
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN))
               .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields()))
                      .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(noIdEntity.getFields()))
                      .thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(
                    "out/generated-src/user"))
               .thenReturn("com.example.generated.user");

            generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    "/tmp/project",
                    GeneratorConstants.OPEN_API_GENERATOR_IGNORE,
                    "pom.xml"
            ));

            ctx.verify(() -> GeneratorContext.markGenerated(
                    GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN));
        }
    }

    @Test
    void generate_shouldThrowWhenOpenApiParseFails() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition userEntity = newModel("UserEntity", List.of(idField));
        final List<ModelDefinition> entities = List.of(userEntity);

        when(cfg.getOpenApiCodegen()).thenReturn(true);
        when(cfg.getSwagger()).thenReturn(true);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final OpenApiCodeGenerator generator =
                new OpenApiCodeGenerator(cfg, projectMetadata, entities, null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedConstruction<OpenAPIV3Parser> parserConstr =
                     mockConstruction(OpenAPIV3Parser.class, (mockParser, constructionCtx) -> {
                         final SwaggerParseResult pr = mock(SwaggerParseResult.class);
                         when(pr.getOpenAPI()).thenReturn(null);
                         when(pr.getMessages()).thenReturn(List.of("Spec invalid", "Missing paths"));
                         when(mockParser.readLocation(anyString(), eq(null), any(ParseOptions.class)))
                                 .thenReturn(pr);
                     });
             final MockedConstruction<DefaultGenerator> genConstr = mockConstruction(DefaultGenerator.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN))
               .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields()))
                      .thenReturn(true);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");

            assertThrows(IllegalStateException.class,
                    () -> generator.generate("out"));

            writer.verify(() -> FileWriterUtils.writeToFile(
                    "/tmp/project",
                    GeneratorConstants.OPEN_API_GENERATOR_IGNORE,
                    "pom.xml"
            ));
        }
    }
}
