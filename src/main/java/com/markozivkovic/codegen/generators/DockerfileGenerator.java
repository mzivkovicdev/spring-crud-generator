package com.markozivkovic.codegen.generators;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;

public class DockerfileGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerfileGenerator.class);
    private static final String DOCKERFILE = "dockerfile";

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;

    public DockerfileGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
    }
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (GeneratorContext.isGenerated(DOCKERFILE)) { return; }

        LOGGER.info("Generating Dockerfile");
        
        if (Objects.isNull(configuration) || Objects.isNull(configuration.getDockerfile()) || !configuration.getDockerfile()) {
            return;
        }

        final Map<String, Object> context = Map.of(
            "artifactId", projectMetadata.getArtifactId(),
            "version", projectMetadata.getVersion()
        );
        
        final String dockerFile = FreeMarkerTemplateProcessorUtils.processTemplate("docker/dockerfile-template.ftl", context);

        FileWriterUtils.writeToFile(projectMetadata.getProjectBaseDir(), "Dockerfile", dockerFile);

        GeneratorContext.markGenerated(DOCKERFILE);
        
        LOGGER.info("Finished generating Dockerfile");
    }

}
