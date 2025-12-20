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

package com.markozivkovic.codegen.migrations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.markozivkovic.codegen.models.flyway.ColumnState;
import com.markozivkovic.codegen.models.flyway.EntityState;
import com.markozivkovic.codegen.models.flyway.FkState;
import com.markozivkovic.codegen.models.flyway.SchemaDiff;
import com.markozivkovic.codegen.models.flyway.SchemaDiff.AddedColumn;
import com.markozivkovic.codegen.models.flyway.SchemaDiff.ColumnChange;
import com.markozivkovic.codegen.models.flyway.SchemaDiff.Result;

public class MigrationDiffer {

    private MigrationDiffer() {}
    
    /**
     * Computes the difference between two states of a database entity
     * @param oldState the old state of the entity
     * @param newCreateCtx the new state of the entity
     * @return a Result object containing the differences between the two states
     */
    @SuppressWarnings("unchecked")
    public static Result diff(final EntityState oldState, final Map<String,Object> newCreateCtx) {

        final Result r = new Result();
        final Map<String, Map<String, Object>> newCols = new LinkedHashMap<>();
        final List<Map<String,Object>> cList = (List<Map<String,Object>>) newCreateCtx.get("columns");

        if (Objects.nonNull(cList)) {
            for (final Map<String,Object> c : cList) {
                newCols.put(String.valueOf(c.get("name")), c);
            }
        }

        final Map<String, ColumnState> oldCols = oldState != null && oldState.getColumns() != null ?
                oldState.getColumns() : Collections.emptyMap();

        diffColumns(r, oldCols, newCols);

        diffPrimaryKey(r, oldState, newCreateCtx);

        diffForeignKeys(r, oldState, newCreateCtx);

        diffAudit(r, oldState, newCreateCtx);

        return r;
    }

    /**
     * Compares two maps of column states and finds the differences between them.
     * Adds the differences to the result object.
     *
     * @param r the result object to add the differences to
     * @param oldCols the map of column states from the old database
     * @param newCols the map of column states from the new database
     */
    private static void diffColumns(final Result r, final Map<String, ColumnState> oldCols, final Map<String, Map<String, Object>> newCols) {

        for (final String n : newCols.keySet()) {
            if (!oldCols.containsKey(n)) {
                final Map<String,Object> v = newCols.get(n);
                final String nt = String.valueOf(v.get("sqlType"));
                final Boolean nn = Boolean.TRUE.equals(v.get("nullable"));
                final Boolean nu = Boolean.TRUE.equals(v.get("unique"));
                final String nd = (String) v.get("defaultExpr");
                r.getAddedColumns().add(new AddedColumn(n, nt, nn, nu, nd));
            }
        }

        for (String n : oldCols.keySet()) {
            if (!newCols.containsKey(n)) {
                r.getRemovedColumns().add(n);
            }
        }

        for (final String n : newCols.keySet()) {
            if (oldCols.containsKey(n)) {
                final ColumnState o = oldCols.get(n);
                final Map<String,Object> nv = newCols.get(n);
                final String newType = String.valueOf(nv.get("sqlType"));
                final Boolean newNullable = Boolean.TRUE.equals(nv.get("nullable"));
                final Boolean newUnique = Boolean.TRUE.equals(nv.get("unique"));
                final String newDefaultExpression = (String) nv.get("defaultExpr");

                final ColumnChange columnChange = new ColumnChange(
                        n, o.getType(), newType, o.getNullable(), newNullable, o.getUnique(), newUnique,
                        o.getDefaultExpr(), newDefaultExpression, !Objects.equals(o.getType(), newType),
                        !Objects.equals(o.getNullable(), newNullable), !Objects.equals(o.getUnique(), newUnique),
                        !Objects.equals(o.getDefaultExpr(), newDefaultExpression)
                );

                if (columnChange.getTypeChanged() || columnChange.getNullableChanged() || columnChange.getUniqueChanged() || columnChange.getDefaultChanged()) {
                    r.getModifiedColumns().add(columnChange);
                }
            }
        }
    }

    /**
     * Compares the primary key of the old state of the entity with the primary key from the new state.
     * If the primary keys are different, adds the new primary key to the result object.
     * 
     * @param r the result object to add the differences to
     * @param oldState the old state of the entity
     * @param newCreateCtx the new state of the entity
     */
    private static void diffPrimaryKey(final Result r, final EntityState oldState, final Map<String,Object> newCreateCtx) {

        final List<String> newPk = splitCsv(String.valueOf(newCreateCtx.get("pkColumns")));
        final List<String> oldPk = oldState != null && oldState.getPk() != null
                ? oldState.getPk() 
                : Collections.emptyList();

        if (!equalListIgnoreOrder(oldPk, newPk)) {
            r.setPkChanged(true)
                    .setNewPk(newPk);
        }
    }

