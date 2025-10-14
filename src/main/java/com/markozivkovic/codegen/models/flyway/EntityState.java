package com.markozivkovic.codegen.models.flyway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EntityState {

    private String name;
    private String table;
    private String fingerprint;
    private Map<String, ColumnState> columns = new HashMap<>();
    private List<String> pk = new ArrayList<>();
    private AuditState audit;
    private List<FkState> fks = new ArrayList<>();
    private List<FileState> files = new ArrayList<>();
    private List<JoinState> joins = new ArrayList<>();

    public EntityState() {

    }

    public EntityState(final String name, final String table, final String fingerprint,
            final Map<String,ColumnState> columns, final List<String> pk, final AuditState audit,
            final List<FkState> fks, final List<FileState> files, final List<JoinState> joins) {
        this.name = name;
        this.table = table;
        this.fingerprint = fingerprint;
        this.columns = columns;
        this.pk = pk;
        this.audit = audit;
        this.fks = fks;
        this.files = files;
        this.joins = joins;
    }

    public String getName() {
        return this.name;
    }

    public EntityState setName(final String name) {
        this.name = name;
        return this;
    }

    public String getTable() {
        return this.table;
    }

    public EntityState setTable(final String table) {
        this.table = table;
        return this;
    }

    public String getFingerprint() {
        return this.fingerprint;
    }

    public EntityState setFingerprint(final String fingerprint) {
        this.fingerprint = fingerprint;
        return this;
    }

    public Map<String,ColumnState> getColumns() {
        return this.columns;
    }

    public EntityState setColumns(final Map<String,ColumnState> columns) {
        this.columns = columns;
        return this;
    }

    public List<String> getPk() {
        return this.pk;
    }

    public EntityState setPk(final List<String> pk) {
        this.pk = pk;
        return this;
    }

    public AuditState getAudit() {
        return this.audit;
    }

    public EntityState setAudit(final AuditState audit) {
        this.audit = audit;
        return this;
    }

    public List<FkState> getFks() {
        return this.fks;
    }

    public EntityState setFks(final List<FkState> fks) {
        this.fks = fks;
        return this;
    }

    public List<FileState> getFiles() {
        return this.files;
    }

    public EntityState setFiles(final List<FileState> files) {
        this.files = files;
        return this;
    }

    public List<JoinState> getJoins() {
        return this.joins;
    }

    public EntityState setJoins(final List<JoinState> joins) {
        this.joins = joins;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof EntityState)) {
            return false;
        }
        final EntityState entityState = (EntityState) o;
        return Objects.equals(name, entityState.name) &&
                Objects.equals(table, entityState.table) &&
                Objects.equals(fingerprint, entityState.fingerprint) &&
                Objects.equals(columns, entityState.columns) &&
                Objects.equals(pk, entityState.pk) &&
                Objects.equals(audit, entityState.audit) &&
                Objects.equals(fks, entityState.fks) &&
                Objects.equals(files, entityState.files) &&
                Objects.equals(joins, entityState.joins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, table, fingerprint, columns, pk, audit, fks, files, joins);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", table='" + getTable() + "'" +
            ", fingerprint='" + getFingerprint() + "'" +
            ", columns='" + getColumns() + "'" +
            ", pk='" + getPk() + "'" +
            ", audit='" + getAudit() + "'" +
            ", fks='" + getFks() + "'" +
            ", files='" + getFiles() + "'" +
            ", joins='" + getJoins() + "'" +
            "}";
    }    
    
}
