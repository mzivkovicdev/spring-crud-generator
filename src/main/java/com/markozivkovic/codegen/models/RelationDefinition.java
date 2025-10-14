package com.markozivkovic.codegen.models;

import java.util.Objects;

public class RelationDefinition {

    private String type;
    private String joinColumn;
    private String fetch;
    private String cascade;
    private JoinTableDefinition joinTable;
    
    public RelationDefinition() {

    }

    public RelationDefinition(final String type, final String joinColumn,
            final String fetch, final String cascade, final JoinTableDefinition joinTable) {
        this.type = type;
        this.joinColumn = joinColumn;
        this.fetch = fetch;
        this.cascade = cascade;
        this.joinTable = joinTable;
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

    public JoinTableDefinition getJoinTable() {
        return this.joinTable;
    }

    public RelationDefinition setJoinTable(final JoinTableDefinition joinTable) {
        this.joinTable = joinTable;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RelationDefinition)) {
            return false;
        }
        final RelationDefinition relationDefinition = (RelationDefinition) o;
        return Objects.equals(type, relationDefinition.type) &&
                Objects.equals(joinColumn, relationDefinition.joinColumn) &&
                Objects.equals(fetch, relationDefinition.fetch) &&
                Objects.equals(cascade, relationDefinition.cascade) &&
                Objects.equals(joinTable, relationDefinition.joinTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, joinColumn, fetch, cascade, joinTable);
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", joinColumn='" + getJoinColumn() + "'" +
            ", fetch='" + getFetch() + "'" +
            ", cascade='" + getCascade() + "'" +
            ", joinTable='" + getJoinTable() + "'" +
            "}";
    }

    public static class JoinTableDefinition {

        private String name;
        private String joinColumn;
        private String inverseJoinColumn;
        
        public JoinTableDefinition() {
            
        }
        
        public JoinTableDefinition(final String name, final String joinColumn,
                final String inverseJoinColumn) {
            this.name = name;
            this.joinColumn = joinColumn;
            this.inverseJoinColumn = inverseJoinColumn;
        }

        public String getName() {
            return this.name;
        }

        public JoinTableDefinition setName(final String name) {
            this.name = name;
            return this;
        }

        public String getJoinColumn() {
            return this.joinColumn;
        }

        public JoinTableDefinition setJoinColumn(final String joinColumn) {
            this.joinColumn = joinColumn;
            return this;
        }

        public String getInverseJoinColumn() {
            return this.inverseJoinColumn;
        }

        public JoinTableDefinition setInverseJoinColumn(final String inverseJoinColumn) {
            this.inverseJoinColumn = inverseJoinColumn;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof JoinTableDefinition)) {
                return false;
            }
            final JoinTableDefinition joinTableDefinition = (JoinTableDefinition) o;
            return Objects.equals(name, joinTableDefinition.name) &&
                    Objects.equals(joinColumn, joinTableDefinition.joinColumn) &&
                    Objects.equals(inverseJoinColumn, joinTableDefinition.inverseJoinColumn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, joinColumn, inverseJoinColumn);
        }

        @Override
        public String toString() {
            return "{" +
                " name='" + getName() + "'" +
                ", joinColumn='" + getJoinColumn() + "'" +
                ", inverseJoinColumn='" + getInverseJoinColumn() + "'" +
                "}";
        }
    }
    
}
