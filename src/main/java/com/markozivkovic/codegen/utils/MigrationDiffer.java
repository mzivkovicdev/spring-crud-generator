package com.markozivkovic.codegen.utils;

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

        for (final String n : newCols.keySet()) {
            if (!oldCols.containsKey(n)) {
                final Map<String,Object> v = newCols.get(n);

                final String nt = String.valueOf(v.get("sqlType"));
                final Boolean nn = Boolean.TRUE.equals(v.get("nullable"));
                final Boolean nu = Boolean.TRUE.equals(v.get("unique"));
                final String nd = (String) v.get("defaultExpr");

                r.getAddedColumns().add(new AddedColumn().setName(n)
                        .setType(nt)
                        .setNullable(nn)
                        .setUnique(nu)
                        .setDefaultValue(nd));
            }
        }

        for (String n : oldCols.keySet()) {
            if (!newCols.containsKey(n)) {
                r.getRemovedColumns().add(n);
            }
        }

        for (String n : newCols.keySet()) {
            if (oldCols.containsKey(n)) {
                final ColumnState o = oldCols.get(n);
                final Map<String,Object> nv = newCols.get(n);

                final String nt = String.valueOf(nv.get("sqlType"));
                final Boolean nn = Boolean.TRUE.equals(nv.get("nullable"));
                final Boolean nu = Boolean.TRUE.equals(nv.get("unique"));
                final String nd = (String) nv.get("defaultExpr");

                final ColumnChange cc = new ColumnChange();
                cc.setName(n)
                    .setOldType(o.getType())
                    .setNewType(nt)
                    .setOldNullable(o.getNullable())
                    .setNewNullable(nn)
                    .setOldUnique(o.getUnique())
                    .setNewUnique(nu)
                    .setOldDefault(o.getDefaultExpr())
                    .setNewDefault(nd);

                cc.setTypeChanged(!Objects.equals(o.getType(), nt))
                        .setNullableChanged(!Objects.equals(o.getNullable(), nn))
                        .setUniqueChanged(!Objects.equals(o.getUnique(), nu))
                        .setDefaultChanged(!Objects.equals(o.getDefaultExpr(), nd));

                if (cc.getTypeChanged() || cc.getNullableChanged() || cc.getUniqueChanged() || cc.getDefaultChanged()) {
                    r.getModifiedColumns().add(cc);
                }
            }
        }

        final List<String> newPk = splitCsv(String.valueOf(newCreateCtx.get("pkColumns")));
        final List<String> oldPk = oldState != null && oldState.getPk() != null ? oldState.getPk() : Collections.emptyList();

        if (!equalListIgnoreOrder(oldPk, newPk)) {
            r.setPkChanged(true);
            r.setNewPk(newPk);
        }

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
        Map<String, Map<String,Object>> newFkByKey = new LinkedHashMap<>();
        final Map<String,Object> fkCtx = (Map<String,Object>) newCreateCtx.get("fksCtx");
        List<Map<String,Object>> fks = fkCtx != null
                ? (List<Map<String,Object>>) fkCtx.get("fks")
                : (List<Map<String,Object>>) newCreateCtx.get("fks");

        if (fks != null) {
            for (Map<String,Object> m : fks) {
                String col = String.valueOf(m.get("column"));
                String rt  = String.valueOf(m.get("refTable"));
                String rc  = String.valueOf(m.get("refColumn"));
                String key = fkKey(col, rt, rc);
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

        return r;
    }

    public static String fkKey(String col, String rt, String rc) {
        return col + "->" + rt + "(" + rc + ")";
    }

    private static List<String> splitCsv(final String raw) {
        if (raw == null || "null".equals(raw))
            return Collections.emptyList();
        
        final List<String> out = new ArrayList<>();
        for (String p : raw.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static boolean equalListIgnoreOrder(final List<String> a, final List<String> b) {
        return new HashSet<>(a)
                .equals(new HashSet<>(b));
    }

}
