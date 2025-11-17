package com.markozivkovic.codegen.models;

import java.util.List;
import java.util.Objects;

public class ModelDefinition {
    
    private String name;
    private String storageName;
    private String description;
    private Boolean ignore = false;
    private List<FieldDefinition> fields; 
    private AuditDefinition audit;

    public ModelDefinition() {

    }

    public ModelDefinition(final String name, final String storageName, final String description,
            final Boolean ignore, final List<FieldDefinition> fields, final AuditDefinition audit) {
        this.name = name;
        this.storageName = storageName;
        this.description = description;
        this.ignore = ignore;
        this.fields = fields;
        this.audit = audit;
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

    public Boolean getIgnore() {
        return this.ignore;
    }

    public ModelDefinition setIgnore(final Boolean ignore) {
        this.ignore = ignore;
        return this;
    }

    public List<FieldDefinition> getFields() {
        return this.fields;
    }

    public ModelDefinition setFields(final List<FieldDefinition> fields) {
        this.fields = fields;
        return this;
    }

    public AuditDefinition getAudit() {
        return this.audit;
    }

    public ModelDefinition setAudit(final AuditDefinition audit) {
        this.audit = audit;
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
                Objects.equals(ignore, modelDefinition.ignore) &&
                Objects.equals(fields, modelDefinition.fields) &&
                Objects.equals(audit, modelDefinition.audit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, storageName, description, ignore, fields, audit);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", storageName='" + getStorageName() + "'" +
            ", description='" + getDescription() + "'" +
            ", ignore='" + getIgnore() + "'" +
            ", fields='" + getFields() + "'" +
            ", audit='" + getAudit() + "'" +
            "}";
    }    

}
