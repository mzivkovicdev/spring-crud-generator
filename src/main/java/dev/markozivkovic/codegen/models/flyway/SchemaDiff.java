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

public class SchemaDiff {

    public static class ColumnChange {

        private String name;
        private String oldType;
        private String newType;
        private Boolean oldNullable;
        private Boolean newNullable;
        private Boolean oldUnique;
        private Boolean newUnique;
        private String oldDefault;
        private String newDefault;
        private Boolean typeChanged;
        private Boolean nullableChanged;
        private Boolean uniqueChanged;
        private Boolean defaultChanged;

        public ColumnChange() { }

        public ColumnChange(final String name, final String oldType, final String newType, final Boolean oldNullable,
                final Boolean newNullable, final Boolean oldUnique, final Boolean newUnique, final String oldDefault,
                final String newDefault, final Boolean typeChanged, final Boolean nullableChanged, final Boolean uniqueChanged,
                final Boolean defaultChanged) {
            this.name = name;
            this.oldType = oldType;
            this.newType = newType;
            this.oldNullable = oldNullable;
            this.newNullable = newNullable;
            this.oldUnique = oldUnique;
            this.newUnique = newUnique;
            this.oldDefault = oldDefault;
            this.newDefault = newDefault;
            this.typeChanged = typeChanged;
            this.nullableChanged = nullableChanged;
            this.uniqueChanged = uniqueChanged;
            this.defaultChanged = defaultChanged;
        }

        public String getName() {
            return this.name;
        }

        public ColumnChange setName(final String name) {
            this.name = name;
            return this;
        }

        public String getOldType() {
            return this.oldType;
        }

        public ColumnChange setOldType(final String oldType) {
            this.oldType = oldType;
            return this;
        }

        public String getNewType() {
            return this.newType;
        }

        public ColumnChange setNewType(final String newType) {
            this.newType = newType;
            return this;
        }

        public Boolean isOldNullable() {
            return this.oldNullable;
        }

        public Boolean getOldNullable() {
            return this.oldNullable;
        }

        public ColumnChange setOldNullable(final Boolean oldNullable) {
            this.oldNullable = oldNullable;
            return this;
        }

        public Boolean isNewNullable() {
            return this.newNullable;
        }

        public Boolean getNewNullable() {
            return this.newNullable;
        }

        public ColumnChange setNewNullable(final Boolean newNullable) {
            this.newNullable = newNullable;
            return this;
        }

        public Boolean isOldUnique() {
            return this.oldUnique;
        }

        public Boolean getOldUnique() {
            return this.oldUnique;
        }

        public ColumnChange setOldUnique(final Boolean oldUnique) {
            this.oldUnique = oldUnique;
            return this;
        }

        public Boolean isNewUnique() {
            return this.newUnique;
        }

        public Boolean getNewUnique() {
            return this.newUnique;
        }

        public ColumnChange setNewUnique(final Boolean newUnique) {
            this.newUnique = newUnique;
            return this;
        }

        public String getOldDefault() {
            return this.oldDefault;
        }

        public ColumnChange setOldDefault(final String oldDefault) {
            this.oldDefault = oldDefault;
            return this;
        }

        public String getNewDefault() {
            return this.newDefault;
        }

        public ColumnChange setNewDefault(final String newDefault) {
            this.newDefault = newDefault;
            return this;
        }

        public Boolean isTypeChanged() {
            return this.typeChanged;
        }

        public Boolean getTypeChanged() {
            return this.typeChanged;
        }

        public ColumnChange setTypeChanged(final Boolean typeChanged) {
            this.typeChanged = typeChanged;
            return this;
        }

        public Boolean isNullableChanged() {
            return this.nullableChanged;
        }

        public Boolean getNullableChanged() {
            return this.nullableChanged;
        }

        public ColumnChange setNullableChanged(final Boolean nullableChanged) {
            this.nullableChanged = nullableChanged;
            return this;
        }

