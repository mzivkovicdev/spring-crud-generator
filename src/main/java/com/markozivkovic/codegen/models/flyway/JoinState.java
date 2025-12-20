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

package com.markozivkovic.codegen.models.flyway;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JoinState {
    
    private String table;
    private JoinSide left;
    private JoinSide right;
    private List<FileState> files = new ArrayList<>();

    public JoinState() {

    }

    public JoinState(final String table, final JoinSide left, final JoinSide right, final List<FileState> files) {
        this.table = table;
        this.left = left;
        this.right = right;
        this.files = files;
    }

    public String getTable() {
        return this.table;
    }

    public JoinState setTable(final String table) {
        this.table = table;
        return this;
    }

    public JoinSide getLeft() {
        return this.left;
    }

    public JoinState setLeft(final JoinSide left) {
        this.left = left;
        return this;
    }

    public JoinSide getRight() {
        return this.right;
    }

    public JoinState setRight(final JoinSide right) {
        this.right = right;
        return this;
    }

    public List<FileState> getFiles() {
        return this.files;
    }

    public JoinState setFiles(final List<FileState> files) {
        this.files = files;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JoinState)) {
            return false;
        }
        final JoinState joinState = (JoinState) o;
        return Objects.equals(table, joinState.table) &&
                Objects.equals(left, joinState.left) &&
                Objects.equals(right, joinState.right) &&
                Objects.equals(files, joinState.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, left, right, files);
    }

    @Override
    public String toString() {
        return "{" +
            " table='" + getTable() + "'" +
            ", left='" + getLeft() + "'" +
            ", right='" + getRight() + "'" +
            ", files='" + getFiles() + "'" +
            "}";
    }    

    public static class JoinSide {

        private String table;
        private String column;

        public JoinSide() {

        }

        public JoinSide(final String table, final String column) {
            this.table = table;
            this.column = column;
        }

        public String getTable() {
            return this.table;
        }

        public JoinSide setTable(final String table) {
            this.table = table;
            return this;
        }

        public String getColumn() {
            return this.column;
        }

        public JoinSide setColumn(final String column) {
            this.column = column;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof JoinSide)) {
                return false;
            }
            final JoinSide joinSide = (JoinSide) o;
            return Objects.equals(table, joinSide.table) &&
                    Objects.equals(column, joinSide.column);
        }

        @Override
        public int hashCode() {
            return Objects.hash(table, column);
        }

        @Override
        public String toString() {
            return "{" +
                " table='" + getTable() + "'" +
                ", column='" + getColumn() + "'" +
                "}";
        }
        
    }

}
