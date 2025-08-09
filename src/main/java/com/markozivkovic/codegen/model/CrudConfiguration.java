package com.markozivkovic.codegen.model;

import java.util.Objects;

public class CrudConfiguration {
    
    private DatabaseType database;
    private String javaVersion;
    private Boolean optimisticLocking;
    private Boolean dockerfile;
    
    public CrudConfiguration() {

    }

    public CrudConfiguration(final DatabaseType database, final String javaVersion, final Boolean optimisticLocking,
            final Boolean dockerfile) {
        this.database = database;
        this.javaVersion = javaVersion;
        this.optimisticLocking = optimisticLocking;
        this.dockerfile = dockerfile;
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
                Objects.equals(dockerfile, crudConfiguration.dockerfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, javaVersion, optimisticLocking, dockerfile);
    }

    @Override
    public String toString() {
        return "{" +
            " database='" + getDatabase() + "'" +
            ", javaVersion='" + getJavaVersion() + "'" +
            ", optimisticLocking='" + isOptimisticLocking() + "'" +
            ", dockerfile='" + isDockerfile() + "'" +
            "}";
    }    

    public enum DatabaseType {
        MYSQL,
        POSTGRESQL;
    }

}
