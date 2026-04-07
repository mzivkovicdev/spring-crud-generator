package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.AiContextConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ApplicationDockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.GraphQLDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.OpenApiDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;

class AiContextGeneratorTest {

    private static class Env {
        CrudConfiguration config;
        AiContextConfiguration aiCfg;
        ProjectMetadata metadata;
        List<ModelDefinition> entities;
        AiContextGenerator generator;
    }

    private Env prepareEnv() {

        final Env env = new Env();
        env.config = mock(CrudConfiguration.class);
        env.aiCfg = mock(AiContextConfiguration.class);
        when(env.config.getAi()).thenReturn(env.aiCfg);
        when(env.config.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(env.config.getDocker()).thenReturn(null);
        when(env.config.getCache()).thenReturn(null);
        when(env.config.getOpenApi()).thenReturn(null);
        when(env.config.getGraphql()).thenReturn(null);
        when(env.config.isMigrationScripts()).thenReturn(null);
        when(env.config.getTests()).thenReturn(null);

        env.metadata = mock(ProjectMetadata.class);
        when(env.metadata.getArtifactId()).thenReturn("my-app");
        when(env.metadata.getProjectBaseDir()).thenReturn("/tmp/project");

        env.entities = List.of();
        env.generator = new AiContextGenerator(env.config, env.metadata, env.entities);
        return env;
    }

    @Test
    void generate_shouldSkipWhenAiConfigIsNull() {

        final Env env = prepareEnv();
        when(env.config.getAi()).thenReturn(null);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            env.generator.generate("out");

            genCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenBothFlagsAreFalse() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(false);
        when(env.aiCfg.getAgents()).thenReturn(false);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            env.generator.generate("out");

            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateOnlyClaudeMd_whenOnlyClaudeIsTrue() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/claude-md-template.ftl"), anyMap())).thenReturn("CLAUDE");
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/claude-md-template.ftl"), anyMap()));
            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/agents-md-template.ftl"), anyMap()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("/tmp/project"), eq("CLAUDE.md"), eq("CLAUDE")));
            writer.verify(() -> FileWriterUtils.writeToFile(anyString(), eq("AGENTS.md"), anyString()), never());
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD));
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD), never());
        }
    }

    @Test
    void generate_shouldGenerateOnlyAgentsMd_whenOnlyAgentsIsTrue() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(false);
        when(env.aiCfg.getAgents()).thenReturn(true);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/agents-md-template.ftl"), anyMap())).thenReturn("AGENTS");
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/agents-md-template.ftl"), anyMap()));
            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/claude-md-template.ftl"), anyMap()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("/tmp/project"), eq("AGENTS.md"), eq("AGENTS")));
            writer.verify(() -> FileWriterUtils.writeToFile(anyString(), eq("CLAUDE.md"), anyString()), never());
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD));
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD), never());
        }
    }

    @Test
    void generate_shouldGenerateBothFiles_whenBothFlagsAreTrue() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(true);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/claude-md-template.ftl"), anyMap())).thenReturn("CLAUDE");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/agents-md-template.ftl"), anyMap())).thenReturn("AGENTS");
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq("/tmp/project"), eq("CLAUDE.md"), eq("CLAUDE")));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("/tmp/project"), eq("AGENTS.md"), eq("AGENTS")));
        }
    }

    @Test
    void generate_shouldSkipClaudeMd_whenAlreadyGenerated() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(true);

            env.generator.generate("out");

            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD), never());
        }
    }

    @Test
    void generate_shouldSkipAgentsMd_whenAlreadyGenerated() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(false);
        when(env.aiCfg.getAgents()).thenReturn(true);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD)).thenReturn(true);

            env.generator.generate("out");

            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD), never());
        }
    }

    @Test
    void generate_shouldBuildContextWithMinimalConfig() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("ai/claude-md-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            assertEquals(1, capturedContexts.size());
            final Map<String, Object> ctx = capturedContexts.get(0);
            assertEquals("my-app", ctx.get("artifactId"));
            assertEquals("POSTGRESQL", ctx.get("database"));
            assertFalse((Boolean) ctx.get("dockerEnabled"));
            assertFalse((Boolean) ctx.get("dockerComposeEnabled"));
            assertEquals(8080, ctx.get("appPort"));
            assertFalse((Boolean) ctx.get("cacheEnabled"));
            assertFalse((Boolean) ctx.get("openApiEnabled"));
            assertFalse((Boolean) ctx.get("graphqlEnabled"));
            assertFalse((Boolean) ctx.get("migrationScripts"));
            assertFalse((Boolean) ctx.get("unitTestsEnabled"));
            assertFalse((Boolean) ctx.get("integrationTestsEnabled"));
            assertFalse((Boolean) ctx.get("testsEnabled"));
            assertEquals(List.of(), ctx.get("entities"));
        }
    }

    @Test
    void generate_shouldBuildContextWithDockerAndCustomPort() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final DockerConfiguration dockerCfg = mock(DockerConfiguration.class);
        when(dockerCfg.getDockerfile()).thenReturn(true);
        when(dockerCfg.getDockerCompose()).thenReturn(true);

        final ApplicationDockerConfiguration appCfg = mock(ApplicationDockerConfiguration.class);
        when(appCfg.getPort()).thenReturn(9090);
        when(dockerCfg.getApp()).thenReturn(appCfg);

        when(env.config.getDocker()).thenReturn(dockerCfg);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            assertEquals(1, capturedContexts.size());
            final Map<String, Object> ctx = capturedContexts.get(0);
            assertTrue((Boolean) ctx.get("dockerEnabled"));
            assertTrue((Boolean) ctx.get("dockerComposeEnabled"));
            assertEquals(9090, ctx.get("appPort"));
        }
    }

    @Test
    void generate_shouldBuildContextWithDefaultPort_whenDockerHasNoAppConfig() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final DockerConfiguration dockerCfg = mock(DockerConfiguration.class);
        when(dockerCfg.getDockerfile()).thenReturn(true);
        when(dockerCfg.getDockerCompose()).thenReturn(false);
        when(dockerCfg.getApp()).thenReturn(null);
        when(env.config.getDocker()).thenReturn(dockerCfg);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            assertEquals(8080, capturedContexts.get(0).get("appPort"));
        }
    }

    @Test
    void generate_shouldBuildContextWithCacheRedis_andRequiresInfrastructureTrue() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final CacheConfiguration cacheCfg = mock(CacheConfiguration.class);
        when(cacheCfg.getEnabled()).thenReturn(true);
        when(cacheCfg.getType()).thenReturn(CacheTypeEnum.REDIS);
        when(env.config.getCache()).thenReturn(cacheCfg);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            final Map<String, Object> ctx = capturedContexts.get(0);
            assertTrue((Boolean) ctx.get("cacheEnabled"));
            assertEquals("REDIS", ctx.get("cacheType"));
            assertTrue((Boolean) ctx.get("cacheRequiresInfrastructure"));
        }
    }

    @Test
    void generate_shouldBuildContextWithCacheHazelcast_andRequiresInfrastructureTrue() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final CacheConfiguration cacheCfg = mock(CacheConfiguration.class);
        when(cacheCfg.getEnabled()).thenReturn(true);
        when(cacheCfg.getType()).thenReturn(CacheTypeEnum.HAZELCAST);
        when(env.config.getCache()).thenReturn(cacheCfg);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            final Map<String, Object> ctx = capturedContexts.get(0);
            assertEquals("HAZELCAST", ctx.get("cacheType"));
            assertTrue((Boolean) ctx.get("cacheRequiresInfrastructure"));
        }
    }

    @Test
    void generate_shouldBuildContextWithCacheCaffeine_andRequiresInfrastructureFalse() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final CacheConfiguration cacheCfg = mock(CacheConfiguration.class);
        when(cacheCfg.getEnabled()).thenReturn(true);
        when(cacheCfg.getType()).thenReturn(CacheTypeEnum.CAFFEINE);
        when(env.config.getCache()).thenReturn(cacheCfg);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            final Map<String, Object> ctx = capturedContexts.get(0);
            assertEquals("CAFFEINE", ctx.get("cacheType"));
            assertFalse((Boolean) ctx.get("cacheRequiresInfrastructure"));
        }
    }

    @Test
    void generate_shouldBuildContextWithOpenApiAndGraphQL() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final OpenApiDefinition openApi = mock(OpenApiDefinition.class);
        when(openApi.getApiSpec()).thenReturn(true);
        when(env.config.getOpenApi()).thenReturn(openApi);

        final GraphQLDefinition graphql = mock(GraphQLDefinition.class);
        when(graphql.getEnabled()).thenReturn(true);
        when(env.config.getGraphql()).thenReturn(graphql);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            final Map<String, Object> ctx = capturedContexts.get(0);
            assertTrue((Boolean) ctx.get("openApiEnabled"));
            assertTrue((Boolean) ctx.get("graphqlEnabled"));
        }
    }

    @Test
    void generate_shouldBuildContextWithMigrationScriptsAndTests() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        when(env.config.isMigrationScripts()).thenReturn(true);

        final TestConfiguration testCfg = mock(TestConfiguration.class);
        when(testCfg.getUnit()).thenReturn(true);
        when(testCfg.getIntegration()).thenReturn(true);
        when(env.config.getTests()).thenReturn(testCfg);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            final Map<String, Object> ctx = capturedContexts.get(0);
            assertTrue((Boolean) ctx.get("migrationScripts"));
            assertTrue((Boolean) ctx.get("unitTestsEnabled"));
            assertTrue((Boolean) ctx.get("integrationTestsEnabled"));
            assertTrue((Boolean) ctx.get("testsEnabled"));
        }
    }

    @Test
    void generate_shouldBuildContextWithMongoMigrationPath() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);
        when(env.config.getDatabase()).thenReturn(DatabaseType.MONGODB);
        when(env.config.isMigrationScripts()).thenReturn(true);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("/tmp/project/src/main/java/com/example/demo");

            final Map<String, Object> ctx = capturedContexts.get(0);
            assertTrue((Boolean) ctx.get("isMongoDatabase"));
            assertEquals("src/main/java/com/example/demo/migration/", ctx.get("mongoMigrationPath"));
        }
    }

    @Test
    void generate_shouldBuildContextWithEntities() {

        final ModelDefinition product = mock(ModelDefinition.class);
        when(product.getName()).thenReturn("Product");
        final ModelDefinition order = mock(ModelDefinition.class);
        when(order.getName()).thenReturn("Order");

        final Env env = prepareEnv();
        env.entities = List.of(product, order);
        env.generator = new AiContextGenerator(env.config, env.metadata, env.entities);
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            @SuppressWarnings("unchecked")
            final List<String> entities = (List<String>) capturedContexts.get(0).get("entities");
            assertEquals(List.of("Product", "Order"), entities);
        }
    }

    @Test
    void generate_shouldSetTestsEnabledTrue_whenOnlyUnitTestsEnabled() {

        final Env env = prepareEnv();
        when(env.aiCfg.getClaude()).thenReturn(true);
        when(env.aiCfg.getAgents()).thenReturn(false);

        final TestConfiguration testCfg = mock(TestConfiguration.class);
        when(testCfg.getUnit()).thenReturn(true);
        when(testCfg.getIntegration()).thenReturn(false);
        when(env.config.getTests()).thenReturn(testCfg);

        final List<Map<String, Object>> capturedContexts = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)).thenReturn(false);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        capturedContexts.add(new HashMap<>(ctx));
                        return "CONTENT";
                    });
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            env.generator.generate("out");

            final Map<String, Object> ctx = capturedContexts.get(0);
            assertTrue((Boolean) ctx.get("unitTestsEnabled"));
            assertFalse((Boolean) ctx.get("integrationTestsEnabled"));
            assertTrue((Boolean) ctx.get("testsEnabled"));
        }
    }
}
