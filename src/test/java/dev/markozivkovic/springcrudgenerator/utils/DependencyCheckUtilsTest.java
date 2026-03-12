package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.markozivkovic.springcrudgenerator.constants.AdditionalConfigurationConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.GraphQLDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.OpenApiDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;

class DependencyCheckUtilsTest {

    @TempDir
    File tempDir;

    @Test
    void findMissingDependencies_whenAllRequiredDependenciesExist_returnsEmptyList() {

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.POSTGRESQL)
                .setSpringBootVersion("3")
                .setOpenApi(new OpenApiDefinition(true, true))
                .setGraphql(new GraphQLDefinition(true, true))
                .setMigrationScripts(true)
                .setOptimisticLocking(true)
                .setCache(new CacheConfiguration().setEnabled(true).setType(CacheTypeEnum.REDIS))
                .setTests(new TestConfiguration().setUnit(true).setDataGenerator(DataGeneratorEnum.INSTANCIO))
                .setAdditionalProperties(Map.of(AdditionalConfigurationConstants.OPT_LOCK_RETRY_CONFIGURATION, true));

        final MavenProject project = createProjectWithDependencies(
                dep("org.springframework.boot", "spring-boot-starter-web"),
                dep("org.springframework.boot", "spring-boot-starter-data-jpa"),
                dep("org.springframework.boot", "spring-boot-starter-validation"),
                dep("org.mapstruct", "mapstruct"),
                dep("org.postgresql", "postgresql"),
                dep("org.openapitools", "jackson-databind-nullable"),
                dep("io.swagger.core.v3", "swagger-annotations"),
                dep("org.springframework.boot", "spring-boot-starter-graphql"),
                dep("com.graphql-java", "graphql-java-extended-scalars"),
                dep("org.springframework.boot", "spring-boot-starter-cache"),
                dep("org.springframework.boot", "spring-boot-starter-data-redis"),
                dep("org.flywaydb", "flyway-core"),
                dep("org.springframework.boot", "spring-boot-starter-test"),
                dep("org.instancio", "instancio-core"),
                dep("org.springframework.graphql", "spring-graphql-test"),
                dep("org.springframework.retry", "spring-retry")
        );

        final List<String> missingDependencies = DependencyCheckUtils.findMissingDependencies(configuration, project);

