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
     * @param project Maven project model injected by plugin
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

    private static String resolveProjectLabel(final MavenProject project) {

        if (Objects.nonNull(project.getFile())) {
            return project.getFile().toPath().toAbsolutePath().normalize().toString();
        }

        if (StringUtils.isNotBlank(project.getGroupId()) && StringUtils.isNotBlank(project.getArtifactId())) {
            return String.format("%s:%s", project.getGroupId(), project.getArtifactId());
        }

        return "unknown project";
    }

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

    private static String toWarningMessage(final DependencyRequirement requirement) {

        final String expectedDependencies = requirement.alternatives().stream()
                .map(DependencyCoordinate::coordinate)
                .collect(Collectors.joining(" or "));

        return String.format("Missing dependency for %s: %s", requirement.scenario(), expectedDependencies);
    }

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

    private static boolean isOpenApiResourcesEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getOpenApi())
                && Boolean.TRUE.equals(configuration.getOpenApi().getApiSpec())
                && Boolean.TRUE.equals(configuration.getOpenApi().getGenerateResources());
    }

    private static boolean isGraphQlEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getGraphql()) && Boolean.TRUE.equals(configuration.getGraphql().getEnabled());
    }

    private static boolean isCacheEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getCache()) && Boolean.TRUE.equals(configuration.getCache().getEnabled());
    }

    private static boolean isUnitTestsEnabled(final CrudConfiguration configuration) {
        return Objects.nonNull(configuration.getTests()) && Boolean.TRUE.equals(configuration.getTests().getUnit());
    }

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

    private static void addTestDataGeneratorRequirement(final List<DependencyRequirement> requirements,
            final CrudConfiguration configuration) {

        final DataGeneratorEnum dataGenerator = Objects.nonNull(configuration.getTests().getDataGenerator())
                ? configuration.getTests().getDataGenerator() : DataGeneratorEnum.INSTANCIO;

        switch (dataGenerator) {
            case INSTANCIO -> addRequirement(requirements, "tests.dataGenerator=INSTANCIO",
                    coordinate("org.instancio", "instancio-core"));
            case PODAM -> addRequirement(requirements, "tests.dataGenerator=PODAM",
                    coordinate("uk.co.jemos.podam", "podam"));
            default -> { }
        }
    }

    private static void addRequirement(final List<DependencyRequirement> requirements, final String scenario,
            final DependencyCoordinate... alternatives) {
        requirements.add(new DependencyRequirement(scenario, List.of(alternatives)));
    }

    private static DependencyCoordinate coordinate(final String groupId, final String artifactId) {
        return new DependencyCoordinate(groupId, artifactId);
    }

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
