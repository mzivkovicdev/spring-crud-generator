/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.generators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.AiContextConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

public class AiContextGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiContextGenerator.class);

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;
    private final List<ModelDefinition> entities;

    public AiContextGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata,
            final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
        this.entities = entities;
    }

    @Override
    public void generate(final String outputDir) {

        final AiContextConfiguration ai = this.configuration.getAi();

        if (Objects.isNull(ai)) {
            return;
        }

        if (Boolean.TRUE.equals(ai.getClaude())) {
            this.generateClaudeMd(outputDir);
        }

        if (Boolean.TRUE.equals(ai.getAgents())) {
            this.generateAgentsMd(outputDir);
        }
    }

    /**
     * Generates a {@code CLAUDE.md} file in the project root directory.
     * <p>
     * The file provides Claude Code with context about the project: build commands,
     * database type, enabled features (cache, Docker, OpenAPI, GraphQL, migrations,
     * tests), project structure, and conventions around generated code.
     * Only sections relevant to the actual configuration are included.
     * </p>
     */
    private void generateClaudeMd(final String outputDir) {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD)) { return; }

        LOGGER.info("Generating CLAUDE.md");

        final String content = FreeMarkerTemplateProcessorUtils.processTemplate(
            "ai/claude-md-template.ftl", buildContext(outputDir));

        FileWriterUtils.writeToFile(projectMetadata.getProjectBaseDir(), "CLAUDE.md", content);

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.CLAUDE_MD);

        LOGGER.info("Finished generating CLAUDE.md");
    }

    /**
     * Generates an {@code AGENTS.md} file in the project root directory.
     * <p>
     * The file provides AI agents (e.g. GitHub Copilot, Gemini) with context about
     * the project: build and test commands, architecture overview, generated layers,
     * entity list, and rules about which files must not be manually edited.
     * Only sections relevant to the actual configuration are included.
     * </p>
     */
    private void generateAgentsMd(final String outputDir) {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD)) { return; }

        LOGGER.info("Generating AGENTS.md");

        final String content = FreeMarkerTemplateProcessorUtils.processTemplate(
            "ai/agents-md-template.ftl", buildContext(outputDir));

        FileWriterUtils.writeToFile(projectMetadata.getProjectBaseDir(), "AGENTS.md", content);

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.AGENTS_MD);

        LOGGER.info("Finished generating AGENTS.md");
    }

    /**
     * Builds the FreeMarker template context from the current {@link CrudConfiguration}
     * and {@link ProjectMetadata}.
     * <p>
     * Derives boolean flags for each optional feature (Docker, cache, OpenAPI, GraphQL,
     * tests, migration scripts) so templates can conditionally include only sections
     * that are relevant to this project. Fields that cannot be determined from the
     * configuration (e.g. DB credentials, custom business logic) are not included.
     * </p>
     *
     * @return a map of template variables consumed by both {@code CLAUDE.md} and
     *         {@code AGENTS.md} templates
     */
    private Map<String, Object> buildContext(final String outputDir) {

        final Map<String, Object> context = new HashMap<>();

        context.put("artifactId", projectMetadata.getArtifactId());
        context.put("database", this.configuration.getDatabase().name());
        context.put("isMongoDatabase", DatabaseType.MONGODB.equals(this.configuration.getDatabase()));
        context.put("mongoMigrationPath", this.resolveMongoMigrationPath(outputDir));

        final boolean dockerEnabled = Objects.nonNull(this.configuration.getDocker())
            && Boolean.TRUE.equals(this.configuration.getDocker().getDockerfile());
        context.put("dockerEnabled", dockerEnabled);

        final boolean dockerComposeEnabled = dockerEnabled
            && Boolean.TRUE.equals(this.configuration.getDocker().getDockerCompose());
        context.put("dockerComposeEnabled", dockerComposeEnabled);

        int appPort = 8080;
        if (dockerEnabled && Objects.nonNull(this.configuration.getDocker().getApp())
                && Objects.nonNull(this.configuration.getDocker().getApp().getPort())) {
            appPort = this.configuration.getDocker().getApp().getPort();
        }
        context.put("appPort", appPort);

        final boolean cacheEnabled = Objects.nonNull(this.configuration.getCache())
            && Boolean.TRUE.equals(this.configuration.getCache().getEnabled());
        context.put("cacheEnabled", cacheEnabled);

        if (cacheEnabled && Objects.nonNull(this.configuration.getCache().getType())) {
            context.put("cacheType", this.configuration.getCache().getType().name());
            context.put("cacheRequiresInfrastructure",
                CacheTypeEnum.REDIS.equals(this.configuration.getCache().getType())
                || CacheTypeEnum.HAZELCAST.equals(this.configuration.getCache().getType()));
        } else {
            context.put("cacheType", "");
            context.put("cacheRequiresInfrastructure", false);
        }

        final boolean openApiEnabled = Objects.nonNull(this.configuration.getOpenApi())
            && Boolean.TRUE.equals(this.configuration.getOpenApi().getApiSpec());
        context.put("openApiEnabled", openApiEnabled);

        final boolean graphqlEnabled = Objects.nonNull(this.configuration.getGraphql())
            && Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled());
        context.put("graphqlEnabled", graphqlEnabled);

        context.put("migrationScripts", Boolean.TRUE.equals(this.configuration.isMigrationScripts()));

        final boolean unitTestsEnabled = Objects.nonNull(this.configuration.getTests())
            && Boolean.TRUE.equals(this.configuration.getTests().getUnit());
        context.put("unitTestsEnabled", unitTestsEnabled);

        final boolean integrationTestsEnabled = Objects.nonNull(this.configuration.getTests())
            && Boolean.TRUE.equals(this.configuration.getTests().getIntegration());
        context.put("integrationTestsEnabled", integrationTestsEnabled);

        context.put("testsEnabled", unitTestsEnabled || integrationTestsEnabled);

        context.put("entities", this.entities.stream().map(ModelDefinition::getName).toList());

        return context;
    }

    /**
     * Resolves the generated MongoDB migration classes path.
     * For MongoDB, migration change units are generated in {@code src/main/java/<basePackage>/migration}.
     * If output directory cannot be parsed, a generic placeholder path is returned.
     *
     * @param outputDir generation output directory
     * @return migration path string for documentation templates
     */
    private String resolveMongoMigrationPath(final String outputDir) {

        try {
            final String basePackage = PackageUtils.getPackagePathFromOutputDir(outputDir);
            if (Objects.isNull(basePackage) || basePackage.isBlank()) {
                return "src/main/java/migration/";
            }
            return "src/main/java/" + basePackage.replace('.', '/') + "/migration/";
        } catch (RuntimeException ex) {
            return "src/main/java/<base-package>/migration/";
        }
    }

}
