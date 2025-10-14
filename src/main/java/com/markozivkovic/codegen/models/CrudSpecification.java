package com.markozivkovic.codegen.models;

import java.util.List;
import java.util.Objects;

public class CrudSpecification {
    
    private CrudConfiguration configuration;
    private List<ModelDefinition> entities;

    public CrudSpecification() {

    }

    public CrudSpecification(final CrudConfiguration configuration, final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.entities = entities;
    }

    public CrudConfiguration getConfiguration() {
        return this.configuration;
    }

    public CrudSpecification setConfiguration(final CrudConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    public List<ModelDefinition> getEntities() {
        return this.entities;
    }

    public CrudSpecification setEntities(final List<ModelDefinition> entities) {
        this.entities = entities;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CrudSpecification)) {
            return false;
        }
        final CrudSpecification crudSpecification = (CrudSpecification) o;
        return Objects.equals(configuration, crudSpecification.configuration) &&
                Objects.equals(entities, crudSpecification.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration, entities);
    }

    @Override
    public String toString() {
        return "{" +
            " configuration='" + getConfiguration() + "'" +
            " entities='" + getEntities() + "'" +
            "}";
    }
    
}
