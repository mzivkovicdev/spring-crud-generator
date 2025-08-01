package com.markozivkovic.codegen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.markozivkovic.codegen.utils.FieldUtils;

public class FieldDefinition {
    
    private String name;
    private String type;
    private String description;
    private boolean id;
    private List<String> values = new ArrayList<>();

    public FieldDefinition() {

    }

    public FieldDefinition(final String name, final String type, final String description,
            final boolean id, final List<String> values) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.id = id;
        this.values = values;
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

    public List<String> getValues() {
        return this.values;
    }

    public FieldDefinition setValues(final List<String> values) {
        this.values = values;
        return this;
    }

    public String getResolvedType() {
        return FieldUtils.computeResolvedType(this.type, this.name);
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
                id == fieldDefinition.id &&
                Objects.equals(values, fieldDefinition.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, description, id, values);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", type='" + getType() + "'" +
            ", description='" + getDescription() + "'" +
            ", id='" + isId() + "'" +
            ", values='" + getValues() + "'" +
            "}";
    }

}
