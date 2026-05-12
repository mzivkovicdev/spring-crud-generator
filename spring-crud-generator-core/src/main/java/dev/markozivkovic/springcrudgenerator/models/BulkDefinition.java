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

public class BulkDefinition {

    private BulkCreateDefinition create = new BulkCreateDefinition();
    private BulkDeleteDefinition delete = new BulkDeleteDefinition();

    public BulkDefinition() {

    }

    public BulkDefinition(final BulkCreateDefinition create, final BulkDeleteDefinition delete) {
        this.create = create;
        this.delete = delete;
    }

    public BulkCreateDefinition getCreate() {
        return this.create;
    }

    public BulkDefinition setCreate(final BulkCreateDefinition create) {
        this.create = create;
        return this;
    }

    public BulkDeleteDefinition getDelete() {
        return this.delete;
    }

    public BulkDefinition setDelete(final BulkDeleteDefinition delete) {
        this.delete = delete;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BulkDefinition)) {
            return false;
        }
        final BulkDefinition bulkDefinition = (BulkDefinition) o;
        return Objects.equals(create, bulkDefinition.create)
                && Objects.equals(delete, bulkDefinition.delete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(create, delete);
    }

    @Override
    public String toString() {
        return "{" +
            " create='" + getCreate() + "'" +
            ", delete='" + getDelete() + "'" +
            "}";
    }
}
