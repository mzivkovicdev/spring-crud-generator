package com.markozivkovic.codegen.migrations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.markozivkovic.codegen.models.flyway.AuditState;
import com.markozivkovic.codegen.models.flyway.ColumnState;
import com.markozivkovic.codegen.models.flyway.EntityState;
import com.markozivkovic.codegen.models.flyway.FileState;
import com.markozivkovic.codegen.models.flyway.FkState;
import com.markozivkovic.codegen.models.flyway.JoinState;
import com.markozivkovic.codegen.models.flyway.JoinState.JoinSide;
import com.markozivkovic.codegen.models.flyway.MigrationState;
import com.markozivkovic.codegen.utils.HashUtils;

public class MigrationManifestBuilder {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final MigrationState state;
    private final Map<String, EntityState> byTable = new LinkedHashMap<>();

    public MigrationManifestBuilder(final MigrationState state) {
        this.state = state;
        state.getEntities()
                .forEach(e -> byTable.put(e.getTable(), e));
    }

    /**
     * Applies the given create context to the migration state, creating a new entity state if necessary.
     * 
     * @param modelName the model name
     * @param tableName the table name
     * @param createCtx the create context
     */
    @SuppressWarnings("unchecked")
    public void applyCreateContext(final String modelName, final String tableName, final Map<String,Object> createCtx) {
        
        final EntityState entityState = byTable.computeIfAbsent(tableName, t -> {
            final EntityState ne = new EntityState();
            ne.setName(modelName);
            ne.setTable(tableName);
            ne.setColumns(new LinkedHashMap<>());
            ne.setPk(new ArrayList<>());
            ne.setFiles(new ArrayList<>());
            ne.setJoins(new ArrayList<>());
            ne.setFks(new ArrayList<>());
            state.getEntities().add(ne);
            return ne;
        });

        final Map<String,ColumnState> cols = new LinkedHashMap<>();
        final List<Map<String,Object>> cList = (List<Map<String,Object>>) createCtx.get("columns");
        if (cList != null) {
            for (final Map<String,Object> c : cList) {
                final String name = String.valueOf(c.get("name"));
                final ColumnState cs = new ColumnState();
                cs.setType(String.valueOf(c.get("sqlType")));
                cs.setNullable(Boolean.TRUE.equals(c.get("nullable")));
                cs.setUnique(Boolean.TRUE.equals(c.get("unique")));
                cs.setDefaultExpr((String) c.get("defaultExpr"));
                cols.put(name, cs);
            }
        }
        entityState.setColumns(cols);

        if (createCtx.containsKey("pkColumns") && createCtx.get("pkColumns") != null) {
            final String raw = String.valueOf(createCtx.get("pkColumns"));
            final List<String> pk = new ArrayList<>();
            for (final String part : raw.split(",")) {
                final String p = part.trim();
                if (!p.isEmpty()) {
                    pk.add(p);
                }
            }
            entityState.setPk(pk);
        }

        final AuditState audit = new AuditState();
        audit.setEnabled(Boolean.TRUE.equals(createCtx.get("auditEnabled")));
        final Object at = createCtx.get("auditCreatedType");
        audit.setType(at != null ? at.toString() : null);
        entityState.setAudit(audit);
        entityState.setFingerprint(fingerprintFromCreateCtx(createCtx));
    }

    /**
     * Adds the given foreign key context to the migration state.
     * 
     * @param tableName the table name
     * @param fkCtx the foreign key context
     */
    @SuppressWarnings("unchecked")
    public void addForeignKeys(final String tableName, final Map<String,Object> fkCtx) {
        
        if (fkCtx == null || fkCtx.isEmpty()) {
            return;
        }
        
        final EntityState entityState = byTable.computeIfAbsent(tableName, t -> {
            final EntityState newEntityState = new EntityState();
            newEntityState.setTable(tableName);
            newEntityState.setColumns(new LinkedHashMap<>());
            newEntityState.setPk(new ArrayList<>());
            newEntityState.setFiles(new ArrayList<>());
            newEntityState.setJoins(new ArrayList<>());
            newEntityState.setFks(new ArrayList<>());
            state.getEntities().add(newEntityState);
            return newEntityState;
        });

        if (entityState.getFks() == null) {
            entityState.setFks(new ArrayList<>());
        }
        final Set<String> dedup = new HashSet<>();
        for (final FkState existing : entityState.getFks()) {
            dedup.add(fkKey(existing.getColumn(), existing.getRefTable(), existing.getRefColumn()));
        }

        final List<Map<String,Object>> fks = (List<Map<String,Object>>) fkCtx.get("fks");
        if (fks != null) {
            for (final Map<String,Object> m : fks) {
                final String col = String.valueOf(m.get("column"));
                final String rt  = String.valueOf(m.get("refTable"));
                final String rc  = String.valueOf(m.get("refColumn"));
                final String key = fkKey(col, rt, rc);
                
                if (!dedup.add(key)) {
                    continue;
                }

                entityState.getFks()
                    .add(new FkState(col, rt, rc));
            }
        }
    }