        assertTrue(missingDependencies.isEmpty());
    }

    @Test
    void findMissingDependencies_whenSomeDependenciesAreMissing_returnsWarnings() {

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.POSTGRESQL)
                .setSpringBootVersion("3")
                .setOpenApi(new OpenApiDefinition(true, true))
                .setGraphql(new GraphQLDefinition(true, true))
                .setMigrationScripts(true)
                .setOptimisticLocking(true)
                .setCache(new CacheConfiguration().setEnabled(true).setType(CacheTypeEnum.REDIS))
                .setTests(new TestConfiguration().setUnit(true).setDataGenerator(DataGeneratorEnum.PODAM))
                .setAdditionalProperties(Map.of(AdditionalConfigurationConstants.OPT_LOCK_RETRY_CONFIGURATION, true));

        final MavenProject project = createProjectWithDependencies(
                dep("org.springframework.boot", "spring-boot-starter-web"),
                dep("org.springframework.boot", "spring-boot-starter-validation")
        );

        final List<String> missingDependencies = DependencyCheckUtils.findMissingDependencies(configuration, project);

        assertFalse(missingDependencies.isEmpty());
        assertTrue(containsDependency(missingDependencies, "org.springframework.boot:spring-boot-starter-data-jpa"));
        assertTrue(containsDependency(missingDependencies, "org.postgresql:postgresql"));
        assertTrue(containsDependency(missingDependencies, "org.flywaydb:flyway-core"));
        assertTrue(containsDependency(missingDependencies, "org.springframework.boot:spring-boot-starter-graphql"));
        assertTrue(containsDependency(missingDependencies, "org.springframework.boot:spring-boot-starter-data-redis"));
        assertTrue(containsDependency(missingDependencies, "org.springframework.boot:spring-boot-starter-test"));
        assertTrue(containsDependency(missingDependencies, "uk.co.jemos.podam:podam"));
        assertTrue(containsDependency(missingDependencies, "org.springframework.retry:spring-retry"));
    }

    @Test
    void findMissingDependencies_openApiWithJakartaSwaggerDependency_isValid() {

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.POSTGRESQL)
                .setSpringBootVersion("3")
                .setOpenApi(new OpenApiDefinition(true, true));

        final MavenProject project = createProjectWithDependencies(
                dep("org.springframework.boot", "spring-boot-starter-web"),
                dep("org.springframework.boot", "spring-boot-starter-data-jpa"),
                dep("org.springframework.boot", "spring-boot-starter-validation"),
                dep("org.mapstruct", "mapstruct"),
                dep("org.postgresql", "postgresql"),
                dep("org.openapitools", "jackson-databind-nullable"),
                dep("io.swagger.core.v3", "swagger-annotations-jakarta")
        );

        final List<String> missingDependencies = DependencyCheckUtils.findMissingDependencies(configuration, project);

        assertTrue(missingDependencies.isEmpty());
    }

    @Test
    void warnMissingDependencies_whenProjectFileMissing_doesNotThrow() {

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.POSTGRESQL)
                .setSpringBootVersion("3")
                .setDependencyCheck(true);

        final MavenProject project = createProjectWithDependencies();

        assertDoesNotThrow(() -> DependencyCheckUtils.warnMissingDependencies(configuration, project));
    }

    @Test
    void warnMissingDependencies_whenProjectIsNull_doesNotThrow() {

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.POSTGRESQL)
                .setSpringBootVersion("3")
                .setDependencyCheck(true);

        assertDoesNotThrow(() -> DependencyCheckUtils.warnMissingDependencies(configuration, null));
    }

    private MavenProject createProjectWithDependencies(final Dependency... dependencies) {

        final MavenProject project = new MavenProject();
        project.setFile(new File(tempDir, "pom.xml"));
        project.setGroupId("com.example");
        project.setArtifactId("demo");

        final List<Dependency> list = new ArrayList<>();
        for (final Dependency dependency : dependencies) {
            list.add(dependency);
        }
        project.setDependencies(list);

        return project;
    }

    @Test
    void findMissingDependencies_springBoot4_dependenciesPresent_returnsEmptyList() {

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.POSTGRESQL)
                .setSpringBootVersion("4")
                .setGraphql(new GraphQLDefinition(true, true))
                .setMigrationScripts(true)
                .setTests(new TestConfiguration().setUnit(true).setDataGenerator(DataGeneratorEnum.INSTANCIO));

        final MavenProject project = createProjectWithDependencies(
                dep("org.springframework.boot", "spring-boot-starter-webmvc"),
                dep("org.springframework.boot", "spring-boot-starter-data-jpa"),
                dep("org.springframework.boot", "spring-boot-starter-validation"),
                dep("org.mapstruct", "mapstruct"),
                dep("org.postgresql", "postgresql"),
                dep("org.springframework.boot", "spring-boot-starter-graphql"),
                dep("com.graphql-java", "graphql-java-extended-scalars"),
                dep("org.springframework.boot", "spring-boot-starter-flyway"),
                dep("org.springframework.boot", "spring-boot-starter-test"),
                dep("org.springframework.boot", "spring-boot-starter-graphql-test"),
                dep("org.instancio", "instancio-core")
        );

        final List<String> missingDependencies = DependencyCheckUtils.findMissingDependencies(configuration, project);

        assertTrue(missingDependencies.isEmpty());
    }

    @Test
    void findMissingDependencies_springBoot4_withOldWebStarter_warnsWebMvcStarter() {

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.POSTGRESQL)
                .setSpringBootVersion("4");

        final MavenProject project = createProjectWithDependencies(
                dep("org.springframework.boot", "spring-boot-starter-web"),
                dep("org.springframework.boot", "spring-boot-starter-data-jpa"),
                dep("org.springframework.boot", "spring-boot-starter-validation"),
                dep("org.mapstruct", "mapstruct"),
                dep("org.postgresql", "postgresql")
        );

        final List<String> missingDependencies = DependencyCheckUtils.findMissingDependencies(configuration, project);

        assertTrue(containsDependency(missingDependencies, "org.springframework.boot:spring-boot-starter-webmvc"));
    }

    private Dependency dep(final String groupId, final String artifactId) {
        final Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        return dependency;
    }

    private boolean containsDependency(final List<String> warnings, final String dependency) {
        return warnings.stream().anyMatch(message -> message.contains(dependency));
    }
}
