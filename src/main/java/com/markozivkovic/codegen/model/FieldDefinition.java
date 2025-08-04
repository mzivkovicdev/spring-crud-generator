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
    private RelationDefinition relation;

    public FieldDefinition() {

    }

    public FieldDefinition(final String name, final String type, final String description,
            final boolean id, final List<String> values, final RelationDefinition relation) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.id = id;
        this.values = values;
        this.relation = relation;
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

    public RelationDefinition getRelation() {
        return this.relation;
    }

    public FieldDefinition setRelation(final RelationDefinition relation) {
        this.relation = relation;
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
                id == fieldDefinition.id &&
                Objects.equals(values, fieldDefinition.values) &&
                Objects.equals(relation, fieldDefinition.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, description, id, values, relation);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", type='" + getType() + "'" +
            ", description='" + getDescription() + "'" +
            ", id='" + isId() + "'" +
            ", values='" + getValues() + "'" +
            ", relation='" + getRelation() + "'" +
            "}";
    }

}
