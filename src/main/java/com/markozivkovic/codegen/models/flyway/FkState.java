package com.markozivkovic.codegen.models.flyway;
import java.util.Objects;

public class FkState {
    
    private String column;
    private String refTable;
    private String refColumn;

    public FkState() {

    }

    public FkState(final String column, final String refTable, final String refColumn) {
        this.column = column;
        this.refTable = refTable;
        this.refColumn = refColumn;
    }

    public String getColumn() {
        return this.column;
    }

    public FkState setColumn(final String column) {
        this.column = column;
        return this;
    }

    public String getRefTable() {
        return this.refTable;
    }

    public FkState setRefTable(final String refTable) {
        this.refTable = refTable;
        return this;
    }

    public String getRefColumn() {
        return this.refColumn;
    }

    public FkState setRefColumn(final String refColumn) {
        this.refColumn = refColumn;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FkState)) {
            return false;
        }
        final FkState fkState = (FkState) o;
        return Objects.equals(column, fkState.column) &&
                Objects.equals(refTable, fkState.refTable) &&
                Objects.equals(refColumn, fkState.refColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, refTable, refColumn);
    }

    @Override
    public String toString() {
        return "{" +
            " column='" + getColumn() + "'" +
            ", refTable='" + getRefTable() + "'" +
            ", refColumn='" + getRefColumn() + "'" +
            "}";
    }    

}
