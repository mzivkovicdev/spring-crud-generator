package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ApplicationDockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DbDockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.utils.DockerUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;

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
        when(env.config.getCache()).thenReturn(null);

        final List<Map<String, Object>> dockerfileContexts = new ArrayList<>();
        final List<Map<String, Object>> composeContexts = new ArrayList<>();
        final List<String> writtenDockerfiles = new ArrayList<>();
        final List<String> writtenComposes = new ArrayList<>();

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

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/dockerfile-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        dockerfileContexts.add(new HashMap<>(ctx));
                        return "DOCKERFILE_CONTENT";
                    });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/docker-compose-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        composeContexts.add(new HashMap<>(ctx));
                        return "DOCKER_COMPOSE_CONTENT";
                    });

            writer.when(() -> FileWriterUtils.writeToFile(eq("/tmp/project"), eq("Dockerfile"), anyString()))
                    .thenAnswer(inv -> { writtenDockerfiles.add(inv.getArgument(2, String.class)); return null; });

            writer.when(() -> FileWriterUtils.writeToFile(eq("/tmp/project"), eq("docker-compose.yml"), anyString()))
                    .thenAnswer(inv -> { writtenComposes.add(inv.getArgument(2, String.class)); return null; });

            env.generator.generate("out");

            assertEquals(1, dockerfileContexts.size());
            final Map<String, Object> dfCtx = dockerfileContexts.get(0);
            assertEquals("my-app", dfCtx.get("artifactId"));
            assertEquals("1.0.0", dfCtx.get("version"));
            assertEquals("eclipse-temurin", dfCtx.get("baseImage"));
            assertEquals("17", dfCtx.get("javaVersion"));
            assertEquals("8080", dfCtx.get("port"));
            assertFalse(dfCtx.containsKey("tag"));

            assertEquals(1, composeContexts.size());
            final Map<String, Object> dcCtx = composeContexts.get(0);
            assertEquals("my-app", dcCtx.get("artifactId"));
            assertEquals("postgresql", dcCtx.get("dbType"));
            assertEquals("8080", dcCtx.get("appPort"));
            assertEquals(5432, dcCtx.get("dbPort"));
            assertEquals("postgres", dcCtx.get("dbImage"));
            assertFalse(dcCtx.containsKey("dbTag"));
            assertFalse(dcCtx.containsKey("cacheType"));

            assertEquals(List.of("DOCKERFILE_CONTENT"), writtenDockerfiles);
            assertEquals(List.of("DOCKER_COMPOSE_CONTENT"), writtenComposes);

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
        when(env.config.getCache()).thenReturn(null);

        final List<Map<String, Object>> dockerfileContexts = new ArrayList<>();
        final List<Map<String, Object>> composeContexts = new ArrayList<>();

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg)).thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE)).thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE)).thenReturn(false);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/dockerfile-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        dockerfileContexts.add(new HashMap<>(ctx));
                        return "DOCKERFILE";
                    });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/docker-compose-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        composeContexts.add(new HashMap<>(ctx));
                        return "COMPOSE";
                    });

            env.generator.generate("out");

            assertEquals(1, dockerfileContexts.size());
            final Map<String, Object> dfCtx = dockerfileContexts.get(0);
            assertEquals("my-app", dfCtx.get("artifactId"));
            assertEquals("1.0.0", dfCtx.get("version"));
            assertEquals("custom/jre", dfCtx.get("baseImage"));
            assertEquals(17, dfCtx.get("javaVersion"));
            assertEquals(9090, dfCtx.get("port"));
            assertEquals("v1", dfCtx.get("tag"));

            assertEquals(1, composeContexts.size());
            final Map<String, Object> dcCtx = composeContexts.get(0);
            assertEquals("my-app", dcCtx.get("artifactId"));
            assertEquals("mysql", dcCtx.get("dbType"));
            assertEquals(9090, dcCtx.get("appPort"));
            assertEquals(3333, dcCtx.get("dbPort"));
            assertEquals("custom/mysql", dcCtx.get("dbImage"));
            assertEquals("8.1", dcCtx.get("dbTag"));
            assertFalse(dcCtx.containsKey("cacheType"));
        }
    }

    @Test
    void generate_shouldIncludeCacheTypeInComposeContext_whenCacheEnabled() {

        final Env env = prepareEnv();

        when(env.config.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(env.config.getJavaVersion()).thenReturn(null);

        when(env.dockerCfg.getDockerCompose()).thenReturn(true);
        when(env.dockerCfg.getDb()).thenReturn(null);
        when(env.dockerCfg.getApp()).thenReturn(null);

        final CacheConfiguration cacheCfg = mock(CacheConfiguration.class);
        when(cacheCfg.getEnabled()).thenReturn(true);
        when(cacheCfg.getType()).thenReturn(CacheTypeEnum.REDIS);
        when(env.config.getCache()).thenReturn(cacheCfg);

        final List<Map<String, Object>> composeContexts = new ArrayList<>();

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg)).thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE)).thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE)).thenReturn(false);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/docker-compose-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        composeContexts.add(new HashMap<>(ctx));
                        return "COMPOSE";
                    });

            env.generator.generate("out");

            assertEquals(1, composeContexts.size());
            assertEquals("redis", composeContexts.get(0).get("cacheType"));
        }
    }

    @Test
    void generate_shouldSkipDockerCompose_whenDockerComposeDisabled() {

        final Env env = prepareEnv();

        when(env.config.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(env.dockerCfg.getDockerCompose()).thenReturn(false);

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg)).thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE)).thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE)).thenReturn(false);

            env.generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/docker-compose-template.ftl"), anyMap()), never());
            writer.verifyNoInteractions();
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE), never());
        }
    }

    @Test
    void generate_shouldSkipDockerfileWhenAlreadyGenerated_butGenerateCompose() {

        final Env env = prepareEnv();

        when(env.config.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(env.config.getJavaVersion()).thenReturn(null);

        when(env.dockerCfg.getDockerCompose()).thenReturn(true);
        when(env.dockerCfg.getDb()).thenReturn(null);
        when(env.dockerCfg.getApp()).thenReturn(null);
        when(env.config.getCache()).thenReturn(null);

        try (final MockedStatic<DockerUtils> dockerUtils = mockStatic(DockerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            dockerUtils.when(() -> DockerUtils.isDockerfileEnabled(env.dockerCfg)).thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_FILE)).thenReturn(true);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.DOCKER_COMPOSE)).thenReturn(false);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/docker-compose-template.ftl"), anyMap())).thenReturn("COMPOSE");

            env.generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("docker/dockerfile-template.ftl"), anyMap()), never());

            writer.verify(() -> FileWriterUtils.writeToFile(eq("/tmp/project"), eq("docker-compose.yml"), eq("COMPOSE")));
            writer.verifyNoMoreInteractions();
        }
    }
    
}
