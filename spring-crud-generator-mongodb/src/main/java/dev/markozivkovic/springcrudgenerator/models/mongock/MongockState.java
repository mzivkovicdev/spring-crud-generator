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

public class MongockState {

    private String generatorVersion;
    private int lastVersion;
    private List<MongockCollectionState> collections = new ArrayList<>();

    public MongockState() {

    }

    public MongockState(final String generatorVersion, final int lastVersion, final List<MongockCollectionState> collections) {
        this.generatorVersion = generatorVersion;
        this.lastVersion = lastVersion;
        this.collections = collections;
    }

    public String getGeneratorVersion() {
        return this.generatorVersion;
    }

    public MongockState setGeneratorVersion(final String generatorVersion) {
        this.generatorVersion = generatorVersion;
        return this;
    }

    public int getLastVersion() {
        return this.lastVersion;
    }

    public MongockState setLastVersion(final int lastVersion) {
        this.lastVersion = lastVersion;
        return this;
    }

    public List<MongockCollectionState> getCollections() {
        return this.collections;
    }

    public MongockState setCollections(final List<MongockCollectionState> collections) {
        this.collections = collections;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MongockState)) return false;
        final MongockState that = (MongockState) o;
        return lastVersion == that.lastVersion &&
                Objects.equals(generatorVersion, that.generatorVersion) &&
                Objects.equals(collections, that.collections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generatorVersion, lastVersion, collections);
    }

    @Override
    public String toString() {
        return "{generatorVersion='" + generatorVersion + "', lastVersion=" + lastVersion + ", collections=" + collections + "}";
    }
}
