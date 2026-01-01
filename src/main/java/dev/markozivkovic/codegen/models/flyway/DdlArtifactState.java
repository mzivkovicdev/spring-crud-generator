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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DdlArtifactState {

    private DdlArtifactType type;
    private String name;
    private String ownerTable;
    private List<FileState> files = new ArrayList<>();

    public DdlArtifactState() {
    }

    public DdlArtifactState(final DdlArtifactType type, final String name, final String ownerTable, final List<FileState> files) {
        this.type = type;
        this.name = name;
        this.ownerTable = ownerTable;
        this.files = files;
    }

    public DdlArtifactType getType() {
        return this.type;
    }

    public DdlArtifactState setType(final DdlArtifactType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public DdlArtifactState setName(final String name) {
        this.name = name;
        return this;
    }

    public String getOwnerTable() {
        return this.ownerTable;
    }

    public DdlArtifactState setOwnerTable(final String ownerTable) {
        this.ownerTable = ownerTable;
        return this;
    }

    public List<FileState> getFiles() {
        return this.files;
    }

    public DdlArtifactState setFiles(final List<FileState> files) {
        this.files = files;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DdlArtifactState)) {
            return false;
        }
        final DdlArtifactState ddlArtifactState = (DdlArtifactState) o;
        return Objects.equals(type, ddlArtifactState.type) &&
                Objects.equals(name, ddlArtifactState.name) &&
                Objects.equals(ownerTable, ddlArtifactState.ownerTable) &&
                Objects.equals(files, ddlArtifactState.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, ownerTable, files);
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", name='" + getName() + "'" +
            ", ownerTable='" + getOwnerTable() + "'" +
            ", files='" + getFiles() + "'" +
            "}";
    }    

    public enum DdlArtifactType { SEQUENCE, TABLE_GENERATOR }
    
}
