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

package dev.markozivkovic.springcrudgenerator.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.AdditionalConfigurationConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;

/**
 * Utilities for validating whether host project dependencies satisfy selected CRUD features.
 */
public final class DependencyCheckUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyCheckUtils.class);

    private DependencyCheckUtils() {}

    /**
     * Runs dependency check against resolved Maven project model and prints warnings for missing dependencies.
     * This check is performed only when configuration.dependencyCheck=true.
     *
     * @param configuration project CRUD configuration
     * @param project Maven project model injected by plugin
     */
    public static void warnMissingDependencies(final CrudConfiguration configuration, final MavenProject project) {

        if (Objects.isNull(configuration) || !Boolean.TRUE.equals(configuration.getDependencyCheck())) {
            return;
        }

        if (Objects.isNull(project)) {
            LOGGER.warn("Dependency check skipped because MavenProject is not available.");
            return;
        }

        final List<String> missingDependencies = findMissingDependencies(configuration, project);
        final String projectLabel = resolveProjectLabel(project);

        if (ContainerUtils.isEmpty(missingDependencies)) {
            LOGGER.info("Dependency check passed. All required dependencies are present in {}", projectLabel);
            return;
        }

        LOGGER.warn("Dependency check detected {} missing dependencies in {}", missingDependencies.size(), projectLabel);
        missingDependencies.forEach(message -> LOGGER.warn("{}", message));
    }

    /**
     * Computes missing dependencies for the given configuration by scanning dependencies declared on MavenProject.
     *
     * @param configuration project CRUD configuration
     * @param project       Maven project model injected by plugin
     * @return list of warning messages for missing dependencies
     */
    public static List<String> findMissingDependencies(final CrudConfiguration configuration, final MavenProject project) {

        if (Objects.isNull(configuration) || Objects.isNull(project)) {
            return List.of();
        }

        final Set<String> declaredDependencies = resolveDeclaredDependencies(project);
        final List<DependencyRequirement> requiredDependencies = resolveRequiredDependencies(configuration);

        return requiredDependencies.stream()
                .filter(requirement -> requirement.alternatives().stream()
                        .map(DependencyCoordinate::coordinate)
                        .noneMatch(declaredDependencies::contains))
                .map(DependencyCheckUtils::toWarningMessage)
                .collect(Collectors.toList());
    }

    /**
     * Returns a string that identifies the given MavenProject.
     * 
     * @param project the Maven project to identify
     * @return a string that identifies the given Maven project
     */
    private static String resolveProjectLabel(final MavenProject project) {

        if (Objects.nonNull(project.getFile())) {
            return project.getFile().toPath().toAbsolutePath().normalize().toString();
        }

        if (StringUtils.isNotBlank(project.getGroupId()) && StringUtils.isNotBlank(project.getArtifactId())) {
            return String.format("%s:%s", project.getGroupId(), project.getArtifactId());
        }

        return "unknown project";
    }

    /**
     * Computes set of dependencies declared on the given MavenProject.
     * It considers both the dependencies declared directly on the project and those declared on the project's model (if available).
     * Each dependency is represented as a string in the format "groupId:artifactId".
     * 
     * @param project the Maven project to resolve dependencies for
     * @return set of declared dependencies
     */
    private static Set<String> resolveDeclaredDependencies(final MavenProject project) {

        final Set<String> dependencies = new HashSet<>();

        if (!ContainerUtils.isEmpty(project.getDependencies())) {
            dependencies.addAll(project.getDependencies().stream()
                    .filter(Objects::nonNull)
                    .map(dep -> normalizeCoordinate(dep.getGroupId(), dep.getArtifactId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }

        if (ContainerUtils.isEmpty(dependencies) && Objects.nonNull(project.getModel())
                && !ContainerUtils.isEmpty(project.getModel().getDependencies())) {

            dependencies.addAll(project.getModel().getDependencies().stream()
                    .filter(Objects::nonNull)
                    .map(dep -> normalizeCoordinate(dep.getGroupId(), dep.getArtifactId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }

        return dependencies;
    }

    /**
     * Computes a warning message for the given dependency requirement.
     * The message is of the format "Missing dependency for <scenario>: <expected dependencies>".
     * <scenario> is the scenario for which the dependency is required, and
     * <expected dependencies> is a comma-separated list of the expected dependencies.
     * 
     * @param requirement the dependency requirement to compute the warning message for
     * @return the computed warning message
     */
    private static String toWarningMessage(final DependencyRequirement requirement) {

        final String expectedDependencies = requirement.alternatives().stream()
                .map(DependencyCoordinate::coordinate)
                .collect(Collectors.joining(" or "));

        return String.format("Missing dependency for %s: %s", requirement.scenario(), expectedDependencies);
    }

    /**
     * Computes a list of dependency requirements for the given configuration.
     * The method resolves the list of required dependencies based on the configuration options.
     * It checks if the configuration has enabled core CRUD generation, openApi resources, graphql, caching,
     * migration scripts, unit tests, and optimistic locking retry configuration.
     * For each enabled feature, it adds a dependency requirement to the list.
     * 
     * @param configuration the CRUD configuration
     * @return list of dependency requirements
     */
    private static List<DependencyRequirement> resolveRequiredDependencies(final CrudConfiguration configuration) {

        final List<DependencyRequirement> requirements = new ArrayList<>();
        final boolean isSpringBoot4 = SpringBootVersionUtils.isSpringBoot4(configuration.getSpringBootVersion());

        if (isSpringBoot4) {
            addRequirement(requirements, "core CRUD generation (Spring Boot 4)",
                    coordinate("org.springframework.boot", "spring-boot-starter-webmvc"));
        } else {
            addRequirement(requirements, "core CRUD generation (Spring Boot 3)",
                    coordinate("org.springframework.boot", "spring-boot-starter-web"));
        }
        addRequirement(requirements, "core CRUD generation", coordinate("org.springframework.boot", "spring-boot-starter-data-jpa"));
        addRequirement(requirements, "core CRUD generation", coordinate("org.springframework.boot", "spring-boot-starter-validation"));
        addRequirement(requirements, "core CRUD generation", coordinate("org.mapstruct", "mapstruct"));

        if (Objects.nonNull(configuration.getDatabase())) {
            addDatabaseRequirements(requirements, configuration.getDatabase());
        }

        if (isOpenApiResourcesEnabled(configuration)) {
            addRequirement(requirements, "openApi.generateResources=true",
                    coordinate("org.openapitools", "jackson-databind-nullable"));
            addRequirement(requirements, "openApi.generateResources=true",
                    coordinate("io.swagger.core.v3", "swagger-annotations"),
                    coordinate("io.swagger.core.v3", "swagger-annotations-jakarta"));
        }

        final boolean isGraphQlEnabled = isGraphQlEnabled(configuration);
        if (isGraphQlEnabled) {
            addRequirement(requirements, "graphql.enabled=true",
                    coordinate("org.springframework.boot", "spring-boot-starter-graphql"));

            if (Objects.nonNull(configuration.getGraphql()) && Boolean.TRUE.equals(configuration.getGraphql().getScalarConfig())) {
                addRequirement(requirements, "graphql.scalarConfig=true",
                        coordinate("com.graphql-java", "graphql-java-extended-scalars"));
            }
        }

        if (isCacheEnabled(configuration)) {
            addRequirement(requirements, "cache.enabled=true",
                    coordinate("org.springframework.boot", "spring-boot-starter-cache"));
            addCacheRequirements(requirements, configuration);
        }

        if (Boolean.TRUE.equals(configuration.isMigrationScripts())) {
            if (isSpringBoot4) {
                addRequirement(requirements, "migrationScripts=true (Spring Boot 4)",
                        coordinate("org.springframework.boot", "spring-boot-starter-flyway"));
            } else {
                addRequirement(requirements, "migrationScripts=true (Spring Boot 3)",
                        coordinate("org.flywaydb", "flyway-core"));
            }
        }

        if (isUnitTestsEnabled(configuration)) {
            addRequirement(requirements, "tests.unit=true",
                    coordinate("org.springframework.boot", "spring-boot-starter-test"));
            addTestDataGeneratorRequirement(requirements, configuration);

            if (isRedisCacheEnabled(configuration)) {
                addRequirement(requirements, "tests.unit=true with cache.type=REDIS",
                        coordinate("org.springframework.boot", "spring-boot-starter-data-redis-test"));
            }

            if (isSpringBoot4) {
                addRequirement(requirements, "tests.unit=true with Spring Boot 4 (WebMvc OAuth2 test support)",
                        coordinate("org.springframework.boot", "spring-boot-starter-oauth2-client"));
                addRequirement(requirements, "tests.unit=true with Spring Boot 4 (WebMvc OAuth2 test support)",
                        coordinate("org.springframework.boot", "spring-boot-starter-security-oauth2-resource-server"));
                addRequirement(requirements, "tests.unit=true with Spring Boot 4",
                        coordinate("org.springframework.boot", "spring-boot-starter-data-jpa-test"));

                if (Boolean.TRUE.equals(configuration.isMigrationScripts())) {
                    addRequirement(requirements, "tests.unit=true with migrationScripts=true (Spring Boot 4)",
                            coordinate("org.springframework.boot", "spring-boot-starter-flyway-test"));
                }

            }

            if (isGraphQlEnabled) {
                if (isSpringBoot4) {
                    addRequirement(requirements, "tests.unit=true with graphql.enabled=true (Spring Boot 4)",
                            coordinate("org.springframework.boot", "spring-boot-starter-graphql-test"));
                } else {
                    addRequirement(requirements, "tests.unit=true with graphql.enabled=true (Spring Boot 3)",
                            coordinate("org.springframework.graphql", "spring-graphql-test"));
                }
            }
        }

        if (requiresSpringRetry(configuration)) {
            addRequirement(requirements, "optimistic locking retry configuration",
                    coordinate("org.springframework.retry", "spring-retry"));
        }

        return requirements;
    }

    /**
     * Returns true if the OpenAPI resources are enabled in the given configuration.
     * OpenAPI resources are enabled if the OpenAPI configuration is not null and both
     * the API specification and resource generation are enabled.
     * 
     * @param configuration the Crud configuration
     * @return true if the OpenAPI resources are enabled, false otherwise
     */
    private static boolean isOpenApiResourcesEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getOpenApi())
                && Boolean.TRUE.equals(configuration.getOpenApi().getApiSpec())
                && Boolean.TRUE.equals(configuration.getOpenApi().getGenerateResources());
    }

    /**
     * Returns true if the GraphQL feature is enabled in the given configuration.
     * GraphQL is enabled if the GraphQL configuration is not null and the enabled flag is true.
     * 
     * @param configuration the Crud configuration
     * @return true if the GraphQL feature is enabled, false otherwise
     */
    private static boolean isGraphQlEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getGraphql()) && Boolean.TRUE.equals(configuration.getGraphql().getEnabled());
    }

    /**
     * Returns true if the cache feature is enabled in the given configuration.
     * Cache is enabled if the cache configuration is not null and the enabled flag is true.
     * 
     * @param configuration the Crud configuration
     * @return true if the cache feature is enabled, false otherwise
     */
    private static boolean isCacheEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getCache()) && Boolean.TRUE.equals(configuration.getCache().getEnabled());
    }

    /**
     * Returns true if Redis cache is explicitly enabled in the given configuration.
     *
     * @param configuration the Crud configuration
     * @return true if Redis cache is enabled, false otherwise
     */
    private static boolean isRedisCacheEnabled(final CrudConfiguration configuration) {
        return isCacheEnabled(configuration) && CacheTypeEnum.REDIS.equals(configuration.getCache().getType());
    }

    /**
     * Returns true if unit tests are enabled in the given configuration, false otherwise.
     * Unit tests are enabled if the tests configuration is not null and the unit flag is true.
     * 
     * @param configuration the Crud configuration
     * @return true if unit tests are enabled, false otherwise
     */
    private static boolean isUnitTestsEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getTests()) && Boolean.TRUE.equals(configuration.getTests().getUnit());
    }

    /**
     * Returns true if the given configuration requires the Spring Retry dependency.
     * Spring Retry is required if optimistic locking is enabled and either the retry configuration flag
     * or any of the retryable configuration overrides are present.
     *
     * @param configuration the configuration to check
     * @return true if Spring Retry is required, false otherwise
     */
    private static boolean requiresSpringRetry(final CrudConfiguration configuration) {

        if (!Boolean.TRUE.equals(configuration.getOptimisticLocking())) {
            return false;
        }

        final Map<String, Object> additionalProperties = configuration.getAdditionalProperties();
        if (ContainerUtils.isEmpty(additionalProperties)) {
            return false;
        }

        final boolean hasRetryConfigurationFlag = Boolean.TRUE.equals(
                additionalProperties.get(AdditionalConfigurationConstants.OPT_LOCK_RETRY_CONFIGURATION)
        );

        return hasRetryConfigurationFlag || AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(additionalProperties);
    }

    /**
     * Adds database dependency requirements for the given database type to the given list of requirements.
     * 
     * @param requirements the list of dependency requirements to add to
     * @param databaseType the database type to add the dependency requirement for
     */
    private static void addDatabaseRequirements(final List<DependencyRequirement> requirements, final DatabaseType databaseType) {

        switch (databaseType) {
            case POSTGRESQL ->
                addRequirement(requirements, "database=postgresql", coordinate("org.postgresql", "postgresql"));
            case MYSQL ->
                addRequirement(requirements, "database=mysql", coordinate("com.mysql", "mysql-connector-j"));
            case MARIADB ->
                addRequirement(requirements, "database=mariadb", coordinate("org.mariadb.jdbc", "mariadb-java-client"));
            case MSSQL ->
                addRequirement(requirements, "database=mssql", coordinate("com.microsoft.sqlserver", "mssql-jdbc"));
            default -> { }
        }
    }

    /**
     * Adds cache-related dependency requirements to the list based on the given configuration.
     * The added requirements are based on the cache type and are used to determine the required dependencies
     * for a given project.
     *
     * @param requirements the list to which the requirements are added
     * @param configuration the configuration used to determine the cache type and added requirements
     */
    private static void addCacheRequirements(final List<DependencyRequirement> requirements, final CrudConfiguration configuration) {

        final CacheTypeEnum cacheType = Objects.nonNull(configuration.getCache().getType())
                ? configuration.getCache().getType() : CacheTypeEnum.SIMPLE;

        switch (cacheType) {
            case REDIS -> addRequirement(requirements, "cache.type=REDIS",
                    coordinate("org.springframework.boot", "spring-boot-starter-data-redis"));
            case CAFFEINE -> addRequirement(requirements, "cache.type=CAFFEINE",
                    coordinate("com.github.ben-manes.caffeine", "caffeine"));
            case HAZELCAST -> {
                addRequirement(requirements, "cache.type=HAZELCAST", coordinate("com.hazelcast", "hazelcast"));
                addRequirement(requirements, "cache.type=HAZELCAST", coordinate("com.hazelcast", "hazelcast-spring"));
            }
            case SIMPLE -> { }
            default -> { }
        }
    }

    /**
     * Adds a dependency requirement to the list based on the data generator configuration.
     * The requirement is added if the data generator is set to INSTANCIO or PODAM.
     *
     * @param requirements the list of dependency requirements to add to
     * @param configuration the CRUD configuration to resolve the data generator from
     */
    private static void addTestDataGeneratorRequirement(final List<DependencyRequirement> requirements,
            final CrudConfiguration configuration) {

        final DataGeneratorEnum dataGenerator = Objects.nonNull(configuration.getTests().getDataGenerator())
                ? configuration.getTests().getDataGenerator() : DataGeneratorEnum.INSTANCIO;

        switch (dataGenerator) {
            case INSTANCIO -> addRequirement(requirements, "tests.dataGenerator=INSTANCIO",
                    coordinate("org.instancio", "instancio-core"));
            case PODAM -> addRequirement(requirements, "tests.dataGenerator=PODAM",
                    coordinate("uk.co.jemos.podam", "podam"));
            default -> {}
        }
    }

    /**
     * Add a dependency requirement to the given list of requirements.
     * 
     * @param requirements the list of dependency requirements to add the new requirement to
     * @param scenario     the scenario for which the dependency is required
     * @param alternatives the list of dependency coordinates that are expected to be present in the project if the scenario is true
     */
    private static void addRequirement(final List<DependencyRequirement> requirements, final String scenario,
            final DependencyCoordinate... alternatives) {

        requirements.add(new DependencyRequirement(scenario, List.of(alternatives)));
    }

    /**
     * Create a dependency coordinate object.
     * 
     * @param groupId    the group id of the dependency
     * @param artifactId the artifact id of the dependency
     * @return a dependency coordinate object
     */
    private static DependencyCoordinate coordinate(final String groupId, final String artifactId) {
        return new DependencyCoordinate(groupId, artifactId);
    }

    /**
     * Normalize a dependency coordinate by trimming and lowercasing the group id and artifact id.
     * 
     * @param groupId the group id
     * @param artifactId the artifact id
     * @return the normalized coordinate or null if either the group id or artifact id is blank
     */
    private static String normalizeCoordinate(final String groupId, final String artifactId) {
        
        if (StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId)) {
            return null;
        }

        return String.format("%s:%s", groupId.trim(), artifactId.trim()).toLowerCase(Locale.ROOT);
    }

    private record DependencyRequirement(String scenario, List<DependencyCoordinate> alternatives) {
    }

    private record DependencyCoordinate(String groupId, String artifactId) {

        private String coordinate() {
            return normalizeCoordinate(groupId, artifactId);
        }
    }
}
