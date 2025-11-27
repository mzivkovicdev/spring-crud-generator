package com.markozivkovic.codegen.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.markozivkovic.codegen.models.GeneratorState;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.GeneratorStateUtils;
import com.markozivkovic.codegen.validators.DockerConfigurationValidator;
import com.markozivkovic.codegen.validators.PackageConfigurationValidator;
import com.markozivkovic.codegen.validators.SpecificationValidator;

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
            SpecificationValidator.validate(spec);
            DockerConfigurationValidator.validate(spec.getConfiguration().getDocker());
            PackageConfigurationValidator.validate(spec.getPackages(), spec.getConfiguration());
            
            final ProjectMetadata projectMetadata = new ProjectMetadata(artifactId, version, projectBaseDir.getAbsolutePath());
            final GeneratorState generatorState = GeneratorStateUtils.loadOrEmpty(projectMetadata.getProjectBaseDir());
            final List<ModelDefinition> activeEntities = spec.getEntities().stream()
                    .filter(entity -> !Boolean.TRUE.equals(entity.getIgnore()))
                    .toList();

            final Map<String, String> fingerprints = activeEntities.stream()
                    .collect(Collectors.toMap(ModelDefinition::getName, entity -> GeneratorStateUtils.computeFingerprint(entity)));
            final List<ModelDefinition> entitiesToGenerate = this.computeEntitiesToGenerate(
                    activeEntities, forceRegeneration, generatorState, fingerprints
            );

            if (entitiesToGenerate.isEmpty()) {
                LOGGER.info("No changes detected in CRUD spec. Skipping code generation.");
                return;
            }
            
            final SpringCrudGenerator generator = new SpringCrudGenerator(
                    spec.getConfiguration(), entitiesToGenerate, projectMetadata, spec.getPackages()
            );
            final SpringCrudTestGenerator testGenerator = new SpringCrudTestGenerator(
                    spec.getConfiguration(), entitiesToGenerate, spec.getPackages()
            );
            entitiesToGenerate.forEach(entity -> generator.generate(entity, outputDir));
            entitiesToGenerate.forEach(entity -> testGenerator.generate(entity, outputDir));
            entitiesToGenerate.forEach(entity ->
                    GeneratorStateUtils.updateFingerprint(generatorState, entity.getName(), fingerprints.get(entity.getName()))
            );
            GeneratorStateUtils.save(projectMetadata.getProjectBaseDir(), generatorState);

            LOGGER.info("Generator finished for file: {}", inputSpecFile);
        } catch (final Exception e) {
            throw new MojoExecutionException("Code generation failed", e);
        }
    }

    /**
     * Computes the list of entities to generate based on the provided active entities and the generator state.
     * If forceRegeneration is true, all active entities are included in the list.
     * Otherwise, only entities with a changed fingerprint are included in the list.
     *
     * @param activeEntities    the list of active entities
     * @param forceRegeneration whether to force regeneration for all active entities
     * @param generatorState    the generator state
     * @param fingerprints      the map of entity names to their respective fingerprints
     * @return the {@link List} of entities {@link ModelDefinition} to generate
     */
    private List<ModelDefinition> computeEntitiesToGenerate(final List<ModelDefinition> activeEntities, final boolean forceRegeneration,
            final GeneratorState generatorState, final Map<String, String> fingerprints) {

        final List<ModelDefinition> entitiesToGenerate;

        if (forceRegeneration) {
            final List<String> entityNames = activeEntities.stream()
                    .map(ModelDefinition::getName)
                    .toList();
            LOGGER.info("forceRegeneration=true -> forcing regeneration for all active entities: {}", String.join(", ", entityNames));
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
