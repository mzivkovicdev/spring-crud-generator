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

package dev.markozivkovic.codegen.models;

import java.util.Objects;

public class ColumnDefinition {
    
    private Boolean unique;
    private Boolean nullable;
    private Boolean insertable;
    private Boolean updateable;
    private Integer length;
    private Integer precision;

    public ColumnDefinition() {

    }

    public ColumnDefinition(final Boolean unique, final Boolean nullable, final Boolean insertable,
            final Boolean updateable, final Integer length, final Integer precision) {
        this.unique = unique;
        this.nullable = nullable;
        this.insertable = insertable;
        this.updateable = updateable;
        this.length = length;
        this.precision = precision;
    }

    public Boolean isUnique() {
        return this.unique;
    }

    public Boolean getUnique() {
        return this.unique;
    }

    public ColumnDefinition setUnique(final Boolean unique) {
        this.unique = unique;
        return this;
    }

    public Boolean isNullable() {
        return this.nullable;
    }

    public Boolean getNullable() {
        return this.nullable;
    }

    public ColumnDefinition setNullable(final Boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public Boolean isInsertable() {
        return this.insertable;
    }

    public Boolean getInsertable() {
        return this.insertable;
    }

    public ColumnDefinition setInsertable(final Boolean insertable) {
        this.insertable = insertable;
        return this;
    }

    public Boolean isUpdateable() {
        return this.updateable;
    }

    public Boolean getUpdateable() {
        return this.updateable;
    }

    public ColumnDefinition setUpdateable(final Boolean updateable) {
        this.updateable = updateable;
        return this;
    }

    public Integer getLength() {
        return this.length;
    }

    public ColumnDefinition setLength(final Integer length) {
        this.length = length;
        return this;
    }

    public Integer getPrecision() {
        return this.precision;
    }

    public ColumnDefinition setPrecision(final Integer precision) {
        this.precision = precision;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ColumnDefinition)) {
            return false;
        }
        final ColumnDefinition columnDefinition = (ColumnDefinition) o;
        return Objects.equals(unique, columnDefinition.unique) &&
                Objects.equals(nullable, columnDefinition.nullable) &&
                Objects.equals(insertable, columnDefinition.insertable) &&
                Objects.equals(updateable, columnDefinition.updateable) &&
                Objects.equals(length, columnDefinition.length) &&
                Objects.equals(precision, columnDefinition.precision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unique, nullable, insertable, updateable, length, precision);
    }

    @Override
    public String toString() {
        return "{" +
            " unique='" + isUnique() + "'" +
            ", nullable='" + isNullable() + "'" +
            ", insertable='" + isInsertable() + "'" +
            ", updateable='" + isUpdateable() + "'" +
            ", length='" + getLength() + "'" +
            ", precision='" + getPrecision() + "'" +
            "}";
    }
    
}
