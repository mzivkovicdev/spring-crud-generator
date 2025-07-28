package com.markozivkovic.codegen.model;

import java.util.Objects;

public class FieldDefinition {
    
    private String name;
    private String type;
    private String description;
    private boolean id;

    public FieldDefinition() {

    }

    public FieldDefinition(final String name, final String type, final String description,
            final boolean id) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public FieldDefinition setName(final String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public FieldDefinition setType(final String type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public FieldDefinition setDescription(final String description) {
        this.description = description;
        return this;
    }

    public boolean isId() {
        return this.id;
    }

    public boolean getId() {
        return this.id;
    }

    public FieldDefinition setId(final boolean id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldDefinition)) {
            return false;
        }
        final FieldDefinition fieldDefinition = (FieldDefinition) o;
        return Objects.equals(name, fieldDefinition.name) &&
                Objects.equals(type, fieldDefinition.type) &&
                Objects.equals(description, fieldDefinition.description) &&
                id == fieldDefinition.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, description, id);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", type='" + getType() + "'" +
            ", description='" + getDescription() + "'" +
            ", id='" + isId() + "'" +
            "}";
    }

}
