package com.markozivkovic.codegen.model;

import java.util.Objects;

public class AuditDefinition {
    
    private boolean enabled;
    private AuditTypeEnum type;

    public AuditDefinition() {

    }

    public AuditDefinition(final boolean enabled, final AuditTypeEnum type) {
        this.enabled = enabled;
        this.type = type;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public AuditDefinition setEnabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AuditTypeEnum getType() {
        return this.type;
    }

    public AuditDefinition setType(final AuditTypeEnum type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AuditDefinition)) {
            return false;
        }
        final AuditDefinition auditDefinition = (AuditDefinition) o;
        return enabled == auditDefinition.enabled && Objects.equals(type, auditDefinition.type);
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

    public enum AuditTypeEnum {
        INSTANT,
        LOCAL_DATE,
        LOCAL_DATE_TIME
    }

}
