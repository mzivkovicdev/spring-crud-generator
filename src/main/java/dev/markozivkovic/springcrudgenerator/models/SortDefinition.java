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

package dev.markozivkovic.springcrudgenerator.models;

import java.util.List;
import java.util.Objects;

public class SortDefinition {
    
    private List<String> allowedFields;
    private SortDirection defaultDirection = SortDirection.ASC;

    public SortDefinition() {

    }

    public SortDefinition(final List<String> allowedFields, final SortDirection defaultDirection) {
        this.allowedFields = allowedFields;
        this.defaultDirection = defaultDirection;
    }

    public List<String> getAllowedFields() {
        return this.allowedFields;
    }

    public SortDefinition setAllowedFields(final List<String> allowedFields) {
        this.allowedFields = allowedFields;
        return this;
    }

    public SortDirection getDefaultDirection() {
        return this.defaultDirection;
    }

    public SortDefinition setDefaultDirection(final SortDirection defaultDirection) {
        this.defaultDirection = defaultDirection;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SortDefinition)) {
            return false;
        }
        final SortDefinition sortDefinition = (SortDefinition) o;
        return Objects.equals(allowedFields, sortDefinition.allowedFields) &&
                Objects.equals(defaultDirection, sortDefinition.defaultDirection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowedFields, defaultDirection);
    }

    @Override
    public String toString() {
        return "{" +
            " allowedFields='" + getAllowedFields() + "'" +
            ", defaultDirection='" + getDefaultDirection() + "'" +
            "}";
    }    
}
