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

package dev.markozivkovic.springcrudgenerator.models.mongock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MongockCollectionState {

    private String entityName;
    private String collection;
    private List<MongockFieldState> fields = new ArrayList<>();

    public MongockCollectionState() {

    }

    public MongockCollectionState(final String entityName, final String collection, final List<MongockFieldState> fields) {
        this.entityName = entityName;
        this.collection = collection;
        this.fields = fields;
    }

    public String getEntityName() {
        return this.entityName;
    }

    public MongockCollectionState setEntityName(final String entityName) {
        this.entityName = entityName;
        return this;
    }

    public String getCollection() {
        return this.collection;
    }

    public MongockCollectionState setCollection(final String collection) {
        this.collection = collection;
        return this;
    }

    public List<MongockFieldState> getFields() {
        return this.fields;
    }

    public MongockCollectionState setFields(final List<MongockFieldState> fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MongockCollectionState)) return false;
        final MongockCollectionState that = (MongockCollectionState) o;
        return Objects.equals(entityName, that.entityName) &&
                Objects.equals(collection, that.collection) &&
                Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityName, collection, fields);
    }

    @Override
    public String toString() {
        return "{entityName='" + entityName + "', collection='" + collection + "', fields=" + fields + "}";
    }
}
