package com.markozivkovic.codegen.models.flyway;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MigrationState {
    
    private String generatorVersion;
    private Integer lastScriptVersion;
    private List<EntityState> entities = new ArrayList<>();

    public MigrationState() {

    }

    public MigrationState(final String generatorVersion, final Integer lastScriptVersion, final List<EntityState> entities) {
        this.generatorVersion = generatorVersion;
        this.lastScriptVersion = lastScriptVersion;
        this.entities = entities;
    }

    public String getGeneratorVersion() {
        return this.generatorVersion;
    }

    public MigrationState setGeneratorVersion(final String generatorVersion) {
        this.generatorVersion = generatorVersion;
        return this;
    }

    public Integer getLastScriptVersion() {
        return this.lastScriptVersion;
    }

    public MigrationState setLastScriptVersion(final Integer lastScriptVersion) {
        this.lastScriptVersion = lastScriptVersion;
        return this;
    }

    public List<EntityState> getEntities() {
        return this.entities;
    }

    public MigrationState setEntities(final List<EntityState> entities) {
        this.entities = entities;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MigrationState)) {
            return false;
        }
        final MigrationState migrationState = (MigrationState) o;
        return Objects.equals(generatorVersion, migrationState.generatorVersion) &&
                Objects.equals(lastScriptVersion, migrationState.lastScriptVersion) &&
                Objects.equals(entities, migrationState.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generatorVersion, lastScriptVersion, entities);
    }

    @Override
    public String toString() {
        return "{" +
            " generatorVersion='" + getGeneratorVersion() + "'" +
            ", lastScriptVersion='" + getLastScriptVersion() + "'" +
            ", entities='" + getEntities() + "'" +
            "}";
    }    

}
