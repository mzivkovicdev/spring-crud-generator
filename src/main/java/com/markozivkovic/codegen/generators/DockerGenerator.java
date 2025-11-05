package com.markozivkovic.codegen.generators;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;

public class DockerGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerGenerator.class);

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;

    public DockerGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
    }
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (Objects.isNull(configuration) || Objects.isNull(configuration.getDocker()) ||
                Objects.isNull(configuration.getDocker().getDockerfile()) || !configuration.getDocker().getDockerfile()) {
            return;
        }

        this.generateDockerfile();
        this.generateDockerCompose();
    }

    /**
     * Generates a Docker-compose file based on the project's artifact ID and database type.
     * This method will only generate the Docker-compose file if the configuration specifies
     * that Docker should be used and if the Docker compose flag is set to true.
     */
    private void generateDockerCompose() {
        
        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE)) { return; }

        if (Objects.isNull(this.configuration.getDocker().getDockerCompose()) || !this.configuration.getDocker().getDockerCompose()) { return; }
        
        LOGGER.info("Generating Docker-compose");

        final Map<String, Object> context = Map.of(
            "artifactId", projectMetadata.getArtifactId(),
            "dbType", this.configuration.getDatabase().name().toLowerCase()
        );

        final String dockerCompose = FreeMarkerTemplateProcessorUtils.processTemplate("docker/docker-compose-template.ftl", context);

        FileWriterUtils.writeToFile(projectMetadata.getProjectBaseDir(), "docker-compose.yml", dockerCompose);

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE);
        
        LOGGER.info("Finished generating Docker-compose");
    }

    /**
     * Generates a Dockerfile based on the project's artifact ID and version.
     */
    private void generateDockerfile() {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE)) { return; }
        
        LOGGER.info("Generating Dockerfile");

        final Map<String, Object> context = Map.of(
            "artifactId", projectMetadata.getArtifactId(),
            "version", projectMetadata.getVersion()
        );
        
        final String dockerFile = FreeMarkerTemplateProcessorUtils.processTemplate("docker/dockerfile-template.ftl", context);

        FileWriterUtils.writeToFile(projectMetadata.getProjectBaseDir(), "Dockerfile", dockerFile);

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE);
        
        LOGGER.info("Finished generating Dockerfile");
    }

}
