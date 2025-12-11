package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.ApplicationDockerConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.DatabaseType;
import com.markozivkovic.codegen.models.CrudConfiguration.DbDockerConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.DockerConfiguration;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.DockerUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;

class DockerGeneratorTest {

    private static class Env {
        CrudConfiguration config;
        DockerConfiguration dockerCfg;
        ProjectMetadata metadata;
        DockerGenerator generator;
    }

    private Env prepareEnv() {
        
        final Env env = new Env();
        env.config = mock(CrudConfiguration.class);
        env.dockerCfg = mock(DockerConfiguration.class);
        when(env.config.getDocker()).thenReturn(env.dockerCfg);

        env.metadata = mock(ProjectMetadata.class);
        when(env.metadata.getArtifactId()).thenReturn("my-app");
        when(env.metadata.getVersion()).thenReturn("1.0.0");
        when(env.metadata.getProjectBaseDir()).thenReturn("/tmp/project");

        env.generator = new DockerGenerator(env.config, env.metadata);
        return env;
    }

    @Test
    void generate_shouldSkipWhenDockerNotEnabled() {
        
        final Env env = prepareEnv();

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg))
                    .thenReturn(false);

            env.generator.generate("out");

            dockerUtils.verify(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg));
            genCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateDockerfileAndComposeWithDefaultsForPostgres() {
        
        final Env env = prepareEnv();

        when(env.config.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(env.config.getJavaVersion()).thenReturn(null);
        when(env.dockerCfg.getDockerCompose()).thenReturn(true);
        when(env.dockerCfg.getDb()).thenReturn(null);
        when(env.dockerCfg.getApp()).thenReturn(null);

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg))
                    .thenReturn(true);

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE))
                    .thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE))
                    .thenReturn(false);

            final AtomicReference<Map<String, Object>> dockerfileCtxRef = new AtomicReference<>();
            final AtomicReference<Map<String, Object>> composeCtxRef = new AtomicReference<>();

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("docker/dockerfile-template.ftl"),
                    anyMap()
            )).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = invocation.getArgument(1, Map.class);
                dockerfileCtxRef.set(ctx);
                return "DOCKERFILE_CONTENT";
            });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("docker/docker-compose-template.ftl"),
                    anyMap()
            )).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = invocation.getArgument(1, Map.class);
                composeCtxRef.set(ctx);
                return "DOCKER_COMPOSE_CONTENT";
            });

            final AtomicReference<String> writtenDockerfile = new AtomicReference<>();
            final AtomicReference<String> writtenCompose = new AtomicReference<>();

            writer.when(() -> FileWriterUtils.writeToFile(
                    eq("/tmp/project"),
                    eq("Dockerfile"),
                    anyString()
            )).thenAnswer(invocation -> {
                writtenDockerfile.set(invocation.getArgument(2, String.class));
                return null;
            });

            writer.when(() -> FileWriterUtils.writeToFile(
                    eq("/tmp/project"),
                    eq("docker-compose.yml"),
                    anyString()
            )).thenAnswer(invocation -> {
                writtenCompose.set(invocation.getArgument(2, String.class));
                return null;
            });

            env.generator.generate("out");

            final Map<String, Object> dfCtx = dockerfileCtxRef.get();
            assertNotNull(dfCtx);
            assertEquals("my-app", dfCtx.get("artifactId"));
            assertEquals("1.0.0", dfCtx.get("version"));
            assertEquals("eclipse-temurin", dfCtx.get("baseImage"));
            assertEquals("17", dfCtx.get("javaVersion"));
            assertEquals("8080", dfCtx.get("port"));

            final Map<String, Object> dcCtx = composeCtxRef.get();
            assertNotNull(dcCtx);
            assertEquals("my-app", dcCtx.get("artifactId"));
            assertEquals("postgresql", dcCtx.get("dbType")); // name().toLowerCase()
            assertEquals("8080", dcCtx.get("appPort"));
            assertEquals(5432, dcCtx.get("dbPort"));
            assertEquals("postgres", dcCtx.get("dbImage"));
            assertFalse(dcCtx.containsKey("dbTag"));

            assertEquals("DOCKERFILE_CONTENT", writtenDockerfile.get());
            assertEquals("DOCKER_COMPOSE_CONTENT", writtenCompose.get());

            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE));
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE));
        }
    }

    @Test
    void generate_shouldRespectCustomDbAndAppSettings() {
        
        final Env env = prepareEnv();

        when(env.config.getDatabase()).thenReturn(DatabaseType.MYSQL);
        when(env.config.getJavaVersion()).thenReturn(17);

        final DbDockerConfiguration dbCfg = mock(DbDockerConfiguration.class);
        when(dbCfg.getImage()).thenReturn("custom/mysql");
        when(dbCfg.getPort()).thenReturn(3333);
        when(dbCfg.getTag()).thenReturn("8.1");

        final ApplicationDockerConfiguration appCfg = mock(ApplicationDockerConfiguration.class);
        when(appCfg.getImage()).thenReturn("custom/jre");
        when(appCfg.getPort()).thenReturn(9090);
        when(appCfg.getTag()).thenReturn("v1");

        when(env.dockerCfg.getDockerCompose()).thenReturn(true);
        when(env.dockerCfg.getDb()).thenReturn(dbCfg);
        when(env.dockerCfg.getApp()).thenReturn(appCfg);

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg))
                    .thenReturn(true);

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE))
                    .thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE))
                    .thenReturn(false);

            AtomicReference<Map<String, Object>> dockerfileCtxRef = new AtomicReference<>();
            AtomicReference<Map<String, Object>> composeCtxRef = new AtomicReference<>();

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("docker/dockerfile-template.ftl"),
                    anyMap()
            )).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = invocation.getArgument(1, Map.class);
                dockerfileCtxRef.set(ctx);
                return "DOCKERFILE";
            });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("docker/docker-compose-template.ftl"),
                    anyMap()
            )).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = invocation.getArgument(1, Map.class);
                composeCtxRef.set(ctx);
                return "COMPOSE";
            });

            env.generator.generate("out");

            final Map<String, Object> dfCtx = dockerfileCtxRef.get();
            assertNotNull(dfCtx);
            assertEquals("my-app", dfCtx.get("artifactId"));
            assertEquals("1.0.0", dfCtx.get("version"));
            assertEquals("custom/jre", dfCtx.get("baseImage"));
            assertEquals(17, dfCtx.get("javaVersion"));
            assertEquals(9090, dfCtx.get("port"));
            assertEquals("v1", dfCtx.get("tag"));

            final Map<String, Object> dcCtx = composeCtxRef.get();
            assertNotNull(dcCtx);
            assertEquals("my-app", dcCtx.get("artifactId"));
            assertEquals("mysql", dcCtx.get("dbType"));
            assertEquals(9090, dcCtx.get("appPort"));
            assertEquals(3333, dcCtx.get("dbPort"));
            assertEquals("custom/mysql", dcCtx.get("dbImage"));
            assertEquals("8.1", dcCtx.get("dbTag"));
        }
    }

    @Test
    void generate_shouldSkipDockerfileWhenAlreadyGenerated() {
        
        final Env env = prepareEnv();

        when(env.config.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(env.config.getJavaVersion()).thenReturn(null);
        when(env.dockerCfg.getDockerCompose()).thenReturn(true);
        when(env.dockerCfg.getDb()).thenReturn(null);
        when(env.dockerCfg.getApp()).thenReturn(null);

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg))
                    .thenReturn(true);

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE))
                    .thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE))
                    .thenReturn(false);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("docker/docker-compose-template.ftl"),
                    anyMap()
            )).thenReturn("COMPOSE");

            env.generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("docker/dockerfile-template.ftl"),
                    anyMap()
            ), never());

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("docker/docker-compose-template.ftl"),
                    anyMap()
            ));

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("/tmp/project"),
                    eq("docker-compose.yml"),
                    eq("COMPOSE")
            ));
            writer.verifyNoMoreInteractions();
        }
    }
    
}
