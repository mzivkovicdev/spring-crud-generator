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

package com.markozivkovic.codegen.models.flyway;

import java.util.Objects;

public class ColumnState {
    
    private String type;
    private Boolean nullable;
    private Boolean unique;
    private String defaultExpr;

    public ColumnState() {

    }

    public ColumnState(final String type, final Boolean nullable, final Boolean unique,
            final String defaultExpr) {
        this.type = type;
        this.nullable = nullable;
        this.unique = unique;
        this.defaultExpr = defaultExpr;
    }

    public String getType() {
        return this.type;
    }

    public ColumnState setType(final String type) {
        this.type = type;
        return this;
    }

    public Boolean isNullable() {
        return this.nullable;
    }

    public Boolean getNullable() {
        return this.nullable;
    }

    public ColumnState setNullable(final Boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public Boolean isUnique() {
        return this.unique;
    }

    public Boolean getUnique() {
        return this.unique;
    }

    public ColumnState setUnique(final Boolean unique) {
        this.unique = unique;
        return this;
    }

    public String getDefaultExpr() {
        return this.defaultExpr;
    }

    public ColumnState setDefaultExpr(final String defaultExpr) {
        this.defaultExpr = defaultExpr;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ColumnState)) {
            return false;
        }
        final ColumnState columnState = (ColumnState) o;
        return Objects.equals(type, columnState.type) &&
                Objects.equals(nullable, columnState.nullable) &&
                Objects.equals(unique, columnState.unique) &&
                Objects.equals(defaultExpr, columnState.defaultExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, nullable, unique, defaultExpr);
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", nullable='" + isNullable() + "'" +
            ", unique='" + isUnique() + "'" +
            ", defaultExpr='" + getDefaultExpr() + "'" +
            "}";
    }    
    
}