        public Boolean isUniqueChanged() {
            return this.uniqueChanged;
        }

        public Boolean getUniqueChanged() {
            return this.uniqueChanged;
        }

        public ColumnChange setUniqueChanged(final Boolean uniqueChanged) {
            this.uniqueChanged = uniqueChanged;
            return this;
        }

        public Boolean isDefaultChanged() {
            return this.defaultChanged;
        }

        public Boolean getDefaultChanged() {
            return this.defaultChanged;
        }

        public ColumnChange setDefaultChanged(final Boolean defaultChanged) {
            this.defaultChanged = defaultChanged;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ColumnChange)) {
                return false;
            }
            final ColumnChange columnChange = (ColumnChange) o;
            return Objects.equals(name, columnChange.name) &&
                    Objects.equals(oldType, columnChange.oldType) &&
                    Objects.equals(newType, columnChange.newType) &&
                    Objects.equals(oldNullable, columnChange.oldNullable) &&
                    Objects.equals(newNullable, columnChange.newNullable) &&
                    Objects.equals(oldUnique, columnChange.oldUnique) &&
                    Objects.equals(newUnique, columnChange.newUnique) &&
                    Objects.equals(oldDefault, columnChange.oldDefault) &&
                    Objects.equals(newDefault, columnChange.newDefault) &&
                    Objects.equals(typeChanged, columnChange.typeChanged) &&
                    Objects.equals(nullableChanged, columnChange.nullableChanged) &&
                    Objects.equals(uniqueChanged, columnChange.uniqueChanged) &&
                    Objects.equals(defaultChanged, columnChange.defaultChanged);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    name, oldType, newType, oldNullable, newNullable, oldUnique, newUnique,
                    oldDefault, newDefault, typeChanged, nullableChanged, uniqueChanged,
                    defaultChanged
            );
        }

        @Override
        public String toString() {
            return "{" +
                " name='" + getName() + "'" +
                ", oldType='" + getOldType() + "'" +
                ", newType='" + getNewType() + "'" +
                ", oldNullable='" + isOldNullable() + "'" +
                ", newNullable='" + isNewNullable() + "'" +
                ", oldUnique='" + isOldUnique() + "'" +
                ", newUnique='" + isNewUnique() + "'" +
                ", oldDefault='" + getOldDefault() + "'" +
                ", newDefault='" + getNewDefault() + "'" +
                ", typeChanged='" + isTypeChanged() + "'" +
                ", nullableChanged='" + isNullableChanged() + "'" +
                ", uniqueChanged='" + isUniqueChanged() + "'" +
                ", defaultChanged='" + isDefaultChanged() + "'" +
                "}";
        }

    }

    public static class AddedColumn {

        private String name;
        private String type;
        private Boolean nullable;
        private Boolean unique;
        private String defaultValue;

        public AddedColumn() {}

        public AddedColumn(final String name, final String type, final Boolean nullable,
                final Boolean unique, final String defaultValue) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.unique = unique;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return this.name;
        }

        public AddedColumn setName(final String name) {
            this.name = name;
            return this;
        }

        public String getType() {
            return this.type;
        }

        public AddedColumn setType(final String type) {
            this.type = type;
            return this;
        }

        public Boolean isNullable() {
            return this.nullable;
        }

        public Boolean getNullable() {
            return this.nullable;
        }

        public AddedColumn setNullable(final Boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Boolean isUnique() {
            return this.unique;
        }

        public Boolean getUnique() {
            return this.unique;
        }

        public AddedColumn setUnique(final Boolean unique) {
            this.unique = unique;
            return this;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }

        public AddedColumn setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof AddedColumn)) {
                return false;
            }
            final AddedColumn addedColumn = (AddedColumn) o;
            return Objects.equals(name, addedColumn.name) &&
                    Objects.equals(type, addedColumn.type) &&
                    Objects.equals(nullable, addedColumn.nullable) &&
                    Objects.equals(unique, addedColumn.unique) &&
                    Objects.equals(defaultValue, addedColumn.defaultValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, nullable, unique, defaultValue);
        }

        @Override
        public String toString() {
            return "{" +
                " name='" + getName() + "'" +
                ", type='" + getType() + "'" +
                ", nullable='" + isNullable() + "'" +
                ", unique='" + isUnique() + "'" +
                ", defaultValue='" + getDefaultValue() + "'" +
                "}";
        }
    }

    public static class FkChange {

        private String column;
        private String refTable;
        private String refColumn;

        public FkChange() {}

        public FkChange(final String column, final String refTable, final String refColumn) {
            this.column = column;
            this.refTable = refTable;
            this.refColumn = refColumn;
        }

        public String getColumn() {
            return this.column;
        }

        public FkChange setColumn(final String column) {
            this.column = column;
            return this;
        }

        public String getRefTable() {
            return this.refTable;
        }

        public FkChange setRefTable(final String refTable) {
            this.refTable = refTable;
            return this;
        }

        public String getRefColumn() {
            return this.refColumn;
        }

        public FkChange setRefColumn(final String refColumn) {
            this.refColumn = refColumn;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof FkChange)) {
                return false;
            }
            final FkChange fkChange = (FkChange) o;
            return Objects.equals(column, fkChange.column) &&
                    Objects.equals(refTable, fkChange.refTable) &&
                    Objects.equals(refColumn, fkChange.refColumn);
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

    public static class Result {

        private List<AddedColumn> addedColumns = new ArrayList<>();
        private List<String> removedColumns = new ArrayList<>();
        private List<ColumnChange> modifiedColumns = new ArrayList<>();
        private boolean pkChanged = false;
        private List<String> newPk = new ArrayList<>();
        private List<FkChange> addedFks = new ArrayList<>();
        private List<FkChange> removedFks = new ArrayList<>();
        private boolean auditAdded;
        private boolean auditRemoved;
        private boolean auditTypeChanged;
        private String oldAuditType;
        private String newAuditType;

        public Result() {}

        public Result(final List<AddedColumn> addedColumns, final List<String> removedColumns,
                final List<ColumnChange> modifiedColumns, final boolean pkChanged, 
                final List<String> newPk, final List<FkChange> addedFks, final List<FkChange> removedFks,
                final boolean auditAdded, final boolean auditRemoved, final boolean auditTypeChanged,
                final String oldAuditType, final String newAuditType) {
            this.addedColumns = addedColumns;
            this.removedColumns = removedColumns;
            this.modifiedColumns = modifiedColumns;
            this.pkChanged = pkChanged;
            this.newPk = newPk;
            this.addedFks = addedFks;
            this.removedFks = removedFks;
            this.auditAdded = auditAdded;
            this.auditRemoved = auditRemoved;
            this.auditTypeChanged = auditTypeChanged;
            this.oldAuditType = oldAuditType;
            this.newAuditType = newAuditType;
        }

        public List<AddedColumn> getAddedColumns() {
            return this.addedColumns;
        }

        public Result setAddedColumns(final List<AddedColumn> addedColumns) {
            this.addedColumns = addedColumns;
            return this;
        }

        public List<String> getRemovedColumns() {
            return this.removedColumns;
        }

        public Result setRemovedColumns(final List<String> removedColumns) {
            this.removedColumns = removedColumns;
            return this;
        }

        public List<ColumnChange> getModifiedColumns() {
            return this.modifiedColumns;
        }

        public Result setModifiedColumns(final List<ColumnChange> modifiedColumns) {
            this.modifiedColumns = modifiedColumns;
            return this;
        }

        public boolean isPkChanged() {
            return this.pkChanged;
        }

        public boolean getPkChanged() {
            return this.pkChanged;
        }

        public Result setPkChanged(final boolean pkChanged) {
            this.pkChanged = pkChanged;
            return this;
        }

        public List<String> getNewPk() {
            return this.newPk;
        }

        public Result setNewPk(final List<String> newPk) {
            this.newPk = newPk;
            return this;
        }

        public List<FkChange> getAddedFks() {
            return this.addedFks;
        }

        public Result setAddedFks(final List<FkChange> addedFks) {
            this.addedFks = addedFks;
            return this;
        }

        public List<FkChange> getRemovedFks() {
            return this.removedFks;
        }

        public Result setRemovedFks(final List<FkChange> removedFks) {
            this.removedFks = removedFks;
            return this;
        }

        public boolean isAuditAdded() {
            return this.auditAdded;
        }

        public Result setAuditAdded(final boolean auditAdded) {
            this.auditAdded = auditAdded;
            return this;
        }

        public boolean isAuditRemoved() {
            return this.auditRemoved;
        }

        public Result setAuditRemoved(final boolean auditRemoved) {
            this.auditRemoved = auditRemoved;
            return this;
        }

        public boolean isAuditTypeChanged() {
            return this.auditTypeChanged;
        }

        public Result setAuditTypeChanged(final boolean auditTypeChanged) {
            this.auditTypeChanged = auditTypeChanged;
            return this;
        }

        public String getOldAuditType() {
            return this.oldAuditType;
        }

        public Result setOldAuditType(final String oldAuditType) {
            this.oldAuditType = oldAuditType;
            return this;
        }

        public String getNewAuditType() {
            return this.newAuditType;
        }

        public Result setNewAuditType(final String newAuditType) {
            this.newAuditType = newAuditType;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Result)) {
                return false;
            }
            final Result result = (Result) o;
            return Objects.equals(addedColumns, result.addedColumns) &&
                    Objects.equals(removedColumns, result.removedColumns) &&
                    Objects.equals(modifiedColumns, result.modifiedColumns) &&
                    pkChanged == result.pkChanged &&
                    Objects.equals(newPk, result.newPk) &&
                    Objects.equals(addedFks, result.addedFks) &&
                    Objects.equals(removedFks, result.removedFks) &&
                    auditAdded == result.auditAdded &&
                    auditRemoved == result.auditRemoved &&
                    auditTypeChanged == result.auditTypeChanged &&
                    Objects.equals(oldAuditType, result.oldAuditType) &&
                    Objects.equals(newAuditType, result.newAuditType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(addedColumns, removedColumns, modifiedColumns, pkChanged, newPk, addedFks, removedFks,
                    auditAdded, auditRemoved, auditTypeChanged, oldAuditType, newAuditType);
        }

        @Override
        public String toString() {
            return "{" +
                " addedColumns='" + getAddedColumns() + "'" +
                ", removedColumns='" + getRemovedColumns() + "'" +
                ", modifiedColumns='" + getModifiedColumns() + "'" +
                ", pkChanged='" + isPkChanged() + "'" +
                ", newPk='" + getNewPk() + "'" +
                ", addedFks='" + getAddedFks() + "'" +
                ", removedFks='" + getRemovedFks() + "'" +
                ", auditAdded='" + isAuditAdded() + "'" +
                ", auditRemoved='" + isAuditRemoved() + "'" +
                ", auditTypeChanged='" + isAuditTypeChanged() + "'" +
                ", oldAuditType='" + getOldAuditType() + "'" +
                ", newAuditType='" + getNewAuditType() + "'" +
                "}";
        }

        public boolean isEmpty() {
            return addedColumns.isEmpty() && removedColumns.isEmpty() && modifiedColumns.isEmpty() &&
                   !pkChanged && addedFks.isEmpty() && removedFks.isEmpty() && !auditAdded && !auditRemoved && !auditTypeChanged
                    && Objects.isNull(oldAuditType) && Objects.isNull(newAuditType);
        }
    }
    
}
