package com.markozivkovic.codegen.model;
import java.util.Objects;

public class RelationDefinition {

    private String type;
    private String joinColumn;
    private String fetch;
    private String cascade;
    
    public RelationDefinition() {

    }

    public RelationDefinition(final String type, final String joinColumn,
            final String fetch, final String cascade) {
        this.type = type;
        this.joinColumn = joinColumn;
        this.fetch = fetch;
        this.cascade = cascade;
    }

    public String getType() {
        return this.type;
    }

    public RelationDefinition setType(final String type) {
        this.type = type;
        return this;
    }

    public String getJoinColumn() {
        return this.joinColumn;
    }

    public RelationDefinition setJoinColumn(final String joinColumn) {
        this.joinColumn = joinColumn;
        return this;
    }

    public String getFetch() {
        return this.fetch;
    }

    public RelationDefinition setFetch(final String fetch) {
        this.fetch = fetch;
        return this;
    }

    public String getCascade() {
        return this.cascade;
    }

    public RelationDefinition setCascade(final String cascade) {
        this.cascade = cascade;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RelationDefinition)) {
            return false;
        }
        RelationDefinition relationDefinition = (RelationDefinition) o;
        return Objects.equals(type, relationDefinition.type) &&
                Objects.equals(joinColumn, relationDefinition.joinColumn) &&
                Objects.equals(fetch, relationDefinition.fetch) &&
                Objects.equals(cascade, relationDefinition.cascade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, joinColumn, fetch, cascade);
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", joinColumn='" + getJoinColumn() + "'" +
            ", fetch='" + getFetch() + "'" +
            ", cascade='" + getCascade() + "'" +
            "}";
    }
    
}
