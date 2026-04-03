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

package dev.markozivkovic.springcrudgenerator.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CrudConfiguration {
    
    private DatabaseType database;
    private Integer javaVersion;
    private String springBootVersion;
    private Boolean optimisticLocking;
    private DockerConfiguration docker;
    private CacheConfiguration cache;
    private OpenApiDefinition openApi;
    private GraphQLDefinition graphql;
    private ErrorResponse errorResponse;
    private Boolean migrationScripts;
    private Boolean dependencyCheck = false;
    private TestConfiguration tests;
    private Map<String, Object> additionalProperties = new HashMap<>();
    private RateLimitingConfiguration rateLimiting;

    public CrudConfiguration() {

    }

    public CrudConfiguration(final DatabaseType database, final Integer javaVersion, final String springBootVersion,
            final Boolean optimisticLocking, final DockerConfiguration docker, final CacheConfiguration cache,
            final OpenApiDefinition openApi, final GraphQLDefinition graphql, final ErrorResponse errorResponse,
            final Boolean migrationScripts, final Boolean dependencyCheck, final TestConfiguration tests,
            final Map<String, Object> additionalProperties, final RateLimitingConfiguration rateLimiting) {
        this.database = database;
        this.javaVersion = javaVersion;
        this.springBootVersion = springBootVersion;
        this.optimisticLocking = optimisticLocking;
        this.docker = docker;
        this.cache = cache;
        this.openApi = openApi;
        this.graphql = graphql;
        this.errorResponse = errorResponse;
        this.migrationScripts = migrationScripts;
        this.dependencyCheck = dependencyCheck;
        this.tests = tests;
        this.additionalProperties = additionalProperties;
        this.rateLimiting = rateLimiting;
    }

    public DatabaseType getDatabase() {
        return this.database;
    }

    public CrudConfiguration setDatabase(final DatabaseType database) {
        this.database = database;
        return this;
    }

    public Integer getJavaVersion() {
        return this.javaVersion;
    }

    public CrudConfiguration setJavaVersion(final Integer javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    public String getSpringBootVersion() {
        return this.springBootVersion;
    }

    public CrudConfiguration setSpringBootVersion(final String springBootVersion) {
        this.springBootVersion = springBootVersion;
        return this;
    }

    public Boolean isOptimisticLocking() {
        return this.optimisticLocking;
    }

    public Boolean getOptimisticLocking() {
        return this.optimisticLocking;
    }

    public CrudConfiguration setOptimisticLocking(final Boolean optimisticLocking) {
        this.optimisticLocking = optimisticLocking;
        return this;
    }

    public DockerConfiguration getDocker() {
        return this.docker;
    }

    public CrudConfiguration setDocker(final DockerConfiguration docker) {
        this.docker = docker;
        return this;
    }

    public CacheConfiguration getCache() {
        return this.cache;
    }

    public CrudConfiguration setCache(final CacheConfiguration cache) {
        this.cache = cache;
        return this;
    }

    public OpenApiDefinition getOpenApi() {
        return this.openApi;
    }

    public CrudConfiguration setOpenApi(final OpenApiDefinition openApi) {
        this.openApi = openApi;
        return this;
    }

    public GraphQLDefinition getGraphql() {
        return this.graphql;
    }

    public CrudConfiguration setGraphql(final GraphQLDefinition graphql) {
        this.graphql = graphql;
        return this;
    }

    public ErrorResponse getErrorResponse() {
        return this.errorResponse;
    }

    public CrudConfiguration setErrorResponse(final ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
        return this;
    }

    public Boolean isMigrationScripts() {
        return this.migrationScripts;
    }

    public CrudConfiguration setMigrationScripts(final Boolean migrationScripts) {
        this.migrationScripts = migrationScripts;
        return this;
    }

    public Boolean getDependencyCheck() {
        return this.dependencyCheck;
    }

    public CrudConfiguration setDependencyCheck(final Boolean dependencyCheck) {
        this.dependencyCheck = dependencyCheck;
        return this;
    }

    public TestConfiguration getTests() {
        return this.tests;
    }

    public CrudConfiguration setTests(final TestConfiguration tests) {
        this.tests = tests;
        return this;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public CrudConfiguration setAdditionalProperties(final Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

    public RateLimitingConfiguration getRateLimiting() {
        return this.rateLimiting;
    }

    public CrudConfiguration setRateLimiting(final RateLimitingConfiguration rateLimiting) {
        this.rateLimiting = rateLimiting;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CrudConfiguration)) {
            return false;
        }
        final CrudConfiguration crudConfiguration = (CrudConfiguration) o;
        return Objects.equals(database, crudConfiguration.database) &&
                Objects.equals(javaVersion, crudConfiguration.javaVersion) &&
                Objects.equals(springBootVersion, crudConfiguration.springBootVersion) &&
                Objects.equals(optimisticLocking, crudConfiguration.optimisticLocking) &&
                Objects.equals(docker, crudConfiguration.docker) &&
                Objects.equals(cache, crudConfiguration.cache) &&
                Objects.equals(openApi, crudConfiguration.openApi) &&
                Objects.equals(graphql, crudConfiguration.graphql) &&
                Objects.equals(errorResponse, crudConfiguration.errorResponse) &&
                Objects.equals(migrationScripts, crudConfiguration.migrationScripts) &&
                Objects.equals(dependencyCheck, crudConfiguration.dependencyCheck) &&
                Objects.equals(tests, crudConfiguration.tests) &&
                Objects.equals(additionalProperties, crudConfiguration.additionalProperties) &&
                Objects.equals(rateLimiting, crudConfiguration.rateLimiting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            database, javaVersion, springBootVersion, optimisticLocking, docker, cache, openApi,
            graphql, errorResponse, migrationScripts, dependencyCheck, tests, additionalProperties, rateLimiting
        );
    }

    @Override
    public String toString() {
        return "{" +
            " database='" + getDatabase() + "'" +
            ", javaVersion='" + getJavaVersion() + "'" +
            ", springBootVersion='" + getSpringBootVersion() + "'" +
            ", optimisticLocking='" + isOptimisticLocking() + "'" +
            ", docker='" + getDocker() + "'" +
            ", cache='" + getCache() + "'" +
            ", openApi='" + getOpenApi() + "'" +
            ", graphql='" + getGraphql() + "'" +
            ", errorResponse='" + getErrorResponse() + "'" +
            ", migrationScripts='" + isMigrationScripts() + "'" +
            ", dependencyCheck='" + getDependencyCheck() + "'" +
            ", tests='" + getTests() + "'" +
            ", additionalProperties='" + getAdditionalProperties() + "'" +
            ", rateLimiting='" + getRateLimiting() + "'" +
            "}";
    }

    public enum DatabaseType {
        MYSQL,
        MARIADB,
        POSTGRESQL,
        MSSQL,
        MONGODB;

        public boolean isSql() {
            return this != MONGODB;
        }

        public boolean isMongo() {
            return this == MONGODB;
        }
    }

    public enum ErrorResponse {
        DETAILED,
        SIMPLE,
        MINIMAL,
        NONE
    }

    public static class GraphQLDefinition {

        private Boolean enabled;
        private Boolean scalarConfig;

        public GraphQLDefinition() {}

        public GraphQLDefinition(final Boolean enabled, final Boolean scalarConfig) {
            this.enabled = enabled;
            this.scalarConfig = scalarConfig;
        }

        public Boolean getEnabled() {
            return this.enabled;
        }

        public GraphQLDefinition setEnabled(final Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Boolean getScalarConfig() {
            return this.scalarConfig;
        }

        public GraphQLDefinition setScalarConfig(final Boolean scalarConfig) {
            this.scalarConfig = scalarConfig;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof GraphQLDefinition)) {
                return false;
            }
            final GraphQLDefinition graphQLDefinition = (GraphQLDefinition) o;
            return Objects.equals(enabled, graphQLDefinition.enabled) &&
                    Objects.equals(scalarConfig, graphQLDefinition.scalarConfig);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, scalarConfig);
        }

        @Override
        public String toString() {
            return "{" +
                " enabled='" + getEnabled() + "'" +
                ", scalarConfig='" + getScalarConfig() + "'" +
                "}";
        }
    }

    public static class TestConfiguration {

        private Boolean unit;
        private Boolean integration;
        private DataGeneratorEnum dataGenerator = DataGeneratorEnum.INSTANCIO;

        public TestConfiguration() {

        }

        public TestConfiguration(final Boolean unit, final Boolean integration, final DataGeneratorEnum dataGenerator) {
            this.unit = unit;
            this.integration = integration;
            this.dataGenerator = dataGenerator;
        }

        public Boolean getUnit() {
            return this.unit;
        }

        public TestConfiguration setUnit(final Boolean unit) {
            this.unit = unit;
            return this;
        }

        public Boolean getIntegration() {
            return this.integration;
        }

        public TestConfiguration setIntegration(final Boolean integration) {
            this.integration = integration;
            return this;
        }

        public DataGeneratorEnum getDataGenerator() {
            return this.dataGenerator;
        }

        public TestConfiguration setDataGenerator(final DataGeneratorEnum dataGenerator) {
            this.dataGenerator = dataGenerator;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestConfiguration)) {
                return false;
            }
            final TestConfiguration testConfiguration = (TestConfiguration) o;
            return Objects.equals(unit, testConfiguration.unit) &&
                    Objects.equals(integration, testConfiguration.integration) &&
                    Objects.equals(dataGenerator, testConfiguration.dataGenerator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(unit, integration, dataGenerator);
        }

        @Override
        public String toString() {
            return "{" +
                " unit='" + getUnit() + "'" +
                ", integration='" + getIntegration() + "'" +
                ", dataGenerator='" + getDataGenerator() + "'" +
                "}";
        }

        public enum DataGeneratorEnum {
            PODAM, INSTANCIO
        }
    }

    public static class CacheConfiguration {

        private Boolean enabled;
        private CacheTypeEnum type;
        private Long maxSize;
        private Integer expiration;

        public CacheConfiguration() {}

        public CacheConfiguration(final Boolean enabled, final CacheTypeEnum cacheType,
                    final Long maxSize, final Integer cacheExpiration) {
            this.type = cacheType;
            this.maxSize = maxSize;
            this.expiration = cacheExpiration;
        }

        public Boolean getEnabled() {
            return this.enabled;
        }

        public CacheConfiguration setEnabled(final Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public CacheTypeEnum getType() {
            return this.type;
        }

        public CacheConfiguration setType(final CacheTypeEnum type) {
            this.type = type;
            return this;
        }

        public Long getMaxSize() {
            return this.maxSize;
        }

        public CacheConfiguration setMaxSize(final Long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Integer getExpiration() {
            return this.expiration;
        }

        public CacheConfiguration setExpiration(final Integer expiration) {
            this.expiration = expiration;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof CacheConfiguration)) {
                return false;
            }
            final CacheConfiguration cacheConfiguration = (CacheConfiguration) o;
            return Objects.equals(enabled, cacheConfiguration.enabled) &&
                    Objects.equals(type, cacheConfiguration.type) &&
                    Objects.equals(maxSize, cacheConfiguration.maxSize) &&
                    Objects.equals(expiration, cacheConfiguration.expiration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, type, maxSize, expiration);
        }

        @Override
        public String toString() {
            return "{" +
                " enabled='" + getEnabled() + "'" +
                ", type='" + getType() + "'" +
                ", maxSize='" + getMaxSize() + "'" +
                ", expiration='" + getExpiration() + "'" +
                "}";
        }

        public enum CacheTypeEnum {
            REDIS, CAFFEINE, SIMPLE, HAZELCAST
        }

    }

    public static class DockerConfiguration {

        private Boolean dockerfile;
        private Boolean dockerCompose;
        private ApplicationDockerConfiguration app;
        private DbDockerConfiguration db;

        public DockerConfiguration() {}

        public DockerConfiguration(final Boolean dockerfile, final Boolean dockerCompose,
                    final ApplicationDockerConfiguration app, final DbDockerConfiguration db) {
            this.dockerfile = dockerfile;
            this.dockerCompose = dockerCompose;
            this.app = app;
            this.db = db;
        }

        public Boolean getDockerfile() {
            return this.dockerfile;
        }

        public DockerConfiguration setDockerfile(final Boolean dockerfile) {
            this.dockerfile = dockerfile;
            return this;
        }

        public Boolean getDockerCompose() {
            return this.dockerCompose;
        }

        public DockerConfiguration setDockerCompose(final Boolean dockerCompose) {
            this.dockerCompose = dockerCompose;
            return this;
        }

        public ApplicationDockerConfiguration getApp() {
            return this.app;
        }

        public DockerConfiguration setApp(final ApplicationDockerConfiguration app) {
            this.app = app;
            return this;
        }

        public DbDockerConfiguration getDb() {
            return this.db;
        }

        public DockerConfiguration setDb(final DbDockerConfiguration db) {
            this.db = db;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DockerConfiguration)) {
                return false;
            }
            final DockerConfiguration dockerConfiguration = (DockerConfiguration) o;
            return Objects.equals(dockerfile, dockerConfiguration.dockerfile) &&
                    Objects.equals(dockerCompose, dockerConfiguration.dockerCompose) &&
                    Objects.equals(app, dockerConfiguration.app) &&
                    Objects.equals(db, dockerConfiguration.db);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dockerfile, dockerCompose, app, db);
        }

        @Override
        public String toString() {
            return "{" +
                " dockerfile='" + getDockerfile() + "'" +
                ", dockerCompose='" + getDockerCompose() + "'" +
                ", app='" + getApp() + "'" +
                ", db='" + getDb() + "'" +
                "}";
        }
    }

    public static class ApplicationDockerConfiguration {

        private String image;
        private String tag;
        private Integer port;

        public ApplicationDockerConfiguration() {}

        public ApplicationDockerConfiguration(final String image, final String tag, final Integer port) {
            this.image = image;
            this.tag = tag;
            this.port = port;
        }

        public String getImage() {
            return this.image;
        }

        public ApplicationDockerConfiguration setImage(final String image) {
            this.image = image;
            return this;
        }

        public String getTag() {
            return this.tag;
        }

        public ApplicationDockerConfiguration setTag(final String tag) {
            this.tag = tag;
            return this;
        }

        public Integer getPort() {
            return this.port;
        }

        public ApplicationDockerConfiguration setPort(final Integer port) {
            this.port = port;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ApplicationDockerConfiguration)) {
                return false;
            }
            final ApplicationDockerConfiguration applicationDockerConfiguration = (ApplicationDockerConfiguration) o;
            return Objects.equals(image, applicationDockerConfiguration.image) &&
                    Objects.equals(tag, applicationDockerConfiguration.tag) &&
                    Objects.equals(port, applicationDockerConfiguration.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(image, tag, port);
        }

        @Override
        public String toString() {
            return "{" +
                " image='" + getImage() + "'" +
                ", tag='" + getTag() + "'" +
                ", port='" + getPort() + "'" +
                "}";
        }

    }

    public static class DbDockerConfiguration {

        private String image;
        private String tag;
        private Integer port;

        public DbDockerConfiguration() {}

        public DbDockerConfiguration(final String image, final String tag, final Integer port) {
            this.image = image;
            this.tag = tag;
            this.port = port;
        }

        public String getImage() {
            return this.image;
        }

        public DbDockerConfiguration setImage(final String image) {
            this.image = image;
            return this;
        }

        public String getTag() {
            return this.tag;
        }

        public DbDockerConfiguration setTag(final String tag) {
            this.tag = tag;
            return this;
        }

        public Integer getPort() {
            return this.port;
        }

        public DbDockerConfiguration setPort(final Integer port) {
            this.port = port;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DbDockerConfiguration)) {
                return false;
            }
            final DbDockerConfiguration dbDockerConfiguration = (DbDockerConfiguration) o;
            return Objects.equals(image, dbDockerConfiguration.image) &&
                    Objects.equals(tag, dbDockerConfiguration.tag) &&
                    Objects.equals(port, dbDockerConfiguration.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(image, tag, port);
        }

        @Override
        public String toString() {
            return "{" +
                " image='" + getImage() + "'" +
                ", tag='" + getTag() + "'" +
                ", port='" + getPort() + "'" +
                "}";
        }
    }

    public static class OpenApiDefinition {

        private Boolean apiSpec;
        private Boolean generateResources;

        public OpenApiDefinition() {}

        public OpenApiDefinition(final Boolean apiSpec, final Boolean generateResources) {
            this.apiSpec = apiSpec;
            this.generateResources = generateResources;
        }

        public Boolean getApiSpec() {
            return this.apiSpec;
        }

        public OpenApiDefinition setApiSpec(final Boolean apiSpec) {
            this.apiSpec = apiSpec;
            return this;
        }

        public Boolean getGenerateResources() {

            return this.generateResources;
        }

        public OpenApiDefinition setGenerateResources(final Boolean generateResources) {
            this.generateResources = generateResources;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof OpenApiDefinition)) {
                return false;
            }
            final OpenApiDefinition openApiDefinition = (OpenApiDefinition) o;
            return Objects.equals(apiSpec, openApiDefinition.apiSpec) &&
                    Objects.equals(generateResources, openApiDefinition.generateResources);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiSpec, generateResources);
        }

        @Override
        public String toString() {
            return "{" +
                " apiSpec='" + getApiSpec() + "'" +
                ", generateResources='" + getGenerateResources() + "'" +
                "}";
        }
    }

    public static class RateLimitingConfiguration {

        private Boolean enabled;
        private RateLimitTypeEnum type;
        private KeyStrategyEnum keyStrategy;
        private String keyHeader;
        private RateLimitDefinition global;
        private RateLimitResponseConfig response;

        public RateLimitingConfiguration() {}

        public RateLimitingConfiguration(final Boolean enabled, final RateLimitTypeEnum type,
                final KeyStrategyEnum keyStrategy, final String keyHeader,
                final RateLimitDefinition global, final RateLimitResponseConfig response) {
            this.enabled = enabled;
            this.type = type;
            this.keyStrategy = keyStrategy;
            this.keyHeader = keyHeader;
            this.global = global;
            this.response = response;
        }

        public Boolean getEnabled() {
            return this.enabled;
        }

        public RateLimitingConfiguration setEnabled(final Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public RateLimitTypeEnum getType() {
            return this.type;
        }

        public RateLimitingConfiguration setType(final RateLimitTypeEnum type) {
            this.type = type;
            return this;
        }

        public KeyStrategyEnum getKeyStrategy() {
            return this.keyStrategy;
        }

        public RateLimitingConfiguration setKeyStrategy(final KeyStrategyEnum keyStrategy) {
            this.keyStrategy = keyStrategy;
            return this;
        }

        public String getKeyHeader() {
            return this.keyHeader;
        }

        public RateLimitingConfiguration setKeyHeader(final String keyHeader) {
            this.keyHeader = keyHeader;
            return this;
        }

        public RateLimitDefinition getGlobal() {
            return this.global;
        }

        public RateLimitingConfiguration setGlobal(final RateLimitDefinition global) {
            this.global = global;
            return this;
        }

        public RateLimitResponseConfig getResponse() {
            return this.response;
        }

        public RateLimitingConfiguration setResponse(final RateLimitResponseConfig response) {
            this.response = response;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RateLimitingConfiguration)) {
                return false;
            }
            final RateLimitingConfiguration other = (RateLimitingConfiguration) o;
            return Objects.equals(enabled, other.enabled) &&
                    Objects.equals(type, other.type) &&
                    Objects.equals(keyStrategy, other.keyStrategy) &&
                    Objects.equals(keyHeader, other.keyHeader) &&
                    Objects.equals(global, other.global) &&
                    Objects.equals(response, other.response);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, type, keyStrategy, keyHeader, global, response);
        }

        @Override
        public String toString() {
            return "{" +
                " enabled='" + getEnabled() + "'" +
                ", type='" + getType() + "'" +
                ", keyStrategy='" + getKeyStrategy() + "'" +
                ", keyHeader='" + getKeyHeader() + "'" +
                ", global='" + getGlobal() + "'" +
                ", response='" + getResponse() + "'" +
                "}";
        }

        public enum RateLimitTypeEnum {
            IN_MEMORY, REDIS
        }

        public enum KeyStrategyEnum {
            IP, API_KEY, HEADER, AUTHENTICATED_USER
        }
    }

    public static class RateLimitDefinition {

        private Long capacity;
        private Long refillTokens;
        private Long refillDuration;
        private RateLimitDefinition overdraft;

        public RateLimitDefinition() {}

        public RateLimitDefinition(final Long capacity, final Long refillTokens, final Long refillDuration,
                final RateLimitDefinition overdraft) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillDuration = refillDuration;
            this.overdraft = overdraft;
        }

        public Long getCapacity() {
            return this.capacity;
        }

        public RateLimitDefinition setCapacity(final Long capacity) {
            this.capacity = capacity;
            return this;
        }

        public Long getRefillTokens() {
            return this.refillTokens;
        }

        public RateLimitDefinition setRefillTokens(final Long refillTokens) {
            this.refillTokens = refillTokens;
            return this;
        }

        public Long getRefillDuration() {
            return this.refillDuration;
        }

        public RateLimitDefinition setRefillDuration(final Long refillDuration) {
            this.refillDuration = refillDuration;
            return this;
        }

        public RateLimitDefinition getOverdraft() {
            return this.overdraft;
        }

        public RateLimitDefinition setOverdraft(final RateLimitDefinition overdraft) {
            this.overdraft = overdraft;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RateLimitDefinition)) {
                return false;
            }
            final RateLimitDefinition other = (RateLimitDefinition) o;
            return Objects.equals(capacity, other.capacity) &&
                    Objects.equals(refillTokens, other.refillTokens) &&
                    Objects.equals(refillDuration, other.refillDuration) &&
                    Objects.equals(overdraft, other.overdraft);
        }

        @Override
        public int hashCode() {
            return Objects.hash(capacity, refillTokens, refillDuration, overdraft);
        }

        @Override
        public String toString() {
            return "{" +
                " capacity='" + getCapacity() + "'" +
                ", refillTokens='" + getRefillTokens() + "'" +
                ", refillDuration='" + getRefillDuration() + "'" +
                ", overdraft='" + getOverdraft() + "'" +
                "}";
        }
    }

    public static class RateLimitResponseConfig {

        private Integer statusCode;
        private Boolean includeHeaders;
        private String message;

        public RateLimitResponseConfig() {}

        public RateLimitResponseConfig(final Integer statusCode, final Boolean includeHeaders, final String message) {
            this.statusCode = statusCode;
            this.includeHeaders = includeHeaders;
            this.message = message;
        }

        public Integer getStatusCode() {
            return this.statusCode;
        }

        public RateLimitResponseConfig setStatusCode(final Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Boolean getIncludeHeaders() {
            return this.includeHeaders;
        }

        public RateLimitResponseConfig setIncludeHeaders(final Boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
            return this;
        }

        public String getMessage() {
            return this.message;
        }

        public RateLimitResponseConfig setMessage(final String message) {
            this.message = message;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RateLimitResponseConfig)) {
                return false;
            }
            final RateLimitResponseConfig other = (RateLimitResponseConfig) o;
            return Objects.equals(statusCode, other.statusCode) &&
                    Objects.equals(includeHeaders, other.includeHeaders) &&
                    Objects.equals(message, other.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statusCode, includeHeaders, message);
        }

        @Override
        public String toString() {
            return "{" +
                " statusCode='" + getStatusCode() + "'" +
                ", includeHeaders='" + getIncludeHeaders() + "'" +
                ", message='" + getMessage() + "'" +
                "}";
        }
    }

}
