package com.markozivkovic.codegen.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.models.flyway.ColumnState;
import com.markozivkovic.codegen.models.flyway.EntityState;
import com.markozivkovic.codegen.models.flyway.FileState;
import com.markozivkovic.codegen.models.flyway.FkState;
import com.markozivkovic.codegen.models.flyway.JoinState;
import com.markozivkovic.codegen.models.flyway.JoinState.JoinSide;
import com.markozivkovic.codegen.models.flyway.MigrationState;
import com.markozivkovic.codegen.utils.HashUtils;

class MigrationManifestBuilderTest {

    private MigrationState newStateWithEntitiesList(List<EntityState> entities) {
        MigrationState state = mock(MigrationState.class);
        when(state.getEntities()).thenReturn(entities);
        return state;
    }

    @Test
    void applyCreateContext_shouldCreateNewEntityWhenTableNotPresent() {
        
        final List<EntityState> entities = new ArrayList<>();
        final MigrationState state = newStateWithEntitiesList(entities);

        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> cId = new HashMap<>();
        cId.put("name", "id");
        cId.put("sqlType", "BIGINT");
        cId.put("nullable", false);
        cId.put("unique", true);
        cId.put("defaultExpr", "1");

        final Map<String, Object> cName = new HashMap<>();
        cName.put("name", "name");
        cName.put("sqlType", "VARCHAR");
        cName.put("nullable", true);
        cName.put("unique", false);
        cName.put("defaultExpr", null);

        final List<Map<String, Object>> columns = List.of(cId, cName);

        final Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("columns", columns);
        createCtx.put("pkColumns", "id, name");
        createCtx.put("auditEnabled", true);
        createCtx.put("auditCreatedType", "LOCAL_DATE_TIME");

        builder.applyCreateContext("User", "users", createCtx);

        assertEquals(1, entities.size());
        final EntityState e = entities.get(0);

        assertEquals("User", e.getName());
        assertEquals("users", e.getTable());

        final Map<String, ColumnState> cols = e.getColumns();
        assertNotNull(cols);
        assertEquals(2, cols.size());

        final ColumnState idCol = cols.get("id");
        assertNotNull(idCol);
        assertEquals("BIGINT", idCol.getType());
        assertFalse(idCol.getNullable());
        assertTrue(idCol.getUnique());
        assertEquals("1", idCol.getDefaultExpr());

        final ColumnState nameCol = cols.get("name");
        assertNotNull(nameCol);
        assertEquals("VARCHAR", nameCol.getType());
        assertTrue(nameCol.getNullable());
        assertFalse(nameCol.getUnique());
        assertNull(nameCol.getDefaultExpr());

        assertEquals(List.of("id", "name"), e.getPk());
        assertNotNull(e.getAudit());
        assertTrue(e.getAudit().getEnabled());
        assertEquals("LOCAL_DATE_TIME", e.getAudit().getType());
        assertNotNull(e.getFiles());
        assertNotNull(e.getJoins());
        assertNotNull(e.getFks());
        assertNotNull(e.getFingerprint());
    }

    @Test
    void applyCreateContext_shouldReuseExistingEntityAndNotDuplicateInState() {
        
        final List<EntityState> entities = new ArrayList<>();

        final EntityState existing = new EntityState();
        existing.setName("ExistingUser");
        existing.setTable("users");
        existing.setColumns(new LinkedHashMap<>());
        existing.setPk(new ArrayList<>());
        existing.setFiles(new ArrayList<>());
        existing.setJoins(new ArrayList<>());
        existing.setFks(new ArrayList<>());

        entities.add(existing);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> cId = new HashMap<>();
        cId.put("name", "id");
        cId.put("sqlType", "BIGINT");
        cId.put("nullable", false);
        cId.put("unique", false);

        final Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("columns", List.of(cId));
        createCtx.put("pkColumns", "id");
        createCtx.put("auditEnabled", false);

        builder.applyCreateContext("IgnoredModelName", "users", createCtx);

        assertEquals(1, entities.size());
        final EntityState e = entities.get(0);
        assertSame(existing, e);

        assertEquals("ExistingUser", e.getName());
        assertEquals(1, e.getColumns().size());
        assertTrue(e.getColumns().containsKey("id"));
        assertEquals(List.of("id"), e.getPk());
    }

