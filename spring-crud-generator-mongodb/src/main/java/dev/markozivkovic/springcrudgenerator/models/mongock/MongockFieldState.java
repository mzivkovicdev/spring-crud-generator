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

import java.util.Objects;

public class MongockFieldState {

    private String name;
    private String bsonType;
    private boolean unique;

    public MongockFieldState() {

    }

    public MongockFieldState(final String name, final String bsonType, final boolean unique) {
        this.name = name;
        this.bsonType = bsonType;
        this.unique = unique;
    }

    public String getName() {
        return this.name;
    }

    public MongockFieldState setName(final String name) {
        this.name = name;
        return this;
    }

    public String getBsonType() {
        return this.bsonType;
    }

    public MongockFieldState setBsonType(final String bsonType) {
        this.bsonType = bsonType;
        return this;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public MongockFieldState setUnique(final boolean unique) {
        this.unique = unique;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MongockFieldState)) return false;
        final MongockFieldState that = (MongockFieldState) o;
        return unique == that.unique &&
                Objects.equals(name, that.name) &&
                Objects.equals(bsonType, that.bsonType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, bsonType, unique);
    }

    @Override
    public String toString() {
        return "{name='" + name + "', bsonType='" + bsonType + "', unique=" + unique + "}";
    }
}
