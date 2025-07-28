package com.markozivkovic.codegen.model;

import java.util.List;
import java.util.Objects;

public class CrudSpecification {
    
    private List<ModelDefinition> entities;

    public CrudSpecification() {

    }

    public CrudSpecification(final List<ModelDefinition> entities) {
        this.entities = entities;
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
        return Objects.equals(entities, crudSpecification.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entities);
    }

    @Override
    public String toString() {
        return "{" +
            " entities='" + getEntities() + "'" +
            "}";
    }
    
}
