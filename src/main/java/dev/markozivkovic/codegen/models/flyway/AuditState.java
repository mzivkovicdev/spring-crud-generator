/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.codegen.models.flyway;

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
