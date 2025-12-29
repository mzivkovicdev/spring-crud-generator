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

import java.util.Objects;

public class FkState {
    
    private String column;
    private String refTable;
    private String refColumn;

    public FkState() {

    }

    public FkState(final String column, final String refTable, final String refColumn) {
        this.column = column;
        this.refTable = refTable;
        this.refColumn = refColumn;
    }

    public String getColumn() {
        return this.column;
    }

    public FkState setColumn(final String column) {
        this.column = column;
        return this;
    }

    public String getRefTable() {
        return this.refTable;
    }

    public FkState setRefTable(final String refTable) {
        this.refTable = refTable;
        return this;
    }

    public String getRefColumn() {
        return this.refColumn;
    }

    public FkState setRefColumn(final String refColumn) {
        this.refColumn = refColumn;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FkState)) {
            return false;
        }
        final FkState fkState = (FkState) o;
        return Objects.equals(column, fkState.column) &&
                Objects.equals(refTable, fkState.refTable) &&
                Objects.equals(refColumn, fkState.refColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, refTable, refColumn);
    }

    @Override
    public String toString() {
        return "{" +
            " column='" + getColumn() + "'" +
            ", refTable='" + getRefTable() + "'" +
            ", refColumn='" + getRefColumn() + "'" +
            "}";
    }    

}
