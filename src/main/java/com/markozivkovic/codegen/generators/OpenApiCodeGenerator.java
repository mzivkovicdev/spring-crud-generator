package com.markozivkovic.codegen.generators;

import java.util.List;
import java.util.Objects;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.model.CrudConfiguration;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.model.ProjectMetadata;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiCodeGenerator implements CodeGenerator {

    private static final String OPENAPI_CODEGEN = "openapi-codegen";

    private static final String SRC_MAIN_RESOURCES_SWAGGER = "src/main/resources/swagger";
    
    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;
    private final List<ModelDefinition> entities;
    
    public OpenApiCodeGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata,
                                final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
    
        if (Objects.isNull(configuration) || Objects.isNull(configuration.getOpenApiCodegen()) || !configuration.getOpenApiCodegen()) {
            return;
        }

        if (GeneratorContext.isGenerated(OPENAPI_CODEGEN)) {
            return;
        }

        final String pathToSwaggerDocs = String.format("%s/%s", projectMetadata.getProjectBaseDir(), SRC_MAIN_RESOURCES_SWAGGER);

        entities.stream()
            .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
            .forEach(e -> {
                
                final String strippedModelName = ModelNameUtils.stripSuffix(e.getName());
                final String apiSpecPath = String.format("%s/%s-api.yaml", pathToSwaggerDocs, StringUtils.uncapitalize(strippedModelName));
                final SwaggerParseResult parsed = new OpenAPIV3Parser()
                        .readLocation(apiSpecPath, null, null);

                if (parsed.getOpenAPI() == null) {
                    throw new IllegalStateException(
                        String.format(
                            "OpenAPI parse failed for %s -> %s", apiSpecPath, parsed.getMessages()
                        )
                    );
                }

                final String outputPath = String.format("%s/generated/%s", outputDir, StringUtils.uncapitalize(strippedModelName));
                final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputPath);
                final CodegenConfigurator cfg = new CodegenConfigurator()
                        .setInputSpec(apiSpecPath)
                        .setGeneratorName("spring")
                        .setOutputDir(outputPath)
                        .setModelNamePrefix(StringUtils.uncapitalize(strippedModelName))
                        .setApiPackage(String.format("%s.api", packagePath))
                        .setModelPackage(String.format("%s.model", packagePath));

                ClientOptInput opts = cfg.toClientOptInput();
                opts.openAPI(parsed.getOpenAPI());
                new DefaultGenerator().opts(opts).generate();
            });

        GeneratorContext.markGenerated(OPENAPI_CODEGEN);
    }
    
}
