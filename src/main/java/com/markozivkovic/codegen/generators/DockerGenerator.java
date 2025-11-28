package com.markozivkovic.codegen.generators;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.DockerUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.StringUtils;

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
        
        if (!DockerUtils.isDockerfileEnabled(configuration.getDocker())) {
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

        if (!Boolean.TRUE.equals(this.configuration.getDocker().getDockerCompose())) { return; }
        
        LOGGER.info("Generating Docker-compose");

        final int defaultDbPort = switch (this.configuration.getDatabase()) {
            case POSTGRESQL -> 5432;
            case MYSQL -> 3306;
            case MSSQL -> 1433;
        };

        final String defaultDbImage = switch (this.configuration.getDatabase()) {
            case POSTGRESQL -> "postgres";
            case MYSQL -> "mysql";
            case MSSQL -> "mcr.microsoft.com/mssql/server";
        };

        final Map<String, Object> context = new HashMap<>(Map.of(
            "artifactId", projectMetadata.getArtifactId(),
            "dbType", this.configuration.getDatabase().name().toLowerCase(),
            "appPort", "8080",
            "dbPort", defaultDbPort,
            "dbImage", defaultDbImage
        ));

        if (Objects.nonNull(this.configuration.getDocker().getDb())) {
            if (StringUtils.isNotBlank(this.configuration.getDocker().getDb().getImage())) {
                context.put("dbImage", this.configuration.getDocker().getDb().getImage());
            }

            if (Objects.nonNull(this.configuration.getDocker().getDb().getPort())) {
                context.put("dbPort", this.configuration.getDocker().getDb().getPort());
            }

            if (StringUtils.isNotBlank(this.configuration.getDocker().getDb().getTag())) {
                context.put("dbTag", this.configuration.getDocker().getDb().getTag());
            }
        }

        if (Objects.nonNull(this.configuration.getDocker().getApp())) {
            if (Objects.nonNull(this.configuration.getDocker().getApp().getPort())) {
                context.put("appPort", this.configuration.getDocker().getApp().getPort());
            }
        }

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

        final Map<String, Object> context = new HashMap<>(Map.of(
            "artifactId", projectMetadata.getArtifactId(),
            "version", projectMetadata.getVersion(),
            "baseImage", "eclipse-temurin",
            "javaVersion", Objects.isNull(this.configuration.getJavaVersion()) ? "17" : this.configuration.getJavaVersion(),
            "port", "8080"
        ));

        if (Objects.nonNull(this.configuration.getDocker().getApp())) {
            if (StringUtils.isNotBlank(this.configuration.getDocker().getApp().getImage())) {
                context.put("baseImage", this.configuration.getDocker().getApp().getImage());
            }

            if (Objects.nonNull(this.configuration.getDocker().getApp().getPort())) {
                context.put("port", this.configuration.getDocker().getApp().getPort());
            }

            if (StringUtils.isNotBlank(this.configuration.getDocker().getApp().getTag())) {
                context.put("tag", this.configuration.getDocker().getApp().getTag());
            }
        }
        
        final String dockerFile = FreeMarkerTemplateProcessorUtils.processTemplate("docker/dockerfile-template.ftl", context);

        FileWriterUtils.writeToFile(projectMetadata.getProjectBaseDir(), "Dockerfile", dockerFile);

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE);
        
        LOGGER.info("Finished generating Dockerfile");
    }

}
