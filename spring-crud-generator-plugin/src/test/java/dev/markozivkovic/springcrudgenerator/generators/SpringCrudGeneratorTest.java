package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;

class SpringCrudGeneratorTest {

    @SuppressWarnings("unchecked")
    @Test
    void generate_shouldDelegateToAllProjectArtifactGenerators() throws Exception {

        final SpringCrudGenerator generator = createGenerator(DatabaseType.POSTGRESQL);

        final Field artifactField = SpringCrudGenerator.class.getDeclaredField("ARTIFACT_GENERATORS");
        artifactField.setAccessible(true);

        final Map<String, ProjectArtifactGenerator> artifactGenerators =
                (Map<String, ProjectArtifactGenerator>) artifactField.get(generator);

        final List<TrackingProjectArtifactGenerator> delegates = new ArrayList<>();
        List.copyOf(artifactGenerators.keySet()).forEach(key -> {
            final TrackingProjectArtifactGenerator delegate = new TrackingProjectArtifactGenerator();
            artifactGenerators.put(key, delegate);
            delegates.add(delegate);
        });

        final String outputDir = "out";
        generator.generate(outputDir);

        delegates.forEach(delegate -> {
            assertTrue(delegate.called);
            assertEquals(outputDir, delegate.lastOutputDir);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void generate_shouldDelegateToAllCodeGeneratorsForGivenModel() throws Exception {

        final SpringCrudGenerator generator = createGenerator(DatabaseType.POSTGRESQL);
        final ModelDefinition model = new ModelDefinition().setName("UserEntity");

        final Field genField = SpringCrudGenerator.class.getDeclaredField("GENERATORS");
        genField.setAccessible(true);

        final Map<String, CodeGenerator> codeGenerators =
                (Map<String, CodeGenerator>) genField.get(generator);

        final List<TrackingCodeGenerator> delegates = new ArrayList<>();
        List.copyOf(codeGenerators.keySet()).forEach(key -> {
            final TrackingCodeGenerator delegate = new TrackingCodeGenerator();
            codeGenerators.put(key, delegate);
            delegates.add(delegate);
        });

        final String outputDir = "out";
        generator.generate(model, outputDir);

        delegates.forEach(delegate -> {
            assertTrue(delegate.called);
            assertEquals(model, delegate.lastModelDefinition);
            assertEquals(outputDir, delegate.lastOutputDir);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("MongoDB mode uses Mongo generators and excludes SQL migration generator")
    void constructor_mongodb_usesMongoGenerators() throws Exception {

        final SpringCrudGenerator generator = createGenerator(DatabaseType.MONGODB);

        final Field genField = SpringCrudGenerator.class.getDeclaredField("GENERATORS");
        genField.setAccessible(true);
        final Map<String, CodeGenerator> codeGenerators =
                (Map<String, CodeGenerator>) genField.get(generator);

        assertTrue(codeGenerators.containsKey("mongo-model"));
        assertTrue(codeGenerators.containsKey("mongo-repository"));
        assertFalse(codeGenerators.containsKey("jpa-model"));
        assertFalse(codeGenerators.containsKey("jpa-repository"));
        assertFalse(codeGenerators.containsKey("migration-script"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("SQL mode keeps JPA generators and migration generator")
    void constructor_sql_usesJpaGenerators() throws Exception {

        final SpringCrudGenerator generator = createGenerator(DatabaseType.POSTGRESQL);

        final Field genField = SpringCrudGenerator.class.getDeclaredField("GENERATORS");
        genField.setAccessible(true);
        final Map<String, CodeGenerator> codeGenerators =
                (Map<String, CodeGenerator>) genField.get(generator);

        assertTrue(codeGenerators.containsKey("jpa-model"));
        assertTrue(codeGenerators.containsKey("jpa-repository"));
        assertTrue(codeGenerators.containsKey("migration-script"));
        assertFalse(codeGenerators.containsKey("mongo-model"));
        assertFalse(codeGenerators.containsKey("mongo-repository"));
    }

    @Test
    @DisplayName("Missing database configuration fails fast")
    void constructor_nullDatabase_throwsException() {

        final CrudConfiguration configuration = new CrudConfiguration();

        assertThrows(
                IllegalStateException.class,
                () -> new SpringCrudGenerator(
                        configuration,
                        List.of(),
                        createProjectMetadata(),
                        createPackageConfiguration()
                )
        );
    }

    private static SpringCrudGenerator createGenerator(final DatabaseType databaseType) {

        final CrudConfiguration configuration = new CrudConfiguration().setDatabase(databaseType);
        return new SpringCrudGenerator(
                configuration,
                List.of(),
                createProjectMetadata(),
                createPackageConfiguration()
        );
    }

    private static ProjectMetadata createProjectMetadata() {
        return new ProjectMetadata("demo-artifact", "1.0.0", ".");
    }

    private static PackageConfiguration createPackageConfiguration() {

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        packageConfiguration.setAnnotations("dev.test.annotations");
        packageConfiguration.setBusinessservices("dev.test.businessservices");
        packageConfiguration.setConfigurations("dev.test.configurations");
        packageConfiguration.setControllers("dev.test.controllers");
        packageConfiguration.setEnums("dev.test.enums");
        packageConfiguration.setExceptions("dev.test.exceptions");
        packageConfiguration.setGenerated("dev.test.generated");
        packageConfiguration.setMappers("dev.test.mappers");
        packageConfiguration.setModels("dev.test.models");
        packageConfiguration.setResolvers("dev.test.resolvers");
        packageConfiguration.setRepositories("dev.test.repositories");
        packageConfiguration.setServices("dev.test.services");
        packageConfiguration.setTransferobjects("dev.test.transferobjects");
        return packageConfiguration;
    }

    private static class TrackingProjectArtifactGenerator implements ProjectArtifactGenerator {

        private boolean called;
        private String lastOutputDir;

        @Override
        public void generate(final String outputDir) {
            this.called = true;
            this.lastOutputDir = outputDir;
        }
    }

    private static class TrackingCodeGenerator implements CodeGenerator {

        private boolean called;
        private ModelDefinition lastModelDefinition;
        private String lastOutputDir;

        @Override
        public void generate(final ModelDefinition modelDefinition, final String outputDir) {
            this.called = true;
            this.lastModelDefinition = modelDefinition;
            this.lastOutputDir = outputDir;
        }
    }
}
