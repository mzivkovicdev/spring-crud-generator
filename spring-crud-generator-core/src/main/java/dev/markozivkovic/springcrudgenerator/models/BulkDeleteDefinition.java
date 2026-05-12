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

import java.util.Objects;

public class BulkDeleteDefinition {

    private Boolean enabled = Boolean.FALSE;

    public BulkDeleteDefinition() {

    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public BulkDeleteDefinition setEnabled(final Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BulkDeleteDefinition)) {
            return false;
        }
        final BulkDeleteDefinition bulkDeleteDefinition = (BulkDeleteDefinition) o;
        return Objects.equals(enabled, bulkDeleteDefinition.enabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled);
    }

    @Override
    public String toString() {
        return "{" +
            " enabled='" + getEnabled() + "'" +
            "}";
    }
}
