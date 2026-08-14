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

package dev.markozivkovic.springcrudgenerator.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.generators.SpringCrudGenerator;
import dev.markozivkovic.springcrudgenerator.generators.tests.SpringCrudTestGenerator;
import dev.markozivkovic.springcrudgenerator.models.CrudSpecification;
import dev.markozivkovic.springcrudgenerator.models.GeneratorState;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.utils.CrudMojoUtils;
import dev.markozivkovic.springcrudgenerator.utils.DependencyCheckUtils;
import dev.markozivkovic.springcrudgenerator.utils.GeneratorStateUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.validators.PackageConfigurationValidator;
import dev.markozivkovic.springcrudgenerator.validators.SpecificationValidator;

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

    @Parameter(property = "forceRegeneration", defaultValue = "false")
    private boolean forceRegeneration;

    @Parameter(defaultValue = "${project.parent.version}", readonly = true)
    private String parentVersion;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor pluginDescriptor;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {

        this.validateRequiredParameters();

        try {
            this.generateFromSpecification();
        } catch (final Exception exception) {
            throw new MojoExecutionException("Code generation failed", exception);
        }
    }

    private void validateRequiredParameters() throws MojoExecutionException {

        if (Objects.isNull(this.inputSpecFile)) {
            throw new MojoExecutionException("inputSpecFile must be specified");
        }
        
        if (Objects.isNull(this.outputDir)) {
            throw new MojoExecutionException("outputDir must be specified");
        }
    }

    private void generateFromSpecification() throws IOException {

        CrudMojoUtils.printBanner(this.pluginDescriptor, this.inputSpecFile, this.outputDir);
        final ObjectMapper mapper = CrudMojoUtils.createSpecMapper(this.inputSpecFile);
        final Path specPath = Paths.get(this.inputSpecFile).toAbsolutePath().normalize();

        LOGGER.info("Generator started for file: {}", specPath);

        final CrudSpecification specification = mapper.readValue(specPath.toFile(), CrudSpecification.class);
        SpecificationValidator.validate(specification);
        PackageConfigurationValidator.validate(specification.getPackages(), specification.getConfiguration());
        SpringBootVersionUtils.resolveAndSetSpringBootMajor(specification, this.parentVersion);

        final ProjectMetadata projectMetadata = new ProjectMetadata(
                this.artifactId,
                this.version,
                this.projectBaseDir.getAbsolutePath());
        final GeneratorState generatorState = GeneratorStateUtils.loadOrEmpty(projectMetadata.getProjectBaseDir());
        final List<ModelDefinition> activeEntities = specification.getEntities().stream()
                .filter(entity -> !Boolean.TRUE.equals(entity.getIgnore()))
                .toList();

        this.generateChangedEntities(specification, projectMetadata, generatorState, activeEntities);
        LOGGER.info("Generator finished for file: {}", this.inputSpecFile);
    }

    private void generateChangedEntities(
            final CrudSpecification specification,
            final ProjectMetadata projectMetadata,
            final GeneratorState generatorState,
            final List<ModelDefinition> activeEntities) {

        final Map<String, String> fingerprints = activeEntities.stream()
                .collect(Collectors.toMap(
                        ModelDefinition::getName,
                        GeneratorStateUtils::computeFingerprint));
        final String configurationFingerprint = GeneratorStateUtils.computeFingerprint(specification.getConfiguration());
        final List<ModelDefinition> entitiesToGenerate = this.computeEntitiesToGenerate(
                activeEntities,
                this.forceRegeneration,
                generatorState,
                fingerprints,
                configurationFingerprint);

        if (entitiesToGenerate.isEmpty()) {
            DependencyCheckUtils.warnMissingDependencies(specification.getConfiguration(), this.project);
            LOGGER.info("No changes detected in CRUD spec. Skipping code generation.");
            return;
        }

        final SpringCrudGenerator generator = new SpringCrudGenerator(
                specification.getConfiguration(), activeEntities, projectMetadata, specification.getPackages());
        final SpringCrudTestGenerator testGenerator = new SpringCrudTestGenerator(
                specification.getConfiguration(), activeEntities, specification.getPackages());
        generator.generate(this.outputDir);
        entitiesToGenerate.forEach(entity -> generator.generate(entity, this.outputDir));
        entitiesToGenerate.forEach(entity -> testGenerator.generate(entity, this.outputDir));
        entitiesToGenerate.forEach(entity -> GeneratorStateUtils.updateFingerprint(
                generatorState,
                entity.getName(),
                fingerprints.get(entity.getName()),
                configurationFingerprint));
        GeneratorStateUtils.save(projectMetadata.getProjectBaseDir(), generatorState);
        DependencyCheckUtils.warnMissingDependencies(specification.getConfiguration(), this.project);
    }

    /**
     * Computes the list of entities to generate based on the provided active entities and the generator state.
     * If forceRegeneration is true, all active entities are included in the list.
     * Otherwise, only entities with a changed fingerprint are included in the list.
     *
     * @param activeEntities            the list of active entities
     * @param forceRegeneration         whether to force regeneration for all active entities
     * @param generatorState            the generator state
     * @param fingerprints              the map of entity names to their respective fingerprints
     * @param configurationFingerprints the fingerprint of the configuration
     * @return the {@link List} of entities {@link ModelDefinition} to generate
     */
    private List<ModelDefinition> computeEntitiesToGenerate(final List<ModelDefinition> activeEntities, final boolean forceRegeneration,
            final GeneratorState generatorState, final Map<String, String> fingerprints, final String configurationFingerprints) {

        final List<ModelDefinition> entitiesToGenerate;

        if (forceRegeneration || !configurationFingerprints.equals(generatorState.getConfiguration())) {
            final List<String> entityNames = activeEntities.stream()
                    .map(ModelDefinition::getName)
                    .toList();
            LOGGER.info("forceRegeneration=true or configuration has changed -> regeneration for all active entities: {}", String.join(", ", entityNames));
            entitiesToGenerate = new ArrayList<>(activeEntities);
        } else {
            entitiesToGenerate = activeEntities.stream()
                    .filter(entity -> {
                        final Optional<String> previousFingerprint = GeneratorStateUtils.findPreviousFingerprint(generatorState, entity.getName());
                        return previousFingerprint.isEmpty() || !previousFingerprint.get().equals(fingerprints.get(entity.getName()));
                    }).toList();
            final List<String> entityNames = entitiesToGenerate.stream()
                    .map(ModelDefinition::getName)
                    .toList();
            LOGGER.info("Entities with changed fingerprint: {}", String.join(", ", entityNames));
        }

        return entitiesToGenerate;
    }

}
