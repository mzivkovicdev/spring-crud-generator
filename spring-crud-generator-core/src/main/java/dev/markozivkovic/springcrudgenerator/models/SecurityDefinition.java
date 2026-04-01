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

/**
 * Entity-level access control definition. Each field maps an endpoint operation
 * to the list of roles required to access it. When null/absent, the global
 * security policy applies (requires authenticated).
 */
public class SecurityDefinition {

    private List<String> getAll;
    private List<String> getById;
    private List<String> create;
    private List<String> update;
    private List<String> delete;

    public SecurityDefinition() {}

    public SecurityDefinition(final List<String> getAll, final List<String> getById,
            final List<String> create, final List<String> update, final List<String> delete) {
        this.getAll = getAll;
        this.getById = getById;
        this.create = create;
        this.update = update;
        this.delete = delete;
    }

    public List<String> getGetAll() {
        return this.getAll;
    }

    public SecurityDefinition setGetAll(final List<String> getAll) {
        this.getAll = getAll;
        return this;
    }

    public List<String> getGetById() {
        return this.getById;
    }

    public SecurityDefinition setGetById(final List<String> getById) {
        this.getById = getById;
        return this;
    }

    public List<String> getCreate() {
        return this.create;
    }

    public SecurityDefinition setCreate(final List<String> create) {
        this.create = create;
        return this;
    }

    public List<String> getUpdate() {
        return this.update;
    }

    public SecurityDefinition setUpdate(final List<String> update) {
        this.update = update;
        return this;
    }

    public List<String> getDelete() {
        return this.delete;
    }

    public SecurityDefinition setDelete(final List<String> delete) {
        this.delete = delete;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SecurityDefinition)) {
            return false;
        }
        final SecurityDefinition sd = (SecurityDefinition) o;
        return Objects.equals(getAll, sd.getAll) &&
                Objects.equals(getById, sd.getById) &&
                Objects.equals(create, sd.create) &&
                Objects.equals(update, sd.update) &&
                Objects.equals(delete, sd.delete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAll, getById, create, update, delete);
    }

    @Override
    public String toString() {
        return "{" +
            " getAll='" + getGetAll() + "'" +
            ", getById='" + getGetById() + "'" +
            ", create='" + getCreate() + "'" +
            ", update='" + getUpdate() + "'" +
            ", delete='" + getDelete() + "'" +
            "}";
    }
}
