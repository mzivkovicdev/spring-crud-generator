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
    private TestConfiguration tests;
    private Map<String, Object> additionalProperties = new HashMap<>();
    
    public CrudConfiguration() {

    }

    public CrudConfiguration(final DatabaseType database, final Integer javaVersion, final String springBootVersion,
            final Boolean optimisticLocking, final DockerConfiguration docker, final CacheConfiguration cache,
            final OpenApiDefinition openApi, final GraphQLDefinition graphql, final ErrorResponse errorResponse,
            final Boolean migrationScripts, final TestConfiguration tests, final Map<String, Object> additionalProperties) {
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
        this.tests = tests;
        this.additionalProperties = additionalProperties;
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
                Objects.equals(tests, crudConfiguration.tests) &&
                Objects.equals(additionalProperties, crudConfiguration.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            database, javaVersion, springBootVersion, optimisticLocking, docker, cache, openApi,
            graphql, errorResponse, migrationScripts, tests, additionalProperties
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
            ", tests='" + getTests() + "'" +
            ", additionalProperties='" + getAdditionalProperties() + "'" +
            "}";
    }

    public enum DatabaseType {
        MYSQL,
        MARIADB,
        POSTGRESQL,
        MSSQL;
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

}