    /**
     * Computes the difference between two states of a database entity's foreign keys.
     * 
     * @param r the result object to add the differences to
     * @param oldState the old state of the entity
     * @param newCreateCtx the new state of the entity
     */
    @SuppressWarnings("unchecked")
    private static void diffForeignKeys(final Result r, final EntityState oldState, final Map<String,Object> newCreateCtx) {
        
        final Set<String> oldFkKeys = new LinkedHashSet<>();
        final Map<String, FkState> oldFkByKey = new LinkedHashMap<>();
        if (oldState != null && oldState.getFks() != null) {
            for (FkState f : oldState.getFks()) {
                final String key = fkKey(f.getColumn(), f.getRefTable(), f.getRefColumn());
                oldFkKeys.add(key);
                oldFkByKey.put(key, f);
            }
        }
        final Set<String> newFkKeys = new LinkedHashSet<>();
        final Map<String, Map<String,Object>> newFkByKey = new LinkedHashMap<>();
        final Map<String,Object> fkCtx = (Map<String,Object>) newCreateCtx.get("fksCtx");
        final List<Map<String,Object>> fks = fkCtx != null
                ? (List<Map<String,Object>>) fkCtx.get("fks")
                : (List<Map<String,Object>>) newCreateCtx.get("fks");

        if (fks != null) {
            for (Map<String,Object> m : fks) {
                final String col = String.valueOf(m.get("column"));
                final String rt  = String.valueOf(m.get("refTable"));
                final String rc  = String.valueOf(m.get("refColumn"));
                final String key = fkKey(col, rt, rc);
                newFkKeys.add(key);
                newFkByKey.put(key, m);
            }
        }

        for (final String k : newFkKeys) {
            if (!oldFkKeys.contains(k)) {
                final Map<String,Object> m = newFkByKey.get(k);
                final SchemaDiff.FkChange fc = new SchemaDiff.FkChange();
                fc.setColumn(String.valueOf(m.get("column")));
                fc.setRefTable(String.valueOf(m.get("refTable")));
                fc.setRefColumn(String.valueOf(m.get("refColumn")));
                r.getAddedFks().add(fc);
            }
        }

        for (final String k : oldFkKeys) {
            if (!newFkKeys.contains(k)) {
                final FkState f = oldFkByKey.get(k);
                final SchemaDiff.FkChange fc = new SchemaDiff.FkChange();
                fc.setColumn(f.getColumn());
                fc.setRefTable(f.getRefTable());
                fc.setRefColumn(f.getRefColumn());
                r.getRemovedFks().add(fc);
            }
        }
    }

    /**
     * Compares the audit state of the old state of the entity with the audit state from the new state.
     * If the audit states are different, adds the differences to the result object.
     *
     * @param r the result object to add the differences to
     * @param oldState the old state of the entity
     * @param newCreateCtx the new state of the entity
     */
    private static void diffAudit(final Result r, final EntityState oldState, final Map<String,Object> newCreateCtx) {

        boolean oldEnabled = false;
        String oldType = null;
        if (oldState != null && oldState.getAudit() != null) {
            oldEnabled = Boolean.TRUE.equals(oldState.getAudit().isEnabled());
            oldType = oldState.getAudit().getType();
        }

        final boolean newEnabled = Boolean.TRUE.equals(newCreateCtx.get("auditEnabled"));
        final String newType = (String) newCreateCtx.get("auditCreatedType");

        if (!oldEnabled && newEnabled) {
            r.setAuditAdded(true)
                .setNewAuditType(newType);
        } else if (oldEnabled && !newEnabled) {
            r.setAuditRemoved(true)
                .setOldAuditType(oldType);
        } else if (oldEnabled && newEnabled && !Objects.equals(oldType, newType)) {
            r.setAuditTypeChanged(true)
                .setOldAuditType(oldType)
                .setNewAuditType(newType);
        }
    }

    /**
     * Creates a string that uniquely identifies a foreign key.
     * 
     * The string is in the format of:
     * <column_name>-><referenced_table_name>(<referenced_column_name>)
     * 
     * @param col the column name of the foreign key
     * @param rt the referenced table name of the foreign key
     * @param rc the referenced column name of the foreign key
     * @return the string that uniquely identifies the foreign key
     */
    public static String fkKey(String col, String rt, String rc) {
        return col + "->" + rt + "(" + rc + ")";
    }

    /**
     * Splits a given string into a list of strings using comma as delimiter.
     * Trims each element of the resulting list.
     * If the given string is null or "null", returns an empty list.
     * 
     * @param raw the string to be split
     * @return a list of strings
     */
    private static List<String> splitCsv(final String raw) {
        if (raw == null || "null".equals(raw))
            return Collections.emptyList();
        
        final List<String> out = new ArrayList<>();
        for (String p : raw.split(",")) {
            final String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /**
     * Compares two lists ignoring order.
     * 
     * @param a the first list
     * @param b the second list
     * @return true if the lists are equal ignoring order, false otherwise
     */
    private static boolean equalListIgnoreOrder(final List<String> a, final List<String> b) {
        return new HashSet<>(a)
                .equals(new HashSet<>(b));
    }

}
