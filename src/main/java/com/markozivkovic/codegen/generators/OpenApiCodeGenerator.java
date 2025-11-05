package com.markozivkovic.codegen.generators;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiCodeGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiCodeGenerator.class);
    
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

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN)) {
            return;
        }

        LOGGER.info("Generating OpenAPI code");

        this.generateOpenApiGeneratorIgnore();
        
        final String pathToSwaggerDocs = String.format("%s/%s", projectMetadata.getProjectBaseDir(), GeneratorConstants.SRC_MAIN_RESOURCES_SWAGGER);

        entities.stream()
            .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
            .forEach(e -> {
                
                final String strippedModelName = ModelNameUtils.stripSuffix(e.getName());
                final String apiSpecPath = String.format("%s/%s-api.yaml", pathToSwaggerDocs, StringUtils.uncapitalize(strippedModelName));
                final Path apiSpecFilePath = Paths.get(apiSpecPath);
                final String specUri = apiSpecFilePath.toUri().toString();

                final ParseOptions parseOptions = new ParseOptions();
                parseOptions.setResolve(true);
                parseOptions.setResolveFully(false);
                parseOptions.setFlatten(false);

                final SwaggerParseResult pr = new OpenAPIV3Parser()
                        .readLocation(specUri, null, parseOptions);
                
                if (Objects.isNull(pr) || Objects.isNull(pr.getOpenAPI())) {
                    final var msgs = (pr != null && pr.getMessages() != null) ?
                            String.join("\n", pr.getMessages()) : "(no parser messages)";
                    throw new IllegalStateException("OpenAPI parse failed for: " + specUri + "\n" + msgs);
                }
                
                final String outputPath = String.format("%s/generated/%s", outputDir, StringUtils.uncapitalize(strippedModelName));
                final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputPath);
                
                final CodegenConfigurator cfg = new CodegenConfigurator()
                        .setInputSpec(specUri)
                        .setGeneratorName("spring")
                        .setLibrary("spring-boot")
                        .setOutputDir(projectMetadata.getProjectBaseDir())
                        .setApiPackage(String.format("%s.api", packagePath))
                        .setModelPackage(String.format("%s.model", packagePath));

                cfg.addAdditionalProperty("useSpringBoot3", true);
                cfg.addAdditionalProperty("interfaceOnly", true);
                cfg.addAdditionalProperty("hideGenerationTimestamp", true);

                final ClientOptInput opts = cfg.toClientOptInput();
                opts.openAPI(pr.getOpenAPI());
                new DefaultGenerator().opts(opts).generate();
            });

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.OPENAPI_CODEGEN);

        LOGGER.info("OpenAPI code generation completed");
    }

    /**
     * Writes a .openapi-generator-ignore file to the project base directory.
     * This file tells the OpenAPI Generator to ignore the pom.xml file.
     */
    private void generateOpenApiGeneratorIgnore() {

        final String fileContent = "pom.xml";
        
        FileWriterUtils.writeToFile(
                projectMetadata.getProjectBaseDir(),
                GeneratorConstants.OPEN_API_GENERATOR_IGNORE,
                fileContent
        );
    }
    
}
