package com.markozivkovic.codegen.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CrudConfiguration {
    
    private DatabaseType database;
    private String javaVersion;
    private Boolean optimisticLocking;
    private DockerConfiguration docker;
    private Boolean cache;
    private Boolean swagger;
    private Boolean openApiCodegen;
    private Boolean graphQl;
    private ErrorResponse errorResponse;
    private Boolean migrationScripts;
    private TestConfiguration tests;
    private Map<String, Object> additionalProperties = new HashMap<>();
    
    public CrudConfiguration() {

    }

    public CrudConfiguration(final DatabaseType database, final String javaVersion, final Boolean optimisticLocking,
            final DockerConfiguration docker, final Boolean cache, final Boolean swagger, final Boolean openApiCodegen,
            final Boolean graphQl, final ErrorResponse errorResponse, Boolean migrationScripts, final TestConfiguration tests,
            final Map<String, Object> additionalProperties) {
        this.database = database;
        this.javaVersion = javaVersion;
        this.optimisticLocking = optimisticLocking;
        this.docker = docker;
        this.cache = cache;
        this.swagger = swagger;
        this.openApiCodegen = openApiCodegen;
        this.graphQl = graphQl;
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

    public String getJavaVersion() {
        return this.javaVersion;
    }

    public CrudConfiguration setJavaVersion(final String javaVersion) {
        this.javaVersion = javaVersion;
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

    public Boolean isCache() {
        return this.cache;
    }

    public Boolean getCache() {
        return this.cache;
    }

    public CrudConfiguration setCache(final Boolean cache) {
        this.cache = cache;
        return this;
    }

    public Boolean isSwagger() {
        return this.swagger;
    }

    public Boolean getSwagger() {
        return this.swagger;
    }

    public CrudConfiguration setSwagger(final Boolean swagger) {
        this.swagger = swagger;
        return this;
    }

    public Boolean getOpenApiCodegen() {
        return this.openApiCodegen;
    }

    public CrudConfiguration setOpenApiCodegen(final Boolean swaggerCodegen) {
        this.openApiCodegen = swaggerCodegen;
        return this;
    }

    public Boolean getGraphQl() {
        return this.graphQl;
    }

    public CrudConfiguration setGraphQl(final Boolean graphQl) {
        this.graphQl = graphQl;
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
                Objects.equals(optimisticLocking, crudConfiguration.optimisticLocking) &&
                Objects.equals(docker, crudConfiguration.docker) &&
                Objects.equals(cache, crudConfiguration.cache) &&
                Objects.equals(swagger, crudConfiguration.swagger) &&
                Objects.equals(openApiCodegen, crudConfiguration.openApiCodegen) &&
                Objects.equals(graphQl, crudConfiguration.graphQl) &&
                Objects.equals(errorResponse, crudConfiguration.errorResponse) &&
                Objects.equals(migrationScripts, crudConfiguration.migrationScripts) &&
                Objects.equals(tests, crudConfiguration.tests) &&
                Objects.equals(additionalProperties, crudConfiguration.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            database, javaVersion, optimisticLocking, docker, cache, swagger,
            openApiCodegen, graphQl, errorResponse, migrationScripts, tests,
            additionalProperties
        );
    }

    @Override
    public String toString() {
        return "{" +
            " database='" + getDatabase() + "'" +
            ", javaVersion='" + getJavaVersion() + "'" +
            ", optimisticLocking='" + isOptimisticLocking() + "'" +
            ", docker='" + getDocker() + "'" +
            ", cache='" + isCache() + "'" +
            ", swagger='" + isSwagger() + "'" +
            ", openApiCodegen='" + getOpenApiCodegen() + "'" +
            ", graphQl='" + getGraphQl() + "'" +
            ", errorResponse='" + getErrorResponse() + "'" +
            ", migrationScripts='" + isMigrationScripts() + "'" +
            ", tests='" + getTests() + "'" +
            ", additionalProperties='" + getAdditionalProperties() + "'" +
            "}";
    }    

    public enum DatabaseType {
        MYSQL,
        POSTGRESQL,
        MSSQL;
    }

    public enum ErrorResponse {
        DETAILED,
        SIMPLE,
        MINIMAL,
        NONE
    }

    public static class TestConfiguration {

        private Boolean unit;
        private Boolean integration;
        private DataGeneratorEnum dataGenerator;

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

    public static class DockerConfiguration {

        private Boolean dockerfile;
        private Boolean dockerCompose;

        public DockerConfiguration() {

        }

        public DockerConfiguration(final Boolean dockerfile, final Boolean dockerCompose) {
            this.dockerfile = dockerfile;
            this.dockerCompose = dockerCompose;
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

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DockerConfiguration)) {
                return false;
            }
            final DockerConfiguration dockerConfiguration = (DockerConfiguration) o;
            return Objects.equals(dockerfile, dockerConfiguration.dockerfile) &&
                    Objects.equals(dockerCompose, dockerConfiguration.dockerCompose);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dockerfile, dockerCompose);
        }

        @Override
        public String toString() {
            return "{" +
                " dockerfile='" + getDockerfile() + "'" +
                ", dockerCompose='" + getDockerCompose() + "'" +
                "}";
        }
    }

}