    @Test
    void applyCreateContext_shouldHandleNullColumnsAndNullPk() {
        
        final List<EntityState> entities = new ArrayList<>();
        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("columns", null);
        createCtx.put("pkColumns", null);
        createCtx.put("auditEnabled", null);
        createCtx.put("auditCreatedType", null);

        builder.applyCreateContext("User", "users", createCtx);

        assertEquals(1, entities.size());
        final EntityState e = entities.get(0);

        assertNotNull(e.getColumns());
        assertTrue(e.getColumns().isEmpty());
        assertNotNull(e.getPk());
        assertTrue(e.getPk().isEmpty());
        assertNotNull(e.getAudit());
        assertFalse(Boolean.TRUE.equals(e.getAudit().getEnabled()));
        assertNull(e.getAudit().getType());
    }

    @Test
    void addForeignKeys_shouldDoNothingWhenFkCtxNullOrEmpty() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setJoins(new ArrayList<>());
        e.setFks(new ArrayList<>());
        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addForeignKeys("users", null);
        builder.addForeignKeys("users", Collections.emptyMap());

        assertEquals(1, entities.size());
        assertSame(e, entities.get(0));
        assertTrue(e.getFks().isEmpty());
    }

    @Test
    void addForeignKeys_shouldCreateEntityIfMissingAndAddForeignKeys() {

        final List<EntityState> entities = new ArrayList<>();
        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> fk1 = new HashMap<>();
        fk1.put("column", "role_id");
        fk1.put("refTable", "roles");
        fk1.put("refColumn", "id");

        final Map<String, Object> fk2 = new HashMap<>();
        fk2.put("column", "tenant_id");
        fk2.put("refTable", "tenants");
        fk2.put("refColumn", "id");

        final Map<String, Object> fkCtx = new HashMap<>();
        fkCtx.put("fks", List.of(fk1, fk2));

        builder.addForeignKeys("users", fkCtx);

        assertEquals(1, entities.size());
        final EntityState e = entities.get(0);

        assertEquals("users", e.getTable());
        assertNotNull(e.getFks());
        assertEquals(2, e.getFks().size());

        final FkState f1 = e.getFks().get(0);
        final FkState f2 = e.getFks().get(1);

        final Set<String> fkStrings = new HashSet<>();
        fkStrings.add(f1.getColumn() + "->" + f1.getRefTable() + "(" + f1.getRefColumn() + ")");
        fkStrings.add(f2.getColumn() + "->" + f2.getRefTable() + "(" + f2.getRefColumn() + ")");

        assertTrue(fkStrings.contains("role_id->roles(id)"));
        assertTrue(fkStrings.contains("tenant_id->tenants(id)"));
    }

    @Test
    void addForeignKeys_shouldDeduplicateExistingAndNewEntries() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setJoins(new ArrayList<>());
        e.setFks(new ArrayList<>());
        e.getFks().add(new FkState("role_id", "roles", "id"));

        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> fkExistingDup = new HashMap<>();
        fkExistingDup.put("column", "role_id");
        fkExistingDup.put("refTable", "roles");
        fkExistingDup.put("refColumn", "id");

        final Map<String, Object> fkNew = new HashMap<>();
        fkNew.put("column", "tenant_id");
        fkNew.put("refTable", "tenants");
        fkNew.put("refColumn", "id");

        final Map<String, Object> fkNewDup = new HashMap<>(fkNew);

        final Map<String, Object> fkCtx = new HashMap<>();
        fkCtx.put("fks", List.of(fkExistingDup, fkNew, fkNewDup));

        builder.addForeignKeys("users", fkCtx);

        assertEquals(2, e.getFks().size());

        final Set<String> fkStrings = new HashSet<>();
        for (final FkState fk : e.getFks()) {
            fkStrings.add(fk.getColumn() + "->" + fk.getRefTable() + "(" + fk.getRefColumn() + ")");
        }

        assertTrue(fkStrings.contains("role_id->roles(id)"));
        assertTrue(fkStrings.contains("tenant_id->tenants(id)"));
    }

    @Test
    void addJoin_shouldCreateEntityIfMissingAndAddJoin() {

        final List<EntityState> entities = new ArrayList<>();
        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> left = new HashMap<>();
        left.put("table", "users");
        left.put("column", "role_id");

        final Map<String, Object> right = new HashMap<>();
        right.put("table", "roles");
        right.put("column", "id");

        final Map<String, Object> joinCtx = new HashMap<>();
        joinCtx.put("joinTable", "user_roles");
        joinCtx.put("left", left);
        joinCtx.put("right", right);

        builder.addJoin("users", joinCtx);

        assertEquals(1, entities.size());
        final EntityState e = entities.get(0);

        assertEquals("users", e.getTable());
        assertNotNull(e.getJoins());
        assertEquals(1, e.getJoins().size());

        final JoinState js = e.getJoins().get(0);
        assertEquals("user_roles", js.getTable());
        assertNotNull(js.getLeft());
        assertNotNull(js.getRight());

        assertEquals("users", js.getLeft().getTable());
        assertEquals("role_id", js.getLeft().getColumn());
        assertEquals("roles", js.getRight().getTable());
        assertEquals("id", js.getRight().getColumn());

        assertNotNull(js.getFiles());
    }

    @Test
    void addJoin_shouldReuseExistingJoinStateForSameJoinTable() {

        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setJoins(new ArrayList<>());
        e.setFks(new ArrayList<>());

        final JoinState existingJoin = new JoinState();
        existingJoin.setTable("user_roles");
        existingJoin.setFiles(new ArrayList<>());

        existingJoin.setLeft(new JoinSide("OLD_LEFT", "OLD_COL"));
        existingJoin.setRight(new JoinSide("OLD_RIGHT", "OLD_COL"));

        e.getJoins().add(existingJoin);
        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> left = new HashMap<>();
        left.put("table", "users");
        left.put("column", "tenant_id");

        final Map<String, Object> right = new HashMap<>();
        right.put("table", "tenants");
        right.put("column", "id");

        final Map<String, Object> joinCtx = new HashMap<>();
        joinCtx.put("joinTable", "user_roles");
        joinCtx.put("left", left);
        joinCtx.put("right", right);

        builder.addJoin("users", joinCtx);

        assertEquals(1, entities.size());
        final EntityState entity = entities.get(0);
        
        assertEquals(1, entity.getJoins().size());
        final JoinState js = entity.getJoins().get(0);
        assertSame(existingJoin, js);
        assertEquals("users", js.getLeft().getTable());
        assertEquals("tenant_id", js.getLeft().getColumn());
        assertEquals("tenants", js.getRight().getTable());
        assertEquals("id", js.getRight().getColumn());
    }

    @Test
    void addJoin_shouldInitializeJoinsListWhenNull() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setJoins(null);
        e.setFks(new ArrayList<>());
        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final Map<String, Object> left = new HashMap<>();
        left.put("table", "users");
        left.put("column", "role_id");

        final Map<String, Object> right = new HashMap<>();
        right.put("table", "roles");
        right.put("column", "id");

        final Map<String, Object> joinCtx = new HashMap<>();
        joinCtx.put("joinTable", "user_roles");
        joinCtx.put("left", left);
        joinCtx.put("right", right);

        builder.addJoin("users", joinCtx);

        assertNotNull(e.getJoins());
        assertEquals(1, e.getJoins().size());
    }

    @Test
    void containsFile_shouldReturnFalseWhenFilesNull() throws Exception {
        final Method m = MigrationManifestBuilder.class
                .getDeclaredMethod("containsFile", List.class, String.class);
        m.setAccessible(true);

        final boolean result = (boolean) m.invoke(null, null, "test.sql");
        assertFalse(result);
    }

    @Test
    void containsFile_shouldReturnTrueWhenFileWithNameExists() throws Exception {
        final Method m = MigrationManifestBuilder.class
                .getDeclaredMethod("containsFile", List.class, String.class);
        m.setAccessible(true);

        final FileState f1 = mock(FileState.class);
        when(f1.getFile()).thenReturn("a.sql");
        final FileState f2 = mock(FileState.class);
        when(f2.getFile()).thenReturn("b.sql");

        final List<FileState> files = List.of(f1, f2);

        final boolean result1 = (boolean) m.invoke(null, files, "a.sql");
        final boolean result2 = (boolean) m.invoke(null, files, "b.sql");
        final boolean result3 = (boolean) m.invoke(null, files, "c.sql");

        assertTrue(result1);
        assertTrue(result2);
        assertFalse(result3);
    }

    @Test
    void addEntityFile_shouldCreateEntityIfMissingAndAddFile() {

        final List<EntityState> entities = new ArrayList<>();
        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addEntityFile("users", "V1__init.sql", "create table users (...)");

        assertEquals(1, entities.size());
        final EntityState e = entities.get(0);
        assertEquals("users", e.getTable());
        assertNotNull(e.getFiles());
        assertEquals(1, e.getFiles().size());

        final FileState fs = e.getFiles().get(0);
        assertEquals("V1__init.sql", fs.getFile());
    }

    @Test
    void addEntityFile_shouldNotAddDuplicateFile() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setJoins(new ArrayList<>());
        e.setFks(new ArrayList<>());

        final FileState existingFile = new FileState();
        existingFile.setFile("V1__init.sql");
        e.getFiles().add(existingFile);

        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addEntityFile("users", "V1__init.sql", "create table users (...);");
        builder.addEntityFile("users", "V1__init.sql", "something else");

        assertEquals(1, e.getFiles().size());
        assertEquals("V1__init.sql", e.getFiles().get(0).getFile());
    }

    @Test
    void addEntityFile_shouldWorkWhenFilesListInitiallyNull() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setJoins(new ArrayList<>());
        e.setFks(new ArrayList<>());

        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addEntityFile("users", "V1__init.sql", "create table users (...);");

        assertNotNull(e.getFiles());
        assertEquals(1, e.getFiles().size());
        assertEquals("V1__init.sql", e.getFiles().get(0).getFile());
    }

    @Test
    void addJoinFile_shouldDoNothingWhenOwnerEntityDoesNotExist() {
        
        final List<EntityState> entities = new ArrayList<>();
        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addJoinFile("users", "user_roles", "V1__join.sql", "sql...");

        assertTrue(entities.isEmpty());
    }

    @Test
    void addJoinFile_shouldDoNothingWhenJoinsListIsNull() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");
        e.setJoins(null);
        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addJoinFile("users", "user_roles", "V1__join.sql", "sql...");

        assertNull(e.getJoins());
    }

    @Test
    void addJoinFile_shouldAddFileToMatchingJoinAndInitializeFilesList() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");

        final JoinState join = new JoinState();
        join.setTable("user_roles");
        join.setFiles(null);

        e.setJoins(new ArrayList<>(List.of(join)));
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setFks(new ArrayList<>());

        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addJoinFile("users", "user_roles", "V1__join.sql", "join sql;");

        assertNotNull(join.getFiles());
        assertEquals(1, join.getFiles().size());
        final FileState fs = join.getFiles().get(0);
        assertEquals("V1__join.sql", fs.getFile());
    }

    @Test
    void addJoinFile_shouldNotAddDuplicateFile() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");

        final JoinState join = new JoinState();
        join.setTable("user_roles");
        join.setFiles(new ArrayList<>());

        final FileState existing = new FileState("V1__join.sql", "hash1");
        join.getFiles().add(existing);

        e.setJoins(new ArrayList<>(List.of(join)));
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setFks(new ArrayList<>());

        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addJoinFile("users", "user_roles", "V1__join.sql", "new content");

        assertEquals(1, join.getFiles().size());
        assertSame(existing, join.getFiles().get(0));
    }

    @Test
    void addJoinFile_shouldDoNothingWhenJoinTableNotFound() {
        
        final List<EntityState> entities = new ArrayList<>();
        final EntityState e = new EntityState();
        e.setTable("users");

        final JoinState join = new JoinState();
        join.setTable("other_join");
        join.setFiles(new ArrayList<>());

        e.setJoins(new ArrayList<>(List.of(join)));
        e.setColumns(new LinkedHashMap<>());
        e.setPk(new ArrayList<>());
        e.setFiles(new ArrayList<>());
        e.setFks(new ArrayList<>());

        entities.add(e);

        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        builder.addJoinFile("users", "user_roles", "V1__join.sql", "sql...");

        assertTrue(join.getFiles().isEmpty());
    }

    @Test
    void build_shouldReturnSameMigrationStateInstance() {
        
        final List<EntityState> entities = new ArrayList<>();
        final MigrationState state = newStateWithEntitiesList(entities);
        final MigrationManifestBuilder builder = new MigrationManifestBuilder(state);

        final MigrationState built = builder.build();

        assertSame(state, built);
    }

    @Test
    void fkKey_shouldBuildExpectedString() throws Exception {
        
        final Method m = MigrationManifestBuilder.class
                .getDeclaredMethod("fkKey", String.class, String.class, String.class);
        m.setAccessible(true);

        final String key = (String) m.invoke(null, "user_id", "users", "id");

        assertEquals("user_id->users(id)", key);
    }

    @Test
    void newFileState_shouldCreateFileStateWithHashFromHashUtils() throws Exception {
        try (final MockedStatic<HashUtils> mocked = mockStatic(HashUtils.class)) {
            mocked.when(() -> HashUtils.sha256("content"))
                    .thenReturn("HASH123");

            final Method m = MigrationManifestBuilder.class
                    .getDeclaredMethod("newFileState", String.class, String.class);
            m.setAccessible(true);

            final FileState fs = (FileState) m.invoke(null, "V1__init.sql", "content");

            assertEquals("V1__init.sql", fs.getFile());
            assertEquals("HASH123", fs.getHash());

            mocked.verify(() -> HashUtils.sha256("content"));
        }
    }

    @Test
    void fingerprintFromCreateCtx_shouldProduceNonNullFingerprint() {
        
        final Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("tableName", "users");
        ctx.put("auditEnabled", true);
        ctx.put("auditCreatedType", "LOCAL_DATE_TIME");
        ctx.put("pkColumns", "id, tenant_id");

        final Map<String, Object> c1 = new HashMap<>();
        c1.put("name", "id");
        c1.put("sqlType", "BIGINT");
        c1.put("nullable", false);
        c1.put("unique", true);
        c1.put("defaultExpr", "1");

        final Map<String, Object> c2 = new HashMap<>();
        c2.put("name", "name");
        c2.put("sqlType", "VARCHAR");
        c2.put("nullable", true);
        c2.put("unique", false);
        c2.put("defaultExpr", null);

        ctx.put("columns", List.of(c1, c2));

        final String fingerprint = MigrationManifestBuilder.fingerprintFromCreateCtx(ctx);

        assertNotNull(fingerprint);
        assertFalse(fingerprint.isEmpty());
    }

    @Test
    void fingerprintFromCreateCtx_shouldBeOrderIndependentForColumns() {
        
        final Map<String, Object> ctx1 = new LinkedHashMap<>();
        ctx1.put("tableName", "users");
        ctx1.put("auditEnabled", true);
        ctx1.put("auditCreatedType", "LOCAL_DATE_TIME");
        ctx1.put("pkColumns", "id, tenant_id");

        final Map<String, Object> cId = new HashMap<>();
        cId.put("name", "id");
        cId.put("sqlType", "BIGINT");
        cId.put("nullable", false);
        cId.put("unique", true);
        cId.put("defaultExpr", "1");

        final Map<String, Object> cName = new HashMap<>();
        cName.put("name", "name");
        cName.put("sqlType", "VARCHAR");
        cName.put("nullable", true);
        cName.put("unique", false);
        cName.put("defaultExpr", null);

        ctx1.put("columns", List.of(cId, cName));

        final Map<String, Object> ctx2 = new LinkedHashMap<>(ctx1);
        ctx2.put("columns", List.of(cName, cId));

        final String fp1 = MigrationManifestBuilder.fingerprintFromCreateCtx(ctx1);
        final String fp2 = MigrationManifestBuilder.fingerprintFromCreateCtx(ctx2);

        assertEquals(fp1, fp2);
    }

}
