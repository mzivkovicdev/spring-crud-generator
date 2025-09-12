package com.markozivkovic.codegen.model.flyway;

import java.util.Objects;

public class AuditState {
    
    private Boolean enabled;
    private String type;

    public AuditState() {

    }

    public AuditState(final Boolean enabled, final String type) {
        this.enabled = enabled;
        this.type = type;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public AuditState setEnabled(final Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public AuditState setType(final String type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AuditState)) {
            return false;
        }
        final AuditState auditState = (AuditState) o;
        return Objects.equals(enabled, auditState.enabled) &&
                Objects.equals(type, auditState.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, type);
    }

    @Override
    public String toString() {
        return "{" +
            " enabled='" + isEnabled() + "'" +
            ", type='" + getType() + "'" +
            "}";
    }    

}
