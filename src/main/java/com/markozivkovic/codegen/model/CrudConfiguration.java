package com.markozivkovic.codegen.model;

import java.util.Objects;

public class CrudConfiguration {
    
    private DatabaseType database;
    private String javaVersion;
    private Boolean optimisticLocking;
    private Boolean dockerfile;
    private Boolean cache;
    
    public CrudConfiguration() {

    }

    public CrudConfiguration(final DatabaseType database, final String javaVersion, final Boolean optimisticLocking,
            final Boolean dockerfile, final Boolean cache) {
        this.database = database;
        this.javaVersion = javaVersion;
        this.optimisticLocking = optimisticLocking;
        this.dockerfile = dockerfile;
        this.cache = cache;
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

    public Boolean isDockerfile() {
        return this.dockerfile;
    }

    public Boolean getDockerfile() {
        return this.dockerfile;
    }

    public CrudConfiguration setDockerfile(final Boolean dockerfile) {
        this.dockerfile = dockerfile;
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
                Objects.equals(dockerfile, crudConfiguration.dockerfile) &&
                Objects.equals(cache, crudConfiguration.cache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, javaVersion, optimisticLocking, dockerfile, cache);
    }

    @Override
    public String toString() {
        return "{" +
            " database='" + getDatabase() + "'" +
            ", javaVersion='" + getJavaVersion() + "'" +
            ", optimisticLocking='" + isOptimisticLocking() + "'" +
            ", dockerfile='" + isDockerfile() + "'" +
            ", cache='" + isCache() + "'" +
            "}";
    }    

    public enum DatabaseType {
        MYSQL,
        POSTGRESQL;
    }

}
