package com.markozivkovic.codegen.generators;

import java.util.Map;
import java.util.Objects;

import com.markozivkovic.codegen.model.CrudConfiguration;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.model.ProjectMetadata;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;

public class DockerfileGenerator implements CodeGenerator {

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;

    public DockerfileGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
    }
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (Objects.isNull(configuration) || Objects.isNull(configuration.getDockerfile()) || !configuration.getDockerfile()) {
            return;
        }

        final Map<String, Object> context = Map.of(
            "artifactId", projectMetadata.getArtifactId(),
            "version", projectMetadata.getVersion()
        );
        
        final String dockerFile = FreeMarkerTemplateProcessorUtils.processTemplate("docker/dockerfile-template.ftl", context);

        FileWriterUtils.writeToFile(projectMetadata.getProjectBaseDir(), "Dockerfile", dockerFile);
    }

}