    /**
     * Adds a join state to the migration state.
     * 
     * @param ownerTable the owner table of the join state
     * @param joinCtx the join context
     */
    @SuppressWarnings("unchecked")
    public void addJoin(final String ownerTable, final Map<String,Object> joinCtx) {
        
        final EntityState e = byTable.computeIfAbsent(ownerTable, t -> {
            final EntityState ne = new EntityState();
            ne.setTable(ownerTable);
            ne.setColumns(new LinkedHashMap<>());
            ne.setPk(new ArrayList<>());
            ne.setFiles(new ArrayList<>());
            ne.setJoins(new ArrayList<>());
            ne.setFks(new ArrayList<>());
            state.getEntities().add(ne);
            return ne;
        });

        if (e.getJoins() == null) {
            e.setJoins(new ArrayList<>());
        }

        final String joinTable = String.valueOf(joinCtx.get("joinTable"));

        final Map<String,Object> left  = (Map<String,Object>) joinCtx.get("left");
        final Map<String,Object> right = (Map<String,Object>) joinCtx.get("right");

        final JoinSide l = new JoinSide(String.valueOf(left.get("table")), String.valueOf(left.get("column")));
        final JoinSide r = new JoinSide(String.valueOf(right.get("table")), String.valueOf(right.get("column")));

        final JoinState js = e.getJoins().stream()
                .filter(j -> joinTable.equals(j.getTable()))
                .findFirst()
                .orElseGet(() -> {
                    JoinState nj = new JoinState();
                    nj.setTable(joinTable);
                    nj.setFiles(new ArrayList<>());
                    e.getJoins().add(nj);
                    return nj;
                });

        js.setLeft(l);
        js.setRight(r);
    }

    /**
     * Checks if a file state list contains a file with a given name.
     *
     * @param files the list of file states to search in
     * @param fileName the name of the file to search for
     * @return true if the list contains a file with the given name, false otherwise
     */
    private static boolean containsFile(final List<FileState> files, final String fileName) {
        if (files == null) {
            return false;
        }
        for (final FileState f : files) {
            if (fileName.equals(f.getFile())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a file to an entity state in the migration state.
     *
     * This method will create a new entity state if it doesn't exist and then add the given file to it.
     * If the entity state already contains the given file, this method will do nothing.
     *
     * @param tableName the name of the table to add the file to
     * @param fileName the name of the file to add
     * @param sqlContent the SQL content of the file to add
     */
    public void addEntityFile(final String tableName, final String fileName, final String sqlContent) {
        
        final EntityState entityState = byTable.computeIfAbsent(tableName, t -> {
            final EntityState newEntityState = new EntityState();
            newEntityState.setTable(tableName);
            newEntityState.setColumns(new LinkedHashMap<>());
            newEntityState.setPk(new ArrayList<>());
            newEntityState.setFiles(new ArrayList<>());
            newEntityState.setJoins(new ArrayList<>());
            newEntityState.setFks(new ArrayList<>());
            state.getEntities().add(newEntityState);
            return newEntityState;
        });

        if (containsFile(entityState.getFiles(), fileName)) {
            return;
        }
        entityState.getFiles()
            .add(newFileState(fileName, sqlContent));
    }

    /**
     * Adds a file to a join state in the migration state.
     *
     * @param ownerTable the owner table of the join state
     * @param joinTable the join table of the join state
     * @param fileName the name of the file to add
     * @param sqlContent the SQL content of the file to add
     */
    public void addJoinFile(final String ownerTable, final String joinTable, final String fileName, final String sqlContent) {
        
        final EntityState entityState = byTable.get(ownerTable);
        
        if (entityState == null) {
            return;
        }
        if (entityState.getJoins() == null) {
            return;
        }

        for (final JoinState j : entityState.getJoins()) {
            if (joinTable.equals(j.getTable())) {
                if (j.getFiles() == null) {
                    j.setFiles(new ArrayList<>());
                }
                if (containsFile(j.getFiles(), fileName)) {
                    return;
                }
                j.getFiles().add(newFileState(fileName, sqlContent));
                return;
            }
        }
    }

    /**
     * Builds the migration state.
     *
     * This method will return the migration state which was built using the addEntity methods.
     *
     * @return the migration state
     */
    public MigrationState build() {
        return state;
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
    private static String fkKey(final String col, final String rt, final String rc) {
        return col + "->" + rt + "(" + rc + ")";
    }

    /**
     * Creates a new file state with the given file name and content.
     * The file state will have its hash set to the SHA-256 hash of the content.
     *
     * @param fileName the name of the file
     * @param content the content of the file
     * @return the new file state
     */
    private static FileState newFileState(final String fileName, final String content) {
        
        return new FileState(fileName, HashUtils.sha256(content));
    }

    /**
     * Generates a fingerprint from the given create context.
     *
     * @param ctx the create context to generate the fingerprint from
     * @return the generated fingerprint
     */
    public static String fingerprintFromCreateCtx(Map<String,Object> ctx) {
        try {
            final Map<String,Object> canonical = new LinkedHashMap<>();
            canonical.put("table", ctx.get("tableName"));
            canonical.put("auditEnabled", ctx.get("auditEnabled"));
            canonical.put("auditType", ctx.get("auditCreatedType"));
            canonical.put("pkColumns", ctx.get("pkColumns"));

            @SuppressWarnings("unchecked")
            final List<Map<String,Object>> cols = (List<Map<String,Object>>) ctx.get("columns");
            final Map<String, Object> colsMap = new TreeMap<>();
            
            if (cols != null) {
                for (final Map<String,Object> c : cols) {
                    final Map<String,Object> v = new LinkedHashMap<>();
                    v.put("type", c.get("sqlType"));
                    v.put("nullable", c.get("nullable"));
                    v.put("unique", c.get("unique"));
                    v.put("default", c.get("defaultExpr"));
                    colsMap.put(String.valueOf(c.get("name")), v);
                }
            }
            canonical.put("columns", colsMap);

            final byte[] bytes = OBJECT_MAPPER.writer().writeValueAsBytes(canonical);
            return HashUtils.sha256(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("fingerprint build failed", e);
        }
    }

}
