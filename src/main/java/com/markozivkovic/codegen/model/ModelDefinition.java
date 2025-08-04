package com.markozivkovic.codegen.model;

import java.util.List;
import java.util.Objects;

public class ModelDefinition {
    
    private String name;
    private String storageName;
    private String description;
    private List<FieldDefinition> fields; 

    public ModelDefinition() {

    }

    public ModelDefinition(final String name, final String storageName, final String description,
            final List<FieldDefinition> fields) {
        this.name = name;
        this.storageName = storageName;
        this.description = description;
        this.fields = fields;
    }

    public String getName() {
        return this.name;
    }

    public ModelDefinition setName(final String name) {
        this.name = name;
        return this;
    }

    public String getStorageName() {
        return this.storageName;
    }

    public ModelDefinition setStorageName(final String tableName) {
        this.storageName = tableName;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public ModelDefinition setDescription(final String description) {
        this.description = description;
        return this;
    }

    public List<FieldDefinition> getFields() {
        return this.fields;
    }

    public ModelDefinition setFields(final List<FieldDefinition> fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ModelDefinition)) {
            return false;
        }
        final ModelDefinition modelDefinition = (ModelDefinition) o;
        return Objects.equals(name, modelDefinition.name) &&
                Objects.equals(storageName, modelDefinition.storageName) &&
                Objects.equals(description, modelDefinition.description) &&
                Objects.equals(fields, modelDefinition.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, storageName, description, fields);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", storageName='" + getStorageName() + "'" +
            ", description='" + getDescription() + "'" +
            ", fields='" + getFields() + "'" +
            "}";
    }    

}
