package com.markozivkovic.codegen.plugins;

import java.io.File;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.markozivkovic.codegen.generators.SpringCrudGenerator;
import com.markozivkovic.codegen.generators.tests.SpringCrudTestGenerator;
import com.markozivkovic.codegen.models.CrudSpecification;
import com.markozivkovic.codegen.models.ProjectMetadata;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CrudGeneratorMojo extends AbstractMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrudGeneratorMojo.class);
    
    @Parameter(property = "inputSpecFile", required = true)
    private String inputSpecFile;

    @Parameter(property = "outputDir", required = true)
    private String outputDir;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true)
    private String artifactId;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String version;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File projectBaseDir;

    public void execute() throws MojoExecutionException {

        if (Objects.isNull(inputSpecFile)) {
            throw new MojoExecutionException("inputSpecFile must be specified");
        }
        
        if (Objects.isNull(outputDir)) {
            throw new MojoExecutionException("outputDir must be specified");
        }
        
        try {
            final ObjectMapper mapper = this.createSpecMapper(inputSpecFile);
            
            LOGGER.info("Generator started for file: {}", inputSpecFile);

            final CrudSpecification spec = mapper.readValue(new File(inputSpecFile), CrudSpecification.class);
            final ProjectMetadata projectMetadata = new ProjectMetadata(artifactId, version, projectBaseDir.getAbsolutePath());
            final SpringCrudGenerator generator = new SpringCrudGenerator(spec.getConfiguration(), spec.getEntities(), projectMetadata);
            final SpringCrudTestGenerator testGenerator = new SpringCrudTestGenerator(spec.getConfiguration(), spec.getEntities());

            spec.getEntities().stream().forEach(entity -> {
                    generator.generate(entity, outputDir);
            });

            spec.getEntities().stream().forEach(entity -> {
                    testGenerator.generate(entity, outputDir);
            });

            LOGGER.info("Generator finished for file: {}", inputSpecFile);
        } catch (final Exception e) {
            throw new MojoExecutionException("Code generation failed", e);
        }
    }

    /**
     * Creates an {@link ObjectMapper} based on the file extension of the inputSpecFile.
     * Supported file formats are: .yaml, .yml, .json
     *
     * @param inputSpecFile the input spec file
     * @return an {@link ObjectMapper} based on the file extension of the inputSpecFile
     * @throws IllegalArgumentException if the file format is not supported
     */
    private ObjectMapper createSpecMapper(final String inputSpecFile) {

        final String specFile = inputSpecFile.toLowerCase().trim();

        if (specFile.endsWith(".yaml") || specFile.endsWith(".yml")) {
            return YAMLMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                    .build();
        } else if (specFile.endsWith(".json")) {
            return JsonMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                    .build();
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported file format: %s. Supported file formats are: .yaml, .yml, .json",
                    specFile
            ));
        }
    }

}
